package dslabs.primarybackup;

import dslabs.framework.Address;
import dslabs.framework.Node;
import static dslabs.primarybackup.PingCheckTimer.PING_CHECK_MILLIS;


import java.util.ArrayList;
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
    private Address primary, backup, previousPrimary;
    private View currentView, nextView, previousView;
    private final ArrayList<Address> servers = new ArrayList<>();    //keep track of ping servers
    private final ArrayList<Integer> serverViewNums = new ArrayList<>(); //keep track of view numbers
    private final ArrayList<Integer> pingCheck = new ArrayList<>(); //keep track of ping check
    private final HashSet<Address> aliveServers = new HashSet<>();    // HashSet to keep track of all alive servers
    private boolean primaryAck, needNewView, handleViewRequest;
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
        primary = null;
        backup = null;
        currentView = new View(STARTUP_VIEWNUM, null, null);
        primaryAck = true;
        nextView = null;
        handleViewRequest = false;
    }

    /* -------------------------------------------------------------------------
        Message Handlers
       -----------------------------------------------------------------------*/
    private void handlePing(Ping m, Address sender) {
        // TODO: handle server ping, filtering outdated ping ...
        if (servers.contains(sender)) {
            // filtering outdated ping
            int index = servers.indexOf(sender);
            if (m.viewNum() < serverViewNums.get(index)) {
                return;
            }
            // update the number of pings received from the server
            pingCheck.set(index, 1);
            serverViewNums.set(index, m.viewNum());
        } else {
            // add the server to pingServers and serverViewNum hashmaps
            servers.add(sender);
            serverViewNums.add(m.viewNum());
            pingCheck.add(1);
        }

        if (primary == null) {
            primary = sender;
            currentView = new View(INITIAL_VIEWNUM, primary, null);
            primaryAck = false;
        } else if (backup == null && !sender.equals(primary) && nextView == null) {
            backup = sender;
            nextView = new View(currentView.viewNum() + 1, primary, backup);
        }


        if (primary.equals(sender)) {
            if (m.viewNum() < currentView.viewNum()) {
                send(new ViewReply(currentView), sender);
            } else if (m.viewNum() == currentView.viewNum() && nextView != null) {
                currentView = nextView;
                primaryAck = false;
                nextView = null;
                send(new ViewReply(currentView), sender);
                send(new ViewReply(currentView), currentView.backup());
            } else {
                primaryAck = true;
                send(new ViewReply(currentView), sender);
            }
        } else if (backup != null && backup.equals(sender))  {
            if (primaryAck && nextView != null) {
                currentView = nextView;
                primaryAck = false;
                nextView = null;
                send(new ViewReply(currentView), currentView.primary());
            }
            send(new ViewReply(currentView), sender);
        } else {
            //send(new ViewReply(currentView), sender);
        }

        //System.out.println("Current View: " + currentView);
        //System.out.println("Ack: " + primaryAck);
        //System.out.println("Next View: " + nextView);
    }

    private void handleGetView(GetView m, Address sender) {
        // TODO: return current view to sender ...
        send(new ViewReply(currentView), sender);
    }

    /* -------------------------------------------------------------------------
        Timer Handlers
       -----------------------------------------------------------------------*/
    private void onPingCheckTimer(PingCheckTimer t) {
        // TODO: check server liveness on ping check ...
        // No server pings, reset timer
        if (servers.isEmpty()) {
            set(t, PING_CHECK_MILLIS);
            return;
        }
        for (int i = 0; i < servers.size(); i++) {
            if (pingCheck.get(i) == 0) {
                if (aliveServers.contains(servers.get(i))) {
                    aliveServers.remove(servers.get(i));
                }
            }else {
                if (!aliveServers.contains(servers.get(i))) {
                    aliveServers.add(servers.get(i));
                }
            }
            pingCheck.set(i, 0);
        }
        // if the number of pings received from previous ping checks is 0, the server is dead
        /*for (Address server : pingServers.keySet()) {
            int totalPings = pingServers.get(server);
            //System.out.println("Server: " + server + " with current and previous pings: " + totalPings);
            if (aliveServers.contains(server)) {
                if (totalPings == 0) {
                    aliveServers.remove(server);
                }
            } else {
                if (totalPings > 0) {
                    aliveServers.add(server);
                }
            }
            pingServers.put(server, 0);
        }*/

        Address[] servers =
                aliveServers.toArray(new Address[aliveServers.size()]);

        if (servers.length == 0) {
            set(t, PING_CHECK_MILLIS);
            return;
        }

        needNewView = false;

        if (primaryAck) {
            if (primary != null && !aliveServers.contains(primary)) {
                previousPrimary = primary;
                primary = backup;
                needNewView = true;
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
                needNewView = true;
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
        }

        if (needNewView) {
            currentView = new View((currentView.viewNum()+1), primary, backup);
            primaryAck = false;
            send(new ViewReply(currentView), primary);
            if (backup != null) {
                send(new ViewReply(currentView), backup);
            }
        }
        //System.out.println("Current View: " + currentView);
        set(t, PING_CHECK_MILLIS);
    }

    /* -------------------------------------------------------------------------
        Utils
       -----------------------------------------------------------------------*/
    // TODO: add utils here ...
}