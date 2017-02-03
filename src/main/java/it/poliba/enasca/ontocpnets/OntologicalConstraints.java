package it.poliba.enasca.ontocpnets;

import it.poliba.enasca.ontocpnets.sat.BooleanFormula;
import it.poliba.enasca.ontocpnets.sat.DimacsLiterals;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.rdf.rdfxml.parser.IRIProvider;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * A set of ontological constraints.
 * Instances of this class are usually obtained through appropriate methods of {@link OntologicalCPNet}.
 */
public class OntologicalConstraints {
    Set<? extends Constraint> constraintSet;
    private ModelConverter converter;
    private OWLDataFactory owlDataFactory;

    OntologicalConstraints(Set<? extends Constraint> constraintSet,
                           ModelConverter converter,
                           OWLDataFactory owlDataFactory) {
        this.constraintSet = Objects.requireNonNull(constraintSet);
        this.converter = Objects.requireNonNull(converter);
        this.owlDataFactory = Objects.requireNonNull(owlDataFactory);
    }

    public Stream<? extends Constraint> constraints() {
        return constraintSet.stream();
    }

    /**
     * @return
     * @see Constraint#asAxiom(OWLDataFactory, IRIProvider)
     */
    public Stream<OWLSubClassOfAxiom> axioms() {
        return constraints().map(constraint -> constraint.asAxiom(owlDataFactory, converter));
    }

    /**
     * Converts this set of ontological constraints into boolean clauses.
     * @return
     * @see Constraint#asClause(DimacsProvider)
     * @see BooleanFormula#toFormula()
     * @see BooleanFormula#toSynchronizedFormula()
     */
    public Stream<DimacsLiterals> clauses() {
        return constraints().map(constraint -> constraint.asClause(converter));
    }
}
