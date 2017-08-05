package io;

public class Site {

    private int id;

    private double x, y;

    public Site() {}

    public Site(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String toString() {
        return Integer.toString(id);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Site site = (Site) o;

        return id == site.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
