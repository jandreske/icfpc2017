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
        for (River river : freeRivers) {
            //TODO: if we have something touching both sides, check whether they are already connected
            if (state.getOwnRiversTouching(river.getSource()).size() > 0) return river;
            if (state.getOwnRiversTouching(river.getTarget()).size() > 0) return river;
        }

        //nothing cool found, take any free one
        return freeRivers.iterator().next();
    }
}
