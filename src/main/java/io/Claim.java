package io;

public class Claim implements Move {

    public static class Data {

        int punter;
        int source;
        int target;

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
}
