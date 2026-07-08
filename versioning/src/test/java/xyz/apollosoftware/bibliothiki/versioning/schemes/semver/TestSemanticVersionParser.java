package xyz.apollosoftware.bibliothiki.versioning.schemes.semver;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import xyz.apollosoftware.bibliothiki.versioning.VersioningException;

import static org.junit.jupiter.api.Assertions.*;
import static xyz.apollosoftware.bibliothiki.versioning.schemes.semver.SemanticVersion.BuildIdentifier.buildMetadata;
import static xyz.apollosoftware.bibliothiki.versioning.schemes.semver.SemanticVersion.PreReleaseIdentifier.preRelease;

class TestSemanticVersionParser {

    private SemanticVersionParser parser;

    @BeforeEach
    void setUp() {
        this.parser = new SemanticVersionParser();
    }

    @Test
    @DisplayName("should successfully parse basic semantic version strings")
    void testParseBasicSemanticVersions() {
        assertEquals(SemanticVersion.builder().version(0, 0, 1).build(), assertDoesNotThrow(() -> parser.parse("0.0.1")));
        assertEquals(SemanticVersion.builder().version(0, 1, 0).build(), assertDoesNotThrow(() -> parser.parse("0.1.0")));
        assertEquals(SemanticVersion.builder().version(1, 0, 0).build(), assertDoesNotThrow(() -> parser.parse("1.0.0")));
        assertEquals(SemanticVersion.builder().version(1, 2, 3).build(), assertDoesNotThrow(() -> parser.parse("1.2.3")));

        assertEquals(SemanticVersion.builder().version(12, 0, 0).build(), assertDoesNotThrow(() -> parser.parse("12.0.0")));
        assertEquals(SemanticVersion.builder().version(Integer.MAX_VALUE, 0, 0).build(), assertDoesNotThrow(() -> parser.parse("2147483647.0.0")));
        assertEquals(SemanticVersion.builder().version(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE).build(), assertDoesNotThrow(() -> parser.parse("2147483647.2147483647.2147483647")));
    }

    @Test
    @DisplayName("should correctly identify when a version string is a pre-release")
    void testIsPreRelease() {
        var parsed = assertDoesNotThrow(() -> parser.parse("0.0.1"));
        assertFalse(parsed.isPreRelease());

        parsed = assertDoesNotThrow(() -> parser.parse("0.0.1-dev.1"));
        assertTrue(parsed.isPreRelease());
    }

    @Test
    @DisplayName("should support parsing pre-release version components")
    void testParsePreReleaseSemanticVersions() {
        final var parsed = assertDoesNotThrow(() -> parser.parse("0.0.1-dev.1"));
        assertEquals(
            SemanticVersion.builder().version(0, 0, 1).addPreRelease(preRelease("dev"), preRelease("1")).build(),
            parsed
        );

        assertEquals(2, parsed.preRelease().size());

        assertEquals("dev", parsed.preRelease().get(0).rawValue());
        assertFalse(parsed.preRelease().get(0).isNumeric());
        assertNull(parsed.preRelease().get(0).numericValue());

        assertEquals("1", parsed.preRelease().get(1).rawValue());
        assertTrue(parsed.preRelease().get(1).isNumeric());
        assertEquals(1, parsed.preRelease().get(1).numericValue());

        // ...and test individually
        assertEquals(
            SemanticVersion.builder().version(0, 0, 1).addPreRelease(preRelease("dev")).build(),
            assertDoesNotThrow(() -> parser.parse("0.0.1-dev"))
        );

        assertEquals(
            SemanticVersion.builder().version(0, 0, 1).addPreRelease(preRelease("1")).build(),
            assertDoesNotThrow(() -> parser.parse("0.0.1-1"))
        );
    }

    @Test
    @DisplayName("should support parsing build version components")
    void testParseBuildSemanticVersions() {
        assertEquals(
            SemanticVersion.builder().version(1, 0, 0).addBuildMetadata(buildMetadata("001")).build(),
            assertDoesNotThrow(() -> parser.parse("1.0.0+001"))
        );

        assertEquals(
            SemanticVersion.builder().version(1, 0, 0).addBuildMetadata(buildMetadata("21AF26D3----117B344092BD")).build(),
            assertDoesNotThrow(() -> parser.parse("1.0.0+21AF26D3----117B344092BD"))
        );
    }

    @Test
    @DisplayName("should support parsing complex semantic version strings")
    void testParseComplexSemanticVersions() {

        assertEquals(
            SemanticVersion.builder()
                .version(1, 0, 0)
                .addPreRelease(preRelease("beta"), preRelease("1"))
                .addBuildMetadata(buildMetadata("exp"), buildMetadata("sha"), buildMetadata("5114f85"))
                .build(),
            assertDoesNotThrow(() -> parser.parse("1.0.0-beta.1+exp.sha.5114f85"))
        );

    }

