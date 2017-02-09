package it.poliba.enasca.ontocpnets.util;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Tests for implementers of {@link BasePreferenceForest}.
 */
public class PreferenceForestTest {
    /**
     * Tests the {@link PreferenceForest#expand()} method.
     * @param dimacsLiterals a stream of DIMACS literals, which will serve as the
     *                       input set for the preference forest
     * @param levels the contents of the branches at each level
     */
    @Test(dataProvider = "expandProvider")
    public void testExpandGeneric(int[] dimacsLiterals, int[][][] levels)
            throws Exception {
        PreferenceForest<Integer> forest = Arrays.stream(dimacsLiterals).boxed()
                .collect(PreferenceForest.toForest(literal -> -literal));
        int level;
        // Iterate over levels.
        for (level = 0; !forest.isEmpty(); level++) {
            int[][] expectedBranches = levels[level];
            List<Stream<Integer>> actualBranches = forest.branches();
            forest.expand();
            // Iterate over the branches at the current level.
            IntStream.range(0, expectedBranches.length).forEach(i -> {
                Set<Integer> expectedBranch = Arrays.stream(expectedBranches[i]).boxed()
                        .collect(Collectors.toSet());
                Set<Integer> actualBranch = actualBranches.get(i)
                        .collect(Collectors.toSet());
                Assert.assertEquals(actualBranch, expectedBranch);
            });
        }
        Assert.assertEquals(level, levels.length);
    }

    /**
     * Tests the {@link IntPreferenceForest#expand()} method.
     * @param dimacsLiterals a stream of DIMACS literals, which will serve as the
     *                       input set for the preference forest
     * @param levels the contents of the branches at each level
     */
    @Test(dataProvider = "expandProvider")
    public void testExpandInt(int[] dimacsLiterals, int[][][] levels)
            throws Exception {
        IntPreferenceForest forest = new IntPreferenceForest(dimacsLiterals.length);
        int level;
        // Iterate over levels.
        for (level = 0; !forest.isEmpty(); level++) {
            int[][] expectedBranches = levels[level];
            List<IntStream> actualBranches = forest.branches();
            forest.expand();
            // Iterate over the branches at the current level.
            IntStream.range(0, expectedBranches.length).forEach(i -> {
                Set<Integer> expectedBranch = Arrays.stream(expectedBranches[i]).boxed()
                        .collect(Collectors.toSet());
                Set<Integer> actualBranch = actualBranches.get(i).boxed()
                        .collect(Collectors.toSet());
                Assert.assertEquals(actualBranch, expectedBranch);
            });
        }
        Assert.assertEquals(level, levels.length);
    }

    @DataProvider
    public Object[][] expandProvider() {
        return new Object[][]{
                {new int[]{1, 2, 3}, new int[][][]{
                        // first level
                        {{1}, {-1}, {2}, {-2}, {3}, {-3}},
                        // second level
                        {{2, 1}, {-2, 1}, {3, 1}, {-3, 1}, {2, -1}, {-2, -1}, {3, -1}, {-3, -1}, {3, 2}, {-3, 2}, {3, -2}, {-3, -2}},
                        // third level
                        {{3, 2, 1}, {-3, 2, 1}, {3, -2, 1}, {-3, -2, 1}, {3, 2, -1}, {-3, 2, -1}, {3, -2, -1}, {-3, -2, -1}}
                }}
        };
    }

}