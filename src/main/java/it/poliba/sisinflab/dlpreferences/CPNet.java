package it.poliba.sisinflab.dlpreferences;

import it.poliba.sisinflab.dlpreferences.except.SpecFileParseException;
import it.poliba.sisinflab.dlpreferences.nusmv.NuSMVModelGenerator;
import it.poliba.sisinflab.dlpreferences.nusmv.NuSMVRunner;
import model.Outcome;
import model.PreferenceSpecification;
import translate.CPTheoryToSMVTranslator;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * A CP-net.
 */
public class CPNet {
    // The NuSMV model of the preference specifications, as a <code>List</code> of lines.
    private List<String> baseModel;
    // An object that interacts with the local NuSMV installation.
    private NuSMVRunner nusmvRunner;
    // A hierarchical structure of preference variables.
    PreferenceGraph graph;

    CPNet(CPNet n) {
        baseModel = n.baseModel;
        nusmvRunner = n.nusmvRunner;
        graph = n.graph;
    }

    /**
     * Constructs a CP-net by parsing the preference specification file <code>xmlPrefSpec</code>.
     * <p>
     * Internally, the preference specification is translated into an equivalent NuSMV model,
     * which is stored as a .smv file in the system temp directory.
     * The system temp directory is specified by the JRE property <code>java.io.tmpdir</code>.
     *
     * @param xmlPrefSpec     the preference specification XML file.
     *                        See the <a href="http://www.ece.iastate.edu/~gsanthan/crisner.html">CRISNER home page</a>
     *                        for details about the preference specification syntax.
     * @param nusmvExecutable the NuSMV executable file
     * @throws SpecFileParseException if <code>xmlPrefSpec</code> cannot be parsed successfully
     * @throws FileNotFoundException if any {@link Path} argument is invalid.
     * To be valid, arguments must be regular, readable files;
     * additionally, <code>nusmvExecutable</code> must be an executable file.
     * @throws IOException if an I/O error occurs while attempting to write in the system temp directory
     * @throws NullPointerException if any argument is <code>null</code>
     */
    public CPNet(Path xmlPrefSpec, Path nusmvExecutable)
            throws SpecFileParseException, IOException {
        // Parse the XML input file.
        PreferenceSpecification prefSpec =
                CPTheoryToSMVTranslator.parsePreferenceSpecification(
                        xmlPrefSpec.toAbsolutePath().toString());
        // Build and test the base NuSMV model.
        nusmvRunner = new NuSMVRunner(nusmvExecutable);
        baseModel = NuSMVModelGenerator.baseModel(prefSpec);
        nusmvRunner.verify(baseModel);
        // Build the preference graph.
        graph = PreferenceGraph.fromCrisnerSpec(prefSpec);
    }

    public PreferenceGraph getPreferenceGraph() {
        return graph;
    }

    /**
     * Performs a dominance check.
     *
     * @param better
     * @param worse
     * @return <code>true</code> if <code>better</code> is preferred to <code>worse</code>;
     * <code>false</code> otherwise.
     * @throws NullPointerException if any argument is <code>null</code>
     */
    boolean dominates(Outcome better, Outcome worse) {
        Objects.requireNonNull(better);
        Objects.requireNonNull(worse);
        // Check a trivial case.
        if (better.equals(worse)) {
            return false;
        }
        // Add the dominance specification to the base NuSMV model.
        Stream<String> dominanceModel = Stream.concat(
                baseModel.stream(),
                Stream.of(NuSMVModelGenerator.dominanceSpec(better, worse)));
        // Invoke NuSMV to perform the dominance query.
        return nusmvRunner.verify(dominanceModel);
    }

}