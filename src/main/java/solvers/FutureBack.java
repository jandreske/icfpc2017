package solvers;

import io.Future;
import io.Move;
import state.GameState;

import java.util.HashSet;
import java.util.Set;

public class FutureBack implements Solver {

    private Solver connector = new MineConnectClaimer();
    private Move bestChoice = null;
    private final int risk;

    public FutureBack(int riskLevel) {
        this.risk = riskLevel;
    }

    @Override
    public Move getNextMove(GameState state) {
        setBestChoice(Move.claim(state.getMyPunterId(), state.getUnclaimedRivers().iterator().next()));
        Future best = null;
        int shortest = Integer.MAX_VALUE;
        if (state.getFutures() != null) {
            for (Future future : state.getFutures()) {
                if (Thread.currentThread().isInterrupted()) return null;
                if (state.isFutureComplete(future)) continue;
                int steps = state.missingStepsForFuture(future);
                if (steps == -1) continue;
                if (best == null || steps < shortest) {
                    shortest = steps;
                    best = future;
                }
            }
            if (best != null) return Move.claim(state.getMyPunterId(), state.nextStepForFuture(best));
        }

        setBestChoice(null);
        return connector.getNextMove(state);
    }

    @Override
    public Future[] getFutures(GameState state) {
        Set<Future> futures = new HashSet<>();
        for (int mine : state.getMines()) {
            Future future = getFuture(mine, state);
            if (future != null) futures.add(future);
        }
        return futures.toArray(new Future[futures.size()]);
    }

    private Future getFuture(int mine, GameState state) {
        if (risk == 0) return null;
        for (int site : state.getSites()) {
            if (state.isMine(site)) continue;
            if (state.getShortestRoute(mine, site).size() == risk) {
                return new Future(mine, site);
            }
        }
        return null;
    }

    @Override
    public String getName() {
        return "Future Back " + risk;
    }

    @Override
    public synchronized Move getBestChoice() {
        return bestChoice == null ? connector.getBestChoice() : bestChoice;
    }

    private synchronized void setBestChoice(Move move) {
        bestChoice = move;
    }
}
