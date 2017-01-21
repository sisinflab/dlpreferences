Ontological CP-nets
===================

## Specifying preferences

Ontological concepts representing preferences are added to the base ontology using _class definitions_, that is, axioms of the form

    EquivalentClasses(C, CE)

where `C` is a class and `CE` is a class expression.
The string representation of the IRI of each OWL class `C` must be structured as follows:

    <ontology IRI> '#' <val>

where `<val>` uniquely matches a domain value in the XML specification file.

## Tests

The test resources in `src/test/resources` model the ongoing example _Conference Organization_ from
[Di Noia et al. "Ontological CP-Nets", (2014)][dinoia_ontological_2014].

The OWL file `hotel_ontology.owl` models the TBox in _Example 1_.
The ontology defines additional restrictions using common modelling patterns, namely _closure axioms_ and _covering axioms_
(see the slides [OWL, FOL and Patterns][owl_patterns]).

The preference specification file `hotel_preferences.xml` models the CP-net in _Example 2_.

The OWL file `hotel_preferences.owl` models the class definitions for ontological preferences in _Example 3_,
following the pattern below.
 * The base ontology `hotel_ontology.owl` is imported into the current ontology.
 * The domain of each variable consists of disjoint classes that form a covering axiom for the _Hotel_ class.

## The `LogicalSortedForest` class

A `LogicalSortedForest` is governed by a `com.google.common.collect.ImmutableBiMap`, which establishes an ordered,
bidirectional association between logical elements and their respective complement.
The forest may only generate nodes from the `ImmutableBiMap`, in the order they are encountered.
Internally, the map is stored as a list of its entries.

The forest also stores an iterator on the list of entries.
Since node generation is sequential, the `LogicalSortedForest.expand()` method can perform efficiently as follows:

 1. Advance the iterator until an element matching the head of the fringe queue is found.
 2. Generate the list of successors by collecting the remaining elements from the list of entries.
    During this process, the iterator is not modified.

When the iterator reaches the end, it restarts.
This unidirectional loop behavior is justified by the fact that the fringe nodes are likely to be in very close succession.

## Computing the optimality constraints

This process requires traversing the list of `PreferenceStatement`s twice.
During the first traversal, the hierarchical relationships between nodes are collected in a `SetMultimap<String, String>`,
where keys represent nodes and values represent sets of parent nodes.
During the second traversal, the sets of parent nodes are converted into sets of
`OptimalityConstraint`s representing _conditional preference tables_ (CPTs).

An `OptimalityConstraint` object contains:

 * a `Set<String>` representing the condition as an intersection of domain values;
 * a `Set<String>` representing the union of preferred domain values.

A node _a_ in the CP-net with parents _p1, p2, ..., pn_, each having _D1, D2, ..., Dn_ elements in their respective domains,
stores a CPT of size

    D1 * D2 * ... * Dn

which is the number of partial assignments that _p1, p2, ..., pn_ can form.

The condition sets of the CPT of a node are pre-populated at construction time by computing every assignment for _p1, p2, ..., pn_
(the method `com.google.common.collect.Sets.cartesianProduct` takes care of this).
Then, the sets of preferred values are filled by performing the following operations for each `PreferenceStatement` object from CRISNER:

 1. retrieve the CPT of the node `variableName`;
 2. filter out the table entries whose condition set is _not_ a superset of `parentAssignments` (using the method `Set.containsAll`);
 3. for each remaining entry, initialize the set of preferred values to `intravarPreferences.get(0)`.

## Computing the ontological closure

The data structure for the computation of the ontological closure is a forest.
More specifically, it is the set of subtrees that would be generated as successors of the dummy root node.

## Implementation notes

 *  The implementation of the constructor

        it.poliba.enasca.ontocpnets.CPNet.CPNet(Path)

    was borrowed from the XML->SMV translation method

        translate.CPTheoryToSMVTranslator.convertToSMV(String, test.CPTheoryDominanceExperimentDriver.REASONING_TASK, int)

 *  The implementation of the dominance query via the method

        it.poliba.enasca.ontocpnets.CPNet.dominates(model.Outcome, model.Outcome)

    was borrowed from

        reasoner.CyclicPreferenceReasoner.dominates(model.Outcome, model.Outcome)

 *  The implementation of
 
        it.poliba.enasca.ontocpnets.CPNet.smvDominanceSpec(Outcome, Outcome)

    was borrowed from

        reasoner.CyclicPreferenceReasoner.getDominanceSpec(Outcome, Outcome)

## Performance notes

### No parallelization when computing the ontological closure

The computation of the ontological closure has a few notable constraints:

 *  If the fringe is split between a pool of worker threads, the descent must be synchronized:
    each thread should expand one level at a time, then wait for the other threads to do the same.
 *  To guarantee the correctness of the result, every access to the closure data structure must be atomic, including read operations.

Therefore, parallel computation would not lead to noticeable improvements.

## Future improvements

### Hard-Pareto algorithm

CRISNER uses a model checker to perform dominance queries.
The query `dominates(a, b)` is converted into the model checking query

    b ⇒ EF a

It should be possible to answer multiple queries of the form

    !dominates(a1, b) && !dominates(a2, b) && ...

with a single model checking query:

    b ⇒ ¬EF(a1 ∨ a2 ∨ ...)

The HARD-Pareto algorithm makes extensive use of these queries.

## Tools

 * [IntelliJ IDEA][idea]
 * [Protégé Desktop][protege]
 * [Papyrus][papyrus]

## Libraries

 * [CRISNER][crisner]
 * [SAT4J][sat4j]
 * [OWLAPI][owlapi]
 * [Guava: Google Core Libraries for Java][guava]

 [idea]: https://www.jetbrains.com/idea/
 [protege]: http://protege.stanford.edu/products.php#desktop-protege
 [papyrus]: https://eclipse.org/papyrus/
 [crisner]: http://home.engineering.iastate.edu/~gsanthan/software.html
 [sat4j]: http://www.sat4j.org/
 [owlapi]: https://github.com/owlcs/owlapi
 [guava]: https://github.com/google/guava

 [dinoia_ontological_2014]: http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.724.6861&rep=rep1&type=pdf
 [owl_patterns]: http://studentnet.cs.manchester.ac.uk/pgt/2016/COMP62342/slides/Week3-OWL-FOL-Patterns.pdf