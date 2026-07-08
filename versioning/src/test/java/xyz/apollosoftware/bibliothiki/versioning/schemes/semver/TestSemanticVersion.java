package xyz.apollosoftware.bibliothiki.versioning.schemes.semver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import xyz.apollosoftware.bibliothiki.versioning.VersioningException;
import xyz.apollosoftware.bibliothiki.versioning.schemes.semver.SemanticVersion.BuildIdentifier;
import xyz.apollosoftware.bibliothiki.versioning.schemes.semver.SemanticVersion.PreReleaseIdentifier;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static xyz.apollosoftware.bibliothiki.versioning.schemes.semver.SemanticVersion.BuildIdentifier.buildMetadata;
import static xyz.apollosoftware.bibliothiki.versioning.schemes.semver.SemanticVersion.PreReleaseIdentifier.preRelease;

class TestSemanticVersion {

    @Test
    @DisplayName("should produce version 0.0.0 if no parameters are supplied")
    void testDefault() {
        assertEquals(new SemanticVersion(0, 0, 0, null, null), SemanticVersion.builder().build());
    }

    @Test
    @DisplayName("should be possible to build SemanticVersion instances with builders")
    void testBuilders() {
        // Check that version is equivalent to major, minor and patch.
        assertEquals(SemanticVersion.builder().version(1, 2, 3).build(), SemanticVersion.builder().major(1).minor(2).patch(3).build());
    }

    @Test
    @DisplayName("should reject negative version numbers")
    void testNegativeSemanticVersions() {
        assertThrowsExactly(VersioningException.class, () -> SemanticVersion.builder().version(-1, 0, 0).build());
        assertThrowsExactly(VersioningException.class, () -> SemanticVersion.builder().version(0, -1, 0).build());
        assertThrowsExactly(VersioningException.class, () -> SemanticVersion.builder().version(0, 0, -1).build());
    }

    @Test
    @DisplayName("should match equal versions (by Java equals rules, not SemVer equals rules)")
    void testIsEqual() {
        assertEquals(
            SemanticVersion.builder().version(1, 0, 0).build(),
            SemanticVersion.builder().version(1, 0, 0).build()
        );

        assertNotEquals(
            SemanticVersion.builder().version(1, 0, 0).build(),
            SemanticVersion.builder().version(1, 0, 0).addPreRelease("alpha").build()
        );

        assertNotEquals(
            SemanticVersion.builder().version(1, 0, 0).build(),
            SemanticVersion.builder().version(1, 0, 1).build()
        );

        assertNotEquals(
            SemanticVersion.builder().version(1, 0, 0).build(),
            SemanticVersion.builder().version(1, 0, 1).build()
        );

        assertNotEquals(
            SemanticVersion.builder().version(1, 0, 0).addBuildMetadata("foo").build(),
            SemanticVersion.builder().version(1, 0, 0).build()
        );

        assertNotEquals(
            SemanticVersion.builder().version(1, 0, 0).addBuildMetadata("foo").build(),
            SemanticVersion.builder().version(1, 0, 0).addBuildMetadata("bar").build()
        );
    }

    @Test
    @DisplayName("should match equivalent versions (by SemVer equals rules, not Java equals rules)")
    void testIsEquivalent() {
        assertTrue(
            SemanticVersion.builder().version(1, 0, 0).build().isEquivalent(
                SemanticVersion.builder().version(1, 0, 0).build()
            )
        );

        assertFalse(
            SemanticVersion.builder().version(1, 0, 0).build().isEquivalent(
                SemanticVersion.builder().version(1, 0, 0).addPreRelease("alpha").build()
            )
        );

        assertFalse(
            SemanticVersion.builder().version(1, 0, 0).build().isEquivalent(
                SemanticVersion.builder().version(1, 0, 1).build()
            )
        );

        assertTrue(
            SemanticVersion.builder().version(1, 0, 0).addBuildMetadata("foo").build().isEquivalent(
                SemanticVersion.builder().version(1, 0, 0).build()
            )
        );

        assertTrue(
            SemanticVersion.builder().version(1, 0, 0).addBuildMetadata("foo").build().isEquivalent(
                SemanticVersion.builder().version(1, 0, 0).addBuildMetadata("bar").build()
            )
        );
    }

