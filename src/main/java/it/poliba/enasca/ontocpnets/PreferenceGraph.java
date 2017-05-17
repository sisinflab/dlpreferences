package it.poliba.enasca.ontocpnets;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import it.poliba.enasca.ontocpnets.except.SpecFileParseException;
import model.PreferenceSpecification;
import model.PreferenceStatement;
import model.PreferenceVariable;
import util.Constants;

import java.util.*;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A simple graph structure where nodes represent preference variables.
 * Each node is identified by a unique variable name and stores the following information:
 * <ul>
 *     <li>the set of domain elements;</li>
 *     <li>the set of parent nodes;</li>
 *     <li>the <em>optimum set</em>, containing the optimal domain element for each parent assignment.</li>
 * </ul>
 */
public class PreferenceGraph {
    /**
     * A map-based representation of the preference graph, where keys are variable names and values are nodes.
     */
    private Map<String, Node> nodeMap;

    private PreferenceGraph(Map<String, Node> nodeMap) {
        this.nodeMap = nodeMap;
    }

    public Map<String, Node> getNodes() {
        return ImmutableMap.copyOf(nodeMap);
    }

    /**
     * Returns an unmodifiable view over the domain values of each preference variable.
     * @return an unmodifiable <code>Map</code> where each variable name is mapped to its set of domain values
     */
    public Map<String, Set<String>> domainMap() {
        Map<String, Set<String>> view = Maps.transformValues(nodeMap, node -> {
            if (node == null) throw new IllegalStateException("the preference graph contains a null node");
            return node.domain;
        });
        return Collections.unmodifiableMap(view);
    }

    /**
     * Returns the variable names as a <code>Stream</code>.
     * @return
     */
    public Stream<String> variableNames() {
        return nodeMap.keySet().stream();
    }

    /**
     * Returns the domain values of each preference variable as a <code>Stream</code>.
     * @return
     */
    public Stream<String> domainValues() {
        return nodeMap.values().stream().flatMap(Node::domain);
    }

    /**
     * Retrieves the set of constraints that must be satisfied by the undominated outcomes.
     * @return
     */
    public Set<OptimalityConstraint> getOptimumSet() {
        return nodeMap.values().stream()
                .flatMap(Node::optimum)
                .collect(Collectors.toSet());
    }

