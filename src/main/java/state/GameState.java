package state;

import io.*;

import java.util.Collections;
import java.util.HashMap;

public class GameState {

    private final int myPunterId;

    private final int numPunters;

    private final Map map;

    private final java.util.Map<River, Integer> rivers;

    public GameState(Setup.Request setup) {
        myPunterId = setup.getPunter();
        numPunters = setup.getPunters();
        map = setup.getMap();
        rivers = new HashMap<>(map.getRivers().size());
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

    /**
     * Rivers and their owners.
     */
    public java.util.Map<River, Integer> getRivers() {
        return Collections.unmodifiableMap(rivers);
    }

    /**
     * Determine the owner of a given river
     * @return The id of the owner, or -1 if the river is not owned by anyone
     */
    public int getOwner(River river) {
        Integer owner = rivers.get(river);
        if (owner == null) {
            return -1;
        }
        return owner;
    }

    /**
     * Update game state with move.
     * @return {@code true} if a new river was claimed,
     *  {@code false} if the move was a pass
     */
    public boolean applyMove(Move move) {
        Claim.Data claim = move.getClaim();
        if (claim != null) {
            River river = new River(claim.source, claim.target);
            Integer owner = rivers.get(river);
            if (owner != null) {
                throw new LogicException("river " + river + " claimed by " + claim.punter + " but already owned by " + owner);
            }
            rivers.put(river, claim.punter);
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
