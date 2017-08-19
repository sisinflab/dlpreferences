package it.poliba.sisinflab.dlpreferences.nusmv;

import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Tests for {@link NuSMVRunner}.
 */
@Test(groups = {"nusmv"})
public class NuSMVRunnerTest {

    @Parameters({"nusmv-path", "hotel-model-resource"})
    public void testVerifyFromFile(@Optional("") String nusmvPathStr, String hotelModelRes) throws Exception {
        Path nusmvPath = Paths.get(nusmvPathStr);
        Path hotelModel = Paths.get(NuSMVRunnerTest.class.getResource(hotelModelRes).toURI());
        new NuSMVRunner(nusmvPath).verify(hotelModel);
    }

}