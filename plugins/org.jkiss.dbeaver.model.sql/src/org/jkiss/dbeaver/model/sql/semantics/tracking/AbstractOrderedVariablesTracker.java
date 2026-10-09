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
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.model.sql.SQLScriptVariableScope;
import org.jkiss.dbeaver.model.sql.semantics.OffsetKeyedTreeMap;
import org.jkiss.dbeaver.model.sql.semantics.OffsetKeyedTreeMap.NodesIterator;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryModelRecognizer;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryRecognitionContext;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryVariableInfo;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryVariablesSubset;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModel;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryVariableStatementModel;
import org.jkiss.dbeaver.utils.ListNode;

import java.util.*;

/**
 * Builds positional variable scopes from the elements maintained by
 * {@link AbstractSQLScriptVariablesTracker}. It evaluates elements in document order, caches variable
 * snapshots, and preserves valid prefixes when later positions are invalidated. Subclasses select
 * relevant elements and define where a new variable scope begins.
 */
abstract class AbstractOrderedVariablesTracker extends AbstractSQLScriptVariablesTracker {
    protected static class ScopeState {
        @NotNull
        private final OffsetKeyedTreeMap<SQLQueryVariablesSubset> snapshots = new OffsetKeyedTreeMap<>();

        protected ScopeState(@NotNull SQLQueryVariablesSubset initialVariables) {
            this.snapshots.put(0, initialVariables);
        }

        @NotNull
        private SQLQueryVariablesSubset getVisibleVariablesAt(int offset) {
            OffsetKeyedTreeMap.ValueAndOffset<SQLQueryVariablesSubset> snapshot = findFloor(this.snapshots, offset);
            if (snapshot == null) {
                throw new IllegalStateException("Variable scope has no initial state");
            }
            return snapshot.value;
        }
    }

    @NotNull
    private final SQLQueryVariablesSubset emptyVariables;
    @NotNull
    private final OffsetKeyedTreeMap<ScopeState> scopes = new OffsetKeyedTreeMap<>();
    private boolean scopesDirty = true;
    private int scopesValidThrough = -1;

    protected AbstractOrderedVariablesTracker(@NotNull SQLScriptVariableScope variableScope) {
        this.emptyVariables = SQLQueryVariablesSubset.makeSnapshot(variableScope, Collections.emptyMap());
    }

    @Override
    public final boolean usesReconciledElements() {
        return true;
    }

    @Override
    public final int getLastCoveredPosition() {
        synchronized (this.getStateLock()) {
            this.consumeQueuedElements();
            return this.scopesDirty ? -1 : this.scopesValidThrough;
        }
    }

    @NotNull
    @Override
    public final SQLQueryVariablesSubset getVisibleVariablesAt(
        @NotNull SQLQueryRecognitionContext context,
        int position
    ) {
        synchronized (this.getStateLock()) {
            this.consumeQueuedElements();
            if (!this.ensureScopesThrough(context, position)) {
                return this.emptyVariables;
            }
            OffsetKeyedTreeMap.ValueAndOffset<ScopeState> scope = findFloor(this.scopes, position);
            return scope == null
                ? this.emptyVariables
                : scope.value.getVisibleVariablesAt(position - scope.offset);
        }
    }

