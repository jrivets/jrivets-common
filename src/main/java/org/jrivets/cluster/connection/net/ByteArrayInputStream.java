package org.jrivets.cluster.connection.net;

/**
 * Exact non-tread safe copy-past of java.io.ByteArrayInputStream. 
 * 
 *  @author Dmitry Spasibenko 
 */
final class ByteArrayInputStream extends java.io.ByteArrayInputStream {

    public ByteArrayInputStream(byte[] buf) {
        super(buf);
    }
    
    @Override
    public int read() {
        return (pos < count) ? (buf[pos++] & 0xff) : -1;
    }
    
    @Override
    public int read(byte b[], int off, int len) {
        if (b == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        }
        if (pos >= count) {
            return -1;
        }
        if (pos + len > count) {
            len = count - pos;
        }
        if (len <= 0) {
            return 0;
        }
        System.arraycopy(buf, pos, b, off, len);
        pos += len;
        return len;
    }

    @Override
    public long skip(long n) {
        if (pos + n > count) {
            n = count - pos;
        }
        if (n < 0) {
            return 0;
        }
        pos += n;
        return n;
    }

    @Override
    public int available() {
        return count - pos;
    }
    
    @Override
    public void reset() {
        pos = mark;
    }
}