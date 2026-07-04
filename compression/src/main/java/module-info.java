/**
 * <h2>compression (bibliothiki)</h2>
 *
 * The compression module.
 *
 * <p>
 * Refer to the {@link xyz.apollosoftware.bibliothiki.compression} package for
 * more details.
 */
module xyz.apollosoftware.bibliothiki.compression {
    requires java.compiler;
    requires static transitive org.jspecify;

    exports xyz.apollosoftware.bibliothiki.compression;
}
