package solvers.chris;

public enum RiverValue {

    /** Both endpoints are already claimed by me, but not connected
     * in any other way. */
    CONNECT_NETS,

    /** One endpoint is a mine not yet claimed by me, and the other
     * is a site already claimed by me.
     */
    CONNECT_MINE,

    /** Exactly one of the endpoints is claimed by me. */
    EXTEND,

    /** At least one of the endpoints is a mine. */
    MINE

}
