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
package org.jkiss.dbeaver.model.lsp.context;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionContext;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionProposalBase;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionRequest;
import org.jkiss.dbeaver.model.sql.parser.SQLRuleManager;

import java.util.Map;

public class LspSQLCompletionContext implements SQLCompletionContext {
    private final DBPDataSource dataSource;
    private final DBCExecutionContext executionContext;
    private final SQLSyntaxManager syntaxManager;
    private final SQLRuleManager ruleManager;

    public LspSQLCompletionContext(
        @Nullable DBPDataSource dataSource,
        @Nullable DBCExecutionContext executionContext,
        @NotNull SQLSyntaxManager syntaxManager,
        @NotNull SQLRuleManager ruleManager
    ) {
        this.dataSource = dataSource;
        this.executionContext = executionContext;
        this.syntaxManager = syntaxManager;
        this.ruleManager = ruleManager;
    }

    @Nullable
    @Override
    public DBPDataSource getDataSource() {
        return dataSource;
    }

    @Nullable
    @Override
    public DBCExecutionContext getExecutionContext() {
        return executionContext;
    }

    @NotNull
    @Override
    public SQLSyntaxManager getSyntaxManager() {
        return syntaxManager;
    }

    @NotNull
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
    public boolean isForceQualifiedColumnNames() {
        return false;
    }

    @NotNull
    @Override
    public SQLCompletionProposalBase createProposal(
        @NotNull SQLCompletionRequest request,
        @NotNull String displayString,
        @NotNull String replacementString,
        int cursorPosition,
        @Nullable DBPImage image,
        @NotNull DBPKeywordType proposalType,
        @Nullable String description,
        @Nullable DBPNamedObject object,
        @NotNull Map<String, Object> params
    ) {
        return new SQLCompletionProposalBase(
            request,
            displayString,
            replacementString,
            cursorPosition,
            image,
            proposalType,
            description,
            object,
            params
        );
    }
}
