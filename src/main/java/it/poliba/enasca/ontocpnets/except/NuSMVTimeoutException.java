package it.poliba.enasca.ontocpnets.except;

import java.util.concurrent.TimeUnit;

/**
 * Thrown when the NuSMV process fails to exit before the waiting time has elapsed.
 */
public class NuSMVTimeoutException extends NuSMVRuntimeException {
    public NuSMVTimeoutException(String[] commandLine, long timeout, TimeUnit unit) {
        super(String.format("%s: the process did not exit after %d seconds",
                buildMessage(commandLine), unit.toSeconds(timeout)));
    }
}
