package it.poliba.sisinflab.dlpreferences;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import model.Outcome;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.io.File;
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
    private OWLOntology constrained;

    @Parameters({"nusmv-path", "hotel-cpnet-resource",
                 "hotel-base-ontology-resource", "hotel-constrained-ontology-resource"})
    public OntologicalCPNetTest(@Optional("") String nusmvPathRes, String xmlSpecRes,
                                String baseOntologyRes, String constrainedOntologyRes)
            throws Exception {
        // Load the base ontology.
        File baseOntologyFile = new File(OntologicalCPNet.class.getResource(baseOntologyRes).toURI());
        OWLOntology baseOntology = OWLManager.createOWLOntologyManager()
                .loadOntologyFromOntologyDocument(baseOntologyFile);
        // Load the constrained ontology with a different manager to avoid conflicts.
        File constrainedOntologyFile = new File(OntologicalCPNet.class.getResource(constrainedOntologyRes).toURI());
        OWLOntology constrainedOntology = OWLManager.createOWLOntologyManager()
                .loadOntologyFromOntologyDocument(constrainedOntologyFile);
        // Build the mapping that will be converted into class definition axioms.
        Path nusmvPath = Paths.get(nusmvPathRes);
        Path xmlSpec = Paths.get(OntologicalCPNet.class.getResource(xmlSpecRes).toURI());
        CPNet baseCPNet = new CPNet(xmlSpec, nusmvPath);
        Stream<String> domainValues = baseCPNet.getPreferenceGraph().domainValues();
        Map<String, OWLClassExpression> preferences =
                collectOntologicalPreferences(constrainedOntology, domainValues);
        // Build the OntologicalCPNet instance.
        OntologicalCPNet.Builder cpnetBuilder = OntologicalCPNet.builder(baseCPNet, baseOntology);
        for (Map.Entry<String, OWLClassExpression> preferenceEntry : preferences.entrySet()) {
            cpnetBuilder.addPreferenceDefinition(preferenceEntry.getKey(), preferenceEntry.getValue());
        }
        this.cpnet = cpnetBuilder.build();
        this.constrained = constrainedOntology;
    }

    public void testConstrainedOntology() throws Exception {
        cpnet.ontology.logicalAxioms().forEach(
                axiom -> Assert.assertTrue(constrained.containsAxiomIgnoreAnnotations(axiom)));
        Assert.assertEquals(cpnet.ontology.getLogicalAxiomCount(), constrained.getLogicalAxiomCount());
    }

    @Test(dataProvider = "optimalityConstraintProvider")
    public void testComputeOptimum(Set<OptimalityConstraint> optimalityConstraints) throws Exception {
        ConstraintSet<OptimalityConstraint> cpnetOptimumSet = cpnet.getOptimumSet();
        ConstraintSet<OptimalityConstraint> optimumSet = cpnet.toConstraintSet(optimalityConstraints);
        Assert.assertEquals(cpnetOptimumSet, optimumSet,
                TestUtils.reportSetDifference(cpnetOptimumSet, optimumSet));
    }

    @DataProvider
    public Object[][] optimalityConstraintProvider() {
        Set<OptimalityConstraint> optimalityConstraints = Stream.<OptimalityConstraint>builder()
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
                .build().collect(Collectors.toSet());
        return new Object[][]{
                {optimalityConstraints}
        };
    }

    /**
     * Checks whether the axioms in the ontological closure satisfy the condition of minimal clause,
     * that is for each axiom
     * <pre>{@code SubClassOf(owl:Thing, ObjectUnionOf(X, Y, ...)) }</pre>
     * there exists no proper subset <em>Q</em> of <em>{X, Y, ...}</em> such that
     * <em>Q</em> is entailed by the constrained ontology.
     * @throws Exception
     */
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

    @Test(dataProvider = "paretoOutcomeProvider")
    public void testHardPareto(Set<Map<String, String>> outcomesAsMaps) throws Exception {
        Set<Outcome> cpnetOutcomes = cpnet.paretoOptimal();
        Set<Map<String, String>> cpnetOutcomesAsMaps = cpnetOutcomes.stream()
                .map(Outcome::getOutcomeAsValuationMap)
                .collect(Collectors.toSet());
        Assert.assertEquals(cpnetOutcomesAsMaps, outcomesAsMaps,
                TestUtils.reportSetDifference(cpnetOutcomesAsMaps, outcomesAsMaps));
    }

    @DataProvider
    public Object[][] paretoOutcomeProvider() {
        Set<Map<String, String>> outcomesAsMaps = Stream.<Map<String, String>>builder()
                .add(ImmutableMap.<String, String>builder()
                        .put("R", "Rl")
                        .put("W", "Wy")
                        .put("B", "Bn")
                        .put("C", "Cy")
                        .put("P", "Pl")
                        .build())
                .build().collect(Collectors.toSet());
        return new Object[][]{
                {outcomesAsMaps}
        };
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