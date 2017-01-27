package it.poliba.enasca.ontocpnets;

import java.util.stream.Collectors;

abstract class AbstractConstraint implements Constraint {
    @Override
    public String toString() {
        String leftSide = left().entrySet().stream()
                .map(entry -> entry.getValue() ?
                        entry.getKey() :
                        "¬" + entry.getKey())
                .collect(Collectors.joining(" ∧ "));
        String rightSide = right().entrySet().stream()
                .map(entry -> entry.getValue() ?
                        entry.getKey() :
                        "¬" + entry.getKey())
                .collect(Collectors.joining(" ∨ "));
        if (leftSide.isEmpty()) {
            return '{' + rightSide + '}';
        }
        return '{' + leftSide + " → " + rightSide + '}';
    }
}
