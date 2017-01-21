package it.poliba.enasca.ontocpnets.sat;

/**
 * Contains static methods to build {@link SATSolver} instances.
 */
public class SATSolverFactory {
    public static SATSolver defaultSolver() {
        return sat4JSolver();
    }
    static SAT4JSolver sat4JSolver() {
        return new SAT4JSolver();
    }
}
