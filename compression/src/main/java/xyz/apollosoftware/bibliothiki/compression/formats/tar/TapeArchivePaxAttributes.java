package xyz.apollosoftware.bibliothiki.compression.formats.tar;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import xyz.apollosoftware.bibliothiki.compression.CompressionException;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import static xyz.apollosoftware.bibliothiki.compression.formats.tar.TapeArchivePaxAttributes.ParsedAttribute.PAX_PATH;

/**
 * The extended attributes data as originally defined by the {@code pax} archive
 * utility and codified in POSIX.1-2001.
 *
 * @param path The full path for the entry.
 */
public record TapeArchivePaxAttributes(@Nullable String path) {

    /**
     * A raw PaxHeader attribute.
     *
     * @param keyword The keyword from the Pax specification.
     * @param value The value of the keyword.
     */
    record ParsedAttribute(String keyword, String value) {

        /**
         * The extended path property (replaces the path from the ordinary or
         * UStar headers).
         */
        public static final String PAX_PATH = "path";

    }

    /**
     * Decode the {@code pax} extended attributes from the given value.
     *
     * @param value The value to decode the attributes from.
     * @return The decoded attributes.
     */
    @NonNull
    public static TapeArchivePaxAttributes decode(@NonNull String value) {
        // Parse the raw attribute lines.
        final var rawAttributes = value.lines().map(line -> {
            final var parts = line.split(" ", 2);

            final long expectedLength;
            try {
                expectedLength = Long.parseLong(parts[0]);
            } catch (NumberFormatException ex) {
                throw new CompressionException("Failed to parse PAX attribute length: %s".formatted(parts[0]), ex);
            }

            // This unfortunately re-decodes the individual line (the
            // alternative is to parse line-by-line when passing to this decode
            // function).
            final var actualLength = line.getBytes(StandardCharsets.UTF_8).length + 1 /* newline */;
            if (expectedLength != actualLength) {
                throw new CompressionException("Extended PaxHeader length is invalid. Expected: %d, got: %d".formatted(expectedLength, actualLength));
            }

            final var entry = parts[1].split("=", 2);
            return new ParsedAttribute(entry[0], entry[1]);
        }).collect(Collectors.toUnmodifiableMap(ParsedAttribute::keyword, ParsedAttribute::value));

        // Gather supported attributes.
        return new TapeArchivePaxAttributes(rawAttributes.get(PAX_PATH));
    }

}
