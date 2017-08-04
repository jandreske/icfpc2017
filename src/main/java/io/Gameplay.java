package io;

import java.util.ArrayList;
import java.util.List;

public class Gameplay {

    public static class Request {

        public static class MoveData {
            public List<Move> moves = new ArrayList<>();
        }

        private MoveData move;

    }

    // Response is a Move
}
