package dslabs.primarybackup;

import dslabs.atmostonce.AMOApplication;
import dslabs.atmostonce.AMOCommand;
import dslabs.atmostonce.AMOResult;
import dslabs.framework.Address;
import dslabs.framework.Application;
import dslabs.framework.Node;
import dslabs.framework.Result;
import dslabs.kvstore.KVStore;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.java.Log;

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
    private boolean isPrimary, isBackup, isSyncing;
    private View currentView, nextView;
    private Address primary, backup;

    private AMOCommand currentCommand;


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
        isPrimary = false;
        isBackup = false;
        primary = null;
        backup = null;
        currentView = new View(STARTUP_VIEWNUM, null, null);
        nextView = null;
        currentCommand = null;
    }

    /* -------------------------------------------------------------------------
        Message Handlers
       -----------------------------------------------------------------------*/
    private void handleRequest(Request m, Address sender) {
        // TODO: handle client request ...
        if (isPrimary) {
            if (isSyncing) {return;}

            if (currentCommand != null) {return;}


            if (backup != null) {
                currentCommand = m.amoCommand();
                send(new BackupRequest(currentView, currentCommand), backup);
                set(new BackupTimer(), BACKUP_MILLIS);
                return;
            }
            AMOCommand command = m.amoCommand();
            AMOResult result = amoApplication.execute(command);
            send(new Reply(result), sender);
        }
    }

    private void handleViewReply(ViewReply m, Address sender) {
        // TODO: handle view reply from view server ...
        View globalView = m.view();
        if (globalView.viewNum() > currentView.viewNum()) {
            primary = globalView.primary();
            backup = globalView.backup();
            if (globalView.primary().equals(address())) {
                isPrimary = true;
                isBackup = false;
                if (backup != null) {
                    isSyncing = true;
                    nextView = globalView;
                    // TODO: send sync message to backup and set SyncTimer...
                    AMOApplication<AMOApplication<KVStore>> syncApplication =
                            new AMOApplication<>(amoApplication);
                    send(new SyncRequest(currentView, syncApplication), backup);
                    set(new SyncTimer(), SYNC_MILLIS);
                    return;
                }
            } else if (globalView.backup().equals(address())) {
                isPrimary = false;
                isBackup = true;
            } else {
                isPrimary = false;
                isBackup = false;
            }
            isSyncing = false;
            nextView = null;
            currentView = globalView;
        }
    }

    // TODO: your message handlers ...

    private void handleBackupRequest(BackupRequest m, Address sender) {
        // TODO: handle backup request from primary ...
    }

    private void handleSyncRequest(Address sender) {
        // TODO: handle application state sync request from primary ...
    }


    /* -------------------------------------------------------------------------
        Timer Handlers
       -----------------------------------------------------------------------*/
    private void onPingTimer(PingTimer t) {
        // TODO: on ping timeout ...
        send(new Ping(currentView.viewNum()), viewServer);
        set(t, PING_MILLIS);
    }

    // TODO: your message handlers ...
    private void onBackupTimer(BackupTimer t) {
        // TODO: on backup timeout ...
    }

    private void onSyncTimer(SyncTimer t) {
        // TODO: on sync timeout ...
        if (isSyncing) {
            AMOApplication<AMOApplication<KVStore>> syncApplication =
                    new AMOApplication<>(amoApplication);
            send(new SyncRequest(currentView, syncApplication), backup);
            set(t, SYNC_MILLIS);
        }
    }


    /* -------------------------------------------------------------------------
        Utils
       -----------------------------------------------------------------------*/
    // TODO: add utils here ...
}
