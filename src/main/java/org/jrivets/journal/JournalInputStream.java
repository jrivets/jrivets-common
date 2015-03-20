package org.jrivets.journal;

import java.io.IOException;
import java.io.InputStream;

import org.jrivets.util.CheckUtils;

/**
 * Input stream for journal data.
 * <p>
 * The class supports "mark position behavior": the current position in this
 * input stream can be marked by <code>mark</code> call. A subsequent call to
 * the <code>reset</code> method repositions this stream at the last marked
 * position so that subsequent reads re-read the same bytes.
 * <p>
 * <code>close</code> method invocation will not take effect, so to close the
 * stream properly <code>Journal.close</code> should be invoked instead.
 * 
 * @author Dmitry Spasibenko 
 *
 */
public final class JournalInputStream extends InputStream {

    private final AbstractChunkingPolicy policy;

    JournalInputStream(AbstractChunkingPolicy policy) {
        this.policy = policy;
    }

    @Override
    public int read() throws IOException {
        int result = -1;
        while (true) {
            result = policy.inputChunk.read();
            if (result >= 0 || !policy.syncInput(true, 0L)) {
                break;
            }
        }
        return result;
    }

    @Override
    public int read(byte b[], int off, int len) throws IOException {
        return read(b, off, len, 0L);
    }

    /**
     * Reads up to <code>len</code> bytes of data from the input stream into an
     * array of bytes. An attempt is made to read as many as <code>len</code>
     * bytes, but a smaller number may be read. The number of bytes actually
     * read is returned as an integer.
     *
     * <p>
     * This method blocks until input data is available, end of file is
     * detected, or an exception is thrown.
     * 
     * <p>
     * If the end of file is detected the method can block until new data is
     * available for <code>timeout</code> milliseconds.
     *
     * <p>
     * If <code>len</code> is zero, then no bytes are read and <code>0</code> is
     * returned; otherwise, there is an attempt to read at least one byte. If no
     * byte is available because the stream is at end of file, and no new data
     * for the next timeout milliseconds the value <code>-1</code> is returned
     * after; otherwise, at least one byte is read and stored into
     * <code>b</code>.
     *
     * <p>
     * The first byte read is stored into element <code>b[off]</code>, the next
     * one into <code>b[off+1]</code>, and so on. The number of bytes read is,
     * at most, equal to <code>len</code>. Let <i>k</i> be the number of bytes
     * actually read; these bytes will be stored in elements <code>b[off]</code>
     * through <code>b[off+</code><i>k</i><code>-1]</code>, leaving elements
     * <code>b[off+</code><i>k</i><code>]</code> through
     * <code>b[off+len-1]</code> unaffected.
     *
     * <p>
     * In every case, elements <code>b[0]</code> through <code>b[off]</code> and
     * elements <code>b[off+len]</code> through <code>b[b.length-1]</code> are
     * unaffected.
     *
     * @param b
     *            the buffer into which the data is read.
     * @param off
     *            the start offset in array <code>b</code> at which the data is
     *            written.
     * @param len
     *            the maximum number of bytes to read.
     * @param timeout
     *            the timeout in milliseconds to wait new data if end of file is
     *            reached. If no new data in the timeout, the method will return
     *            -1 (EOF)
     * @return the total number of bytes read into the buffer, or
     *         <code>-1</code> if there is no more data because the end of the
     *         stream has been reached.
     * @exception IOException
     *                If the first byte cannot be read for any reason other than
     *                end of file, or if the input stream has been closed, or if
     *                some other I/O error occurs.
     * @exception NullPointerException
     *                If <code>b</code> is <code>null</code>.
     * @exception IndexOutOfBoundsException
     *                If <code>off</code> is negative, <code>len</code> is
     *                negative, or <code>len</code> is greater than
     *                <code>b.length - off</code>
     * @see java.io.InputStream#read()
     */
    public int read(byte b[], int off, int len, long timeout) throws IOException {
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
            if (result == len || !policy.syncInput(result == 0, timeout)) {
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
            if (skipped >= n || !policy.syncInput(false, 0L)) {
                break;
            }
        }
        ;

        return skipped;
    }

    @Override
    public int available() throws IOException {
        return (int) policy.availableForInput();
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