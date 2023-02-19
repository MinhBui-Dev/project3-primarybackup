package dslabs.atmostonce;

import dslabs.framework.Application;
import dslabs.framework.Command;
import dslabs.framework.Result;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode
@ToString
@RequiredArgsConstructor
public final class AMOApplication<T extends Application>
        implements Application {
    @Getter @NonNull private final T application;

    //TODO: declare fields for your implementation
    private Map<String, Integer> clientSequenceNumber = new HashMap<>();
    private final Map<String, Result> clientResult = new HashMap<>();

    @Override
    public AMOResult execute(Command command) {
        if (!(command instanceof AMOCommand)) {
            throw new IllegalArgumentException();
        }

        AMOCommand amoCommand = (AMOCommand) command;

        //TODO: execute the command
        //Hints: remember to check whether the command is executed before and update records
        if (alreadyExecuted(amoCommand)) {
            return new AMOResult(clientResult.get(amoCommand.clientAddress()),amoCommand.sequenceNumber());
        } else {
            Result result = application.execute(amoCommand.command());
            clientSequenceNumber.put(amoCommand.clientAddress(), amoCommand.sequenceNumber());
            clientResult.put(amoCommand.clientAddress(), result);
            return new AMOResult(result, amoCommand.sequenceNumber());
        }
    }

    // copy constructor
    public AMOApplication(AMOApplication<Application> amoApplication)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Application application = amoApplication.application();
        this.application = (T) application.getClass().getConstructor(application.getClass()).newInstance(application);
        //TODO: a deepcopy constructor
        //Hints: remember to deepcopy all fields, especially the mutable ones

        this.clientSequenceNumber = new HashMap<>(amoApplication.clientSequenceNumber);
        for (Map.Entry<String, Result> entry : amoApplication.clientResult.entrySet()) {
            this.clientResult.put(entry.getKey(), entry.getValue());
        }
    }

    public Result executeReadOnly(Command command) {
        if (!command.readOnly()) {
            throw new IllegalArgumentException();
        }

        if (command instanceof AMOCommand) {
            return execute(command);
        }

        return application.execute(command);
    }

    public boolean alreadyExecuted(Command command) {
        if (!(command instanceof AMOCommand)) {
            throw new IllegalArgumentException();
        }

        AMOCommand amoCommand = (AMOCommand) command;

        //TODO: check whether the amoCommand is already executed or not

        if (clientSequenceNumber.containsKey(amoCommand.clientAddress())) {
            if (clientSequenceNumber.get(amoCommand.clientAddress()) >= amoCommand.sequenceNumber()) {
                return true;
            }
        }
        return false;
    }
}
