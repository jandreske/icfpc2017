package io;

public class Pass implements Move {

    public static class Data {
        int punter;
    }

    public Data pass;

    @Override
    public Claim.Data getClaim() {
        return null;
    }

    @Override
    public Data getPass() {
        return pass;
    }

    @Override
    public String toString() {
        return "pass " + pass.punter;
    }
}
