package solvers;

import io.Future;
import io.Gameplay;
import io.Move;
import io.River;
import state.GameState;

public interface Solver {

    Move getNextMove(GameState state);

    Future[] getFutures(GameState state);

    String getName();

    Move getBestChoice();
}
