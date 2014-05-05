package org.jrivets.journal;

import java.io.IOException;
import java.io.InputStream;

import org.jrivets.util.CheckUtils;

/**
 * close - will not take effect
 * 
 *  @author Dmitry Spasibenko (dmitry.spasibenko@mulesoft.com)
 *
 */
final class JournalInputStream extends InputStream {

    private final AbstractChunkingPolicy policy;
    
    JournalInputStream(AbstractChunkingPolicy policy) {
        this.policy = policy;
    }
    
    @Override
    public int read() throws IOException {
        int result = -1;
        while (true) {
            result = policy.inputChunk.read();
            if (result >= 0 || !policy.syncInput(true)) {
                break;
            }
        }
        return result;
    }

    @Override
    public int read(byte b[], int off, int len) throws IOException {
        CheckUtils.arrayBounds(b.length, off, len);    
        if (len == 0) {
            return 0;
        }
        
        int result = 0;
        while (true) {
            int actual = policy.inputChunk.read(b, result, len - result);
            if (actual > 0) {
                result += actual;
            }
            if (result == len || !policy.syncInput(result == 0)) {
                break;
            }
        }
        return result == 0 ? -1 : result;
    }

    @Override
    public long skip(long n) throws IOException {
        if (n <= 0) {
            return 0;
        }
        
        long skipped = 0L;
        while (true) {
            skipped += policy.inputChunk.skip(n - skipped);
            if (skipped >= n || !policy.syncInput(false)) {
                break;
            }
        };
        
        return skipped;
    }

    @Override
    public int available() throws IOException {
        return policy.inputChunk.available();
    }

    @Override
    public void mark(int readlimit) {
        policy.mark(readlimit);
    }
    
    @Override
    public void reset() throws IOException {
        policy.reset();
    }

    @Override
    public boolean markSupported() {
        return true;
    }
}