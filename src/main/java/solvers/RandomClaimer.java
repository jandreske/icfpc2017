package solvers;

import io.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import state.GameState;

import java.util.Random;

public class RandomClaimer implements Solver {

    private static final Logger LOG = LoggerFactory.getLogger(RandomClaimer.class);

    private final Random random = new Random();
    private River bestChoice = null;

    @Override
    public River getNextMove(GameState state) {
        int numRivers = state.getMap().getRivers().size();
        int item = random.nextInt(numRivers);
        int i = 0;
        for (River river : state.getMap().getRivers()) {
            if (i == item) {
                return river;
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
    public synchronized River getBestChoice() {
        return bestChoice;
    }

    private synchronized void setBestChoice(River river) {
        this.bestChoice = river;
    }

}
