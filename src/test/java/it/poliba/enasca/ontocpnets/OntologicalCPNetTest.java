package it.poliba.enasca.ontocpnets;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Sets;
import it.poliba.enasca.ontocpnets.util.NuSMVRunnerTest;
import model.Outcome;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Tests for {@link OntologicalCPNet}.
 */
@Test(dependsOnGroups = {"plainCPnet"})
public class OntologicalCPNetTest {
    private OntologicalCPNet cpnet;
    private ImmutableSetMultimap<String, String> preferenceVariables;
    private OWLOntology augmented;
    private Set<OptimalityConstraint> optimalityConstraints;
    private Set<FeasibilityConstraint> closure;
    private Set<Map<String, String>> outcomesAsMaps;

    @Factory(dataProvider = "factoryProvider")
    public OntologicalCPNetTest(Path xmlPrefSpec, Path baseOntologyPath, Path augmentedOntologyPath,
                                ImmutableSetMultimap<String, String> preferenceVariables,
                                Stream<OptimalityConstraint> optimalityStream,
                                Stream<FeasibilityConstraint> closure,
                                Stream<Map<String, String>> outcomesAsMaps)
            throws Exception {
        // Load the base ontology.
        OWLOntology baseOntology = OWLManager.createOWLOntologyManager()
                .loadOntologyFromOntologyDocument(baseOntologyPath.toFile());
        // Load the augmented ontology with a different manager to avoid conflicts.
        OWLOntology augmentedOntology = OWLManager.createOWLOntologyManager()
                .loadOntologyFromOntologyDocument(augmentedOntologyPath.toFile());
        // Build the mapping that will be converted into class definition axioms.
        Map<String, OWLClassExpression> preferences =
                collectOntologicalPreferences(augmentedOntology, preferenceVariables.values().stream());
        // Build the current OntologicalCPNet instance.
        CPNet baseCPNet = new CPNet(xmlPrefSpec, Paths.get(NuSMVRunnerTest.NUSMV_EXECUTABLE.toURI()));
        OntologicalCPNet.Builder cpnetBuilder = OntologicalCPNet.builder(baseCPNet, baseOntology);
        for (Map.Entry<String, OWLClassExpression> preferenceEntry : preferences.entrySet()) {
            cpnetBuilder.addPreferenceDefinition(preferenceEntry.getKey(), preferenceEntry.getValue());
        }
        this.cpnet = cpnetBuilder.build();
        this.preferenceVariables = preferenceVariables;
        this.augmented = augmentedOntology;
        this.optimalityConstraints = optimalityStream.collect(Collectors.toSet());
        this.closure = closure.collect(Collectors.toSet());
        this.outcomesAsMaps = outcomesAsMaps.collect(Collectors.toSet());
    }

