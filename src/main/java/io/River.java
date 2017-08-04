package io;

public class River {

    private final int source;
    private final int target;

    public River(int source, int target) {
        this.source = source;
        this.target = target;
    }

    public int getSource() {
        return source;
    }

    public int getTarget() {
        return target;
    }

    public String toString() {
        return source + "-" + target;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        River river = (River) o;

        if (source != river.source) return false;
        return target == river.target;
    }

    @Override
    public int hashCode() {
        int result = source;
        result = 31 * result + target;
        return result;
    }
}
