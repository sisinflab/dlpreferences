package it.poliba.enasca.ontocpnets;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSetMultimap;
import exception.PreferenceReasonerException;
import it.poliba.enasca.ontocpnets.sat.SATSolverFactory;
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
    private Set<OptimalityConstraint> optimumSet;
    private Set<FeasibilityConstraint> closure;
    private Set<Outcome> outcomes;

    @Factory(dataProvider = "factoryProvider")
    public OntologicalCPNetTest(Path xmlPrefSpec, Path baseOntologyPath, Path augmentedOntologyPath,
                                ImmutableSetMultimap<String, String> preferenceVariables,
                                Stream<OptimalityConstraint> optimum,
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
        OntologicalCPNet.Builder builder = OntologicalCPNet.builder(baseCPNet);
        builder.withOntology(baseOntology);
        for (Map.Entry<String, OWLClassExpression> preferenceEntry : preferences.entrySet()) {
            builder.addPreferenceDefinition(preferenceEntry.getKey(), preferenceEntry.getValue());
        }
        this.cpnet = builder.build();
        this.preferenceVariables = preferenceVariables;
        this.augmented = augmentedOntology;
        this.optimumSet = optimum.collect(Collectors.toSet());
        this.closure = closure.collect(Collectors.toSet());
        this.outcomes = outcomesAsMaps.map(assignments -> {
            try {
                Outcome o = new Outcome(assignments);
                o.validateOutcome();
                return o;
            } catch (PreferenceReasonerException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toSet());
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
        Stream<OptimalityConstraint> hotelOptimum = Stream.<OptimalityConstraint>builder()
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
                        hotelPreferenceVariables, hotelOptimum, hotelClosure, hotelOutcomes}
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
        Assert.assertEquals(cpnet.getPreferenceGraph().domainMap(), preferenceVariables.asMap());
    }

    @Test
    public void testComputeOptimum() throws Exception {
        Assert.assertEquals(cpnet.getPreferenceGraph().getOptimumSet(), optimumSet);
    }

    @Test
    public void testComputeClosure() throws Exception {
        Assert.assertEquals(cpnet.getClosure(), closure);
    }

    @Test
    public void testHardPareto() throws Exception {
        // Since the class Outcome does not implement toString(), an explicit conversion is needed.
        Set<Map<String, String>> hardParetoOutcomes = cpnet.hardPareto(SATSolverFactory.defaultSolver())
                .stream()
                .map(Outcome::getOutcomeAsValuationMap)
                .collect(Collectors.toSet());
        Set<Map<String, String>> expectedOutcomes = this.outcomes
                .stream()
                .map(Outcome::getOutcomeAsValuationMap)
                .collect(Collectors.toSet());
        Assert.assertEquals(hardParetoOutcomes, expectedOutcomes);
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