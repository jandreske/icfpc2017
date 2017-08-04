package io;

import java.util.Collections;
import java.util.Set;

public class Map {

    private final Set<Site> sites;

    private final Set<River> rivers;

    private final Set<Integer> mines;

    public Map(Set<Site> sites, Set<River> rivers, Set<Integer> mines) {
        this.sites = Collections.unmodifiableSet(sites);
        this.rivers = Collections.unmodifiableSet(rivers);
        this.mines = Collections.unmodifiableSet(mines);
    }

}
