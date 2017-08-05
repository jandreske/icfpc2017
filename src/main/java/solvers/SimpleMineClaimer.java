package solvers;

import io.Future;
import io.River;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import state.GameState;

import java.util.Set;

public class SimpleMineClaimer implements Solver {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleMineClaimer.class);

    @Override
    public River getNextMove(GameState state) {
        Set<Integer> mines = state.getMap().getMines();
        Set<River> freeRivers = state.getUnclaimedRivers();
        for (Integer mine : mines) {
            Set<River> mineRivers = state.getRiversTouching(mine);
            for (River mineRiver : mineRivers) {
                if (freeRivers.contains(mineRiver)) {
                    return mineRiver;
                }
            }
        }

        for (River freeRiver : freeRivers) {
            return freeRiver;
        }

        LOG.error("No free rivers available");
        return null;
    }

    @Override
    public Future[] getFutures(GameState state) {
        return new Future[0];
    }

    @Override
    public String getName() {
        return "Simple Mine Claimer";
    }
}
