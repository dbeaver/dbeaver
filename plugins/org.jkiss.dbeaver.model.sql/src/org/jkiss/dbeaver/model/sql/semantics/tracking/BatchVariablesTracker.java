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
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.model.sql.SQLScriptVariableScope;
import org.jkiss.dbeaver.model.sql.semantics.OffsetKeyedTreeMap;

/**
 * Ordered tracker for batch-scoped variables. It tracks both variable operations and batch delimiters,
 * starting a new empty variable scope at each delimiter.
 */
final class BatchVariablesTracker extends AbstractOrderedVariablesTracker {
    BatchVariablesTracker() {
        super(SQLScriptVariableScope.BATCH);
    }

    @Nullable
    @Override
    protected TrackedElement createTrackedElement(@NotNull SQLScriptElement element) {
        if (element instanceof SQLBatchDelimiterElement delimiter) {
            return TrackedElement.batchDelimiter(delimiter);
        }
        return isVariableStatement(element) ? TrackedElement.variableStatement(element) : null;
    }

    @Override
    protected void registerScopeBoundaries(
        @NotNull OffsetKeyedTreeMap<ScopeState> scopes,
        int elementOffset,
        @NotNull TrackedElement element,
        int previouslyValidThrough,
        int position
    ) {
        if (element.batchDelimiter() && elementOffset > previouslyValidThrough && elementOffset <= position) {
            scopes.put(elementOffset, new ScopeState(this.getEmptyVariables()));
        }
    }
}
