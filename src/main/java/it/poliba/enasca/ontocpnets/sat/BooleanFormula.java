package it.poliba.enasca.ontocpnets.sat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A boolean formula in conjunctive normal form.
 */
public class BooleanFormula {
    Set<DIMACSLiterals> clauses;

    protected BooleanFormula(Set<DIMACSLiterals> clauses) {
        this.clauses = clauses;
    }

    public BooleanFormula() {
        this(new HashSet<>());
    }

    /**
     * Adds the specified clause.
     * @param clause
     */
    public void addClause(IntStream clause) {
        clauses.add(new DIMACSLiterals(clause));
    }

    /**
     * Adds the negation of the specified clause, that is,
     * the conjunction of its negated literals (De Morgan's law).
     * @param clause
     */
    public void addNegatedClause(IntStream clause) {
        Objects.requireNonNull(clause).forEachOrdered(
                literal -> addLiteral(-literal));
    }

    /**
     * Adds a trivial clause consisting of the specified literal.
     * @param literal
     */
    public void addLiteral(int literal) {
        clauses.add(DIMACSLiterals.of(literal));
    }

    /**
     * Returns the number of clauses in this formula.
     * @return
     */
    public int size() {
        return clauses.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BooleanFormula other = (BooleanFormula) o;
        return clauses.equals(other.clauses);
    }

    @Override
    public int hashCode() {
        return clauses.hashCode();
    }

    @Override
    public String toString() {
        return clauses.stream()
                .map(clause -> Arrays.stream(clause.literals)
                        .mapToObj(String::valueOf)
                        .collect(Collectors.joining(" ∨ ", "(", ")")))
                .collect(Collectors.joining(" ∧ "));
    }

    public static BooleanFormula copyOf(BooleanFormula orig) {
        return orig.clauses.stream().collect(toFormula());
    }

    public static Collector<DIMACSLiterals, ?, BooleanFormula> toFormula() {
        return Collectors.collectingAndThen(
                Collectors.toCollection(HashSet::new),
                BooleanFormula::new);
    }
}
