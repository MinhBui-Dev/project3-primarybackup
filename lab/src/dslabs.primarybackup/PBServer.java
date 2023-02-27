package dslabs.primarybackup;

import dslabs.atmostonce.AMOApplication;
import dslabs.atmostonce.AMOCommand;
import dslabs.atmostonce.AMOResult;
import dslabs.framework.Address;
import dslabs.framework.Application;
import dslabs.framework.Node;
import dslabs.framework.Result;
import dslabs.kvstore.KVStore;
import java.util.LinkedList;
import java.util.Queue;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.java.Log;
import sun.security.krb5.internal.ReplayCache;

import static dslabs.primarybackup.BackupTimer.BACKUP_MILLIS;
import static dslabs.primarybackup.SyncTimer.SYNC_MILLIS;
import static dslabs.primarybackup.ViewServer.STARTUP_VIEWNUM;
import static dslabs.primarybackup.PingTimer.PING_MILLIS;

@Log
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
class PBServer extends Node {
    private final Address viewServer;

    public interface PBResult extends Result {
    }

    @Data
    public static final class InvalidView implements PBResult {
        @NonNull private final View view;
    }

    @Data
    public static final class BackupSuccess implements PBResult {
    }

    @Data
    public static final class SyncSuccess implements PBResult {
    }

    // TODO: declare fields for your implementation ...
    private AMOApplication<KVStore> amoApplication;
    private boolean isPrimary, isBackup, isSyncing, isSynced, waitingOnBackup;
    private View currentView;
    private AMOCommand currentCommand, relayCommand;
    private int oldViewNum, backupReTry;
    private Queue<AMOCommand> commandQueue;



    /* -------------------------------------------------------------------------
        Construction and Initialization
       -----------------------------------------------------------------------*/
    PBServer(Address address, Address viewServer, Application app) {
        super(address);
        this.viewServer = viewServer;

        // TODO: wrap app inside AMOApplication ...
        amoApplication = new AMOApplication<>((KVStore) app);
    }

    @Override
    public void init() {
        // TODO: initialize fields ...
        currentView = new View(STARTUP_VIEWNUM, null, null);
        currentCommand = null;
        relayCommand = null;
        send(new Ping(STARTUP_VIEWNUM), viewServer);
        set(new PingTimer(), PING_MILLIS);
        commandQueue = new LinkedList<>();
    }

    /* -------------------------------------------------------------------------
        Message Handlers
       -----------------------------------------------------------------------*/
    private void handleRequest(Request m, Address sender) {
        // TODO: handle client request ...
        if (isPrimary && m.view().viewNum() == currentView.viewNum()) {
            if (isSyncing || waitingOnBackup) {
                if (!commandQueue.contains(m.amoCommand())){
                    commandQueue.add(m.amoCommand());
                }
                return;
            }

            if (currentView.backup() != null) {
                if (amoApplication.alreadyExecuted(m.amoCommand())){
                    AMOResult result = amoApplication.execute(m.amoCommand());
                    send(new Reply(currentView,result), sender);
                } else {
                    currentCommand = m.amoCommand();
                    waitingOnBackup = true;
                    send(new BackupRequest(currentView.viewNum(), currentCommand),
                            currentView.backup());
                    set(new BackupTimer( currentCommand),
                            BACKUP_MILLIS);
                }
            } else {
                AMOCommand command = m.amoCommand();
                AMOResult result = amoApplication.execute(command);
                send(new Reply(currentView, result), sender);
            }
        } else if (isBackup && m.view().viewNum() == currentView.viewNum()) {
            if (relayCommand == null || relayCommand.equals(m.amoCommand())) {
                relayCommand = m.amoCommand();
                send(new RelayRequest(currentView.viewNum(), m.amoCommand()), currentView.primary());
            }
        } /*else {
            InvalidView invalidView = new InvalidView(currentView);
            send(new InvalidReply(invalidView), sender);
        }*/
    }

    private void handleRelayRequest(RelayRequest m, Address sender){
        if (isPrimary && m.viewNum() == currentView.viewNum()) {
            if (currentCommand != null) {
                relayCommand = m.amoCommand();
                return;
            }
            amoApplication.execute(m.amoCommand());
            send(new RelayReply(currentView.viewNum(),m.amoCommand()),sender);
        }
    }
    private void handleRelayReply(RelayReply m, Address sender) {
        if (m.viewNum() == currentView.viewNum()){
            if ( relayCommand != null && relayCommand.equals(m.amoCommand())) {
                if (currentCommand != null) {
                    amoApplication.execute(currentCommand);
                }
                AMOResult result = amoApplication.execute(relayCommand);
                send(new Reply(currentView, result), relayCommand.clientAddress());
                relayCommand = null;
            }
        }
    }

    private void handleViewReply(ViewReply m, Address sender) {
        // TODO: handle view reply from view server ...
        //if (m.view() == null) {return;}
        View viewServerView = m.view();
        if (viewServerView.viewNum() > currentView.viewNum()) {
            oldViewNum = currentView.viewNum();
            currentView = viewServerView;

            if (currentView.primary().equals(this.address())){ // I am the primary
                isPrimary = true;
                isBackup = false;
                isSyncing = false;

                while (!commandQueue.isEmpty()) {
                    currentCommand = commandQueue.poll();
                    AMOResult result = amoApplication.execute(currentCommand);
                    send(new Reply(currentView, result),
                            currentCommand.clientAddress());
                }
                currentCommand = null;
                waitingOnBackup = false;

                if (currentView.backup() != null) { // there is a backup
                        isSyncing = true;
                        AMOApplication<AMOApplication<KVStore>> syncApplication =
                                new AMOApplication<>(amoApplication);
                        send(new SyncRequest(currentView, syncApplication),
                                currentView.backup());
                        set(new SyncTimer(currentView.backup()), SYNC_MILLIS);
                }
            } else {
                currentCommand = null;
                commandQueue.clear();
                if (viewServerView.backup() == null || !viewServerView.backup().equals(this.address())) {
                    isBackup = false;
                }
                isPrimary = false;
                isSynced = false;
            }
        }
    }
    // TODO: your message handlers ...

