package xyz.apollosoftware.bibliothiki.compression.utils;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class TestPeekableInputStream {

    private InputStream rawStream;
    private InputStream stream;

    @BeforeEach
    void setUp() {
        rawStream = createInputStream();
        stream = PeekableInputStream.ensurePeekable(rawStream);
    }

    @AfterEach
    void destroy() {
        assertDoesNotThrow(stream::close);
    }

    @Test
    @DisplayName("should support available()")
    void testAvailable() {
        assertNotEquals(0, assertDoesNotThrow(stream::available));

        while (assertDoesNotThrow(stream::available) > 0) {
            assertDoesNotThrow(() -> stream.read());
        }

        assertEquals(0, assertDoesNotThrow(stream::available));
    }

    @Test
    @DisplayName("should return a non-zero available() when marked bytes are available")
    void testAvailableMarked() {
        assertNotEquals(0, assertDoesNotThrow(stream::available));
        stream.mark(1);

        while (assertDoesNotThrow(stream::available) > 0) {
            assertDoesNotThrow(() -> stream.read());
        }

        assertEquals(0, assertDoesNotThrow(stream::available));

        // After the reset, one byte should be available (see the read limit).
        assertDoesNotThrow(stream::reset);
        assertNotEquals(0, assertDoesNotThrow(stream::available));
        assertDoesNotThrow(() -> stream.read());
        assertEquals(0, assertDoesNotThrow(stream::available));
    }

    @Test
    @DisplayName("should indicate support for mark(int) when wrapping a stream that doesn't support it")
    void testMarkAvailable() {
        assertFalse(rawStream.markSupported(), "For test purposes, the original stream should not report markSupported");
        assertTrue(stream.markSupported(), "Peekable stream should report markSupported");
    }

    @Test
    @DisplayName("should return false for isMarked by default")
    void testIsNotMarkedByDefault() {
        assertFalse(assertInstanceOf(PeekableInputStream.class, stream).isMarked());
    }

    @Test
    @DisplayName("should return true for isMarked between mark(int) and reset()")
    void testIsMarked() {
        assertFalse(assertInstanceOf(PeekableInputStream.class, stream)::isMarked);
        stream.mark(1);
        assertTrue(assertInstanceOf(PeekableInputStream.class, stream)::isMarked);
        assertDoesNotThrow(stream::reset);
        assertFalse(assertInstanceOf(PeekableInputStream.class, stream)::isMarked);

        stream.mark(1);
        assertTrue(assertInstanceOf(PeekableInputStream.class, stream)::isMarked);
        assertEquals(251, assertDoesNotThrow(() -> stream.read()));
        assertDoesNotThrow(stream::reset);
        assertFalse(assertInstanceOf(PeekableInputStream.class, stream)::isMarked);
        assertEquals(251, assertDoesNotThrow(() -> stream.read()));
        assertFalse(assertInstanceOf(PeekableInputStream.class, stream)::isMarked);
    }

    @Test
    @DisplayName("should correctly handle sequential calls to mark(int) and reset()")
    void testSequentialMarkAndResetCalls() {
        stream.mark(2);
        assertEquals(251, assertDoesNotThrow(() -> stream.read()));
        assertEquals(252, assertDoesNotThrow(() -> stream.read()));
        assertDoesNotThrow(stream::reset);

        stream.mark(4);
        assertEquals(251, assertDoesNotThrow(() -> stream.read()));
        assertEquals(252, assertDoesNotThrow(() -> stream.read()));
        assertEquals(253, assertDoesNotThrow(() -> stream.read()));
        assertEquals(254, assertDoesNotThrow(() -> stream.read()));
        assertDoesNotThrow(stream::reset);

        stream.mark(2);
        assertEquals(251, assertDoesNotThrow(() -> stream.read()));
        assertEquals(252, assertDoesNotThrow(() -> stream.read()));
        assertDoesNotThrow(stream::reset);

        assertEquals(251, assertDoesNotThrow(() -> stream.read()));
        assertEquals(252, assertDoesNotThrow(() -> stream.read()));
        assertEquals(253, assertDoesNotThrow(() -> stream.read()));
        assertEquals(254, assertDoesNotThrow(() -> stream.read()));
        assertEquals(255, assertDoesNotThrow(() -> stream.read()));
    }

    @Test
    @DisplayName("should ignore marked bytes that exceed the mark limit")
    void testMarkLimit() {
        stream.mark(1);
        assertEquals(251, assertDoesNotThrow(() -> stream.read()));

        // exceeds the limit
        assertEquals(252, assertDoesNotThrow(() -> stream.read()));

        assertDoesNotThrow(stream::reset);
        assertEquals(251, assertDoesNotThrow(() -> stream.read()));
        assertEquals(253, assertDoesNotThrow(() -> stream.read()));
        assertEquals(254, assertDoesNotThrow(() -> stream.read()));
        assertEquals(255, assertDoesNotThrow(() -> stream.read()));

        assertEquals(0, assertDoesNotThrow(stream::available));
    }

    @Test
    @DisplayName("should work the same as an un-peekable stream when mark(int) is not called")
    void testNormalRead() {
        assertNotEquals(0, assertDoesNotThrow(stream::available));
        assertEquals(251, assertDoesNotThrow(() -> stream.read()));
        assertEquals(252, assertDoesNotThrow(() -> stream.read()));
        assertEquals(253, assertDoesNotThrow(() -> stream.read()));
        assertEquals(254, assertDoesNotThrow(() -> stream.read()));
        assertEquals(255, assertDoesNotThrow(() -> stream.read()));
        assertEquals(0, assertDoesNotThrow(stream::available));
    }

    @Test
    @DisplayName("calling reset() without mark(int) should be a no-op")
    void testResetWithoutMark() {
        assertNotEquals(0, assertDoesNotThrow(stream::available));
        assertEquals(251, assertDoesNotThrow(() -> stream.read()));
        assertEquals(252, assertDoesNotThrow(() -> stream.read()));
        assertDoesNotThrow(stream::reset);
        assertEquals(253, assertDoesNotThrow(() -> stream.read()));
        assertEquals(254, assertDoesNotThrow(() -> stream.read()));
        assertEquals(255, assertDoesNotThrow(() -> stream.read()));
        assertEquals(0, assertDoesNotThrow(stream::available));
    }

    @Test
    @DisplayName("calling read() after close() should throw")
    void testReadAfterCloseThrows() {
        assertNotEquals(0, assertDoesNotThrow(stream::available));
        assertEquals(251, assertDoesNotThrow(() -> stream.read()));
        assertEquals(252, assertDoesNotThrow(() -> stream.read()));
        assertDoesNotThrow(stream::close);
        assertThrowsExactly(IOException.class, () -> stream.read());
    }

    @Test
    @DisplayName("calling mark() after close() should appear to be a no-op")
    void testMarkAfterCloseIsNoOp() {
        assertNotEquals(0, assertDoesNotThrow(stream::available));
        assertEquals(251, assertDoesNotThrow(() -> stream.read()));
        assertEquals(252, assertDoesNotThrow(() -> stream.read()));
        assertFalse(assertInstanceOf(PeekableInputStream.class, stream)::isMarked);
        assertDoesNotThrow(stream::close);
        assertThrowsExactly(IOException.class, () -> stream.read());
        assertFalse(assertInstanceOf(PeekableInputStream.class, stream)::isMarked);
        assertDoesNotThrow(() -> stream.mark(1));
        assertFalse(assertInstanceOf(PeekableInputStream.class, stream)::isMarked);
        assertThrowsExactly(IOException.class, () -> stream.read());
    }

    @Test
    @DisplayName("should be able to replay marked bytes")
    void testReplay() {
        assertNotEquals(0, assertDoesNotThrow(stream::available));
        assertEquals(251, assertDoesNotThrow(() -> stream.read()));
        assertEquals(252, assertDoesNotThrow(() -> stream.read()));
        stream.mark(2);
        assertEquals(253, assertDoesNotThrow(() -> stream.read()));
        assertEquals(254, assertDoesNotThrow(() -> stream.read()));
        assertDoesNotThrow(stream::reset);
        assertEquals(253, assertDoesNotThrow(() -> stream.read()));
        assertEquals(254, assertDoesNotThrow(() -> stream.read()));
        assertEquals(255, assertDoesNotThrow(() -> stream.read()));
        assertEquals(0, assertDoesNotThrow(stream::available));
    }

    @Test
    @DisplayName("ensurePeekable should be a no-op if the stream already reports markSupported()")
    void testEnsurePeekableForMarkSupported() {
        final var markedStream = new ByteArrayInputStream(new byte[]{
            (byte) 251,
            (byte) 252,
            (byte) 253,
            (byte) 254,
            (byte) 255
        });

        // Ensure the exact same reference is returned.
        final var markedPeekableStream = PeekableInputStream.ensurePeekable(markedStream);
        assertTrue(markedStream.markSupported());
        assertEquals(markedStream, markedPeekableStream);
        assertSame(markedStream, markedPeekableStream, "The stream reference should match.");
    }

    @NonNull
    private InputStream createInputStream() {
        final var data = new byte[]{
            (byte) 251,
            (byte) 252,
            (byte) 253,
            (byte) 254,
            (byte) 255
        };

        // Return an input stream that does not support mark.
        return new PrimitiveByteArrayInputStream(data);
    }

    private static class PrimitiveByteArrayInputStream extends InputStream {

        private final byte @NonNull [] data;
        private int index = 0;

        public PrimitiveByteArrayInputStream(byte @NonNull [] data) {
            this.data = data;
        }

        @Override
        public int available() {
            return index < data.length ? 1 : 0;
        }

        @Override
        public int read() {
            return data[index++] & 0xFF;
        }

    }

}
