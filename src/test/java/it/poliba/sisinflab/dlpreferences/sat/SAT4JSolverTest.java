package it.poliba.sisinflab.dlpreferences.sat;

import it.poliba.sisinflab.dlpreferences.TestUtils;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class SAT4JSolverTest {
    private SAT4JSolver solver;

    public SAT4JSolverTest() {
        solver = new SAT4JSolver();
    }

    @Test(dataProvider = "satProvider")
    public void testSolveSAT(BooleanFormula problem,
                             Set<DimacsLiterals> solution) throws Exception {
        Set<DimacsLiterals> result =
                solver.solveSAT(problem)
                        .collect(Collectors.toSet());
        Assert.assertEquals(result, solution,
                TestUtils.reportSetDifference(result, solution));
    }

    @DataProvider
    public Object[][] satProvider() {
        // SAT problem:
        // p1 AND (p2 OR p3) AND ((NOT p2) OR (NOT p3))
        BooleanFormula problem =
                Stream.of(
                        DimacsLiterals.of(1),
                        DimacsLiterals.of(2, 3),
                        DimacsLiterals.of(-2, -3)
                ).collect(BooleanFormula.toFormula());
        // Solutions:problem
        // p1 AND (NOT p2) AND p3
        // p1 AND p2 AND (NOT p3)
        Set<DimacsLiterals> solution =
                Stream.of(
                        DimacsLiterals.of(1, -2, 3),
                        DimacsLiterals.of(1, 2, -3)
                ).collect(Collectors.toSet());
        return new Object[][]{
                {problem, solution}
        };
    }

    @Test(dataProvider = "pseudoBooleanProvider")
    public void testSolvePseudoBoolean(BooleanFormula constraints,
                                       IntStream objective,
                                       DimacsLiterals solution) throws Exception {
        DimacsLiterals result = solver.solvePseudoBoolean(constraints, objective).get();
        Assert.assertEquals(result, solution);
    }

    @DataProvider
    public Object[][] pseudoBooleanProvider() {
        // Problem 1
        // SAT constraints: p1 AND (p2 OR p3) AND ((NOT p2) OR (NOT p3))
        // Objective: min: 5*p1 + 6*p2 + 4*p3
        // Solution: p1 AND (NOT p2) AND p3
        BooleanFormula constraints =
                Stream.of(
                        DimacsLiterals.of(1),
                        DimacsLiterals.of(2, 3),
                        DimacsLiterals.of(-2, -3)
                ).collect(BooleanFormula.toFormula());
        IntStream objective1 = IntStream.of(5, 6, 4);
        DimacsLiterals solution1 = DimacsLiterals.of(1, -2, 3);
        // Problem 2
        // SAT constraints: p1 AND (p2 OR p3) AND ((NOT p2) OR (NOT p3))
        // Objective: min: 5*p1 + 4*p2 + 6*p3
        // Solution: p1 AND p2 AND (NOT p3)
        IntStream objective2 = IntStream.of(5, 4, 6);
        DimacsLiterals solution2 = DimacsLiterals.of(1, 2, -3);
        return new Object[][]{
                {constraints, objective1, solution1},
                {constraints, objective2, solution2}
        };
    }

}