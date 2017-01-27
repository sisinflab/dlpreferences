package it.poliba.enasca.ontocpnets.sat;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * A skeletal implementation of {@link BooleanFormula}.
 */
abstract class AbstractBooleanFormula implements BooleanFormula {
    protected Set<DIMACSLiterals> clausesCNF;

    AbstractBooleanFormula(Set<DIMACSLiterals> clausesCNF) {
        this.clausesCNF = clausesCNF;
    }

    @Override
    public void addClause(IntStream clause) {
        clausesCNF.add(new DIMACSLiterals(clause));
    }

    @Override
    public void addNegatedClause(IntStream clause) {
        Objects.requireNonNull(clause).forEachOrdered(literal -> addLiteral(-literal));
    }

    @Override
    public void addLiteral(int literal) {
        clausesCNF.add(DIMACSLiterals.of(literal));
    }

    @Override
    public Stream<DIMACSLiterals> clauses() {
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
                        .collect(Collectors.joining(" ∨ ", "(", ")")))
                .collect(Collectors.joining(" ∧ "));
    }
}
