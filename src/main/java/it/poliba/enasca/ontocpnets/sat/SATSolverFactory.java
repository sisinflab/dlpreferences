package it.poliba.enasca.ontocpnets.sat;

/**
 * Contains static methods to build {@link SATSolver} instances.
 */
public class SATSolverFactory {

    /**
     * Returns the default <code>SATSolver</code> implementation.
     * @return
     */
    public static SATSolver defaultSolver() {
        return sat4JSolver();
    }

    /**
     * Returns a solver compatible with the SAT4J library.
     * @return
     */
    public static SAT4JSolver sat4JSolver() {
        return new SAT4JSolver();
    }

}
