package it.poliba.enasca.ontocpnets;

import it.poliba.enasca.ontocpnets.except.MalformedNuSMVModelException;
import it.unibg.nuseen.modeladvisor.ModelLoader;
import it.unibg.nuseen.modeladvisor.NuSMVExecutor;
import it.unibg.nuseen.modeladvisor.executor.NuSMVModelAdvisor;
import it.unibg.nuseen.modeladvisor.metaproperties.MetaPropertyChecker;
import it.unibg.nuseen.modeladvisor.metaproperties.NoPropertyIsFalse;
import it.unibg.nuseen.nusmvlanguage.nuSMV.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

/**
 * Verifies NuSMV models using a local NuSMV installation.
 */
public class NuSMVRunner {
    static final String SMV_TEMPFILE_PREFIX = "nsumv";
    static final String SMV_FILE_SUFFIX = ".smv";

    private static final NuSmvModel TRIVIAL_MODEL = trivialModel();

    private NuSMVModelAdvisor nma;

    /**
     * Creates a <code>NuSMVRunner</code> instance that verifies NuSMV models
     * using the specified NuSMV executable file.
     * <p>
     * NuSMVRunner may need to store an on-disk representation of the model being verified,
     * in the form of a .smv file in the system temp directory, as specified by the
     * JRE property <code>java.io.tmpdir</code>.
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
        Path smvTemp = Files.createTempFile(SMV_TEMPFILE_PREFIX, SMV_FILE_SUFFIX);
        smvTemp.toFile().deleteOnExit();
        nma = new NuSMVModelAdvisor(smvTemp.toAbsolutePath().toString());
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
     * @param model
     * @throws MalformedNuSMVModelException if <code>model</code> is not a valid NuSMV model
     * @return
     */
    public boolean verify(NuSmvModel model) {
        ModelLoader modelLoader = new ModelLoader(model);
        return loadAndVerify(modelLoader);
    }

    /**
     * Checks whether the CTL and LTL properties of a NuSMV model are all true.
     * @param smvInput a text file containing the NuSMV model
     * @throws FileNotFoundException if <code>smvInput</code> is not an existing, readable file
     * @throws MalformedNuSMVModelException if <code>smvInput</code> contains an invalid NuSMV model
     * @return
     */
    public boolean verify(Path smvInput) throws FileNotFoundException {
        if (!Files.isRegularFile(smvInput) || !Files.isReadable(smvInput)) {
            throw new FileNotFoundException();
        }
        ModelLoader modelLoader = new ModelLoader(smvInput.toAbsolutePath().toString(), false);
        return loadAndVerify(modelLoader);
    }

    /**
     * Checks whether the CTL and LTL properties of a NuSMV model are all true.
     * @param modelLoader
     * @throws MalformedNuSMVModelException if the input model is not a valid NuSMV model
     * @throws IllegalStateException if <code>nma</code> has
     * no {@link it.unibg.nuseen.modeladvisor.metaproperties.MetaPropertyChecker} enabled
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
     * Creates a {@link NuSmvModel} representation of the trivial model:
     * <pre>{@code
     * MODULE main
     * VAR
     *     state: {ready, busy};
     * ASSIGN
     *     init(state) := ready;
     * }</pre>
     * @return
     */
    private static NuSmvModel trivialModel() {
        Module mainModule = NuSMVFactory.eINSTANCE.createModule();
        mainModule.setName("main");

        Val readyVal = NuSMVFactory.eINSTANCE.createVal();
        readyVal.setName("ready");
        Val busyVal = NuSMVFactory.eINSTANCE.createVal();
        busyVal.setName("busy");
        EnumType stateEnum = NuSMVFactory.eINSTANCE.createEnumType();
        stateEnum.getVal().add(readyVal);
        stateEnum.getVal().add(busyVal);

        VarBody stateVar = NuSMVFactory.eINSTANCE.createVarBody();
        stateVar.setName("state");
        stateVar.setSemicolon(true);
        stateVar.setType(stateEnum);
        VariableDeclaration varDecl = NuSMVFactory.eINSTANCE.createVariableDeclaration();
        varDecl.getVars().add(stateVar);
        mainModule.getModuleElement().add(varDecl);

        ValueExpression readyExpr = NuSMVFactory.eINSTANCE.createValueExpression();
        readyExpr.setValue("ready");
        InitBody initAssign = NuSMVFactory.eINSTANCE.createInitBody();
        initAssign.setVar("state");
        initAssign.setSemicolon(true);
        initAssign.setInitial(readyExpr);
        AssignConstraintElement assignElem = NuSMVFactory.eINSTANCE.createAssignConstraintElement();
        assignElem.setAssign("ASSIGN");
        assignElem.getBodies().add(initAssign);
        mainModule.getModuleElement().add(assignElem);

        NuSmvModel model = NuSMVFactory.eINSTANCE.createNuSmvModel();
        model.getModules().add(mainModule);

        return model;
    }

}
