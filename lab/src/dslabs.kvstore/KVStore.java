package dslabs.kvstore;

import dslabs.framework.Application;
import dslabs.framework.Command;
import dslabs.framework.Result;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;


@ToString
@EqualsAndHashCode
public class KVStore implements Application {

    public interface KVStoreCommand extends Command {
    }

    public interface SingleKeyCommand extends KVStoreCommand {
        String key();
    }

    @Data
    public static final class Get implements SingleKeyCommand {
        @NonNull private final String key;

        @Override
        public boolean readOnly() {
            return true;
        }
    }

    @Data
    public static final class Put implements SingleKeyCommand {
        @NonNull private final String key, value;
    }

    @Data
    public static final class Append implements SingleKeyCommand {
        @NonNull private final String key, value;
    }

    public interface KVStoreResult extends Result {
    }

    @Data
    public static final class GetResult implements KVStoreResult {
        @NonNull private final String value;
    }

    @Data
    public static final class KeyNotFound implements KVStoreResult {
    }

    @Data
    public static final class PutOk implements KVStoreResult {
    }

    @Data
    public static final class AppendResult implements KVStoreResult {
        @NonNull private final String value;
    }

    //TODO: declare fields for your implementation

    public KVStore() {}

    // copy constructor
    public KVStore(KVStore application){
        //TODO: a deepcopy constructor
        //Hints: remember to deepcopy all fields, especially the mutable ones
    }

    @Override
    public KVStoreResult execute(Command command) {
        if (command instanceof Get) {
            Get g = (Get) command;
            //TODO: get the value
        }

        if (command instanceof Put) {
            Put p = (Put) command;
            //TODO: put the value
        }

        if (command instanceof Append) {
            Append a = (Append) command;
            //TODO: append after previous
        }

        throw new IllegalArgumentException();
    }
}
