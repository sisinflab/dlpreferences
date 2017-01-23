package it.poliba.enasca.ontocpnets;

import com.google.common.collect.Sets;

import java.util.Set;

/**
 * Contains static helper methods.
 */
public class Utils {
    /**
     * This method assumes that <code>expected.equals(actual) == false</code>.
     * @param actual
     * @param expected
     * @param <T>
     * @return
     */
    public static <T> String reportSetDifference(
            Set<? extends T> actual, Set<? extends T> expected) {
        StringBuilder message = new StringBuilder();
        if (actual.containsAll(expected)) {
            return message.append("The expected set is a subset of the actual set. Difference: ")
                    .append(Sets.difference(actual, expected))
                    .toString();
        }
        if (expected.containsAll(actual)) {
            return message.append("The actual set is a subset of the expected set. Difference: ")
                    .append(Sets.difference(expected, actual))
                    .toString();
        }
        if (expected.size() == actual.size()) {
            message.append("The expected and actual size match.");
        }
        else {
            message.append("Expected size ").append(expected.size())
                    .append(", but got ").append(actual.size())
                    .append('.');
        }
        return message.append("\nDifference between expected and actual: ")
                .append(Sets.difference(expected, actual))
                .append("\nDifference between actual and expected: ")
                .append(Sets.difference(actual, expected))
                .toString();
    }
}
