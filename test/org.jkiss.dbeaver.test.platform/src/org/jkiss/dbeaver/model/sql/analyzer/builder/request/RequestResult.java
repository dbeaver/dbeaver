/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

import org.eclipse.jface.text.Document;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionAnalyzer;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionContext;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionProposalBase;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionRequest;
import org.jkiss.dbeaver.model.sql.parser.SQLRuleManager;
import org.jkiss.utils.Pair;

import java.util.List;
import java.util.Map;

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
        final DBCExecutionContext executionContext = mock(DBCExecutionContext.class);
        when(executionContext.getDataSource()).thenReturn(dataSource);

        final SQLSyntaxManager syntaxManager = new SQLSyntaxManager();
        syntaxManager.init(dataSource);

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

        final SQLCompletionAnalyzer analyzer = new SQLCompletionAnalyzer(request);
        analyzer.runAnalyzer(new VoidProgressMonitor());
        return analyzer.getProposals();
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
        public SQLCompletionProposalBase createProposal(@NotNull SQLCompletionRequest request, @NotNull String displayString, @NotNull String replacementString, int cursorPosition, @Nullable DBPImage image, @NotNull DBPKeywordType proposalType, @Nullable String description, @Nullable DBPNamedObject object, @NotNull Map<String, Object> params) {
            return new SQLCompletionProposalBase(this, request.getWordDetector(), displayString, replacementString, cursorPosition, image, proposalType, description, object, params);
        }
    }
}
