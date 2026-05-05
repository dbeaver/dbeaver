/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.sql.analyzer.builder;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.ArrayList;
import java.util.List;

public abstract class Builder<T, C> {
    protected final DBPDataSource dataSource;
    protected final DBSObject parent;
    protected final List<C> children;

    protected Builder(@NotNull DBPDataSource dataSource, @NotNull DBSObject parent) {
        this.dataSource = dataSource;
        this.parent = parent;
        this.children = new ArrayList<>();
    }

    @NotNull
    public abstract T build() throws DBException;

    @NotNull
    public List<C> getChildren() {
        return children;
    }

    public interface Consumer<T extends Builder<?, ?>> {
        Consumer<?> EMPTY = t -> {};

        void apply(@NotNull T builder) throws DBException;

        @SuppressWarnings("unchecked")
        static <T extends Builder<?, ?>> Consumer<T> empty() {
            return (Consumer<T>) EMPTY;
        }
    }
}
