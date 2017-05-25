package it.poliba.sisinflab.dlpreferences.sat;

import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A boolean formula in conjunctive normal form.
 */
public class BooleanFormula {
    /**
     * The formula in conjunctive normal form.
     * Each element represents a clause.
     */
    private Set<DimacsLiterals> clausesCNF;

    private BooleanFormula(Set<DimacsLiterals> clauses) {
        clausesCNF = clauses;
    }

    /**
     * Returns the clauses that make up this formula.
     * @return
     */
    public Stream<DimacsLiterals> clauses() {
        return clausesCNF.stream();
    }

    /**
     * Adds the specified clause.
     * @param clause
     */
    public void addClause(DimacsLiterals clause) {
        clausesCNF.add(Objects.requireNonNull(clause));
    }

    /**
     * Adds the negation of the specified clause, that is,
     * the conjunction of its negated literals (De Morgan's law).
     * @param clause
     */
    public void addNegatedClause(DimacsLiterals clause) {
        Objects.requireNonNull(clause);
        for (int l : clause.literals) addLiteral(-l);
    }

    /**
     * Adds a trivial clause consisting of the specified literal.
     * @param literal
     */
    public void addLiteral(int literal) {
        clausesCNF.add(DimacsLiterals.of(literal));
    }

    /**
     * Returns the number of clauses in this formula.
     * @return
     */
    public int size() {
        return clausesCNF.size();
    }

    /**
     * Returns a new empty formula.
     * @return
     */
    public static BooleanFormula empty() {
        return new BooleanFormula(new HashSet<>());
    }

    /**
     * Returns a new empty formula, backed by a synchronized set.
     * @return
     */
    public static BooleanFormula emptySynchronized() {
        return new BooleanFormula(Collections.synchronizedSet(new HashSet<>()));
    }

    /**
     * Returns a <code>Collector</code> that accumulates input clauses into a new formula.
     * @return
     * @see SAT4JSolver#solve(BooleanFormula)
     */
    public static Collector<DimacsLiterals, ?, BooleanFormula> toFormula() {
        return Collectors.collectingAndThen(
                Collectors.toCollection(HashSet::new),
                BooleanFormula::new);
    }

    /**
     * Returns a <code>Collector</code> that accumulates input clauses into a new formula,
     * backed by a synchronized set.
     * @return
     * @see SAT4JSolver#solve(BooleanFormula)
     */
    public static Collector<DimacsLiterals, ?, BooleanFormula> toSynchronizedFormula() {
        return Collectors.collectingAndThen(
                Collectors.toCollection(() -> Collections.synchronizedSet(new HashSet<>())),
                BooleanFormula::new);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BooleanFormula other = (BooleanFormula) o;
        return clausesCNF.equals(other.clausesCNF);
    }

    @Override
    public int hashCode() {
        return clausesCNF.hashCode();
    }

    @Override
    public String toString() {
        return clausesCNF.stream()
                .map(clause -> Arrays.stream(clause.literals)
                        .mapToObj(String::valueOf)
                        .collect(Collectors.joining(" OR ", "(", ")")))
                .collect(Collectors.joining(" AND "));
    }
}
