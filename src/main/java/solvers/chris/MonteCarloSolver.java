package solvers.chris;

import io.Future;
import io.Move;
import io.River;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import solvers.Solver;
import state.GameState;

import java.util.Collection;
import java.util.List;
import java.util.Random;

public class MonteCarloSolver implements Solver {

    private static final Logger LOG = LoggerFactory.getLogger(MonteCarloSolver.class);

    private final long timeout;
    private final Random rng;

    private Move bestSoFar = null;

    private int punter = -1;
    private River[] rivers;
    private int[] origOwner;
    private int[] numSamples;
    private int[] score;

    /**
     * @param timeout timeout in nanoseconds
     */
    public MonteCarloSolver(long timeout) {
        this.timeout = 9 * timeout / 10;
        this.rng = new Random();
    }

    @Override
    public Move getNextMove(GameState state) {
        long stopTime = System.nanoTime() + timeout;
        int numMoves = state.getRemainingNumberOfMoves();
        Collection<River> possibleMoves = state.getUnclaimedRivers();
        this.punter = state.getMyPunterId();
        int n = possibleMoves.size();
        rivers = new River[n];
        origOwner = new int[n];
        numSamples = new int[n];
        score = new int[n];
        int i = 0;
        for (River r : possibleMoves) {
            rivers[i] = r;
            origOwner[i] = r.getOwner();
            i++;
        }

        while (System.nanoTime() < stopTime) {
            // choose random moves for all punters
            int myMove = rng.nextInt(n);
            int myPunter = state.getMyPunterId();
            // can use claim() because we need to invalidate score caches
            state.applyMove(Move.claim(myPunter, rivers[myMove]));
            for (i = 1; i < numMoves; i++) {
                int punter = (myPunter + i) % state.getNumPunters();
                int move = rng.nextInt(n);
                claim(move, punter);
            }

            // compute score (1 - numPunters)
            int myScore = state.getScore(myPunter);
            restoreOwners();
            /*
            int myScore = state.getNumPunters();
            for (i = 0; i < state.getNumPunters(); i++) {
                // LOG.info("score({}): {}", i, state.getScore(i));
                if (i != myPunter && state.getScore(i) > own) {
                    myScore--;
                }
            } */
            // assign score to first move chosen for this punter
            numSamples[myMove]++;
            score[myMove] += myScore;
            // write the best move so far into bestSoFar
            int bestIndex = getBest();
            synchronized (this) {
                bestSoFar = makeMove(bestIndex);
            }
        }
        report();
        return bestSoFar;
    }

    private Move makeMove(int i) {
        if (i < 0) {
            return null;
        }
        return Move.claim(punter, rivers[i]);
    }

    private int getBest() {
        double best = 0;
        int bestIndex = -1;
        for (int i = 0; i < rivers.length; i++) {
            if (numSamples[i] > 0) {
                double avgScore = (double) score[i] / numSamples[i];
                if (avgScore > best) {
                    best = avgScore;
                    bestIndex = i;
                }
            }
        }
        return bestIndex;
    }

    private void report() {
        int totalSamples = 0;
        for (int i = 0; i < rivers.length; i++) {
            if (numSamples[i] > 0) {
                totalSamples += numSamples[i];
                double avgScore = (double) score[i] / numSamples[i];
                LOG.info("mct: {}-{}: {} {}", rivers[i].getSource(), rivers[i].getTarget(), numSamples[i], avgScore);
            }
        }
        LOG.info("Monte Carlo total samples: {}", totalSamples);
    }

    private void claim(int i, int punter) {
        while (rivers[i].isClaimed()) {
            i++;
            if (i == rivers.length) {
                i = 0;
            }
        }
        rivers[i].setOwner(punter);
    }

    private void restoreOwners() {
        for (int i = 0; i < rivers.length; i++) {
            if (origOwner[i] >= 0) {
                rivers[i].setOption(-1);
            } else {
                rivers[i].setOwner(-1);
            }
        }
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
    public synchronized Move getBestChoice() {
        return bestSoFar;
    }
}
