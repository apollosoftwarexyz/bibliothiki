package xyz.apollosoftware.bibliothiki.versioning.schemes.semver;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import xyz.apollosoftware.bibliothiki.versioning.Version;
import xyz.apollosoftware.bibliothiki.versioning.VersioningException;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * A Semantic Versioning (SemVer) version.
 *
 * <p>
 * This is an immutable representation of a SemVer string.
 *
 * @implNote
 * This class has a natural ordering that is inconsistent with
 * equals. Semantic Versioning (SemVer) does not consider build metadata when
 * determining precedence, however it is included naturally for
 * {@link #equals(Object)} to maintain principle of least surprise.
 *
 * <p>
 * Therefore, if you wish to compare Semantic Versions for equality per the
 * SemVer specification, you should use {@link #compareTo(SemanticVersion)}
 *
 * @param major The major version component (for breaking API changes).
 * @param minor The minor version component (for backwards-compatible
 *              functionality).
 * @param patch The patch version component (for backwards-compatible bug
 *              fixes).
 * @param preRelease The list of pre-release identifiers (see
 *                   {@link PreReleaseIdentifier}).
 * @param buildMetadata The list of build metadata identifiers (see
 *                      {@link BuildIdentifier}).
 */
public record SemanticVersion(
    int major,
    int minor,
    int patch,
    List<PreReleaseIdentifier> preRelease,
    List<BuildIdentifier> buildMetadata
) implements Version<SemanticVersion> {

    /**
     * A builder for a {@link SemanticVersion}.
     */
    public static final class Builder {

        private int major;
        private int minor;
        private int patch;
        private final List<PreReleaseIdentifier> preRelease;
        private final List<BuildIdentifier> buildMetadata;

        private Builder() {
            this.preRelease = new ArrayList<>();
            this.buildMetadata = new ArrayList<>();
        }

        private Builder(@NonNull SemanticVersion version) {
            this.major = version.major();
            this.minor = version.minor();
            this.patch = version.patch();
            this.preRelease = new ArrayList<>(version.preRelease());
            this.buildMetadata = new ArrayList<>(version.buildMetadata());
        }

        /**
         * Set the core version components (major, minor and patch) all at once.
         *
         * @param major The major version component.
         * @param minor The minor version component.
         * @param patch The patch version component.
         * @return The builder instance.
         */
        @NonNull
        public Builder version(int major, int minor, int patch) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
            return this;
        }

        /**
         * Set the major version component.
         *
         * @param major The major version component.
         * @return The builder instance.
         */
        @NonNull
        public Builder major(int major) {
            this.major = major;
            return this;
        }

        /**
         * Set the minor version component.
         *
         * @param minor The minor version component.
         * @return The builder instance.
         */
        @NonNull
        public Builder minor(int minor) {
            this.minor = minor;
            return this;
        }

        /**
         * Set the patch version component.
         *
         * @param patch The patch version component.
         * @return The builder instance.
         */
        @NonNull
        public Builder patch(int patch) {
            this.patch = patch;
            return this;
        }

        /**
         * Set the pre-release components.
         *
         * <p>
         * This method will overwrite any existing pre-release identifiers.
         *
         * @param preRelease The pre-release version components.
         * @return The builder instance.
         */
        @NonNull
        public Builder preRelease(@NonNull PreReleaseIdentifier @NonNull ... preRelease) {
            this.preRelease.clear();
            this.preRelease.addAll(Arrays.asList(preRelease));
            return this;
        }

        /**
         * Add to the pre-release components.
         *
         * <p>
         * This method parses the strings into {@link PreReleaseIdentifier}
         * objects for convenience.
         *
         * @param preRelease The pre-release version components to add.
         * @return The builder instance.
         * @see #addPreRelease(PreReleaseIdentifier...)
         */
        @NonNull
        public Builder addPreRelease(@NonNull String @NonNull ... preRelease) {
            this.preRelease.addAll(Arrays.stream(preRelease).map(PreReleaseIdentifier::parse).toList());
            return this;
        }

        /**
         * Add to the pre-release components.
         *
         * @param preRelease The pre-release version components to add.
         * @return The builder instance.
         * @see #addPreRelease(String...)
         */
        @NonNull
        public Builder addPreRelease(@NonNull PreReleaseIdentifier @NonNull ... preRelease) {
            this.preRelease.addAll(Arrays.asList(preRelease));
            return this;
        }

        /**
         * Set the build metadata components.
         *
         * <p>
         * This method will overwrite any existing build metadata components.
         *
         * @param buildMetadata The build metadata components.
         * @return The builder instance.
         */
        @NonNull
        public Builder buildMetadata(@NonNull BuildIdentifier @NonNull ... buildMetadata) {
            this.buildMetadata.clear();
            this.buildMetadata.addAll(Arrays.asList(buildMetadata));
            return this;
        }

        /**
         * Add to the build metadata components.
         *
         * <p>
         * This method parses the strings into {@link BuildIdentifier} objects
         * for convenience.
         *
         * @param buildMetadata The build metadata components to add.
         * @return The builder instance.
         * @see #addBuildMetadata(BuildIdentifier...)
         */
        @NonNull
        public Builder addBuildMetadata(@NonNull String @NonNull ... buildMetadata) {
            this.buildMetadata.addAll(Arrays.stream(buildMetadata).map(BuildIdentifier::new).toList());
            return this;
        }

        /**
         * Add to the build metadata components.
         *
         * @param buildMetadata The build metadata components to add.
         * @return The builder instance.
         * @see #addBuildMetadata(String...)
         */
        @NonNull
        public Builder addBuildMetadata(@NonNull BuildIdentifier @NonNull ... buildMetadata) {
            this.buildMetadata.addAll(Arrays.asList(buildMetadata));
            return this;
        }

        /**
         * Build the {@link SemanticVersion} representation from the properties
         * set on the builder.
         *
         * @return The built {@link SemanticVersion} instance.
         */
        @NonNull
        public SemanticVersion build() {
            return new SemanticVersion(major, minor, patch, preRelease, buildMetadata);
        }

    }

    /**
     * Construct a new {@link Builder} for {@link SemanticVersion}
     * representations.
     *
     * @return The new builder instance.
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Construct a {@link Builder} that has the current version's values set as
     * initial values.
     *
     * @return The new builder instance.
     */
    @NonNull
    public Builder asBuilder() {
        return new Builder(this);
    }

    /**
     * Constructor for semantic version representations.
     *
     * <p>
     * If {@link #preRelease} or {@link #buildMetadata} are null, they are
     * replaced with empty lists. They are also both frozen to ensure they are
     * immutable.
     */
    public SemanticVersion {
        if (major < 0) throw new VersioningException("Major version must be zero or positive.");
        if (minor < 0) throw new VersioningException("Minor version must be zero or positive.");
        if (patch < 0) throw new VersioningException("Patch version must be zero or positive.");
        preRelease = Collections.unmodifiableList(Optional.ofNullable(preRelease).orElseGet(Collections::emptyList));
        buildMetadata = Collections.unmodifiableList(Optional.ofNullable(buildMetadata).orElseGet(Collections::emptyList));

        for (int i = 0; i < preRelease.size(); i++) {
            if (Objects.isNull(preRelease.get(i))) {
                throw new NullPointerException("Pre-release component %d is null".formatted(i));
            }
        }

        for (int i = 0; i < buildMetadata.size(); i++) {
            if (Objects.isNull(buildMetadata.get(i))) {
                throw new NullPointerException("Build metadata component %d is null".formatted(i));
            }
        }
    }

    /**
     * Whether the version is a pre-release version.
     *
     * <p>
     * This is true when there are one, or more, pre-release components in the
     * version string.
     *
     * @return True if, and only if, this is a pre-release version.
     */
    public boolean isPreRelease() {
        return !preRelease.isEmpty();
    }

    /**
     * Whether the version has build metadata.
     *
     * <p>
     * This is true when there are one, or more, build metadata components in
     * the version string.
     *
     * @return True if, and only if, this version string has build metadata.
     */
    public boolean hasBuildMetadata() {
        return !buildMetadata.isEmpty();
    }

    @Override
    @NonNull
    public List<PreReleaseIdentifier> preRelease() {
        return preRelease;
    }

    @Override
    @NonNull
    public List<BuildIdentifier> buildMetadata() {
        return buildMetadata;
    }

    @Override
    public int compareTo(@NonNull final SemanticVersion other) {
        Objects.requireNonNull(other, "The version to compare to must not be null");

        // Compare core version components.
        if (major() != other.major()) return Integer.compareUnsigned(major(), other.major());
        if (minor() != other.minor()) return Integer.compareUnsigned(minor(), other.minor());
        if (patch() != other.patch()) return Integer.compareUnsigned(patch(), other.patch());

        // A version with no pre-release fields has higher precedence than a
        // version that has pre-release fields.
        final var hasPreReleaseComparison = Boolean.compare(!isPreRelease(), !other.isPreRelease());
        if (hasPreReleaseComparison != 0) return hasPreReleaseComparison;

        // Compare pre-release version components.
        for (int i = 0; i < Math.min(preRelease().size(), other.preRelease().size()); i++) {
            // If any of the common pre-release versions are different, return
            // the comparison.
            final var preReleaseComparison = preRelease().get(i).compareTo(other.preRelease().get(i));
            if (preReleaseComparison != 0) return preReleaseComparison;
        }

        // A larger set of pre-release fields as a higher precedence than a
        // smaller set (if the preceding are equal).
        return Integer.compareUnsigned(preRelease.size(), other.preRelease().size());
    }

    /**
     * Types of Semantic Versioning (SemVer) increments.
     */
    public enum IncrementType {
        /**
         * Increment the major version.
         *
         * <p>
         * This should be incremented for incompatible (breaking) API changes.
         */
        MAJOR,

        /**
         * Increment the minor version.
         *
         * <p>
         * This should be incremented for functionality that has been added in
         * a backwards-compatible manner.
         */
        MINOR,

        /**
         * Increment the patch version.
         *
         * <p>
         * This should be incremented for backwards-compatible bug fixes.
         */
        PATCH,
    }

    /**
     * Increments the version by performing an increment of the specified
     * {@link IncrementType}.
     *
     * <p>
     * This method increments the version to the next non-prerelease version.
     * To increment a pre-release version, use
     * {@link #withIncrementedPreRelease(String)} or
     * {@link #withIncrementedPreRelease(String, int)}.
     *
     * @param incrementType The type of increment to perform.
     * @return A new {@link SemanticVersion}, derived from the current one by
     *         performing the specified increment.
     * @see #withIncrementedPreRelease(String)
     * @see #withIncrementedPreRelease(String, int)
     */
    @NonNull
    public SemanticVersion withIncremented(@NonNull IncrementType incrementType) {
        Objects.requireNonNull(incrementType, "The increment type must be specified but was null");

        return switch (incrementType) {
            case MAJOR -> asBuilder().major(major() + 1).minor(0).patch(0).preRelease().build();
            case MINOR -> asBuilder().minor(minor() + 1).patch(0).preRelease().build();
            case PATCH -> asBuilder().patch(patch() + 1).preRelease().build();
        };
    }

    /**
     * Increments the specified pre-release version.
     *
     * <p>
     * The behavior is identical to {@link #withIncrementedPreRelease(String, int)}
     * except the {@code initialValue} is defined as one for convenience.
     *
     * @param name The name of the pre-release version track.
     * @return The incremented version.
     * @see #withIncremented(IncrementType) 
     * @see #withIncrementedPreRelease(String, int)
     */
    @NonNull
    public SemanticVersion withIncrementedPreRelease(String name) {
        return withIncrementedPreRelease(name, 1);
    }

    /**
     * Increments the specified pre-release version.
     *
     * <p>
     * Unlike most other functions in the versioning library, this method is
     * fairly opinionated and describes a specific (but interoperable) approach
     * to versioning with pre-release versions. If you need more specific
     * behavior, it may be preferable to implement your own increment method
     * using the {@link SemanticVersion} API.
     *
     * <p>
     * If a pre-release version component is present in the string that matches
     * the given name and there is a numeric component immediately following it,
     * the numeric component is incremented. Otherwise, to avoid modifying
     * unintended parts of the version string, the name and {@code initialValue}
     * are instead appended to the new version string. If there are multiple
     * occurrences, the first one is matched and incremented.
     *
     * <p>
     * To avoid errors, the {@code name} must not be a numeric identifier when
     * parsed as a pre-release identifier. If it is, a
     * {@link VersioningException} is thrown.
     *
     * @param name The name of the pre-release version track (must not be
     *             numeric).
     * @param initialValue The initial value to use (if the pre-release track is
     *                     not present in the version string). The initial
     *                     version must be positive.
     * @return The incremented version.
     * @throws VersioningException If the specified name is numeric or if the
     *                             initial value is negative.
     * @see #withIncremented(IncrementType)
     * @see #withIncrementedPreRelease(String)
     */
    @NonNull
    public SemanticVersion withIncrementedPreRelease(String name, int initialValue) {
        if (initialValue < 0) {
            throw new VersioningException("Pre-release version (%d) must be greater than or equal to zero.".formatted(initialValue));
        }

        int nameIndex = -1;

        for (int i = 0; i < preRelease().size(); i++) {
            // If a pre-release component matches the name...
            if (!preRelease().get(i).isNumeric() &&
                preRelease().get(i).rawValue().equals(name) &&
                // ...and there is a next element that is numeric...
                preRelease().size() > i + 1 &&
                preRelease().get(i + 1).isNumeric()) {
                // ...store the index of the element.
                nameIndex = i;
            }
        }

        // If there were no occurrences, add a new one.
        if (nameIndex == -1) {
            return asBuilder().addPreRelease(name, Integer.toString(initialValue)).build();
        } else {
            final List<PreReleaseIdentifier> preRelease = new ArrayList<>(preRelease());
            preRelease.set(nameIndex + 1, PreReleaseIdentifier.preRelease(Integer.toString(Objects.requireNonNull(preRelease.get(nameIndex + 1).numericValue()) + 1)));
            return asBuilder().preRelease(preRelease.toArray(PreReleaseIdentifier[]::new)).build();
        }
    }

    @Override
    @NonNull
    public String toString() {
        final var result = new StringBuilder("%d.%d.%d".formatted(major(), minor(), patch()));

        // Add pre-release segments.
        if (isPreRelease()) {
            result.append('-');
            for (int i = 0; i < preRelease().size(); i++) {
                if (i != 0) result.append('.');
                result.append(preRelease().get(i).rawValue());
            }
        }

        // Add build metadata segments.
        if (hasBuildMetadata()) {
            result.append('+');
            for (int i = 0; i < buildMetadata().size(); i++) {
                if (i != 0) result.append('.');
                result.append(buildMetadata().get(i).value());
            }
        }

        return result.toString();
    }

    /**
     * A pre-release build identifier.
     *
     * <p>
     * Identifiers are parsed as numeric identifiers first and, if not numeric,
     * validated as non-numeric identifiers using the regular expression
     * provided by the Semantic Versioning (SemVer) specification.
     *
     * @see PreReleaseIdentifier#parse(String)
     */
    public static class PreReleaseIdentifier implements Comparable<PreReleaseIdentifier> {

        /**
         * A predicate that checks the validity of pre-release identifiers.
         */
        private static final Predicate<String> IS_VALID =
                Pattern.compile("^((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*)$").asMatchPredicate();

        /**
         * A static sugar for {@link #parse(String)}.
         *
         * @param value The value to parse.
         * @return The parsed {@link PreReleaseIdentifier}.
         * @see PreReleaseIdentifier#parse(String)
         */
        @NonNull
        public static PreReleaseIdentifier preRelease(@NonNull String value) {
            return parse(value);
        }

        /**
         * Parse the given value as a {@link PreReleaseIdentifier}.
         *
         * <p>
         * If the value is empty, it will be rejected upon construction as this
         * is forbidden by the Semantic Versioning (SemVer) specification.
         *
         * <p>
         * If the value is numeric, it is parsed as a numeric identifier. If the
         * identifier starts with a leading zero, but would otherwise be
         * numeric, a {@link VersioningException} is thrown.
         *
         * <p>
         * If the value is not numeric, it is parsed as a non-numeric identifier
         * and is then validated against the prescribed regular expression (from
         * the specification) upon construction. If it is not valid, per the
         * regular expression, a {@link VersioningException} is thrown.
         *
         * @param value The value to parse.
         * @return The parsed {@link PreReleaseIdentifier}.
         * @throws VersioningException If the identifier is not valid.
         */
        @NonNull
        public static PreReleaseIdentifier parse(@NonNull String value) {
            try {
                final int numeric = Integer.parseInt(value);

                // Ensure there are no leading zeroes.
                if (numeric != 0 && value.startsWith("0")) {
                    throw new VersioningException("Invalid numeric identifier (must not start with leading zero): '%s'".formatted(value));
                }

                return new PreReleaseIdentifier(value, numeric);
            } catch (final NumberFormatException ignored) {
            }

            return new PreReleaseIdentifier(value, null);
        }

        @NonNull
        private final String rawValue;

        @Nullable
        private final Integer numericValue;

        private PreReleaseIdentifier(@NonNull final String rawValue, @Nullable Integer numericValue) {
            if (rawValue.isEmpty()) {
                throw new VersioningException("Pre-release identifiers must not be empty");
            }

            // Validate against the pattern.
            if (!IS_VALID.test(rawValue)) {
                throw new VersioningException("Invalid pre-release identifier: '%s'".formatted(rawValue));
            }

            this.rawValue = rawValue;
            this.numericValue = numericValue;
        }

        /**
         * Get the raw string value for the identifier.
         *
         * @return The raw string value.
         */
        @NonNull
        public String rawValue() {
            return this.rawValue;
        }

        /**
         * Get the numeric value, if there is one.
         *
         * @return The numeric value, or null if one is not set.
         */
        @Nullable
        public Integer numericValue() {
            return this.numericValue;
        }

        /**
         * Whether the identifier was parsed as a numeric value.
         *
         * <p>
         * This is true when the {@link #numericValue()} is not null.
         *
         * @return True if, and only if, the identifier has a numeric value.
         */
        public boolean isNumeric() {
            return this.numericValue() != null;
        }

        @Override
        public String toString() {
            return rawValue();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            PreReleaseIdentifier that = (PreReleaseIdentifier) o;
            return Objects.equals(rawValue, that.rawValue) && Objects.equals(numericValue, that.numericValue);
        }

        @Override
        public int hashCode() {
            return Objects.hash(rawValue, numericValue);
        }

        @Override
        public int compareTo(@NonNull PreReleaseIdentifier other) {
            Objects.requireNonNull(other, "The pre-release identifier to compare to must not be null");

            if (!isNumeric() && !other.isNumeric()) {
                // Compare lexicographically.
                return this.rawValue().compareTo(other.rawValue());
            }

            if (isNumeric() && other.isNumeric()) {
                // Compare numerically.
                //noinspection DataFlowIssue - covered by comparison
                return this.numericValue().compareTo(other.numericValue());
            }

            // Otherwise, whichever one is alphabetic has higher precedence.
            return Boolean.compare(!isNumeric(), !other.isNumeric());
        }

    }

    /**
     * A build metadata identifier.
     *
     * <p>
     * Identifiers are simply retained as a raw string but must only contain
     * alphanumeric characters and hyphens.
     *
     * <p>
     * Per the Semantic Versioning (SemVer) specification, build metadata
     * identifiers must not be empty.
     *
     * @param value The build identifier string value.
     */
    public record BuildIdentifier(String value) {

        /**
         * A predicate that checks the validity of build metadata identifiers.
         */
        private static final Predicate<String> IS_VALID =
                Pattern.compile("^[0-9a-zA-Z-]+$").asMatchPredicate();

        /**
         * Construct the build metadata identifier.
         *
         * <p>
         * Build identifiers must not empty so this constructor enforces that by
         * ensuring the given value is non-empty.
         *
         * <p>
         * This constructor also enforces the character set requirements for the
         * build identifier.
         */
        public BuildIdentifier {
            if (value.isEmpty()) {
                throw new VersioningException("Build metadata identifiers must not be empty");
            }

            if (!IS_VALID.test(value)) {
                throw new VersioningException("Invalid build metadata identifier: %s".formatted(value));
            }
        }

        /**
         * A static sugar for {@link BuildIdentifier#BuildIdentifier(String)}.
         *
         * @param value The build identifier value.
         * @return The created build identifier.
         */
        @NonNull
        public static BuildIdentifier buildMetadata(String value) {
            return new BuildIdentifier(value);
        }

        @NonNull
        @Override
        public String toString() {
            return value;
        }
    }

}
