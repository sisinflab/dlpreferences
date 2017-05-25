package it.poliba.sisinflab.dlpreferences.sat;

import it.poliba.sisinflab.dlpreferences.VarNameProvider;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A collection of DIMACS literals.
 */
public class DimacsLiterals {
    int[] literals;

    DimacsLiterals(int[] literals) {
        this.literals = literals;
    }

    /**
     * Constructs a <code>DimacsLiterals</code> object containing the literals in the specified stream.
     * Duplicate values and zeroes are discarded.
     * @param literals
     */
    public DimacsLiterals(IntStream literals) {
        this(Objects.requireNonNull(literals)
                .filter(literal -> literal != 0)
                .distinct()
                .toArray());
    }

    /**
     * Returns a <code>DimacsLiterals</code> object containing the specified literal.
     * If the argument is 0, an empty <code>DimacsLiterals</code> is returned.
     * @param literal
     */
    public static DimacsLiterals of(int literal) {
        return literal != 0 ?
                new DimacsLiterals(new int[]{literal}) :
                new DimacsLiterals(new int[]{});
    }

    public IntStream stream() {
        return Arrays.stream(literals);
    }

    public Map<String, Boolean> asMap(VarNameProvider converter) {
        return stream().boxed()
                .collect(Collectors.toMap(
                        converter::fromLiteral,
                        literal -> literal > 0));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DimacsLiterals other = (DimacsLiterals) o;
        return Arrays.equals(literals, other.literals);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(literals);
    }
}
