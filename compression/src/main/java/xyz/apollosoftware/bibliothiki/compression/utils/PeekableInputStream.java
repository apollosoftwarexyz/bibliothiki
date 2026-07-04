package xyz.apollosoftware.bibliothiki.compression.utils;

import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * A wrapper for {@link InputStream} that is able to memoize bytes read after
 * {@link #mark(int)} to replay them.
 *
 * <p>
 * This is intended for use in {@link xyz.apollosoftware.bibliothiki.compression.ArchiveInputStream}
 * implementations to peek magic headers in streams where
 * {@link #markSupported()} is false.
 *
 * <p>
 * As such, this implementation is not designed for peeking large amounts of
 * bytes (i.e., more than ~4 KiB). If working with larger buffers is required,
 * this implementation should be extended to provide an implementation of
 * {@link #read(byte[], int, int)}.
 */
public class PeekableInputStream extends InputStream {

    /**
     * If, for the given stream, {@link InputStream#markSupported()} returns
     * true, this method is a no-op, returning the provided stream.
     *
     * <p>
     * Otherwise, the stream is wrapped with {@link PeekableInputStream}.
     *
     * @param stream The stream to peek/read from.
     * @return A peekable stream which may be have been wrapped with
     *         {@link PeekableInputStream} to make it peekable.
     */
    @NonNull
    public static InputStream ensurePeekable(@NonNull InputStream stream) {
        if (stream.markSupported()) {
            return stream;
        }

        return new PeekableInputStream(stream);
    }

    /**
     * Whether the stream has been closed.
     */
    private boolean closed = false;

    /**
     * The stream being peeked/read from.
     */
    private final InputStream stream;

    /**
     * The bytes being stored because of an active mark.
     */
    private final Deque<ByteBuffer> markedBytes = new ArrayDeque<>();

    /**
     * The bytes being replayed because an active mark was reset.
     */
    private final Deque<ByteBuffer> replayBytes = new ArrayDeque<>();

    /**
     * Construct a {@link PeekableInputStream} that wraps the given
     * {@link InputStream}.
     *
     * @param stream The stream to peek/read from.
     */
    protected PeekableInputStream(@NonNull final InputStream stream) {
        this.stream = Objects.requireNonNull(stream, "The stream must not be null");
    }

    /**
     * Get an estimate of the number of bytes available in the stream.
     *
     * <p>
     * This method nominally preserves the behavior of the {@link InputStream}
     * that has been wrapped. However, when the peekable stream
     * {@link #isMarked()} and then {@link #reset()} is called, this returns the
     * number of bytes read since {@link #mark(int)} was called (this should
     * still be treated as an estimate).
     *
     * <p>
     * Per the documentation of {@link InputStream#available()}, it is never
     * correct to use this to determine the size for buffers for the stream. It
     * is an estimate and many implementations will simply return one or zero to
     * merely indicate whether data is available.
     *
     * @return The number of available bytes, or zero at the end of the stream.
     * @throws IOException If an I/O error occurs whilst checking for the number
     *                     of available bytes.
     * @see InputStream#available()
     */
    @Override
    public int available() throws IOException {
        if (!replayBytes.isEmpty()) {
            final var replayBuffer = replayBytes.getFirst();
            if (replayBuffer.hasRemaining()) {
                return replayBuffer.remaining();
            }
        }

        return stream.available();
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    /**
     * Whether the stream is currently marked (i.e., buffering).
     *
     * <p>
     * This method returns true when {@link #mark(int)} has been called and
     * remains true for as long as the stream has not been closed and
     * {@link #reset()} has not been called.
     *
     * @return True if, and only if, the stream is marked.
     */
    public synchronized boolean isMarked() {
        return !markedBytes.isEmpty();
    }

    @Override
    public synchronized void mark(int limit) {
        // Do nothing if the stream is closed. (This doesn't handle the wrapped
        // stream being closed separately but reading after close is undefined
        // behavior).
        if (closed) return;

        markedBytes.push(ByteBuffer.allocate(limit));
    }

    private int getNextByte() throws IOException {
        // If replaying bytes, return the replay byte.
        if (!this.replayBytes.isEmpty()) {
            final ByteBuffer replayBuffer;
            if (this.replayBytes.getFirst().remaining() > 1) {
                replayBuffer = this.replayBytes.getFirst();
            } else {
                replayBuffer = this.replayBytes.pop();
            }

            if (replayBuffer.hasRemaining()) {
                return replayBuffer.get() & 0xFF;
            }
        }

        // Otherwise, read from the underlying stream.
        return stream.read() & 0xFF;
    }

    @Override
    public int read() throws IOException {
        if (this.closed) {
            throw new IOException("Stream already closed");
        }

        final int read = getNextByte();

        // If marking bytes, mark the byte (unless the mark buffer is at the
        // limit).
        if (!this.markedBytes.isEmpty()) {
            final var buffer = this.markedBytes.getFirst();

            if (buffer.hasRemaining()) {
                buffer.put((byte) read);
            }
        }

        return read;
    }

    @Override
    public synchronized void reset() {
        if (this.markedBytes.isEmpty()) return;

        final var replayBuffer = this.markedBytes.pop();
        replayBuffer.limit(replayBuffer.position());
        replayBuffer.position(0);
        this.replayBytes.push(replayBuffer);
    }

    @Override
    public void close() throws IOException {
        this.replayBytes.clear();
        this.markedBytes.clear();
        this.closed = true;
        this.stream.close();
    }

}
