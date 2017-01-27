package it.poliba.enasca.ontocpnets.sat;

import it.poliba.enasca.ontocpnets.except.SATRuntimeException;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;
import org.sat4j.tools.ModelIterator;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * A boolean SAT solver based on the SAT4J library.
 */
public class SAT4JSolver {
    /**
     * If <code>&gt; 0</code>, this value overrides the number of variables
     * for problems handled by this solver.
     */
    protected int maxLiteral;

    public SAT4JSolver() {
        maxLiteral = 0;
    }

    /**
     * Constructs a <code>SAT4JSolver</code> that defines <code>maxLiteral</code> variables
     * when solving each problem. If the actual number of variables in a problem
     * is less than <code>maxLiteral</code>, the remaining variables are generated automatically.
     *
     * <p>This is equivalent to
     * <pre>{@code
     * SAT4JSolver s = new SAT4JSolver();
     * s.setMaxLiteral(maxLiteral);
     * }</pre>
     * @param maxLiteral
     */
    public SAT4JSolver(int maxLiteral) {
        this.maxLiteral = maxLiteral;
    }

    /**
     * Sets the number of variables for problems handled by this solver.
     */
    public void setMaxLiteral(int maxLiteral) {
        this.maxLiteral = maxLiteral;
    }

    /**
         * Returns <code>true</code> if models exist for the input problem.
         * @param problem
         * @return
         */
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
         * Solves a boolean satisfiability problem expressed in DIMACS CNF format.
         * If the <code>maxLiteral</code> parameter has been set using {@link #setMaxLiteral(int)}
         * and <code>maxLiteral &gt; 0</code>, the solver will use <code>maxLiteral</code> as
         * the total number of variables in the boolean problem, otherwise the actual number
         * of variables in <code>problem</code> will be used.
         * @param problem
         * @return the satisfiable models for the input problem
         */
    public Stream<DIMACSLiterals> solve(BooleanFormula problem) {
        Optional<ModelIterator> modelIterator = populate(problem);
        if (!modelIterator.isPresent()) return Stream.empty();
        return models(modelIterator.get());
    }

    /**
     * Returns <code>true</code> if <code>formula</code> implies <code>clause</code>.
     *
     * <p>This is equivalent to
     * <pre>{@code
     * BooleanFormula testFormula = BooleanFormula.copyOf(formula);
     * testFormula.addNegatedClause(clause);
     * return !isSatisfiable(testFormula);
     * }</pre>
     * @param formula
     * @param clause
     * @return
     */
    public boolean implies(BooleanFormula formula, IntStream clause) {
        BooleanFormula testFormula = BooleanFormula.copyOf(formula);
        testFormula.addNegatedClause(clause);
        return !isSatisfiable(testFormula);
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

}
