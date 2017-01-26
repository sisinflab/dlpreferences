package it.poliba.enasca.ontocpnets.sat;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.IntStream;

/**
 * A thread-safe equivalent of {@link BooleanFormula}.
 */
public class ConcurrentBooleanFormula extends BooleanFormula {
    private ConcurrentBooleanFormula(Set<DIMACSLiterals> clauses) {
        super(clauses);
    }

    public ConcurrentBooleanFormula() {
        super(Collections.synchronizedSet(new HashSet<>()));
    }

    @Override
    public void addNegatedClause(IntStream clause) {
        Objects.requireNonNull(clause).forEach(
                literal -> addLiteral(-literal));
    }

    public static Collector<DIMACSLiterals, ?, ConcurrentBooleanFormula> toConcurrentFormula() {
        return Collector.of(
                () -> Collections.<DIMACSLiterals>synchronizedSet(new HashSet<>()),
                Set::add,
                (left, right) -> { left.addAll(right); return left; },
                ConcurrentBooleanFormula::new,
                Collector.Characteristics.CONCURRENT, Collector.Characteristics.UNORDERED);
    }
}
