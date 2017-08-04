package solvers;

import io.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import state.GameState;

import java.util.Random;

public class RandomClaimer implements Solver {

    private static final Logger LOG = LoggerFactory.getLogger(RandomClaimer.class);

    private GameState state;


    @Override
    public Move getNextMove(GameState newState, Gameplay.Request request) {
        state = newState;
        int numRivers = state.map.getRivers().size();
        int item = new Random().nextInt(numRivers);
        int i = 0;
        for (River river : state.map.getRivers()) {
            if (i == item) {
                Claim claim = new Claim();
                Claim.Data data = claim.getClaim();
                data.punter = state.punter;
                data.source = river.getSource();
                data.target = river.getTarget();
                return claim;
            }
        }
        LOG.error("Failure when trying to claim random river");
        return new Pass();
    }

    @Override
    public GameState getState() {
        return state;
    }
}
