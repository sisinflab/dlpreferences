package it.poliba.enasca.ontocpnets;

import it.poliba.enasca.ontocpnets.except.NuSMVExitStatusException;
import it.poliba.enasca.ontocpnets.except.NuSMVInterruptedException;
import it.poliba.enasca.ontocpnets.except.NuSMVRuntimeException;
import it.poliba.enasca.ontocpnets.except.NuSMVTimeoutException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

/**
 * Contains facilities to run NuSMV in a {@link Process}.
 * The main point of entry is {@link #run(Path, Path, BiPredicate)}, which uses a binary predicate
 * to check the output of NuSMV.
 */
public class NuSMVRunner {
    /**
     * The default timeout for the NuSMV process, in milliseconds.
     */
    public static final long TIMEOUTMS = 5000;

    /**
     * Invokes a NuSMV executable with the specified input file.
     * The output and error streams from NuSMV are redirected to newly created {@link BufferedReader}s,
     * which are passed to <code>checker</code>.
     * <p>
     * The <code>checker</code> function takes two parameters:
     * <ol>
     * <li>a buffered reader connected to the standard output stream of NuSMV;</li>
     * <li>a buffered reader connected to the standard error stream of NuSMV.</li>
     * </ol>
     * The purpose of <code>checker</code> is to read from the buffers, perform some checks
     * and return <code>true</code> if it was successful.
     *
     * @param executable the path to the NuSMV binary
     * @param input      the input file that NuSMV will receive as command line argument
     * @param checker    a predicate that checks whether NuSMV ran successfully
     * @return the same value returned by <code>checker</code>
     * @throws FileNotFoundException if any of the {@link Path} arguments are invalid.
     * To be valid, the arguments must be regular files. Additionally, <code>executable</code> must be an executable file
     * and <code>input</code> must be a readable file.
     * @throws IOException              if the NuSMV process could not start due to an I/O error
     * @throws NuSMVExitStatusException if the NuSMV process exited with a non-zero status code
     * @throws NuSMVTimeoutException    if the NuSMV process was still alive after a waiting time of {@link #TIMEOUTMS}
     * @throws NuSMVInterruptedException if the NuSMV process is interrupted
     * @throws NuSMVRuntimeException    if the <code>checker</code> function threw an unchecked exception.
     * The original exception is wrapped in a NuSMVRuntimeException
     * by adding details about the NuSMV process.
     * @throws NullPointerException if any of the arguments are <code>null</code>
     */
    public static boolean run(Path executable, Path input, BiPredicate<BufferedReader, BufferedReader> checker)
            throws IOException {
        // Check the input arguments.
        Objects.requireNonNull(checker);
        if (!Files.isExecutable(executable)) {
            throw new FileNotFoundException(executable.toString());
        }
        if (!Files.isRegularFile(input) || !Files.isReadable(input)) {
            throw new FileNotFoundException(input.toString());
        }
        // Build the command line.
        String[] commandLine = {executable.toString(),
                "-dcx",  // do not generate counterexamples
                input.toString()};
        // Create and start the NuSMV process.
        Process process = new ProcessBuilder(Arrays.stream(commandLine).collect(Collectors.toList()))
                .start();
        // Apply the predicate.
        boolean result;
        try (BufferedReader pOutputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
             BufferedReader pErrorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))
        ) {
            result = checker.test(pOutputReader, pErrorReader);
            if (!process.waitFor(TIMEOUTMS, TimeUnit.MILLISECONDS)) {
                throw new NuSMVTimeoutException(commandLine, TIMEOUTMS, TimeUnit.MILLISECONDS);
            }
            int exitVal = process.exitValue();
            if (exitVal != 0) {
                throw new NuSMVExitStatusException(commandLine, exitVal);
            }
        } catch (NuSMVTimeoutException | NuSMVExitStatusException e) {
            throw e;
        } catch (InterruptedException e) {
            throw new NuSMVInterruptedException(commandLine, e);
        } catch (RuntimeException | Error e) {
            throw new NuSMVRuntimeException(commandLine, e);
        } finally {
            if (process.isAlive()) {
                process.destroy();
            }
        }
        return result;
    }

    /**
     * An implementer of {@link java.util.function.BiPredicate} that checks whether the output stream
     * contains at least one occurrence of the string {@link CPNet#MODEL_CHECKER_NAME}.
     *
     * @param outputReader a reader connected to the output stream of NuSMV
     * @param errorReader  a reader connected to the error stream of NuSMV
     * @return <code>true</code> if the condition is met; <code>false</code> otherwise
     */
    public static boolean sanityCheck(BufferedReader outputReader, BufferedReader errorReader) {
        return outputReader.lines().anyMatch(e -> e.contains(CPNet.MODEL_CHECKER_NAME));
    }
}
