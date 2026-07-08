package xyz.apollosoftware.bibliothiki.versioning;

import org.jspecify.annotations.NonNull;

import java.io.Serial;

/**
 * An exception that was thrown by the versioning module (because of a version
 * formatting or semantics issue).
 */
public final class VersioningException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -3108508327526200335L;

    /**
     * Construct a {@link VersioningException} raised by this module directly.
     *
     * @param message The human-readable description of the cause.
     */
    public VersioningException(String message) {
        super(message);
    }

    /**
     * Construct a {@link VersioningException} that was ultimately caused by an
     * exception within a different module.
     *
     * @param message The human-readable description of the cause.
     * @param throwable The exception that caused the issue.
     */
    public VersioningException(@NonNull final String message, @NonNull final Throwable throwable) {
        super(message, throwable);
    }

}
