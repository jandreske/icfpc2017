package solvers;

import io.Future;
import io.Gameplay;
import io.Move;
import io.River;
import state.GameState;

public interface Solver {

    /**
     * Return next river to claim or {@code null} to pass.
     */
    River getNextMove(GameState state);

    Future[] getFutures(GameState state);

    String getName();

    River getBestChoice();
}
