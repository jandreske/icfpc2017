package solvers;

import io.Future;
import io.River;
import io.Site;
import state.GameState;

import java.util.HashSet;
import java.util.Set;

public class FutureBack implements Solver {

    private Solver connector = new MineConnectClaimer();
    private River bestChoice = null;
    private final int risk;

    public FutureBack(int riskLevel) {
        this.risk = riskLevel;
    }

    @Override
    public River getNextMove(GameState state) {
        setBestChoice(state.getUnclaimedRivers().iterator().next());
        Future best = null;
        int shortest = Integer.MAX_VALUE;
        if (state.getFutures() != null) {
            for (Future future : state.getFutures()) {
                if (state.isFutureComplete(future)) continue;
                int steps = state.missingStepsForFuture(future);
                if (steps == -1) continue;
                if (best == null || steps < shortest) {
                    shortest = steps;
                    best = future;
                }
            }
            if (best != null) return state.nextStepForFuture(best);
        }

        setBestChoice(null);
        return connector.getNextMove(state);
    }

    @Override
    public Future[] getFutures(GameState state) {
        Set<Future> futures = new HashSet<>();
        for (int mine : state.getMap().getMines()) {
            Future future = getFuture(mine, state);
            if (future != null) futures.add(future);
        }
        return futures.toArray(new Future[futures.size()]);
    }

    private Future getFuture(int mine, GameState state) {
        if (risk == 0) return null;
        Set<Integer> mines = state.getMap().getMines();
        for (Site site : state.getMap().getSites()) {
            if (mines.contains(site.getId())) continue;
            if (state.getShortestRoute(mine, site.getId()).size() == risk) {
                return new Future(mine, site.getId());
            }
        }
        return null;
    }

    @Override
    public String getName() {
        return "Future Back " + risk;
    }

    @Override
    public synchronized River getBestChoice() {
        return bestChoice == null ? connector.getBestChoice() : bestChoice;
    }

    private synchronized void setBestChoice(River river) {
        bestChoice = river;
    }
}
