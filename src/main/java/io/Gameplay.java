package io;

import java.util.ArrayList;
import java.util.List;

public class Gameplay {

    public static class Request {

        public static class MoveData {
            public final List<Move> moves = new ArrayList<>();
        }

        private final MoveData move;

        public Request(MoveData move) {
            this.move = move;
        }

    }

    // Response is a Move
}
