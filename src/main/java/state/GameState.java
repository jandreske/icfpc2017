package state;

import io.*;

import java.beans.Transient;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public interface GameState {

    int getMyPunterId();

    int getNumPunters();

    int getNumRivers();

    /**
     * The total number of moves I have in the current game.
     */
    @Transient
    default int getTotalNumberOfMoves() {
        int numRivers = getNumRivers();
        int numPunters = getNumPunters();
        int punterId = getMyPunterId();
        return (numRivers / numPunters) + ((numRivers % numPunters) > punterId ? 1 : 0);
    }

    /**
     * Number of moves this punter has made in this game.
     */
    int getMovesPerformed();

    /**
     * The number of moves I can still issue in this game.
     */
    @Transient
    default int getRemainingNumberOfMoves() {
        return getTotalNumberOfMoves() - getMovesPerformed();
    }

    /**
     * Increment move number.
     */
    void movePerformed();

    /** Get all site IDs, including mines. */
    Set<Integer> getSites();

    Set<River> getUnclaimedRivers();

    Set<River> getRiversTouching(int siteId);

    default Set<River> getUnclaimedRiversTouching(int siteId) {
        return getRiversTouching(siteId).stream().filter(r -> !r.isClaimed()).collect(Collectors.toSet());
    }

    default Set<River> getOwnRiversTouching(int siteId) {
        return getOwnRivers().stream().filter(r -> r.touches(siteId)).collect(Collectors.toSet());
    }

    default int getDegree(int siteId) {
        return getRiversTouching(siteId).size();
    }

    @Transient
    default Set<River> getOwnRivers() {
        return getRiversByOwner(getMyPunterId());
    }

    /**
     * Get the set of rivers claimed by the given punter.
     */
    Set<River> getRiversByOwner(int punter);

    /**
     * Find a river with the given end sites.
     * The order of source and target does not matter.
     */
    Optional<River> getRiver(int source, int target);

    /**
     * Apply a move to the game state.
     * @return {@code true} if the move changed the state,
     *  {@code false} otherwise (i.e. if the move was a pass)
     */
    boolean applyMove(Move move);

    /** Can a punter go from site1 to site2 using his rivers? */
    boolean canReach(int punter, int site1, int site2);

    /** Can a punter reach at least one mine using his rivers? */
    boolean canReachMine(int punter, int site);

    int getScore(int punter);

    default int getShortestRouteLength(int site1, int site2) {
        return getShortestRoute(site1, site2).size();
    }

    /**
     * Get the shortest route from site1 to site2.
     */
    List<River> getShortestRoute(int site1, int site2);

    /**
     * Get the shortest route from site1 to site2 not using rivers claimed
     * by others than the given punter.
     */
    List<River> getShortestOpenRoute(int punterId, int site1, int site2);

    /**
     * Compute potential score increase from claiming the given river.
     */
    int getPotentialPoints(River river);

    /** Is the given site ID a mine? */
    default boolean isMine(int siteId) {
        return getMines().contains(siteId);
    }

    /** Get the set of site IDs that are mines. */
    Set<Integer> getMines();

    Settings getSettings();

    void setFutures(Future[] futures);

    Future[] getFutures();

    boolean isFutureComplete(Future future);

    int missingStepsForFuture(Future future);

    River nextStepForFuture(Future future);

    /**
     * Is the given site part of a river claimed by the given punter?
     */
    boolean isOnRiver(int punter, int site);

    boolean areFuturesActive();

    boolean areSplurgesActive();

    int getSplurgeCredits(int punter);
}
