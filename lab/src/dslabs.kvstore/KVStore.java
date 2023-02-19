package dslabs.kvstore;

import dslabs.framework.Application;
import dslabs.framework.Command;
import dslabs.framework.Result;
import java.util.HashMap;
import java.util.Map;
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
    private final Map<String, String> kvstore = new HashMap<>();

    public KVStore() {}

    // copy constructor
    public KVStore(KVStore application){
        //TODO: a deepcopy constructor
        //Hints: remember to deepcopy all fields, especially the mutable ones
        this.kvstore.putAll(application.kvstore);
    }

    @Override
    public KVStoreResult execute(Command command) {
        if (command instanceof Get) {
            Get g = (Get) command;
            //TODO: get the value
            if (kvstore.containsKey(g.key())) {
                return new GetResult(kvstore.get(g.key()));
            } else {
                return new KeyNotFound();
            }
        }

        if (command instanceof Put) {
            Put p = (Put) command;
            //TODO: put the value
            kvstore.put(p.key(), p.value());
            return new PutOk();
        }

        if (command instanceof Append) {
            Append a = (Append) command;
            //TODO: append after previous
            if (kvstore.containsKey(a.key())) {
                kvstore.put(a.key(), kvstore.get(a.key()) + a.value());
            } else {
                kvstore.put(a.key(), a.value());
            }
            return new AppendResult(kvstore.get(a.key()));
        }

        throw new IllegalArgumentException();
    }
}
