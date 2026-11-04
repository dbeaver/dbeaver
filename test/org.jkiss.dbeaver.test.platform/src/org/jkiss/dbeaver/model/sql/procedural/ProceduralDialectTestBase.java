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

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.Trees;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.lsm.LSMAnalyzerParameters;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardLexer;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.analyzer.builder.request.RequestBuilder;
import org.jkiss.dbeaver.model.sql.analyzer.builder.request.RequestResult;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryModelRecognizer;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolEntry;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModel;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryNodeModel;
import org.jkiss.dbeaver.model.sql.semantics.model.programmaticals.SQLQueryProgrammaticalBodyStatementModel;
import org.jkiss.dbeaver.model.sql.semantics.model.statements.SQLQueryConditionalStatement;
import org.jkiss.dbeaver.model.sql.semantics.model.statements.SQLQueryEmbeddedStatement;
import org.jkiss.dbeaver.model.sql.semantics.model.statements.SQLQueryStatementBlock;
import org.jkiss.dbeaver.model.sql.semantics.model.statements.SQLQueryStatementModel;
import org.jkiss.dbeaver.model.sql.semantics.model.statements.SQLQueryStatementSequence;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Shared fixture for supported procedural syntax and its semantic completion context. */
abstract class ProceduralDialectTestBase extends DBeaverUnitTest {
    @NotNull
    protected abstract SQLDialect dialect();

    @NotNull
    protected final SQLDialect dialectById(@NotNull String id) {
        var metadata = DBWorkbench.getPlatform().getSQLDialectRegistry().getDialect(id);
        Assertions.assertNotNull(metadata, () -> "No dialect registered under " + id);
        try {
            return metadata.createInstance();
        } catch (DBException e) {
            throw new AssertionError("Cannot initialize dialect " + id, e);
        }
    }

    @NotNull
    protected final SQLDialect dialectFromBundle(@NotNull String bundleId, @NotNull String className) {
        var bundle = Platform.getBundle(bundleId);
        Assertions.assertNotNull(bundle, () -> "Missing dialect bundle " + bundleId);
        try {
            return (SQLDialect) bundle.loadClass(className).getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Cannot initialize dialect " + className, e);
        }
    }

    @NotNull
    protected final SQLQueryModel recognize(@NotNull String sql) {
        SQLDialect dialect = dialect();
        SQLSyntaxManager syntaxManager = new SQLSyntaxManager();
        syntaxManager.init(dialect, DBWorkbench.getPlatform().getPreferenceStore());
        SQLQueryRecognitionContext context = new SQLQueryRecognitionContext(
            new VoidProgressMonitor(), null, false, false, syntaxManager, dialect
        );
        SQLQueryModel model = SQLQueryModelRecognizer.recognizeQuery(context, sql);
        Assertions.assertNotNull(model, sql);
        Assertions.assertNotNull(model.getQueryModel(), () -> "No structured semantic model for " + sql);
        return model;
    }

    @NotNull
    protected final <T extends SQLQueryNodeModel> T root(@NotNull String sql, @NotNull Class<T> type) {
        return Assertions.assertInstanceOf(type, recognize(sql).getQueryModel(), sql);
    }

    @NotNull
    protected final <T extends SQLQueryNodeModel> T root(@NotNull SQLQueryModel model, @NotNull Class<T> type) {
        return Assertions.assertInstanceOf(type, model.getQueryModel(), model.getSyntaxNode().getTextContent());
    }

    @NotNull
    protected final <T extends SQLQueryNodeModel> T statementAt(
        @NotNull String sql, @NotNull String snippet, @NotNull Class<T> type
    ) {
        int position = sql.indexOf(snippet);
        Assertions.assertTrue(position >= 0, () -> "Missing snippet '" + snippet + "' in " + sql);
        return Assertions.assertInstanceOf(type, recognize(sql).findNodeContaining(position + 1), sql);
    }

    @NotNull
    protected final <T extends SQLQueryNodeModel> T statementAt(
        @NotNull SQLQueryModel model, @NotNull String sql, @NotNull String snippet, @NotNull Class<T> type
    ) {
        int position = sql.indexOf(snippet);
        Assertions.assertTrue(position >= 0, () -> "Missing snippet '" + snippet + "' in " + sql);
        return Assertions.assertInstanceOf(type, model.findNodeContaining(position + 1), sql);
    }

