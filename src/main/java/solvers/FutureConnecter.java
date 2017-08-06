package solvers;

import io.Future;
import io.Move;
import io.River;
import state.GameState;

import java.util.HashSet;
import java.util.Set;

public class FutureConnecter implements Solver {

    private Solver connector = new MineConnectClaimer();
    private Move bestChoice = null;
    private final int risk;

    public FutureConnecter(int riskLevel) {
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
        Set<Integer> mines = state.getMines();

        Set<River> rivers = state.getRiversTouching(mine);
        if (rivers.isEmpty()) return null;
        int site1 = -1;
        long score1 = 0;
        for (River river : rivers) {
            int opposite = river.getOpposite(mine);
            if (mines.contains(opposite)) continue;
            long degree = state.getDegree(opposite);
            if (site1 == -1 || degree  > score1) {
                site1 = opposite;
                score1 = degree;
            }
        }
        if (risk == 1) return new Future(mine, site1);

        rivers = state.getRiversTouching(site1);
        int site2 = -1;
        long score2 = 0;
        for (River river : rivers) {
            int opposite = river.getOpposite(site1);
            if (mines.contains(opposite)) continue;
            if (state.getShortestRoute(mine, opposite).size() <= 1) continue;
            long degree = state.getDegree(opposite);
            if (site2 == -1 || degree  > score2) {
                site2 = opposite;
                score2 = degree;
            }
        }
        if (site2 == -1) return new Future(mine, site1);
        if (risk == 2) return new Future(mine, site2);

        rivers = state.getRiversTouching(site2);
        int site3 = -1;
        long score3 = 0;
        for (River river : rivers) {
            int opposite = river.getOpposite(site2);
            if (mines.contains(opposite)) continue;
            if (state.getShortestRoute(mine, opposite).size() <= 2) continue;
            long degree = state.getDegree(opposite);
            if (site3 == -1 || degree  > score3) {
                site3 = opposite;
                score3 = degree;
            }
        }
        if (site3 == -1) return new Future(mine, site2);
        if (risk == 3) return new Future(mine, site3);

        rivers = state.getRiversTouching(site3);
        int site4 = -1;
        long score4 = 0;
        for (River river : rivers) {
            int opposite = river.getOpposite(site3);
            if (mines.contains(opposite)) continue;
            if (state.getShortestRoute(mine, opposite).size() <= 3) continue;
            long degree = state.getDegree(opposite);
            if (site4 == -1 || degree  > score4) {
                site4 = opposite;
                score4 = degree;
            }
        }
        if (site4 == -1) return new Future(mine, site3);
        return new Future(mine, site4);
    }

    @Override
    public String getName() {
        return "Future Risk " + risk;
    }

    @Override
    public synchronized Move getBestChoice() {
        return bestChoice == null ? connector.getBestChoice() : bestChoice;
    }

    private synchronized void setBestChoice(Move move) {
        bestChoice = move;
    }
}
