package it.poliba.enasca.ontocpnets.except;

/**
 * Thrown when a {@link it.poliba.enasca.ontocpnets.sat.SATSolver} detects inconsistencies
 * in the set of input clauses.
 */
public class SATInvalidException extends Exception {
    public SATInvalidException(Throwable cause) {
        super(cause);
    }
}
