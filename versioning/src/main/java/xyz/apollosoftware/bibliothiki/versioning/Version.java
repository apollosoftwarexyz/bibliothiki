package xyz.apollosoftware.bibliothiki.versioning;

import org.jspecify.annotations.NonNull;

/**
 * A parsed representation of a version.
 *
 * @param <T> The version type (denotes which types this version can be compared
 *            with).
 */
public interface Version<T extends Version<T>> extends Comparable<T> {

    /**
     * Compares this version with the specified version.
     *
     * <p>
     * The comparison adheres to the {@link Comparable} specification and
     * semantics.
     *
     * @param other the object to be compared.
     * @return a negative integer if this version is less than the other
     *         version, zero if the versions are equal or a positive integer if
     *         this version is greater than the other version.
     * @throws NullPointerException If the other version is null.
     */
    @Override
    int compareTo(@NonNull T other);

    /**
     * A convenience method that invokes {@link #compareTo(T)} and checks
     * whether the result is less than zero.
     *
     * @param other The other version.
     * @return True if, and only if, the other version is less than this
     *         version.
     */
    default boolean isLessThan(@NonNull T other) {
        return compareTo(other) < 0;
    }

    /**
     * A convenience method that invokes {@link #compareTo(T)} and checks
     * whether the result is equal to zero.
     *
     * <p>
     * This is <b>NOT</b> necessarily the same as {@link #equals(Object)}.
     * This method follows the semantics of the version specification rather
     * than of Java objects (for the latter, use {@link #equals(Object)}).
     * The implementation notes of the concrete version type should provide more
     * specific details.
     *
     * @param other The other version.
     * @return True if, and only if, the other version is considered equivalent
     *         per the version's specification.
     * @see #equals(Object) 
     */
    default boolean isEquivalent(@NonNull T other) {
        return compareTo(other) == 0;
    }

    /**
     * A convenience method that invokes {@link #compareTo(T)} and checks
     * whether the result is greater than zero.
     *
     * @param other The other version.
     * @return True if, and only if, the other version is greater than this
     *         version.
     */
    default boolean isGreaterThan(@NonNull T other) {
        return compareTo(other) > 0;
    }

    /**
     * Indicates whether the other version object is equal to this one.
     *
     * <p>
     * This follows the semantics of {@link Object#equals(Object)} and is
     * intended for comparing version object values (the representation rather
     * than the semantics) with each other.
     *
     * <p>
     * This method does not necessarily follow the ordering or semantics of
     * equality of the version specification. For that use case, instead use
     * {@link #isEquivalent(Version)}.
     *
     * <p>
     * For example, Semantic Versioning (SemVer) stipulates that build metadata
     * not be considered in precedence, but it is still part of the version
     * string, so for versions that differ only by build metadata,
     * {@code equals} would return false and {@link #isEquivalent(Version)}
     * would return true.
     *
     * @param other The other version.
     * @return True if, and only if, the other version's value is identical in
     *         that it would produce the exact same representation.
     * @see #isEquivalent(Version)
     */
    @Override
    boolean equals(Object other);

    /**
     * Renders the version as either its original string, or a new equivalent
     * string.
     *
     * @return The string form of the version.
     */
    @Override
    @NonNull
    String toString();

}
