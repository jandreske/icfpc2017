package solvers;

import io.Future;
import io.River;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import state.GameState;

import java.util.List;
import java.util.Set;

public class MineConnectClaimer implements Solver {

    private static final Logger LOG = LoggerFactory.getLogger(MineConnectClaimer.class);

    @Override
    public River getNextMove(GameState state) {
        Set<Integer> mines = state.getMap().getMines();

        for (int mineS : mines) {
            for (int mineT : mines) {
                if (mineS == mineT) continue;
                if (state.canReach(state.getMyPunterId(), mineS, mineS)) continue;
                List<River> path = state.getShortestOpenRoute(state.getMyPunterId(), mineS, mineT);
                if (path.size() > 0) return path.get(0);
            }
        }


        Set<River> freeRivers = state.getUnclaimedRivers();
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
        return new Future[0];
    }

}
