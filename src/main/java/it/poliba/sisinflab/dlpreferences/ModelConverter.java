package it.poliba.sisinflab.dlpreferences;

import org.semanticweb.owlapi.rdf.rdfxml.parser.IRIProvider;

/**
 * A set of methods that enable conversion between equivalent representations of propositional variables.
 */
public interface ModelConverter extends
        DimacsProvider, VarNameProvider, IRIProvider {
}
