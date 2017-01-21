package it.poliba.enasca.ontocpnets.sat;

import it.poliba.enasca.ontocpnets.except.SATInvalidException;

import java.util.Set;
import java.util.stream.Stream;

/**
 * A solver of boolean satisfiability problems.
 */
public interface SATSolver {
    /**
     * Solves a boolean satisfiability problem expressed in DIMACS CNF format.
     * @param clauses
     * @param maxLiteral the number of variables in the problem.
     * @return a stream of satisfiable models of the problem.
     * A model is a set of literals ranging from 1 to <code>maxLiteral</code>.
     * @throws SATInvalidException if the solver detects inconsistencies in the list of clauses
     * @throws NullPointerException if <code>clauses</code> is <code>null</code> or contains <code>null</code>s.
     */
    Stream<Set<Integer>> solveDimacsCNF(Stream<Set<Integer>> clauses, int maxLiteral)
            throws SATInvalidException;
}
