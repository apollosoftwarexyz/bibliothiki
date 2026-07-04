package xyz.apollosoftware.bibliothiki.compression.utils;

import org.jspecify.annotations.NonNull;
import xyz.apollosoftware.bibliothiki.compression.CompressionException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * Utility methods for working with streams.
 */
public final class Streams {

    /**
     * The size of the buffers to transfer.
     */
    private static final int BUFFER_SIZE = 4096;

    private Streams() {}

    /**
     * Pipe n bytes from the given {@link InputStream} to the given
     * {@link OutputStream}.
     *
     * <p>
     * If n is less than or equal to zero, this method immediately returns zero.
     *
     * @param input The input stream to read bytes from.
     * @param output The output stream to write bytes to.
     * @param n The number of bytes to pipe.
     * @return The number of bytes that were piped.
     */
    public static long pipeNBytes(@NonNull InputStream input, @NonNull OutputStream output, final long n) {
        Objects.requireNonNull(input, "The input stream must not be null.");
        Objects.requireNonNull(output, "The output stream must not be null.");

        // If there are no bytes to copy, do nothing.
        if (n <= 0) return 0;

        byte[] buffer = new byte[BUFFER_SIZE];
        long bytesRead = 0;
        while (bytesRead < n) {
            // Safely compute how much to transfer (it should be any number of
            // bytes up to BUFFER_SIZE, ensuring overflows do not cause the
            // wrong amount).
            final long remaining = n - bytesRead;

            // Read the requested bytes from the buffer.
            int result;
            try {
                result = input.read(buffer, 0, remaining >= BUFFER_SIZE ? BUFFER_SIZE : (int) remaining);
            } catch (IOException ex) {
                ifPossible(input::close);
                ifPossible(output::close);
                throw new CompressionException("Failed to read entry from archive", ex);
            }

            // Ensure we do not receive a premature EOF.
            if (result == -1) {
                ifPossible(input::close);
                ifPossible(output::close);
                throw new CompressionException("Unexpected EOF");
            }

            // Write the buffer to the output stream.
            try {
                output.write(buffer, 0, result);
                bytesRead += result;
            } catch (IOException ex) {
                ifPossible(input::close);
                ifPossible(output::close);
                throw new CompressionException("Failed to write bytes to output stream", ex);
            }
        }

        return bytesRead;
    }

    @FunctionalInterface
    interface IfPossible {
        void execute() throws IOException;
    }

    private static void ifPossible(@NonNull IfPossible ifPossible) {
        try {
            ifPossible.execute();
        } catch (IOException ignored) {
        }
    }

}
