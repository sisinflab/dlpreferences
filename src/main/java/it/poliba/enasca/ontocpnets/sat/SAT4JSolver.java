package it.poliba.enasca.ontocpnets.sat;

import it.poliba.enasca.ontocpnets.except.SATRuntimeException;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;
import org.sat4j.tools.ModelIterator;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * A boolean SAT solver that uses the SAT4J library.
 */
public class SAT4JSolver extends SATSolver {
    public SAT4JSolver() {
        super();
    }

    /**
     * @param problem
     * @param maxLiteral
     * @return
     * @throws SATRuntimeException if the solver behaves unexpectedly while computing the satisfiable models
     */
    @Override
    protected Stream<DIMACSLiterals> solve(BooleanFormula problem, int maxLiteral) {
        Objects.requireNonNull(problem);
        // Configure the SAT4J solver.
        ModelIterator solver = new ModelIterator(SolverFactory.newDefault());
        solver.newVar(maxLiteral);
        solver.setExpectedNumberOfClauses(problem.size());
        // Add clauses.
        try {
            for (DIMACSLiterals clause : problem.clauses) {
                solver.addClause(new VecInt(clause.literals));
            }
        } catch (ContradictionException e) {
            // the problem is unsatisfiable
            return Stream.empty();
        }
        // Find satisfiable models.
        Stream.Builder<DIMACSLiterals> models = Stream.builder();
        try {
            while (solver.isSatisfiable()) {
                models.accept(new DIMACSLiterals(solver.model()));
            }
        } catch (TimeoutException e) {
            throw new SATRuntimeException(e);
        }
        return models.build();
    }
}
