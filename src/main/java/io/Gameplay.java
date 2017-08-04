package io;

import com.fasterxml.jackson.annotation.JsonInclude;
import state.GameState;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Gameplay {

    public static class Request {

        public static class MoveData {
            public List<Move> moves = new ArrayList<>();
        }

        private MoveData move;
        private GameState state = null;

        public MoveData getMove() {
            return move;
        }

        public GameState getState() {
            return state;
        }

        public void setState(GameState state) {
            this.state = state;
        }
    }

    // Response is a Move
}
