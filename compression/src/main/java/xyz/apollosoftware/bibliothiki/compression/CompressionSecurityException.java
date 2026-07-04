package xyz.apollosoftware.bibliothiki.compression;

import org.jspecify.annotations.NonNull;

import java.io.Serial;

/**
 * A subset of {@link CompressionException} that is caused by a specific
 * {@link SecurityViolation}.
 *
 * <p>
 * These are categorized in this manner to allow applications to treat them in
 * a specific manner, if required.
 */
public final class CompressionSecurityException extends CompressionException {

    @Serial
    private static final long serialVersionUID = -8984010978683058037L;

    /**
     * Construct a {@link CompressionSecurityException} based on the given
     * preset {@link SecurityViolation} type.
     *
     * @param violation The violation that caused the security exception.
     */
    public CompressionSecurityException(@NonNull final SecurityViolation violation) {
        super(violation.getDefaultMessage());
    }

    /**
     * The security violation that caused the exception.
     */
    public enum SecurityViolation {
        /**
         * An archive entry was created (or was found in an existing archive)
         * with an absolute file path, which is forbidden.
         */
        FORBIDDEN_ABSOLUTE_PATH("Absolute paths are forbidden"),

        /**
         * An archive entry was outside of the parent directory (e.g., because
         * of relative paths).
         */
        PATH_OUTSIDE_PARENT("Archive entry path is outside of the parent directory"),

        /**
         * An archive entry was created (or was found in an existing archive)
         * with a blank filename.
         */
        BLANK_FILENAME("Filenames must not be blank");

        @NonNull
        private final String defaultMessage;

        SecurityViolation(@NonNull final String defaultMessage) {
            this.defaultMessage = defaultMessage;
        }

        /**
         * Get the default message (in English) for the Security Violation.
         *
         * @return The default error message.
         */
        @NonNull
        public String getDefaultMessage() {
            return defaultMessage;
        }
    }

}
