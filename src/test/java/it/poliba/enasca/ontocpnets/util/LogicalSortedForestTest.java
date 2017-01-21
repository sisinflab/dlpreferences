package it.poliba.enasca.ontocpnets.util;

import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Tests for {@link LogicalSortedForest<Integer>}.
 */
public class LogicalSortedForestTest {
    /**
     * Tests whether the forest expands correctly, level by level.
     * @param dimacsLiterals a stream of DIMACS literals, which will serve as keys
     *                       for the <em>ordering map</em> of the forest.
     *                       The values of the ordering map are obtained by flipping the sign of each literal.
     * @param levels the contents of the fringe of the forest at each level
     */
    @Test(dataProvider = "expandProvider")
    public void testExpand(int[] dimacsLiterals, int[][] levels)
            throws Exception {
        LogicalSortedForest<Integer> forest = Arrays.stream(dimacsLiterals).boxed()
                .collect(LogicalSortedForest.toLogicalSortedForest(literal -> -literal));
        LogicalSortedForest<Integer> forestCopy = Arrays.stream(dimacsLiterals).boxed()
                .collect(LogicalSortedForest.toLogicalSortedForest(literal -> -literal));
        int i;
        for (i = 0; !forest.isEmpty() && !forestCopy.isEmpty(); i++) {
            AssertJUnit.assertArrayEquals(levels[i], forest.fringe().mapToInt(Integer::intValue).toArray());
            AssertJUnit.assertArrayEquals(levels[i], forestCopy.fringe().mapToInt(Integer::intValue).toArray());
            forest.expand();
            forestCopy.expandOrdered(branch -> false);
        }
        // Verify that the array has been fully processed.
        Assert.assertTrue(levels.length == i);
    }

    @DataProvider
    public Object[][] expandProvider() {
        return new Object[][]{
                {IntStream.rangeClosed(1, 3).toArray(), new int[][]{
                        {1,                      -1,                       2,      -2,       3,  -3},
                        {2,     -2,      3,  -3,  2,     -2,      3,  -3,  3,  -3,  3,  -3},
                        {3, -3,  3, -3,           3, -3,  3, -3}
                }}
        };
    }

}