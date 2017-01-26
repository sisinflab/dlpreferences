package it.poliba.enasca.ontocpnets.sat;

import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * A solver of boolean satisfiability problems.
 */
public abstract class SATSolver {
    /**
     * If <code>&gt; 0</code>, this value overrides the number of variables
     * for problems handled by this <code>SATSolver</code>.
     */
    protected int maxLiteral;

    protected SATSolver() {
        maxLiteral = 0;
    }

    /**
     * Sets the number of variables for problems handled by this <code>SATSolver</code>.
     */
    public void setMaxLiteral(int maxLiteral) {
        this.maxLiteral = maxLiteral;
    }

    /**
     * Returns <code>true</code> if models exist for the input problem.
     * @param problem
     * @return
     */
    public abstract boolean isSatisfiable(BooleanFormula problem);

    /**
     * Solves a boolean satisfiability problem expressed in DIMACS CNF format.
     * If the <code>maxLiteral</code> parameter has been set using {@link #setMaxLiteral(int)}
     * and <code>maxLiteral &gt; 0</code>, the solver will use <code>maxLiteral</code> as
     * the total number of variables in the boolean problem, otherwise the actual number
     * of variables in <code>problem</code> will be used.
     * @param problem
     * @return the satisfiable models for the input problem
     */
    public abstract Stream<DIMACSLiterals> solve(BooleanFormula problem);

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
}