    @Test
    @DisplayName("should handle precedence examples per the specification")
    void testPrecedence() {
        final var versions = new SemanticVersion[] {
            // The list of versions in ascending order.
            SemanticVersion.builder().version(1, 0, 0).addPreRelease("alpha").build(),
            SemanticVersion.builder().version(1, 0, 0).addPreRelease("alpha", "1").build(),
            SemanticVersion.builder().version(1, 0, 0).addPreRelease("alpha", "beta").build(),
            SemanticVersion.builder().version(1, 0, 0).addPreRelease("beta").build(),
            SemanticVersion.builder().version(1, 0, 0).addPreRelease("beta", "2").build(),
            SemanticVersion.builder().version(1, 0, 0).addPreRelease("beta", "11").build(),
            SemanticVersion.builder().version(1, 0, 0).addPreRelease("rc", "1").build(),
            SemanticVersion.builder().version(1, 0, 0).build(),
            SemanticVersion.builder().version(2, 0, 0).build(),
            SemanticVersion.builder().version(2, 1, 0).build(),
            SemanticVersion.builder().version(2, 1, 1).build(),
        };

        for (int i = 1; i < versions.length; i++) {
            assertTrue(versions[i].isGreaterThan(versions[i - 1]), "%s > %s".formatted(versions[i], versions[i - 1]));
            assertFalse(versions[i - 1].isGreaterThan(versions[i]), "%s > %s".formatted(versions[i - 1], versions[i]));

            assertTrue(versions[i - 1].isLessThan(versions[i]), "%s < %s".formatted(versions[i], versions[i - 1]));
            assertFalse(versions[i].isLessThan(versions[i - 1]), "%s < %s".formatted(versions[i - 1], versions[i]));
        }
    }

    @Test
    @DisplayName("should stringify versions as expected")
    void testToString() {
        assertEquals("0.0.0", SemanticVersion.builder().build().toString());
        assertEquals("1.0.0", SemanticVersion.builder().version(1, 0, 0).build().toString());
        assertEquals("1.0.0-alpha.1", SemanticVersion.builder().version(1, 0, 0).addPreRelease("alpha", "1").build().toString());
        assertEquals("1.0.0+001", SemanticVersion.builder().version(1, 0, 0).addBuildMetadata("001").build().toString());
        assertEquals("1.0.0-alpha.1+001", SemanticVersion.builder().version(1, 0, 0).addPreRelease("alpha", "1").addBuildMetadata("001").build().toString());
    }

    @Test
    @DisplayName("should stringify pre-release components as expected")
    void testPreReleaseToString() {
        assertEquals("alpha", PreReleaseIdentifier.parse("alpha").toString());
    }

    @Test
    @DisplayName("should stringify build metadata components as expected")
    void testBuildMetadataToString() {
        assertEquals("001", new BuildIdentifier("001").toString());
    }

    @Test
    @DisplayName("should throw if a pre-release component is null")
    void testPreReleaseComponentIsNull() {
        final List<PreReleaseIdentifier> preRelease = new ArrayList<>();
        preRelease.add(null);
        assertThrowsExactly(NullPointerException.class, () -> new SemanticVersion(0, 0, 0, preRelease, null));
    }

    @Test
    @DisplayName("should throw if a pre-release component is empty")
    void testPreReleaseComponentIsEmpty() {
        assertThrowsExactly(VersioningException.class, () -> PreReleaseIdentifier.parse(""));
    }

    @Test
    @DisplayName("should accept a pre-release component of '0'")
    void testPreReleaseComponentZero() {
        assertDoesNotThrow(() -> PreReleaseIdentifier.parse("0"));
    }

    @Test
    @DisplayName("should throw if a pre-release component has a leading zero")
    void testPreReleaseComponentLeadingZeroes() {
        assertThrowsExactly(VersioningException.class, () -> PreReleaseIdentifier.parse("01"));
    }

    @Test
    @DisplayName("should have a semantically correct equals implementation for PreReleaseIdentifier")
    @SuppressWarnings("SimplifiableAssertion")
    void testPreReleaseComponentEquals() {
        final var preRelease = PreReleaseIdentifier.parse("alpha");
        assertTrue(preRelease.equals(PreReleaseIdentifier.parse("alpha")));

        //noinspection ConstantValue
        assertFalse(preRelease.equals(null));
        assertFalse(preRelease.equals(new Object()));
        assertFalse(preRelease.equals(PreReleaseIdentifier.parse("beta")));
    }

    @Test
    @DisplayName("should have a semantically correct equals implementation for PreReleaseIdentifier")
    void testPreReleaseComponentHashCode() {
        assertEquals(
            PreReleaseIdentifier.parse("alpha").hashCode(),
            PreReleaseIdentifier.parse("alpha").hashCode()
        );

        assertNotEquals(
            PreReleaseIdentifier.parse("alpha").hashCode(),
            PreReleaseIdentifier.parse("beta").hashCode()
        );
    }

