/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.sql.analyzer.builder.request;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionAnalyzer;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionContext;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionProposalBase;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionRequest;
import org.jkiss.dbeaver.model.sql.parser.SQLRuleManager;
import org.jkiss.dbeaver.model.sql.semantics.SQLDocumentScriptItemSyntaxContext;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryModelRecognizer;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.SQLScriptItemAtOffset;
import org.jkiss.dbeaver.model.sql.semantics.completion.SQLQueryCompletionAnalyzer;
import org.jkiss.dbeaver.model.sql.semantics.completion.SQLQueryCompletionContext;
import org.jkiss.dbeaver.model.sql.semantics.completion.SQLQueryCompletionProposal;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModel;
import org.jkiss.utils.Pair;
import org.junit.Assert;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RequestResult {
    private final DBPDataSource dataSource;

    public RequestResult(@NotNull DBPDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @NotNull
    public List<SQLCompletionProposalBase> request(@NotNull String sql) throws DBException {
        return request(sql, true);
    }

    @NotNull
    public List<SQLCompletionProposalBase> request(@NotNull String sql, boolean simpleMode) throws DBException {
        final SQLCompletionRequest request = prepareCompletionRequest(sql, simpleMode);

        final SQLCompletionAnalyzer analyzer = new SQLCompletionAnalyzer(request);
        analyzer.setCheckNavigatorNodes(false);
        analyzer.runAnalyzer(new VoidProgressMonitor());
        return analyzer.getProposals();
    }

    /**
     * Returns the list of proposals that semantic autocompletion engine returns for the provided text in simple mode
     */
    public Set<String> requestNewStrings(@NotNull String sql) throws DBException {
        return this.requestNewStrings(sql, true);
    }

    /**
     * Returns the list of proposals that semantic autocompletion engine returns for the provided text
     */
    public Set<String> requestNewStrings(@NotNull String sql, boolean simpleMode) throws DBException {
        var r = this.requestNewInternal(sql, simpleMode);
        return r.getSecond().stream().map(p -> {
            try {
                if (p.getReplacementLength() > 0) {
                    String originalString = r.getFirst().getDocument().get(p.getReplacementOffset(), p.getReplacementLength());
                    String replacementFragment = p.getReplacementString().substring(0, originalString.length());
                    if (originalString.equals(replacementFragment) && replacementFragment.endsWith(".")) {
                        return p.getReplacementString().substring(replacementFragment.length());
                    } else {
                        return p.getReplacementString();
                    }
                } else {
                    return p.getReplacementString();
                }
            } catch (BadLocationException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toSet());
    }

    @NotNull
    private Pair<SQLCompletionRequest, List<? extends SQLQueryCompletionProposal>> requestNewInternal(
        @NotNull String sql,
        boolean simpleMode
    ) throws DBException {
        final SQLCompletionRequest request = prepareCompletionRequest(sql, simpleMode);
        final String queryText = request.getActiveQuery().getText();
        final DBRProgressMonitor monitor = new VoidProgressMonitor();

        SQLQueryRecognitionContext recognitionContext = new SQLQueryRecognitionContext(
            monitor,
            request.getContext().getExecutionContext(),
            true,
            true,
            request.getContext().getSyntaxManager(),
            request.getContext().getDataSource().getSQLDialect()
        );
        recognitionContext.reset();
        SQLQueryModel queryModel = SQLQueryModelRecognizer.recognizeQuery(recognitionContext, queryText);
        Assert.assertNotNull(queryModel);

        final SQLQueryCompletionAnalyzer analyzer = getSqlQueryCompletionAnalyzer(queryText, queryModel, request);
        try {
            analyzer.run(monitor);
        } catch (InvocationTargetException | InterruptedException e) {
            throw new DBException("Error while preparing completion proposals", e);
        }
        return Pair.of(request, analyzer.getResult());
    }

    @NotNull
    private static SQLQueryCompletionAnalyzer getSqlQueryCompletionAnalyzer(
        String queryText,
        SQLQueryModel queryModel,
        SQLCompletionRequest request
    ) {
        SQLDocumentScriptItemSyntaxContext scriptItemContext = new SQLDocumentScriptItemSyntaxContext(
            0,
            queryText,
            queryModel,
            queryText.length()
        );
        scriptItemContext.setHasContextBoundaryAtLength(false);

        final SQLQueryCompletionAnalyzer analyzer = new SQLQueryCompletionAnalyzer(
            m -> SQLQueryCompletionContext.prepareCompletionContext(
                new SQLScriptItemAtOffset(0, scriptItemContext),
                request.getDocumentOffset(),
                request.getContext().getExecutionContext(),
                request.getContext().getDataSource().getSQLDialect()
            ),
            request,
            request::getDocumentOffset
        );
        return analyzer;
    }

    @NotNull
    private SQLCompletionRequest prepareCompletionRequest(@NotNull String sql, boolean simpleMode) {
        final DBCExecutionContext executionContext = mock(DBCExecutionContext.class);
        when(executionContext.getDataSource()).thenReturn(dataSource);

        final SQLSyntaxManager syntaxManager = new SQLSyntaxManager();
        syntaxManager.init(dataSource.getSQLDialect(), dataSource.getContainer().getPreferenceStore());

        final SQLRuleManager ruleManager = new SQLRuleManager(syntaxManager);
        ruleManager.loadRules(dataSource, false);

        final Pair<String, Integer> cursor = getCursorFromQuery(sql);

        final Document document = new Document();
        document.set(cursor.getFirst());

        final SQLCompletionContext context = new CompletionContext(
            dataSource,
            syntaxManager,
            ruleManager,
            executionContext
        );

        final SQLCompletionRequest request = new SQLCompletionRequest(
            context,
            document,
            cursor.getSecond(),
            new SQLQuery(context.getDataSource(), cursor.getFirst()),
            simpleMode
        );
        return request;
    }

    @NotNull
    private static Pair<String, Integer> getCursorFromQuery(@NotNull String sql) {
        final int cursor = sql.indexOf('|');
        if (cursor < 0) {
            throw new IllegalArgumentException("Can't locate cursor in query");
        }
        return new Pair<>(sql.substring(0, cursor) + sql.substring(cursor + 1), cursor);
    }

    private static class CompletionContext implements SQLCompletionContext {
        private final DBPDataSource dataSource;
        private final SQLSyntaxManager syntaxManager;
        private final SQLRuleManager ruleManager;
        private final DBCExecutionContext executionContext;

        private CompletionContext(DBPDataSource dataSource, SQLSyntaxManager syntaxManager, SQLRuleManager ruleManager, DBCExecutionContext executionContext) {
            this.dataSource = dataSource;
            this.syntaxManager = syntaxManager;
            this.ruleManager = ruleManager;
            this.executionContext = executionContext;
        }

        @Override
        public DBPDataSource getDataSource() {
            return dataSource;
        }

        @Override
        public DBCExecutionContext getExecutionContext() {
            return executionContext;
        }

        @Override
        public SQLSyntaxManager getSyntaxManager() {
            return syntaxManager;
        }

        @Override
        public SQLRuleManager getRuleManager() {
            return ruleManager;
        }

        @Override
        public boolean isUseFQNames() {
            return false;
        }

        @Override
        public boolean isReplaceWords() {
            return false;
        }

        @Override
        public boolean isShowServerHelp() {
            return false;
        }

        @Override
        public boolean isUseShortNames() {
            return false;
        }

        @Override
        public int getInsertCase() {
            return PROPOSAL_CASE_DEFAULT;
        }

        @Override
        public boolean isSearchProcedures() {
            return false;
        }

        @Override
        public boolean isSearchInsideNames() {
            return false;
        }

        @Override
        public boolean isSortAlphabetically() {
            return false;
        }

        @Override
        public boolean isSearchGlobally() {
            return false;
        }

        @Override
        public boolean isHideDuplicates() {
            return false;
        }

        @Override
        public boolean isShowValues() {
            return true;
        }

        @Override
        public boolean isForceQualifiedColumnNames() { return false; }

        @Override
        public SQLCompletionProposalBase createProposal(@NotNull SQLCompletionRequest request, @NotNull String displayString, @NotNull String replacementString, int cursorPosition, @Nullable DBPImage image, @NotNull DBPKeywordType proposalType, @Nullable String description, @Nullable DBPNamedObject object, @NotNull Map<String, Object> params) {
            return new SQLCompletionProposalBase(request, displayString, replacementString, cursorPosition, image, proposalType, description, object, params);
        }
    }
}
