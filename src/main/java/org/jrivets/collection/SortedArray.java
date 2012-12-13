package org.jrivets.collection;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Resizable auto-sorted array. Added element is placed to the position in order
 * defined by {@link Comparator} which can be optional. Some operations like
 * putting an element to a specified position doesn't make sense.
 * <p>
 * The implementation expects a {@link Comparator} in constructor. This case
 * sort order defined by the provided comparator. <tt>null</tt> values are
 * supported if the comparator supports them.
 * <p>
 * The implementation allows not to provide a {@link Comparator}, this case it
 * is supposed that the type <tt>T</tt> implements {@link Comparable} interface.
 * For the case when a comparator is not provided in the constructor
 * <tt>add(T t)</tt>, <tt>remove(T t)</tt>, <tt>indexOf(T t)</tt> and other
 * methods will throw {@link NullPointerException} if null is provided like
 * argument.
 * 
 * <p>
 * This is not thread-safe implementation, so concurrent modifications from
 * different threads will follow to unpredictable results.
 * 
 * @author Dmitry Spasibenko
 * 
 */
public class SortedArray<T> extends AbstractCollection<T> implements Serializable {

    private static final long serialVersionUID = 7201373894996158833L;

    private final Comparator<T> comparator;

    private transient T[] elements;

    private int size;

    public SortedArray(int capacity) {
        this(null, capacity);
    }

    public SortedArray(Comparator<T> comparator, int capacity) {
        this.comparator = comparator;
        setCapacity(capacity);
    }

    public SortedArray() {
        this(10);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void clear() {
        size = 0;
        elements = null;
        setCapacity(10);
    }

    public void trimToSize() {
        if (elements.length > size) {
            elements = Arrays.copyOf(elements, size);
        }
    }

    public T get(int index) {
        checkIndexInRange(index);
        return elements[index];
    }

    @Override
    public boolean add(T element) {
        ensureCapacityBeforeInsertion();
        int idx = getIndexForInsertion(element);
        insertElementAtSpecificPosition(element, idx);
        return true;
    }

    public int getIndexOf(T element) {
        return Arrays.binarySearch(elements, 0, size, element, comparator);
    }

    public boolean removeElement(T element) {
        int idx = getIndexOf(element);
        if (idx >= 0) {
            removeByIndex(idx);
            return true;
        }
        return false;
    }

    public T removeByIndex(int index) {
        checkIndexInRange(index);
        if (index < --size) {
            System.arraycopy(elements, index + 1, elements, index, size - index);
        }
        T result = elements[size];
        elements[size] = null;
        return result;
    }

    public Iterator<T> iterator() {
        return new Iterator<T>() {

            private int idx;

            private int removeIdx = -1;

            @Override
            public boolean hasNext() {
                return idx < size;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                removeIdx = idx++;
                return elements[removeIdx];
            }

            @Override
            public void remove() {
                if (removeIdx == -1) {
                    throw new IllegalStateException();
                }
                removeByIndex(removeIdx);
                removeIdx = -1;
                --idx;
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean contains(Object o) {
        return getIndexForInsertion((T) o) >= 0;
    }

    @Override
    public Object[] toArray() {
        return Arrays.copyOf(elements, size);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean remove(Object o) {
        return removeElement((T) o);
    }

    @Override
    public int hashCode() {
        int hashCode = size;
        for (T element : elements) {
            hashCode = 31 * hashCode + (element == null ? 0 : element.hashCode());
        }
        return hashCode;
    }

    @SuppressWarnings("unchecked")
    private void setCapacity(int newCapacity) {
        if (newCapacity < size) {
            throw new IllegalArgumentException("Capacity " + newCapacity + " cannot be less than size=" + size);
        }
        T[] oldData = elements;
        elements = (T[]) new Object[newCapacity];
        if (oldData != null) {
            System.arraycopy(oldData, 0, elements, 0, size);
        }
    }

    private void insertElementAtSpecificPosition(T element, int idx) {
        System.arraycopy(elements, idx, elements, idx + 1, size - idx);
        elements[idx] = element;
        size++;
    }

    private void ensureCapacityBeforeInsertion() {
        if (size == elements.length) {
            setCapacity(size * 3 / 2 + 1);
        }
    }

    private int getIndexForInsertion(T element) {
        if (size == 0) {
            return 0;
        }
        if (compare(elements[size - 1], element) <= 0) {
            return size;
        }
        int idx = getIndexOf(element);
        if (idx < 0) {
            return -idx - 1;
        }
        return idx;
    }

    @SuppressWarnings("unchecked")
    private int compare(T element1, T element2) {
        return comparator != null ? comparator.compare(element1, element2) : ((Comparable<T>) element1)
                .compareTo(element2);
    }

    private void checkIndexInRange(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("index: " + index + ", size: " + size);
        }
    }

    private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
        s.defaultWriteObject();
        s.writeInt(elements.length);

        for (int i = 0; i < size; i++) {
            s.writeObject(elements[i]);
        }
    }

    @SuppressWarnings("unchecked")
    private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
        s.defaultReadObject();
        int arrayLength = s.readInt();
        Object[] a = new Object[arrayLength];

        for (int i = 0; i < size; i++) {
            a[i] = s.readObject();
        }
        elements = (T[]) a;
    }

    @Override
    public String toString() {
        return new StringBuilder().append("{size=").append(size).append(", capacity=").append(elements.length)
                .append(", elements=").append(Arrays.toString(elements)).append("}").toString();
    }
}
