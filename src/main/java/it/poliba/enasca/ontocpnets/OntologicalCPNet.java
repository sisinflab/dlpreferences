package it.poliba.enasca.ontocpnets;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Sets;
import exception.PreferenceReasonerException;
import it.poliba.enasca.ontocpnets.sat.*;
import it.poliba.enasca.ontocpnets.util.Lazy;
import it.poliba.enasca.ontocpnets.util.LogicalSortedForest;
import model.Outcome;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.ChangeApplied;
import org.semanticweb.owlapi.model.parameters.OntologyCopy;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An ontological CP-net.
 */
public class OntologicalCPNet extends CPNet {

    /**
     * Stores equivalent representations of the preference domain entities that were added to the base ontology.
     */
    private DomainTable domainTable;

    /**
     * The augmented ontology, constructed by adding preference domain entities to the base ontology.
     */
    OWLOntology ontology;

    /**
     * The <code>SATSolver</code> implementation used internally to find satisfiable models
     * for collections of ontological constraints, such as {@link PreferenceGraph#getOptimumSet()}
     * and {@link #getClosure()}.
     */
    private SATSolver solver;

    /**
     * The ontological closure, wrapped in a lazy initializer.
     * The inner integers represent domain values, in accordance with the mappings
     * specified in {@link DomainTable#table}. A negative integer indicates
     * the complement of the corresponding domain value.
     */
    private Lazy<Set<FeasibilityConstraint>> closure;

    private OntologicalCPNet(Builder builder) {
        super(builder.baseCPNet);
        domainTable = builder.domainTable;
        ontology = builder.augmentedOntology;
        solver = builder.solver;
        closure = new Lazy<>(this::computeClosure);
    }

    public Set<FeasibilityConstraint> getClosure() {
        return closure.getOrCompute();
    }

    /**
     * Computes the optimal outcomes using the ontological variant of the HARD-PARETO algorithm.
     * @return
     * @throws IOException if an internal call to {@link #dominates(Outcome, Outcome)} results in an <code>IOException</code>
     */
    public Set<Outcome> hardPareto() throws IOException {
        Set<DIMACSLiterals> undominatedModels, feasibleModels, optimalModels;
        // Solve the optimum set and the ontological closure as boolean problems.
        undominatedModels = solveConstraints(graph.getOptimumSet().stream())
                .collect(Collectors.toSet());
        feasibleModels = solveConstraints(getClosure().stream())
                .collect(Collectors.toSet());
        optimalModels = solveConstraints(
                Stream.concat(
                        graph.getOptimumSet().stream(),
                        getClosure().stream()))
                .collect(Collectors.toSet());
        Set<Outcome> feasibleOutcomes = feasibleModels.stream()
                .map(this::interpretModel)
                .collect(Collectors.toSet());
        Set<Outcome> optimalOutcomes = optimalModels.stream()
                .map(this::interpretModel)
                .collect(Collectors.toSet());
        // Find the set of optimal (feasible + undominated) outcomes.
        if (optimalModels.equals(feasibleModels) ||
                (!undominatedModels.isEmpty() && optimalModels.equals(undominatedModels))) {
            return optimalOutcomes;
        }
        Stream.Builder<Outcome> additionalOptimal = Stream.builder();
        for (Outcome unverified : Sets.difference(feasibleOutcomes, optimalOutcomes)) {
            boolean isDominated = false;
            for (Iterator<Outcome> i = feasibleOutcomes.iterator(); i.hasNext() && !isDominated; ) {
                try {
                    isDominated = dominates(i.next(), unverified);
                } catch (PreferenceReasonerException e) {
                    throw new IllegalStateException("invalid Outcome object", e);
                }
            }
            if (!isDominated) {
                additionalOptimal.accept(unverified);
            }
        }
        return Sets.union(optimalOutcomes, additionalOptimal.build().collect(Collectors.toSet()));
    }

