package it.poliba.enasca.ontocpnets.sat;

import it.poliba.enasca.ontocpnets.except.SATRuntimeException;
import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVec;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.TimeoutException;
import org.sat4j.tools.ModelIterator;

import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * A boolean SAT solver based on the SAT4J library.
 */
public class SAT4JSolver {
    private static int MAXLITERAL_DEFAULT = 0;
    private static int MAXRESULTS_UNLIMITED = -1;

    /**
     * If <code>!= MAXLITERAL_DEFAULT</code>, this value overrides the number of variables
     * for problems handled by this solver.
     */
    private int maxLiteral;

    public SAT4JSolver() {
        maxLiteral = MAXLITERAL_DEFAULT;
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
     * @param maxLiteral a positive integer indicating a fixed number of variables for problems
     *                   handled by this <code>SAT4JSolver</code>
     * @throws IllegalArgumentException if <code>maxLiteral &lt; 1</code>
     */
    public SAT4JSolver(int maxLiteral) {
        setMaxLiteral(maxLiteral);
    }

    /**
     * Sets the number of variables for problems handled by this solver.
     * @throws IllegalArgumentException if <code>maxLiteral &lt; 1</code>
     */
    public void setMaxLiteral(int maxLiteral) {
        if (maxLiteral < 1) throw new IllegalArgumentException();
        this.maxLiteral = maxLiteral;
    }

    /**
         * Returns <code>true</code> if models exist for the input problem.
         * @param problem
         * @return
         */
    public boolean isSatisfiable(BooleanFormula problem) {
        return models(Objects.requireNonNull(problem), 0) != null;
    }

    /**
     * Solves a boolean satisfiability problem expressed in DIMACS CNF format.
     * If the <code>maxLiteral</code> parameter has been set using {@link #setMaxLiteral(int)}
     * the solver will use <code>maxLiteral</code> as the total number of variables in the boolean problem,
     * otherwise the actual number of variables in <code>problem</code> will be used.
     * @param problem
     * @return the satisfiable models for the input problem
     */
    public Stream<DimacsLiterals> solve(BooleanFormula problem) {
        Stream<DimacsLiterals> modelStream = models(Objects.requireNonNull(problem), MAXRESULTS_UNLIMITED);
        return modelStream != null ? modelStream : Stream.empty();
    }

    /**
     * Returns <code>true</code> if <code>formula</code> implies <code>clause</code>.
     *
     * <p>This is equivalent to
     * <pre>{@code
     * BooleanFormula testFormula = formula.copy();
     * testFormula.addNegatedClause(clause);
     * return !isSatisfiable(testFormula);
     * }</pre>
     * @param formula
     * @param clause
     * @return
     */
    public boolean implies(BooleanFormula formula, DimacsLiterals clause) {
        BooleanFormula testFormula = formula.copy();
        testFormula.addNegatedClause(clause);
        return !isSatisfiable(testFormula);
    }

    /**
     * Finds up to <code>maxResults</code> models for the specified problem.
     * If <code>maxResults &lt; 0</code>, all models are returned.
     * If <code>problem</code> is unsatisfiable, <code>null</code> is returned.
     * @param problem
     * @param maxResults if <code>!= MAXRESULTS_UNLIMITED</code>, limits the maximum number
     *                   of solutions to be returned, otherwise has no effect
     * @return the models of the input problem, or <code>null</code> if the input problem
     * is unsatisfiable
     */
    private Stream<DimacsLiterals> models(BooleanFormula problem, int maxResults) {
        // Build the internal representation of the input problem.
        IVec<IVecInt> problemAsIVec = problem.clauses().collect(toIVec());
        ModelIterator solver = new ModelIterator(SolverFactory.newDefault());
        if (maxLiteral != MAXLITERAL_DEFAULT) {
            solver.newVar(maxLiteral);
        }
        solver.setExpectedNumberOfClauses(problemAsIVec.size());
        try {
            solver.addAllClauses(problemAsIVec);
        } catch (ContradictionException e) {
            return null;
        }
        // Find models.
        Stream.Builder<DimacsLiterals> builder = Stream.builder();
        try {
            if (!solver.isSatisfiable()) {
                return null;
            }
            if (maxResults > 0) {
                builder.accept(new DimacsLiterals(solver.model()));
            }
            while ((solver.numberOfModelsFoundSoFar() < maxResults || maxResults == MAXRESULTS_UNLIMITED)
                    && solver.isSatisfiable()) {
                builder.accept(new DimacsLiterals(solver.model()));
            }
        } catch (TimeoutException e) {
            throw new SATRuntimeException(e);
        } finally {
            // Free the resources acquired by solver.
            // This call prevents memory leak issues in the SAT4J library.
            solver.reset();
        }
        return builder.build();
    }

    /**
     * Returns a Collector that accumulates input clauses into an <code>IVec<IVecInt></code>,
     * which is the representation used by the SAT4J library for collections of clauses.
     * @return
     */
    private static Collector<DimacsLiterals, ?, IVec<IVecInt>> toIVec() {
        return Collector.of(
                Vec::new,
                (vec, dimacs) -> vec.push(new VecInt(dimacs.literals)),
                (left, right) -> { right.moveTo(left); return left; });
    }

}
