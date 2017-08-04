package io;

import java.util.Collections;
import java.util.Set;

public class Map {

    private Set<Site> sites;

    private Set<River> rivers;

    private Set<Integer> mines;

    public Map() {}

    public Map(Set<Site> sites, Set<River> rivers, Set<Integer> mines) {
        this.sites = Collections.unmodifiableSet(sites);
        this.rivers = Collections.unmodifiableSet(rivers);
        this.mines = Collections.unmodifiableSet(mines);
    }

    public Set<Site> getSites() {
        return sites;
    }

    public Set<River> getRivers() {
        return rivers;
    }

    public Set<Integer> getMines() {
        return mines;
    }
}
