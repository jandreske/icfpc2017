package solvers.chris;

import io.Future;
import io.Move;
import io.River;
import solvers.Solver;
import state.GameState;

import java.util.Collection;
import java.util.List;

public class MonteCarloSolver implements Solver {

    private final long timeout;

    private Move bestSoFar = null;

    private River[] moves;

    /**
     * @param timeout timeout in nanoseconds
     */
    public MonteCarloSolver(long timeout) {
        this.timeout = 9 * timeout / 10;
    }

    @Override
    public Move getNextMove(GameState state) {
        long stopTime = System.nanoTime() + timeout;
        int numMoves = state.getRemainingNumberOfMoves();
        Collection<River> possibleMoves = state.getUnclaimedRivers();

        while (System.nanoTime() < stopTime) {
            // choose random moves for all punters
            // compute score (1 - numPunters)
            // assign score to first move chosen for this punter
            // write the best move so far into bestSoFar
        }
        return null;
    }

    @Override
    public Future[] getFutures(GameState state) {
        return new Future[0];
    }

    @Override
    public String getName() {
        return "MCTS";
    }

    @Override
    public Move getBestChoice() {
        return null;
    }
}
