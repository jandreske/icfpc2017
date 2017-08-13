package state;

import io.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.Transient;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Game state with roughly the same representation as in the setup message.
 * Does not store precomputed information.
 */
class MapBasedGameState implements GameState {
    private static final Logger LOG = LoggerFactory.getLogger(MapBasedGameState.class);

    private int myPunterId;

    private int numPunters;

    private int optionsUsed = 0;

    private int movesPerformed = 0;

    private Set<Integer> sites;
    private Set<River> rivers;
    private Set<Integer> mines;

    private final java.util.Map<Integer, Integer> credits = new HashMap<>();

    public java.util.Map<Integer, Integer> getCredits() {
        return credits;
    }

    private GraphMap graphMap;
    private GraphMap[] graphMapByPunter = null;
    private Future[] futures;
    private Settings settings;

    private int score = -1;

    public MapBasedGameState() {}

    public MapBasedGameState(Setup.Request setup) {
        myPunterId = setup.getPunter();
        numPunters = setup.getPunters();
        sites = setup.getMap().getSites().stream().map(Site::getId).collect(Collectors.toSet());
        rivers = setup.getMap().getRivers();
        mines = setup.getMap().getMines();
        settings = setup.getSettings();
        graphMap = null;
    }

    @Override
    public Settings getSettings() {
        if (settings == null) {
            settings = new Settings();
        }
        return settings;
    }

    @Transient
    GraphMap getGraphMap() {
        if (graphMap == null) {
            graphMap = new GraphMap(getSites(), getRivers());
        }
        return graphMap;
    }

    private GraphMap getGraphMap(int punter) {
        if (graphMapByPunter == null) {
            graphMapByPunter = new GraphMap[getNumPunters()];
        }
        if (graphMapByPunter[punter] == null) {
            graphMapByPunter[punter] = new GraphMap(getSites(), getRiversByOwner(punter));
        }
        return graphMapByPunter[punter];
    }

    @Override
    public int getMyPunterId() {
        return myPunterId;
    }

    @Override
    public int getNumPunters() {
        return numPunters;
    }

    @Override
    public int getMovesPerformed() {
        return movesPerformed;
    }

    @Override
    public int getOptionsUsed() {
        return optionsUsed;
    }

    @Override
    public void movePerformed() {
        movesPerformed++;
    }

    @Transient
    @Override
    public int getNumRivers() {
        return getRivers().size();
    }

    @Override
    public Set<Integer> getSites() {
        return sites;
    }

    @Override
    @Transient
    public Set<River> getAvailableOptions() {
        int punter = getMyPunterId();
        return getRivers().stream()
                .filter(r -> r.canOption(punter))
                .collect(Collectors.toSet());
    }

    @Override
    @Transient
    public Set<River> getUnclaimedRivers() {
        return getRivers().stream()
                .filter(r -> !r.isClaimed())
                .collect(Collectors.toSet());
    }

