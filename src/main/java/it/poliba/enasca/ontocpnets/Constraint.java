package it.poliba.enasca.ontocpnets;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import java.util.Map;
import java.util.function.ToIntFunction;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * An ontological constraint, represented as a propositional implication of the form
 * <pre>A ∧ B ∧ ... → X ∨ Y ∨ ...</pre>
 * where the left side is a logical conjunction and the right side is a logical disjunction.
 */
public abstract class Constraint {
    /**
     * Retrieves the left side of the propositional formula.
     * @return a <code>Map</code> that encodes variables as <code>String</code>s
     * associated with their truth value
     */
    public abstract Map<String, Boolean> left();

    /**
     * Retrieves the right side of the propositional formula.
     * @return a <code>Map</code> that encodes variables as <code>String</code>s
     * associated with their truth value
     */
    public abstract Map<String, Boolean> right();

    /**
     * Translates this propositional implication into a boolean clause in DIMACS format.
     * DIMACS literals are obtained using the specified <code>converter</code>, which maps
     * propositional variables to positive integers.
     *
     * <p>The general form
     * <pre>A ∧ B ∧ ... → X ∨ Y ∨ ...</pre>
     * is translated into the equivalent clause
     * <pre>¬A ∨ ¬B ∨ ... ∨ X ∨ Y ∨ ...</pre>
     * @param converter a mapping function between element names and positive DIMACS literals
     * @return
     */
    public IntStream asClause(ToIntFunction<String> converter) {
        IntStream leftSide = left().entrySet().stream()
                .mapToInt(entry -> entry.getValue() ?
                        -converter.applyAsInt(entry.getKey()) :
                        converter.applyAsInt(entry.getKey()));
        IntStream rightSide = right().entrySet().stream()
                .mapToInt(entry -> entry.getValue() ?
                        converter.applyAsInt(entry.getKey()) :
                        -converter.applyAsInt(entry.getKey()));
        return IntStream.concat(leftSide, rightSide);
    }

    /**
     * Translates this propositional implication into an OWL axiom of the form:
     * <pre>{@code SubClassOf(ObjectIntersectionOf(A, B, ...), ObjectUnionOf(X, Y, ...))}</pre>
     * Propositional variables are converted into {@link org.semanticweb.owlapi.model.IRI}s
     * using the specified <code>UnaryOperator</code>, then into {@link org.semanticweb.owlapi.model.OWLClass}es
     * using the specified <code>OWLDataFactory</code>.
     *
     * <p>If the left side of the axiom is empty, the <em>Top</em> entity <code>owl:Thing</code>
     * will be used instead. If the right side is empty, the <em>Bottom</em> entity <code>owl:Nothing</code>
     * will be used instead.
     * @param df
     * @param converter a mapping function between propositional variables and <code>String</code>
     *                  representations of {@link org.semanticweb.owlapi.model.IRI}s
     * @return
     */
    public OWLSubClassOfAxiom asAxiom(OWLDataFactory df, UnaryOperator<String> converter) {
        // Build the left side of the axiom.
        Map<String, Boolean> condition = left();
        OWLClassExpression leftSide = !condition.isEmpty() ?
                df.getOWLObjectIntersectionOf(
                        condition.entrySet().stream().map(entry -> {
                            OWLClass owlClass = df.getOWLClass(converter.apply(entry.getKey()));
                            return (entry.getValue()) ? owlClass : owlClass.getObjectComplementOf();
                        })) :
                df.getOWLThing();
        // Build the right side of the axiom.
        Map<String, Boolean> clause = right();
        OWLClassExpression rightSide = !clause.isEmpty() ?
                df.getOWLObjectUnionOf(
                        clause.entrySet().stream().map(entry -> {
                            OWLClass owlClass = df.getOWLClass(converter.apply(entry.getKey()));
                            return (entry.getValue()) ? owlClass : owlClass.getObjectComplementOf();
                        })) :
                df.getOWLNothing();
        return df.getOWLSubClassOfAxiom(leftSide, rightSide);
    }

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
