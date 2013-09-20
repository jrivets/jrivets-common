package org.jrivets.collection;

import static org.testng.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;

import org.jrivets.collection.SortedArray;
import org.testng.annotations.Test;

public class SortedArrayTest {

    private Comparator<Integer> descComparator = new DescComparator();

    private static class DescComparator implements Comparator<Integer>, Serializable {

        private static final long serialVersionUID = 1L;

        public int compare(Integer o1, Integer o2) {
            if (o1 == o2) {
                return 0;
            }
            if (o1 == null) {
                return 1;
            }
            if (o2 == null) {
                return -1;
            }
            return o2.compareTo(o1);
        }

    };

    @Test
    public void add() {
        SortedArray<Integer> sa = new SortedArray<Integer>();
        sa.add(2);
        sa.add(3);
        sa.add(1);
        assertTrue(sa.getIndexOf(3) == 2);
        assertTrue(sa.getIndexOf(2) == 1);
        assertTrue(sa.getIndexOf(1) == 0);
    }

    @Test
    public void add2() {
        SortedArray<Integer> sa = new SortedArray<Integer>(descComparator, 2);
        sa.add(2);
        sa.add(3);
        sa.add(1);
        assertTrue(sa.get(0) == 3);
        assertTrue(sa.get(1) == 2);
        assertTrue(sa.get(2) == 1);
    }
    
    @Test
    public void addAndNulls() {
        SortedArray<Integer> sa = new SortedArray<Integer>(descComparator, 2);
        sa.add(2);
        sa.add(null);
        sa.add(3);
        sa.add(1);
        assertNull(sa.get(3));
        assertTrue(sa.get(0) == 3);
        assertTrue(sa.get(1) == 2);
        assertTrue(sa.get(2) == 1);
    }

    @Test(expectedExceptions = IndexOutOfBoundsException.class)
    public void get() {
        new SortedArray<Integer>().get(0);
    }

    @Test
    public void get2() {
        SortedArray<Integer> sa = new SortedArray<Integer>();
        sa.add(1);
        assertTrue(sa.get(0) == 1);
    }
    
    @Test
    public void getNull() {
        SortedArray<Integer> sa = new SortedArray<Integer>(descComparator, 2);
        sa.add(2);
        sa.add(null);
        assertNull(sa.get(1));
        assertTrue(sa.get(0) == 2);
    }

    @Test
    public void removeElement() {
        SortedArray<Integer> sa = new SortedArray<Integer>();
        sa.add(1);
        assertFalse(sa.removeElement(0));
        assertTrue(sa.removeElement(1));
        assertTrue(sa.size() == 0);
    }
    
    @Test
    public void removeElementNull() {
        SortedArray<Integer> sa = new SortedArray<Integer>(descComparator, 2);
        sa.add(1);
        assertFalse(sa.removeElement(null));
        sa.add(null);
        assertTrue(sa.removeElement(null));
    }
    
    @Test
    public void remove() {
        SortedArray<Integer> sa = new SortedArray<Integer>();
        sa.add(1);
        assertFalse(sa.remove(0));
        assertTrue(sa.remove(1));
        assertTrue(sa.size() == 0);
    }
    
    @Test
    public void removeNull() {
        SortedArray<Integer> sa = new SortedArray<Integer>(descComparator, 2);
        sa.add(1);
        assertFalse(sa.remove(null));
        sa.add(null);
        assertTrue(sa.remove(null));
    }

    @Test
    public void removeByIndex() {
        SortedArray<Integer> sa = new SortedArray<Integer>();
        sa.add(1);
        assertEquals(new Integer(1), sa.removeByIndex(0));
    }
    
    @Test
    public void removeByIndexNull() {
        SortedArray<Integer> sa = new SortedArray<Integer>(descComparator, 2);
        sa.add(1);
        sa.add(null);
        assertEquals(2, sa.size());
        assertEquals(null, sa.removeByIndex(1));
        assertEquals(1, sa.size());
    }

