package it.poliba.enasca.ontocpnets;

import com.google.common.collect.ImmutableMap;
import it.poliba.enasca.ontocpnets.sat.DimacsLiterals;
import it.poliba.enasca.ontocpnets.sat.VarNameProvider;
import it.poliba.enasca.ontocpnets.util.StreamBasedBuilder;
import org.semanticweb.owlapi.model.OWLDataFactory;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An element of the ontological closure.
 */
public class FeasibilityConstraint extends AbstractConstraint {
    /**
     * A union of elements with their respective logical state.
     * A boolean value of <code>false</code> indicates a logical complement.
     *
     * <p>This map encodes the elements on the right side of the OWL axiom
     * returned by {@link Constraint#asAxiom(OWLDataFactory, org.semanticweb.owlapi.rdf.rdfxml.parser.IRIProvider)}.
     */
    private Map<String, Boolean> clause;

    private FeasibilityConstraint(Map<String, Boolean> clause) {
        this.clause = clause;
    }

    public FeasibilityConstraint(DimacsLiterals clause, VarNameProvider converter) {
        this(clause.asMap(converter));
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

    @Override
    public Map<String, Boolean> left() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Boolean> right() {
        return ImmutableMap.copyOf(clause);
    }

    /**
     * Returns a <code>Collector</code> that accumulates elements into a <code>FeasibilityConstraint</code>.
     * Elements are converted into preference domain values by the specified <code>converter</code>,
     * then partitioned according to <code>truthValueFunc</code>, which returns their truth value.
     * @param truthValueFunc a function that returns the truth value of the input element
     * @param converter a function that converts an input element into a preference domain value
     * @param <T> the type of input elements
     * @return
     */
    static <T> Collector<T, ?, FeasibilityConstraint> toConstraint(
            Predicate<T> truthValueFunc,
            Function<T, String> converter) {
        Objects.requireNonNull(truthValueFunc);
        Objects.requireNonNull(converter);
        return Collectors.collectingAndThen(
                Collectors.toMap(converter, truthValueFunc::test),
                FeasibilityConstraint::new);
    }

    /**
     * Returns a builder for a {@link FeasibilityConstraint}.
     * @return
     */
    public static Builder builder() {
        return new Builder();
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
