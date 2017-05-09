package it.poliba.enasca.ontocpnets;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

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

    @Test(dataProvider = "verifyFromFileProvider")
    public void testVerifyFromFile(Path smvInput) throws Exception {
        NuSMVRunner runner = new NuSMVRunner(Paths.get(NUSMV_EXECUTABLE.toURI()));
        runner.verify(smvInput);
    }

    @DataProvider
    public Object[][] verifyFromFileProvider() throws URISyntaxException {
        return new Object[][]{
                {Paths.get(NuSMVRunnerTest.class.getResource("/hotel_model.smv").toURI())}
        };
    }

}