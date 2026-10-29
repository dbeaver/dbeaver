/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.model.sql.procedural;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.analyzer.builder.request.RequestBuilder;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolEntry;
import org.jkiss.dbeaver.model.sql.semantics.completion.SQLQueryCompletionItemKind;
import org.jkiss.dbeaver.model.sql.semantics.completion.SQLQueryCompletionProposal;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryLocalVariableInfo;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModel;
import org.jkiss.dbeaver.model.sql.semantics.model.programmaticals.SQLQueryProgrammaticalBodyStatementModel;
import org.jkiss.dbeaver.model.sql.semantics.model.statements.SQLQueryConditionalStatement;
import org.junit.jupiter.api.Assertions;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Builds the eleven-site procedural fixture described by {@link ProceduralNestedCompletionTest}. */
final class ProceduralNestedCompletionScenario {
    private static final String FROM = " FROM scratch s JOIN tab o ON s.id = o.order_id; ";
    private static final int QUERY_COUNT = 11;
    private static final List<String> VARIABLES = List.of(
        "counter_value", "threshold_value", "first_branch", "first_loop_value", "second_branch", "second_loop_value"
    );
    private static final List<Set<String>> VISIBLE_VARIABLES = List.of(
        Set.of("counter_value", "threshold_value"), // P0
        Set.of("counter_value", "threshold_value", "first_branch"), // P1
        Set.of("counter_value", "threshold_value", "first_branch", "first_loop_value"), // P2
        Set.of("counter_value", "threshold_value"), // P3: other IF branch
        Set.of("counter_value", "threshold_value"), // P4: after IF
        Set.of("counter_value", "threshold_value"), // P5: after WHILE
        Set.of("counter_value", "threshold_value", "second_branch"), // P6
        Set.of("counter_value", "threshold_value", "second_branch", "second_loop_value"), // P7
        Set.of("counter_value", "threshold_value", "second_branch"), // P8: other inner IF branch
        Set.of("counter_value", "threshold_value"), // P9: other outer IF branch
        Set.of("counter_value", "threshold_value") // P10: after IF
    );

    private ProceduralNestedCompletionScenario() {
    }

