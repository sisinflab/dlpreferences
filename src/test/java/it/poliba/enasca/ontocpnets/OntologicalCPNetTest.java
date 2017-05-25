package it.poliba.enasca.ontocpnets;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Sets;
import it.poliba.enasca.ontocpnets.nusmv.NuSMVRunnerTest;
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
    private OWLOntology constrained;
    private Set<OptimalityConstraint> optimalityConstraints;
    private Set<Map<String, String>> outcomesAsMaps;

    @Factory(dataProvider = "factoryProvider")
    public OntologicalCPNetTest(Path xmlPrefSpec, Path baseOntologyPath, Path constrainedOntologyPath,
                                ImmutableSetMultimap<String, String> preferenceVariables,
                                Stream<OptimalityConstraint> optimalityStream,
                                Stream<Map<String, String>> outcomesAsMaps)
            throws Exception {
        // Load the base ontology.
        OWLOntology baseOntology = OWLManager.createOWLOntologyManager()
                .loadOntologyFromOntologyDocument(baseOntologyPath.toFile());
        // Load the constrained ontology with a different manager to avoid conflicts.
        OWLOntology constrainedOntology = OWLManager.createOWLOntologyManager()
                .loadOntologyFromOntologyDocument(constrainedOntologyPath.toFile());
        // Build the mapping that will be converted into class definition axioms.
        Map<String, OWLClassExpression> preferences =
                collectOntologicalPreferences(constrainedOntology, preferenceVariables.values().stream());
        // Build the current OntologicalCPNet instance.
        CPNet baseCPNet = new CPNet(xmlPrefSpec, Paths.get(NuSMVRunnerTest.NUSMV_EXECUTABLE.toURI()));
        OntologicalCPNet.Builder cpnetBuilder = OntologicalCPNet.builder(baseCPNet, baseOntology);
        for (Map.Entry<String, OWLClassExpression> preferenceEntry : preferences.entrySet()) {
            cpnetBuilder.addPreferenceDefinition(preferenceEntry.getKey(), preferenceEntry.getValue());
        }
        this.cpnet = cpnetBuilder.build();
        this.preferenceVariables = preferenceVariables;
        this.constrained = constrainedOntology;
        this.optimalityConstraints = optimalityStream.collect(Collectors.toSet());
        this.outcomesAsMaps = outcomesAsMaps.collect(Collectors.toSet());
    }

    @DataProvider
    public static Object[][] factoryProvider() throws Exception {
        // The XML preference specification file.
        Path hotelXMLPrefSpec = Paths.get(CPNetTest.class.getResource("/hotel_preferences.xml").toURI());
        // The base ontology.
        Path hotelBaseOntology = Paths.get(CPNetTest.class.getResource("/hotel_ontology.owl").toURI());
        // The "constrained" ontology is constructed by adding preference definitions to the base ontology.
        Path hotelConstrainedOntology = Paths.get(CPNetTest.class.getResource("/hotel_ontology_constrained.owl").toURI());
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
        // The set of optimal outcomes.
        Stream<Map<String, String>> hotelOutcomes = Stream.<Map<String, String>>builder()
                .add(ImmutableMap.<String, String>builder()
                        .put("R", "Rl").put("W", "Wy").put("B", "Bn").put("C", "Cy").put("P", "Pl").build())
                .build();
        return new Object[][]{
                {hotelXMLPrefSpec, hotelBaseOntology, hotelConstrainedOntology,
                        hotelPreferenceVariables, hotelOptimalityStream, hotelOutcomes}
        };
    }

    @Test
    public void testConstrainedOntology() throws Exception {
        // Uncomment this line to save the ontology in the default temp directory
        //cpnet.ontology.saveOntology(new RDFXMLDocumentFormat(), Files.newOutputStream(Files.createTempFile("ontology", ".owl")));
        cpnet.ontology.logicalAxioms().forEach(
                axiom -> Assert.assertTrue(constrained.containsAxiomIgnoreAnnotations(axiom)));
        Assert.assertEquals(cpnet.ontology.getLogicalAxiomCount(), constrained.getLogicalAxiomCount());
    }

    @Test
    public void testPreferenceVariables() throws Exception {
        Map<String, Collection<String>> domainMap = preferenceVariables.asMap();
        Map<String, Set<String>> cpnetDomainMap = cpnet.getPreferenceGraph().domainMap();
        Assert.assertEquals(cpnetDomainMap, domainMap,
                TestUtils.reportSetDifference(cpnetDomainMap.entrySet(), domainMap.entrySet()));
    }

    @Test
    public void testComputeOptimum() throws Exception {
        ConstraintSet<OptimalityConstraint> cpnetOptimumSet =
                cpnet.getOptimumSet();
        ConstraintSet<OptimalityConstraint> optimumSet =
                cpnet.toConstraintSet(this.optimalityConstraints);
        Assert.assertEquals(cpnetOptimumSet, optimumSet,
                TestUtils.reportSetDifference(cpnetOptimumSet, optimumSet));
    }

    /**
     * Checks whether the axioms in the ontological closure satisfy the condition of minimal clause,
     * that is for each axiom
     * <pre>{@code SubClassOf(owl:Thing, ObjectUnionOf(X, Y, ...)) }</pre>
     * there exists no proper subset <em>Q</em> of <em>{X, Y, ...}</em> such that
     * <em>Q</em> is entailed by the constrained ontology.
     * @throws Exception
     */
    @Test
    public void testComputeClosure() throws Exception {
        ConstraintSet<FeasibilityConstraint> cpnetClosure =
                cpnet.getClosure();
        OWLDataFactory df = OWLManager.createConcurrentOWLOntologyManager().getOWLDataFactory();
        cpnetClosure.axioms().parallel()
                // Retrieve the set of disjunct OWL classes from the current covering axiom.
                .map(axiom -> axiom.getSubClass().asDisjunctSet())
                // Generate all proper, non-empty subsets.
                .map(clause -> Sets.difference(
                        Sets.powerSet(clause),
                        ImmutableSet.<Set<OWLClassExpression>>of(Collections.emptySet(), clause)))
                .flatMap(Collection::parallelStream)
                // Create a covering axiom from the current subset.
                .map(subset -> df.getOWLSubClassOfAxiom(
                        df.getOWLThing(),
                        df.getOWLObjectUnionOf(subset)))
                // Assert that the new covering axiom is not entailed by the constrained ontology.
                .forEach(axiom -> Assert.assertFalse(
                        cpnet.applyService(reasoner -> reasoner.isEntailed(axiom)),
                        String.format("the axiom %s is entailed by the constrained ontology", axiom)));
    }

    @Test
    public void testHardPareto() throws Exception {
        Set<Map<String, String>> hardParetoOutcomes =
                cpnet.paretoOptimal().stream()
                        .map(Outcome::getOutcomeAsValuationMap)
                        .collect(Collectors.toSet());
        Assert.assertEquals(hardParetoOutcomes, outcomesAsMaps,
                TestUtils.reportSetDifference(hardParetoOutcomes, outcomesAsMaps));
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