    @DataProvider
    public static Object[][] factoryProvider() throws Exception {
        // The XML preference specification file.
        Path hotelXMLPrefSpec = Paths.get(CPNetTest.class.getResource("/hotel_preferences.xml").toURI());
        // The base ontology.
        Path hotelBaseOntology = Paths.get(CPNetTest.class.getResource("/hotel_ontology.owl").toURI());
        // The "augmented" ontology is constructed by adding preference definitions to the base ontology.
        Path hotelAugmentedOntology = Paths.get(CPNetTest.class.getResource("/hotel_ontology_augmented.owl").toURI());
        // The set of preference variables and domain values that are expected to be read from the XML spec file.
        ImmutableSetMultimap<String, String> hotelPreferenceVariables =
                ImmutableSetMultimap.<String, String>builder()
                        .putAll("W", "Wy", "Wn")
                        .putAll("R", "Rl", "Rm", "Rs")
                        .putAll("B", "Bo", "Bn")
                        .putAll("C", "Cy", "Cn")
                        .putAll("P", "Pl", "Ps")
                        .build();
        // The optimality constraints.
        Stream<OptimalityConstraint> hotelOptimalityStream = Stream.<OptimalityConstraint>builder()
                .add(OptimalityConstraint.builder()
                        .addToClause("Rl")
                        .build())
                .add(OptimalityConstraint.builder()
                        .addToClause("Wy")
                        .build())
                .add(OptimalityConstraint.builder()
                        .addToCondition("Rs")
                        .addToClause("Bo")
                        .build())
                .add(OptimalityConstraint.builder()
                        .addToCondition("Rm")
                        .addToClause("Bn")
                        .build())
                .add(OptimalityConstraint.builder()
                        .addToCondition("Rl")
                        .addToClause("Bn")
                        .build())
                .add(OptimalityConstraint.builder()
                        .addToClause("Cy")
                        .build())
                .add(OptimalityConstraint.builder()
                        .addToCondition("Bn", "Cy")
                        .addToClause("Pl")
                        .build())
                .add(OptimalityConstraint.builder()
                        .addToCondition("Bo", "Cy")
                        .addToClause("Ps")
                        .build())
                .add(OptimalityConstraint.builder()
                        .addToCondition("Bn", "Cn")
                        .addToClause("Ps")
                        .build())
                .add(OptimalityConstraint.builder()
                        .addToCondition("Bo", "Cn")
                        .addToClause("Ps")
                        .build())
                .build();
        // The ontological closure.
        Stream<FeasibilityConstraint> hotelClosure = Stream.<FeasibilityConstraint>builder()
                .add(FeasibilityConstraint.builder()
                        .addPositive("Cy")
                        .addNegated("Wy")
                        .build())
                .add(FeasibilityConstraint.builder()
                        .addNegated("Rs", "Rm")
                        .build())
                .add(FeasibilityConstraint.builder()
                        .addNegated("Rm", "Rl")
                        .build())
                .add(FeasibilityConstraint.builder()
                        .addNegated("Rs", "Rl")
                        .build())
                .add(FeasibilityConstraint.builder()
                        .addPositive("Rs", "Rm", "Rl")
                        .build())
                .add(FeasibilityConstraint.builder()
                        .addNegated("Wy", "Wn")
                        .build())
                .add(FeasibilityConstraint.builder()
                        .addPositive("Wy", "Wn")
                        .build())
                .add(FeasibilityConstraint.builder()
                        .addNegated("Bo", "Bn")
                        .build())
                .add(FeasibilityConstraint.builder()
                        .addPositive("Bo", "Bn")
                        .build())
                .add(FeasibilityConstraint.builder()
                        .addNegated("Cy", "Cn")
                        .build())
                .add(FeasibilityConstraint.builder()
                        .addPositive("Cy", "Cn")
                        .build())
                .add(FeasibilityConstraint.builder()
                        .addNegated("Pl", "Ps")
                        .build())
                .add(FeasibilityConstraint.builder()
                        .addPositive("Pl", "Ps")
                        .build())
                .build();
        // The set of optimal outcomes.
        Stream<Map<String, String>> hotelOutcomes = Stream.<Map<String, String>>builder()
                .add(ImmutableMap.<String, String>builder()
                        .put("R", "Rl").put("W", "Wy").put("B", "Bn").put("C", "Cy").put("P", "Pl").build())
                .build();
        return new Object[][]{
                {hotelXMLPrefSpec, hotelBaseOntology, hotelAugmentedOntology,
                        hotelPreferenceVariables, hotelOptimalityStream, hotelClosure, hotelOutcomes}
        };
    }

    @Test
    public void testAugmentedOntology() throws Exception {
        // Uncomment this line to save the ontology in the default temp directory
        //cpnet.ontology.saveOntology(new RDFXMLDocumentFormat(), Files.newOutputStream(Files.createTempFile("ontology", ".owl")));
        cpnet.ontology.logicalAxioms().forEach(
                axiom -> Assert.assertTrue(augmented.containsAxiomIgnoreAnnotations(axiom)));
        Assert.assertEquals(cpnet.ontology.getLogicalAxiomCount(), augmented.getLogicalAxiomCount());
    }

