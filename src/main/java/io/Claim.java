package io;

public class Claim implements Move {

    public static class Data {

        public int punter;
        public int source;
        public int target;

        @Override
        public String toString() {
            return "claim " + punter + " " + source + "-" + target;
        }

    }

    private Data claim;

    @Override
    public Claim.Data getClaim() {
        return claim;
    }

    @Override
    public Pass.Data getPass() {
        return null;
    }

    @Override
    public String toString() {
        return claim.toString();
    }
}
