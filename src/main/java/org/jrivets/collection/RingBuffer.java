package org.jrivets.collection;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * Ring Buffer is a container with fixed capacity and FIFO discipline: elements
 * can be added to the end of the buffer and removed from the head only. The
 * <tt>RingBuffer</tt> implements {@link Queue} interface to be used easily in
 * some iteration routines, but some methods for modification the collection
 * like <tt>addAll()</tt>, <tt>removeAll()</tt> etc. are not supported and will
 * throw {@link UnsupportedOperationException} exception.
 * <p>
 * <strong>Note that this implementation is not synchronized.</strong> If
 * multiple threads access a <tt>RingBuffer</tt> instance concurrently, and at
 * least one of the threads modifies the list structurally, it <i>must</i> be
 * synchronized externally <strong>except the use case as follows:</strong>
 * <p>
 * If one thread is reader (uses <tt>remove()</tt>, <tt>poll()</tt>,
 * <tt>element()</tt>, <tt>peek()</tt>, <tt>capacity()</tt> and <tt>size()</tt>
 * methods only), and another one is writer (uses <tt>add()</tt>,
 * <tt>offer()</tt> <tt>capacity()</tt> and <tt>size()</tt> methods only), the
 * methods can be called without any synchronization. For all other scenarios
 * the buffer is not thread-safe, so methods invocations should be properly
 * guarded in case of multi-threads usage.
 * 
 * @author Dmitry Spasibenko
 * 
 * @param <T>
 */
public final class RingBuffer<T> implements Queue<T>, Serializable {

    private static final long serialVersionUID = -8275240829599598029L;

    private transient T[] values;

    private transient int headIdx;

    private transient int tailIdx;

    private class BufferIterator implements Iterator<T> {

        private int idx;

        BufferIterator() {
            this.idx = headIdx;
        }

        @Override
        public boolean hasNext() {
            return idx != tailIdx;
        }

        @Override
        public T next() {
            T t = values[idx];
            idx = correctIdx(idx + 1);
            return t;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("The method remove() is not applicable for RingBuffer iterator.");
        }

    }

    @SuppressWarnings("unchecked")
    public RingBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException();
        }
        this.values = (T[]) new Object[capacity + 1];
    }

    @Override
    public boolean add(T value) {
        if (!offer(value)) {
            throw new IllegalStateException("The buffer is full");
        }
        return true;
    }

    @Override
    public boolean offer(T value) {
        if (size() < values.length - 1) {
            values[tailIdx] = value;
            tailIdx = correctIdx(tailIdx + 1);
            return true;
        }
        return false;
    }

    @Override
    public T remove() {
        assertSizeIsNotZero();
        return removeFirst();
    }

    @Override
    public T poll() {
        if (size() == 0) {
            return null;
        }
        return removeFirst();
    }

    @Override
    public T element() {
        assertSizeIsNotZero();
        return values[headIdx];
    }

    @Override
    public T peek() {
        if (size() == 0) {
            return null;
        }
        return values[headIdx];
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object co : c) {
            if (!contains(co)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        throw new UnsupportedOperationException("The method addAll() is not supported by the RingBuffer");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("The method removeAll() is not supported by the RingBuffer");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("The method retainAll() is not supported by the RingBuffer");
    }

    @Override
    public void clear() {
        int size = size();
        if (size > 0) {
            Arrays.fill(values, 0, values.length - 1, null);
        }
        headIdx = tailIdx;
    }

    public T last() {
        assertSizeIsNotZero();
        return values[correctIdx(tailIdx - 1)];
    }
    
    public T peekLast() {
        if (size() == 0) {
            return null;
        }
        return values[correctIdx(tailIdx - 1)];
    }

    /**
     * The method is safe for 1 reader, 1 writer threads if and only if one of
     * the threads calls the method. It is safe this case because for the
     * execution only head or tail index can be changed.
     */
    @Override
    public int size() {
        int head = headIdx;
        int tail = tailIdx;
        return head > tail ? values.length - head + tail : tail - head;
    }

    @Override
    public boolean isEmpty() {
        return headIdx == tailIdx;
    }

    @Override
    public boolean contains(Object o) {
        return getIndexOf(o) != -1;
    }

    @Override
    public Iterator<T> iterator() {
        return new BufferIterator();
    }

    @Override
    public Object[] toArray() {
        if (tailIdx == headIdx) {
            return new Object[0];
        }
        if (tailIdx > headIdx) {
            return Arrays.copyOfRange(values, headIdx, tailIdx);
        }
        int size = size();
        Object[] result = new Object[size];
        int i = 0;
        for (int idx = headIdx; idx != tailIdx; idx = correctIdx(idx + 1)) {
            result[i++] = values[idx];
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E> E[] toArray(E[] a) {
        if (a.length < size()) {
            return (E[]) toArray();
        }
        int i = 0;
        for (int idx = headIdx; idx != tailIdx; idx = correctIdx(idx + 1)) {
            a[i++] = (E) values[idx];
        }
        return null;
    }

    public int capacity() {
        return values.length - 1;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RingBuffer)) {
            return false;
        }
        @SuppressWarnings("unchecked")
        RingBuffer<T> other = (RingBuffer<T>) o;
        if (other.size() != size()) {
            return false;
        }
        Iterator<T> i1 = iterator();
        Iterator<T> i2 = other.iterator();
        while (i1.hasNext()) {
            T t1 = i1.next();
            T t2 = i2.next();
            if (t1 == null) {
                if (t2 != null) {
                    return false;
                }
            } else if (!t1.equals(t2)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        Iterator<T> i = iterator();
        while (i.hasNext()) {
            T obj = i.next();
            hashCode = 31 * hashCode + (obj == null ? 0 : obj.hashCode());
        }
        return hashCode;
    }

    private T removeFirst() {
        T result = values[headIdx];
        values[headIdx] = null;
        headIdx = correctIdx(headIdx + 1);
        return result;
    }

    private int correctIdx(int idx) {
        if (idx >= values.length) {
            return idx - values.length;
        }
        if (idx < 0) {
            return idx + values.length;
        }
        return idx;
    }

    private void assertSizeIsNotZero() {
        if (size() == 0) {
            throw new NoSuchElementException("RingBuffer is empty");
        }
    }

    private int getIndexOf(Object o) {
        for (int idx = headIdx; idx != tailIdx; idx = correctIdx(idx + 1)) {
            if (values[idx] == null) {
                if (o == null) {
                    return idx;
                }
            } else if (values[idx].equals(o)) {
                return idx;
            }
        }
        return -1;
    }

    private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
        int size = size();
        s.defaultWriteObject();
        s.writeInt(size);
        s.writeInt(values.length);

        for (int i = 0; i < size; i++) {
            int idx = correctIdx(headIdx + i);
            s.writeObject(values[idx]);
        }
    }

    @SuppressWarnings("unchecked")
    private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
        s.defaultReadObject();
        int size = s.readInt();
        int arrayLength = s.readInt();
        Object[] a = new Object[arrayLength];

        for (int i = 0; i < size; i++) {
            a[i] = s.readObject();
        }
        headIdx = 0;
        tailIdx = size;
        values = (T[]) a;
    }

    @Override
    public String toString() {
        return new StringBuilder().append("{size=").append(size()).append(", capacity=").append(capacity())
                .append(", headIdx=").append(headIdx).append(", tailIdx=").append(tailIdx).append("}").toString();
    }

}
