package it.poliba.enasca.ontocpnets.except;

/**
 * Thrown when a NuSMV process is interrupted before completion.
 */
public class NuSMVInterruptedException extends NuSMVRuntimeException {
    public NuSMVInterruptedException(String[] commandLine, Throwable cause) {
        super(commandLine, cause);
    }
}
