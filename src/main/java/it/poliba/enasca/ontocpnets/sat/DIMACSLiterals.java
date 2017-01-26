package it.poliba.enasca.ontocpnets.sat;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * A collection of DIMACS literals.
 */
public class DIMACSLiterals {
    int[] literals;

    DIMACSLiterals(int[] literals) {
        this.literals = literals;
    }

    /**
     * Constructs a <code>DIMACSLiterals</code> object containing the literals in the specified stream.
     * Duplicate values and zeroes are discarded.
     * @param literals
     */
    public DIMACSLiterals(IntStream literals) {
        this(Objects.requireNonNull(literals)
                .filter(literal -> literal != 0)
                .distinct()
                .toArray());
    }

    /**
     * Returns a <code>DIMACSLiterals</code> object containing the specified literal.
     * If the argument is 0, an empty <code>DIMACSLiterals</code> is returned.
     * @param literal
     */
    public static DIMACSLiterals of(int literal) {
        return literal != 0 ?
                new DIMACSLiterals(new int[]{literal}) :
                new DIMACSLiterals(new int[]{});
    }

    public IntStream stream() {
        return Arrays.stream(literals);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DIMACSLiterals other = (DIMACSLiterals) o;
        return Arrays.equals(literals, other.literals);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(literals);
    }
}
