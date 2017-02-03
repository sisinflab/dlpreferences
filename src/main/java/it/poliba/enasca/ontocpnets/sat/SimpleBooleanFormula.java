package it.poliba.enasca.ontocpnets.sat;

import java.util.HashSet;
import java.util.Set;

/**
 * A simple implementation of {@link BooleanFormula}.
 */
class SimpleBooleanFormula extends AbstractBooleanFormula {
    SimpleBooleanFormula(Set<DimacsLiterals> clauses) {
        super(clauses);
    }

    SimpleBooleanFormula() {
        super(new HashSet<>());
    }

    @Override
    public BooleanFormula copy() {
        return clauses().collect(BooleanFormula.toFormula());
    }

}
