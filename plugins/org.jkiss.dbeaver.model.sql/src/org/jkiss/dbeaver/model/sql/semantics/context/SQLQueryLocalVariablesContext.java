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
package org.jkiss.dbeaver.model.sql.semantics.context;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.semantics.SQLQuerySymbolEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Immutable lexical snapshots of locally bound names, indexed by dialect-defined operation namespaces. */
public final class SQLQueryLocalVariablesContext {
    @NotNull
    private final SQLDialect dialect;
    @NotNull
    private final List<SQLDialect.LocalVariableNamespace> namespaces;
    @Nullable
    private final SQLQueryLocalVariablesContext parent;
    @NotNull
    private final Map<Set<SQLDialect.LocalVariableCapability>, Map<String, SQLQueryLocalVariableInfo>> current;
    @NotNull
    private final Map<Set<SQLDialect.LocalVariableCapability>, Map<String, SQLQueryLocalVariableInfo>> program;

    /**
     * Start with no declarations and validate that the dialect assigns every capability to exactly one namespace.
     * A capability must have an unambiguous lookup bucket even when a binding has several capabilities.
     */
    public SQLQueryLocalVariablesContext(@NotNull SQLDialect dialect) {
        this(dialect, List.copyOf(dialect.getLocalVariableNamespaces()), null, Map.of(), Map.of());
        EnumSet<SQLDialect.LocalVariableCapability> seen = EnumSet.noneOf(SQLDialect.LocalVariableCapability.class);
        for (SQLDialect.LocalVariableNamespace namespace : this.namespaces) {
            if (!Collections.disjoint(seen, namespace.capabilities())) {
                throw new IllegalArgumentException("A local binding capability belongs to multiple namespaces");
            }
            seen.addAll(namespace.capabilities());
        }
        if (!seen.equals(EnumSet.allOf(SQLDialect.LocalVariableCapability.class))) {
            throw new IllegalArgumentException("A local binding capability has no namespace");
        }
    }

    /** Share immutable namespace indexes between snapshots; only the maps changed by an operation are replaced. */
    private SQLQueryLocalVariablesContext(
        @NotNull SQLDialect dialect,
        @NotNull List<SQLDialect.LocalVariableNamespace> namespaces,
        @Nullable SQLQueryLocalVariablesContext parent,
        @NotNull Map<Set<SQLDialect.LocalVariableCapability>, Map<String, SQLQueryLocalVariableInfo>> current,
        @NotNull Map<Set<SQLDialect.LocalVariableCapability>, Map<String, SQLQueryLocalVariableInfo>> program
    ) {
        this.dialect = dialect;
        this.namespaces = namespaces;
        this.parent = parent;
        this.current = current;
        this.program = program;
    }

    /** Begin a separate program so neither lexical nor program-visible declarations leak from the previous one. */
    @NotNull
    public SQLQueryLocalVariablesContext resetForProgram() {
        return new SQLQueryLocalVariablesContext(this.dialect, this.namespaces, null, Map.of(), Map.of());
    }

    /** Add a lexical frame while retaining the program-wide declarations already visible at this point. */
    @NotNull
    public SQLQueryLocalVariablesContext withNestedBlock() {
        return new SQLQueryLocalVariablesContext(this.dialect, this.namespaces, this, Map.of(), this.program);
    }

    /**
     * Carry program-visible declarations out of a nested block without exporting its lexical declarations.
     * Preserve names already present in this snapshot when merging branches, so later branches cannot replace them.
     */
    @NotNull
    public SQLQueryLocalVariablesContext withProgramVariablesFrom(@NotNull SQLQueryLocalVariablesContext nested) {
        if (this.program == nested.program) {
            return this;
        }
        Map<Set<SQLDialect.LocalVariableCapability>, Map<String, SQLQueryLocalVariableInfo>> merged = new HashMap<>(this.program);
        nested.program.forEach((mask, bindings) -> {
            Map<String, SQLQueryLocalVariableInfo> names = new HashMap<>(merged.getOrDefault(mask, Map.of()));
            bindings.forEach(names::putIfAbsent);
            merged.put(mask, Map.copyOf(names));
        });
        return new SQLQueryLocalVariablesContext(this.dialect, this.namespaces, this.parent, this.current, Map.copyOf(merged));
    }

