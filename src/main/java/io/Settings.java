package io;

public class Settings {

    private boolean futures;

    public Settings() {}

    public Settings(boolean futures) {
        this.futures = futures;
    }

    public boolean getFutures() {
        return futures;
    }

    public void setFutures(boolean futures) {
        this.futures = futures;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Settings other = (Settings) o;

        return futures == other.futures;
    }

    @Override
    public int hashCode() {
        return futures ? 1 : 0;
    }
}
