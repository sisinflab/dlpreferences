package it.poliba.enasca.ontocpnets.sat;

/**
 * A function that converts integer DIMACS literals into <code>String</code>s
 * representing propositional variable names.
 */
@FunctionalInterface
public interface VarNameProvider {
    /**
     * Inverse of {@link DimacsProvider#getPositiveLiteral(String)}.
     * @param literal a positive DIMACS literal
     * @return
     */
    String fromPositiveLiteral(int literal);

    /**
     * Inverse of {@link DimacsProvider#getLiteral(java.util.Map.Entry)}.
     * @param literal a non-zero DIMACS literal
     * @return
     * @throws IllegalArgumentException if <code>literal == 0</code>
     */
    default String fromLiteral(int literal) {
        if (literal == 0) throw new IllegalArgumentException();
        return literal > 0 ?
                fromPositiveLiteral(literal) :
                fromPositiveLiteral(-literal);
    }

}
