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
            return punter + " " + source + "-" + target;
        }

    }

    public static class PassData {
        public int punter;

        @Override
        public String toString() {
            return "Pass " + punter;
        }
    }

    public static class SplurgeData {
        public int punter;
        public List<Integer> route = new ArrayList<>();

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder();
            b.append("Splurge ");
            b.append(punter);
            b.append(' ');
            char sep = '<';
            for (int site : route) {
                b.append(sep);
                sep = '-';
                b.append(site);
            }
            b.append('>');
            return b.toString();
        }
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
    private ClaimData option = null;
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

    public ClaimData getOption() {
        return option;
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
        if (claim != null) {
            return "Claim " + claim;
        }
        if (option != null) {
            return "Option " + option;
        }
        if (pass != null) {
            return pass.toString();
        }
        return splurge.toString();
    }
}
