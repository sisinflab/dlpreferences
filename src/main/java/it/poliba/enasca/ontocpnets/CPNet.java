package it.poliba.enasca.ontocpnets;

import com.google.common.collect.ListMultimap;
import exception.PreferenceReasonerException;
import it.poliba.enasca.ontocpnets.except.NuSMVExitStatusException;
import it.poliba.enasca.ontocpnets.except.NuSMVRuntimeException;
import it.poliba.enasca.ontocpnets.except.SpecFileParseException;
import it.poliba.enasca.ontocpnets.util.NuSMVRunner;
import model.Outcome;
import model.PreferenceMetaData;
import model.PreferenceSpecification;
import model.WorkingPreferenceModel;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import test.CPTheoryDominanceExperimentDriver;
import translate.CPTheoryToSMVTranslator;
import util.Constants;
import util.XPathUtil;
import verify.SpecHelper;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A CP-net.
 */
public class CPNet {
    public static final String MODEL_CHECKER_NAME = "NuSMV";
    private static final String SMV_FILE_SUFFIX = ".smv";
    private static final String SMV_TEMPFILE_PREFIX = "nsumv";

    // The NuSMV model of the preference specifications.
    private Path smvModel;
    // The NuSMV binary.
    private Path nusmvExec;
    // A hierarchical structure of preference variables.
    protected PreferenceGraph graph;

    CPNet(CPNet n) {
        smvModel = n.smvModel;
        nusmvExec = n.nusmvExec;
        graph = n.graph;
    }

    /**
     * Constructs a CP-net by parsing the preference specification file <code>xmlPrefSpec</code>.
     * <p>
     * The specification is translated into an equivalent NuSMV model, which is saved
     * as a .smv file in the default temporary directory. The default temporary-file directory is specified
     * by the system property <code>java.io.tmpdir</code>.
     *
     * @param xmlPrefSpec     the preference specification XML file.
     *                        See the <a href="http://www.ece.iastate.edu/~gsanthan/crisner.html">CRISNER home page</a>
     *                        for details about the preference specification syntax.
     * @param nusmvExecutable the path to the NuSMV binary
     * @throws FileNotFoundException if any {@link Path} argument is invalid
     * To be valid, the arguments must be regular files. Additionally, <code>nusmvExecutable</code> must be an executable file
     * and <code>xmlPrefSpec</code> must be a readable file.
     * @throws SpecFileParseException if <code>xmlPrefSpec</code> could not be parsed successfully
     * @throws IOException            if an I/O error occurred while attempting to encode <code>xmlPrefSpec</code>
     *                                into a NuSMV model
     * @throws NullPointerException if any argument is <code>null</code>
     */
    public CPNet(Path xmlPrefSpec, Path nusmvExecutable)
            throws SpecFileParseException, IOException {
        if (!Files.isExecutable(nusmvExecutable)) {
            throw new FileNotFoundException();
        }
        if (!Files.isRegularFile(xmlPrefSpec) || !Files.isReadable(xmlPrefSpec)) {
            throw new FileNotFoundException();
        }
        // Declare support for NuSMV only.
        Constants.CURRENT_MODEL_CHECKER = Constants.MODEL_CHECKER.NuSMV;
        // Parse the XML input file.
        PreferenceSpecification prefSpec =
                CPTheoryToSMVTranslator.parsePreferenceSpecification(xmlPrefSpec.toString());
        prefSpec.makeValid();
        // Build the NuSMV model and save it in a temporary file.
        smvModel = createSMVModel(prefSpec);
        WorkingPreferenceModel.setPrefMetaData(new PreferenceMetaData(smvModel.toString()));
        // Build the preference graph.
        graph = PreferenceGraph.fromCrisnerSpec(prefSpec);
        // Verify that the local NuSMV installation works.
        if (!NuSMVRunner.run(nusmvExecutable, smvModel, NuSMVRunner::sanityCheck)) {
            throw new NuSMVRuntimeException(
                    new String[]{nusmvExecutable.toString(), smvModel.toString()},
                    new RuntimeException("check your NuSMV installation"));
        }
        nusmvExec = nusmvExecutable.toAbsolutePath();
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
     * @throws PreferenceReasonerException if the input parameters are not consistent with this CP-net
     * @throws IOException                 if an I/O error occurs while attempting to write the .smv query file
     *                                     in the default temporary directory
     * @throws NuSMVExitStatusException    if the NuSMV process exited with a non-zero status code
     * @throws NullPointerException if any argument is <code>null</code>
     */
    public boolean dominates(Outcome better, Outcome worse)
            throws PreferenceReasonerException, IOException {
        // Check trivial cases.
        if (!better.validateOutcome() || !worse.validateOutcome() || better.equals(worse)) {
            return false;
        }
        // Build the NuSMV query file and save it in the default temporary directory.
        String dominanceSpec = smvDominanceSpec(better, worse);
        Path smvQuery = Files.createTempFile(SMV_TEMPFILE_PREFIX, SMV_FILE_SUFFIX);
        OutputStream smvOutputStream = Files.newOutputStream(smvQuery);
        Files.copy(this.smvModel, smvOutputStream);
        try (PrintWriter smvWriter = new PrintWriter(smvOutputStream)) {
            smvWriter.println();
            smvWriter.println(dominanceSpec);
        }
        // Invoke NuSMV to perform the dominance query.
        boolean result = NuSMVRunner.run(this.nusmvExec, smvQuery, (outputReader, errorReader) -> {
            // Retrieve the line that matches a specific pattern (at most one line is expected).
            Pattern regex = Pattern.compile("specification.*is\\s*(?:true)|(?:false)$", Pattern.CASE_INSENSITIVE);
            String line = outputReader.lines()
                    .filter(regex.asPredicate())
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("the output of NuSMV does not match a known pattern"));
            // Parse the matching line.
            return line.toLowerCase().endsWith("true");
        });
        // Delete the temporary file.
        Files.deleteIfExists(smvQuery);
        return result;
    }

