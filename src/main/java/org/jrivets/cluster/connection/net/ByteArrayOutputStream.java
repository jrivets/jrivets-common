package org.jrivets.cluster.connection.net;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

final class ByteArrayOutputStream extends OutputStream {
    
    private int capacity;
    
    private int position;
    
    private byte[] buffer;
    
    private final ArrayList<byte[]> buffers = new ArrayList<byte[]>(3);
    
    ByteArrayOutputStream() {
        this(256);
    }

    ByteArrayOutputStream(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Size should be positive integer. size=" + size);
        }
        arrangeNew(size);
    }
    
    @Override
    public void write(int b) throws IOException {
        if (position == buffer.length) {
            arrangeNew(1);
        }
        buffer[position++] = (byte) b;
    }
    
    @Override
    public void write(byte[] b, int off, int len) {
        if (b == null) {
            throw new NullPointerException();
        } else if ((off < 0) || (off > b.length) || (len < 0) ||
                   ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }
        
        int remaining = len;
        while (remaining > 0) {
            int toCopy = Math.min(remaining, buffer.length - position);
            System.arraycopy(b, off+len-remaining, buffer, position, toCopy);
            remaining -= toCopy;
            if (remaining > 0) {
                arrangeNew(remaining);
            } else {
                position += toCopy;
            }
        }
    }
    
    int size() {
        return capacity - buffer.length + position;
    }
    
    int capacity() {
        return capacity;
    }
    
    ArrayList<byte[]> getBuffers() {
        return buffers;
    }
    
    int getPosition() {
        return position;
    }
    
    private void arrangeNew(int size) {
        if (size < 1024 || buffers.size() > 5) {
            size = Math.max(size, capacity);
        }
        buffer = new byte[size];
        buffers.add(buffer);
        position = 0;
        capacity += size;
    }
}
