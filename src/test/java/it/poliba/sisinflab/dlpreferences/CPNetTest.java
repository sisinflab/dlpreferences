package it.poliba.sisinflab.dlpreferences;

import model.Outcome;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Tests for {@link CPNet} using well-formed preference specification files.
 */
@Test(groups = {"plainCPnet"}, dependsOnGroups = {"nusmv"})
public class CPNetTest {
    private CPNet cpnet;

    @Parameters({"nusmv-path", "hotel-cpnet-resource"})
    public CPNetTest(@Optional("") String nusmvPathRes, String xmlSpecRes) throws Exception {
        Path nusmvPath = Paths.get(nusmvPathRes);
        Path xmlSpec = Paths.get(CPNetTest.class.getResource(xmlSpecRes).toURI());
        this.cpnet = new CPNet(xmlSpec, nusmvPath);
    }

    @Test(dataProvider = "preferenceVariableProvider")
    public void testPreferenceVariables(Map<String, Set<String>> domainMap) throws Exception {
        Map<String, Set<String>> cpnetDomainMap = cpnet.getPreferenceGraph().domainMap();
        Assert.assertEquals(cpnetDomainMap, domainMap,
                TestUtils.reportSetDifference(cpnetDomainMap.entrySet(), domainMap.entrySet()));
    }

    @DataProvider
    public Object[][] preferenceVariableProvider() {
        Map<String, Set<String>> hotelPreferenceVariables = Stream.of(
                new AbstractMap.SimpleEntry<>("W", Stream.of("Wy", "Wn")),
                new AbstractMap.SimpleEntry<>("R", Stream.of("Rl", "Rm", "Rs")),
                new AbstractMap.SimpleEntry<>("B", Stream.of("Bo", "Bn")),
                new AbstractMap.SimpleEntry<>("C", Stream.of("Cy", "Cn")),
                new AbstractMap.SimpleEntry<>("P", Stream.of("Pl", "Ps")))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().collect(Collectors.toSet())));
        return new Object[][]{
                {hotelPreferenceVariables}
        };
    }

    @Test(dataProvider = "dominanceQueryProvider")
    public void testDominates(boolean expected, Outcome better, Outcome worse) throws Exception {
        Assert.assertEquals(this.cpnet.dominates(better, worse), expected);
    }

    @DataProvider
    public Object[][] dominanceQueryProvider() throws Exception {
        return new Object[][]{  // test data for the hotel_preferences.xml specification file
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
    }

}