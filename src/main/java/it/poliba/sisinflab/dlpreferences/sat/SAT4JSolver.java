package it.poliba.sisinflab.dlpreferences.sat;

import it.poliba.sisinflab.dlpreferences.except.SATRuntimeException;
import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.pb.IPBSolver;
import org.sat4j.pb.ObjectiveFunction;
import org.sat4j.specs.*;
import org.sat4j.tools.ModelIterator;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * A boolean SAT solver based on the SAT4J library.
 */
public class SAT4JSolver {
    /**
     * A value that, if passed to {@link #setMaxLiteral(int)}, signals that the number of variables
     * should be computed automatically for each problem handled by this solver.
     */
    public static int MAXLITERAL_AUTO = 0;

    private static int MAXRESULTS_UNLIMITED = -1;

    /**
     * If <code>!= {@link #MAXLITERAL_AUTO}</code>, this value overrides the number of variables
     * for problems handled by this solver.
     */
    private int maxLiteral;

    /**
     * Constructs a <code>SAT4JSolver</code> that automatically computes the number of variables
     * for each problem. This constructor is equivalent to invoking {@link #SAT4JSolver(int)}
     * with {@link #MAXLITERAL_AUTO} as argument.
     */
    public SAT4JSolver() {
        this(MAXLITERAL_AUTO);
    }

    /**
     * Constructs a <code>SAT4JSolver</code> that defines <code>maxLiteral</code> variables
     * when solving each problem. If the actual number of variables in a problem
     * is less than <code>maxLiteral</code>, the remaining variables are generated automatically.
     *
     * @param maxLiteral a positive integer indicating a fixed number of variables for problems
     *                   handled by this <code>SAT4JSolver</code>, or {@link #MAXLITERAL_AUTO}
     *                   if the number of variables should be computed automatically for each problem.
     * @throws IllegalArgumentException if <code>maxLiteral</code> is nor a positive value
     * neither equal to {@link #MAXLITERAL_AUTO}.
     */
    public SAT4JSolver(int maxLiteral) {
        setMaxLiteral(maxLiteral);
    }

    /**
     * Sets the number of variables for problems handled by this solver.
     *
     * @param maxLiteral if positive, each problem handled by this solver will be treated as having
     *                   a number of variables equal to this value.
     *                   If {@link #MAXLITERAL_AUTO}, the number of variables
     *                   is computed automatically for each problem.
     * @throws IllegalArgumentException if <code>maxLiteral</code> is nor a positive value
     * neither equal to {@link #MAXLITERAL_AUTO}.
     */
    public void setMaxLiteral(int maxLiteral) {
        if ((maxLiteral < 1) && (maxLiteral != MAXLITERAL_AUTO)) {
            throw new IllegalArgumentException();
        }
        this.maxLiteral = maxLiteral;
    }

    /**
     * Returns <code>true</code> if the input problem has at least one model.
     *
     * @param problem
     * @return
     */
    public boolean isSatisfiable(BooleanFormula problem) {
        Objects.requireNonNull(problem);
        Stream<DimacsLiterals> modelStream = models(buildSATSolver(problem), 0);
        return modelStream != null;
    }

    /**
     * Checks whether <code>formula</code> implies <code>clause</code>.
     * @param formula
     * @param clause
     * @return
     */
    public boolean implies(BooleanFormula formula, DimacsLiterals clause) {
        Objects.requireNonNull(formula);
        Objects.requireNonNull(clause);
        BooleanFormula testFormula = formula.clauses().collect(BooleanFormula.toFormula());
        testFormula.addNegatedClause(clause);
        return !isSatisfiable(testFormula);
    }

    /**
     * Solves a boolean satisfiability problem expressed in DIMACS CNF format.
     * If the <code>maxLiteral</code> parameter has been set using {@link #setMaxLiteral(int)}
     * the solver will use <code>maxLiteral</code> as the total number of variables in the boolean problem,
     * otherwise the actual number of variables in <code>problem</code> will be used.
     *
     * @param problem
     * @return the satisfiable models for the input problem, or an empty <code>Stream</code>
     * if the problem is unsatisfiable
     */
    public Stream<DimacsLiterals> solveSAT(BooleanFormula problem) {
        Objects.requireNonNull(problem);
        ModelIterator solver = buildSATSolver(problem);
        Stream<DimacsLiterals> modelStream = models(solver, MAXRESULTS_UNLIMITED);
        if (modelStream == null) {
            return Stream.empty();
        }
        return modelStream;
    }

    /**
     * Solves a pseudo-boolean problem.
     * The <code>constraints</code> parameter is expressed as a set of DIMACS boolean clauses
     * that are converted internally into linear constraints of the form:
     *
     * <pre>
     *    sum(Pi for each positive variable Pi in the clause)
     * +  sum((1-Qi) for each negated variable Qi in the clause)
     * >= 1
     * </pre>
     *
     * The <code>objective</code> parameter is a a stream of integer coefficients <code>c1, c2, &hellip;</code>
     * representing the objective function:
     *
     * <pre>min c1*X1 + c2*X2 + &hellip;</pre>
     *
     * for each boolean variable <code>Xi</code>.
     *
     * @param constraints
     * @param objective
     * @return a model that minimizes the objective function, or an empty <code>Optional</code>
     * if the constraints are unsatisfiable
     */
    public Optional<DimacsLiterals> solvePseudoBoolean(BooleanFormula constraints, IntStream objective) {
        Objects.requireNonNull(constraints);
        Objects.requireNonNull(objective);
        ModelIterator solver = buildPBSolver(constraints, objective);
        Stream<DimacsLiterals> modelStream = models(solver, 1);
        if (modelStream == null) {
            return Optional.empty();
        }
        return modelStream.findFirst();
    }

