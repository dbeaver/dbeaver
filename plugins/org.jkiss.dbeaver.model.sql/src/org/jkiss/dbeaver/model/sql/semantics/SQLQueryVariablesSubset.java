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
package org.jkiss.dbeaver.model.sql.semantics;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.SQLScriptVariableScope;
import org.jkiss.dbeaver.model.sql.semantics.context.SQLQueryExprType;
import org.jkiss.dbeaver.utils.ListNode;

import java.util.*;
import java.util.concurrent.ConcurrentMap;

/**
 * Read-only set of script variables visible at a particular document position.
 * Ordered scopes use immutable snapshots, while session scope uses a live view of the tracker's state.
 * Incremental semantic-analysis updates are kept locally until their result is accepted by the tracker.
 */
public final class SQLQueryVariablesSubset {
    private sealed interface VariablesView permits SnapshotVariablesView, SessionVariablesView {
        @NotNull
        Collection<SQLQueryVariableInfo> getVariables();

        @Nullable
        SQLQueryVariableInfo resolve(@NotNull String canonicalName);

        @NotNull
        VariablesView append(@NotNull SQLQueryVariableInfo variable);
    }

    private static final class SnapshotVariablesView implements VariablesView {
        @NotNull
        private final Map<String, SQLQueryVariableInfo> variables;

        private SnapshotVariablesView(@NotNull Map<String, SQLQueryVariableInfo> variables) {
            this.variables = variables;
        }

        @NotNull
        @Override
        public Collection<SQLQueryVariableInfo> getVariables() {
            return Collections.unmodifiableCollection(this.variables.values());
        }

        @Nullable
        @Override
        public SQLQueryVariableInfo resolve(@NotNull String canonicalName) {
            return this.variables.get(canonicalName);
        }

        @NotNull
        @Override
        public VariablesView append(@NotNull SQLQueryVariableInfo variable) {
            Map<String, SQLQueryVariableInfo> variables = new HashMap<>(this.variables);
            variables.put(variable.canonicalName(), variable);
            return new SnapshotVariablesView(Collections.unmodifiableMap(variables));
        }
    }

    private static final class SessionVariablesView implements VariablesView {
        @NotNull
        private final ConcurrentMap<String, SQLQueryVariableInfo> sessionVariables;
        @NotNull
        private final Map<String, SQLQueryVariableInfo> localVariables;

        private SessionVariablesView(@NotNull ConcurrentMap<String, SQLQueryVariableInfo> sessionVariables) {
            this(sessionVariables, Collections.emptyMap());
        }

        private SessionVariablesView(
            @NotNull ConcurrentMap<String, SQLQueryVariableInfo> sessionVariables,
            @NotNull Map<String, SQLQueryVariableInfo> localVariables
        ) {
            this.sessionVariables = sessionVariables;
            this.localVariables = localVariables;
        }

        @NotNull
        @Override
        public Collection<SQLQueryVariableInfo> getVariables() {
            if (this.localVariables.isEmpty()) {
                return Collections.unmodifiableCollection(this.sessionVariables.values());
            }
            Map<String, SQLQueryVariableInfo> variables = new HashMap<>(this.sessionVariables);
            variables.putAll(this.localVariables);
            return Collections.unmodifiableCollection(variables.values());
        }

        @Nullable
        @Override
        public SQLQueryVariableInfo resolve(@NotNull String canonicalName) {
            SQLQueryVariableInfo variable = this.localVariables.get(canonicalName);
            return variable == null ? this.sessionVariables.get(canonicalName) : variable;
        }

        @NotNull
        @Override
        public VariablesView append(@NotNull SQLQueryVariableInfo variable) {
            Map<String, SQLQueryVariableInfo> variables = new HashMap<>(this.localVariables);
            variables.put(variable.canonicalName(), variable);
            return new SessionVariablesView(
                this.sessionVariables,
                Collections.unmodifiableMap(variables)
            );
        }
    }

    public static final SQLQueryVariablesSubset EMPTY = makeSnapshot(SQLScriptVariableScope.BATCH, Map.of());

    @NotNull
    private final SQLScriptVariableScope scriptVariableScope;
    @Nullable
    private final ListNode<SQLQueryVariableInfo> introducedVariables;
    @NotNull
    private final VariablesView variables;

    private SQLQueryVariablesSubset(
        @NotNull SQLScriptVariableScope scriptVariableScope,
        @Nullable ListNode<SQLQueryVariableInfo> introducedVariables,
        @NotNull VariablesView variables
    ) {
        this.scriptVariableScope = scriptVariableScope;
        this.introducedVariables = introducedVariables;
        this.variables = variables;
    }

    @NotNull
    public static SQLQueryVariablesSubset makeSnapshot(
        @NotNull SQLScriptVariableScope scriptVariableScope,
        @NotNull Map<String, SQLQueryVariableInfo> variablesByCanonicalName
    ) {
        Map<String, SQLQueryVariableInfo> variables = variablesByCanonicalName.isEmpty()
            ? Collections.emptyMap()
            : Collections.unmodifiableMap(new HashMap<>(variablesByCanonicalName));
        return new SQLQueryVariablesSubset(scriptVariableScope, null, new SnapshotVariablesView(variables));
    }

    /**
     * Creates a read-only live view of session variables. Changes to the supplied map are reflected immediately.
     */
    @NotNull
    public static SQLQueryVariablesSubset makeLiveView(
        @NotNull ConcurrentMap<String, SQLQueryVariableInfo> variables
    ) {
        return new SQLQueryVariablesSubset(
            SQLScriptVariableScope.SESSION,
            null,
            new SessionVariablesView(variables)
        );
    }

    @NotNull
    public Collection<SQLQueryVariableInfo> getVariables() {
        return this.variables.getVariables();
    }

    /**
     * Returns variable operations introduced during semantic analysis, ordered from newest to oldest.
     */
    @Nullable
    public ListNode<SQLQueryVariableInfo> getIntroducedVariables() {
        return this.introducedVariables;
    }

    @NotNull
    public SQLScriptVariableScope getScriptVariableScope() {
        return this.scriptVariableScope;
    }

    @Nullable
    public SQLQueryVariableInfo resolve(@NotNull SQLQuerySymbolEntry name) {
        return this.variables.resolve(canonicalize(name.getName()));
    }

    @NotNull
    public SQLQueryVariablesSubset appendVariable(
        @NotNull SQLQuerySymbolEntry name,
        @NotNull SQLQueryExprType type,
        @NotNull SQLQueryVariableInfo.OperationKind operationKind,
        int relativeOffset
    ) {
        String canonicalName = canonicalize(name.getName());
        if (operationKind == SQLQueryVariableInfo.OperationKind.ASSIGNMENT &&
            this.scriptVariableScope != SQLScriptVariableScope.SESSION &&
            this.variables.resolve(canonicalName) == null
        ) {
            return this;
        }
        SQLQueryVariableInfo variable = new SQLQueryVariableInfo(
            name.getRawName(),
            canonicalName,
            type,
            this.scriptVariableScope,
            operationKind,
            relativeOffset
        );
        return new SQLQueryVariablesSubset(
            this.scriptVariableScope,
            ListNode.push(this.introducedVariables, variable),
            this.variables.append(variable)
        );
    }

    @NotNull
    private static String canonicalize(@NotNull String name) {
        // Variable folding can be dialect-specific; retain the existing case-insensitive behavior for now.
        return name.toLowerCase(Locale.ENGLISH);
    }
}
