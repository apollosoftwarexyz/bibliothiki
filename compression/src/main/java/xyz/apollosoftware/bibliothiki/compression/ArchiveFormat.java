package xyz.apollosoftware.bibliothiki.compression;

import org.jspecify.annotations.NonNull;

/**
 * Archive formats supported by Bibliothiki Compression.
 */
public enum ArchiveFormat {

    /**
     * The (T)ape (AR)chive (TAR) file format.
     *
     * <p>
     * <a href="https://en.wikipedia.org/wiki/Tar_(computing)" target="_blank">tar (Wikipedia)</a>
     */
    TAR(".tar"),

    /**
     * The ZIP archive file format.
     *
     * <p>
     * Java's internal ZIP library is used to process ZIP archives.
     *
     * <p>
     * <a href="https://en.wikipedia.org/wiki/ZIP_(file_format)" target="_blank">ZIP (Wikipedia)</a>
     */
    ZIP(".zip");

    @NonNull
    private final String extensionSuffix;

    ArchiveFormat(@NonNull final String extensionSuffix) {
        this.extensionSuffix = extensionSuffix;
    }

    /**
     * The canonical file extension for this file format.
     *
     * <p>
     * This can be used as a detection mechanism, however inspecting the file is
     * preferred.
     *
     * @return The canonical file format extension.
     */
    @NonNull
    public String getExtensionSuffix() {
        return this.extensionSuffix;
    }

}
