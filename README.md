DLPreferences
=============

DLPreferences is a Java API for computing the Pareto-optimal outcomes in a semantic negotiation
between a provider and a decision maker.
The provider describes his offer by sharing an [OWL 2 DL][owl2dl] ontology, while the decision maker
expresses preferences using [OWL API][owlapi] class expressions.

DLPreferences currently supports a qualitative preference representation formalism
called *ontological CP-nets*, which expands the expressive power of propositional CP-nets
by adopting Description Logics.

A CP-net is a convenient way of representing a ranking of the possible outcomes of a negotiation.
To build a CP-net, the decision maker defines the set of features that make up an outcome,
then they rank the domain values of each feature; finally, they specify conditional preferences
on different features.
To extend a CP-net into an ontological CP-net, domain values must be specified as OWL 2 DL classes
w.r.t. the provider's OWL 2 DL ontology.

Some outcomes may be unfeasible due to constraints imposed by the shared ontology.
DLPreferences computes the Pareto-optimal outcomes, that is the feasible outcomes who are not dominated
by any other feasible outcome.
To learn more about ontological CP-nets and reasoning with qualitative preferences,
see [Di Noia et al. (2005) "Ontological CP-Nets"][dinoia_paper_doi] ([PDF][dinoia_paper_pdf]).

Support for quantitative preferences, whereby outcomes are ranked according to the *utility value*
the decision maker assigns to each preference, is planned for a future version.

## Prerequisites

* A JDK supporting Java SE 8 or superior.
* [NuSMV][nusmv] 2.5.4 or superior.

## Usage

Since ontological CP-nets are an extension of CP-nets, a CP-net must be specified first.
This is achieved by creating an XML document that follows the [CRISNER][crisner] input syntax.
A `CPNet` instance is constructed passing the CRISNER spec file and the NuSMV executable as arguments:

    CPNet baseCPNet = new CPNet(
        Paths.get("spec.xml"),
        Paths.get("nusmv-2.6.0", "bin", "NuSMV"));

The provider's ontology is created as an OWL API ontology object:

    OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    OWLOntology ontology = manager.loadOntologyFromOntologyDocument(
        Paths.get("ontology.owl").toFile());

To build an ontological CP-net, an OWL API class expression must be associated
with each domain value defined in the CRISNER spec file.
Let *R* be a feature defined in the XML spec file and *{Rs, Rm}* its domain;
the ontological CP-net is constructed as follows:

    OntologicalCPNet.Builder cpnetBuilder =
        OntologicalCPNet.builder(baseCPNet, ontology);

    OWLClassExpression rs = ...
    cpnetBuilder.addPreferenceDefinition("Rs", rs);

    OWLClassExpression rm = ...
    cpnetBuilder.addPreferenceDefinition("Rm", rm);

    OntologicalCPNet cpnet = cpnetBuilder.build();

The Pareto-optimal outcomes can now be computed:

    Set<Outcome> outcomes = cpnet.paretoOptimal();

## Running the tests

Download the latest [NuSMV][nusmv] release, rename the root directory to `nusmv` and place it
inside `src/test/resources`. The relative path of the NuSMV executable should be

    src/test/resources/nusmv/bin/NuSMV

From the root directory, run the tests with

    gradlew test

## Status

Development is in the early stages, therefore the API is subject to change without notice.

## Acknowledgements

Thanks to Dr. Ganesh Ram Santhanam for providing the [CRISNER][crisner] library.
His work is described in [Santhanam et al. (2010) "Dominance Testing via Model Checking"][crisner_paper_link]
([PDF][crisner_paper_pdf]).

## License

DLPreferences is released under the MIT license.

[owlapi]: https://github.com/owlcs/owlapi
[nusmv]: http://nusmv.fbk.eu
[crisner]: https://www.ece.iastate.edu/~gsanthan/crisner.html
[owl2dl]: https://www.w3.org/TR/owl2-syntax
[dinoia_paper_pdf]: http://sisinflab.poliba.it/publications/2014/DLMST14/URSWII2014.pdf
[dinoia_paper_doi]: https://doi.org/10.1007/978-3-319-13413-0_15
[crisner_paper_link]: https://www.aaai.org/ocs/index.php/AAAI/AAAI10/paper/view/1844
[crisner_paper_pdf]: https://www.aaai.org/ocs/index.php/AAAI/AAAI10/paper/view/1844
