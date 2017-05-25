package it.poliba.sisinflab.dlpreferences.nusmv;

import it.poliba.sisinflab.dlpreferences.except.MalformedNuSMVModelException;
import it.unibg.nuseen.modeladvisor.ModelLoader;
import it.unibg.nuseen.modeladvisor.NuSMVExecutor;
import it.unibg.nuseen.modeladvisor.executor.NuSMVModelAdvisor;
import it.unibg.nuseen.modeladvisor.metaproperties.MetaPropertyChecker;
import it.unibg.nuseen.modeladvisor.metaproperties.NoPropertyIsFalse;
import it.unibg.nuseen.nusmvlanguage.nuSMV.NuSmvModel;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Verifies NuSMV models using a local NuSMV installation.
 */
public class NuSMVRunner {
    private static final String SMV_FILE_PREFIX = "nsumv";
    private static final String SMV_FILE_SUFFIX = ".smv";
    private static final List<String> TRIVIAL_MODEL = Stream.<String>builder()
            .add("MODULE main")
            .add("VAR")
            .add("state: {ready, busy};")
            .add("ASSIGN")
            .add("init(state) := ready;")
            .build()
            .collect(Collectors.toList());

    private NuSMVModelAdvisor nma;
    private Path modelPath;

    /**
     * Creates a <code>NuSMVRunner</code> instance that verifies NuSMV models
     * using the specified NuSMV executable file.
     * <p>
     * NuSMVRunner may need to store an on-disk representation of the model being verified,
     * in the form of a .smv file in the system temp directory.
     * The system temp directory is specified by the JRE property <code>java.io.tmpdir</code>.
     *
     * @param nusmvExec the NuSMV executable file
     * @throws FileNotFoundException if <code>nusmvExec</code> is not an existing, executable file.
     * @throws IOException if an I/O error occurs while attempting to write in the system temp directory.
     */
    public NuSMVRunner(Path nusmvExec) throws IOException {
        if (!Files.isExecutable(nusmvExec)) {
            throw new FileNotFoundException();
        }
        // Set up the NuSMV executor.
        modelPath = Files.createTempFile(SMV_FILE_PREFIX, SMV_FILE_SUFFIX);
        modelPath.toFile().deleteOnExit();
        nma = new NuSMVModelAdvisor(modelPath.toAbsolutePath().toString());
        nma.setPath(nusmvExec.toAbsolutePath().toString());
        nma.setMetapropertiesExecution(
                false,  // disable check for "Every assignment condition can be true"
                false,  // disable check for "Every assignment is eventually applied"
                false,  // disable check for "The assignment conditions are mutually exclusive"
                false,  // disable check for "For every assignment terminated by a default condition true, at least an assignment condition is true"
                false,  // disable check for "No assignment is always trivial"
                false,  // disable check for "Every variable can take any value in its type"
                false,  // disable check for "Every variable not explicitly assigned is used"
                false,  // disable check for "Every independent variable is used"
                true    // enable check for "Every property is true and no property is vacuously satisfied"
        );
        NuSMVExecutor.jna = false;
        // Check the local NuSMV installation by verifying a trivial model.
        verify(TRIVIAL_MODEL);
    }

    /**
     * Checks whether the CTL and LTL properties of a NuSMV model are all true.
     *
     * @param model
     * @throws MalformedNuSMVModelException if <code>model</code> is not a valid NuSMV model
     * @return
     */
    public boolean verify(NuSmvModel model) {
        return loadAndVerify(new ModelLoader(model));
    }

    /**
     * Checks whether the CTL and LTL properties of a NuSMV model are all true.
     * The input model is specified as a <code>Stream</code> of lines.
     * <p>
     * This is a convenience method that stores <code>model</code> as a .smv file
     * in the system temp directory, then invokes {@link #verify(Path)}.
     * The system temp directory is specified by the JRE property <code>java.io.tmpdir</code>.
     *
     * @param model a <code>Stream</code> of lines representing the NuSMV model
     * @throws MalformedNuSMVModelException if <code>model</code> is not a valid NuSMV model
     * @return
     */
    public boolean verify(Stream<String> model) {
        return verify(model.collect(Collectors.toList()));
    }

    /**
     * Checks whether the CTL and LTL properties of a NuSMV model are all true.
     * The input model is specified as a <code>List</code> of lines.
     * <p>
     * This is a convenience method that stores <code>model</code> as a .smv file
     * in the system temp directory, then invokes {@link #verify(Path)}.
     * The system temp directory is specified by the JRE property <code>java.io.tmpdir</code>.
     *
     * @param model a <code>List</code> of lines representing the NuSMV model
     * @throws MalformedNuSMVModelException if <code>model</code> is not a valid NuSMV model
     * @return
     */
    public boolean verify(List<String> model) {
        try {
            Files.write(modelPath, model);
            return verify(modelPath);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    /**
     * Checks whether the CTL and LTL properties of a NuSMV model are all true.
     *
     * @param smvInput a text file containing the NuSMV model
     * @throws FileNotFoundException if <code>smvInput</code> is not an existing, readable file
     * @throws MalformedNuSMVModelException if <code>smvInput</code> contains an invalid NuSMV model
     * @return
     */
    public boolean verify(Path smvInput) throws FileNotFoundException {
        if (!Files.isRegularFile(smvInput) || !Files.isReadable(smvInput)) {
            throw new FileNotFoundException();
        }
        return loadAndVerify(
                new ModelLoader(smvInput.toAbsolutePath().toString(), false));
    }

    /**
     * Checks whether the CTL and LTL properties of a NuSMV model are all true.
     *
     * @param modelLoader
     * @throws MalformedNuSMVModelException if the input model is not a valid NuSMV model
     * @throws IllegalStateException if <code>nma</code> has no
     * {@link it.unibg.nuseen.modeladvisor.metaproperties.MetaPropertyChecker} enabled
     * @return
     */
    private boolean loadAndVerify(ModelLoader modelLoader) {
        // Evaluate the model.
        try {
            modelLoader.loadModel();
            nma.runCheck(modelLoader);
        } catch (Exception e) {
            throw new MalformedNuSMVModelException(e);
        }
        // Collect the results.
        boolean arePropertiesTrue = true;
        Collection<MetaPropertyChecker> checkers = nma.mapMpClasses.get(NoPropertyIsFalse.class);
        if (!checkers.isEmpty()) {
            for (MetaPropertyChecker checker : checkers) {
                arePropertiesTrue = arePropertiesTrue && (checker.getNumOfViolations() == 0);
            }
        } else {
            throw new IllegalStateException();
        }
        return arePropertiesTrue;
    }

}