    /** Require a complete structured parse, not just a semantic model built from recovered fragments. */
    protected final void assertStructuredControlFlow(@NotNull String sql, int rule) {
        SQLSyntaxManager syntaxManager = new SQLSyntaxManager();
        syntaxManager.init(dialect(), DBWorkbench.getPlatform().getPreferenceStore());
        LSMAnalyzerParameters parameters = LSMAnalyzerParameters.forDialect(dialect(), syntaxManager);
        SQLStandardLexer lexer = new SQLStandardLexer(CharStreams.fromString(sql), parameters);
        SQLStandardParser parser = new SQLStandardParser(new CommonTokenStream(lexer), parameters);
        var tree = parser.sqlQuery();
        Assertions.assertEquals(0, parser.getNumberOfSyntaxErrors(), sql);
        Assertions.assertEquals(Token.EOF, parser.getInputStream().LA(1), sql);
        Assertions.assertEquals(1, Trees.findAllRuleNodes(tree, rule).size(), sql);
        Assertions.assertTrue(Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_routineBodyTokens).isEmpty(), sql);
        Assertions.assertTrue(Trees.findAllRuleNodes(tree, SQLStandardParser.RULE_proceduralStatementTokens).isEmpty(), sql);
    }

    protected final void assertBound(@NotNull SQLQueryModel model, @NotNull String name, int declaration, int use) {
        List<SQLQuerySymbolEntry> occurrences = model.getAllSymbols().stream()
            .filter(symbol -> symbol.getRawName().equalsIgnoreCase(name))
            .sorted(Comparator.comparingInt(symbol -> symbol.getInterval().a)).toList();
        Assertions.assertTrue(occurrences.size() > Math.max(declaration, use),
            () -> name + ": " + occurrences + "; all symbols: " + model.getAllSymbols());
        Assertions.assertSame(occurrences.get(declaration), occurrences.get(use).getDefinition(), name);
    }

    protected final void assertEmbeddedSqlCount(@NotNull SQLQueryProgrammaticalBodyStatementModel program, int expected) {
        List<SQLQueryEmbeddedStatement> statements = new ArrayList<>();
        Assertions.assertNotNull(program.getBody());
        collectEmbeddedSql(program.getBody(), statements);
        Assertions.assertEquals(expected, statements.size(), () -> "Embedded SQL in " + program.getSyntaxNode().getTextContent());
        Assertions.assertTrue(statements.stream().allMatch(s -> s.getSql() != null));
    }

    protected final void assertEverySelectProposes(
        @NotNull RequestResult request, @NotNull String sql, @NotNull String expected, int count
    ) throws DBException {
        String selection = "SELECT " + expected;
        int completed = 0;
        for (int start = 0; (start = sql.indexOf(selection + " FROM", start)) >= 0; start += selection.length()) {
            String marked = sql.substring(0, start) + "SELECT |" + sql.substring(start + selection.length());
            Assertions.assertTrue(request.requestNewStrings(marked).stream().anyMatch(p -> p.trim().equalsIgnoreCase(expected)),
                "P" + completed + ": " + marked);
            completed++;
        }
        Assertions.assertEquals(count, completed, sql);
    }

    private static void collectEmbeddedSql(
        @NotNull SQLQueryStatementModel node, @NotNull List<SQLQueryEmbeddedStatement> result
    ) {
        if (node instanceof SQLQueryEmbeddedStatement embedded) {
            result.add(embedded);
        } else if (node instanceof SQLQueryStatementSequence sequence) {
            sequence.getStatements().forEach(child -> collectEmbeddedSql(child, result));
        } else if (node instanceof SQLQueryStatementBlock block) {
            block.getSections().forEach(child -> collectEmbeddedSql(child, result));
        } else if (node instanceof SQLQueryConditionalStatement conditional) {
            conditional.getBranches().forEach(child -> collectEmbeddedSql(child, result));
        }
    }

    @NotNull
    protected final Set<String> proposals(@NotNull String markedSql) throws DBException {
        return RequestBuilder.empty().dialect(dialect()).prepare().requestNewStrings(markedSql);
    }

    @NotNull
    protected final Set<String> scriptProposals(@NotNull String markedSql) throws DBException {
        return RequestBuilder.empty().dialect(dialect()).prepare().requestNewScriptStrings(markedSql);
    }

    protected final void assertScriptProposes(@NotNull String markedSql, @NotNull String expected) throws DBException {
        Set<String> actual = scriptProposals(markedSql);
        Assertions.assertTrue(actual.stream().anyMatch(value -> value.trim().equalsIgnoreCase(expected)),
            () -> "Missing '" + expected + "' in " + actual + " for " + markedSql);
    }

    protected final void assertScriptDoesNotPropose(@NotNull String markedSql, @NotNull String excluded) throws DBException {
        Set<String> actual = scriptProposals(markedSql);
        Assertions.assertTrue(actual.stream().noneMatch(value -> value.trim().equalsIgnoreCase(excluded)),
            () -> "Unexpected '" + excluded + "' in " + actual + " for " + markedSql);
    }

    protected final void assertProposes(@NotNull String markedSql, @NotNull String... expected) throws DBException {
        Set<String> proposals = proposals(markedSql);
        for (String value : expected) {
            Assertions.assertTrue(proposals.stream().anyMatch(text -> text.trim().equalsIgnoreCase(value)),
                () -> "Missing '" + value + "' in " + proposals + " for " + markedSql);
        }
    }

    protected final void assertDoesNotPropose(@NotNull String markedSql, @NotNull String... forbidden) throws DBException {
        Set<String> proposals = proposals(markedSql);
        for (String value : forbidden) {
            Assertions.assertTrue(proposals.stream().noneMatch(text -> text.trim().equalsIgnoreCase(value)),
                () -> "Unexpected '" + value + "' in " + proposals + " for " + markedSql);
        }
    }
}
