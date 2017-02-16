package it.poliba.enasca.ontocpnets.tree;

import it.poliba.enasca.ontocpnets.TestUtils;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;

/**
 * Test class for {@link IntPreferenceForest}.
 */
public class IntPreferenceForestTest {
    /**
     * Tests the {@link IntPreferenceForest#expand()} method.
     * @param maxLiterals
     * @param levels
     * @throws Exception
     */
    @Test(dataProvider = "expandProvider")
    public void testExpand(int maxLiterals, int[][][] levels) throws Exception {
        testExpandInternal(maxLiterals, levels,
                (forest, currentLevel) -> forest.expand());
    }

    @DataProvider(name = "expandProvider")
    public static Object[][] expandProvider() {
        return new Object[][]{
                {3, new int[][][]{
                        // first level
                        {{1}, {-1}, {2}, {-2}, {3}, {-3}},
                        // second level
                        {{2, 1}, {-2, 1}, {3, 1}, {-3, 1}, {2, -1}, {-2, -1}, {3, -1}, {-3, -1}, {3, 2}, {-3, 2}, {3, -2}, {-3, -2}},
                        // third level
                        {{3, 2, 1}, {-3, 2, 1}, {3, -2, 1}, {-3, -2, 1}, {3, 2, -1}, {-3, 2, -1}, {3, -2, -1}, {-3, -2, -1}}
                }}
        };
    }

    @Test(dataProvider = "filterExpandProvider")
    public void testFilterExpand(int maxLiterals, int[][][] levels,
                                 Predicate<IntStream> branchFilter) throws Exception {
        testExpandInternal(maxLiterals, levels,
                (forest, currentLevel) -> forest.expand(branchFilter));
    }

    @DataProvider(name = "filterExpandProvider")
    public static Object[][] filterExpandProvider() {
        return new Object[][]{
                {3, new int[][][]{
                        // first level
                        {{1}, {-1}, {2}, {-2}, {3}, {-3}},
                        // second level
                        {{2, -1}, {-2, -1}, {3, -1}, {-3, -1}, {3, -2}, {-3, -2}},
                        // third level
                        {{3, -2, -1}, {-3, -2, -1}}
                }, (Predicate<IntStream>) branch -> branch.sum() < 0 }
        };
    }

    @Test(dataProvider = "maskExpandProvider")
    public void testMaskExpand(int maxLiterals, int[][][] levels, boolean[][] masks)
            throws Exception {
        testExpandInternal(maxLiterals, levels,
                (forest, currentLevel) -> forest.expand(masks[currentLevel]));
    }

    @DataProvider(name = "maskExpandProvider")
    public static Object[][] maskExpandProvider() {
        return new Object[][]{
                {3, new int[][][]{
                        // first level
                        {{1}, {-1}, {2}, {-2}, {3}, {-3}},
                        // second level
                        {{2, 1}, {-2, 1}, {3, 1}, {-3, 1}, {3, -2}, {-3, -2}},
                        // third level
                        {{3, 2, 1}, {-3, 2, 1}}
                }, new boolean[][]{
                        {true, false, false, true, true, true},
                        {true, false, false, true, true, true},
                        {true, false}}
                }
        };
    }

    /**
     * A generic test for the expansion process of {@link IntPreferenceForest}.
     * @param maxLiterals the number of propositional variables.
     *                    The {@link IntPreferenceForest} instance will be constructed from the
     *                    DIMACS literals <code>1, 2, &hellip;, maxLiterals</code>.
     * @param levels the contents of the forest at each level.
     *               <code>levels[i]</code> represents the collection of branches at level <code>i</code>;
     *               <code>levels[i][j]</code> the <code>j-th</code> branch at level <code>i</code>.
     * @param expandImpl the actual expansion function, which should contain an invocation
 *                   to {@link IntPreferenceForest#expand()} or its overloaded versions.
     *                   Accepts an {@link IntPreferenceForest} instance and the 0-based index
     *                   of the current level.
     */
    private void testExpandInternal(int maxLiterals, int[][][] levels,
                                    BiConsumer<IntPreferenceForest, Integer> expandImpl)
            throws Exception {
        IntPreferenceForest forest = new IntPreferenceForest(maxLiterals);
        int lv;
        // Iterate over levels.
        for (lv = 0; !forest.isEmpty(); lv++) {
            List<IntStream> actualBranches = forest.branches();
            int[][] expectedBranches = levels[lv];
            expandImpl.accept(forest, lv);
            // Iterate over the branches at the current level.
            IntStream.range(0, expectedBranches.length)
                    .forEach(i -> TestUtils.assertEqualsUnordered(
                            actualBranches.get(i).toArray(),
                            expectedBranches[i]));
        }
        Assert.assertEquals(lv, levels.length);
    }

}