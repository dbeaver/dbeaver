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
package org.jkiss.dbeaver.ui.editors.sql.semantics;

import org.eclipse.jface.viewers.StyledString;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionActivityTracker;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionContext;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionRequest;
import org.jkiss.dbeaver.model.sql.semantics.completion.SQLQueryCompletionItemKind;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;

import java.util.EnumMap;

public class SQLQueryCompletionProposalContext {

    // static one-time initialized
    private static final EnumMap<SQLQueryCompletionItemKind, String> registryStyleByItemKind = new EnumMap<>(SQLQueryCompletionItemKind.class) {{
        put(SQLQueryCompletionItemKind.RESERVED, SQLConstants.CONFIG_COLOR_KEYWORD);
        put(SQLQueryCompletionItemKind.SUBQUERY_ALIAS, SQLConstants.CONFIG_COLOR_TABLE_ALIAS);
        put(SQLQueryCompletionItemKind.DERIVED_COLUMN_NAME, SQLConstants.CONFIG_COLOR_COLUMN_DERIVED);
        put(SQLQueryCompletionItemKind.NEW_TABLE_NAME, SQLConstants.CONFIG_COLOR_TABLE);
        put(SQLQueryCompletionItemKind.USED_TABLE_NAME, SQLConstants.CONFIG_COLOR_TABLE);
        put(SQLQueryCompletionItemKind.TABLE_COLUMN_NAME, SQLConstants.CONFIG_COLOR_COLUMN);
    }};

    // per completion request initialized to be in sync with actual preferences, consider listening for preference event
    private final EnumMap<SQLQueryCompletionItemKind, StyledString.Styler> stylerByItemKind = new EnumMap<>(SQLQueryCompletionItemKind.class) {{
        registryStyleByItemKind.forEach((k , v) -> put(k, StyledString.createColorRegistryStyler(v, null)));
    }};

    private final SQLCompletionRequest completionRequest;
    private final boolean insertSpaceAfterProposal;
    private final int requestOffset;

    public SQLQueryCompletionProposalContext(@NotNull SQLCompletionRequest completionRequest, int requestOffset) {
        this.completionRequest = completionRequest;
        this.requestOffset = requestOffset;

        DBCExecutionContext executionContext = completionRequest.getContext().getExecutionContext();
        this.insertSpaceAfterProposal = executionContext == null || executionContext.getDataSource().getContainer().getPreferenceStore().getBoolean(SQLPreferenceConstants.INSERT_SPACE_AFTER_PROPOSALS);
    }

    @Nullable
    public StyledString.Styler getStyler(@NotNull SQLQueryCompletionItemKind itemKind) {
        return this.stylerByItemKind.get(itemKind);
    }

    @NotNull
    public SQLCompletionContext getCompletionContext() {
        return this.completionRequest.getContext();
    }

    public int getRequestOffset() {
        return this.requestOffset;
    }

    public boolean isInsertSpaceAfterProposal() {
        return this.insertSpaceAfterProposal;
    }

    public SQLCompletionActivityTracker getActivityTracker() {
        return this.completionRequest.getActivityTracker();
    }
}
