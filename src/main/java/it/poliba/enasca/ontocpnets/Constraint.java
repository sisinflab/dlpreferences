package it.poliba.enasca.ontocpnets;

import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import java.util.Set;
import java.util.function.ToIntFunction;
import java.util.function.UnaryOperator;

/**
 * An ontological constraint.
 */
interface Constraint {
    /**
     * Converts the ontological constraint represented by this object into a <em>SubClassOf</em> axiom.
     * @param df the <code>OWLDataFactory</code> instance that will be used to create the axiom
     * @param toIRIString a mapping function between element names and IRI strings
     * @return
     */
    OWLSubClassOfAxiom asAxiom(OWLDataFactory df, UnaryOperator<String> toIRIString);

    /**
     * Converts the ontological constraint represented by this object into a boolean clause in DIMACS format.
     * First, each element belonging to this <code>Constraint</code> is converted into a positive integer
     * using the specified function; then, if the implementation requires it, its sign is flipped to indicate a negation.
     * @param toPositiveLiteral a mapping function between element names and DIMACS literals
     * @return
     */
    Set<Integer> asClause(ToIntFunction<String> toPositiveLiteral);
}
