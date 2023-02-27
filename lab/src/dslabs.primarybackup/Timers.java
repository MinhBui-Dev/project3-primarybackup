package dslabs.primarybackup;

import dslabs.atmostonce.AMOCommand;
import dslabs.framework.Address;
import dslabs.framework.Command;
import dslabs.framework.Timer;
import lombok.Data;

@Data
final class PingCheckTimer implements Timer {
    static final int PING_CHECK_MILLIS = 100;
}

@Data
final class PingTimer implements Timer {
    static final int PING_MILLIS = 25;
}

@Data
final class ClientTimer implements Timer {
    static final int CLIENT_RETRY_MILLIS = 100;
    private final Command command;

    // TODO: add fields for client request
}

// TODO: add more timers here ...
@Data
final class SyncTimer implements Timer {
    static final int SYNC_MILLIS = 100;
    private final Address backup;
}

@Data
final class BackupTimer implements Timer {
    static final int BACKUP_MILLIS = 50;
    private final AMOCommand command;
}
/*
@Data final class ViewTimer implements Timer {
    static final int VIEW_MILLIS = 50;
}*/