    @Test
    @DisplayName("should throw if a build metadata component is null")
    void testBuildMetadataComponentIsNull() {
        final List<BuildIdentifier> buildMetadata = new ArrayList<>();
        buildMetadata.add(null);
        assertThrowsExactly(NullPointerException.class, () -> new SemanticVersion(0, 0, 0, null, buildMetadata));
    }

    @Test
    @DisplayName("should throw if a build metadata component is invalid")
    void testBuildMetadataComponentInvalid() {
        assertThrowsExactly(VersioningException.class, () -> new BuildIdentifier("£"));
    }

    @Test
    @DisplayName("builder should allow adding to or overwriting build metadata")
    void testPreReleaseBuilder() {
        assertEquals(
            "0.0.0-alpha.beta",
            SemanticVersion.builder().addPreRelease("alpha").addPreRelease("beta").build().toString()
        );

        assertEquals(
            "0.0.0-beta",
            SemanticVersion.builder().preRelease(preRelease("alpha")).preRelease(preRelease("beta")).build().toString()
        );
    }

    @Test
    @DisplayName("builder should allow adding to or overwriting build metadata")
    void testBuildMetadataBuilder() {
        assertEquals(
            "0.0.0+alpha.beta",
            SemanticVersion.builder().addBuildMetadata("alpha").addBuildMetadata("beta").build().toString()
        );

        assertEquals(
            "0.0.0+beta",
            SemanticVersion.builder().buildMetadata(buildMetadata("alpha")).buildMetadata(buildMetadata("beta")).build().toString()
        );
    }

    @Test
    @DisplayName("builder can be constructed from existing version")
    void testAsBuilder() {
        final var version = SemanticVersion.builder().version(1, 2, 3).build();

        assertEquals(
            "1.2.3",
            version.toString()
        );

        assertEquals(
            "1.2.3-alpha",
            version.asBuilder().addPreRelease("alpha").build().toString()
        );
    }

    @Test
    @DisplayName("can be incremented by major, minor and release versions")
    void testIncrement() {
        final var version = SemanticVersion.builder().version(1, 2, 3).build();
        assertEquals("1.2.3", version.toString());

        assertEquals("1.2.4", version.withIncremented(SemanticVersion.IncrementType.PATCH).toString());
        assertEquals("1.3.0", version.withIncremented(SemanticVersion.IncrementType.MINOR).toString());
        assertEquals("2.0.0", version.withIncremented(SemanticVersion.IncrementType.MAJOR).toString());
    }

    @Test
    @DisplayName("can be incremented to a pre-release version")
    void testIncrementToPreRelease() {
        final var version = SemanticVersion.builder().version(1, 2, 3).build();
        assertEquals("1.2.3", version.toString());

        final var dev1 = version.withIncrementedPreRelease("dev");
        assertEquals("1.2.3-dev.1", dev1.toString());

        final var dev2 = dev1.withIncrementedPreRelease("dev");
        assertEquals("1.2.3-dev.2", dev2.toString());

        final var partialDev = version.asBuilder().addPreRelease("dev").build();
        assertEquals("1.2.3-dev", partialDev.toString());

        // To avoid accidentally modifying unintended parts of the version
        // string, this correctly ignores any partial version numbers and adds
        // to the end of the string instead.
        assertEquals("1.2.3-dev.dev.1", partialDev.withIncrementedPreRelease("dev").toString());

        final var devWithAlpha = version.asBuilder().addPreRelease("dev", "alpha").build();
        assertEquals("1.2.3-dev.alpha", devWithAlpha.toString());
        assertEquals("1.2.3-dev.alpha.dev.1", devWithAlpha.withIncrementedPreRelease("dev").toString());
    }

    @Test
    @DisplayName("can be incremented to an arbitrary initial pre-release version")
    void testIncrementToCustomPreRelease() {
        final var version = SemanticVersion.builder().version(1, 2, 3).build();
        assertEquals("1.2.3", version.toString());

        final var incremented = version.withIncrementedPreRelease("dev", 5);
        assertEquals("1.2.3-dev.5", incremented.toString());
    }

    @Test
    @DisplayName("cannot be incremented to a negative pre-release version")
    void testIncrementToNegativePreReleaseFails() {
        final var version = SemanticVersion.builder().version(1, 2, 3).build();
        assertEquals("1.2.3", version.toString());
        assertThrowsExactly(VersioningException.class, () -> version.withIncrementedPreRelease("dev", -1));
    }

}
