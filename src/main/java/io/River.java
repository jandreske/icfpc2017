package io;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import state.LogicException;

import java.beans.Transient;

/**
 * River, augmented by owner id (-1 if unclaimed).
 * Rivers are normalized so that the source site id is always <= the target site id.
 */
public class River {

    private final int source;
    private final int target;

    private int owner = -1;

    @JsonCreator
    public River(@JsonProperty("source") int source, @JsonProperty("target") int target) {
        if (source <= target) {
            this.source = source;
            this.target = target;
        } else {
            this.source = target;
            this.target = source;
        }
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
        if (!(o instanceof River)) {
            return false;
        }
        River river = (River) o;
        return source == river.source && target == river.target;
    }

    @Override
    public int hashCode() {
        return 31 * source + target;
    }

    public int getOpposite(Integer site) {
        if (site == source) return target;
        if (site == target) return source;
        throw new LogicException("Cannot get opposite from site not touching river");
    }
}
