package state;

import io.Future;
import io.Map;
import io.Move;
import io.River;

import java.beans.Transient;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface GameState {

    int getMyPunterId();

    int getNumPunters();

    int getNumRivers();

    /** Get all site IDs, including mines. */
    Set<Integer> getSites();

    Set<River> getUnclaimedRivers();

    Set<River> getRiversTouching(int siteId);

    Set<River> getUnclaimedRiversTouching(int siteId);

    Set<River> getOwnRiversTouching(int siteId);

    long getDegree(int siteId);

    Set<River> getOwnRivers();

    Set<River> getRiversByOwner(int punter);

    Optional<River> getRiver(int source, int target);

    boolean applyMove(Move move);

    boolean canReach(int punter, int site1, int site2);

    boolean canReachMine(int punter, int site);

    int getScore(int punter);

    int getScore(GraphMap punterMap);

    int getShortestRouteLength(int site1, int site2);

    List<River> getShortestRoute(int site1, int site2);

    List<River> getShortestOpenRoute(int punterId, int site1, int site2);

    int getPotentialPoints(River river);

    // API ideas: best candidate rivers, considering already taken ones
    List<River> getMostPromisingRivers();

    /** Is the given site ID a mine? */
    boolean isMine(int siteId);

    /** Get the set of site IDs that are mines. */
    Set<Integer> getMines();

    void setFutures(Future[] futures);

    Future[] getFutures();

    boolean isFutureComplete(Future future);

    int missingStepsForFuture(Future future);

    River nextStepForFuture(Future future);

    boolean isOnRiver(int punter, int site);
}
