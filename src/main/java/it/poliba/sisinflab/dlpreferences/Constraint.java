package it.poliba.sisinflab.dlpreferences;

import it.poliba.sisinflab.dlpreferences.sat.DimacsLiterals;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.rdf.rdfxml.parser.IRIProvider;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * An ontological constraint, represented as a propositional implication of the form
 * <pre>A AND B AND ... IMPLIES X OR Y OR ...</pre>
 * where the left side is a logical conjunction and the right side is a logical disjunction.
 */
public interface Constraint {
    /**
     * Retrieves the left side of the propositional formula.
     * @return a <code>Map</code> that encodes variables as <code>String</code>s
     * associated with their truth value
     */
    Map<String, Boolean> left();

    /**
     * Retrieves the right side of the propositional formula.
     * @return a <code>Map</code> that encodes variables as <code>String</code>s
     * associated with their truth value
     */
    Map<String, Boolean> right();

    /**
     * Translates this propositional implication into a boolean clause in DIMACS format.
     * DIMACS literals are obtained using the specified <code>converter</code>.
     *
     * <p>The general form
     * <pre>A AND B AND ... IMPLIES X OR Y OR ...</pre>
     * is translated into the equivalent clause
     * <pre>not(A) OR not(B) OR ... OR X OR Y OR ...</pre>
     * @param converter a mapping function between element names and DIMACS literals
     * @return
     */
    default DimacsLiterals asClause(DimacsProvider converter) {
        IntStream leftSide = left().entrySet().stream()
                .mapToInt(converter::getLiteral)
                .map(literal -> -literal);
        IntStream rightSide = right().entrySet().stream()
                .mapToInt(converter::getLiteral);
        return new DimacsLiterals(IntStream.concat(leftSide, rightSide));
    }

    /**
     * Translates this propositional implication into an OWL axiom of the form:
     * <pre>{@code SubClassOf(ObjectIntersectionOf(A, B, ...), ObjectUnionOf(X, Y, ...))}</pre>
     * Propositional variables are converted into {@link org.semanticweb.owlapi.model.IRI}s
     * using the specified <code>IRIProvider</code>, then into {@link org.semanticweb.owlapi.model.OWLClass}es
     * using the specified <code>OWLDataFactory</code>.
     *
     * <p>If the left side of the axiom is empty, the <em>Top</em> entity <code>owl:Thing</code>
     * will be used instead. If the right side is empty, the <em>Bottom</em> entity <code>owl:Nothing</code>
     * will be used instead.
     * @param df
     * @param iriProvider
     * @return
     */
    default OWLSubClassOfAxiom asAxiom(OWLDataFactory df, IRIProvider iriProvider) {
        // Define a function that converts propositional variables into OWL classes.
        Function<Map<String, Boolean>, Stream<OWLClassExpression>> owlClassProvider =
                propositionalVars -> propositionalVars.entrySet().stream()
                        .map(entry -> {
                            OWLClass owlClass = df.getOWLClass(iriProvider.getIRI(entry.getKey()));
                            return (entry.getValue()) ? owlClass : owlClass.getObjectComplementOf();
                        });
        // Build the left side of the axiom.
        Map<String, Boolean> condition = left();
        OWLClassExpression leftSide = !condition.isEmpty() ?
                df.getOWLObjectIntersectionOf(owlClassProvider.apply(condition)) :
                df.getOWLThing();
        // Build the right side of the axiom.
        Map<String, Boolean> clause = right();
        OWLClassExpression rightSide = !clause.isEmpty() ?
                df.getOWLObjectUnionOf(owlClassProvider.apply(clause)) :
                df.getOWLNothing();
        return df.getOWLSubClassOfAxiom(leftSide, rightSide);
    }

}
