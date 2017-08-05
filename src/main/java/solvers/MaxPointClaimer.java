package solvers;

import io.Future;
import io.River;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import state.GameState;

import java.util.Set;

public class MaxPointClaimer implements Solver {

    private static final Logger LOG = LoggerFactory.getLogger(MaxPointClaimer.class);

    @Override
    public River getNextMove(GameState state) {
        Set<River> freeRivers = state.getUnclaimedRivers();

        River best = null;
        int bestPoints = 0;
        for (River river : freeRivers) {

            //only one side is connected, we consider this one
            int points = state.getPotentialPoints(river);
            if (best == null || points > bestPoints) {
                best = river;
                bestPoints = points;
            }
        }

        return best;
    }

    @Override
    public Future[] getFutures(GameState state) {
        return new Future[0];
    }

}
