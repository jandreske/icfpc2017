package io;

import java.util.List;

public class Scoring {

    public static class Data {

        public List<Move> moves;
        public List<Score> scores;

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder();
            String sep = "";
            for (Score score : scores) {
                b.append(sep);
                sep = ", ";
                b.append(score.punter).append(": ").append(score.score);
            }
            return b.toString();
        }
    }

    public static class Score {
        public int punter;
        public int score;
    }

    public Data stop;

}
