package it.poliba.enasca.ontocpnets.sat;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A skeletal implementation of {@link BooleanFormula}.
 */
public abstract class AbstractBooleanFormula implements BooleanFormula {
    /**
     * The formula in conjunctive normal form.
     * Each element represents a clause.
     */
    protected Set<DimacsLiterals> clausesCNF;

    AbstractBooleanFormula(Set<DimacsLiterals> clausesCNF) {
        this.clausesCNF = clausesCNF;
    }

    @Override
    public void addClause(DimacsLiterals clause) {
        clausesCNF.add(Objects.requireNonNull(clause));
    }

    @Override
    public void addNegatedClause(DimacsLiterals clause) {
        Objects.requireNonNull(clause);
        for (int l : clause.literals) {
            addLiteral(-l);
        }
    }

    @Override
    public void addLiteral(int literal) {
        clausesCNF.add(DimacsLiterals.of(literal));
    }

    @Override
    public Stream<DimacsLiterals> clauses() {
        return clausesCNF.stream();
    }

    @Override
    public int size() {
        return clausesCNF.size();
    }

    @Override
    public abstract BooleanFormula copy();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractBooleanFormula other = (AbstractBooleanFormula) o;
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
