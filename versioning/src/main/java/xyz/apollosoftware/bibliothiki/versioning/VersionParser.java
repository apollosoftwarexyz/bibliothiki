package xyz.apollosoftware.bibliothiki.versioning;

import org.jspecify.annotations.NonNull;

/**
 * A parser for a version scheme.
 *
 * @param <T> The representation that this parser produces.
 */
public interface VersionParser<T extends Version<T>> {

    /**
     * Parse the given version string.
     *
     * @param version The string to parse.
     * @return The parsed version representation.
     * @throws IllegalArgumentException If the version number is not supported.
     */
    @NonNull
    T parse(String version);

}
