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
package org.jkiss.dbeaver.model.sql;

import org.jkiss.code.NotNull;

import java.util.List;

/**
 * Semantic description of an operation on a database object.
 */
public record SQLObjectOperation(
    @NotNull Operation operation,
    @NotNull ObjectKind objectKind,
    @NotNull List<String> qualifiedNameParts,
    boolean temporary,
    boolean ifNotExists,
    @NotNull List<String> newName
) {
    public SQLObjectOperation(@NotNull Operation operation, @NotNull ObjectKind objectKind, @NotNull List<String> qualifiedNameParts) {
        this(operation, objectKind, qualifiedNameParts, false);
    }

    public SQLObjectOperation(
        @NotNull Operation operation, @NotNull ObjectKind objectKind, @NotNull List<String> qualifiedNameParts, boolean temporary
    ) {
        this(operation, objectKind, qualifiedNameParts, temporary, false, List.of());
    }

    public SQLObjectOperation {
        qualifiedNameParts = List.copyOf(qualifiedNameParts);
        newName = List.copyOf(newName);
    }

    public enum Operation {
        CREATE,
        DROP,
        ALTER,
        RENAME
    }

    public enum ObjectKind {
        TABLE,
        VIEW,
        INDEX,
        SCHEMA,
        DATABASE,
        CATALOG,
        FUNCTION,
        PROCEDURE,
        SEQUENCE,
        SYNONYM,
        OTHER
    }
}