    static void verify(
        @NotNull ProceduralDialectTestBase test,
        @NotNull String prefix,
        @NotNull String temporaryTable,
        @NotNull String suffix,
        @NotNull String whileBodyKeyword,
        @NotNull String databaseTableName
    ) throws DBException {
        Assertions.assertEquals(SQLDialect.LocalVariableVisibility.LEXICAL,
            test.dialect().getLocalVariableVisibility(SQLDialect.LocalVariableKind.VALUE),
            "This fixture expects block-local declarations; program-visible dialects need their own visibility expectations");
        String sql = program(prefix, temporaryTable, suffix, whileBodyKeyword);
        var builder = RequestBuilder.tables(tables -> tables.table(databaseTableName, table -> {
            table.attribute("order_id");
            table.attribute("amount");
        }));
        // BasicSQLDialect has no connection metadata; the fixture's generic dialect does.
        if (test.dialect() != BasicSQLDialect.INSTANCE) {
            builder.dialect(test.dialect());
        }
        var request = builder.prepare();
        Set<String> databaseProposals = request.requestNewStrings("SELECT | FROM tab");
        assertContains(databaseProposals, "order_id", -1, "standalone database table");
        assertContains(databaseProposals, "amount", -1, "standalone database table");
        var preparedQuery = request.prepareQuery(sql);
        var model = preparedQuery.model();
        var routine = Assertions.assertInstanceOf(SQLQueryProgrammaticalBodyStatementModel.class, model.getQueryModel(), sql);
        test.assertEmbeddedSqlCount(routine, QUERY_COUNT);
        test.assertBound(model, "counter_value", 0, 1);
        test.assertBound(model, "first_branch", 0, 1);
        test.assertBound(model, "first_branch", 0, 2);
        test.assertBound(model, "first_loop_value", 0, 1);
        test.assertBound(model, "second_branch", 0, 1);
        test.assertBound(model, "second_loop_value", 0, 1);
        Assertions.assertEquals(SQLQueryConditionalStatement.Kind.LOOP,
            Assertions.assertInstanceOf(SQLQueryConditionalStatement.class,
                model.findNodeContaining(sql.indexOf("WHILE 1") + 1)).getKind());
        Assertions.assertEquals(SQLQueryConditionalStatement.Kind.IF,
            Assertions.assertInstanceOf(SQLQueryConditionalStatement.class,
                model.findNodeContaining(sql.indexOf("IF 1") + 1)).getKind());

        List<Integer> positions = queryPositions(sql);
        Assertions.assertEquals(QUERY_COUNT, positions.size(), sql);
        for (int i = 0; i < positions.size(); i++) {
            var queryProposals = request.requestNewProposals(preparedQuery, positions.get(i) + "SELECT ".length());
            Set<String> proposals = queryProposals.stream().map(SQLQueryCompletionProposal::getReplacementString)
                .collect(Collectors.toSet());
            assertContains(proposals, "id", i, "temporary table");
            Set<String> localProposals = queryProposals.stream()
                .filter(p -> p.getItemKind() == SQLQueryCompletionItemKind.LOCAL_VARIABLE)
                .map(p -> p.getReplacementString().trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
            Assertions.assertEquals(VISIBLE_VARIABLES.get(i), localProposals,
                "P" + i + " local-variable proposals: " + sql);
        }
    }

    /** Probe all sites in one model, including declarations from future or sibling blocks. */
    static void assertVariableResolution(
        @NotNull ProceduralDialectTestBase test,
        @NotNull String prefix,
        @NotNull String temporaryTable,
        @NotNull String suffix,
        @NotNull String whileBodyKeyword
    ) {
        String original = program(prefix, temporaryTable, suffix, whileBodyKeyword);
        StringBuilder expanded = new StringBuilder(original);
        List<Integer> originalPositions = queryPositions(original);
        for (int i = originalPositions.size() - 1; i >= 0; i--) {
            int select = originalPositions.get(i);
            expanded.replace(select + "SELECT ".length(), original.indexOf(FROM, select),
                String.join(", ", VARIABLES) + ", s.id, o.order_id");
        }
        String probe = expanded.toString();
        SQLQueryModel model = test.recognize(probe);
        Map<String, SQLQuerySymbolEntry> declarations = model.getAllSymbols().stream()
            .filter(entry -> entry.getDefinition() instanceof SQLQueryLocalVariableInfo)
            .filter(entry -> VARIABLES.contains(entry.getRawName().toLowerCase(Locale.ROOT)))
            .collect(Collectors.toMap(entry -> entry.getRawName().toLowerCase(Locale.ROOT), Function.identity()));
        Assertions.assertEquals(Set.copyOf(VARIABLES), declarations.keySet(), "Local declarations: " + probe);
        List<Integer> positions = queryPositions(probe);
        Assertions.assertEquals(QUERY_COUNT, positions.size(), probe);
        for (int i = 0; i < positions.size(); i++) {
            int useStart = positions.get(i) + "SELECT ".length();
            int useEnd = probe.indexOf(" FROM", useStart);
            Map<String, SQLQuerySymbolEntry> uses = model.getAllSymbols().stream()
                .filter(entry -> entry.getInterval().a >= useStart && entry.getInterval().a < useEnd)
                .filter(entry -> VARIABLES.contains(entry.getRawName().toLowerCase(Locale.ROOT)))
                .collect(Collectors.toMap(entry -> entry.getRawName().toLowerCase(Locale.ROOT), Function.identity()));
            Assertions.assertEquals(Set.copyOf(VARIABLES), uses.keySet(), "P" + i + " references: " + probe);
            for (String name : VARIABLES) {
                if (VISIBLE_VARIABLES.get(i).contains(name)) {
                    Assertions.assertSame(declarations.get(name), uses.get(name).getDefinition(), "P" + i + " should resolve " + name);
                } else {
                    Assertions.assertNotSame(declarations.get(name), uses.get(name).getDefinition(), "P" + i + " must not resolve " + name);
                }
            }
        }
    }

    private static void assertContains(@NotNull Set<String> proposals, @NotNull String expected, int index, @NotNull String source) {
        Assertions.assertTrue(proposals.stream().anyMatch(p -> {
            String name = p.trim().replace("\"", "");
            return name.equalsIgnoreCase(expected)
                || name.toLowerCase(java.util.Locale.ROOT).endsWith("." + expected.toLowerCase(java.util.Locale.ROOT));
        }),
            () -> "P" + index + " missing " + source + " column " + expected + ": " + proposals);
    }

    @NotNull
    private static List<Integer> queryPositions(@NotNull String sql) {
        List<Integer> positions = new ArrayList<>();
        for (int from = 0, index; (index = sql.indexOf("SELECT ", from)) >= 0; from = index + "SELECT ".length()) {
            positions.add(index);
        }
        return positions;
    }

    @NotNull
    private static String query(@NotNull String variables) {
        return "SELECT " + (variables.isEmpty() ? "" : variables + ", ") + "s.id, o.order_id" + FROM;
    }

    @NotNull
    private static String program(
        @NotNull String prefix, @NotNull String temporaryTable, @NotNull String suffix, @NotNull String whileBodyKeyword
    ) {
        StringBuilder sql = new StringBuilder(prefix).append(' ')
            .append("DECLARE counter_value int; DECLARE threshold_value int; ")
            .append(temporaryTable).append(' ');
        sql.append(query("counter_value, threshold_value")); // P0
        sql.append("WHILE 1 = 1 ").append(whileBodyKeyword).append(' ')
            .append("IF 1 = 1 THEN BEGIN DECLARE first_branch int; ").append(query("first_branch")) // P1
            .append("LOOP BEGIN DECLARE first_loop_value int; ").append(query("first_branch, first_loop_value")) // P2
            .append("END; END LOOP; END; ELSE ").append(query("counter_value")) // P3
            .append("END IF; ").append(query("counter_value")) // P4
            .append("END WHILE; ").append(query("counter_value")) // P5
            .append("IF 1 = 1 THEN BEGIN DECLARE second_branch int; IF 1 = 1 THEN ").append(query("second_branch")) // P6
            .append("LOOP BEGIN DECLARE second_loop_value int; ").append(query("second_branch, second_loop_value")) // P7
            .append("END; END LOOP; ELSE ").append(query("second_branch")) // P8
            .append("END IF; END; ELSE ").append(query("counter_value")) // P9
            .append("END IF; ").append(query("counter_value, threshold_value")) // P10
            .append(suffix);
        return sql.toString();
    }
}
