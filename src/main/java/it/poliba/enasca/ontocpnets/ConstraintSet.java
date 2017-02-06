package it.poliba.enasca.ontocpnets;

import it.poliba.enasca.ontocpnets.sat.BooleanFormula;
import it.poliba.enasca.ontocpnets.sat.DimacsLiterals;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.rdf.rdfxml.parser.IRIProvider;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * A set of ontological constraints.
 * Instances of this class are usually obtained through appropriate methods of {@link OntologicalCPNet}.
 * @param <T> the type of Constraint stored in this object
 */
public class ConstraintSet<T extends Constraint>
        extends AbstractSet<T>
        implements Set<T> {
    private Set<T> constraints;
    private ModelConverter converter;
    private OWLDataFactory owlDataFactory;

    /**
     * Constructs a new <code>ConstraintSet</code> backed by the specified <code>Set</code>.
     * @param constraints
     * @param converter
     * @param owlDataFactory
     */
    ConstraintSet(Set<T> constraints,
                  ModelConverter converter,
                  OWLDataFactory owlDataFactory) {
        super();
        this.constraints = Objects.requireNonNull(constraints);
        this.converter = Objects.requireNonNull(converter);
        this.owlDataFactory = Objects.requireNonNull(owlDataFactory);
    }

    /**
     * @return
     * @see Constraint#asAxiom(OWLDataFactory, IRIProvider)
     */
    public Stream<OWLSubClassOfAxiom> axioms() {
        return stream().map(constraint -> constraint.asAxiom(owlDataFactory, converter));
    }

    /**
     * Converts this set of ontological constraints into boolean clauses.
     * @return
     * @see Constraint#asClause(DimacsProvider)
     * @see BooleanFormula#toFormula()
     * @see BooleanFormula#toSynchronizedFormula()
     */
    public Stream<DimacsLiterals> clauses() {
        return stream().map(constraint -> constraint.asClause(converter));
    }

    @Override
    public int size() {
        return constraints.size();
    }

    @Override
    public Iterator<T> iterator() {
        return constraints.iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ConstraintSet<?> other = (ConstraintSet<?>) o;
        return converter.equals(other.converter);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + converter.hashCode();
        return result;
    }
}