    /**
     * Translates the preference specifications gathered by CRISNER into a NuSMV model for dominance queries.
     * The resulting model is saved in a .smv file in the default temporary directory,
     * which is specified by the system property <code>java.io.tmpdir</code>.
     * @param prefSpec the preference specification object built by CRISNER
     * @return the <code>Path</code> to the newly created file
     * @throws SpecFileParseException if <code>prefSpec</code> was built from an invalid XML specification file
     * @throws IOException if an I/O error occurs while writing the .smv file
     */
    private static Path createSMVModel(PreferenceSpecification prefSpec)
            throws SpecFileParseException, IOException {
        CPTheoryDominanceExperimentDriver.REASONING_TASK reasoningTask =
                CPTheoryDominanceExperimentDriver.REASONING_TASK.DOMINANCE;
        Document doc;
        try {
            doc = XPathUtil.makeDocument(prefSpec.getPrefSpecFileName());
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new SpecFileParseException(prefSpec.getPrefSpecFileName(), e);
        }
        Path smvModel = Files.createTempFile(SMV_TEMPFILE_PREFIX, SMV_FILE_SUFFIX);
        smvModel.toFile().deleteOnExit();
        CPTheoryToSMVTranslator translator = new CPTheoryToSMVTranslator();
        try (BufferedWriter writer = Files.newBufferedWriter(smvModel)) {
            writer.write("MODULE main");
            writer.newLine();
            writer.newLine();
            writer.write("VAR");
            writer.newLine();
            ListMultimap<String, String> preferenceStatementGuards =
                    translateConditionalPreferences(translator, prefSpec, doc);
            translator.declareVariablesAndConstraints(prefSpec, reasoningTask, writer);
            writer.write("ASSIGN");
            writer.newLine();
            translator.encodeGuards(prefSpec, writer, preferenceStatementGuards, reasoningTask);
        }
        return smvModel;
    }

    /**
     * Builds a computation tree logic formula that represents the dominance query <code>better &gt; worse</code>.
     */
    private static String smvDominanceSpec(Outcome better, Outcome worse) {
        String[] variables = WorkingPreferenceModel.getPrefMetaData().getNamesOfVariables();
        StringBuilder stringWorse = new StringBuilder();
        StringBuilder stringBetter = new StringBuilder();
        StringBuilder readableStringWorse = new StringBuilder();
        StringBuilder readableStringBetter = new StringBuilder();
        for (String v : variables) {
            if (stringWorse.length() > 0) {
                stringWorse.append(" & ");
                readableStringWorse.append(",");
            }
            if (stringBetter.length() > 0) {
                stringBetter.append(" & ");
                readableStringBetter.append(",");
            }
            stringWorse.append(v).append("=").append(worse.getValuationOfVariable(v));
            stringBetter.append(v).append("=").append(better.getValuationOfVariable(v));
        }
        return SpecHelper.getCTLSpec(
                "(" + stringWorse.toString() + " -> EX EF (" + stringBetter.toString() + ")) ",
                "dominance",
                "--  (" + readableStringWorse.toString() + ") -> (" + readableStringBetter.toString() + ")");
    }

    /**
     * Invokes the private method
     * {@link CPTheoryToSMVTranslator#translateConditionalPreferences(PreferenceSpecification, Document, boolean)}
     * by means of the Java Reflection API.
     */
    @SuppressWarnings("unchecked")
    private static ListMultimap<String, String> translateConditionalPreferences(
            CPTheoryToSMVTranslator translator,
            PreferenceSpecification ps, Document doc) {
        Method privateMethod = null;
        boolean accessStatus = false;
        ListMultimap<String, String> result;
        try {
            // Retrieve and invoke the private method.
            privateMethod = CPTheoryToSMVTranslator.class.getDeclaredMethod(
                    "translateConditionalPreferences",
                    PreferenceSpecification.class, Document.class, boolean.class);
            accessStatus = privateMethod.isAccessible();
            privateMethod.setAccessible(true);
            result = (ListMultimap<String, String>) Objects.requireNonNull(
                    privateMethod.invoke(translator, ps, doc, true));
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);  // this should not happen if privateMethod is invoked correctly
        } finally {
            // Restore the original accessibility value.
            if (Objects.nonNull(privateMethod)) {
                privateMethod.setAccessible(accessStatus);
            }
        }
        return result;
    }
}