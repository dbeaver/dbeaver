/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryQualifiedName;

import java.util.List;
import java.util.Objects;

/**
 * Describes qualified name of the database entity
 */
public class SQLQueryComplexName {
    @NotNull
    private final List<String> parts;
    @Nullable
    private final SQLQueryQualifiedName qualifiedName;

    public SQLQueryComplexName(@NotNull String ... parts) {
        this.parts = List.of(parts);
        this.qualifiedName = null;
    }

    public SQLQueryComplexName(@NotNull List<String> parts) {
        this.parts = parts;
        this.qualifiedName = null;
    }

    public SQLQueryComplexName(@NotNull SQLQueryQualifiedName name) {
        this.parts = name.toListOfStrings();
        this.qualifiedName = name;
    }

    @NotNull
    public List<String> getParts() {
        return this.parts;
    }

    @Nullable
    public SQLQueryComplexName trimEnd() {
        return this.parts.size() < 2 ? null : new SQLQueryComplexName(this.parts.subList(0, this.parts.size() - 1));
    }

    @Nullable
    public SQLQueryQualifiedName qualifiedName() {
        return this.qualifiedName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SQLQueryComplexName name)) {
            return false;
        }
        return Objects.equals(this.parts, name.parts);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.parts);
    }
}
