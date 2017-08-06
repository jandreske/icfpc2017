package state;

import io.*;

import java.beans.Transient;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

class MapBasedGameState implements GameState {

    private int myPunterId;

    private int numPunters;

    private java.util.Map<Integer, Integer> credits = new HashMap<>();

    private Map map;

    private GraphMap graphMap;
    private Future[] futures;
    private Settings settings;

    public MapBasedGameState() {}

    private int score = -1;

    public MapBasedGameState(Setup.Request setup) {
        myPunterId = setup.getPunter();
        numPunters = setup.getPunters();
        map = setup.getMap();
        settings = setup.getSettings();
        graphMap = null;
    }

    public Map getMap() {
        return map;
    }

    @Transient
    private GraphMap getGraphMap() {
        if (graphMap == null) {
            graphMap = new GraphMap(getSites(), map.getRivers());
        }
        return graphMap;
    }

    @Override
    public int getMyPunterId() {
        return myPunterId;
    }

    @Override
    public int getNumPunters() {
        return numPunters;
    }

    @Transient
    @Override
    public int getNumRivers() {
        return map.getRivers().size();
    }

    @Transient
    @Override
    public Set<Integer> getSites() {
        return map.getSites().stream().map(Site::getId).collect(Collectors.toSet());
    }

    @Override
    @Transient
    public Set<River> getUnclaimedRivers() {
        return map.getRivers().stream()
                .filter(r -> !r.isClaimed())
                .collect(Collectors.toSet());
    }

