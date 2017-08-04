package solvers;

import io.Gameplay;
import io.Map;
import io.Move;
import io.River;
import state.GameState;

public interface Solver {

    /**
     * Return next river to claim or {@code null} to pass.
     */
    River getNextMove(GameState state);

}
