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
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryVariableInfo;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryVariablesSubset;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModel;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryVariableStatementModel;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Non-positional tracker for session-scoped variables. It ignores reconciled document elements and
 * edit deltas, accumulating operations from accepted semantic models until cleared; every position
 * observes the same live variable set.
 */
final class SessionVariablesTracker implements SQLScriptVariablesTracker {
    @NotNull
    private final Object stateLock = new Object();
    @NotNull
    private final ConcurrentMap<String, SQLQueryVariableInfo> variables = new ConcurrentHashMap<>();
    @NotNull
    private final SQLQueryVariablesSubset visibleVariables = SQLQueryVariablesSubset.makeLiveView(this.variables);

    @Override
    public boolean usesReconciledElements() {
        return false;
    }

    @Override
    public void trackElements(@NotNull Collection<? extends SQLScriptElement> elements) {
    }

    @Override
    public void applyDelta(int offset, int oldLength, int newLength) {
    }

    @Override
    public void clear() {
        synchronized (this.stateLock) {
            this.variables.clear();
        }
    }

    @NotNull
    @Override
    public SQLQueryVariablesSubset getVisibleVariablesAt(
        @NotNull SQLQueryRecognitionContext context,
        int position
    ) {
        return this.visibleVariables;
    }

    @Override
    public void acceptAnalysisResult(@NotNull SQLScriptElement element, @NotNull SQLQueryModel model) {
        if (!(model.getQueryModel() instanceof SQLQueryVariableStatementModel)) {
            return;
        }
        synchronized (this.stateLock) {
            List<SQLQueryVariableInfo> operations = AbstractSQLScriptVariablesTracker.getIntroducedVariables(model);
            for (SQLQueryVariableInfo operation : operations) {
                this.variables.put(operation.canonicalName(), operation);
            }
        }
    }
}
