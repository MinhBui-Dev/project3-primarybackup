package dslabs.primarybackup;

import dslabs.framework.Address;
import dslabs.framework.Node;
import static dslabs.primarybackup.PingCheckTimer.PING_CHECK_MILLIS;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.java.Log;

@Log
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
class ViewServer extends Node {
    static final int STARTUP_VIEWNUM = 0;
    private static final int INITIAL_VIEWNUM = 1;

    // TODO: declare fields for your implementation ...


    /* -------------------------------------------------------------------------
        Construction and Initialization
       -----------------------------------------------------------------------*/
    public ViewServer(Address address) {
        super(address);
    }

    @Override
    public void init() {
        set(new PingCheckTimer(), PING_CHECK_MILLIS);
        // TODO: initialize fields ...
    }

    /* -------------------------------------------------------------------------
        Message Handlers
       -----------------------------------------------------------------------*/
    private void handlePing(Ping m, Address sender) {
        // TODO: handle server ping, filtering outdated ping ...
    }

    private void handleGetView(GetView m, Address sender) {
        // TODO: return current view to sender ...
    }

    /* -------------------------------------------------------------------------
        Timer Handlers
       -----------------------------------------------------------------------*/
    private void onPingCheckTimer(PingCheckTimer t) {
        // TODO: check server liveness on ping check ...
    }

    /* -------------------------------------------------------------------------
        Utils
       -----------------------------------------------------------------------*/
    // TODO: add utils here ...
}