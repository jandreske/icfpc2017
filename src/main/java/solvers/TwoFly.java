package solvers;

import io.Future;
import io.Move;
import io.River;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import state.GameState;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TwoFly implements Solver {
    private Move bestChoice = null;
    private final int risk;
    private final int stepDivider;

    private static final Logger LOG = LoggerFactory.getLogger(TwoFly.class);
    public TwoFly(int riskLevel, int stepDivider) {
        this.risk = riskLevel;
        this.stepDivider = stepDivider;
    }

    @Override
    public Move getNextMove(GameState state) {
        Set<River> freeRivers = state.getUnclaimedRivers();
        setBestChoice(Move.claim(state.getMyPunterId(), state.getUnclaimedRivers().iterator().next()));
        Future bestFuture = null;
        int shortest = Integer.MAX_VALUE;
        if (state.getFutures() != null) {
            for (Future future : state.getFutures()) {
                if (state.isFutureComplete(future)) continue;
                int steps = state.missingStepsForFuture(future);
                if (steps == -1) continue;
                if (bestFuture == null || steps < shortest) {
                    shortest = steps;
                    bestFuture = future;
                }
            }
            if (bestFuture != null) return Move.claim(state.getMyPunterId(), state.nextStepForFuture(bestFuture));
        }

        Set<Integer> mines = state.getMines();
        if (mines.size() == 1) {
            int mine = mines.iterator().next();
            if (state.getOwnRiversTouching(mine).size() < 10) {
                Set<River> rivers = state.getUnclaimedRiversTouching(mine);
                if (!rivers.isEmpty()) {
                    return Move.claim(state.getMyPunterId(), rivers.iterator().next());
                }
            }
        }

        List<River> bestPath = null;
        shortest = Integer.MAX_VALUE;
        for (int mineS : mines) {
            for (int mineT : mines) {
                if (mineS == mineT) continue;
                if (state.canReach(state.getMyPunterId(), mineS, mineT)) continue;
                List<River> path = state.getShortestOpenRoute(state.getMyPunterId(), mineS, mineT);
                if (path.isEmpty()) continue;
                int missing = (int) path.stream().filter(river -> !river.isClaimed()).count();
                if (bestPath == null || missing < shortest) {
                    bestPath = path;
                    shortest = missing;
                }
            }
        }

        if (bestPath != null) {
            for (River river : bestPath) {
                if (!river.isClaimed()) {
                    return Move.claim(state.getMyPunterId(), river);
                }
            }
        }


        River best = null;
        int bestPoints = 0;
        for (River river : freeRivers) {
            boolean connectedSource = (state.getOwnRiversTouching(river.getSource()).size() > 0)
                    || mines.contains(river.getSource());
            boolean connectedTarget = (state.getOwnRiversTouching(river.getTarget()).size() > 0)
                    || mines.contains(river.getTarget());

            //ignore those unconnected to our current rivers
            if (!connectedSource && ! connectedTarget) continue;

            if (connectedSource && connectedTarget) {
                // both sides touch one of ours - if they are already connected, ignore
                if (state.canReach(state.getMyPunterId(), river.getSource(), river.getTarget())) continue;
                //otherwise, take this one - to connect subgraphs
                return Move.claim(state.getMyPunterId(), river);
            }

            //only one side is connected, we consider this one
            int points = state.getPotentialPoints(river);
            if (best == null || points > bestPoints) {
                best = river;
                bestPoints = points;
                setBestChoice(Move.claim(state.getMyPunterId(), river));
            }
        }

        if (best != null) return Move.claim(state.getMyPunterId(), best);
        return Move.claim(state.getMyPunterId(), freeRivers.iterator().next());
    }

    @Override
    public Future[] getFutures(GameState state) {
        int maxTurns = state.getNumRivers() / state.getNumPunters();
        int maxFutureSteps = maxTurns / stepDivider;
        int usedSteps = 0;
        Set<Future> futures = new HashSet<>();
        for (int mine : state.getMines()) {
            if (usedSteps >= maxFutureSteps) break;
            Future future = getFuture(mine, state);
            if (future != null) {
                futures.add(future);
                usedSteps += state.getShortestRouteLength(future.getSource(), future.getTarget());
                LOG.info("Added future ({}->{}) with {} / {} steps used", future.getSource(), future.getTarget(), usedSteps, maxFutureSteps);
            }
        }
        return futures.toArray(new Future[futures.size()]);
    }

    private Future getFuture(int mine, GameState state) {
        if (risk == 0) return null;
        Set<Integer> mines = state.getMines();
        int target = -1;
        int shortest = Integer.MAX_VALUE;
        for (int candidate : mines) {
            if (candidate == mine) continue;
            int distance = state.getShortestRoute(mine, candidate).size();
            if (distance < shortest && distance > 1) {
                target = candidate;
                shortest = distance;
            }
        }
        if (target == -1) return null;
        List<River> path = state.getShortestRoute(mine, target);
        if (risk == -1) {
            int index = path.size() / 2;
            River river = path.get(index);
            River previous = path.get(index - 1);
            int site = river.getTarget();
            if (river.getSource() == previous.getSource() || river.getSource() == previous.getTarget()) {
                site = river.getSource();
            }
            return new Future(mine, site);
        }
        int index = Math.min(risk, path.size() - 1);
        River river = path.get(index);
        River previous = path.get(index - 1);
        int site = river.getTarget();
        if (river.getSource() == previous.getSource() || river.getSource() == previous.getTarget()) {
            site = river.getSource();
        }
        return new Future(mine, site);
    }

    @Override
    public String getName() {
        return "Two Fly " + risk + " at 1/" + stepDivider;
    }

    @Override
    public synchronized Move getBestChoice() {
        return bestChoice;
    }

    private synchronized void setBestChoice(Move move) {
        bestChoice = move;
    }
}
