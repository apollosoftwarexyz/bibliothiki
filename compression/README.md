# compression (bibliothiki)

The compression library handles decompression (and soon, compression) of
archives and streams.

## Security

This library is not yet hardened for use with potentially malicious archive
files. You should screen untrusted files first, implement your own security
measures, or rely on alternative tools for this purpose, for now.

The work needed to harden this library for potentially malicious archive files
is as follows:

1. Permit setting size limits on file entries (prevents (D)DoS attacks).
2. Permit setting quantity limits on entries.
3. Permit setting depth limits on directories.
4. Block absolute file paths.
5. (When links are supported) limit the number of hard links.
6. Allow fine-grained control over formats/lockdown mode that supports only a 
   very simple, limited, subset of all file formats.
7. Set limits on paths/filenames.
