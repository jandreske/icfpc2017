package io;

import state.LogicException;

import java.beans.Transient;

public class River {

    private int source;
    private int target;

    private int owner = -1;

    public River() {}

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

    public boolean touches(int siteId) {
        return source == siteId || target == siteId;
    }

    public int getOwner() {
        return owner;
    }

    public void setOwner(int owner) {
        this.owner = owner;
    }

    @Transient
    public boolean isClaimed() {
        return owner >= 0;
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

    public int getOpposite(Integer site) {
        if (site == source) return target;
        if (site == target) return source;
        throw new LogicException("Cannot get opposite from site not touching river");
    }
}
