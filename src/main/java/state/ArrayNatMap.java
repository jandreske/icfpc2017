package state;

import java.util.Arrays;

/**
 * Map for int keys >= 0. Does not support null values.
 */
public class ArrayNatMap<T> {

    private Object[] values;
    private int size;

    public ArrayNatMap(int expectedMaxKey) {
        values = new Object[expectedMaxKey + 1];
        size = 0;
    }

    public T get(int key) {
        if (key >= values.length) {
            return null;
        }
        return (T) values[key];
    }

    public T put(int key, T value) {
        if (key >= values.length) {
            int newSize = Math.max(2 * values.length, key + 1);
            values = Arrays.copyOf(values, newSize);
            values[key] = value;
            size++;
            return null;
        }
        T oldValue = (T) values[key];
        values[key] = value;
        if (oldValue == null) {
            size++;
        }
        return oldValue;
    }

    public boolean containsKey(int key) {
        return key < values.length && values[key] != null;
    }

    public int size() {
        return size;
    }

}
