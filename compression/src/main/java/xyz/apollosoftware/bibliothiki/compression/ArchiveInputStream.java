package xyz.apollosoftware.bibliothiki.compression;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import xyz.apollosoftware.bibliothiki.compression.formats.tar.TapeArchiveInputStream;
import xyz.apollosoftware.bibliothiki.compression.utils.PeekableInputStream;
import xyz.apollosoftware.bibliothiki.compression.formats.zip.ZipArchiveInputStream;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

import static xyz.apollosoftware.bibliothiki.compression.formats.tar.TapeArchiveInputStream.isGzipStream;
import static xyz.apollosoftware.bibliothiki.compression.formats.tar.TapeArchiveInputStream.isTarStream;
import static xyz.apollosoftware.bibliothiki.compression.formats.zip.ZipArchiveInputStream.isZipStream;

/**
 * Process an archive one entry at a time.
 */
public abstract class ArchiveInputStream implements Closeable {

    /**
     * The stream of compressed data.
     */
    protected final InputStream stream;

    /**
     * Construct an {@link ArchiveInputStream} from the given stream.
     *
     * <p>
     * The stream should not be exposed externally to the class hierarchy of
     * the {@link ArchiveInputStream}.
     *
     * @param stream The stream containing archive data.
     */
    public ArchiveInputStream(@NonNull final InputStream stream) {
        this.stream = Objects.requireNonNull(stream);
    }

    /**
     * Get the next entry in the archive.
     *
     * <p>
     * If there are no (more) entries in the archive, this method returns null,
     * instead.
     *
     * @return The {@link ArchiveEntry}, if there is one.
     * @see ArchiveEntry
     */
    @Nullable
    public abstract ArchiveEntry getNextEntry();

    /**
     * Write the current archive entry to the given {@link OutputStream}.
     *
     * <p>
     * The {@link #getNextEntry()} must have been called prior, or this method
     * will throw a {@link CompressionException}.
     *
     * <p>
     * A common pattern for writing to files, therefore, would be to invoke
     * {@link #getNextEntry()} and then write the file data with this method.
     *
     * <p>
     * <i>Some</i> implementations may return a {@link CompressionException} for
     * format or decompression failures. Implementations that do this will still
     * return {@link IOException} when an exception is raised writing to the
     * given output stream. In cases where it is not possible to disambiguate,
     * an {@link IOException} is always thrown instead.
     *
     * @param stream The stream to write the file data to.
     * @see #getNextEntry()
     * @throws IOException If writing to the given {@link OutputStream} fails
     *                     with an {@link IOException}.
     * @throws CompressionException If decompressing or reading an entry to
     *                              write fails.
     */
    public abstract void writeCurrentEntryTo(@NonNull OutputStream stream) throws IOException;

    /**
     * Close the current entry.
     *
     * <p>
     * This may be a no-op for some implementations.
     */
    public abstract void closeCurrentEntry();

    @Override
    public void close() {
        try {
            stream.close();
        } catch (final IOException ex) {
            throw new CompressionException("Failed to close archive", ex);
        }
    }

    /**
     * Automatically detects the archive format (and therefore the
     * {@link ArchiveInputStream} implementation) to use for the given stream.
     *
     * @param stream The stream to detect the input stream for.
     * @return The {@link ArchiveInputStream} implementation for the given
     *         stream format.
     * @throws CompressionException If the archive format is not supported.
     */
    @NonNull
    public static ArchiveInputStream detect(@NonNull InputStream stream) {

        if (isGzipStream(stream)) {
            try {
                stream = PeekableInputStream.ensurePeekable(new GZIPInputStream(stream));
            } catch (IOException ex) {
                throw new CompressionException("Failed to begin processing GZIP stream");
            }
        }

        if (isZipStream(stream)) {
            return new ZipArchiveInputStream(stream);
        }

        if (isTarStream(stream)) {
            return new TapeArchiveInputStream(stream);
        }

        throw new CompressionException("Unsupported archive format");
    }

}
