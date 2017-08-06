package io;

import com.fasterxml.jackson.annotation.JsonInclude;
import state.GameState;

import java.beans.Transient;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Move {

    public static class ClaimData {

        public int punter;
        public int source;
        public int target;

        @Override
        public String toString() {
            return "claim " + punter + " " + source + "-" + target;
        }

    }

    public static class PassData {
        public int punter;

        @Override
        public String toString() {
            return "pass " + punter;
        }
    }

    public static class SplurgeData {
        public int punter;
        public List<Integer> route = new ArrayList<>();
    }

    public static Move claim(int punter, River river) {
        ClaimData claim = new ClaimData();
        claim.punter = punter;
        claim.source = river.getSource();
        claim.target = river.getTarget();
        Move move = new Move();
        move.claim = claim;
        return move;
    }

    public static Move pass(int punter) {
        PassData pass = new PassData();
        pass.punter = punter;
        Move move = new Move();
        move.pass = pass;
        return move;
    }

    public static Move splurge(int punter, Collection<Integer> route) {
        SplurgeData splurge = new SplurgeData();
        splurge.punter = punter;
        splurge.route.addAll(route);
        Move move = new Move();
        move.splurge = splurge;
        return move;
    }


    private ClaimData claim = null;
    private PassData pass = null;
    private SplurgeData splurge = null;
    private GameState state = null;

    /** Returns null if this move is a pass. */
    public ClaimData getClaim() {
        return claim;
    }

    /** Returns null if this move is a claim. */
    public PassData getPass() {
        return pass;
    }

    public SplurgeData getSplurge() {
        return splurge;
    }

    /** Offline mode only. */
    public GameState getState() {
        return state;
    }

    public void setState(GameState state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return (claim != null ? claim.toString() : pass.toString());
    }
}
