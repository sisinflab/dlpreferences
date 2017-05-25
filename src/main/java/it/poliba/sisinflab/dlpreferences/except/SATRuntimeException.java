package it.poliba.sisinflab.dlpreferences.except;

import it.poliba.sisinflab.dlpreferences.sat.SAT4JSolver;

/**
 * The superclass of runtime exceptions thrown by a {@link SAT4JSolver}.
 */
public class SATRuntimeException extends RuntimeException {
    public SATRuntimeException(Throwable cause) {
        super(cause);
    }
}
