package dslabs.primarybackup;

import dslabs.atmostonce.AMOApplication;
import dslabs.atmostonce.AMOCommand;
import dslabs.atmostonce.AMOResult;
import dslabs.framework.Address;
import dslabs.framework.Message;
import dslabs.kvstore.KVStore;
import dslabs.primarybackup.PBServer.PBResult;
import lombok.Data;

/* -------------------------------------------------------------------------
    ViewServer Messages
   -----------------------------------------------------------------------*/
@Data
class Ping implements Message {
    private final int viewNum;
}

@Data
class GetView implements Message {
}

@Data
class ViewReply implements Message {
    private final View view;
}

/* -------------------------------------------------------------------------
    Primary-Backup Messages
   -----------------------------------------------------------------------*/
@Data
class Request implements Message {
    // TODO: client request ...
    private final View view;
    private final AMOCommand amoCommand;
}

@Data
class Reply implements Message {
    //TODO: server response ...
    private final View view;
    private final AMOResult amoResult;
}

@Data
class BackupRequest implements Message {
    // TODO: primary send backup request to backup ...
    private final int viewNum;
    private final AMOCommand amoCommand;
}

@Data
class BackupReply implements Message {
    // TODO: backup send reply to primary ...
    private final int viewNum;
    private final AMOCommand command;
}

@Data
class SyncRequest implements Message {
    // TODO: primary sync up with backup ...
    private final View view;
    private final AMOApplication<AMOApplication<KVStore>> amoApplication;
}

@Data
class SyncReply implements Message {
    // TODO: backup send reply to primary ...
    private final int viewNum;
}

// TODO: add more messages here ...

@Data
class ViewRequest implements Message {
    private final int viewNum;
}

@Data
class InvalidReply implements Message{
    private final PBResult result;
}

@Data
class RelayRequest implements Message {
    private final int viewNum;
    private final AMOCommand amoCommand;
}

@Data
class RelayReply implements Message {
    private final int viewNum;
    private final AMOCommand amoCommand;
}
