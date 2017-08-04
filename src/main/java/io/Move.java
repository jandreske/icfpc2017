package io;

public interface Move {

    /** Returns null if this move is a pass. */
    Claim.Data getClaim();
    Pass.Data getPass();

}
