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
package org.jkiss.dbeaver.model.sql.analyzer;

import org.eclipse.jface.text.Document;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionAnalyzer;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionContext;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionProposalBase;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionRequest;
import org.jkiss.dbeaver.model.sql.parser.SQLRuleManager;
import org.jkiss.dbeaver.model.sql.registry.SQLDialectRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SQLCompletionAnalyzerTest {

    @Mock
    private DBPDataSource dataSource;
    @Mock
    private DBPDataSourceContainer dataSourceContainer;

    private SQLSyntaxManager syntaxManager;
    private SQLRuleManager ruleManager;

    @Before
    public void init() throws DBException {
        final DBPConnectionConfiguration connectionConfiguration = new DBPConnectionConfiguration();
        final DBPPreferenceStore preferenceStore = DBWorkbench.getPlatform().getPreferenceStore();
        final SQLDialect dialect = SQLDialectRegistry.getInstance().getDialect("generic").createInstance();

        when(dataSource.getSQLDialect()).thenReturn(dialect);
        when(dataSource.getContainer()).thenReturn(dataSourceContainer);
        when(dataSourceContainer.getConnectionConfiguration()).thenReturn(connectionConfiguration);
        when(dataSourceContainer.getActualConnectionConfiguration()).thenReturn(connectionConfiguration);
        when(dataSourceContainer.getPreferenceStore()).thenReturn(preferenceStore);
        when(dataSourceContainer.getPreferenceStore()).thenReturn(preferenceStore);

        syntaxManager = new SQLSyntaxManager();
        syntaxManager.init(dataSource);

        ruleManager = new SQLRuleManager(syntaxManager);
        ruleManager.loadRules(dataSource, false);
    }

    @Test
    public void testCompletionKeywordSelect() throws DBException {
        final List<SQLCompletionProposalBase> proposals = getCompletionProposals("SEL", 3);
        Assert.assertFalse(proposals.isEmpty());
        Assert.assertEquals("SELECT", proposals.get(0).getReplacementString());
    }

    private List<SQLCompletionProposalBase> getCompletionProposals(@NotNull String query, int queryOffset) throws DBException {
        final Document document = new Document();
        document.set(query);

        final SQLCompletionContext context = new TestSQLCompletionContext(
            dataSource,
            syntaxManager,
            ruleManager
        );

        final SQLCompletionAnalyzer analyzer = new SQLCompletionAnalyzer(new SQLCompletionRequest(
            context,
            document,
            queryOffset,
            new SQLQuery(context.getDataSource(), query),
            false
        ));

        analyzer.runAnalyzer(new VoidProgressMonitor());
        return analyzer.getProposals();
    }

    private static class TestSQLCompletionContext implements SQLCompletionContext {
        private final DBPDataSource dataSource;
        private final SQLSyntaxManager syntaxManager;
        private final SQLRuleManager ruleManager;

        private TestSQLCompletionContext(DBPDataSource dataSource, SQLSyntaxManager syntaxManager, SQLRuleManager ruleManager) {
            this.dataSource = dataSource;
            this.syntaxManager = syntaxManager;
            this.ruleManager = ruleManager;
        }

        @Override
        public DBPDataSource getDataSource() {
            return dataSource;
        }

        @Override
        public DBCExecutionContext getExecutionContext() {
            return null;
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