    @Override
    public Set<River> getRiversTouching(int siteId) {
        return map.getRivers().stream()
                .filter(r -> r.touches(siteId))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<River> getUnclaimedRiversTouching(int siteId) {
        return map.getRivers().stream()
                .filter(r -> r.touches(siteId) && !r.isClaimed())
                .collect(Collectors.toSet());
    }

    @Override
    public Set<River> getOwnRiversTouching(int siteId) {
        return map.getRivers().stream()
                .filter(r -> r.getOwner() == myPunterId && r.touches(siteId))
                .collect(Collectors.toSet());
    }

    @Override
    public int getDegree(int siteId) {
        return (int) map.getRivers().stream()
                .filter(r -> r.touches(siteId))
                .count();
    }

    @Override
    public Set<River> getRiversByOwner(int punter) {
        return map.getRivers().stream()
                .filter(r -> r.getOwner() == punter)
                .collect(Collectors.toSet());
    }

    @Override
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
    @Override
    public boolean applyMove(Move move) {
        Move.ClaimData claim = move.getClaim();
        if (claim != null) {
            River river = getRiver(claim.source, claim.target).get();
            //TODO can this stay?
            if (river.isClaimed() && claim.punter != myPunterId) {
                throw new LogicException("river " + river + " claimed by " + claim.punter + " but already owned by " + river.getOwner());
            }
            river.setOwner(claim.punter);
            //reset score cache
            if (claim.punter == myPunterId) score = -1;
            return true;
        }
        Move.SplurgeData splurge = move.getSplurge();
        if (splurge != null) {
            int punter = splurge.punter;
            int cred = credits.getOrDefault(punter, 0) + 1;
            int n = splurge.route.size();
            for (int i = 1; i < n; i++) {
                River river = getRiver(splurge.route.get(i-1), splurge.route.get(i)).get();
                if (river.isClaimed() && punter != myPunterId) {
                    throw new LogicException("river " + river + " claimed by " + punter + " but already owned by " + river.getOwner());
                }
                river.setOwner(splurge.punter);
                cred--;
            }
            credits.put(punter, cred);
            //reset score cache
            if (splurge.punter == myPunterId) score = -1;
            return true;
        }
        Move.PassData pass = move.getPass();
        if (pass != null) {
            //TODO this will give false credits at game start (we start with -1 for now)
            int punter = pass.punter;
            int cred = credits.getOrDefault(punter, -1);
            cred++;
            credits.put(punter, cred);
        }
        return false;
    }

    /**
     * Can punter reach site1 from site2 using his claimed rivers?
     */
    @Override
    public boolean canReach(int punter, int site1, int site2) {
        GraphMap punterMap = new GraphMap(getSites(), getRiversByOwner(punter));
        return punterMap.hasRoute(site1, site2);
    }

    @Override
    public boolean canReachMine(int punter, int site) {
        GraphMap punterMap = new GraphMap(getSites(), getRiversByOwner(punter));
        return map.getMines().stream()
                .anyMatch(mine -> punterMap.hasRoute(site, mine));
    }

    @Override
    public int getScore(int punter) {
        GraphMap punterMap = new GraphMap(getSites(), getRiversByOwner(punter));
        return map.getMines().stream()
                .mapToInt(mine -> getScore(punterMap, mine))
                .sum();
    }

    private int getScore(GraphMap punterMap) {
        return map.getMines().stream()
                .mapToInt(mine -> getScore(punterMap, mine))
                .sum();
    }

    private int getScore(GraphMap punterMap, int mine) {
        return getSites().stream()
                .mapToInt(site -> getScore(punterMap, mine, site))
                .sum();
    }

    private int getScore(GraphMap punterMap, int mine, int site) {
        if (site == mine || !punterMap.hasRoute(mine, site)) {
            return 0;
        }
        int shortest = getShortestRouteLength(mine, site);
        return shortest * shortest;
    }

    @Override
    public int getShortestRouteLength(int site1, int site2) {
        return getGraphMap().getShortestRouteLength(site1, site2);
    }

    @Override
    public List<River> getShortestRoute(int site1, int site2) {
        return getGraphMap().getShortestRoute(site1, site2);
    }

    @Override
    public List<River> getShortestOpenRoute(int punterId, int site1, int site2) {
        Set<River> rivers = getRiversByOwner(punterId);
        rivers.addAll(getUnclaimedRivers());
        GraphMap punterMap = new GraphMap(getSites(), rivers);
        return punterMap.getShortestRoute(site1, site2);
    }

    @Override
    public int getPotentialPoints(River river) {
        if (river.isClaimed()) return 0;
        //if we did not calculate score since last own move, do it now
        if (score == -1) {
            score = getScore(myPunterId);
        }
        Set<River> rivers = getRiversByOwner(myPunterId);
        rivers.add(river);
        GraphMap newMap = new GraphMap(getSites(), rivers);
        int newScore = getScore(newMap);
        return newScore - score;
    }

    @Transient
    @Override
    public Set<Integer> getMines() {
        return map.getMines();
    }

    @Override
    public void setFutures(Future[] futures) {
        this.futures = futures;
    }

    @Override
    public Future[] getFutures() {
        return futures;
    }

    @Override
    public boolean isFutureComplete(Future future) {
        return canReach(myPunterId, future.getSource(), future.getTarget());
    }

    @Override
    public int missingStepsForFuture(Future future) {
        List<River> path = getShortestOpenRoute(myPunterId, future.getSource(), future.getTarget());
        if (path.isEmpty()) return -1;
        return (int) path.stream().filter(river -> !river.isClaimed()).count();
    }

    @Override
    public River nextStepForFuture(Future future) {
        List<River> path = getShortestOpenRoute(myPunterId, future.getSource(), future.getTarget());
        for (River river : path) {
            if (!river.isClaimed()) return river;
        }
        return null;
    }
    
    /**
     * Ist this site the source or target of any river claimed by the punter?
     */
    @Override
    public boolean isOnRiver(int punter, int site) {
        return map.getRivers().stream()
                .anyMatch(r -> r.getOwner() == punter && r.touches(site));
    }

    @Override
    public boolean areFuturesActive() {
        return settings.getFutures();
    }

    @Override
    public boolean areSplurgesActive() {
        return settings.isSplurges();
    }

    @Override
    public int getSplurgeCredits(int punterId) {
        return credits.get(punterId);
    }
}
