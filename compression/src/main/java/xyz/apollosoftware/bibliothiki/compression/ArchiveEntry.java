package xyz.apollosoftware.bibliothiki.compression;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

import static xyz.apollosoftware.bibliothiki.compression.CompressionSecurityException.SecurityViolation.*;

/**
 * An entry found in an archive.
 *
 * @param name The name of the entry, relative to the root of the archive.
 * @param type The type of entry (see {@link Type}).
 * @param permissions The permissions for the entry (if available).
 * @param size The uncompressed size of the entry (if available).
 */
public record ArchiveEntry(@NonNull String name, @NonNull Type type, @Nullable PermissionSet permissions, @Nullable Long size) {

    /**
     * The builder for {@link ArchiveEntry}.
     */
    public static final class Builder {

        private String name;
        private Type type;
        private PermissionSet permissions;
        private Long size;

        private Builder() {
        }

        /**
         * Set the name (qualified file path) of the entry.
         *
         * @param name The name of the entry.
         * @return The same {@link Builder}.
         */
        @NonNull
        public Builder name(@NonNull String name) {
            this.name = name;
            return this;
        }

        /**
         * Set the {@link Type} of the entry.
         *
         * @param type The type of the entry.
         * @return The same {@link Builder}.
         */
        @NonNull
        public Builder type(@NonNull Type type) {
            this.type = type;
            return this;
        }

        /**
         * Set the UNIX permissions of the entry.
         *
         * @param permissions The permissions of the entry.
         * @return The same {@link Builder}.
         */
        @NonNull
        public Builder permissions(@Nullable PermissionSet permissions) {
            this.permissions = permissions;
            return this;
        }

        /**
         * Set the file size, in bytes, of the entry.
         *
         * @param size The size of the entry.
         * @return The same {@link Builder}.
         */
        @NonNull
        public Builder size(@Nullable Long size) {
            this.size = size;
            return this;
        }

        /**
         * Build the {@link ArchiveEntry}.
         *
         * <p>
         * If any of the fields were invalid, they will fail at this point as
         * this is when the {@link ArchiveEntry} constructor is executed.
         *
         * @return The built {@link ArchiveEntry}.
         */
        @NonNull
        public ArchiveEntry build() {
            return new ArchiveEntry(this.name, this.type, this.permissions, this.size);
        }

    }

    /**
     * Create a builder for an {@link ArchiveEntry}.
     *
     * @return The {@link Builder}.
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Permission flags for a single target (e.g., user, group or world).
     *
     * <p>
     * In a UNIX file permissions value, this is represented by a single octal
     * digit.
     *
     * @param read Whether read permission is granted.
     * @param write Whether write permission is granted.
     * @param execute Whether execute permission is granted.
     * @see PermissionSet
     */
    public record Permission(boolean read, boolean write, boolean execute) {

        /**
         * The read permission bit mask (UNIX {@code 0b100}).
         */
        public static final int READ_MASK = 0b100;

        /**
         * The write permission bit mask (UNIX {@code 0b010}).
         */
        public static final int WRITE_MASK = 0b010;

        /**
         * The execute permission bit mask (UNIX {@code 0b001}).
         */
        public static final int EXECUTE_MASK = 0b001;

        /**
         * Decode the permission from the numeric value.
         *
         * <p>
         * This method assumes that any necessary shifts have already been
         * performed (i.e., the only bits that should be set are the lower three
         * octets).
         *
         * @param value The value to decode.
         * @return The decoded value.
         */
        @NonNull
        public static Permission decode(int value) {
            if ((value & 0b111) != value) {
                throw new IllegalArgumentException("Invalid permission bits: %x".formatted(value));
            }

            return new Permission(
                (value & READ_MASK) != 0,
                (value & WRITE_MASK) != 0,
                (value & EXECUTE_MASK) != 0
            );
        }

        @Override
        @NonNull
        public String toString() {
            return (this.read() ? "r" : "-") +
                    (this.write() ? "w" : "-") +
                    (this.execute() ? "x" : "-");
        }
    }

