package org.jrivets.journal;

import java.io.IOException;
import java.io.OutputStream;

import org.jrivets.util.CheckUtils;

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
        CheckUtils.arrayBounds(b.length, off, len);  
        if (len == 0) {
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