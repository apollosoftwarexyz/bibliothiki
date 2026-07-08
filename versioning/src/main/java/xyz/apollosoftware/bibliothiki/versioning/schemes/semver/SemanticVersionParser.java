package xyz.apollosoftware.bibliothiki.versioning.schemes.semver;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import xyz.apollosoftware.bibliothiki.versioning.VersionParser;
import xyz.apollosoftware.bibliothiki.versioning.VersioningException;
import xyz.apollosoftware.bibliothiki.versioning.schemes.semver.SemanticVersion.BuildIdentifier;
import xyz.apollosoftware.bibliothiki.versioning.schemes.semver.SemanticVersion.PreReleaseIdentifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A parser for a {@link SemanticVersion}.
 *
 * <p>
 * This is implemented as a linear (LR) bottom-up parser, similarly to a regular
 * expression however implemented manually for more flexibility in error
 * handling.
 *
 * <p>
 * This implementation delegates to other parsers defined for pre-release
 * ({@link PreReleaseIdentifier}) and build identifiers
 * ({@link BuildIdentifier}) in the constructors of those respective classes for
 * consistency.
 */
public class SemanticVersionParser implements VersionParser<SemanticVersion> {

    /**
     * The version components in the core part of the version string.
     */
    private static final List<String> CORE_PARTS = List.of("major", "minor", "patch");

    /**
     * Construct a {@link SemanticVersionParser}.
     */
    public SemanticVersionParser() {
    }

    /**
     * The parser state stores parsed semantic version components.
     */
    private static class ParserState {

        /**
         * The major, minor and patch versions.
         */
        private final int[] core;

        private final List<PreReleaseIdentifier> preReleaseIdentifiers;

        private final List<BuildIdentifier> buildIdentifiers;

        public ParserState(int coreParts) {
            this.core = new int[coreParts];
            this.preReleaseIdentifiers = new ArrayList<>();
            this.buildIdentifiers = new ArrayList<>();
        }

    }

    /**
     * Try to parse the given version string as a {@link SemanticVersion}.
     *
     * @param version The string to parse.
     * @return The parsed {@link SemanticVersion}.
     */
    @NonNull
    @Override
    public SemanticVersion parse(@NonNull String version) {
        Objects.requireNonNull(version, "The version string must not be null");

        if (version.isEmpty()) {
            throw new VersioningException("The version string must not be empty");
        }

        final var state = new ParserState(CORE_PARTS.size());
        int offset = 0;

        // Parse the version core.
        try {
            offset = parseVersionCore(state, version, offset, state.core.length);
        } catch (NumberFormatException ex) {
            // If any of the versions are invalid, reject the format.
            throw new VersioningException("Version core part invalid", ex);
        }

        while (offset < version.length()) {
            final var nextSegment = version.charAt(offset);
            switch (nextSegment) {
                case '+' -> offset = parseBuildMetadata(state, version, offset + 1);
                case '-' -> offset = parsePreReleases(state, version, offset + 1);
                default ->
                        throw new VersioningException("Unexpected character following version core. Got '%s', expecting '+' or '-'".formatted(nextSegment));
            }
        }

        return new SemanticVersion(state.core[0], state.core[1], state.core[2], state.preReleaseIdentifiers, state.buildIdentifiers);
    }

    /**
     * Parse the core parts of the semantic version.
     *
     * @param state The parser state.
     * @param version The version string to parse.
     * @param offset The current offset in the version string.
     * @param parts The number of core parts to parse.
     * @return The new offset in the version string.
     */
    private static int parseVersionCore(@NonNull ParserState state, @NonNull String version, int offset, int parts) {
        final int versionLength = version.length();
        // String[] is used instead of StringBuilder as in testing it seemed to
        // be substantially faster (2x) to do string appending (perhaps due to
        // reduced allocations as a result of the small string sizes).
        final String[] coreParts = new String[parts];

        // Loop over the version string to extract the version parts.
        int part = 0;
        while (offset < versionLength && part < coreParts.length) {
            // Read the character at the current offset.
            final char c = version.charAt(offset);

            if (!Character.isDigit(c) && c != '.') {
                // The end of the core is denoted by either + or -.
                break;
            }

            if (c == '.') {
                // The core parts are delimited by '.'.
                part++;
            } else {
                // Append the character to the current part.
                coreParts[part] = (coreParts[part] == null ? "" : coreParts[part]) + c;
            }

            offset++;
        }

        // Now parse each of the parts.
        for (int i = 0; i < coreParts.length; i++) {
            final var value = coreParts[i];

            // Validate the part for a more friendly error message.
            if (value == null) {
                throw new VersioningException("Invalid version string, %s component missing".formatted(CORE_PARTS.get(i)));
            }

            try {
                state.core[i] = Integer.parseInt(value);
            } catch (NumberFormatException ex) {
                throw new VersioningException("Invalid version string, %s component is not a valid number: %s".formatted(CORE_PARTS.get(i), value));
            }

            if (state.core[i] != 0 && value.startsWith("0")) {
                throw new VersioningException("Invalid version string, %s component must not start with a leading zero: %s".formatted(CORE_PARTS.get(i), value));
            }
        }

        return offset;
    }

    private static int parsePreReleases(@NonNull ParserState state, @NonNull String version, int offset) {
        return parseComponent(
            "pre-release",
            '+',
            identifier -> state.preReleaseIdentifiers.add(PreReleaseIdentifier.parse(identifier)),
            version,
            offset
        );
    }

    private static int parseBuildMetadata(@NonNull ParserState state, @NonNull String version, int offset) {
        return parseComponent(
            "build metadata",
            null,
            identifier -> state.buildIdentifiers.add(new BuildIdentifier(identifier)),
            version,
            offset
        );
    }

    private static int parseComponent(@NonNull String componentName, @Nullable Character nextSegmentDelimiter, @NonNull Consumer<String> onComponent, @NonNull String version, int offset) {
        StringBuilder builder = new StringBuilder();

        while (offset < version.length()) {
            final char c = version.charAt(offset);
            boolean isValidIdentifierChar =
                    (c >= '0' && c <= '9') ||
                    (c >= 'A' && c <= 'Z') ||
                    (c >= 'a' && c <= 'z') ||
                    c == '-';

            boolean isEndOfVersionString = offset == version.length() - 1;
            if (isEndOfVersionString) {
                // If it's the end of the string, add the character immediately.
                builder.append(c);
            }

            boolean isDelimiter = nextSegmentDelimiter != null && c == nextSegmentDelimiter;

            if (isEndOfVersionString || isDelimiter || c == '.') {
                onComponent.accept(builder.toString());
                builder = new StringBuilder();

                // If it's '+' or the end of the string, break out of the loop
                // to continue processing other components.
                if (isDelimiter) {
                    break;
                }

                // Otherwise, increment the counter to continue processing the
                // pre-releases component.
                offset++;
                continue;
            }

            if (!isValidIdentifierChar) {
                throw new VersioningException("Found unexpected character in %s identifier: '%s'".formatted(componentName, c));
            }

            builder.append(c);
            offset++;
        }

        return offset;
    }

}
