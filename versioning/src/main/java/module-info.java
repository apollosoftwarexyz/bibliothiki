/**
 * <h2>versioning (bibliothiki)</h2>
 *
 * The versioning module.
 *
 * <p>
 * Refer to the {@link xyz.apollosoftware.bibliothiki.versioning} package for
 * more details.
 *
 * <p>
 * <a href="https://github.com/apollosoftwarexyz/bibliothiki/tree/main/versioning" target="_blank">versioning (bibliothiki) - GitHub</a>
 */
module xyz.apollosoftware.bibliothiki.versioning {
    requires static transitive org.jspecify;

    exports xyz.apollosoftware.bibliothiki.versioning;
    exports xyz.apollosoftware.bibliothiki.versioning.schemes.semver;
}
