package it.poliba.enasca.ontocpnets;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Tests for {@link NuSMVRunner}.
 */
@Test(groups = {"nusmv"})
public class NuSMVRunnerTest {
    public static final URL NUSMV_EXECUTABLE = NuSMVRunnerTest.class.getResource("/nusmv-2.5.4/bin/NuSMV");

    /**
     * Tests {@link NuSMVRunner#sanityCheck(BufferedReader, BufferedReader)}
     * by generating {@link java.io.BufferedReader}s that will read the elements of
     * <code>outputStreamContent</code> and <code>errorStreamContent</code> as lines.
     * @param expected the expected return value of {@link NuSMVRunner#sanityCheck(BufferedReader, BufferedReader)}
     * @param outputStreamContent the strings that will simulate output lines from NuSMV
     * @param errorStreamContent the strings that will simulate error lines from NuSMV  */
    @Test(dataProvider = "sanityCheckProvider")
    public void testSanityCheck(boolean expected, String[] outputStreamContent, String[] errorStreamContent)
            throws Exception {
        boolean actual;
        try (
            BufferedReader outputReader = strings2Reader(outputStreamContent);
            BufferedReader errorReader = strings2Reader(errorStreamContent)
        ) {
            actual = NuSMVRunner.sanityCheck(outputReader, errorReader);
        }
        Assert.assertEquals(actual, expected);
    }

    @DataProvider
    public Object[][] sanityCheckProvider() {
        return new Object[][]{
                {true,  // expected return value
                new String[]{"This is " + CPNet.MODEL_CHECKER_NAME},  // output stream lines
                new String[]{}  // error stream lines
                },
                {false,  // expected return value
                new String[]{"This is NoSMV", "This is Nu SMV"},  // output stream lines
                new String[]{}  // error stream lines
                }
        };
    }

    @Test(dependsOnMethods = {"testSanityCheck"}, dataProvider = "nusmvProcessProvider")
    public void testRunNuSMV(Path input) throws Exception {
        Assert.assertTrue(NuSMVRunner.run(Paths.get(NUSMV_EXECUTABLE.toURI()), input, NuSMVRunner::sanityCheck));
    }

    @DataProvider
    public Object[][] nusmvProcessProvider() throws URISyntaxException {
        return new Object[][]{
                {Paths.get(NuSMVRunnerTest.class.getResource("/hotel_model.smv").toURI())}
        };
    }

    /**
     * Generates a buffered reader that will read lines from the input parameter.
     * @param lines the strings that the output reader will read as lines
     * @return
     */
    private BufferedReader strings2Reader(String[] lines) throws IOException {
        // Connect the I/O streams.
        PipedOutputStream outStream = new PipedOutputStream();
        PipedInputStream inStream = new PipedInputStream(outStream);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
        // Fill the output stream.
        try (PrintWriter writer = new PrintWriter(outStream)) {
            for (String line : lines) {
                writer.println(line);
            }
        }
        return reader;
    }

}