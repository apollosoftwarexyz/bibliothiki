package xyz.apollosoftware.bibliothiki.compression.zip;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import xyz.apollosoftware.bibliothiki.compression.ArchiveEntry;
import xyz.apollosoftware.bibliothiki.compression.ArchiveInputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class TestZipArchiveInputStream {

    private static final int TEST_CASE_01_ENTRIES = 8;

    @Test
    void testDetectStream() throws IOException {
        final Map<String, Boolean> files = Set.of(
                // Check for file.
                "go/bin/go",

                // Check for file with extended characters.
                "go/test/fixedbugs/issue27836.dir/Þfoo.go"
        ).stream().collect(Collectors.toMap(Function.identity(), entry -> false));

        try (final var stream = getResourceAsStream("/test_case_01.zip");
             final var archiveStream = assertDoesNotThrow(() -> ArchiveInputStream.detect(stream))) {

            ArchiveEntry entry;
            int entries = 0;

            while ((entry = assertDoesNotThrow(archiveStream::getNextEntry)) != null) {
                if (files.containsKey(entry.name())) {
                    files.put(entry.name(), true);
                }

                entries++;
            }

            assertEquals(TEST_CASE_01_ENTRIES, entries);
        }

        files.forEach((file, value) -> assertTrue(files.get(file), "File was not visited: %s".formatted(file)));
    }

    @Test
    void testGetFileContents() throws IOException {
        boolean foundTestFile = false;

        try (final var stream = getResourceAsStream("/test_case_01.zip");
             final var archiveStream = assertDoesNotThrow(() -> ArchiveInputStream.detect(stream))) {

            ArchiveEntry entry;
            while ((entry = assertDoesNotThrow(archiveStream::getNextEntry)) != null) {
                if (entry.name().equals("go/bin/go")) {
                    final var outputStream = new ByteArrayOutputStream();
                    assertDoesNotThrow(() -> archiveStream.writeCurrentEntryTo(outputStream));
                    assertEquals("test file\n", outputStream.toString(StandardCharsets.UTF_8));
                    foundTestFile = true;
                }
            }

        }

        assertTrue(foundTestFile);
    }

    private static InputStream getResourceAsStream(@NonNull final String name) {
        return TestZipArchiveInputStream.class.getResourceAsStream(name);
    }

}
