package solvers;

import io.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import state.GameState;

import java.util.Random;
import java.util.Set;

public class RandomClaimer implements Solver {

    private static final Logger LOG = LoggerFactory.getLogger(RandomClaimer.class);

    private final Random random = new Random();
    private Move bestChoice;

    @Override
    public Move getNextMove(GameState state) {
        bestChoice = Move.pass(state.getMyPunterId());
        Set<River> rivers = state.getUnclaimedRivers();
        int numRivers = rivers.size();
        int item = random.nextInt(numRivers);
        int i = 0;
        for (River river : rivers) {
            if (i == item) {
                return Move.claim(state.getMyPunterId(), river);
            }
            i++;
        }
        LOG.error("Failure when trying to claim random river");
        return null;
    }

    @Override
    public Future[] getFutures(GameState state) {
        return new Future[0];
    }

    @Override
    public String getName() {
        return "Random Claimer";
    }

    @Override
    public Move getBestChoice() {
        return bestChoice;
    }

}
