package state;

import io.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class GameState {

    private final int myPunterId;

    private final int numPunters;

    private final Map map;

    public GameState(Setup.Request setup) {
        myPunterId = setup.getPunter();
        numPunters = setup.getPunters();
        map = setup.getMap();
    }

    public int getMyPunterId() {
        return myPunterId;
    }

    public int getNumPunters() {
        return numPunters;
    }

    public Map getMap() {
        return map;
    }

    public Set<River> getUnclaimedRivers() {
        return map.getRivers().stream()
                .filter(r -> !r.isClaimed())
                .collect(Collectors.toSet());
    }

    public Set<River> getRiversTouching(int siteId) {
        return map.getRivers().stream()
                .filter(r -> r.touches(siteId))
                .collect(Collectors.toSet());
    }

    public Set<River> getOwnRiversTouching(int siteId) {
        return map.getRivers().stream()
                .filter(r -> r.getOwner() == myPunterId && r.touches(siteId))
                .collect(Collectors.toSet());
    }

    public Set<River> getOwnRivers() {
        return map.getRivers().stream()
                .filter(r -> r.getOwner() == myPunterId)
                .collect(Collectors.toSet());
    }

    public Optional<River> getRiver(int source, int target) {
        return map.getRivers().stream()
                .filter(r -> r.getSource() == source && r.getTarget() == target)
                .findAny();
    }

    /**
     * Update game state with move.
     * @return {@code true} if a new river was claimed,
     *  {@code false} if the move was a pass
     */
    public boolean applyMove(Move move) {
        Claim.Data claim = move.getClaim();
        if (claim != null) {
            River river = getRiver(claim.source, claim.target).get();
            if (river.isClaimed()) {
                throw new LogicException("river " + river + " claimed by " + claim.punter + " but already owned by " + river.getOwner());
            }
            river.setOwner(claim.punter);
            return true;
        }
        return false;
    }

    public int getScore(int punter) {
        return map.getMines().stream()
                .mapToInt(mine -> getScore(punter, mine))
                .sum();
    }

    private int getScore(int punter, int mine) {
        return map.getSites().stream()
                .mapToInt(site -> getScore(punter, mine, site))
                .sum();
    }

    private int getScore(int punter, int mine, Site site) {
        throw new LogicException("score not yet implemented");
    }

}
