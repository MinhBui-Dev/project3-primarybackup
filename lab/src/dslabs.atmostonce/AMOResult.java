package dslabs.atmostonce;

import dslabs.framework.Result;
import lombok.Data;

@Data
public final class AMOResult implements Result {
    //TODO: implement your wrapper for result
    //Hints: think carefully about what information is required for client to check duplication
    private final Result result;
    private final int sequenceNumber;
}
