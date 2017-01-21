package it.poliba.enasca.ontocpnets.except;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * The superclass of runtime exceptions concerning the NuSMV process invoked by {@link it.poliba.enasca.ontocpnets.CPNet}.
 */
public class NuSMVRuntimeException extends RuntimeException {
    public NuSMVRuntimeException(String message) {
        super(message);
    }

    public NuSMVRuntimeException(String[] commandLine, Throwable cause) {
        super(buildMessage(commandLine), cause);
    }

    /**
     * Builds an exception message by joining the elements of <code>commandLine</code>.
     *
     * @param commandLine
     * @return
     */
    static String buildMessage(String[] commandLine) {
        return String.format("error while running '%s'", Arrays.stream(commandLine).collect(Collectors.joining(" ")));
    }
}
