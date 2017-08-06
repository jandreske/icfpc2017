package solvers;

import io.Future;
import io.Move;
import io.River;
import state.GameState;

import java.util.Set;

public class MaxPointClaimer implements Solver {

    private Move bestChoice = null;

    @Override
    public Move getNextMove(GameState state) {
        Set<River> freeRivers = state.getUnclaimedRivers();
        setBestChoice(Move.claim(state.getMyPunterId(), freeRivers.iterator().next()));

        River best = null;
        int bestPoints = 0;
        for (River river : freeRivers) {

            //only one side is connected, we consider this one
            int points = state.getPotentialPoints(river);
            if (best == null || points > bestPoints) {
                best = river;
                bestPoints = points;
                setBestChoice(Move.claim(state.getMyPunterId(), river));
            }
        }

        return Move.claim(state.getMyPunterId(), best);
    }

    @Override
    public Future[] getFutures(GameState state) {
        return new Future[0];
    }

    @Override
    public String getName() {
        return "Max Point Claimer";
    }

    @Override
    public synchronized Move getBestChoice() {
        return bestChoice;
    }

    private synchronized void setBestChoice(Move move) {
        this.bestChoice = move;
    }

}
