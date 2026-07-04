package xyz.apollosoftware.bibliothiki.compression.zip;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import xyz.apollosoftware.bibliothiki.compression.ArchiveEntry;
import xyz.apollosoftware.bibliothiki.compression.ArchiveFormat;
import xyz.apollosoftware.bibliothiki.compression.ArchiveInputStream;
import xyz.apollosoftware.bibliothiki.compression.CompressionException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static xyz.apollosoftware.bibliothiki.compression.utils.Streams.pipeNBytes;

/**
 * An adapter for the built-in {@code java.util.zip} capability.
 */
public class ZipArchiveInputStream extends ArchiveInputStream {

    /**
     * Peek the first four bytes of the stream to see if they match the ZIP
     * magic number.
     *
     * @param stream The stream to check.
     * @return True, if the stream is a ZIP stream, otherwise false.
     */
    public static boolean isZipStream(@NonNull final InputStream stream) {
        if (!stream.markSupported()) {
            throw new UnsupportedOperationException("Unable to peek stream bytes (mark not supported)");
        }

        boolean isZip = false;

        stream.mark(4);
        try {
            final var magic = stream.readNBytes(4);

            if (magic[0] == (byte) 0x50 &&
                magic[1] == (byte) 0x4B &&
                magic[2] == (byte) 0x03 &&
                magic[3] == (byte) 0x04) {
                isZip = true;
            }
        } catch (IOException ignored) {
        }

        try {
            stream.reset();
        } catch (IOException ex) {
            throw new CompressionException("Failed to rewind stream header", ex);
        }

        return isZip;
    }

    @NonNull
    private final ZipInputStream zipStream;

    @Nullable
    private ZipEntry zipEntry = null;

    /**
     * Construct a {@link ZipArchiveInputStream} that encapsulates the given
     * {@link InputStream}.
     *
     * @param stream The input stream that will provide ZIP data.
     */
    public ZipArchiveInputStream(@NonNull final InputStream stream) {
        super(stream);
        this.zipStream = new ZipInputStream(stream);
    }

    @Override
    @Nullable
    public ArchiveEntry getNextEntry() {
        try {
            zipEntry = zipStream.getNextEntry();
        } catch (IOException ex) {
            throw new CompressionException("I/O error whilst getting next ZIP entry", ex);
        }

        return Optional.ofNullable(zipEntry)
            .map(entry -> ArchiveEntry.builder()
                .name(entry.getName())
                .type(entry.isDirectory() ? ArchiveEntry.Type.DIRECTORY : ArchiveEntry.Type.FILE)
                // (permission data is not available for a ZIP)
                .size(entry.getSize())
                .build()
            )
            .orElse(null);
    }

    @Override
    public void writeCurrentEntryTo(@NonNull OutputStream stream) {
        if (zipEntry == null) {
            throw new CompressionException("Cannot write current entry as there is no current entry");
        }

        try {
            pipeNBytes(this.zipStream, stream, zipEntry.getSize());
        } catch (CompressionException ex) {
            close();
            throw ex;
        }
    }

    @Override
    public void closeCurrentEntry() {
        if (zipEntry != null) {
            try {
                zipStream.closeEntry();
                zipEntry = null;
            } catch (IOException ex) {
                throw new CompressionException("Failed to close ZIP entry", ex);
            }
        }
    }

    @Override
    public void close() {
        try {
            zipStream.close();
            super.close();
        } catch (final IOException ex) {
            throw new CompressionException("Failed to close ZIP stream", ex);
        }
    }

}