    /**
     * Return the number of preference variables (nodes) in the graph.
     * @return
     */
    public int size() {
        return nodeMap.size();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Collects information from a <code>PreferenceSpecification</code> instance
     * to build a <code>PreferenceGraph</code>.
     * @param prefSpec
     * @return
     * @throws SpecFileParseException if <code>prefSpec</code> was built from an invalid XML specification file
     */
    static PreferenceGraph fromCrisnerSpec(PreferenceSpecification prefSpec)
            throws SpecFileParseException {
        PreferenceGraph.Builder graphBuilder = PreferenceGraph.builder();
        for (PreferenceVariable v : prefSpec.getVariables()) {
            graphBuilder.addDomainValues(v.getVariableName(), v.getDomainValues());
        }
        for (PreferenceStatement stmt : prefSpec.getStatements()) {
            String variableName = stmt.getVariableName();
            OptimalityConstraint.Builder constraintBuilder = OptimalityConstraint.builder();
            try {
                for (String parentAssignment : stmt.getParentAssignments()) {
                    String[] parsedAssignment = parentAssignment.split("\\=");
                    if (parsedAssignment.length != 2) {
                        throw new Exception(String.format("bad condition specification: '%s'", parentAssignment));
                    }
                    constraintBuilder.addToCondition(parsedAssignment[1]);
                    graphBuilder.addParentNodes(variableName, parsedAssignment[0]);
                }
                if (stmt.getIntravarPreferences().isEmpty()) {
                    throw new Exception(String.format("bad preference statement: '%s'", stmt));
                }
                String intravarPref = stmt.getIntravarPreferences().get(0);
                String[] preferred = intravarPref.split(Constants.PREFERENCE_SYMBOL_IN_XML);
                if (preferred.length == 0) {
                    throw new Exception(String.format("bad preference specification: '%s'", intravarPref));
                }
                constraintBuilder.addToClause(preferred[0]);
            } catch (PatternSyntaxException e) {
                throw new SpecFileParseException(prefSpec.getPrefSpecFileName(), e);
            } catch (Exception e) {
                throw new SpecFileParseException(prefSpec.getPrefSpecFileName(), e.getMessage());
            }
            graphBuilder.addConstraints(variableName, constraintBuilder.build());
        }
        return graphBuilder.build();
    }

    public static class Builder extends StreamBasedBuilder<PreferenceGraph> {
        private Map<String, Node.Builder> builders;

        private Builder() {
            builders = new HashMap<>();
        }

        /**
         * If <code>variableName</code> has no associated node builder in {@link #builders}, creates a new instance.
         * @param variableName
         * @return the (existing or computed) node builder associated with <code>variableName</code>
         * @throws NullPointerException if <code>variableName</code> is <code>null</code>
         */
        private Node.Builder createIfAbsent(String variableName) {
            return builders.computeIfAbsent(Objects.requireNonNull(variableName), key -> Node.builder());
        }

        /**
         * Adds <code>elements</code> to the set of domain values of the specified variable.
         * The corresponding node will be updated if already present, otherwise it will be created.
         * @param elements
         * @return
         * @throws NullPointerException if any argument is <code>null</code>
         */
        public Builder addDomainValues(String variableName, String... elements) {
            super.addElements(createIfAbsent(variableName).domainBuilder, elements);
            return this;
        }

        /**
         * Adds <code>elements</code> to the set of domain values of the specified variable.
         * The corresponding node will be updated if already present, otherwise it will be created.
         * @param elements
         * @return
         * @throws NullPointerException if any argument is <code>null</code>
         */
        public Builder addDomainValues(String variableName, Iterable<String> elements) {
            super.addElements(createIfAbsent(variableName).domainBuilder, elements);
            return this;
        }

        /**
         * Adds <code>elements</code> to the set of parents of the specified variable.
         * The corresponding node will be updated if already present, otherwise it will be created.
         * @param elements
         * @return
         * @throws NullPointerException if any argument is <code>null</code>
         */
        public Builder addParentNodes(String variableName, String... elements) {
            super.addElements(createIfAbsent(variableName).parentsBuilder, elements);
            return this;
        }

        /**
         * Adds <code>elements</code> to the set of parents of the specified variable.
         * The corresponding node will be updated if already present, otherwise it will be created.
         * @param elements
         * @return
         * @throws NullPointerException if any argument is <code>null</code>
         */
        public Builder addParentNodes(String variableName, Iterable<String> elements) {
            super.addElements(createIfAbsent(variableName).parentsBuilder, elements);
            return this;
        }

        /**
         * Adds <code>elements</code> to the set of parents of the specified variable.
         * The corresponding node will be updated if already present, otherwise it will be created.
         * @param elements
         * @return
         * @throws NullPointerException if any argument is <code>null</code>
         */
        public Builder addConstraints(String variableName, OptimalityConstraint... elements) {
            super.addElements(createIfAbsent(variableName).optimumBuilder, elements);
            return this;
        }

        /**
         * Adds <code>elements</code> to the set of parents of the specified variable.
         * The corresponding node will be updated if already present, otherwise it will be created.
         * @param elements
         * @return
         * @throws NullPointerException if any argument is <code>null</code>
         */
        public Builder addConstraints(String variableName, Iterable<OptimalityConstraint> elements) {
            super.addElements(createIfAbsent(variableName).optimumBuilder, elements);
            return this;
        }

        /**
         * Build a {@link PreferenceGraph} instance.
         * For each node, the optimum set undergoes a two-step validation process:
         * first, it is checked for consistency; then, it is rebuilt in an equivalent long form,
         * which lists every parent assignment explicitly.
         *
         * <p>The consistency check involves satisfying the following conditions:
         * <ul>
         *     <li>for each parent assignment, there must be exactly one applicable optimality constraint;</li>
         *     <li><em>clause</em>s must not contain elements outside the domain of the current node;</li>
         * </ul>
         * As an additional validation requirement, no duplicate domain values are allowed across nodes.
         * @return
         * @throws IllegalStateException if the <code>PreferenceGraph</code> being built is not in a valid state
         */
        @Override
        public PreferenceGraph build() {
            // Build the temporary graph.
            Map<String, Node> nodeMap = builders.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().build()));
            // Validate the domain values.
            if (nodeMap.values().stream().mapToInt(node -> node.domain.size()).sum() !=
                    nodeMap.values().stream().flatMap(Node::domain).distinct().count()) {
                throw new IllegalStateException("duplicate domain elements in different nodes");
            }
            // Validate the temporary graph and rebuild the optimum sets.
            for (Node node : nodeMap.values()) {
                // Build the list of parent assignments.
                List<Set<String>> parentDomains = node.parents()
                        .map(parentName -> nodeMap.get(parentName).domain)
                        .collect(Collectors.toList());
                Set<List<String>> parentAssignments = Sets.cartesianProduct(parentDomains);
                // Validate the optimum set and build the equivalent long form.
                Stream.Builder<OptimalityConstraint> optimumLongForm = Stream.builder();
                for (List<String> assignment : parentAssignments) {
                    Set<Set<String>> applicableClauses = node.getApplicableConstraints(assignment)
                            .map(constraint -> constraint.clause)
                            .collect(Collectors.toSet());
                    if (applicableClauses.isEmpty()) {
                        throw new IllegalStateException("no applicable preference for assignment " + assignment);
                    }
                    if (applicableClauses.size() > 1) {
                        throw new IllegalStateException("conflicting preferences for assignment " + assignment);
                    }
                    optimumLongForm.accept(OptimalityConstraint.builder()
                            .addToCondition(assignment)
                            .addToClause(applicableClauses.iterator().next())
                            .build());
                }
                // Replace the optimum set with the equivalent long form.
                node.optimum = optimumLongForm.build().collect(Collectors.toSet());
            }
            return new PreferenceGraph(nodeMap);
        }
    }

    /**
     * The set of properties associated with a node of the preference graph.
     */
    public static class Node {
        // The set of domain values.
        Set<String> domain;
        // The set of parent nodes.
        Set<String> parents;
        // The optimum set.
        Set<OptimalityConstraint> optimum;

        private Node(Set<String> domain, Set<String> parents, Set<OptimalityConstraint> optimum) {
            this.domain = domain;
            this.parents = parents;
            this.optimum = optimum;
        }

        public Stream<String> domain() {
            return domain.stream();
        }

        public Stream<String> parents() {
            return parents.stream();
        }

        public Stream<OptimalityConstraint> optimum() {
            return optimum.stream();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node that = (Node) o;
            return domain.equals(that.domain) &&
                    parents.equals(that.parents) &&
                    optimum.equals(that.optimum);
        }

        @Override
        public int hashCode() {
            int result = domain.hashCode();
            result = 31 * result + parents.hashCode();
            return 31 * result + optimum.hashCode();
        }

        /**
         * Retrieves the applicable constraints (if any) for the specified <code>assignment</code>.
         * An applicable constraint is an element of <em>optimum set</em> whose <em>condition</em>
         * is a subset of <code>assignment</code>.
         * In other words, an applicable constraint verifies the following predicate:
         * <pre>{@code assignment.containsAll(constraint.condition)}</pre>
         * @return
         */
        Stream<OptimalityConstraint> getApplicableConstraints(Collection<String> assignment) {
            return optimum().filter(constraint -> assignment.containsAll(constraint.condition));
        }

        private static Builder builder() {
            return new Builder();
        }

        private static class Builder {
            // Builds the set of domain values.
            private Stream.Builder<String> domainBuilder;
            // Builds the set of parents.
            private Stream.Builder<String> parentsBuilder;
            // Builds the optimum
            private Stream.Builder<OptimalityConstraint> optimumBuilder;

            private Builder() {
                domainBuilder = Stream.builder();
                parentsBuilder = Stream.builder();
                optimumBuilder = Stream.builder();
            }

            /**
             * Builds a {@link Node} instance.
             * This method fails if the <code>Node</code> being built is not in a valid state.
             * For a <code>Node</code> to be in a valid state, no <em>clause</em> of the <em>optimum set</em>
             * may contain items outside the set of domain elements.
             * @return
             * @throws IllegalStateException if the <code>Node</code> being built contains invalid clauses
             */
            public Node build() {
                Set<String> domain = domainBuilder.build().collect(Collectors.toSet());
                Set<String> parents = parentsBuilder.build().collect(Collectors.toSet());
                Set<OptimalityConstraint> optimum = optimumBuilder.build().collect(Collectors.toSet());
                if (optimum.stream().anyMatch(constraint -> !domain.containsAll(constraint.clause))) {
                    throw new IllegalStateException();
                }
                return new Node(domain, parents, optimum);
            }
        }

    }
}
