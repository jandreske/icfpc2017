package solvers;

import io.Future;
import io.River;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import state.GameState;

import java.util.Set;

public class ExpandingMineClaimer implements Solver {

    private static final Logger LOG = LoggerFactory.getLogger(ExpandingMineClaimer.class);

    @Override
    public River getNextMove(GameState state) {
        Set<Integer> mines = state.getMap().getMines();
        Set<River> freeRivers = state.getUnclaimedRivers();

        //highest priority: if we cann connect to unconnected own sub graphs, we do it
        for (River river : freeRivers) {
            boolean connectedSource = (state.getOwnRiversTouching(river.getSource()).size() > 0);
            if (!connectedSource) continue;
            boolean connectedTarget = (state.getOwnRiversTouching(river.getTarget()).size() > 0);
            if (!connectedTarget) continue;

            if (!state.canReach(state.getMyPunterId(), river.getSource(), river.getTarget())) {
                return river;
            }
        }

        for (Integer mine : mines) {
            Set<River> ownRivers = state.getOwnRiversTouching(mine);
            //if we already have a river at that mine, we dont want another one
            if (ownRivers.size() > 0) continue;
            Set<River> mineRivers = state.getUnclaimedRiversTouching(mine);
            // if there is nothing free at that mine, continue
            if (mineRivers.isEmpty()) continue;
            //we want the free river with most expansion options
            River best = null;
            int expansionOptions = 0;
            for (River river : mineRivers) {
                int otherSite = river.getOpposite(mine);
                int opts = state.getUnclaimedRiversTouching(otherSite).size();
                if (best == null || opts > expansionOptions) {
                    best = river;
                    expansionOptions = opts;
                }
            }
            return best;
        }

        //if we dont want a river at a mine, lets expand
        //lets check all the free ones and pick one
        River best = null;
        int bestPoints = 0;
        for (River river : freeRivers) {
            boolean connectedSource = (state.getOwnRiversTouching(river.getSource()).size() > 0);
            boolean connectedTarget = (state.getOwnRiversTouching(river.getTarget()).size() > 0);

            //ignore those unconnected to our current rivers
            if (!connectedSource && ! connectedTarget) continue;

            if (connectedSource && connectedTarget) {
                // both sides touch one of ours - if they are already connected, ignore
                if (state.canReach(state.getMyPunterId(), river.getSource(), river.getTarget())) continue;
            }

            //only one side is connected, we consider this one
            int points = state.getPotentialPoints(river);
            if (best == null || points > bestPoints) {
                best = river;
                bestPoints = points;
            }
        }

        if (best != null) return best;
        return freeRivers.iterator().next();
    }

    @Override
    public Future[] getFutures(GameState state) {
        return new Future[]{new Future(1, 6)};
    }

}
