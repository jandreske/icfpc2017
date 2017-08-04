package solvers;

import io.Gameplay;
import io.Map;
import io.Move;
import state.GameState;

public interface Solver {

        Move getNextMove(GameState state, Gameplay.Request request);

        GameState getState();

}