    @Override
    public Set<River> getRiversTouching(int siteId) {
        return getRivers().stream()
                .filter(r -> r.touches(siteId))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<River> getUnclaimedRiversTouching(int siteId) {
        return getRivers().stream()
                .filter(r -> r.touches(siteId) && !r.isClaimed())
                .collect(Collectors.toSet());
    }

    @Override
    public Set<River> getOpenRiversTouching(int siteId) {
        if (!areOptionsActive() || getRemainingOptions() == 0) return getUnclaimedRiversTouching(siteId);
        return getRivers().stream()
                .filter(r -> r.touches(siteId) && (!r.isClaimed() || r.canOption(myPunterId)))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<River> getOwnRiversTouching(int siteId) {
        return getRivers().stream()
                .filter(r -> r.canUse(myPunterId) && r.touches(siteId))
                .collect(Collectors.toSet());
    }

    @Override
    public int getDegree(int siteId) {
        return (int) getRivers().stream()
                .filter(r -> r.touches(siteId))
                .count();
    }

    @Override
    public Set<River> getRiversByOwner(int punter) {
        return getRivers().stream()
                .filter(r -> r.canUse(punter))
                .collect(Collectors.toSet());
    }

    /**
     * Find a river with the given end sites.
     * The order of source and target does not matter.
     */
    private Optional<River> getRiver(int source, int target) {
        return getRivers().stream()
                .filter(r -> (r.getSource() == source && r.getTarget() == target)
                          || (r.getSource() == target && r.getTarget() == source))
                .findAny();
    }

    private void invalidateCaches(int punter) {
        if (punter == getMyPunterId()) {
            score = -1;
        }
        if (graphMapByPunter != null) {
            graphMapByPunter[punter] = null;
        }
    }

    @Override
    public void applyMoves(List<Move> moves) {
        if (moves.size() > numPunters) {
            throw new LogicException("too many previous moves: " + moves.size());
        }
        for (int i = 0; i < numPunters; i++) {
            int pid = (myPunterId + i) % numPunters;
            moves.stream().filter(m -> m.getPunter() == pid)
                    .findAny()
                    .ifPresent(this::applyMove);
        }
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
            if (river.isClaimed()) {
                throw new LogicException("river " + river + " claimed by " + claim.punter + " but already owned by " + river.getOwner());
            }
            river.setOwner(claim.punter);
            invalidateCaches(claim.punter);
            return true;
        }
        Move.SplurgeData splurge = move.getSplurge();
        if (splurge != null) {
            if (!getSettings().isSplurges()) {
                throw new LogicException("splurges not enabled: " + splurge);
            }
            int punter = splurge.punter;
            int cred = credits.getOrDefault(punter, 0) + 1;
            int n = splurge.route.size();
            for (int i = 1; i < n; i++) {
                River river = getRiver(splurge.route.get(i-1), splurge.route.get(i)).get();
                if (!river.isClaimed()) {
                    river.setOwner(punter);
                } else if (!takeOption(river, punter)) {
                    throw new LogicException("river " + river + " not eligible for splurge");
                }
                cred--;
            }
            credits.put(punter, cred);
            invalidateCaches(punter);
            return true;
        }
        Move.ClaimData option = move.getOption();
        if (option != null) {
            if (!getSettings().isOptions()) {
                throw new LogicException("options not enabled: " + option);
            }
            River river = getRiver(option.source, option.target).get();
            if (!takeOption(river, option.punter)) {
                throw new LogicException("river " + river + " not eligible for option");
            }
            invalidateCaches(option.punter);
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

    private boolean takeOption(River river, int punter) {
        if (river.canOption(punter)) {
            river.setOption(punter);
            if (punter == myPunterId) optionsUsed++;
            return true;
        }
        return false;
    }

    /**
     * Can punter reach site1 from site2 using his claimed rivers?
     */
    @Override
    public boolean canReach(int punter, int site1, int site2) {
        GraphMap punterMap = getGraphMap(punter);
        return punterMap.hasRoute(site1, site2);
    }

    @Override
    public boolean canReachMine(int punter, int site) {
        GraphMap punterMap = getGraphMap(punter);
        return getMines().stream()
                .anyMatch(mine -> punterMap.hasRoute(site, mine));
    }

    @Override
    public int getScore(int punter) {
        GraphMap punterMap = getGraphMap(punter);
        return getScore(punterMap);
    }

    private int getScore(GraphMap map) {
        int score = 0;
        for (int mine : getMines()) {
            if (map.containsSite(mine)) {
                ArrayNatMap<Integer> reach = map.getAllShortestRouteLengths(mine);
                ArrayNatMap<Integer> dists = getGraphMap().getAllShortestRouteLengths(mine);
                for (int site : getSites()) {
                    if (site != mine && reach.containsKey(site)) {
                        int shortest = dists.get(site);
                        score += shortest * shortest;
                    }
                }
            }
        }
        return score;
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
        if (settings != null && settings.isOptions() && this.getRemainingOptions() > 0) {
            List<River> rivers = getRivers().stream()
                    .filter(r -> r.canUse(punterId) || !r.isClaimed() || r.canOption(punterId))
                    .collect(Collectors.toList());
            GraphMap punterMap = new GraphMap(getSites(), rivers);
            List<River> path = punterMap.getShortestRoute(site1, site2);
            if (path.stream().filter(river -> river.isClaimed() && river.canOption(punterId)).count() <= this.getRemainingOptions()) {
                return path;
            }
        }
        List<River> rivers = getRivers().stream()
                .filter(r -> r.canUse(punterId) || !r.isClaimed())
                .collect(Collectors.toList());
        GraphMap punterMap = new GraphMap(getSites(), rivers);
        return punterMap.getShortestRoute(site1, site2);
    }

    @Override
    public int getPotentialPoints(River river) {
        if (river.isClaimed()) return 0;
        //if we did not calculate score since last own move, do it now
        if (score < 0) {
            score = getScore(myPunterId);
        }
        Set<River> rivers = getRiversByOwner(myPunterId);
        rivers.add(river);
        GraphMap newMap = new GraphMap(getSites(), rivers);
        int newScore = getScore(newMap);
        return newScore - score;
    }

    @Override
    public int getPotentialPoints(River river1, River river2) {
        if (river1.isClaimed() || river2.isClaimed()) return 0;
        //if we did not calculate score since last own move, do it now
        if (score < 0) {
            score = getScore(myPunterId);
        }
        Set<River> rivers = getRiversByOwner(myPunterId);
        rivers.add(river1);
        rivers.add(river2);
        GraphMap newMap = new GraphMap(getSites(), rivers);
        int newScore = getScore(newMap);
        return newScore - score;
    }

    @Override
    public Set<Integer> getMines() {
        return mines;
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
        return (int) path.stream().filter(river -> !river.isClaimed()
                || (areOptionsActive() && river.canOption(myPunterId))).count();
    }

    @Override
    public River nextStepForFuture(Future future) {
        List<River> path = getShortestOpenRoute(myPunterId, future.getSource(), future.getTarget());
        for (River river : path) {
            if (!river.isClaimed() || (areOptionsActive() && river.canOption(myPunterId))) return river;
        }
        return null;
    }

    /**
     * Ist this site the source or target of any river usable by the punter?
     */
    @Override
    public boolean isOnRiver(int punter, int site) {
        return getRivers().stream()
                .anyMatch(r -> r.canUse(punter) && r.touches(site));
    }

    @Override
    public int getSplurgeCredits(int punterId) {
        return credits.get(punterId);
    }

    public Set<River> getRivers() {
        return rivers;
    }
}
