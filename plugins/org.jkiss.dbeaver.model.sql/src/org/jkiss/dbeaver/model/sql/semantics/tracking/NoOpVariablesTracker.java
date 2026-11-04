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
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryVariablesSubset;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModel;

import java.util.Collection;

/**
 * Tracker used when the dialect has no script-variable scope. It retains no state and always exposes
 * an empty variable set.
 */
final class NoOpVariablesTracker implements SQLScriptVariablesTracker {
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
    }

    @NotNull
    @Override
    public SQLQueryVariablesSubset getVisibleVariablesAt(
        @NotNull SQLQueryRecognitionContext context,
        int position
    ) {
        return SQLQueryVariablesSubset.EMPTY;
    }

    @Override
    public void acceptAnalysisResult(@NotNull SQLScriptElement element, @NotNull SQLQueryModel model) {
    }
}
