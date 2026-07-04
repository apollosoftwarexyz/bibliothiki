package xyz.apollosoftware.bibliothiki.compression.tar;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import xyz.apollosoftware.bibliothiki.compression.CompressionException;
import xyz.apollosoftware.bibliothiki.compression.CompressionSecurityException;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import static xyz.apollosoftware.bibliothiki.compression.CompressionSecurityException.SecurityViolation.BLANK_FILENAME;

/**
 * The header for a Tape Archive file.
 *
 * @param name The path and name of the file (max. 100 chars).
 * @param mode The UNIX permission mode of the file.
 * @param owner The UID of the owner of the file.
 * @param group The UID of the group that owns the file.
 * @param fileSize The file size, in bytes.
 * @param lastModified The timestamp that the file was last modified.
 * @param checksum The checksum for the header record.
 * @param type The file type.
 * @param linkedFile The path that the file links to, if the link indicator
 *                   indicates that the file is a link, rather than a normal
 *                   file. If the link indicator is such that the file is not
 *                   a link, this is null.
 * @param unixStandardHeader The UNIX Standard header data (if present).
 */
public record TapeArchiveEntryHeader(
    @NonNull String name,
    long mode,
    long owner,
    long group,
    long fileSize,
    Instant lastModified,
    long checksum,
    @NonNull Type type,
    @Nullable String linkedFile,
    @Nullable UnixStandardEntryHeader unixStandardHeader
) {

    /**
     * The builder for {@link TapeArchiveEntryHeader}.
     */
    public static final class Builder {

        private String name;
        private long mode;
        private long owner;
        private long group;
        private long fileSize;
        private Instant lastModified;
        private long checksum;
        private Type type;
        private String linkedFile;
        private UnixStandardEntryHeader unixStandardHeader;

        private Builder() {
        }

        /**
         * Set the name of the entry.
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
         * Set the mode of the entry.
         *
         * @param mode The mode of the entry.
         * @return The same {@link Builder}.
         */
        @NonNull
        public Builder mode(long mode) {
            this.mode = mode;
            return this;
        }

        /**
         * Set the owner of the entry.
         *
         * @param owner The owner of the entry.
         * @return The same {@link Builder}.
         */
        @NonNull
        public Builder owner(long owner) {
            this.owner = owner;
            return this;
        }

        /**
         * Set the group of the entry.
         *
         * @param group The group of the entry.
         * @return The same {@link Builder}.
         */
        @NonNull
        public Builder group(long group) {
            this.group = group;
            return this;
        }

        /**
         * Set the fileSize of the entry.
         *
         * @param fileSize The fileSize of the entry.
         * @return The same {@link Builder}.
         */
        @NonNull
        public Builder fileSize(long fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        /**
         * Set the last modification timestamp of the entry.
         *
         * @param lastModified The last modification timestamp of the entry.
         * @return The same {@link Builder}.
         */
        @NonNull
        public Builder lastModified(Instant lastModified) {
            this.lastModified = lastModified;
            return this;
        }

        /**
         * Set the checksum of the entry.
         *
         * @param checksum The checksum of the entry.
         * @return The same {@link Builder}.
         */
        @NonNull
        public Builder checksum(long checksum) {
            this.checksum = checksum;
            return this;
        }

        /**
         * Set the {@link Type} of the entry.
         *
         * @param type The type of the entry.
         * @return The same {@link Builder}.
         */
        @NonNull
        public Builder type(Type type) {
            this.type = type;
            return this;
        }

        /**
         * Set the linked file of the entry (relevant only to types where
         * {@link Type#isLink()} is true).
         *
         * @param linkedFile The linked file of the entry.
         * @return The same {@link Builder}.
         */
        @NonNull
        public Builder linkedFile(String linkedFile) {
            this.linkedFile = linkedFile;
            return this;
        }

        /**
         * Set the (UStar) UNIX Standard header information of the entry.
         *
         * @param unixStandardHeader The UNIX Standard header information of the
         *                           entry.
         * @return The same {@link Builder}.
         */
        @NonNull
        public Builder unixStandardHeader(UnixStandardEntryHeader unixStandardHeader) {
            this.unixStandardHeader = unixStandardHeader;
            return this;
        }

        /**
         * Build the {@link TapeArchiveEntryHeader}.
         *
         * <p>
         * If any of the fields were invalid, they will fail at this point as
         * this is when the {@link TapeArchiveEntryHeader} constructor is
         * executed.
         *
         * @return The built {@link TapeArchiveEntryHeader}.
         */
        @NonNull
        public TapeArchiveEntryHeader build() {
            return new TapeArchiveEntryHeader(
                this.name,
                this.mode,
                this.owner,
                this.group,
                this.fileSize,
                this.lastModified,
                this.checksum,
                this.type,
                this.linkedFile,
                this.unixStandardHeader
            );
        }

    }

    /**
     * Create a builder for a {@link TapeArchiveEntryHeader}.
     *
     * <p>
     * This can be useful when wrapping code for creating archives, as well as
     * for reading parameters from a tar file.
     *
     * @return The {@link Builder}.
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * The length, in bytes, of the tape archive header.
     */
    public static final int LENGTH_BYTES = 512;

    /**
     * The length, in bytes, of the file path and name field.
     */
    public static final int FILENAME_LENGTH_BYTES = 100;

    /**
     * The length, in bytes, of the file path and name prefix.
     */
    public static final int FILENAME_PREFIX_LENGTH_BYTES = 155;

    /**
     * The maximum name that can be stored in a {@link TapeArchiveEntryHeader},
     * including the {@link UnixStandardEntryHeader}.
     *
     * <p>
     * (Longer names may be supported but will be implemented with the
     * {@link TapeArchivePaxAttributes}).
     */
    public static final int FULL_FILENAME_LENGTH_BYTES = FILENAME_LENGTH_BYTES + FILENAME_PREFIX_LENGTH_BYTES;

    /**
     * Construct a header for a tar archive.
     *
     * <p>
     * This record represents either an entry that is being added to an archive
     * or which has been parsed from an existing archive.
     *
     * @param name The name of the entry.
     * @param mode The UNIX permission mode of the entry.
     * @param owner The owner UID of the entry.
     * @param group The group UID of the entry.
     * @param fileSize The size of the entry, in bytes.
     * @param lastModified The last modification timestamp of the entry.
     * @param checksum The checksum of the entry header.
     * @param type The type of entry.
     * @param linkedFile The linked file (if applicable).
     * @param unixStandardHeader The UNIX Standard tar (UStar) header data (if
     *                           applicable).
     */
    public TapeArchiveEntryHeader {
        Objects.requireNonNull(name, "The entry name must not be null");

        // Ensure the name is not blank.
        if (name.isBlank()) {
            throw new CompressionSecurityException(BLANK_FILENAME);
        }

        if (name.length() > FILENAME_LENGTH_BYTES) {
            throw new IllegalArgumentException("The entry name length field (%d) exceeded the maximum length (%d)".formatted(name.length(), FILENAME_LENGTH_BYTES));
        }
    }

    /**
     * The type of tape archive entry.
     */
    public enum Type {
        /**
         * A normal file.
         *
         * <p>
         * In some cases, ASCII NUL (or '\0') is supported. Thus,
         * {@link #getByValue(byte)} will return this value for either '0'
         * (ASCII 0x30) or '\0' (ASCII NUL).
         */
        NORMAL((byte) '0'),

        /**
         * A hard link to another file in the archive.
         */
        HARD_LINK((byte) '1'),

        /**
         * A soft (symbolic) link to another file that may be in the archive
         * or may exist elsewhere on the system.
         */
        SOFT_LINK((byte) '2'),

        /**
         * A UNIX character (device) special file.
         */
        CHARACTER_SPECIAL((byte) '3'),

        /**
         * A UNIX block (device) special file.
         */
        BLOCK_SPECIAL((byte) '4'),

        /**
         * A directory (folder).
         */
        DIRECTORY((byte) '5'),

        /**
         * A FIFO (named pipe).
         */
        FIFO((byte) '6'),

        /**
         * A contiguous file.
         *
         * <p>
         * This type is generally considered obsolete or reserved.
         */
        CONTIGUOUS_FILE((byte) '7'),

        /**
         * A "global extended header with metadata" (POSIX.1-2001).
         */
        GLOBAL_EXTENDED_HEADER((byte) 'g'),

        /**
         * An extended header with metadata for the next file in the
         * archive (POSIX.1-2001).
         */
        EXTENDED_HEADER_WITH_METADATA((byte) 'x');

        private final byte value;

        Type(byte value) {
            this.value = value;
        }

        /**
         * The byte value used in the UNIX Standard entry header for the
         * type of file.
         *
         * @return The byte value for the file type.
         */
        public byte getValue() {
            return this.value;
        }

        /**
         * Resolve the type by its byte value.
         *
         * @param value The byte value to resolve.
         * @return The resolved {@link Type}, or null if one couldn't be
         *         resolved.
         */
        @Nullable
        public static Type getByValue(byte value) {
            if (value == '\0') return Type.NORMAL;

            return Arrays.stream(values())
                    .filter(entry -> entry.getValue() == value)
                    .findFirst()
                    .orElse(null);
        }

        /**
         * Whether the entry type is a link (i.e., {@link #HARD_LINK} or
         * {@link #SOFT_LINK}).
         *
         * @return True if, and only if, the entry type is a link type.
         */
        public boolean isLink() {
            return this == HARD_LINK || this == SOFT_LINK;
        }

        /**
         * Whether the entry type is a special/device entry (i.e.,
         * {@link #CHARACTER_SPECIAL} or {@link #BLOCK_SPECIAL}).
         *
         * @return True if, and only if, the entry type is a device type.
         */
        public boolean isDevice() {
            return this == CHARACTER_SPECIAL || this == BLOCK_SPECIAL;
        }
    }

    /**
     * The UStar (UNIX Standard tar) format extends the nominal tar header to
     * include additional fields.
     *
     * <p>
     * These fields appear within the 255 bytes of the header record that
     * would otherwise be NULL (when padding the header to 512 bytes).
     *
     * @param version The UStar version number (should be "00").
     * @param userName The name of the user that owns the file.
     * @param groupName The name of the group that owns the file.
     * @param deviceMajorNumber The major version number of the device (for
     *                          special device files).
     * @param deviceMinorNumber The minor version number of the device (for
     *                          special device files).
     * @param filenamePrefix The prefix to prepend to the filename.
     */
    public record UnixStandardEntryHeader(
        short version,
        @Nullable String userName,
        @Nullable String groupName,
        long deviceMajorNumber,
        long deviceMinorNumber,
        @Nullable String filenamePrefix
    ) {

        /**
         * The builder for {@link UnixStandardEntryHeader}.
         */
        public static final class Builder {

            private short version;
            private String userName;
            private String groupName;
            private long deviceMajorNumber;
            private long deviceMinorNumber;
            private String filenamePrefix;

            private Builder() {
            }

            /**
             * Set the version of the entry.
             *
             * @param version The version of the entry.
             * @return The same {@link Builder}.
             */
            @NonNull
            public Builder version(short version) {
                this.version = version;
                return this;
            }

            /**
             * Set the owner username of the entry.
             *
             * @param userName The username of the entry.
             * @return The same {@link Builder}.
             */
            @NonNull
            public Builder userName(String userName) {
                this.userName = userName;
                return this;
            }

            /**
             * Set the owner group name of the entry.
             *
             * @param groupName The group name of the entry.
             * @return The same {@link Builder}.
             */
            @NonNull
            public Builder groupName(String groupName) {
                this.groupName = groupName;
                return this;
            }

            /**
             * Set the device major version number of the entry. (This is
             * relevant only to types where {@link Type#isDevice()} is true).
             *
             * @param deviceMajorNumber The device major version number of the
             *                          entry.
             * @return The same {@link Builder}.
             */
            @NonNull
            public Builder deviceMajorNumber(long deviceMajorNumber) {
                this.deviceMajorNumber = deviceMajorNumber;
                return this;
            }

            /**
             * Set the device minor version number of the entry. (This is
             * relevant only to types where {@link Type#isDevice()} is true).
             *
             * @param deviceMinorNumber The device minor version number of the
             *                          entry.
             * @return The same {@link Builder}.
             */
            @NonNull
            public Builder deviceMinorNumber(long deviceMinorNumber) {
                this.deviceMinorNumber = deviceMinorNumber;
                return this;
            }

            /**
             * Set the filename prefix of the entry.
             *
             * <p>
             * This is the additional 155 characters that can be prepended to
             * the {@link TapeArchiveEntryHeader#name()} to produce a full 255
             * character UNIX filename.
             *
             * @param filenamePrefix The filename prefix of the entry.
             * @return The same {@link Builder}.
             */
            @NonNull
            public Builder filenamePrefix(String filenamePrefix) {
                this.filenamePrefix = filenamePrefix;
                return this;
            }

            /**
             * Build the {@link UnixStandardEntryHeader}.
             *
             * <p>
             * If any of the fields were invalid, they will fail at this point
             * as this is when the {@link UnixStandardEntryHeader} constructor
             * is executed.
             *
             * @return The built {@link UnixStandardEntryHeader}.
             */
            @NonNull
            public UnixStandardEntryHeader build() {
                return new UnixStandardEntryHeader(
                    version,
                    userName,
                    groupName,
                    deviceMajorNumber,
                    deviceMinorNumber,
                    filenamePrefix
                );
            }

        }

        /**
         * Construct a UNIX Standard (UStar) header for a tar archive header.
         *
         * @param version The version of the header.
         * @param userName The name of the user that owns the entry.
         * @param groupName The name of the group that owns the entry.
         * @param deviceMajorNumber The major version number (for device files).
         * @param deviceMinorNumber The minor version number (for device files).
         * @param filenamePrefix The prefix to prepend to the name.
         */
        public UnixStandardEntryHeader {
            if (filenamePrefix != null && filenamePrefix.length() > FILENAME_PREFIX_LENGTH_BYTES) {
                throw new IllegalArgumentException("The entry name prefix length field (%d) exceeded the maximum length (%d)".formatted(filenamePrefix.length(), FILENAME_LENGTH_BYTES));
            }
        }

        /**
         * The ASCII bytes for 'ustar' are used as the indicator for a
         * {@link UnixStandardEntryHeader}.
         */
        public static final String INDICATOR = "ustar";

        /**
         * Create a builder for a UStar header
         * ({@link UnixStandardEntryHeader}).
         *
         * @return The {@link Builder}.
         */
        @NonNull
        public static Builder builder() {
            return new Builder();
        }

    }

    /**
     * Returns whether the {@link UnixStandardEntryHeader} is present (i.e.,
     * whether the UStar fields are present).
     *
     * @return Whether the UStar header is present.
     */
    public boolean hasUnixStandardHeader() {
        return this.unixStandardHeader != null;
    }

    /**
     * Get the full name of the entry.
     *
     * <p>
     * If the entry {@link #hasUnixStandardHeader()} and
     * {@link UnixStandardEntryHeader#filenamePrefix()}, they are concatenated
     * to produce the full name.
     *
     * <p>
     * Otherwise, the {@link #name()} field is used directly.
     *
     * @return The full path and name of the entry.
     */
    @NonNull
    public String getFullName() {
        if (hasUnixStandardHeader()) {
            final var prefix = this.unixStandardHeader().filenamePrefix();
            if (prefix != null) {
                return prefix + this.name();
            }
        }

        return this.name();
    }

    /**
     * Decode a header from its binary representation.
     *
     * @param buffer A buffer pointing to the binary representation of a header.
     * @return The decoded header.
     */
    @NonNull
    public static TapeArchiveEntryHeader decode(@NonNull ByteBuffer buffer) {
        Objects.requireNonNull(buffer, "The buffer must not be null.");
        if (buffer.capacity() != LENGTH_BYTES) {
            throw new IllegalArgumentException("Invalid header length");
        }

        // Read the default header.
        final var headerBuilder = TapeArchiveEntryHeader.builder();
        final String name = readTerminatedString(buffer, FILENAME_LENGTH_BYTES);
        final String mode = readTerminatedString(buffer, 8, true);
        if (!mode.isEmpty()) {
            headerBuilder.mode(Long.parseLong(mode, 8));
        }

        final String owner = readTerminatedString(buffer, 8, true);
        if (!owner.isEmpty()) {
            headerBuilder.owner(Long.parseLong(owner, 8));
        }

        final String group = readTerminatedString(buffer, 8, true);
        if (!group.isEmpty()) {
            headerBuilder.group(Long.parseLong(group, 8));
        }

        final String fileSize = readTerminatedString(buffer, 12, true);

        // Some types (e.g., extended metadata) have empty size (not zero).
        if (!fileSize.isEmpty()) {
            final long parsedFileSize = Long.parseLong(fileSize, 8);

            // Historically, archives can contain files up to 8 GB as only the first
            // 11 octal digits are used. For simplicity, larger files are currently
            // not supported.
            if (parsedFileSize >> (11 * 3) != 0) {
                throw new CompressionException("Unsupported file size: %d".formatted(parsedFileSize));
            }

            headerBuilder.fileSize(parsedFileSize);
        }

        final String lastModified = readTerminatedString(buffer, 12, true);
        if (!lastModified.isEmpty()) {
            headerBuilder.lastModified(Instant.ofEpochSecond(Long.parseLong(lastModified, 8)));
        }

        final long checksum = buffer.getLong();

        final byte rawType = buffer.get();
        final Type type = Optional.ofNullable(Type.getByValue(rawType))
            .orElseThrow(() -> new CompressionException("Unsupported type: '%s'".formatted((char) rawType)));

        final String linkedFileName = type.isLink()
                ? readTerminatedString(buffer, FILENAME_LENGTH_BYTES, true)
                : null;

        // Check if there's an extended header by reading the next bytes after
        // the default header.
        final String extendedHeaderIndicator = readTerminatedString(buffer, 6);

        return headerBuilder
            .name(name)
            .checksum(checksum)
            .type(type)
            .linkedFile(linkedFileName)
            // Interpret the UNIX Standard (UStar) header if it's present.
            .unixStandardHeader(switch (extendedHeaderIndicator) {
                case UnixStandardEntryHeader.INDICATOR -> UnixStandardEntryHeader.builder()
                    .version(buffer.getShort())
                    .userName(readTerminatedString(buffer, 32))
                    .groupName(readTerminatedString(buffer, 32))
                    .deviceMajorNumber(buffer.getLong())
                    .deviceMinorNumber(buffer.getLong())
                    .filenamePrefix(readTerminatedString(buffer, FILENAME_PREFIX_LENGTH_BYTES))
                    .build();
                case "" -> null;
                default -> throw new IllegalArgumentException("Unsupported extended header: %s".formatted(extendedHeaderIndicator));
            })
            .build();
    }

    /**
     * Read up to {@code maxLength} bytes until a null terminator is reached
     * (or {@code maxLength} is reached) as a US-ASCII string.
     *
     * <p>
     * The returned string will never be null but may be empty.
     *
     * @param buffer The data to read.
     * @param maxLength The maximum number of bytes to read.
     * @return The read and decoded string.
     */
    @NonNull
    private static String readTerminatedString(@NonNull final ByteBuffer buffer, final int maxLength) {
        return readTerminatedString(buffer, maxLength, false);
    }

    /**
     * Read up to {@code maxLength} bytes until a null terminator is reached
     * (or {@code maxLength} is reached) as a US-ASCII string.
     *
     * <p>
     * The returned string will never be null but may be empty.
     *
     * <p>
     * If {@code spaceTerminator} is true, a trailing space will also be treated
     * as a terminator character.
     *
     * @param buffer The data to read.
     * @param maxLength The maximum number of bytes to read.
     * @param spaceTerminator Whether spaces should count as terminators.
     * @return The read and decoded string.
     */
    @NonNull
    private static String readTerminatedString(@NonNull final ByteBuffer buffer, final int maxLength, boolean spaceTerminator) {
        final int offset = buffer.arrayOffset() + buffer.position();

        int length = 0;
        for (int i = 0; i < maxLength; i++) {
            // Check whether the current character is a termination character.
            final byte c = buffer.array()[i + offset];
            if (c == '\0') break;

            // In some cases, space can be used as a terminator (e.g., for octal
            // values). It's hard to track down where the definition of this
            // comes from, but JTar and Apache Commons Compress both treat octal
            // values in this way.
            if (spaceTerminator && c == ' ') break;

            // Update the length according to that described by this offset.
            length = i + 1;
        }

        // Advance the buffer position and decode the identified offsets as
        // ASCII.
        buffer.position(buffer.position() + maxLength);
        return new String(buffer.array(), offset, length, StandardCharsets.US_ASCII);
    }

}