    /**
     * Returns a builder that builds an {@link OntologicalCPNet} upon the specified {@link CPNet}.
     * @param baseCPNet the base object that will be used when constructing the {@link OntologicalCPNet} instance
     * @return
     */
    public static Builder builder(CPNet baseCPNet) {
        return new Builder(baseCPNet);
    }

    private Set<FeasibilityConstraint> computeClosure() {
        LogicalSortedForest<Integer> forest = domainTable.getDimacsLiterals().stream()
                .collect(LogicalSortedForest.toLogicalSortedForest(literal -> -literal));
        ClosureBuilder closureBuilder = new ClosureBuilder();
        while (!forest.isEmpty()) {
            forest.expand(branch -> closureBuilder.accept(
                    branch.collect(FeasibilityConstraint.toFeasibilityConstraint(
                            literal -> literal > 0,
                            domainTable::getDomainElement))));
        }
        return closureBuilder.build();
    }

    /**
     * Translates a set of constraints into a boolean satisfiability problem
     * and finds satisfiable models.
     * @param constraints
     * @return
     */
    private Stream<DIMACSLiterals> solveConstraints(Stream<? extends Constraint> constraints) {
        BooleanFormula formula = constraints
                .map(constraint -> constraint.asClause(domainTable::getDimacsLiteral))
                .map(DIMACSLiterals::new)
                .collect(ConcurrentBooleanFormula.toFormula());
        return solver.solve(formula);
    }

