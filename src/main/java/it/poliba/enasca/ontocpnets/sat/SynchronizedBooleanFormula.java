package it.poliba.enasca.ontocpnets.sat;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * A thread-safe implementation of {@link BooleanFormula}.
 */
class SynchronizedBooleanFormula extends AbstractBooleanFormula implements BooleanFormula {
    SynchronizedBooleanFormula(Set<DimacsLiterals> clauses) {
        super(clauses);
    }

    SynchronizedBooleanFormula() {
        super(Collections.synchronizedSet(new HashSet<>()));
    }

    @Override
    public void addNegatedClause(DimacsLiterals clause) {
        Objects.requireNonNull(clause).stream()
                .forEach(literal -> addLiteral(-literal));
    }

    @Override
    public Stream<DimacsLiterals> clauses() {
        return clausesCNF.parallelStream();
    }

    @Override
    public BooleanFormula copy() {
        return clauses().collect(BooleanFormula.toSynchronizedFormula());
    }
}
