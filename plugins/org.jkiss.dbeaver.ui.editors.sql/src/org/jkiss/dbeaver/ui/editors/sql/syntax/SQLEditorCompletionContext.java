/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionContext;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionProposalBase;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionRequest;
import org.jkiss.dbeaver.model.sql.parser.SQLRuleManager;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants.*;

import java.util.Map;


public class SQLEditorCompletionContext implements SQLCompletionContext {
    private final SQLEditorBase editor;
    private final SQLCompletionObjectNameFormKind objectNameFormKind;

    public SQLEditorCompletionContext(SQLEditorBase editor) {
        this.editor = editor;
        this.objectNameFormKind = SQLCompletionObjectNameFormKind.getFromPreferences(editor.getActivePreferenceStore());
    }

    @NotNull
    public SQLCompletionObjectNameFormKind getObjectNameForm() {
        return this.objectNameFormKind;
    }

    @Override
    public DBPDataSource getDataSource() {
        return editor.getDataSource();
    }

    @Nullable
    @Override
    public DBCExecutionContext getExecutionContext() {
        return editor.getExecutionContext();
    }

    @Override
    public SQLSyntaxManager getSyntaxManager() {
        return editor.getSyntaxManager();
    }

    @Override
    public SQLRuleManager getRuleManager() {
        return editor.getRuleManager();
    }

    @Override
    public boolean isUseFQNames() {
        return objectNameFormKind.qualified;
    }

    @Override
    public boolean isReplaceWords() {
        return getActivePreferenceStore().getBoolean(SQLPreferenceConstants.PROPOSAL_REPLACE_WORD);
    }

    @Override
    public boolean isShowServerHelp() {
        return getActivePreferenceStore().getBoolean(SQLPreferenceConstants.SHOW_SERVER_HELP_TOPICS);
    }

    @Override
    public boolean isUseShortNames() {
        return objectNameFormKind.unqualified;
    }

    @Override
    public int getInsertCase() {
        return getActivePreferenceStore().getInt(SQLPreferenceConstants.PROPOSAL_INSERT_CASE);
    }

    @Override
    public boolean isSearchProcedures() {
        return getActivePreferenceStore().getBoolean(SQLPreferenceConstants.SHOW_COLUMN_PROCEDURES);
    }

    @Override
    public boolean isSearchInsideNames() {
        return getActivePreferenceStore().getBoolean(SQLPreferenceConstants.PROPOSALS_MATCH_CONTAINS);
    }

    @Override
    public boolean isSortAlphabetically() {
        return getActivePreferenceStore().getBoolean(SQLPreferenceConstants.PROPOSAL_SORT_ALPHABETICALLY);
    }

    @Override
    public boolean isSearchGlobally() {
        return getActivePreferenceStore().getBoolean(SQLPreferenceConstants.USE_GLOBAL_ASSISTANT);
    }

    @Override
    public boolean isHideDuplicates() {
        return getActivePreferenceStore().getBoolean(SQLPreferenceConstants.HIDE_DUPLICATE_PROPOSALS);
    }

    @Override
    public boolean isShowValues() {
        return getActivePreferenceStore().getBoolean(SQLPreferenceConstants.SHOW_VALUES);
    }

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
        @NotNull Map<String, Object> params) {
        return new SQLCompletionProposal(
            request,
            displayString,
            replacementString,
            cursorPosition,
            image,
            proposalType,
            description,
            object,
            params);
    }

    private DBPPreferenceStore getActivePreferenceStore() {
        return editor.getActivePreferenceStore();
    }
}
