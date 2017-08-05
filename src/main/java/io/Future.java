package io;

public class Future {

    private int source;

    private int target;

    public Future() {}

    public Future(int source, int target) {
        this.source = source;
        this.target = target;
    }

    public int getSource() {
        return source;
    }

    public int getTarget() {
        return target;
    }

    public void setSource(int source) {
        this.source = source;
    }

    public void setTarget(int target) {
        this.target = target;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Future other = (Future) o;

        return source == other.source && target == other.target;
    }

    @Override
    public int hashCode() {
        return source * 7 + target;
    }
}
