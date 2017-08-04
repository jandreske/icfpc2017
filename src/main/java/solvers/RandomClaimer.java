package solvers;

import io.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import state.GameState;

import java.util.Random;

public class RandomClaimer implements Solver {

    private static final Logger LOG = LoggerFactory.getLogger(RandomClaimer.class);



    @Override
    public Move getNextMove(GameState state, Gameplay.Request request) {
        int numRivers = state.getMap().getRivers().size();
        int item = new Random().nextInt(numRivers);
        int i = 0;
        for (River river : state.getMap().getRivers()) {
            if (i == item) {
                Claim claim = new Claim();
                Claim.Data data = claim.getClaim();
                data.punter = state.getMyPunterId();
                data.source = river.getSource();
                data.target = river.getTarget();
                return claim;
            }
            i++;
        }
        LOG.error("Failure when trying to claim random river");
        return new Pass();
    }

}
