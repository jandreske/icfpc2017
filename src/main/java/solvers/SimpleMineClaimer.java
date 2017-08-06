package solvers;

import io.Future;
import io.Move;
import io.River;
import state.GameState;

import java.util.Set;

public class SimpleMineClaimer implements Solver {


    private Move bestChoice = null;

    @Override
    public Move getNextMove(GameState state) {
        Set<Integer> mines = state.getMines();
        Set<River> freeRivers = state.getUnclaimedRivers();
        setBestChoice(Move.claim(state.getMyPunterId(), freeRivers.iterator().next()));
        for (Integer mine : mines) {
            if (Thread.currentThread().isInterrupted()) return null;
            Set<River> mineRivers = state.getRiversTouching(mine);
            for (River mineRiver : mineRivers) {
                if (freeRivers.contains(mineRiver)) {
                    return Move.claim(state.getMyPunterId(), mineRiver);
                }
            }
        }

        return Move.claim(state.getMyPunterId(), freeRivers.iterator().next());
    }

    @Override
    public Future[] getFutures(GameState state) {
        return new Future[0];
    }

    @Override
    public String getName() {
        return "Simple Mine Claimer";
    }

    @Override
    public synchronized Move getBestChoice() {
        return bestChoice;
    }

    private synchronized void setBestChoice(Move move) {
        this.bestChoice = move;
    }
}
