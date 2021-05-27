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
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionAnalyzer;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionContext;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionProposalBase;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionRequest;
import org.jkiss.dbeaver.model.sql.parser.SQLRuleManager;
import org.jkiss.dbeaver.model.sql.registry.SQLDialectRegistry;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SQLCompletionRequestBuilder {
    private final DataSource dataSource = Mockito.mock(DataSource.class);
    private final List<DBSObject> entities = new ArrayList<>();
    private String dialect = "generic";
    private String contentType = null;

    @NotNull
    public TableBuilder addTable(@NotNull String name) throws DBException {
        return new TableBuilder(this, name);
    }

    @NotNull
    public SQLCompletionRequestBuilder setDialect(@NotNull String dialect) {
        this.dialect = dialect;
        return this;
    }

    @NotNull
    public SQLCompletionRequestBuilder setContentType(@NotNull String contentType) {
        this.contentType = contentType;
        return this;
    }

    @NotNull
    public List<SQLCompletionProposalBase> request(@NotNull String sql, int offset) throws DBException {
        final DBPConnectionConfiguration connectionConfiguration = new DBPConnectionConfiguration();
        final DBPPreferenceStore preferenceStore = DBWorkbench.getPlatform().getPreferenceStore();
        final SQLDialectRegistry dialectRegistry = SQLDialectRegistry.getInstance();

        final DBPDataSourceContainer dataSourceContainer = Mockito.mock(DBPDataSourceContainer.class);
        when(dataSourceContainer.getConnectionConfiguration()).thenReturn(connectionConfiguration);
        when(dataSourceContainer.getActualConnectionConfiguration()).thenReturn(connectionConfiguration);
        when(dataSourceContainer.getPreferenceStore()).thenReturn(preferenceStore);

        when(dataSource.getSQLDialect()).thenReturn(dialectRegistry.getDialect(dialect).createInstance());
        when(dataSource.getContainer()).thenReturn(dataSourceContainer);
        when(dataSource.getChild(any(), any())).then(x -> DBUtils.findObject(entities, x.getArgumentAt(1, String.class)));
        when(dataSource.getChildren(any())).then(x -> entities);

        final DBCExecutionContext executionContext = Mockito.mock(DBCExecutionContext.class);

        final SQLSyntaxManager syntaxManager = new SQLSyntaxManager();
        syntaxManager.init(dataSource);

        final SQLRuleManager ruleManager = new SQLRuleManager(syntaxManager);
        ruleManager.loadRules(dataSource, false);

        final Document document = new Document();
        document.set(sql);

        final SQLCompletionContext context = new SQLCompletionContextImpl(
            dataSource,
            syntaxManager,
            ruleManager,
            executionContext
        );

        final SQLCompletionRequest request = new SQLCompletionRequest(
            context,
            document,
            offset,
            new SQLQuery(context.getDataSource(), sql),
            false
        );

        if (CommonUtils.isNotEmpty(contentType)) {
            request.setContentType(contentType);
        }

        final SQLCompletionAnalyzer analyzer = new SQLCompletionAnalyzer(request);
        analyzer.runAnalyzer(new VoidProgressMonitor());
        return analyzer.getProposals();
    }

    public static class TableBuilder {
        private final SQLCompletionRequestBuilder builder;
        private final DBSEntity entity;
        private final List<DBSEntityAttribute> attributes;

        private TableBuilder(@NotNull SQLCompletionRequestBuilder builder, @NotNull String name) throws DBException {
            this.builder = builder;
            this.entity = mock(DBSEntity.class);
            this.attributes = new ArrayList<>();

            when(entity.getEntityType()).thenReturn(DBSEntityType.TABLE);
            when(entity.getName()).thenReturn(name);
            when(entity.getDataSource()).thenReturn(builder.dataSource);
            when(entity.getParentObject()).thenReturn(builder.dataSource);

            when(entity.getAttributes(any())).then(x -> attributes);
            when(entity.getAttribute(any(), any())).then(x -> DBUtils.findObject(attributes, x.getArgumentAt(1, String.class)));
        }

        @NotNull
        public TableBuilder addAttribute(@NotNull String name) {
            final DBSEntityAttribute attribute = Mockito.mock(DBSEntityAttribute.class);
            when(attribute.getParentObject()).thenReturn(entity);
            when(attribute.getDataSource()).thenReturn(builder.dataSource);
            when(attribute.getName()).thenReturn(name);
            when(attribute.getTypeName()).thenReturn("Unknown");
            when(attribute.getDataKind()).thenReturn(DBPDataKind.UNKNOWN);
            attributes.add(attribute);
            return this;
        }

        @NotNull
        public SQLCompletionRequestBuilder build() {
            if (!builder.entities.contains(entity)) {
                builder.entities.add(entity);
            }
            return builder;
        }
    }

    public static abstract class DataSource implements DBPDataSource, DBSObjectContainer {
    }

    private static class SQLCompletionContextImpl implements SQLCompletionContext {
        private final DBPDataSource dataSource;
        private final SQLSyntaxManager syntaxManager;
        private final SQLRuleManager ruleManager;
        private final DBCExecutionContext executionContext;

        private SQLCompletionContextImpl(DBPDataSource dataSource, SQLSyntaxManager syntaxManager, SQLRuleManager ruleManager, DBCExecutionContext executionContext) {
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
