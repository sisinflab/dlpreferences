package it.poliba.enasca.ontocpnets;

import it.poliba.enasca.ontocpnets.util.StreamBasedBuilder;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An element of the ontological closure.
 */
public class FeasibilityConstraint implements Constraint {
    /**
     * A union of elements with their respective logical state.
     * A boolean value of <code>false</code> indicates a logical complement.
     *
     * <p>This map encodes the elements on the right side of the OWL axiom
     * returned by {@link Constraint#asAxiom(OWLDataFactory, UnaryOperator)}.
     */
    Map<String, Boolean> clause;

    private FeasibilityConstraint(Map<String, Boolean> clause) {
        this.clause = clause;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FeasibilityConstraint other = (FeasibilityConstraint) o;
        return clause.equals(other.clause);

    }

    @Override
    public int hashCode() {
        return clause.hashCode();
    }

    /**
     * Converts this ontological constraint into an axiom of the form:
     * <pre>{@code SubClassOf(owl:Thing, ObjectUnionOf(X, Y, ...))}</pre>
     * @param df the <code>OWLDataFactory</code> instance that will be used to create the axiom
     * @param toIRIString a mapping function between elements stored in this {@link FeasibilityConstraint} and IRI strings
     * @return
     */
    @Override
    public OWLSubClassOfAxiom asAxiom(OWLDataFactory df, UnaryOperator<String> toIRIString) {
        OWLObjectUnionOf union = df.getOWLObjectUnionOf(
                clause.entrySet().stream()
                        .map(entry -> {
                            OWLClass owlClass = df.getOWLClass(toIRIString.apply(entry.getKey()));
                            return (entry.getValue()) ? owlClass : owlClass.getObjectComplementOf();
                        }));
        return df.getOWLSubClassOfAxiom(df.getOWLThing(), union);
    }

    @Override
    public Set<Integer> asClause(ToIntFunction<String> toPositiveLiteral) {
        return clause.entrySet().stream()
                .map(entry -> {
                    int literal = toPositiveLiteral.applyAsInt(entry.getKey());
                    return (entry.getValue()) ? literal : -literal;
                }).collect(Collectors.toSet());
    }

    /**
     * Returns a <code>Collector</code> that accumulates elements into a <code>FeasibilityConstraint</code>.
     * Elements are partitioned according to the return value of <code>logicState</code>,
     * which should return <code>false</code> if the element is a logical complement,
     * then converted by the specified <code>converter</code> into preference domain values.
     * @param logicState a function that returns <code>false</code> if the input element
     *                   is a logical complement, <code>true</code> otherwise
     * @param converter a function that converts an input element into a preference domain value
     * @param <T> the type of input elements
     * @return
     */
    public static <T> Collector<T, ?, FeasibilityConstraint> toFeasibilityConstraint(
            Predicate<T> logicState,
            Function<T, String> converter) {
        Objects.requireNonNull(logicState);
        Objects.requireNonNull(converter);
        return Collectors.collectingAndThen(
                Collectors.toMap(converter, logicState::test),
                FeasibilityConstraint::new);
    }

    /**
     * Returns a builder for a {@link FeasibilityConstraint}.
     * @return
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        String clauseAsString = clause.entrySet().stream()
                .map(entry -> entry.getValue() ?
                        entry.getKey() :
                        "¬" + entry.getKey())
                .collect(Collectors.joining(" ∨ "));
        return "{ " + clauseAsString + " }";
    }

    public static class Builder extends StreamBasedBuilder<FeasibilityConstraint> {
        // Builds the set of positive elements.
        private Stream.Builder<String> positiveBuilder;
        // Builds the set of negated elements.
        private Stream.Builder<String> negatedBuilder;

        private Builder() {
            positiveBuilder = Stream.builder();
            negatedBuilder = Stream.builder();
        }

        /**
         * Adds elements in a positive logical state to the {@link FeasibilityConstraint} being built.
         * @param elements
         * @return
         * @throws NullPointerException if <code>elements</code> is <code>null</code> or contains <code>null</code>s
         */
        public Builder addPositive(String... elements) {
            super.addElements(positiveBuilder, elements);
            return this;
        }

        /**
         * Adds elements in a positive logical state to the {@link FeasibilityConstraint} being built.
         * @param elements
         * @return
         * @throws NullPointerException if <code>elements</code> is <code>null</code> or contains <code>null</code>s
         */
        public Builder addPositive(Iterable<String> elements) {
            super.addElements(positiveBuilder, elements);
            return this;
        }

        /**
         * Adds elements in a negated logical state to the {@link FeasibilityConstraint} being built.
         * @param elements
         * @return
         * @throws NullPointerException if <code>elements</code> is <code>null</code> or contains <code>null</code>s
         */
        public Builder addNegated(String... elements) {
            super.addElements(negatedBuilder, elements);
            return this;
        }

        /**
         * Adds elements in a negated logical state to the {@link FeasibilityConstraint} being built.
         * @param elements
         * @return
         * @throws NullPointerException if <code>elements</code> is <code>null</code> or contains <code>null</code>s
         */
        public Builder addNegated(Iterable<String> elements) {
            super.addElements(negatedBuilder, elements);
            return this;
        }

        /**
         * Builds a {@link FeasibilityConstraint} instance.
         * This method fails if the <code>FeasibilityConstraint</code> being built is not in a valid state.
         * For a <code>FeasibilityConstraint</code> to be in a valid state,
         * the <em>positive set</em> and the <em>negated set</em> must be disjoint and cannot be both empty.
         * @return
         * @throws IllegalStateException if the <code>FeasibilityConstraint</code> being built is not in a valid state
         */
        @Override
        public FeasibilityConstraint build() {
            Map<String, Boolean> positive = positiveBuilder.build()
                    .distinct()
                    .collect(Collectors.toMap(UnaryOperator.identity(), elem -> true));
            Map<String, Boolean> negated = negatedBuilder.build()
                    .distinct()
                    .collect(Collectors.toMap(UnaryOperator.identity(), elem -> false));
            if (positive.isEmpty() && negated.isEmpty()) {
                throw new IllegalStateException("empty clause");
            }
            return new FeasibilityConstraint(
                    Stream.concat(positive.entrySet().stream(), negated.entrySet().stream())
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        }
    }

}
