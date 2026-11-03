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
package org.jkiss.dbeaver.model.access;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.List;

public record DBAAccessState(
    @NotNull Scope scope,
    @Nullable Mapping mapping,
    @NotNull List<Role> roles,
    @NotNull List<Target> targets,
    @NotNull List<Permission> permissions
) {
    public DBAAccessState {
        roles = List.copyOf(roles);
        targets = List.copyOf(targets);
        permissions = List.copyOf(permissions);
    }

    public record Scope(@NotNull String id, @NotNull String label) {
    }

    public record Mapping(
        boolean present,
        @NotNull String name,
        @NotNull String defaultSchema,
        boolean editable,
        boolean schemaEditable,
        boolean schemaNullable
    ) {
        public Mapping(boolean present, @NotNull String name, @NotNull String defaultSchema, boolean editable, boolean schemaEditable) {
            this(present, name, defaultSchema, editable, schemaEditable, false);
        }
    }

    public record Role(@NotNull String name, boolean granted, boolean editable) {
    }

    public record Target(@NotNull String id, @NotNull String label, @NotNull List<String> permissions, boolean editable) {
        public Target {
            permissions = List.copyOf(permissions);
        }
    }

    public enum PermissionState {
        GRANT, GRANT_WITH_OPTION, DENY, REVOKE
    }

    public record Permission(
        @NotNull Target target,
        @NotNull String name,
        @NotNull PermissionState state,
        @Nullable String grantor,
        boolean editable,
        boolean cascade
    ) {
        public Permission(
            @NotNull Target target,
            @NotNull String name,
            @NotNull PermissionState state,
            @Nullable String grantor,
            boolean editable
        ) {
            this(target, name, state, grantor, editable, false);
        }
    }
}
