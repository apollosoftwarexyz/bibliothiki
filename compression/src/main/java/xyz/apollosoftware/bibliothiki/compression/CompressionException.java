package xyz.apollosoftware.bibliothiki.compression;

import org.jspecify.annotations.NonNull;

import java.io.Serial;

/**
 * An exception that was thrown by the compression module (because of a file
 * format issue or because of an exception raised during a compression or
 * decompression operation).
 */
public sealed class CompressionException extends RuntimeException permits CompressionSecurityException {

    @Serial
    private static final long serialVersionUID = -507114624300185866L;

    /**
     * Construct a {@link CompressionException} raised by this module directly.
     *
     * @param message The human-readable description of the cause.
     */
    public CompressionException(@NonNull final String message) {
        super(message);
    }

    /**
     * Construct a {@link CompressionException} that was ultimately caused by an
     * exception within a different module.
     *
     * @param message The human-readable description of the cause.
     * @param throwable The exception that caused the issue.
     */
    public CompressionException(@NonNull final String message, @NonNull final Throwable throwable) {
        super(message, throwable);
    }

}
