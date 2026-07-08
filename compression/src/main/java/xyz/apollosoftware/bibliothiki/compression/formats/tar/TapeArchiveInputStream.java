package xyz.apollosoftware.bibliothiki.compression.formats.tar;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import xyz.apollosoftware.bibliothiki.compression.ArchiveEntry;
import xyz.apollosoftware.bibliothiki.compression.ArchiveInputStream;
import xyz.apollosoftware.bibliothiki.compression.CompressionException;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static xyz.apollosoftware.bibliothiki.compression.utils.Streams.pipeNBytes;

/**
 * Support for reading (T)ape (AR)chive (TAR) files.
 *
 * <h2>Implementation Notes</h2>
 *
 * <p>
 * To better support unbuffered streams, this API is not random access. (That
 * is, once an entry's header has been read, the content must be read by calling
 * {@link #writeCurrentEntryTo(OutputStream)} or skipped by calling
 * {@link #getNextEntry()}).
 *
 * <p>
 * {@link #closeCurrentEntry()} is a no-op. Close entries by calling
 * {@link #getNextEntry()} or close the stream by calling {@link #close()},
 * instead.
 *
 * <p>
 * <a href="https://en.wikipedia.org/wiki/Tar_(computing)" target="_blank">tar (Wikipedia)</a>
 */
public class TapeArchiveInputStream extends ArchiveInputStream {

    /**
     * Peek the first two bytes of the stream to see if they match the GZIP
     * magic number.
     *
     * @param stream The stream to check.
     * @return True, if the stream is a GZIP stream, otherwise false.
     */
    public static boolean isGzipStream(@NonNull final InputStream stream) {
        if (!stream.markSupported()) {
            throw new UnsupportedOperationException("Unable to peek stream bytes (mark not supported)");
        }

        try {
            stream.mark(2);
            final var magic = stream.readNBytes(2);
            stream.reset();

            if (magic[0] == (byte) 0x1F &&
                magic[1] == (byte) 0x8B) {
                return true;
            }
        } catch (IOException ignored) {
        }

        return false;
    }

    /**
     * Peek the first 512 bytes (TAR header) of the stream to see if the stream
     * contains TAR data.
     *
     * @param stream The stream to check.
     * @return True, if the stream is a TAR stream, otherwise false.
     */
    public static boolean isTarStream(@NonNull final InputStream stream) {
        if (!stream.markSupported()) {
            throw new UnsupportedOperationException("Unable to peek stream bytes (mark not supported)");
        }

        try {
            stream.mark(TAR_ENTRY_ALIGNMENT);

            // TODO: check the header
            // https://github.com/apollosoftwarexyz/bibliothiki/issues/1
            stream.readNBytes(TAR_ENTRY_ALIGNMENT);

            stream.reset();

            return true;
        } catch (IOException ignored) {
        }

        return false;
    }

    /**
     * The byte alignment of TAR entries.
     */
    private static final int TAR_ENTRY_ALIGNMENT = 512;

    /**
     * The active entry header.
     */
    @Nullable
    private TapeArchiveEntryHeader activeEntry = null;

    /**
     * Whether there is an active entry for which the header has already been
     * read.
     *
     * <p>
     * (Implying that the next data on the stream is the contents of that
     * header).
     */
    private boolean entryDataWaiting = false;

    /**
     * Used to immediately throw if the stream is accessed after closing.
     */
    private boolean isClosed = false;

    /**
     * Construct a {@link TapeArchiveInputStream} that encapsulates the given
     * {@link InputStream}.
     *
     * @param stream The input stream that will provide tar data.
     */
    public TapeArchiveInputStream(@NonNull final InputStream stream) {
        super(stream);
    }

    @Override
    public synchronized @Nullable ArchiveEntry getNextEntry() {
        return getNextEntry(null);
    }