    @Override
    public final void acceptAnalysisResult(@NotNull SQLScriptElement element, @NotNull SQLQueryModel model) {
        if (!(model.getQueryModel() instanceof SQLQueryVariableStatementModel)) {
            return;
        }
        TrackedElement analyzedElement = TrackedElement.analyzedVariableStatement(element);
        synchronized (this.getStateLock()) {
            this.consumeQueuedElements();
            int itemOffset = element.getOffset();
            int itemEnd = itemOffset + element.getLength();
            TrackedElement trackedElement = this.getTrackedElements().find(itemOffset);
            boolean newlyTracked = trackedElement == null;
            if (newlyTracked) {
                this.getTrackedElements().put(itemOffset, analyzedElement);
                this.trackedElementChanged(itemOffset);
            } else if (!analyzedElement.equals(trackedElement)) {
                return;
            }
            if (this.scopesDirty || this.scopesValidThrough < itemOffset) {
                return;
            }
            if (newlyTracked && this.scopesValidThrough > itemOffset) {
                this.invalidateDerivedStateAfter(itemOffset);
            } else if (itemEnd <= this.scopesValidThrough) {
                return;
            }

            this.registerScopeBoundaries(
                this.scopes,
                itemOffset,
                analyzedElement,
                this.scopesValidThrough,
                itemEnd
            );
            OffsetKeyedTreeMap.ValueAndOffset<ScopeState> scope = findFloor(this.scopes, itemOffset);
            if (scope == null) {
                return;
            }
            SQLQueryVariablesSubset inputVariables = scope.value.getVisibleVariablesAt(itemOffset - scope.offset);
            Map<String, SQLQueryVariableInfo> effectiveVariables = new HashMap<>();
            resetEffectiveVariables(effectiveVariables, inputVariables);
            registerVariableOperations(
                scope,
                itemOffset,
                inputVariables,
                effectiveVariables,
                getIntroducedVariables(model)
            );
            this.scopesValidThrough = itemEnd;
        }
    }

    private void invalidateDerivedStateAfter(int offset) {
        // Preserve the statement input at this offset so the accepted model can be applied without reanalysis.
        OffsetKeyedTreeMap.ValueAndOffset<ScopeState> currentScope = findFloor(this.scopes, offset);
        if (currentScope != null) {
            removeAtOrAfter(currentScope.value.snapshots, offset - currentScope.offset + 1);
        }
        removeAtOrAfter(this.scopes, offset + 1);
        this.scopesValidThrough = offset;
    }

    @Override
    protected final void invalidateDerivedStateFrom(int offset) {
        if (this.scopesDirty || offset > this.scopesValidThrough) {
            return;
        }
        if (offset <= 0) {
            this.resetDerivedState();
            return;
        }

        OffsetKeyedTreeMap.ValueAndOffset<ScopeState> precedingScope = findFloor(this.scopes, offset - 1);
        if (precedingScope != null) {
            removeAtOrAfter(precedingScope.value.snapshots, offset - precedingScope.offset);
        }
        removeAtOrAfter(this.scopes, offset);
        this.scopesValidThrough = offset - 1;
    }

    private void resetDerivedState() {
        this.scopes.clear();
        this.scopesDirty = true;
        this.scopesValidThrough = -1;
    }

    @Override
    protected final void clearDerivedState() {
        this.resetDerivedState();
    }

    protected void registerScopeBoundaries(
        @NotNull OffsetKeyedTreeMap<ScopeState> scopes,
        int elementOffset,
        @NotNull TrackedElement element,
        int previouslyValidThrough,
        int position
    ) {
    }

    @NotNull
    protected final SQLQueryVariablesSubset getEmptyVariables() {
        return this.emptyVariables;
    }

    private boolean ensureScopesThrough(@NotNull SQLQueryRecognitionContext context, int position) {
        if (!this.scopesDirty && position <= this.scopesValidThrough) {
            return true;
        }
        if (this.scopesDirty) {
            this.scopes.clear();
        }
        int previouslyValidThrough = this.scopesDirty ? -1 : this.scopesValidThrough;
        if (this.scopesDirty) {
            this.scopes.put(0, new ScopeState(this.emptyVariables));
        }
        int registeredThrough = this.registerElementsInRange(
            context,
            this.scopes,
            previouslyValidThrough,
            position
        );
        this.scopesDirty = false;
        this.scopesValidThrough = Math.max(this.scopesValidThrough, registeredThrough);
        return registeredThrough >= position && !context.getMonitor().isCanceled();
    }

