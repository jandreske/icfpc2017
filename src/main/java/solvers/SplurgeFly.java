package solvers;

import io.Future;
import io.Move;
import io.River;
import org.jgrapht.alg.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import state.GameState;

import java.util.*;

public class SplurgeFly implements Solver {

    private Move bestChoice = null;

    private static final Logger LOG = LoggerFactory.getLogger(SplurgeFly.class);


    @Override
    public Move getNextMove(GameState state) {
        Set<Integer> mines = state.getMines();
        Set<River> freeRivers = state.getUnclaimedRivers();
        setBestChoice(Move.claim(state.getMyPunterId(), state.getUnclaimedRivers().iterator().next()));

        //if there is just one mine, and we have less than 10 connections, grab one
        Move move = getSingleMineRiver(state, mines);
        if (move != null) return move;

        //if we have an unfulfilled future that is still possible to fulfill, get the next step for that
        if (state.areFuturesActive()) {
            move = getNextFutureStep(state);
            if (move != null) return move;
        }

        //if we can connect two mines which are not yet connected, work on that
        move = getMineConnectionStep(state, mines);
        if (move != null) return move;

        //if we have no other idea, get the one which gives the most points
        move = getMaxPoints(state, mines, freeRivers);
        if (move != null) return move;

        // if nothing even gices points, just take something (to hurt other players)
        return Move.claim(state.getMyPunterId(), freeRivers.iterator().next());
    }

    private Move getMaxPoints(GameState state, Set<Integer> mines, Set<River> freeRivers) {
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
        if (best != null) {
            return Move.claim(state.getMyPunterId(), best);
        }
        return null;
    }

    private Move getMineConnectionStep(GameState state, Set<Integer> mines) {
        if (state.areSplurgesActive()) {
            int credit = state.getSplurgeCredits(state.getMyPunterId());
            java.util.Map<Pair<Integer, Integer>, Integer> canClaimNow = new HashMap<>();
            boolean canClaimAtOnceLater = false;

            for (int mineS : mines) {
                for (int mineT : mines) {
                    if (mineS == mineT) continue;
                    if (state.canReach(state.getMyPunterId(), mineS, mineT)) continue;
                    List<River> path = state.getShortestOpenRoute(state.getMyPunterId(), mineS, mineT);
                    if (path.isEmpty()) continue;
                    int missing = (int) path.stream().filter(river -> !river.isClaimed()).count();
                    if (hasSingleOpenFragment(state, mineS, mineT)) {
                        if (missing <= credit + 1) {
                            canClaimNow.put(Pair.of(mineS, mineT), missing);
                        } else {
                            canClaimAtOnceLater = true;
                        }
                    }
                }
            }
            //if we can claim an entire path now, lets take the longest and do it
            if (canClaimNow.size() > 0) {
                Pair<Integer, Integer> longest = Collections.max(canClaimNow.entrySet(), Comparator.comparingInt(Map.Entry::getValue)).getKey();
                return getSplurge(state, longest.getFirst(), longest.getSecond());
            }
            //if we can claim an entire future later, lets wait and get splurge credits
            if (canClaimAtOnceLater) return Move.pass(state.getMyPunterId());
        }


        List<River> bestPath = null;
        int shortest = Integer.MAX_VALUE;
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
        return null;
    }

    private Move getNextFutureStep(GameState state) {
        if (state.areSplurgesActive()) {
            int credit = state.getSplurgeCredits(state.getMyPunterId());
            java.util.Map<Future, Integer> canClaimNow = new HashMap<>();
            boolean canClaimAtOnceLater = false;

            if (state.getFutures() != null) {
                for (Future future : state.getFutures()) {
                    if (state.isFutureComplete(future)) continue;
                    int steps = state.missingStepsForFuture(future);
                    if (steps == -1) continue;
                    //check if we can claim the entire path now / later at once
                    if (hasSingleOpenFragment(state, future.getSource(), future.getTarget())) {
                        if (steps <= credit + 1) {
                            canClaimNow.put(future, steps);
                        } else {
                            canClaimAtOnceLater = true;
                        }
                    }
                }
                //if we can claim an entire future now, lets take the longest and do it
                if (canClaimNow.size() > 0) {
                    Future longest = Collections.max(canClaimNow.entrySet(), Comparator.comparingInt(Map.Entry::getValue)).getKey();
                    return getSplurge(state, longest.getSource(), longest.getTarget());
                }
                //if we can claim an entire future later, lets wait and get splurge credits
                if (canClaimAtOnceLater) return Move.pass(state.getMyPunterId());
            }
        }

        //if splurges are not enabled or there is nothing to splurge for, we just do single steps
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
        return null;
    }

    private Move getSplurge(GameState state, int sourceSite, int targetSite) {
        List<Integer> sites = new ArrayList<>();
        int source = sourceSite;
        int target;
        for (River river : state.getShortestOpenRoute(state.getMyPunterId() ,sourceSite, targetSite)) {
            target = river.getOpposite(source);
            if (!river.isClaimed()) {
                if (sites.isEmpty()) sites.add(source);
                sites.add(target);
            }
            source = target;
        }
        return Move.splurge(state.getMyPunterId(), sites);
    }

