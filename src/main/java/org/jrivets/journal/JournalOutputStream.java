package org.jrivets.journal;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Not multi-threaded!
 * close - will not take effect
 * 
 * @author Dmitry Spasibenko 
 *
 */
final class JournalOutputStream extends OutputStream {

    private final AbstractChunkingPolicy policy;

    JournalOutputStream(AbstractChunkingPolicy policy) {
        this.policy = policy;
    }

    /**
     * Can throw IllegalArgumentException if the thread is interrupted
     */
    @Override
    public void write(int b) throws IOException {
        while (!policy.outputChunk.write(b)) {
            policy.advanceOutputChunk();
        }
    }

    /**
     * Can throw IllegalArgumentException if the thread is interrupted
     */
    @Override
    public void write(byte b[], int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }

        while (len > 0) {
            int written = policy.outputChunk.write(b, off, len);
            if (written == 0) {
                policy.advanceOutputChunk();
            }
            off += written;
            len -= written;
        }
    }

    @Override
    public void flush() throws IOException {
        policy.outputChunk.flush();
    }
}