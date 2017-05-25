package it.poliba.sisinflab.dlpreferences;

import it.poliba.sisinflab.dlpreferences.nusmv.NuSMVRunnerTest;
import model.Outcome;
import org.testng.AssertJUnit;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Tests for {@link CPNet} using well-formed preference specification files.
 */
@Test(groups = {"plainCPnet"}, dependsOnGroups = {"nusmv"})
public class CPNetTest {
    private CPNet cpnet;
    private Object[][] dominanceQueryData;

    @Factory(dataProvider = "factoryProvider")
    public CPNetTest(Path xmlPrefSpec, Object[][] dominanceQueryData) throws Exception {
        this.cpnet = new CPNet(xmlPrefSpec, Paths.get(NuSMVRunnerTest.NUSMV_EXECUTABLE.toURI()));
        this.dominanceQueryData = dominanceQueryData;
    }

    @DataProvider
    public static Object[][] factoryProvider() throws Exception {
        Object[][] hotelQueryData = {  // test data for the hotel_preferences.xml specification file
                // Query no. 1
                {true,  // expected result
                new Outcome(Stream.of(  // better outcome
                        new AbstractMap.SimpleEntry<>("W", "Wn"),
                        new AbstractMap.SimpleEntry<>("R", "Rm"),
                        new AbstractMap.SimpleEntry<>("B", "Bn"),
                        new AbstractMap.SimpleEntry<>("C", "Cy"),
                        new AbstractMap.SimpleEntry<>("P", "Pl"))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))),
                new Outcome(Stream.of(  // worse outcome
                        new AbstractMap.SimpleEntry<>("W", "Wn"),
                        new AbstractMap.SimpleEntry<>("R", "Rs"),
                        new AbstractMap.SimpleEntry<>("B", "Bo"),
                        new AbstractMap.SimpleEntry<>("C", "Cy"),
                        new AbstractMap.SimpleEntry<>("P", "Ps"))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))},
                // Query no. 2
                {false,  // expected result
                new Outcome(Stream.of(  // better outcome
                        new AbstractMap.SimpleEntry<>("W", "Wn"),
                        new AbstractMap.SimpleEntry<>("R", "Rl"),
                        new AbstractMap.SimpleEntry<>("B", "Bo"),
                        new AbstractMap.SimpleEntry<>("C", "Cy"),
                        new AbstractMap.SimpleEntry<>("P", "Ps"))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))),
                new Outcome(Stream.of(  // worse outcome
                        new AbstractMap.SimpleEntry<>("W", "Wy"),
                        new AbstractMap.SimpleEntry<>("R", "Rl"),
                        new AbstractMap.SimpleEntry<>("B", "Bo"),
                        new AbstractMap.SimpleEntry<>("C", "Cy"),
                        new AbstractMap.SimpleEntry<>("P", "Ps"))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))}
        };
        return new Object[][]{
                {Paths.get(CPNetTest.class.getResource("/hotel_preferences.xml").toURI()), hotelQueryData}
        };
    }

    @Test(dataProvider = "dominanceQueryProvider")
    public void testDominates(boolean expected, Outcome better, Outcome worse) throws Exception {
        AssertJUnit.assertEquals(expected, this.cpnet.dominates(better, worse));
    }

    @DataProvider
    public Object[][] dominanceQueryProvider() {
        return this.dominanceQueryData;
    }

}