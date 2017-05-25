package it.poliba.sisinflab.dlpreferences;

import java.util.Map;
import java.util.Objects;

/**
 * A function that converts propositional variable names (represented as <code>String</code>s)
 * into integer DIMACS literals.
 * For the inverse function, see {@link VarNameProvider}.
 */
@FunctionalInterface
public interface DimacsProvider {
    /**
     * Converts the specified propositional variable name into a positive integer.
     * @param varName
     * @return
     */
    int getPositiveLiteral(String varName);

    /**
     * @param varName
     * @param isPositive
     * @return a non-zero DIMACS literal
     * @throws IllegalArgumentException if passing <code>varName</code>
     * to {@link #getPositiveLiteral(String)} produces a zero value
     * @throws NullPointerException if <code>varName</code> is <code>null</code>
     */
    default int getLiteral(String varName, boolean isPositive) {
        int literal = getPositiveLiteral(Objects.requireNonNull(varName));
        if (literal == 0) throw new IllegalArgumentException();
        return isPositive ? literal : -literal;
    }

    /**
     * @param varEntry
     * @return a non-zero DIMACS literal
     * @throws IllegalArgumentException if passing <code>varEntry.getKey()</code>
     * to {@link #getPositiveLiteral(String)} produces a zero value
     * @throws NullPointerException if <code>varEntry</code> is <code>null</code>
     * or contains a <code>null</code>
     */
    default int getLiteral(Map.Entry<String, Boolean> varEntry) {
        Objects.requireNonNull(varEntry);
        return getLiteral(varEntry.getKey(), varEntry.getValue());
    }

}
