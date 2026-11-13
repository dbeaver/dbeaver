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
package org.jkiss.dbeaver.model.lsm.test;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.preferences.SimplePreferenceStore;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.lsm.LSMAnalyzerParameters;
import org.jkiss.dbeaver.model.lsm.sql.dialect.SQLStandardAnalyzer;
import org.jkiss.dbeaver.model.lsm.sql.impl.syntax.SQLStandardParser;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.stm.STMErrorListener;
import org.jkiss.dbeaver.model.stm.STMSource;
import org.jkiss.dbeaver.model.stm.STMTreeRuleNode;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class LSMAnalyzerImplTest extends DBeaverUnitTest {
    private static final LSMAnalyzerParameters PARAMETERS = createParameters();

    private static LSMAnalyzerParameters createParameters() {
        SimplePreferenceStore preferences = new SimplePreferenceStore() {
            @Override
            public void save() {
                // In-memory test preferences.
            }
        };
        SQLSyntaxManager syntaxManager = new SQLSyntaxManager(preferences);
        syntaxManager.init(BasicSQLDialect.INSTANCE, preferences);
        return LSMAnalyzerParameters.forDialect(BasicSQLDialect.INSTANCE, syntaxManager);
    }

    @Test
    public void doesNotForwardErrorsFromFailedSllAttempt() {
        CountingErrorListener listener = new CountingErrorListener();
        SQLStandardAnalyzer analyzer = new ForcedFallbackAnalyzer(PARAMETERS);

        STMTreeRuleNode tree = analyzer.parseSqlQueryTree(STMSource.fromString("SELECT 1"), listener);

        Assertions.assertNotNull(tree);
        Assertions.assertEquals("SELECT", tree.start.getText());
        Assertions.assertEquals(0, listener.errorCount);
    }

    @Test
    public void usesOnlySllWhenItSucceeds() {
        CountingErrorListener listener = new CountingErrorListener();
        TrackingAnalyzer analyzer = new TrackingAnalyzer(PARAMETERS);

        STMTreeRuleNode tree = analyzer.parseSqlQueryTree(STMSource.fromString("SELECT 1"), listener);

        Assertions.assertNotNull(tree);
        Assertions.assertEquals(List.of(PredictionMode.SLL), analyzer.predictions);
        Assertions.assertEquals(0, analyzer.reusedParserCount);
        Assertions.assertEquals(0, listener.errorCount);
    }

    @Test
    public void fallsBackWhenSllHitsParserError() {
        CountingErrorListener listener = new CountingErrorListener();
        TrackingAnalyzer analyzer = new TrackingAnalyzer(PARAMETERS);

        STMTreeRuleNode tree = analyzer.parseSqlQueryTree(STMSource.fromString("(SELECT 1"), listener);

        Assertions.assertNotNull(tree);
        Assertions.assertEquals(List.of(PredictionMode.SLL, PredictionMode.LL), analyzer.predictions);
        Assertions.assertEquals(1, analyzer.reusedParserCount);
        Assertions.assertTrue(listener.errorCount > 0);
    }

    @Test
    public void fallsBackToFreshParserWhenTokenReuseIsUnsupported() {
        CountingErrorListener listener = new CountingErrorListener();
        NoReuseAnalyzer analyzer = new NoReuseAnalyzer(PARAMETERS);

        STMTreeRuleNode tree = analyzer.parseSqlQueryTree(STMSource.fromString("(SELECT 1"), listener);

        Assertions.assertNotNull(tree);
        Assertions.assertEquals(List.of(PredictionMode.SLL, PredictionMode.LL), analyzer.predictions);
        Assertions.assertEquals(2, analyzer.prepareParserCount);
    }

    @Test
    public void fallsBackWhenSllLeavesTrailingTokens() {
        CountingErrorListener listener = new CountingErrorListener();
        TrackingAnalyzer analyzer = new TrackingAnalyzer(PARAMETERS);

        STMTreeRuleNode tree = analyzer.parseSqlQueryTree(
            STMSource.fromString("SELECT * FROM Table1 t WHERE t."),
            listener
        );

        Assertions.assertNotNull(tree);
        Assertions.assertEquals(List.of(PredictionMode.SLL, PredictionMode.LL), analyzer.predictions);
        Assertions.assertEquals(1, analyzer.reusedParserCount);
    }

    @Test
    public void reusesTokenStreamWhenSourceComesFromReader() throws Exception {
        CountingErrorListener listener = new CountingErrorListener();
        TrackingAnalyzer analyzer = new TrackingAnalyzer(PARAMETERS);

        STMTreeRuleNode tree = analyzer.parseSqlQueryTree(
            STMSource.fromReader(new StringReader("(SELECT 1")),
            listener
        );

        Assertions.assertNotNull(tree);
        Assertions.assertEquals(1, analyzer.reusedParserCount);
    }

    @Test
    public void fallsBackWhenSllCannotTokenizeInput() {
        CountingErrorListener listener = new CountingErrorListener();
        TrackingAnalyzer analyzer = new TrackingAnalyzer(PARAMETERS);

        STMTreeRuleNode tree = analyzer.parseSqlQueryTree(STMSource.fromString("DROP TABLE foo^"), listener);

        Assertions.assertNotNull(tree);
        Assertions.assertEquals(List.of(PredictionMode.SLL, PredictionMode.LL), analyzer.predictions);
        Assertions.assertEquals(1, analyzer.reusedParserCount);
        Assertions.assertEquals(1, listener.errorCount);
    }

    @Test
    public void reportsSameLexerErrorsAsFreshLexerWhenReusingTokens() {
        for (String sql : List.of(
            "DROP TABLE foo^",
            "SELECT !x FROM t",
            "SELECT \\ FROM t",
            "SELECT café FROM t",
            ") SELECT ^",
            "^1 ^2 ^3"
        )) {
            List<String> reused = parseErrors(new TrackingAnalyzer(PARAMETERS), sql);
            List<String> fresh = parseErrors(new NoReuseAnalyzer(PARAMETERS), sql);

            Assertions.assertEquals(fresh, reused, sql);
            Assertions.assertFalse(reused.isEmpty(), sql);
        }
    }

    @Test
    public void reportsLexerErrorsToConsoleWhenNoListenerIsProvided() {
        TrackingAnalyzer analyzer = new TrackingAnalyzer(PARAMETERS);

        STMTreeRuleNode tree = analyzer.parseSqlQueryTree(STMSource.fromString("DROP TABLE foo^"), null);

        Assertions.assertNotNull(tree);
        Assertions.assertEquals(List.of(PredictionMode.SLL, PredictionMode.LL), analyzer.predictions);
        Assertions.assertEquals(1, analyzer.reusedParserCount);
    }

    @Test
    public void relexesWhenTokenReuseIsUnsupportedAndLexerFails() {
        RecordingErrorListener listener = new RecordingErrorListener();
        NoReuseAnalyzer analyzer = new NoReuseAnalyzer(PARAMETERS);

        STMTreeRuleNode tree = analyzer.parseSqlQueryTree(STMSource.fromString("DROP TABLE foo^"), listener);

        Assertions.assertNotNull(tree);
        Assertions.assertEquals(List.of(PredictionMode.SLL, PredictionMode.LL), analyzer.predictions);
        Assertions.assertEquals(2, analyzer.prepareParserCount);
        Assertions.assertEquals(1, listener.errors.size());
    }

    @Test
    public void reusesTokenStreamWhenSourceComesFromReaderAndLexerFails() throws Exception {
        RecordingErrorListener listener = new RecordingErrorListener();
        TrackingAnalyzer analyzer = new TrackingAnalyzer(PARAMETERS);

        STMTreeRuleNode tree = analyzer.parseSqlQueryTree(
            STMSource.fromReader(new StringReader("DROP TABLE foo^")),
            listener
        );

        Assertions.assertNotNull(tree);
        Assertions.assertEquals(List.of(PredictionMode.SLL, PredictionMode.LL), analyzer.predictions);
        Assertions.assertEquals(1, analyzer.reusedParserCount);
        Assertions.assertEquals(1, listener.errors.size());
    }

    private static List<String> parseErrors(SQLStandardAnalyzer analyzer, String sql) {
        RecordingErrorListener listener = new RecordingErrorListener();
        analyzer.parseSqlQueryTree(STMSource.fromString(sql), listener);
        return listener.errors;
    }

    private static final class TrackingAnalyzer extends SQLStandardAnalyzer {
        private final List<PredictionMode> predictions = new ArrayList<>();
        private int reusedParserCount;

        private TrackingAnalyzer(@NotNull LSMAnalyzerParameters parameters) {
            super(parameters);
        }

        @NotNull
        @Override
        protected STMTreeRuleNode parseSqlQueryImpl(@NotNull SQLStandardParser parser) {
            predictions.add(parser.getInterpreter().getPredictionMode());
            return super.parseSqlQueryImpl(parser);
        }

        @NotNull
        @Override
        protected SQLStandardParser createParser(
            @NotNull TokenStream tokenStream,
            @NotNull LSMAnalyzerParameters parameters
        ) {
            reusedParserCount++;
            return super.createParser(tokenStream, parameters);
        }
    }

    private static final class NoReuseAnalyzer extends SQLStandardAnalyzer {
        private final List<PredictionMode> predictions = new ArrayList<>();
        private int prepareParserCount;

        private NoReuseAnalyzer(@NotNull LSMAnalyzerParameters parameters) {
            super(parameters);
        }

        @NotNull
        @Override
        protected STMTreeRuleNode parseSqlQueryImpl(@NotNull SQLStandardParser parser) {
            predictions.add(parser.getInterpreter().getPredictionMode());
            return super.parseSqlQueryImpl(parser);
        }

        @Nullable
        @Override
        protected SQLStandardParser createParser(
            @NotNull TokenStream tokenStream,
            @NotNull LSMAnalyzerParameters parameters
        ) {
            return null;
        }

        @NotNull
        @Override
        protected SQLStandardParser prepareParser(
            @NotNull STMSource source,
            @Nullable STMErrorListener errorListener
        ) {
            prepareParserCount++;
            return super.prepareParser(source, errorListener);
        }
    }

    private static final class ForcedFallbackAnalyzer extends SQLStandardAnalyzer {
        private ForcedFallbackAnalyzer(@NotNull LSMAnalyzerParameters parameters) {
            super(parameters);
        }

        @NotNull
        @Override
        protected STMTreeRuleNode parseSqlQueryImpl(@NotNull SQLStandardParser parser) {
            if (parser.getInterpreter().getPredictionMode() == PredictionMode.SLL) {
                parser.notifyErrorListeners("Forced SLL failure");
            }
            return super.parseSqlQueryImpl(parser);
        }
    }

    private static final class RecordingErrorListener extends BaseErrorListener implements STMErrorListener {
        private final List<String> errors = new ArrayList<>();

        @Override
        public void syntaxError(
            @NotNull Recognizer<?, ?> recognizer,
            @Nullable Object offendingSymbol,
            int line,
            int charPositionInLine,
            @NotNull String msg,
            @Nullable RecognitionException e
        ) {
            errors.add(line + ":" + charPositionInLine + ":" + msg);
        }
    }

    private static final class CountingErrorListener extends BaseErrorListener implements STMErrorListener {
        private int errorCount;

        @Override
        public void syntaxError(
            @NotNull Recognizer<?, ?> recognizer,
            @Nullable Object offendingSymbol,
            int line,
            int charPositionInLine,
            @NotNull String msg,
            @Nullable RecognitionException e
        ) {
            errorCount++;
        }
    }
}