    @Test(expectedExceptions = IndexOutOfBoundsException.class)
    public void removeByIndex2() {
        SortedArray<Integer> sa = new SortedArray<Integer>();
        sa.add(1);
        assertTrue(sa.removeByIndex(1) == 1);
    }

    @Test
    public void getIndexOf() {
        SortedArray<Integer> sa = new SortedArray<Integer>();
        sa.add(1);
        assertTrue(sa.getIndexOf(0) < 0);
        assertTrue(sa.getIndexOf(1) == 0);
    }
    
    @Test
    public void getIndexOfNull() {
        SortedArray<Integer> sa = new SortedArray<Integer>(descComparator, 2);
        sa.add(1);
        assertTrue(sa.getIndexOf(null) < 0);
        sa.add(null);
        assertTrue(sa.getIndexOf(null) == 1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void serialization() throws IOException, ClassNotFoundException {
        SortedArray<Integer> sa = new SortedArray<Integer>(descComparator, 1);
        sa.add(1);
        sa.add(2);
        sa.add(3);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(baos);
        os.writeObject(sa);

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);

        SortedArray<Integer> readArray = (SortedArray<Integer>) ois.readObject();
        assertTrue(readArray.size() == 3);
        assertTrue(readArray.get(0) == 3);
        assertTrue(readArray.get(1) == 2);
        assertTrue(readArray.get(2) == 1);
        readArray.add(5);
        assertTrue(readArray.get(0) == 5);
    }

    @Test
    public void iteratorTest() {
        SortedArray<Integer> sa = new SortedArray<Integer>();
        for (int idx = 0; idx < 1000; idx++) {
            sa.add(idx);
        }

        int value = 0;
        for (Integer i : sa) {
            assertTrue(i == value++);
        }

        assertTrue(value == 1000);
    }
    
    @Test
    public void iteratorEmpty() {
        SortedArray<Integer> sa = new SortedArray<Integer>();
        Iterator<Integer> it = sa.iterator();
        assertFalse(it.hasNext());
    }
    
    @Test
    public void iterator1Elem() {
        SortedArray<Integer> sa = new SortedArray<Integer>();
        sa.add(1);
        Iterator<Integer> it = sa.iterator();
        assertTrue(it.hasNext());
        assertEquals(new Integer(1), it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void iterator2Elems() {
        SortedArray<Integer> sa = new SortedArray<Integer>();
        sa.add(2);
        sa.add(1);
        Iterator<Integer> it = sa.iterator();
        assertTrue(it.hasNext());
        assertEquals(new Integer(1), it.next());
        assertTrue(it.hasNext());
        assertEquals(new Integer(2), it.next());
        assertFalse(it.hasNext());
    }
    
    @Test
    public void iteratorRemove() {
        SortedArray<Integer> sa = new SortedArray<Integer>();
        sa.add(2);
        sa.add(1);
        Iterator<Integer> it = sa.iterator();
        assertTrue(it.hasNext());
        it.next();
        it.remove();
        assertTrue(it.hasNext());
        assertEquals(new Integer(2), it.next());
        assertFalse(it.hasNext());
    }
    
    @Test
    public void iteratorRemoveLast() {
        SortedArray<Integer> sa = new SortedArray<Integer>();
        sa.add(2);
        sa.add(1);
        Iterator<Integer> it = sa.iterator();
        assertTrue(it.hasNext());
        assertEquals(new Integer(1), it.next());
        assertEquals(new Integer(2), it.next());
        it.remove();
        assertFalse(it.hasNext());
        assertEquals(1, sa.size());
    }

    @Test
    public void toStringTest() {
        SortedArray<Integer> sa = new SortedArray<Integer>();
        sa.add(1);
        sa.add(2);
        sa.add(-3);
        assertTrue(sa.toString().contains("{size=3, capacity=10, elements=[-3, 1, 2"));
    }
}
