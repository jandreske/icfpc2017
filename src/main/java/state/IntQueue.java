package state;

import java.util.NoSuchElementException;

/**
 * Array based FIFO queue for int values.
 */
public class IntQueue {

    private int[] values;
    private int start;
    private int size;

    public IntQueue(int initialCapacity) {
        values = new int[initialCapacity];
        start = 0;
        size = 0;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int removeFirst() {
        if (size == 0) {
            throw new NoSuchElementException("empty queue");
        }
        int value = values[start];
        start++;
        size--;
        if (start == values.length) {
            start = 0;
        }
        return value;
    }

    public void addLast(int value) {
        if (size == values.length) {
            grow();
        }
        int i = start + size;
        if (i >= values.length) {
            i -= values.length;
        }
        values[i] = value;
        size++;
    }

    private void grow() {
        int[] ext = new int[2*size];
        int n = values.length - start;
        System.arraycopy(values, start, ext, 0, n);
        System.arraycopy(values, 0, ext, n, size - n);
        values = ext;
        start = 0;
    }

}