    @Nullable
    private synchronized ArchiveEntry getNextEntry(@Nullable final TapeArchivePaxAttributes metadata) {
        if (this.isClosed) {
            throw new IllegalStateException("Stream already closed");
        }

        try {

            // If the data for the current entry has not yet been read, skip it.
            if (entryDataWaiting && activeEntry != null && activeEntry.fileSize() > 0) {
                try {
                    final long fileSize = activeEntry.fileSize();

                    // Skip the file (and move to the next block)...
                    stream.skipNBytes(fileSize);
                    skipToNextAlignment(fileSize);

                    entryDataWaiting = false;
                    return getNextEntry();
                } catch (final IOException ex) {
                    throw new CompressionException("Failed to skip to next entry", ex);
                }
            }

            final TapeArchiveEntryHeader header;
            try {
                final var headerData = ByteBuffer.wrap(stream.readNBytes(TapeArchiveEntryHeader.LENGTH_BYTES)).mark();

                // Check whether the header is empty.
                if (isZero(headerData)) {
                    if (isZero(ByteBuffer.wrap(stream.readNBytes(TapeArchiveEntryHeader.LENGTH_BYTES)))) {
                        // This is two consecutive NULL entries - close the
                        // archive as this signals the end.
                        this.close();
                        return null;
                    }

                    // The archive is corrupted.
                    throw new CompressionException("Unexpected NULL entry.");
                }

                header = TapeArchiveEntryHeader.decode(headerData.reset());
            } catch (final IOException ex) {
                throw new CompressionException("Failed to read archive header", ex);
            }

            // Silently handle extended metadata.
            if (header.type() == TapeArchiveEntryHeader.Type.EXTENDED_HEADER_WITH_METADATA) {
                if (metadata == null) {
                    try {
                        final int metadataLength = (int) header.fileSize();
                        if (metadataLength == 0) {
                            throw new CompressionException("Invalid metadata");
                        }

                        final var nextMetadata = TapeArchivePaxAttributes.decode(new String(stream.readNBytes(metadataLength), StandardCharsets.UTF_8));
                        this.skipToNextAlignment(metadataLength);

                        return getNextEntry(nextMetadata);
                    } catch (final IOException ex) {
                        throw new CompressionException("Failed to read extended attributes (%s)".formatted(header.getFullName()), ex);
                    }
                } else {
                    throw new CompressionException("Found consecutive entries of type %s (illegal)".formatted(TapeArchiveEntryHeader.Type.EXTENDED_HEADER_WITH_METADATA));
                }
            }

            // Resolve the type of entry.
            final ArchiveEntry.Type type = switch (header.type()) {
                case NORMAL -> ArchiveEntry.Type.FILE;
//                case HARD_LINK -> ArchiveEntry.Type.HARD_LINK;
//                case SOFT_LINK -> ArchiveEntry.Type.SOFT_LINK;
                case DIRECTORY -> ArchiveEntry.Type.DIRECTORY;
                default -> throw new CompressionException("Unsupported entry type: %s".formatted(header.type()));
            };

            try {
                return ArchiveEntry.builder()
                    .name(metadata != null && metadata.path() != null ? metadata.path() : header.getFullName())
                    .type(type)
                    .permissions(ArchiveEntry.PermissionSet.decode(header.mode()))
                    .size(header.fileSize())
                    .build();
            } finally {
                activeEntry = header;
                entryDataWaiting = true;
            }
        } catch (final Exception ex) {
            // Handle any exception by closing the stream.
            close();
            throw ex;
        }
    }

    @Override
    public synchronized void writeCurrentEntryTo(@NonNull OutputStream stream) throws IOException {
        if (activeEntry == null) {
            throw new CompressionException("Cannot write current entry as there is no current entry");
        }

        // Pipe the entry bytes into the output stream and then skip bytes to
        // align with the next header.
        try {
            skipToNextAlignment(pipeNBytes(this.stream, stream, activeEntry.fileSize()));
        } catch (CompressionException ex) {
            close();
            throw ex;
        }

        activeEntry = null;
        entryDataWaiting = false;
    }

    @Override
    public synchronized void closeCurrentEntry() {
        // no-op (handled by getNextEntry).
    }

    @Override
    public synchronized void close() {
        try {
            // Using the input stream after close is undefined behavior.
            this.activeEntry = null;
            this.entryDataWaiting = false;
            super.close();
        } finally {
            this.isClosed = true;
        }
    }

    private void skipToNextAlignment(long relativeOffset) throws IOException {
        if (relativeOffset % TAR_ENTRY_ALIGNMENT != 0) {
            this.stream.skipNBytes(TAR_ENTRY_ALIGNMENT - (relativeOffset % TAR_ENTRY_ALIGNMENT));
        }
    }

    /**
     * Exhausts the buffer, ensuring that every byte exhausted is zero.
     *
     * @param buffer The buffer to check.
     * @return True if, and only if, every subsequent byte in the buffer was
     *         zero.
     */
    private static boolean isZero(@NonNull ByteBuffer buffer) {
        boolean isZero = true;

        while (buffer.remaining() >= Long.BYTES) {
            isZero &= buffer.getLong() == 0;
        }

        while (buffer.remaining() >= Byte.BYTES) {
            isZero &= buffer.get() == 0;
        }

        return isZero;
    }

}
