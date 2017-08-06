package state;

import io.Setup;

public class GameStateFactory {

    public GameState create(Setup.Request setup) {
        return new MapBasedGameState(setup);
    }

    public Class<? extends GameState> getImplementationClass() {
        return MapBasedGameState.class;
    }

}
