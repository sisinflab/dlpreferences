package it.poliba.enasca.ontocpnets.sat;

import java.util.stream.Stream;

/**
 * A solver of boolean satisfiability problems.
 */
public abstract class SATSolver {
    /**
     * The number of variables for problems handled by this <code>SATSolver</code>.
     * If <code>maxLiteral &lt;= 0</code>, the number of variables is computed
     * each time {@link #solve(BooleanFormula)} is invoked.
     */
    protected int maxLiteral;

    protected SATSolver() {
        maxLiteral = 0;
    }

    /**
     * Sets the number of variables for problems handled by this <code>SATSolver</code>.
     * If <code>maxLiteral &lt;= 0</code>, the number of variables is computed
     * each time {@link #solve(BooleanFormula)} is invoked.
     */
    public void setMaxLiteral(int maxLiteral) {
        this.maxLiteral = maxLiteral;
    }

    protected abstract Stream<DIMACSLiterals> solve(BooleanFormula problem, int maxLiteral);

    /**
     * Solves a boolean satisfiability problem expressed in DIMACS CNF format.
     * If the <code>maxLiteral</code> parameter has been set using {@link #setMaxLiteral(int)}
     * and <code>maxLiteral &gt; 0</code>, the solver will use <code>maxLiteral</code> as
     * the total number of variables in the boolean problem, otherwise the actual number
     * of variables in <code>problem</code> will be used.
     * @param problem
     * @return a stream of satisfiable models of the problem.
     * A model is a set of literals ranging from 1 to <code>maxLiteral</code>.
     */
    public Stream<DIMACSLiterals> solve(BooleanFormula problem) {
        if (maxLiteral > 0) {
            return solve(problem, maxLiteral);
        }
        int tempMaxLiteral = problem.clauses.stream()
                .flatMapToInt(DIMACSLiterals::stream)
                .max().orElse(0);
        if (tempMaxLiteral > 0) {
            return solve(problem, tempMaxLiteral);
        }
        return Stream.empty();
    }
}
