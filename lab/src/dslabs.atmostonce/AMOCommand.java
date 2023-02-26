package dslabs.atmostonce;

import dslabs.framework.Address;
import dslabs.framework.Command;
import lombok.Data;

@Data
public final class AMOCommand implements Command {
    //TODO: implement your wrapper for command
    //Hints: think carefully about what information is required for server to check duplication
    private final Command command;
    private final int sequenceNumber;
    private final Address clientAddress;
}
