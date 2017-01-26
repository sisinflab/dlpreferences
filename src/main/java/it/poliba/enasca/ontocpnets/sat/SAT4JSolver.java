package it.poliba.enasca.ontocpnets.sat;

import it.poliba.enasca.ontocpnets.except.SATRuntimeException;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;
import org.sat4j.tools.ModelIterator;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A boolean SAT solver that uses the SAT4J library.
 */
public class SAT4JSolver extends SATSolver {
    public SAT4JSolver() {
        super();
    }

    /**
     * Creates a {@link ModelIterator} object ready to find satisfiable models
     * for the specified problem.
     * If <code>problem</code> is trivially unsatisfiable, an empty <code>Optional</code> is returned.
     * @param problem
     * @return a solver for the specified problem, or an empty <code>Optional</code>
     * if the problem is trivially unsatisfiable.
     */
    private Optional<ModelIterator> populate(BooleanFormula problem) {
        Objects.requireNonNull(problem);
        ModelIterator solver = new ModelIterator(SolverFactory.newDefault());
        if (maxLiteral > 0) {
            solver.newVar(maxLiteral);
        }
        solver.setExpectedNumberOfClauses(problem.size());
        try {
            for (DIMACSLiterals clause : problem.clauses) {
                solver.addClause(new VecInt(clause.literals));
            }
        } catch (ContradictionException e) {
            solver = null;
        }
        return Optional.ofNullable(solver);
    }

    /**
     * Finds satisfiable models using the specified <code>ModelIterator</code>.
     * If the problem is unsatisfiable, an empty <code>Stream</code> is returned.
     * @param solver
     * @return
     * @throws SATRuntimeException if the solver behaves unexpectedly while computing satisfiable models
     */
    private static Stream<DIMACSLiterals> models(ModelIterator solver) {
        Objects.requireNonNull(solver);
        Stream.Builder<DIMACSLiterals> builder = Stream.builder();
        try {
            while (solver.isSatisfiable()) {
                builder.accept(new DIMACSLiterals(solver.model()));
            }
        } catch (TimeoutException e) {
            throw new SATRuntimeException(e);
        }
        return builder.build();
    }

    /**
     * @param problem
     * @return
     * @throws SATRuntimeException if the solver behaves unexpectedly while computing satisfiable models
     */
    @Override
    public boolean isSatisfiable(BooleanFormula problem) {
        Optional<ModelIterator> modelIterator = populate(problem);
        if (!modelIterator.isPresent()) return false;
        boolean result;
        try {
            result = modelIterator.get().isSatisfiable();
        } catch (TimeoutException e) {
            throw new SATRuntimeException(e);
        }
        return result;
    }

    /**
     * @param problem
     * @return
     * @throws SATRuntimeException if the solver behaves unexpectedly while computing satisfiable models
     */
    @Override
    public Stream<DIMACSLiterals> solve(BooleanFormula problem) {
        Optional<ModelIterator> modelIterator = populate(problem);
        if (!modelIterator.isPresent()) return Stream.empty();
        return models(modelIterator.get());
    }
}
