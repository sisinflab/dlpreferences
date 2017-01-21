package it.poliba.enasca.ontocpnets.sat;

import com.google.common.primitives.Ints;
import it.poliba.enasca.ontocpnets.except.SATInvalidException;
import it.poliba.enasca.ontocpnets.except.SATRuntimeException;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.TimeoutException;
import org.sat4j.tools.ModelIterator;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A boolean SAT solver that uses the SAT4J library.
 */
public class SAT4JSolver implements SATSolver {
    /**
     * @param clauses
     * @param maxLiteral the number of variables in the problem.
     * @return
     * @throws SATInvalidException
     * @throws SATRuntimeException if the solver behaves unexpectedly while computing the satisfiable models
     */
    @Override
    public Stream<Set<Integer>> solveDimacsCNF(Stream<Set<Integer>> clauses, int maxLiteral)
            throws SATInvalidException {
        List<Set<Integer>> clauseList = clauses.collect(Collectors.toList());
        // Configure the SAT4J solver.
        ModelIterator solver = new ModelIterator(SolverFactory.newDefault());
        solver.newVar(maxLiteral);
        solver.setExpectedNumberOfClauses(clauseList.size());
        // Add clauses.
        for (Set<Integer> clause : clauseList) {
            IVecInt v = new VecInt(Ints.toArray(clause));
            try {
                solver.addClause(v);
            } catch (ContradictionException e) {
                throw new SATInvalidException(e);
            }
        }
        // Retrieve the satisfiable models.
        Stream.Builder<Set<Integer>> modelBuilder = Stream.builder();
        try {
            while (solver.isSatisfiable()) {
                int[] model = solver.model();
                modelBuilder.accept(Arrays.stream(model).boxed().collect(Collectors.toSet()));
            }
        } catch (TimeoutException e) {
            throw new SATRuntimeException(e);
        }
        return modelBuilder.build();
    }
}