    /**
     * If {@link #maxLiteral} is set to {@link #MAXLITERAL_AUTO}, returns the number of variables
     * in the input problem, otherwise simply returns {@link #maxLiteral}.
     *
     * @param problem
     * @return
     */
    private int size(BooleanFormula problem) {
        if (maxLiteral == MAXLITERAL_AUTO) {
            return problem.max();
        }
        return maxLiteral;
    }

    /**
     * Builds a <code>ModelIterator</code> over the models of the input SAT problem.
     *
     * @param problem
     * @return a <code>ModelIterator</code> instance to iterate over the models of the input SAT problem,
     * or <code>null</code> if the problem is trivially unsatisfiable.
     */
    private ModelIterator buildSATSolver(BooleanFormula problem) {
        ISolver solver = SolverFactory.newLight();
        solver.newVar(size(problem));
        // Wrap the boolean clauses in an IVec.
        IVec<IVecInt> problemAsIVec = problem.clauses()
                .<IVecInt>map(dimacs -> new VecInt(dimacs.literals))
                .collect(toIVec());
        solver.setExpectedNumberOfClauses(problemAsIVec.size());
        // Add the clauses to the solver instance.
        try {
            solver.addAllClauses(problemAsIVec);
        } catch (ContradictionException e) {
            return null;
        }
        // Wrap the solver instance in a ModelIterator.
        return new ModelIterator(solver);
    }

    /**
     * Builds a <code>ModelIterator</code> over the models of the input pseudo-boolean problem.
     * See the javadoc of {@link #solvePseudoBoolean(BooleanFormula, IntStream)} for details
     * about the parameters.
     *
     * @param constraints
     * @param objective
     * @return a <code>ModelIterator</code> instance to iterate over the models of the
     * input pseudo-boolean problem, or <code>null</code> if the problem is trivially unsatisfiable.
     */
    private ModelIterator buildPBSolver(BooleanFormula constraints, IntStream objective) {
        int problemSize = size(constraints);
        IPBSolver solver = org.sat4j.pb.SolverFactory.newLight();
        solver.newVar(problemSize);
        // Convert boolean clauses into linear constraints.
        for (Iterator<DimacsLiterals> clauses = constraints.clauses().iterator(); clauses.hasNext(); ) {
            DimacsLiterals dimacs = clauses.next();
            int[] literalsAbsolute = dimacs.stream()
                    .map(value -> value < 0 ? -value : value)
                    .toArray();
            int[] coefficients = dimacs.stream()
                    .map(value -> value < 0 ? -1 : 1)
                    .toArray();
            int numberOfNegated = dimacs.stream()
                    .filter(value -> value < 0)
                    .map(value -> 1)
                    .sum();
            try {
                solver.addAtLeast(
                        new VecInt(literalsAbsolute),
                        new VecInt(coefficients),
                        1 - numberOfNegated);
            } catch (ContradictionException e) {
                return null;
            }
        }
        // Build the objective function.
        int[] problemVars = IntStream.rangeClosed(1, problemSize).toArray();
        IVec<BigInteger> coefficients = objective
                .mapToObj(BigInteger::valueOf)
                .collect(toIVec());
        solver.setObjectiveFunction(
                new ObjectiveFunction(new VecInt(problemVars), coefficients));
        // Wrap the solver instance in a ModelIterator.
        return new ModelIterator(solver);
    }

    /**
     * Finds up to <code>maxResults</code> models using the specified <code>ModelIterator</code>.
     *
     * @param solver a <code>ModelIterator</code> instance to iterate over the models
     *               of the underlying problem. If <code>null</code>, this method simply
     *               returns <code>null</code>.
     * @param maxResults the maximum number of models to return. A value of {@link #MAXRESULTS_UNLIMITED}
     *                   indicates that all models should be returned.
     * @return up to <code>maxResults</code> models of the underlying problem,
     * or <code>null</code> if the problem is unsatisfiable
     */
    private Stream<DimacsLiterals> models(ModelIterator solver, int maxResults) {
        if (solver == null) {
            return null;
        }
        Stream.Builder<DimacsLiterals> builder = Stream.builder();
        try {
            if (!solver.isSatisfiable()) {
                return null;
            }
            if (maxResults > 0) {
                builder.accept(new DimacsLiterals(solver.model()));
            }
            while ((maxResults == MAXRESULTS_UNLIMITED || solver.numberOfModelsFoundSoFar() < maxResults)
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
     * Return a <code>Collector</code> that accumulates elements of type {@link T}
     * into an <code>IVec&lt;T&gt;</code>.
     *
     * @param <T>
     * @return
     */
    private static <T> Collector<T, ?, IVec<T>> toIVec() {
        return Collector.of(
                Vec::new,
                IVec::push,
                (left, right) -> {right.moveTo(left); return left;}
        );
    }

}
