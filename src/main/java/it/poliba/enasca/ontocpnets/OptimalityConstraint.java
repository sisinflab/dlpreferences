package it.poliba.enasca.ontocpnets;

import it.poliba.enasca.ontocpnets.util.StreamBasedBuilder;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import java.util.Set;
import java.util.function.ToIntFunction;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An element of the optimum set.
 */
public class OptimalityConstraint implements Constraint {
    /**
     * Encodes the elements on the left side of the OWL axiom returned by {@link Constraint#asAxiom(OWLDataFactory, UnaryOperator)}.
     */
    Set<String> condition;

    /**
     * Encodes the elements on the right side of the OWL axiom returned by {@link Constraint#asAxiom(OWLDataFactory, UnaryOperator)}.
     */
    Set<String> clause;

    /**
     * Constructs an <code>OptimalityConstraint</code> as a subclass relationship between
     * an intersection of elements (the <code>condition</code>) on the left side
     * and a union of elements (the <code>clause</code>) on the right side.
     * @param condition the intersection of conditions
     * @param clause the union of preferred elements
     */
    private OptimalityConstraint(Set<String> condition, Set<String> clause) {
        this.condition = condition;
        this.clause = clause;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OptimalityConstraint that = (OptimalityConstraint) o;
        return condition.equals(that.condition) && clause.equals(that.clause);
    }

    @Override
    public int hashCode() {
        int result = condition.hashCode();
        return 31 * result + clause.hashCode();
    }

    /**
     * Converts this ontological constraint into an axiom of the form:
     * <pre>{@code SubClassOf(ObjectIntersectionOf(A, B, ...), ObjectUnionOf(X, Y, ...))}</pre>
     * If this class was constructed with an empty <em>condition set</em>, the left side of the
     * <em>SubClassOf</em> relationship is replaced with <i>owl:Thing</i>.
     * @param df the <code>OWLDataFactory</code> instance that will be used to create the axiom
     * @param toIRIString a mapping function between elements stored in this {@link OptimalityConstraint} and IRI strings
     * @return
     */
    @Override
    public OWLSubClassOfAxiom asAxiom(OWLDataFactory df, UnaryOperator<String> toIRIString) {
        OWLClassExpression leftSide = condition.isEmpty() ?
                df.getOWLThing() :
                df.getOWLObjectIntersectionOf(
                        condition.stream()
                                .map(name -> df.getOWLClass(toIRIString.apply(name))));
        OWLClassExpression rightSide = df.getOWLObjectUnionOf(
                clause.stream()
                        .map(name -> df.getOWLClass(toIRIString.apply(name))));
        return df.getOWLSubClassOfAxiom(leftSide, rightSide);
    }

    @Override
    public Set<Integer> asClause(ToIntFunction<String> toPositiveLiteral) {
        // De Morgan's law: NOT(a AND b) is equivalent to (NOT(a) OR NOT(b))
        Stream<Integer> leftSide = condition.stream()
                .map(elementName -> -toPositiveLiteral.applyAsInt(elementName));
        Stream<Integer> rightSide = clause.stream()
                .map(toPositiveLiteral::applyAsInt);
        return Stream.concat(leftSide, rightSide)
                .collect(Collectors.toSet());
    }

    /**
     * Returns a builder for an {@link OptimalityConstraint}.
     * @return
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{ ");
        if (!condition.isEmpty()) {
            sb.append(String.join(" ∧ ", condition));
            sb.append(" -> ");
        }
        sb.append(String.join(" ∨ ", clause));
        sb.append(" }");
        return sb.toString();
    }

    public static class Builder extends StreamBasedBuilder<OptimalityConstraint> {
        // Builds the condition set.
        private Stream.Builder<String> conditionBuilder;
        // Builds the clause.
        private Stream.Builder<String> clauseBuilder;

        private Builder() {
            conditionBuilder = Stream.builder();
            clauseBuilder = Stream.builder();
        }

        /**
         * Adds elements to the <em>condition set</em> of the {@link OptimalityConstraint} being built.
         * @param elements
         * @return
         * @throws NullPointerException if <code>elements</code> is <code>null</code> or contains <code>null</code>s
         */
        public Builder addToCondition(String... elements) {
            super.addElements(conditionBuilder, elements);
            return this;
        }

        /**
         * Adds elements to the <em>condition set</em> of the {@link OptimalityConstraint} being built.
         * @param elements
         * @return
         * @throws NullPointerException if <code>elements</code> is <code>null</code> or contains <code>null</code>s
         */
        public Builder addToCondition(Iterable<String> elements) {
            super.addElements(conditionBuilder, elements);
            return this;
        }

        /**
         * Adds elements to the <em>clause</em> of the {@link OptimalityConstraint} being built.
         * @param elements
         * @return
         * @throws NullPointerException if <code>elements</code> is <code>null</code> or contains <code>null</code>s
         */
        public Builder addToClause(String... elements) {
            super.addElements(clauseBuilder, elements);
            return this;
        }

        /**
         * Adds elements to the <em>clause</em> of the {@link OptimalityConstraint} being built.
         * @param elements
         * @return
         * @throws NullPointerException if <code>elements</code> is <code>null</code> or contains <code>null</code>s
         */
        public Builder addToClause(Iterable<String> elements) {
            super.addElements(clauseBuilder, elements);
            return this;
        }

        /**
         * Builds an {@link OptimalityConstraint} instance.
         * This method fails if the <code>OptimalityConstraint</code> being built is not in a valid state.
         * For an <code>OptimalityConstraint</code> to be in a valid state, no <em>clause</em> may be empty.
         * @return
         * @throws IllegalStateException if the <code>OptimalityConstraint</code> being built contains an empty clause
         */
        @Override
        public OptimalityConstraint build() {
            Set<String> clause = clauseBuilder.build()
                    .collect(Collectors.toSet());
            if (clause.isEmpty()) throw new IllegalStateException("empty clause");
            Set<String> condition = conditionBuilder.build()
                    .collect(Collectors.toSet());
            return new OptimalityConstraint(condition, clause);
        }
    }

}
