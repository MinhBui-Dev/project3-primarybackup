package dslabs.primarybackup;

import dslabs.atmostonce.AMOCommand;
import dslabs.atmostonce.AMOResult;
import dslabs.framework.Address;
import dslabs.framework.Client;
import dslabs.framework.Command;
import dslabs.framework.Node;
import dslabs.framework.Result;
import dslabs.primarybackup.PBServer.InvalidView;
import dslabs.primarybackup.PBServer.PBResult;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.java.Log;

import static dslabs.primarybackup.ViewServer.STARTUP_VIEWNUM;
import static dslabs.primarybackup.ClientTimer.CLIENT_RETRY_MILLIS;
import static dslabs.primarybackup.ViewTimer.VIEW_MILLIS;

@Log
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
class PBClient extends Node implements Client {
    private final Address viewServer;
    private View currentView;
    private boolean gettingNewView;
    private int sequenceNumber, retryCount;
    private AMOCommand command;
    private AMOResult result;

    //TODO: declare fields for your implementation ...

    /* -------------------------------------------------------------------------
        Construction and Initialization
       -----------------------------------------------------------------------*/
    public PBClient(Address address, Address viewServer) {
        super(address);
        this.viewServer = viewServer;
    }

    @Override
    public synchronized void init() {
        // TODO: initialize fields ...
        sequenceNumber = 0;
        gettingNewView = true;
        send(new GetView(), viewServer);
        currentView = new View(STARTUP_VIEWNUM, null, null);
        set(new ViewTimer(currentView.viewNum()), VIEW_MILLIS);
    }

    /* -------------------------------------------------------------------------
        Client Methods
       -----------------------------------------------------------------------*/
    @Override
    public synchronized void sendCommand(Command command) {
        //TODO: send command to server ...
        sequenceNumber++;
        AMOCommand c = new AMOCommand(command, sequenceNumber,
                this.address());

        AMOCommand cprime = new AMOCommand(command, sequenceNumber+1,
                this.address());

        this.command = c;
        this.result = null;
        if (currentView.primary() != null) {
            retryCount = 0;
            this.send(new Request(currentView,c), currentView.primary());
            this.set(new ClientTimer(c, sequenceNumber), CLIENT_RETRY_MILLIS);
        }
    }

    @Override
    public synchronized boolean hasResult() {
        //TODO: check whether there is result ...
        return result != null;
    }

    @Override
    public synchronized Result getResult() throws InterruptedException {
        //TODO: wait to get result ...
        while (result == null) {
            wait();
        }
        return result.result();
    }

    /* -------------------------------------------------------------------------
        Message Handlers
       -----------------------------------------------------------------------*/
    private synchronized void handleReply(Reply m, Address sender) {
        //TODO: check desired reply arrive ...
        if (result == null && m.amoResult().sequenceNumber() == sequenceNumber) {
            command = null;
            result = m.amoResult();
            notifyAll();
            if (m.view().viewNum() > currentView.viewNum()) {
                gettingNewView = true;
                send(new GetView(), viewServer);
                set(new ViewTimer(currentView.viewNum()), VIEW_MILLIS);
            }
        }
    }
    
    private synchronized void handleViewReply(ViewReply m, Address sender) {
        //TODO: perform action when timer reach timeout ...
        if (gettingNewView) {
            if (m.view().viewNum() > currentView.viewNum()) {
                currentView = m.view();
                gettingNewView = false;
                if (command != null && currentView.primary() != null) {
                    retryCount = 0;
                    send(new Request(currentView, command), currentView.primary());
                    this.set(new ClientTimer(command, sequenceNumber),
                            CLIENT_RETRY_MILLIS);
                }
            } else if (m.view().viewNum() == currentView.viewNum() && currentView.backup() != null) {
                gettingNewView = false;
                if (command != null) {
                    send(new Request(currentView, command), currentView.backup());
                    this.set(new ClientTimer(command, sequenceNumber),
                            CLIENT_RETRY_MILLIS);
                }
            }
        }
    }

    private synchronized void handleInvalidReply(InvalidReply m, Address sender) {
        PBResult result = m.result();
        if (result instanceof InvalidView){
            if (currentView.viewNum() < ((InvalidView) result).view().viewNum() && !gettingNewView) {
                gettingNewView = true;
                send(new GetView(), viewServer);
                set(new ViewTimer(currentView.viewNum()), VIEW_MILLIS);
            }
        }
    }
    // TODO: add utils here ...

    /* -------------------------------------------------------------------------
        Timer Handlers
       -----------------------------------------------------------------------*/
    private synchronized void onClientTimer(ClientTimer t) {
        // TODO: handle client request timeout ...
        if (command != null && t.sequenceNumber() == sequenceNumber && !gettingNewView) {
            if (retryCount > 10) {
                send(new GetView(), viewServer);
                set(new ViewTimer(currentView.viewNum()), CLIENT_RETRY_MILLIS);
                gettingNewView = true;
                retryCount = 0;
            } else {
                this.send(new Request(currentView, command), currentView.primary());
                this.set(t, CLIENT_RETRY_MILLIS);
                retryCount++;
            }
        } else {
            //System.out.println("Timer no longer needed");
        }
    }

    private synchronized void onViewTimer(ViewTimer t) {
        if (t.viewNum() == currentView.viewNum() && gettingNewView) {
            send(new GetView(), viewServer);
            set(t, VIEW_MILLIS);
        }
    }

}
