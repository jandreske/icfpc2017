package solvers;

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

            }
        }

        for (River freeRiver : freeRivers) {
            return freeRiver;
        }

        LOG.error("No free rivers available");
        return null;
    }
}
