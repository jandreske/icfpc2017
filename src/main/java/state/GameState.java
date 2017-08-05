package state;

import io.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class GameState {

    private final int myPunterId;

    private final int numPunters;

    private final Map map;

    private final GraphMap graphMap;

    public GameState(Setup.Request setup) {
        myPunterId = setup.getPunter();
        numPunters = setup.getPunters();
        map = setup.getMap();
        graphMap = new GraphMap(map.getSites(), map.getRivers());
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

    public Set<River> getUnclaimedRiversTouching(int siteId) {
        return map.getRivers().stream()
                .filter(r -> r.touches(siteId) && !r.isClaimed())
                .collect(Collectors.toSet());
    }

    public Set<River> getOwnRiversTouching(int siteId) {
        return map.getRivers().stream()
                .filter(r -> r.getOwner() == myPunterId && r.touches(siteId))
                .collect(Collectors.toSet());
    }

    public Set<River> getOwnRivers() {
        return getRiversByOwner(myPunterId);
    }

    public Set<River> getRiversByOwner(int punter) {
        return map.getRivers().stream()
                .filter(r -> r.getOwner() == punter)
                .collect(Collectors.toSet());
    }

    public Optional<River> getRiver(int source, int target) {
        return map.getRivers().stream()
                .filter(r -> (r.getSource() == source && r.getTarget() == target)
                          || (r.getSource() == target && r.getTarget() == source))
                .findAny();
    }

    /**
     * Update game state with move.
     * @return {@code true} if a new river was claimed,
     *  {@code false} if the move was a pass
     */
    public boolean applyMove(Move move) {
        Move.ClaimData claim = move.getClaim();
        if (claim != null) {
            River river = getRiver(claim.source, claim.target).get();
            if (river.isClaimed() && claim.punter != myPunterId) {
                throw new LogicException("river " + river + " claimed by " + claim.punter + " but already owned by " + river.getOwner());
            }
            river.setOwner(claim.punter);
            return true;
        }
        return false;
    }

    /**
     * Can punter reach site1 from site2 using his claimed rivers?
     */
    public boolean canReach(int punter, int site1, int site2) {
        GraphMap punterMap = new GraphMap(map.getSites(), getRiversByOwner(punter));
        return punterMap.hasRoute(site1, site2);
    }

    public int getScore(int punter) {
        GraphMap punterMap = new GraphMap(map.getSites(), getRiversByOwner(punter));
        return map.getMines().stream()
                .mapToInt(mine -> getScore(punterMap, mine))
                .sum();
    }

    public int getScore(GraphMap punterMap) {
        return map.getMines().stream()
                .mapToInt(mine -> getScore(punterMap, mine))
                .sum();
    }

    private int getScore(GraphMap punterMap, int mine) {
        return map.getSites().stream()
                .mapToInt(site -> getScore(punterMap, mine, site))
                .sum();
    }

    private int getScore(GraphMap punterMap, int mine, Site site) {
        if (site.getId() == mine || !punterMap.hasRoute(mine, site.getId())) {
            return 0;
        }
        int shortest = graphMap.getShortestRouteLength(mine, site.getId());
        return shortest * shortest;
    }

    public List<River> getShortestRoute(int site1, int site2) {
        return graphMap.getShortestRoute(site1, site2);
    }

    public List<River> getShortestOpenRoute(int punterId, int site1, int site2) {
        Set<River> rivers = getRiversByOwner(punterId);
        rivers.addAll(getUnclaimedRivers());
        GraphMap punterMap = new GraphMap(map.getSites(), rivers);
        return punterMap.getShortestRoute(site1, site2);
    }

    public int getPotentialPoints(River river) {
        if (river.isClaimed()) return 0;
        int currentScore = getScore(myPunterId);
        Set<River> rivers = getRiversByOwner(myPunterId);
        rivers.add(river);
        GraphMap newMap = new GraphMap(map.getSites(), rivers);
        int newScore = getScore(newMap);
        return newScore - currentScore;
    }
}