    private boolean hasSingleOpenFragment(GameState state, int source, int target) {
        List<River> path = state.getShortestOpenRoute(state.getMyPunterId(), source, target);
        boolean startFound = false;
        boolean endFound = false;
        for (River river : path) {
            if (!startFound) {
                startFound = !river.isClaimed();
            } else if (!endFound) {
                endFound = river.isClaimed();
            } else {
                if (!river.isClaimed()) return false;
            }
        }
        return startFound;
    }

    private Move getSingleMineRiver(GameState state, Set<Integer> mines) {
        if (mines.size() == 1) {
            int mine = mines.iterator().next();
            if (state.getOwnRiversTouching(mine).size() < 10) {
                Set<River> rivers = state.getUnclaimedRiversTouching(mine);
                if (!rivers.isEmpty()) {
                    return Move.claim(state.getMyPunterId(), rivers.iterator().next());
                }
            }
        }
        return null;
    }

    @Override
    public Future[] getFutures(GameState state) {
        int risk = getRisk(state);
        int stepDivider = getStepDivider(state);
        int maxTurns = state.getNumRivers() / state.getNumPunters();
        int maxFutureSteps = maxTurns / stepDivider;
        int usedSteps = 0;
        Set<Future> futures = new HashSet<>();
        for (int mine : state.getMines()) {
            if (usedSteps >= maxFutureSteps) break;
            Future future = getFuture(mine, state, risk);
            if (future != null) {
                futures.add(future);
                usedSteps += state.getShortestRouteLength(future.getSource(), future.getTarget());
                LOG.info("Added future ({}->{}) with {} / {} steps used", future.getSource(), future.getTarget(), usedSteps, maxFutureSteps);
            }
        }
        return futures.toArray(new Future[futures.size()]);
    }

    private Future getFuture(int mine, GameState state, int risk) {
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
        return "Splurge Fly";
    }

    @Override
    public synchronized Move getBestChoice() {
        return bestChoice;
    }

    private synchronized void setBestChoice(Move move) {
        bestChoice = move;
    }

    private int getRisk(GameState state) {
        int sites = state.getSites().size();
        int rivers = state.getNumRivers();
        int mines = state.getMines().size();

        if (sites == 8 && rivers == 12 && mines == 2) return 0;         //sample
        if (sites == 38 && rivers == 60 && mines == 4) return 4;        //lambda
        if (sites == 42 && rivers == 81 && mines == 3) return 5;        //sierpinski
        if (sites == 27 && rivers == 65 && mines == 4) return 5;        //circle
        if (sites == 97 && rivers == 187 && mines == 4) return 5;       //randomMedium
        if (sites == 86 && rivers == 123 && mines == 4) return 0;       //randomSparse
        if (sites == 488 && rivers == 945 && mines == 8) return 4;      //bostonSparse
        if (sites == 301 && rivers == 386 && mines == 5) return 4;      //tube
        if (sites == 961 && rivers == 1751 && mines == 32) return 0;    //edinburghSparse
        if (sites == 1560 && rivers == 2197 && mines == 12) return 0;   //naraSparse
        if (sites == 614 && rivers == 1132 && mines == 1) return 5;     //oxfordSparse
        if (sites == 1175 && rivers == 2234 && mines == 8) return 5;    //gothenburgSparse

        //default, works best on most
        LOG.warn("UNKNOWN MAP with {} sites, {} rivers, {} mines", sites, rivers, mines);
        return 5;
    }

    private int getStepDivider(GameState state) {
        int sites = state.getSites().size();
        int rivers = state.getNumRivers();
        int mines = state.getMines().size();

        if (sites == 8 && rivers == 12 && mines == 2) return 0;         //sample
        if (sites == 38 && rivers == 60 && mines == 4) return 4;        //lambda
        if (sites == 42 && rivers == 81 && mines == 3) return 3;        //sierpinski
        if (sites == 27 && rivers == 65 && mines == 4) return 3;        //circle
        if (sites == 97 && rivers == 187 && mines == 4) return 3;       //randomMedium
        if (sites == 86 && rivers == 123 && mines == 4) return 0;       //randomSparse
        if (sites == 488 && rivers == 945 && mines == 8) return 4;      //bostonSparse
        if (sites == 301 && rivers == 386 && mines == 5) return 4;      //tube
        if (sites == 961 && rivers == 1751 && mines == 32) return 0;    //edinburghSparse
        if (sites == 1560 && rivers == 2197 && mines == 12) return 0;   //naraSparse
        if (sites == 614 && rivers == 1132 && mines == 1) return 3;     //oxfordSparse
        if (sites == 1175 && rivers == 2234 && mines == 8) return 3;    //gothenburgSparse

        //default, works best on most
        return 3;
    }
}
