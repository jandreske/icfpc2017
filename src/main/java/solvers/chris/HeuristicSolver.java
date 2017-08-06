package solvers.chris;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.Future;
import io.Move;
import io.River;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import solvers.RandomClaimer;
import solvers.Solver;
import state.GameState;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class HeuristicSolver implements Solver {

    private static final Logger LOG = LoggerFactory.getLogger(HeuristicSolver.class);

    // near mines
    // Block mines
    // connect SCCs
    // does not use Futures

    private final RandomClaimer randomClaimer = new RandomClaimer();

    private GameState state;

    private Move bestMoveSoFar;
    private RiverValue bestMoveValue;

    private final Multimap<RiverValue, River> candidates = HashMultimap.create();

    private void addCandidate(RiverValue value, River candidate) {
        synchronized (candidates) {
            if (bestMoveValue == null || value.compareTo(bestMoveValue) < 0) {
                bestMoveValue = value;
                bestMoveSoFar = Move.claim(state.getMyPunterId(), candidate);
            }
            candidates.put(value, candidate);
        }
    }


    private void classifyUnclaimedRivers() {
        for (River river : state.getUnclaimedRivers()) {
            if (isClaimed(river.getSource())) {
                if (isClaimed(river.getTarget())) {
                    if (!state.canReach(state.getMyPunterId(), river.getSource(), river.getTarget())) {
                        // BIG gain
                        addCandidate(RiverValue.CONNECT_NETS, river);
                    }
                } else if (state.isMine(river.getTarget())) {
                    addCandidate(RiverValue.CONNECT_MINE, river);
                } else {
                    if (state.canReachMine(state.getMyPunterId(), river.getSource())) {
                        addCandidate(RiverValue.EXTEND_FROM_MINE, river);
                    } else {
                        addCandidate(RiverValue.EXTEND, river);
                    }
                }
            } else if (isClaimed(river.getTarget())) {
                if (state.isMine(river.getSource())) {
                    addCandidate(RiverValue.CONNECT_MINE, river);
                } else {
                    if (state.canReachMine(state.getMyPunterId(), river.getTarget())) {
                        addCandidate(RiverValue.EXTEND_FROM_MINE, river);
                    } else {
                        addCandidate(RiverValue.EXTEND, river);
                    }
                }
            } else if (state.isMine(river.getSource()) || state.isMine(river.getTarget())) {
                addCandidate(RiverValue.MINE, river);
            }
        }
    }

    private void reset(GameState state) {
        this.state = state;
        synchronized (candidates) {
            bestMoveSoFar = null;
            bestMoveValue = null;
        }
        candidates.clear();
    }

    @Override
    public Move getNextMove(GameState state) {
        reset(state);
        classifyUnclaimedRivers();
        RiverValue bestValue = getBestValue();
        if (bestValue != null) {
            LOG.info("found cadidate of value {}", bestValue);
            Collection<River> choices = candidates.get(bestValue);
            return Move.claim(state.getMyPunterId(), chooseByDegree(bestValue, choices));
        }
        LOG.info("falling back to random");
        return randomClaimer.getNextMove(state);
    }

    private River chooseByDegree(RiverValue type, Collection<River> choices) {
        River[] rivers = choices.toArray(new River[choices.size()]);
        if (type == RiverValue.EXTEND_FROM_MINE) {
            return chooseLongestPath(rivers);
        }
        Arrays.sort(rivers, Comparator.comparingLong(this::getDegreeQuality));
        return rivers[0];
    }

    private River chooseLongestPath(River[] rivers) {
        int maxSoFar = 0;
        int maxIndex = 0;
        List<Integer> mines = state.getMines().stream()
                .filter(mine -> state.isOnRiver(state.getMyPunterId(), mine))
                .collect(Collectors.toList());
        for (int i = 0; i < rivers.length; i++) {
            River river = rivers[i];
            int site;
            if (isClaimed(river.getTarget())) {
                site = river.getSource();
            } else {
                site = river.getTarget();
            }
            for (int mine : mines) {
                int dist = state.getShortestRouteLength(mine, site);
                if (dist > maxSoFar) {
                    maxSoFar = dist;
                    maxIndex = i;
                }
            }
        }
        return rivers[maxIndex];
    }


    @Override
    public Future[] getFutures(GameState state) {
        return new Future[0];
    }

    @Override
    public String getName() {
        return "heuristic";
    }

    @Override
    public Move getBestChoice() {
        synchronized (candidates) {
            return bestMoveSoFar;
        }
    }

    private RiverValue getBestValue() {
        synchronized (candidates) {
            return bestMoveValue;
        }
    }

    /**
     * Is this site touched by a river that is claimed by me?
     */
    private boolean isClaimed(int site) {
        return !state.getOwnRiversTouching(site).isEmpty();
    }

    private long getDegreeQuality(River r) {
        if (state.isMine(r.getSource()) && state.isMine(r.getTarget())) {
            return state.getDegree(r.getSource()) + state.getDegree(r.getTarget());
        }
        if (state.isMine(r.getSource())) {
            return state.getDegree(r.getSource()) + 5 - Math.max(5, state.getDegree(r.getTarget()));
        }
        if (state.isMine(r.getTarget())) {
            return state.getDegree(r.getTarget()) + 5 - Math.max(5, state.getDegree(r.getSource()));
        }
        return 15 - (Math.max(5, state.getDegree(r.getSource())) + Math.max(5, state.getDegree(r.getTarget())));
    }
}
