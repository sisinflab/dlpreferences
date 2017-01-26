package it.poliba.enasca.ontocpnets;

import it.poliba.enasca.ontocpnets.util.StreamBasedBuilder;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An element of the optimum set.
 */
public class OptimalityConstraint extends Constraint {
    /**
     * Encodes the conjunction of elements on the left side of the implication.
     */
    Set<String> condition;

    /**
     * Encodes the disjunction of elements on the right side of the implication.
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

    @Override
    public Map<String, Boolean> left() {
        return condition.stream()
                .collect(Collectors.toMap(Function.identity(), elem -> true));
    }

    @Override
    public Map<String, Boolean> right() {
        return clause.stream()
                .collect(Collectors.toMap(Function.identity(), elem -> true));
    }

    /**
     * Returns a builder for an {@link OptimalityConstraint}.
     * @return
     */
    public static Builder builder() {
        return new Builder();
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
