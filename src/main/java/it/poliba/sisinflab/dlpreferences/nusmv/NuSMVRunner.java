package it.poliba.sisinflab.dlpreferences.nusmv;

import it.poliba.sisinflab.dlpreferences.except.MalformedNuSMVModelException;
import it.unibg.nuseen.modeladvisor.ModelLoader;
import it.unibg.nuseen.modeladvisor.NuSMVExecutor;
import it.unibg.nuseen.modeladvisor.executor.NuSMVModelAdvisor;
import it.unibg.nuseen.modeladvisor.metaproperties.MetaPropertyChecker;
import it.unibg.nuseen.modeladvisor.metaproperties.NoPropertyIsFalse;
import it.unibg.nuseen.nusmvlanguage.nuSMV.NuSmvModel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
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
     * A NuSMV-compatible binary is searched in the directories specified by the system's PATH environment variable.
     * If PATH does not contain an executable file named <code>NuSMV</code> or <code>nuXmv</code>
     * (lowercase names are also checked), a {@link FileNotFoundException} is raised.
     * To provide a specific path for the NuSMV binary, use {@link #NuSMVRunner(Path)}.
     * <p>
     * NuSMVRunner may need to store an on-disk representation of the model being verified,
     * in the form of a .smv file in the system temp directory.
     * The system temp directory is specified by the JRE property <code>java.io.tmpdir</code>.
     *
     * @throws IllegalStateException if the PATH environment variable is not defined.
     * @throws FileNotFoundException if none of the directories specified by the system's PATH environment variable
     * contain an executable file named <code>NuSMV</code> or <code>nuXmv</code> (lowercase names are also checked).
     * @throws IOException if an I/O error occurs while attempting to write in the system temp directory.
     */
    public NuSMVRunner() throws IOException {
        this(Paths.get(""));
    }

    /**
     * Allows for specifying the path of the NuSMV-compatible binary.
     * If invoked with an empty or <code>null</code> path, this constructor is equivalent to {@link #NuSMVRunner()}.
     *
     * @param nusmvExec the NuSMV executable file. If empty or <code>null</code>, a suitable file is searched
     *                  in the system's PATH (equivalent to invoking {@link #NuSMVRunner()}).
     * @throws FileNotFoundException if <code>nusmvExec</code> is not an existing, executable file.
     * @throws IOException if an I/O error occurs while attempting to write in the system temp directory.
     */
    public NuSMVRunner(Path nusmvExec) throws IOException {
        if (nusmvExec == null || nusmvExec.toString().isEmpty()) {
            nusmvExec = findBinaries(Stream.of("NuSMV", "nuXmv"))
                    .findFirst()
                    .orElseThrow(() -> new FileNotFoundException("No NuSMV-compatible binary found in the system's PATH."));
        } else if (!Files.isExecutable(nusmvExec)) {
            throw new FileNotFoundException(nusmvExec.toString());
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
        // This attribute determines whether the execution of NuSMV is controlled
        // by the "org.eclipselabs.nusmvtools.nusmv4j" Java library.
        // By setting it to false, NuSMV is executed as an external process.
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

    /**
     * Searches system's PATH for the executable files specified in <code>filenames</code>.
     * For each element of <code>filenames</code> successfully resolved against a system PATH directory
     * as an executable file, the corresponding {@link Path} is added to the return <code>Stream</code>.
     * Lowercase variants of the elements in <code>filenames</code> are also checked.
     *
     * @param filenames
     * @throws IllegalStateException if the PATH environment variable is not defined.
     * @return
     */
    private Stream<Path> findBinaries(Stream<String> filenames) {
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null) {
            throw new IllegalStateException("PATH environment variable not defined.");
        }
        if (pathEnv.isEmpty()) {
            return Stream.empty();
        }
        // Add lowercase elements to the Stream argument.
        Set<String> filenamesWithLower =
                filenames.flatMap(f -> Stream.of(f, f.toLowerCase()))
                        .collect(Collectors.toSet());
        // Search the system's PATH.
        return Pattern.compile(Pattern.quote(File.pathSeparator))
                .splitAsStream(pathEnv)  // split into directory names
                .map(Paths::get)  // build a Path object from each directory
                .flatMap(directory -> filenamesWithLower.stream().map(directory::resolve))  // build a full Path from each file name
                .filter(path -> Files.isExecutable(path) && !Files.isDirectory(path));
    }

}