    /**
     * A set of permissions.
     *
     * <p>
     * This covers the UNIX permissions values for user, group and world (other)
     * permissions. Each permission within the set is represented by a
     * {@link Permission} value.
     *
     * @param user The user (owner) permissions.
     * @param group The group (owner) permissions.
     * @param world The world (non-owner) permissions.
     */
    public record PermissionSet(@NonNull Permission user, @NonNull Permission group, @NonNull Permission world) {

        /**
         * Decode the permission set from the numeric value.
         *
         * <p>
         * This method assumes there are three octal digits starting in the
         * least significant bit position - in the order user, group then world.
         * (That is, the world permission digit is expected to be the least
         * significant three octets).
         *
         * @param mode The mode value to decode.
         * @return The decoded {@link PermissionSet}.
         */
        @NonNull
        public static PermissionSet decode(long mode) {
            return new PermissionSet(
                Permission.decode((int) ((mode >> 6) & 0b111)),
                Permission.decode((int) ((mode >> 3) & 0b111)),
                Permission.decode((int) (mode & 0b111))
            );
        }

    }

    /**
     * The type of {@link ArchiveEntry}.
     */
    public enum Type {
        /**
         * An ordinary file.
         */
        FILE,

        /**
         * A directory (folder).
         */
        DIRECTORY,

        /**
         * Hard link to another file (from elsewhere in the archive).
         */
        HARD_LINK,

        /**
         * Soft (symbolic) link to another file.
         */
        SOFT_LINK
    }

    /**
     * Construct an entry found in an archive.
     *
     * <p>
     * This constructor also normalizes the path and ensures it is not an
     * absolute path.
     *
     * @param name The name of the archive entry.
     * @param type The type of the archive entry.
     * @param permissions The permissions (if available) of the archive entry.
     * @param size The size (if available) of the archive entry.
     * @see Type
     * @see Builder
     * @throws CompressionSecurityException If the name denotes an absolute file
     *                                      path.
     */
    public ArchiveEntry {
        Objects.requireNonNull(name, "The name of the archive entry must not be null");
        Objects.requireNonNull(type, "The type of the archive entry must not be null");

        // Ensure the name is not blank.
        if (name.isBlank()) {
            throw new CompressionSecurityException(BLANK_FILENAME);
        }

        // Normalize the path (removes redundant path components).
        final var path = Path.of(name).normalize();
        if (path.isAbsolute()) {
            throw new CompressionSecurityException(FORBIDDEN_ABSOLUTE_PATH);
        }

        // Use the interpreted path.
        name = path.toString();

        if (type == Type.DIRECTORY && !name.endsWith("/")) {
            // If the path is a directory, ensure it ends with a trailing slash.
            name += "/";
        } else if (name.endsWith("/")) {
            // Otherwise, ensure it does not end with a trailing slash.
            name = name.substring(0, name.lastIndexOf('/'));
        }
    }

    /**
     * Resolve a reference to a file, relative to the given {@code parent}.
     *
     * <p>
     * If {@code includeRoot} is false, the first path component of every entry
     * inside the archive will be ignored (this allows an archived directory to
     * be extracted directly into the given {@code parent}).
     *
     * @param parent The parent directory to resolve paths relative to.
     * @param includeRoot Whether to include the first path component for each
     *                    component in the archive.
     * @return The resolved file.
     * @throws IOException If file system queries fail.
     */
    @NonNull
    public File resolveFile(@NonNull File parent, boolean includeRoot) throws IOException {
        final var canonicalParent = parent.getCanonicalFile();
        var normalizedPath = Path.of(this.name);

        // If the root directory should not be included, relativize the path to
        // that directory.
        if (!includeRoot) {
            final var topLevelDirectory = normalizedPath.subpath(0, 1);
            normalizedPath = normalizedPath.relativize(topLevelDirectory);
        }

        // Canonicalize the path and then return it.
        final var destinationFile = new File(canonicalParent, normalizedPath.toString()).getCanonicalFile();
        if (!destinationFile.toPath().startsWith(canonicalParent.toPath())) {
            throw new CompressionSecurityException(PATH_OUTSIDE_PARENT);
        }

        return destinationFile;
    }

}
