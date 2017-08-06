package io;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Site without coordinates.
 * For serialization only, not used anywhere else.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Site {

    private final int id;

    @JsonCreator
    public Site(@JsonProperty("id") int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String toString() {
        return Integer.toString(id);
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
