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
package org.jkiss.dbeaver.model.sql.semantics.tracking;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.SQLBatchDelimiterElement;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.model.sql.SQLScriptVariableScope;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryVariablesSubset;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModel;

import java.util.Collection;

/**
 * Provides variables visible to semantic analysis according to their configured lifetime.
 * Script and batch implementations evaluate reconciled document elements by position, while
 * the session implementation accumulates accepted operations independently of document position.
 */
public interface SQLScriptVariablesTracker {
    @NotNull
    static SQLScriptVariablesTracker create(@Nullable SQLScriptVariableScope variableScope) {
        if (variableScope == null) {
            return new NoOpVariablesTracker();
        }
        return switch (variableScope) {
            case SCRIPT -> new ScriptVariablesTracker();
            case BATCH -> new BatchVariablesTracker();
            case SESSION -> new SessionVariablesTracker();
        };
    }

    static boolean isRelevant(@NotNull SQLScriptElement element) {
        return element instanceof SQLBatchDelimiterElement ||
            element instanceof SQLQuery query && query.hasVariableKeyword();
    }

    boolean usesReconciledElements();

    /**
     * Returns the last document position covered by the ordered variable state, inclusively.
     * Trackers without positional scopes don't require gap recovery.
     */
    default int getLastCoveredPosition() {
        return Integer.MAX_VALUE;
    }

    void trackElements(@NotNull Collection<? extends SQLScriptElement> elements);

    default void reconcileElements(
        int offset,
        int length,
        @NotNull Collection<? extends SQLScriptElement> elements
    ) {
        this.trackElements(elements);
    }

    void applyDelta(int offset, int oldLength, int newLength);

    void clear();

    @NotNull
    SQLQueryVariablesSubset getVisibleVariablesAt(@NotNull SQLQueryRecognitionContext context, int position);

    void acceptAnalysisResult(@NotNull SQLScriptElement element, @NotNull SQLQueryModel model);
}
