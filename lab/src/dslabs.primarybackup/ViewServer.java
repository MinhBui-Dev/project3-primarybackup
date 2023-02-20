package dslabs.primarybackup;

import dslabs.framework.Address;
import dslabs.framework.Node;
import static dslabs.primarybackup.PingCheckTimer.PING_CHECK_MILLIS;


import java.util.HashMap;
import java.util.HashSet;
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
    private int currentViewNum, nextViewNum;
    private Address primary, backup, previousPrimary;

    private View currentView, nextView;
    private final HashMap<Address, Integer> pingServers = new HashMap<>();    // HashMap to keep track of active ping servers
    private final HashMap<Address, Integer> serverViewNum = new HashMap<>();    // HashMap to keep track of current view number for each server
    private final HashSet<Address> aliveServers = new HashSet<>();    // HashSet to keep track of all alive servers
    private boolean primaryAck;
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
        currentViewNum = STARTUP_VIEWNUM;
        nextViewNum = currentViewNum;
        primary = null;
        backup = null;
        currentView = new View(currentViewNum, primary, backup);
        nextView = null;
    }

    /* -------------------------------------------------------------------------
        Message Handlers
       -----------------------------------------------------------------------*/
    private void handlePing(Ping m, Address sender) {
        // TODO: handle server ping, filtering outdated ping ...
        // if view number in message is less than current view number, ignore
        if (serverViewNum.containsKey(sender)) {
            // if view number in message is less than current view number, ignore
            if (m.viewNum() < serverViewNum.get(sender)) {
                return;
            }
            // update the number of pings received from the server
            pingServers.put(sender, pingServers.get(sender) + 1);
            // update the view number of the server
            serverViewNum.put(sender, m.viewNum());
        } else {
            // add the server to pingServers and serverViewNum hashmaps
            serverViewNum.put(sender, m.viewNum());
            pingServers.put(sender, 1);
        }

        if (primary == null) {
            primary = sender;
            currentViewNum = INITIAL_VIEWNUM;
            currentView = new View(INITIAL_VIEWNUM, primary, backup);
            primaryAck = false;
            send(new ViewReply(currentView), sender);
        } else if (backup == null && !sender.equals(primary)) {
            backup = sender;
            nextViewNum = currentViewNum + 1;
            nextView = new View(nextViewNum, primary, backup);

            if (primaryAck) {
                currentView = nextView;
                currentViewNum = nextViewNum;
                nextView = null;
                primaryAck = false;
            }
            send(new ViewReply(currentView), sender);
        } else {
            // Case where sender is primary:
            if (sender.equals(primary)) {
                if (m.viewNum() < currentViewNum) {
                    send(new ViewReply(currentView), sender); // if the server view is less than current view, send current view
                } else if (m.viewNum() == currentViewNum && nextView == null) {
                    // if the server view is equal to current view and next view is null, send current view and set primaryAck to true
                    primaryAck = true;
                    send(new ViewReply(currentView), sender);
                } else {
                    // if the server view is equal to current view and next view is not null, send next view and set primaryAck to false
                    currentViewNum = nextViewNum;
                    currentView = nextView;
                    nextView = null;
                    primaryAck = false;
                    send(new ViewReply(currentView), sender);
                }
            } else {
                send(new ViewReply(currentView), sender);
            }
        }
        //System.out.println("Current View: " + currentView);
        //System.out.println("Ack: " + primaryAck);
        //System.out.println("Next View: " + nextView);
    }

    private void handleGetView(GetView m, Address sender) {
        // TODO: return current view to sender ...
        send(new ViewReply(currentView), sender);
        //System.out.println("Current View: " + currentView);
    }

    /* -------------------------------------------------------------------------
        Timer Handlers
       -----------------------------------------------------------------------*/
    private void onPingCheckTimer(PingCheckTimer t) {
        // TODO: check server liveness on ping check ...
        // if the number of pings received from previous ping checks is 0, the server is dead
        for (Address server : pingServers.keySet()) {
            int totalPings = pingServers.get(server);
            if (aliveServers.contains(server)) {
                if (totalPings == 0) {
                    aliveServers.remove(server);
                }
            } else {
                if (totalPings > 0) {
                    aliveServers.add(server);
                }
            }
            //System.out.println("Server: " + server + " with current and previous pings: " + totalPings);
            pingServers.put(server, 0);

        }

        Address[] servers =
                aliveServers.toArray(new Address[aliveServers.size()]);

        if (primary != null && !aliveServers.contains(primary) && primaryAck) {
            previousPrimary = primary;
            primary = backup;
            if (servers.length > 1) {
                if (servers[0].equals(primary)) {
                    backup = servers[1];
                } else {
                    backup = servers[0];
                }
            } else {
                backup = null;
            }
        } else if (backup != null && !aliveServers.contains(backup)) {
            if (servers.length > 1) {
                if (aliveServers.contains(previousPrimary)) {
                    backup = previousPrimary;
                } else {
                    if (servers[0].equals(primary)) {
                        backup = servers[1];
                    } else {
                        backup = servers[0];
                    }
                }
            } else {
                backup = null;
            }
        }

        if (!primary.equals(currentView.primary()) || backup == null ||
                !backup.equals(currentView.backup())) {
            currentViewNum = currentViewNum + 1;
            currentView = new View(currentViewNum, primary, backup);
            primaryAck = false;
            nextView = null;
        }
        //System.out.println("Current View: " + currentView);
        set(t, PING_CHECK_MILLIS);
    }

    /* -------------------------------------------------------------------------
        Utils
       -----------------------------------------------------------------------*/
    // TODO: add utils here ...
}