    @Test
    public void testPreferenceVariables() throws Exception {
        Map<String, Collection<String>> domainMap = preferenceVariables.asMap();
        Map<String, Set<String>> cpnetDomainMap = cpnet.getPreferenceGraph().domainMap();
        Assert.assertEquals(cpnetDomainMap, domainMap,
                Utils.reportSetDifference(cpnetDomainMap.entrySet(), domainMap.entrySet()));
    }

    @Test
    public void testComputeOptimum() throws Exception {
        Set<? extends Constraint> cpnetOptimalityConstraints =
                cpnet.getOptimalityConstraints().constraintSet;
        Assert.assertEquals(cpnetOptimalityConstraints, optimalityConstraints,
                Utils.reportSetDifference(cpnetOptimalityConstraints, optimalityConstraints));
    }

    /**
     * Checks whether the axioms in the ontological closure satisfy the condition of minimal clause,
     * that is for each axiom
     * <pre>{@code SubClassOf(owl:Thing, ObjectUnionOf(X, Y, ...)) }</pre>
     * there exists no strict subset <em>Q</em> of <em>{X, Y, ...}</em> such that
     * <em>Q</em> is entailed by the augmented ontology.
     * @throws Exception
     */
    @Test
    public void testComputeClosure() throws Exception {
        OntologicalConstraints closure = cpnet.getFeasibilityConstraints();
        OWLDataFactory df = OWLManager.createConcurrentOWLOntologyManager().getOWLDataFactory();
        closure.axioms().parallel()
                // Retrieve the set of disjunct OWL classes from the current covering axiom.
                .map(axiom -> axiom.getSubClass().asDisjunctSet())
                // Compute the powerset, excluding the empty set and the original set.
                .map(clause -> Sets.difference(
                        Sets.powerSet(clause),
                        ImmutableSet.<Set<OWLClassExpression>>of(Collections.emptySet(), clause)))
                .flatMap(Collection::parallelStream)
                // Create a covering axiom from the current subset.
                .map(subset -> df.getOWLSubClassOfAxiom(
                        df.getOWLThing(),
                        df.getOWLObjectUnionOf(subset)))
                // Assert that the new covering axiom is not entailed by the augmented ontology.
                .forEach(axiom -> Assert.assertFalse(
                        cpnet.applyService(reasoner -> reasoner.isEntailed(axiom)),
                        String.format("the axiom %s is entailed by the augmented ontology", axiom)));
    }

    @Test
    public void testHardPareto() throws Exception {
        Set<Map<String, String>> hardParetoOutcomes =
                cpnet.paretoOptimal().stream()
                        .map(Outcome::getOutcomeAsValuationMap)
                        .collect(Collectors.toSet());
        Assert.assertEquals(hardParetoOutcomes, outcomesAsMaps,
                Utils.reportSetDifference(hardParetoOutcomes, outcomesAsMaps));
    }

    /**
     * Searches the specified ontology for axioms of the form
     * <pre>{@code EquivalentClasses(C, CE)}</pre>
     * where <i>C</i> is an OWL class whose name is an element of <code>OWLClassNames</code>,
     * and <i>CE</i> is a class expression.
     * The returned map associates elements of <code>OWLClassNames</code> with the corresponding class expression.
     * @param ontology
     * @param OWLClassNames
     * @return
     */
    private static Map<String, OWLClassExpression> collectOntologicalPreferences(
            OWLOntology ontology, Stream<String> OWLClassNames) {
        String ontologyIRIString = ontology.getOntologyID()
                .getOntologyIRI().orElseThrow(RuntimeException::new).getIRIString();
        OWLDataFactory dataFactory = ontology.getOWLOntologyManager().getOWLDataFactory();
        return OWLClassNames.collect(Collectors.toMap(
                UnaryOperator.identity(),
                domainValue -> {
                    OWLClass domainValueClass = dataFactory.getOWLClass(
                            IRI.create(ontologyIRIString + "#" + domainValue));
                    OWLEquivalentClassesAxiom equivalentClassesAxiom =
                            ontology.equivalentClassesAxioms(domainValueClass).findAny().orElseThrow(RuntimeException::new);
                    return equivalentClassesAxiom.getClassExpressionsMinus(domainValueClass).iterator().next();
                }));
    }

}