    private void handleBackupRequest(BackupRequest m, Address sender) {
        // TODO: handle backup request from primary ...
        if (m.viewNum() == currentView.viewNum()) {
            if (relayCommand != null) {
                currentCommand = m.amoCommand();
                send(new BackupReply(currentView.viewNum(), currentCommand), sender);
            }
            amoApplication.execute(m.amoCommand());
            send(new BackupReply(currentView.viewNum(), m.amoCommand()), sender);
        }
    }

    private void handleBackupReply(BackupReply m, Address sender) {
        // TODO: handle backup reply from backup ...
        /*if (m.viewNum() == currentView.viewNum() && currentCommand != null) {
            if (isPrimary && backupResult == null && currentCommand.equals(m.command())) {
                backupResult = m.result();
                if (backupResult instanceof BackupSuccess) {
                    AMOResult result = amoApplication.execute(currentCommand);
                    send(new Reply(currentView, result), client);
                    currentCommand = null;
                    client = null;
                    if (relayCommand != null) {
                        amoApplication.execute(relayCommand);
                        send(new RelayReply(currentView.viewNum(),relayCommand,relayClient),sender);
                        relayCommand = null;
                        relayClient = null;
                    }
                }
            }
        }*/
        if(m.viewNum() == currentView.viewNum() &&currentCommand != null && currentCommand.equals(m.command()) && waitingOnBackup){
            AMOResult result = amoApplication.execute(currentCommand);
            send(new Reply(currentView, result), currentCommand.clientAddress());
            if (relayCommand != null) {
                amoApplication.execute(relayCommand);
                send(new RelayReply(currentView.viewNum(),relayCommand),sender);
                relayCommand = null;
            }

            if (commandQueue.size() > 0) {
                currentCommand = commandQueue.remove();
                if (currentView.backup() != null) {
                    send(new BackupRequest(currentView.viewNum(), currentCommand),
                            currentView.backup());
                    set(new BackupTimer(currentCommand), BACKUP_MILLIS);
                }else {
                    result = amoApplication.execute(currentCommand);
                    send(new Reply(currentView, result), currentCommand.clientAddress());
                }
            } else {
                currentCommand = null;
                waitingOnBackup = false;
            }
        }
    }

    private void handleSyncRequest(SyncRequest m, Address sender) {
        // TODO: handle application state sync request from primary ...
        if (m.view().viewNum() == currentView.viewNum()) {
            if (!isSynced) {
                this.amoApplication = m.amoApplication().application();
                currentCommand = null;
                commandQueue.clear();
                isSynced = true;
                isBackup = true;
                isPrimary = false;
                send(new SyncReply(m.view().viewNum()), sender);
            } else  {
                send(new SyncReply(m.view().viewNum()), sender);
            }
        }
    }

    private void handleSyncReply(SyncReply m, Address sender) {
        // TODO: handle application state sync reply from backup ...
        if (isPrimary && isSyncing && m.viewNum() == currentView.viewNum()) {
            isSyncing = false;
            if (commandQueue.size() > 0) {
                currentCommand = commandQueue.remove();
                waitingOnBackup = true;
                send(new BackupRequest(currentView.viewNum(), currentCommand),
                        currentView.backup());
                set(new BackupTimer( currentCommand),
                        BACKUP_MILLIS);
            }
        }
    }



    /* -------------------------------------------------------------------------
        Timer Handlers
       -----------------------------------------------------------------------*/
    private void onPingTimer(PingTimer t) {
        // TODO: on ping timeout ...
        if (isSyncing){
            send(new Ping(oldViewNum), viewServer);
            set(t, PING_MILLIS);
        } else {
            //System.out.println("Ping sent to view server: " + currentView.viewNum());
            send(new Ping(currentView.viewNum()), viewServer);
            set(t, PING_MILLIS);
        }
    }

    // TODO: your message handlers ...
    private void onBackupTimer(BackupTimer t) {
        // TODO: on backup timeout ...
        // If sync is in progress, backup dead, not primary, drop client request
        //System.out.println("Current view is " + currentView + " and Timber backup is " + t.backup() + " with Command " + t.command() + " Current command is " + currentCommand);
        if (!isPrimary || isSyncing || currentView.backup() == null){
            currentCommand = null;
            return;
        }
        if (currentCommand != null && currentCommand.equals(t.command()) && waitingOnBackup) {
            if (backupReTry < 3) {
                backupReTry++;
                send(new BackupRequest(currentView.viewNum(), currentCommand),
                        currentView.backup());
                set(t, BACKUP_MILLIS);
            } else {
                backupReTry = 0;
                send(new Ping(currentView.viewNum()), viewServer);
                set(t, PING_MILLIS);
            }

        }
    }

    private void onSyncTimer(SyncTimer t) {
        // TODO: on sync timeout ...
        // Syncing application state from backup is priority task
        // Check if still syncing and backup is still the same, and resend request
        if (isSyncing && currentView.backup() != null && currentView.backup().equals(t.backup())) {
            AMOApplication<AMOApplication<KVStore>> syncApplication =
                    new AMOApplication<>(amoApplication);
            send(new SyncRequest(currentView, syncApplication), currentView.backup());
            set(t, SYNC_MILLIS);
        }
    }


    /* -------------------------------------------------------------------------
        Utils
       -----------------------------------------------------------------------*/
    // TODO: add utils here ...
}
