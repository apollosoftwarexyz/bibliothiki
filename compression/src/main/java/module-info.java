/**
 * <h2>compression (bibliothiki)</h2>
 *
 * The compression module.
 *
 * <p>
 * Refer to the {@link xyz.apollosoftware.bibliothiki.compression} package for
 * more details.
 *
 * <p>
 * <a href="https://github.com/apollosoftwarexyz/bibliothiki/tree/main/compression" target="_blank">compression (bibliothiki) - GitHub</a>
 */
module xyz.apollosoftware.bibliothiki.compression {
    requires java.compiler;
    requires static transitive org.jspecify;

    exports xyz.apollosoftware.bibliothiki.compression;
    opens xyz.apollosoftware.bibliothiki.compression;
    opens xyz.apollosoftware.bibliothiki.compression.formats.tar;
    opens xyz.apollosoftware.bibliothiki.compression.formats.zip;
    opens xyz.apollosoftware.bibliothiki.compression.utils;
}
