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
    /**
     * Creates the tracker matching the dialect's variable lifetime.
     *
     * @param variableScope configured variable scope, or {@code null} when tracking is unsupported or disabled
     * @return a tracker that implements the requested lifetime semantics
     */
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

    /**
     * Tests whether an extracted script element can affect ordered variable visibility.
     *
     * @param element extracted script element
     * @return {@code true} for variable statements and batch boundaries
     */
    static boolean isRelevant(@NotNull SQLScriptElement element) {
        return element instanceof SQLBatchDelimiterElement ||
            element instanceof SQLQuery query && query.hasVariableKeyword();
    }

    /**
     * Returns whether this tracker consumes authoritative element ranges produced by script reconciliation.
     * Session and no-op trackers rely only on accepted semantic results and therefore return {@code false}.
     */
    boolean usesReconciledElements();

    /**
     * Returns the last document position covered by authoritative parser observation, inclusively.
     * Trackers without positional scopes don't require observation gap recovery.
     *
     * @return inclusive observation frontier, {@code -1} when no prefix is observed, or
     * {@link Integer#MAX_VALUE} when observation coverage is irrelevant
     */
    default int getLastCoveredPosition() {
        return Integer.MAX_VALUE;
    }

    /**
     * Records elements known to be present without making absence authoritative anywhere else.
     * Implementations may defer applying the elements until variable state is next requested.
     *
     * @param elements extracted elements to upsert
     */
    void trackElements(@NotNull Collection<? extends SQLScriptElement> elements);

    /**
     * Replaces structural knowledge in an authoritative observed range. Previously tracked elements overlapping the
     * range are invalidated, then supplied relevant elements are upserted. Supplied elements outside the range remain
     * authoritative for their own coverage but do not make absence outside the range authoritative.
     * <p>
     * The special range {@code (0, 0)} clears all ordered structural knowledge.
     *
     * @param offset   start of the authoritative range
     * @param length   length of the authoritative range
     * @param elements elements observed by the parser
     */
    default void reconcileElements(
        int offset,
        int length,
        @NotNull Collection<? extends SQLScriptElement> elements
    ) {
        this.trackElements(elements);
    }

    /**
     * Applies a document replacement before subsequent parser results are accepted. Positional trackers invalidate
     * affected structural and derived state while preserving valid prefixes; non-positional trackers ignore the edit.
     *
     * @param offset    replacement offset in the old document
     * @param oldLength replaced length in the old document
     * @param newLength inserted length in the new document
     */
    void applyDelta(int offset, int oldLength, int newLength);

    /**
     * Drops all accumulated structural and semantic variable state.
     */
    void clear();

    /**
     * Resolves variables visible immediately before semantic analysis at the given position. Ordered trackers lazily
     * rebuild invalidated scopes from tracked structural elements and may perform lightweight variable recognition.
     *
     * @param context  recognition context used for lazy rebuilding
     * @param position absolute document position
     * @return variables visible at the position
     */
    @NotNull
    SQLQueryVariablesSubset getVisibleVariablesAt(@NotNull SQLQueryRecognitionContext context, int position);

    /**
     * Supplies a successfully recognized script element. Ordered trackers use accepted variable models to avoid
     * recognizing the same statement again; session trackers accumulate their operations independent of position.
     *
     * @param element analyzed script element
     * @param model   accepted semantic model
     */
    void acceptAnalysisResult(@NotNull SQLScriptElement element, @NotNull SQLQueryModel model);
}
