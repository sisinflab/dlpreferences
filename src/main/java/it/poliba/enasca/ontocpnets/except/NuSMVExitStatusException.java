package it.poliba.enasca.ontocpnets.except;

/**
 * Thrown when the NuSMV process exits with a non-zero status code.
 */
public class NuSMVExitStatusException extends NuSMVRuntimeException {
    public NuSMVExitStatusException(String[] commandLine, int exitStatus) {
        super(String.format("%s: the NuSMV process exited with status code %d",
                buildMessage(commandLine), exitStatus));
    }
}