    /**
     * Index a declaration in every namespace intersecting its capabilities, in the lexical or program snapshot
     * selected by the dialect. Reject a collision in any such namespace before publishing any of the new maps,
     * so a multi-capability binding cannot become visible for only some of its operations.
     */
    @NotNull
    public SQLQueryLocalVariablesContext withLocalVariable(@NotNull SQLQueryLocalVariableInfo binding) {
        String key = this.key(binding.declaration());
        boolean programVisible = this.dialect.getLocalVariableVisibility(binding.kind()) == SQLDialect.LocalVariableVisibility.PROGRAM;
        Map<Set<SQLDialect.LocalVariableCapability>, Map<String, SQLQueryLocalVariableInfo>> target = programVisible ? this.program : this.current;
        Map<Set<SQLDialect.LocalVariableCapability>, Map<String, SQLQueryLocalVariableInfo>> updated = new HashMap<>(target);
        boolean inserted = false;
        for (SQLDialect.LocalVariableNamespace namespace : this.namespaces) {
            if (!Collections.disjoint(binding.kind().capabilities(), namespace.capabilities())) {
                Map<String, SQLQueryLocalVariableInfo> existing = target.getOrDefault(namespace.capabilities(), Map.of());
                if (existing.containsKey(key) || this.dialect.getLocalVariableShadowing(binding.kind()) ==
                    SQLDialect.LocalVariableShadowing.REJECT_VISIBLE && this.findInNamespace(key, namespace.capabilities()) != null) {
                    return this;
                }
                Map<String, SQLQueryLocalVariableInfo> names = new HashMap<>(existing);
                names.put(key, binding);
                updated.put(namespace.capabilities(), Map.copyOf(names));
                inserted = true;
            }
        }
        if (!inserted) {
            return this;
        }
        return new SQLQueryLocalVariablesContext(this.dialect, this.namespaces, this.parent,
            programVisible ? this.current : Map.copyOf(updated), programVisible ? Map.copyOf(updated) : this.program);
    }

    @NotNull
    private String key(@NotNull SQLQuerySymbolEntry name) {
        return name.getName();
    }

    /**
     * Search one conflict namespace by name, checking this lexical frame, program declarations, then outer frames.
     * Return the first declaration regardless of capability: a nearer incompatible kind still masks an outer one.
     */
    @Nullable
    private SQLQueryLocalVariableInfo findInNamespace(
        @NotNull String name, @NotNull Set<SQLDialect.LocalVariableCapability> namespace
    ) {
        SQLQueryLocalVariableInfo binding = this.current.getOrDefault(namespace, Map.of()).get(name);
        if (binding != null) {
            return binding;
        }
        binding = this.program.getOrDefault(namespace, Map.of()).get(name);
        return binding != null ? binding : this.parent == null ? null : this.parent.findInNamespace(name, namespace);
    }

    /**
     * Choose the unique namespace for the requested operation, then check the nearest declaration's capabilities.
     * Do not search past an incompatible declaration: its name masks outer bindings in the same namespace.
     */
    @Nullable
    public SQLQueryLocalVariableInfo findLocalVariable(
        @NotNull SQLQuerySymbolEntry name, @NotNull SQLDialect.LocalVariableCapability capability
    ) {
        String key = this.key(name);
        for (SQLDialect.LocalVariableNamespace namespace : this.namespaces) {
            if (namespace.capabilities().contains(capability)) {
                SQLQueryLocalVariableInfo binding = this.findInNamespace(key, namespace.capabilities());
                return binding != null && binding.kind().capabilities().contains(capability) ? binding : null;
            }
        }
        return null;
    }

    @NotNull
    public List<SQLQueryLocalVariableInfo> getVisibleValueVariables() {
        return this.getVisibleVariables(SQLDialect.LocalVariableCapability.VALUE);
    }

    @NotNull
    public List<SQLQueryLocalVariableInfo> getVisibleRowsVariables() {
        return this.getVisibleVariables(SQLDialect.LocalVariableCapability.ROWS);
    }

    /**
     * Collect candidates from inner to outer lexical frames, followed by the program snapshot, in the namespace
     * for this operation. Record a name before testing its capability so an incompatible inner declaration still
     * masks an outer candidate; only declarations supporting the operation enter the result list.
     */
    @NotNull
    private List<SQLQueryLocalVariableInfo> getVisibleVariables(@NotNull SQLDialect.LocalVariableCapability capability) {
        List<SQLQueryLocalVariableInfo> visible = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (SQLQueryLocalVariablesContext frame = this; frame != null; frame = frame.parent) {
            for (SQLDialect.LocalVariableNamespace namespace : this.namespaces) {
                if (namespace.capabilities().contains(capability)) {
                    frame.current.getOrDefault(namespace.capabilities(), Map.of()).forEach((name, binding) -> {
                        if (names.add(name) && binding.kind().capabilities().contains(capability)) {
                            visible.add(binding);
                        }
                    });
                }
            }
        }
        for (SQLDialect.LocalVariableNamespace namespace : this.namespaces) {
            if (namespace.capabilities().contains(capability)) {
                this.program.getOrDefault(namespace.capabilities(), Map.of()).forEach((name, binding) -> {
                    if (names.add(name) && binding.kind().capabilities().contains(capability)) {
                        visible.add(binding);
                    }
                });
            }
        }
        return List.copyOf(visible);
    }
}
