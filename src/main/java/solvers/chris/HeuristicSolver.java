package solvers.chris;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.Future;
import io.River;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import solvers.RandomClaimer;
import solvers.Solver;
import state.GameState;

import java.util.Collection;
import java.util.Set;

public class HeuristicSolver implements Solver {

    private static final Logger LOG = LoggerFactory.getLogger(HeuristicSolver.class);

    // near mines
    // Block mines
    // connect SCCs
    // does not use Futures

    private final RandomClaimer randomClaimer = new RandomClaimer();

    private GameState state;

    private River bestMoveSoFar;
    private RiverValue bestMoveValue;

    private final Multimap<RiverValue, River> candidates = HashMultimap.create();

    private void addCandidate(RiverValue value, River candidate) {
        synchronized (candidates) {
            if (bestMoveValue == null || value.compareTo(bestMoveValue) < 0) {
                bestMoveValue = value;
                bestMoveSoFar = candidate;
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
                    addCandidate(RiverValue.EXTEND, river);
                }
            } else if (isClaimed(river.getTarget())) {
                if (state.isMine(river.getSource())) {
                    addCandidate(RiverValue.CONNECT_MINE, river);
                } else {
                    addCandidate(RiverValue.EXTEND, river);
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
    }

    @Override
    public River getNextMove(GameState state) {
        reset(state);
        classifyUnclaimedRivers();
        RiverValue bestValue = getBestValue();
        if (bestValue != null) {
            LOG.info("found cadidate of value {}", bestValue);
            Collection<River> choices = candidates.get(bestValue);
            return chooseByDegree(choices);
        }
        LOG.info("falling back to random");
        return randomClaimer.getNextMove(state);
    }

    private River chooseByDegree(Collection<River> choices) {
        // TODO

        // prefer mines with few unclaimed rivers
        // and non-mines with many (unclaimed?)

        return choices.stream().findAny().get();
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
    public River getBestChoice() {
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
}
