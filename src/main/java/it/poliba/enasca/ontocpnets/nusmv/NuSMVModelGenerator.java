package it.poliba.enasca.ontocpnets.nusmv;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import model.Outcome;
import model.PreferenceSpecification;
import model.PreferenceStatement;
import model.PreferenceVariable;
import util.Constants;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A utility class that converts CRISNER objects into NuSMV statements and expressions.
 */
public class NuSMVModelGenerator {
    /**
     * Translates preference specifications gathered by CRISNER into a NuSMV model.
     *
     * @param prefSpec the preference specification object, as built by CRISNER
     * @return a <code>String</code> representation of the equivalent NuSMV model,
     * as a <code>List</code> of lines
     */
    public static List<String> baseModel(PreferenceSpecification prefSpec) {
        Map<String, String> varsToDomains = prefSpec.getVariables().stream()
                .collect(Collectors.toMap(
                        PreferenceVariable::getVariableName,
                        prefVar -> enumType(prefVar.getDomainValues())));
        ImmutableListMultimap.Builder<String, String> expressionBuilder =
                ImmutableListMultimap.builder();
        for (PreferenceStatement statement : prefSpec.getStatements()) {
            statement.makeValid();
            for (String ordering : statement.getIntravarPreferences()) {
                String superior = ordering.split(Constants.PREFERENCE_SYMBOL_IN_XML)[0];
                String inferior = ordering.split(Constants.PREFERENCE_SYMBOL_IN_XML)[1];
                String inferiorValueAssigment = String.format("%s=%s", statement.getVariableName(), inferior);
                Stream<String> nonEmptyConditions = statement.getParentAssignments().stream()
                        .filter(condition -> condition.trim().length() > 0);
                Map<Boolean, List<String>> valueToInputVar = varsToDomains.keySet().stream()
                        .collect(Collectors.partitioningBy(
                                varName -> statement.getLessImpVariables().contains(varName) ||
                                        varName.equals(statement.getVariableName())));
                Stream<String> inputVarAssignments = valueToInputVar.entrySet().stream()
                        .flatMap(entry -> entry.getValue().stream()
                                .map(varName -> String.format("ch%s=%s", varName, entry.getKey() ? "1" : "0")));
                String leftExpression = Stream.concat(
                        Stream.of(inferiorValueAssigment),
                        Stream.concat(nonEmptyConditions, inputVarAssignments)
                ).collect(Collectors.joining(" & "));
                expressionBuilder.put(
                        statement.getVariableName(),
                        String.format("%s : %s;", leftExpression, superior));
                for (String unimportantVarName : statement.getLessImpVariables()) {
                    expressionBuilder.put(
                            unimportantVarName,
                            String.format("%s : %s;", leftExpression, varsToDomains.get(unimportantVarName)));
                }
            }
        }
        ImmutableListMultimap<String, String> caseBodyExpressions = expressionBuilder.build();

        String indentStr = "  ";
        Stream<String> assignList = varsToDomains.keySet().stream()
                .flatMap(varName -> {
                    Stream<String> opening = Stream.of(
                            indentStr + String.format("next(%s) :=", varName),
                            indentStr + indentStr + "case");
                    Stream<String> caseBody = caseBodyExpressions.get(varName).stream()
                            .map(expr -> indentStr + indentStr + indentStr + expr);
                    Stream<String> closing = Stream.of(
                            indentStr + indentStr + indentStr + String.format("TRUE : %s;", varName),
                            indentStr + indentStr + "esac;");
                    return Stream.concat(opening, Stream.concat(caseBody, closing));
                });

        Stream<String> varList = varsToDomains.entrySet().stream()
                .map(entry -> indentStr + String.format("%s : %s;", entry.getKey(), entry.getValue()));
        varList = Stream.concat(varList, Stream.of(indentStr + "gch : {0,1};"));

        Stream<String> frozenVarList = varsToDomains.entrySet().stream()
                .map(entry -> indentStr + String.format("%s_0 : %s;", entry.getKey(), entry.getValue()));

        Stream<String> ivarList = varsToDomains.keySet().stream()
                .map(varName -> indentStr + String.format("ch%s : {0,1};", varName));

        String defineBody = varsToDomains.keySet().stream()
                .map(varName -> String.format("%s=%s_0", varName, varName))
                .collect(Collectors.joining(" & ", "start := ", ";"));

        String transExpression = varsToDomains.keySet().stream()
                .map(varName -> String.format("%s_0=next(%s_0)", varName, varName))
                .collect(Collectors.joining(" & ", "", ";"));

        return ImmutableList.<String>builder()
                .add("MODULE main")
                .add("VAR").addAll(varList.iterator())
                .add("FROZENVAR").addAll(frozenVarList.iterator())
                .add("IVAR").addAll(ivarList.iterator())
                .add("DEFINE").add(defineBody)
                .add("INIT start=TRUE;")
                .add("TRANS").add(transExpression)
                .add("ASSIGN").addAll(assignList.iterator())
                .build();
    }

    /**
     * Translates the dominance query <code>better &gt; worse</code> into a NuSMV CTL specification.
     *
     * @param better
     * @param worse
     * @return a <code>String</code> representation of the equivalent NuSMV CTL specification.
     */
    public static String dominanceSpec(Outcome better, Outcome worse) {
        return String.format("CTLSPEC (%s -> EX EF (%s))",
                outcomeExpr(worse),
                outcomeExpr(better));
    }

    /**
     * Translates a sequence of <code>String</code>s into a NuSMV enumeration type.
     *
     * @param values
     * @return a <code>String</code> representation of the equivalent NuSMV enumeration type
     */
    private static String enumType(Set<String> values) {
        return values.stream()
                .collect(Collectors.joining(",", "{", "}"));
    }

    /**
     * Translates a CRISNER <code>Outcome</code> into a NuSMV expression.
     *
     * @param o
     * @return a <code>String</code> representation of the equivalent NuSMV expression
     */
    private static String outcomeExpr(Outcome o) {
        return o.getOutcomeAsValuationMap().entrySet().stream()
                .map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(" & "));
    }
}