    @ParameterizedTest
    @ValueSource(strings = {
        "0.0.4",
        "1.2.3",
        "10.20.30",
        "1.1.2-prerelease+meta",
        "1.1.2+meta",
        "1.1.2+meta-valid",
        "1.0.0-alpha",
        "1.0.0-beta",
        "1.0.0-alpha.beta",
        "1.0.0-alpha.beta.1",
        "1.0.0-alpha.1",
        "1.0.0-alpha0.valid",
        "1.0.0-alpha.0valid",
        "1.0.0-alpha-a.b-c-somethinglong+build.1-aef.1-its-okay",
        "1.0.0-rc.1+build.1",
        "2.0.0-rc.1+build.123",
        "1.2.3-beta",
        "10.2.3-DEV-SNAPSHOT",
        "1.2.3-SNAPSHOT-123",
        "1.0.0",
        "2.0.0",
        "1.1.7",
        "2.0.0+build.1848",
        "2.0.1-alpha.1227",
        "1.0.0-alpha+beta",
        "1.2.3----RC-SNAPSHOT.12.9.1--.12+788",
        "1.2.3----R-S.12.9.1--.12+meta",
        "1.2.3----RC-SNAPSHOT.12.9.1--.12",
        "1.0.0+0.build.1-rc.10000aaa-kk-0.1",
        "1.0.0-0A.is.legal",

        // Excluded due to integer limit:
        // "99999999999999999999999.999999999999999999.99999999999999999",
    })
    @DisplayName("should recognize all valid sample strings")
    void testValidExample(@NonNull String version) {
        assertDoesNotThrow(() -> parser.parse(version));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "1",
        "1.2",
        "1.2.3-0123",
        "1.2.3-0123.0123",
        "1.1.2+.123",
        "+invalid",
        "-invalid",
        "-invalid+invalid",
        "-invalid.01",
        "alpha",
        "alpha.beta",
        "alpha.beta.1",
        "alpha.1",
        "alpha+beta",
        "alpha_beta",
        "alpha.",
        "alpha..",
        "beta",
        "1.0.0-alpha_beta",
        "-alpha.",
        "1.0.0-alpha..",
        "1.0.0-alpha..1",
        "1.0.0-alpha...1",
        "1.0.0-alpha....1",
        "1.0.0-alpha.....1",
        "1.0.0-alpha......1",
        "1.0.0-alpha.......1",
        "01.1.1",
        "1.01.1",
        "1.1.01",
        "1.2",
        "1.2.3.DEV",
        "1.2-SNAPSHOT",
        "1.2.31.2.3----RC-SNAPSHOT.12.09.1--..12+788",
        "1.2-RC-SNAPSHOT",
        "-1.0.3-gamma+b7718",
        "+justmeta",
        "9.8.7+meta+meta",
        "9.8.7-whatever+meta+meta",
        "99999999999999999999999.999999999999999999.99999999999999999----RC-SNAPSHOT.12.09.1--------------------------------..12",
    })
    @DisplayName("should reject all invalid sample strings")
    void testInvalidExample(@NonNull String version) {
        assertThrowsExactly(VersioningException.class, () -> parser.parse(version));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "1.2.3.4",
        "1.2.3.4.5",
        "1.2.3.4.foo",
        "1.2.3+foo+foo",
        "1.2.3-£",
        "1.2.3-",
        "1.2.3+",
        "1.2.3-+",
        "1.2.3££",
        "1.2.3+££",
        "1.2.3-alpha.",
        "1.2.3@",
        "1.2.3-alpha@",
        "1.2.3 ",
        "1.2.3-alpha ",
    })
    @DisplayName("should reject additional known-bad strings")
    void testAdditionalInvalidExamples(@NonNull String version) {
        assertThrowsExactly(VersioningException.class, () -> parser.parse(version));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "0.0.4",
        "1.2.3",
        "10.20.30",
        "1.1.2-prerelease+meta",
        "1.1.2+meta",
        "1.1.2+meta-valid",
        "1.0.0-alpha",
        "1.0.0-beta",
        "1.0.0-alpha.beta",
        "1.0.0-alpha.beta.1",
        "1.0.0-alpha.1",
        "1.0.0-alpha0.valid",
        "1.0.0-alpha.0valid",
        "1.0.0-alpha-a.b-c-somethinglong+build.1-aef.1-its-okay",
        "1.0.0-rc.1+build.1",
        "2.0.0-rc.1+build.123",
        "1.2.3-beta",
        "10.2.3-DEV-SNAPSHOT",
        "1.2.3-SNAPSHOT-123",
        "1.0.0",
        "2.0.0",
        "1.1.7",
        "2.0.0+build.1848",
        "2.0.1-alpha.1227",
        "1.0.0-alpha+beta",
        "1.2.3----RC-SNAPSHOT.12.9.1--.12+788",
        "1.2.3----R-S.12.9.1--.12+meta",
        "1.2.3----RC-SNAPSHOT.12.9.1--.12",
        "1.0.0+0.build.1-rc.10000aaa-kk-0.1",
        "1.0.0-0A.is.legal",
    })
    @DisplayName("should stringify all versions back to their original form")
    void testToString(@NonNull String version) {
        assertEquals(version, parser.parse(version).toString());
    }

}