    private int registerElementsInRange(
        @NotNull SQLQueryRecognitionContext context,
        @NotNull OffsetKeyedTreeMap<ScopeState> scopes,
        int previouslyValidThrough,
        int position
    ) {
        NodesIterator<TrackedElement> iterator = this.getTrackedElements().nodesIteratorAt(previouslyValidThrough);
        TrackedElement element = iterator.getCurrValue();
        if (element == null && !iterator.prev() && !iterator.next()) {
            return position;
        }
        Map<String, SQLQueryVariableInfo> effectiveVariables = new HashMap<>();
        ScopeState effectiveVariablesScope = null;
        SQLQueryVariablesSubset inputVariables = null;
        while ((element = iterator.getCurrValue()) != null && element.coverageStart(iterator.getCurrOffset()) <= position) {
            int elementOffset = iterator.getCurrOffset();
            this.registerScopeBoundaries(scopes, elementOffset, element, previouslyValidThrough, position);
            if (element.variableStatementText() != null &&
                elementOffset >= previouslyValidThrough && elementOffset < position
            ) {
                OffsetKeyedTreeMap.ValueAndOffset<ScopeState> scope = findFloor(scopes, elementOffset);
                if (scope != null) {
                    if (effectiveVariablesScope != scope.value) {
                        effectiveVariablesScope = scope.value;
                        inputVariables = scope.value.getVisibleVariablesAt(elementOffset - scope.offset);
                        resetEffectiveVariables(effectiveVariables, inputVariables);
                    }
                    if (context.getMonitor().isCanceled()) {
                        return elementOffset;
                    }
                    SQLQueryModel model = SQLQueryModelRecognizer.recognizeVariableStatement(
                        context,
                        element.variableStatementText(),
                        inputVariables
                    );
                    if (context.getMonitor().isCanceled()) {
                        return elementOffset;
                    }
                    inputVariables = registerVariableOperations(
                        scope,
                        elementOffset,
                        Objects.requireNonNull(inputVariables),
                        effectiveVariables,
                        getIntroducedVariables(model)
                    );
                }
            }
            if (!iterator.next()) {
                break;
            }
        }
        return position;
    }

    @NotNull
    private static SQLQueryVariablesSubset registerVariableOperations(
        @NotNull OffsetKeyedTreeMap.ValueAndOffset<ScopeState> scope,
        int elementOffset,
        @NotNull SQLQueryVariablesSubset inputVariables,
        @NotNull Map<String, SQLQueryVariableInfo> effectiveVariables,
        @NotNull List<SQLQueryVariableInfo> operations
    ) {
        SQLQueryVariablesSubset variables = inputVariables;
        for (SQLQueryVariableInfo operation : operations) {
            effectiveVariables.put(operation.canonicalName(), operation);
            variables = SQLQueryVariablesSubset.makeSnapshot(variables.getScriptVariableScope(), effectiveVariables);
            scope.value.snapshots.put(elementOffset + operation.relativeOffset() - scope.offset, variables);
        }
        return variables;
    }

    private static void resetEffectiveVariables(
        @NotNull Map<String, SQLQueryVariableInfo> effectiveVariables,
        @NotNull SQLQueryVariablesSubset variables
    ) {
        effectiveVariables.clear();
        for (SQLQueryVariableInfo variable : variables.getVariables()) {
            effectiveVariables.put(variable.canonicalName(), variable);
        }
    }

    private static <T> void removeAtOrAfter(@NotNull OffsetKeyedTreeMap<T> map, int offset) {
        ListNode<Integer> offsetsToRemove = null;
        NodesIterator<T> iterator = map.nodesIteratorAt(offset);
        if (iterator.getCurrValue() == null && !iterator.next()) {
            return;
        }
        do {
            offsetsToRemove = ListNode.push(offsetsToRemove, iterator.getCurrOffset());
        } while (iterator.next());
        for (ListNode<Integer> node = offsetsToRemove; node != null; node = node.next) {
            map.removeAt(node.data);
        }
    }

    @Nullable
    protected static <T> OffsetKeyedTreeMap.ValueAndOffset<T> findFloor(
        @NotNull OffsetKeyedTreeMap<T> map,
        int offset
    ) {
        NodesIterator<T> iterator = map.nodesIteratorAt(offset);
        T value = iterator.getCurrValue();
        if (value == null && iterator.prev()) {
            value = iterator.getCurrValue();
        }
        return value == null ? null : new OffsetKeyedTreeMap.ValueAndOffset<>(value, iterator.getCurrOffset());
    }
}
