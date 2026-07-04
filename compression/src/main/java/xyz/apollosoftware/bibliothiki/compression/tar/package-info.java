/**
 * Support for the <a href="https://en.wikipedia.org/wiki/Tar_(computing)" target="_blank">(T)ape (AR)chive</a>
 * (TAR) file format.
 *
 * <p>
 * Standalone Tape Archives are not necessarily compressed (i.e., a file ending
 * with {@code .tar}) is normally <b>not</b> compressed. However, tar files are
 * ubiquitously used as a means of compressing files by encapsulating them in
 * a compression stream (such as GZIP {@code .gz} or XZ/LZMA {@code .xz}).
 *
 * <p>
 * This package supports raw tar files and the compression library supports tar
 * files wrapped in certain compression streams.
 *
 * @see xyz.apollosoftware.bibliothiki.compression.tar.TapeArchiveInputStream
 */
package xyz.apollosoftware.bibliothiki.compression.tar;