    /**
     * Converts a satisfiable DIMACS model into an {@link Outcome}.
     * @param model
     * @return
     */
    private Outcome interpretModel(DIMACSLiterals model) {
        Set<String> domainValuesInModel = model.stream()
                .filter(dimacsLiteral -> dimacsLiteral > 0)
                .mapToObj(domainTable::getDomainElement)
                .collect(Collectors.toSet());
        Map<String, String> assignments = graph.domainMap().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            Set<String> matchingDomainValues = entry.getValue().stream()
                                    .filter(domainValuesInModel::contains)
                                    .collect(Collectors.toSet());
                            if (matchingDomainValues.isEmpty()) {
                                throw new IllegalStateException(
                                        String.format("no assignment for variable '%s'", entry.getKey()));
                            }
                            if (matchingDomainValues.size() > 1) {
                                throw new IllegalStateException(
                                        String.format("conflicting assignments for variable '%s'", entry.getKey()));
                            }
                            return matchingDomainValues.iterator().next();
                        }
                ));
        try {
            return new Outcome(assignments);
        } catch (PreferenceReasonerException e) {
            throw new IllegalStateException("invalid outcome", e);
        }
    }

    /**
     * Builds the ontological closure by accepting {@link FeasibilityConstraint}s
     * and checking whether they are eligible for inclusion in the closure.
     * A feasibility constraint is eligible if the axiom obtained from
     * {@link Constraint#asAxiom(OWLDataFactory, UnaryOperator)} is entailed by the ontology.
     *
     * <p>This is a thread-safe implementation.
     */
    private class ClosureBuilder {
        private Set<FeasibilityConstraint> closure;
        private ConcurrentBooleanFormula closureAsFormula;
        private OWLDataFactory owlDataFactory;

        public ClosureBuilder() {
            closure = Collections.synchronizedSet(new HashSet<>());
            closureAsFormula = new ConcurrentBooleanFormula();
            owlDataFactory = OWLManager.createConcurrentOWLOntologyManager().getOWLDataFactory();
        }

        /**
         * Returns <code>true</code> if the specified <code>constraint</code> is eligible
         * for inclusion in the ontological closure.
         * A feasibility constraint is eligible if the axiom obtained from
         * {@link Constraint#asAxiom(OWLDataFactory, UnaryOperator)} is entailed by the ontology.
         *
         * <p>If an eligible constraint is not redundant (that is, it is not already
         * entailed by the axioms collected in the closure so far), it is added to the closure.
         * @param constraint
         * @return
         */
        public boolean accept(FeasibilityConstraint constraint) {
            int[] branchClause = Objects.requireNonNull(constraint)
                    .asClause(domainTable::getDimacsLiteral)
                    .toArray();
            // Check whether the current branch clause is entailed by the closure.
            if (solver.implies(closureAsFormula, Arrays.stream(branchClause))) {
                return true;
            }
            // Since the HermiT reasoner does not support concurrency, a fresh instance must be created.
            OWLReasoner ontologyReasoner = new ReasonerFactory().createReasoner(ontology);
            // Check whether the current branch axiom is entailed by the ontology.
            OWLSubClassOfAxiom branchAxiom = constraint.asAxiom(owlDataFactory, domainTable::getIRIString);
            boolean isEntailed = ontologyReasoner.isEntailed(branchAxiom);
            ontologyReasoner.dispose();
            if (isEntailed) {
                closure.add(constraint);
                closureAsFormula.addClause(Arrays.stream(branchClause));
            }
            return isEntailed;
        }

        /**
         * Returns an immutable <code>Set</code> containing the constraints collected so far.
         * @return
         */
        public Set<FeasibilityConstraint> build() {
            return ImmutableSet.copyOf(closure);
        }

    }

    /**
     * Stores equivalent representations of the preference domain entities that were
     * added to the base ontology.
     */
    private static class DomainTable {

        /**
         * Stores equivalent representations of the preference domain entities that were added to the base ontology.
         * Specifically:
         * <ul>
         *     <li>rows contain the string identifiers found in the preference specification file;</li>
         *     <li>columns contain positive integers, for use as DIMACS literals in boolean satisfiability problems;</li>
         *     <li>values contain IRI strings.</li>
         * </ul>
         *
         * <p>For example, consider the preference variables <i>A</i> and <i>B</i>, with domain values
         * <i>a1, a2, a3</i> and <i>b1, b2</i>, respectively. The corresponding table might be:
         * <pre>{@code
         * "a1" <-> 1 -> "http://www.semanticweb.org/myname/myontology#a1"
         * "a2" <-> 2 -> "http://www.semanticweb.org/myname/myontology#a2"
         * "a3" <-> 3 -> "http://www.semanticweb.org/myname/myontology#a3"
         * "b1" <-> 4 -> "http://www.semanticweb.org/myname/myontology#b1"
         * "b2" <-> 5 -> "http://www.semanticweb.org/myname/myontology#b2"
         * }</pre>
         *
         * <p>Note that no particular order is guaranteed when assigning DIMACS literals.
         */
        private ImmutableTable<String, Integer, String> table;

        public DomainTable(Set<String> elements, UnaryOperator<String> toIRIString) {
            Objects.requireNonNull(toIRIString);
            ImmutableTable.Builder<String, Integer, String> builder = ImmutableTable.builder();
            int dimacsLiteral = 0;
            for (String element : Objects.requireNonNull(elements)) {
                builder.put(element, ++dimacsLiteral, toIRIString.apply(element));
            }
            table = builder.build();
        }

        /**
         * Retrieves the DIMACS representation of the specified domain element.
         * @param domainElement
         * @return
         * @throws NoSuchElementException if the argument does not exist in the internal table
         */
        public int getDimacsLiteral(String domainElement) {
            return table.row(domainElement).keySet().iterator().next();
        }

        /**
         * Retrieves the domain element corresponding to the specified DIMACS literal.
         * If <code>value</code> is negative, its sign is automatically flipped
         * before checking the internal table.
         * @param value
         * @return
         * @throws NoSuchElementException if the argument does not exist in the internal table
         * @throws IllegalArgumentException if the argument is 0
         */
        public String getDomainElement(int value) {
            if (value == 0) throw new IllegalArgumentException();
            if (value < 0) value = -value;
            return table.column(value).keySet().iterator().next();
        }

        /**
         * Retrieves the IRI string corresponding to the specified domain element.
         * @param domainElement
         * @return
         * @throws NoSuchElementException if the argument does not exist in the internal table
         */
        public String getIRIString(String domainElement) {
            return table.row(domainElement).values().iterator().next();
        }

        /**
         * Returns the set of domain elements.
         * @return
         */
        public Set<String> getDomainElements() {
            return table.rowKeySet();
        }

        /**
         * Returns the set of DIMACS literals.
         * @return
         */
        public Set<Integer> getDimacsLiterals() {
            return table.columnKeySet();
        }

    }

    /**
     * Creates instances of {@link OntologicalCPNet}.
     */
    public static class Builder {
        // constants
        private static final String[] DISAMBIGUATION_SUFFIXES = {"", "_pref", "_user", "_aug"};
        // required parameters for the OntologicalCPNet instance
        private CPNet baseCPNet;
        private OWLOntology augmentedOntology;
        private DomainTable domainTable;
        // optional parameters
        private SATSolver solver;
        // build parameters
        private Set<String> domainValues;
        private OWLOntology baseOntology;
        private Map<String, OWLClassExpression> definitions;

        private Builder(CPNet baseCPNet) {
            this.baseCPNet = Objects.requireNonNull(baseCPNet);
            definitions = new HashMap<>();
            domainValues = this.baseCPNet.graph.domainValues().collect(Collectors.toSet());
            domainTable = null;
            solver = null;
            baseOntology = null;
            augmentedOntology = null;
        }

        /**
         * Sets the specified ontology as the base ontology for the {@link OntologicalCPNet} being built.
         * Any changes to the ontology applied before invoking {@link #build()} will take effect.
         *
         * <p>When {@link #build()} is invoked, <code>baseOntology</code> is checked for consistency;
         * if the check fails, an {@link IllegalStateException} will be thrown.
         * @param baseOntology
         * @return
         * @throws IllegalStateException if a base ontology was already set for this builder
         */
        public Builder withOntology(OWLOntology baseOntology) {
            if (this.baseOntology != null) throw new IllegalStateException();
            this.baseOntology = Objects.requireNonNull(baseOntology);
            return this;
        }

        /**
         * Specifies a <code>SATSolver</code> implementation for the {@link OntologicalCPNet} being built.
         *
         * <p>If {@link #build()} is invoked without setting a <code>SATSolver</code>,
         * the default implementation will be used.
         * @param solver
         * @return
         */
        public Builder withSATSolver(SATSolver solver) {
            if (this.solver != null) throw new IllegalStateException();
            this.solver = Objects.requireNonNull(solver);
            return this;
        }

        /**
         * OWL class definitions are required parameters. This method must be invoked for each element
         * returned by <code>getPreferenceGraph().domainValues()</code>, otherwise {@link #build()}
         * will throw an {@link IllegalStateException}.
         * @param domainValue
         * @param definition
         * @return
         * @throws IllegalStateException if the specified <code>domainValue</code> is absent in the base CP-net
         * or already defined set this builder
         */
        public Builder addPreferenceDefinition(String domainValue, OWLClassExpression definition) {
            Objects.requireNonNull(domainValue);
            Objects.requireNonNull(definition);
            if (!domainValues.contains(domainValue)) {
                throw new IllegalStateException(String.format("Unknown domain value '%s'", domainValue));
            }
            if (definitions.putIfAbsent(domainValue, definition) != null) {
                throw new IllegalStateException(String.format("OWL class definition for '%s' already set", domainValue));
            }
            return this;
        }

        /**
         * Builds an {@link OntologicalCPNet} using the parameters set by this {@link Builder}.
         * The preference definitions, provided to this <code>Builder</code>
         * with invocations of {@link #addPreferenceDefinition(String, OWLClassExpression)},
         * are converted into OWL <em>class definition</em> axioms.
         * More specifically, for each call to
         * <pre>{@code addPreferenceDefinition(domainValue, definition)}</pre>
         * an OWL class <em>C</em> is created using <code>domainValue</code> as IRI suffix;
         * then, the OWL axiom
         * <pre>{@code EquivalentClasses(C, definition)}</pre>
         * is created.
         *
         * <p>Additionally, for each preference variable <em>A</em> in the base CP-net
         * with domain values <em>a1, a2, a3, ...</em> the OWL axiom
         * <pre>{@code DisjointUnion(owl:Thing, a1, a2, a3, ...)}</pre>
         * is created, in order to "close down the world" with respect to preferences.
         *
         * <p>The augmented ontology is created by copying the base ontology into a
         * local {@link OWLOntologyManager} and adding the new axioms.
         *
         * <p>This method fails if the invoking <code>Builder</code> is in an inconsistent state.
         * For detailed information on setting the required parameters, see the javadoc
         * of the remaining {@link Builder} methods.
         * @return
         * @throws OWLOntologyCreationException if the base ontology cannot be copied into
         * a local {@link OWLOntologyManager} to create the augmented ontology.
         * @throws IllegalStateException if the required parameters were not set properly
         */
        public OntologicalCPNet build() throws OWLOntologyCreationException {
            // Check required parameters.
            if (baseOntology == null) {
                throw new IllegalStateException("base ontology not set");
            }
            if (!domainValues.equals(definitions.keySet())) {
                throw new IllegalStateException("missing OWL definition for some domain values");
            }
            // Copy the base ontology into a local manager and check for consistency.
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLDataFactory owlDataFactory = manager.getOWLDataFactory();
            augmentedOntology = manager.copyOntology(baseOntology, OntologyCopy.SHALLOW);
            OWLReasoner reasoner = new ReasonerFactory().createNonBufferingReasoner(augmentedOntology);
            if (!reasoner.isConsistent()) {
                throw new IllegalStateException("inconsistent base ontology");
            }
            // Generate a unique IRI string for each domain value.
            String baseIRIString = augmentedOntology.getOntologyID().getOntologyIRI()
                    .orElse(manager.getOntologyDocumentIRI(augmentedOntology))
                    .getIRIString();
            Set<String> existingIRIStrings = augmentedOntology.classesInSignature()
                    .map(owlClass -> owlClass.getIRI().getIRIString())
                    .collect(Collectors.toSet());
            domainTable = new DomainTable(
                    domainValues,
                    domainValue -> Arrays.stream(DISAMBIGUATION_SUFFIXES)
                            .map(suffix -> baseIRIString + "#" + domainValue + suffix)
                            .filter(iriString -> !existingIRIStrings.contains(iriString))
                            .findFirst().orElseThrow(() -> new IllegalStateException(
                                    String.format("Unable to generate a unique IRI for domain value '%s'", domainValue))));
            // Configure the SAT solver.
            if (solver == null) {
                solver = SATSolverFactory.defaultSolver();
            }
            solver.setMaxLiteral(domainTable.getDimacsLiterals().size());
            // Build a mapping between domain values and their OWL representations.
            Map<String, OWLClass> owlDomainValues = domainValues.stream()
                    .collect(Collectors.toMap(
                            Function.identity(),
                            domainValue -> owlDataFactory.getOWLClass(domainTable.getIRIString(domainValue))));
            // Build the new class definition axioms.
            Stream<OWLEquivalentClassesAxiom> classDefinitions = domainValues.stream()
                    .map(domainValue -> owlDataFactory.getOWLEquivalentClassesAxiom(
                            owlDomainValues.get(domainValue),
                            definitions.get(domainValue)));
            // Build the new partition axioms.
            Stream<OWLDisjointUnionAxiom> partitions = baseCPNet.graph.domainMap()
                    .values().stream()
                    .map(domain -> owlDataFactory.getOWLDisjointUnionAxiom(
                            owlDataFactory.getOWLThing(),
                            domain.stream().map(owlDomainValues::get)));
            // Add the new axioms to the augmented ontology.
            if (augmentedOntology.addAxioms(Stream.concat(classDefinitions, partitions))
                    != ChangeApplied.SUCCESSFULLY) {
                throw new OWLRuntimeException("error while applying changes to the new ontology");
            }
            if (!reasoner.isConsistent()) {
                throw new IllegalStateException("inconsistent set of preferences");
            }
            // Build the OntologicalCPNet instance.
            return new OntologicalCPNet(this);
        }
    }

}
