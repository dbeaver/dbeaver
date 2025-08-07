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
package org.jkiss.dbeaver.model.sql;

import org.jkiss.code.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Category of a query type
 *
 * @see SQLQueryType
 */
public enum SQLQueryCategory {
    SQL,
    DML,
    DDL,
    TCL,
    UNKNOWN;

    @NotNull
    public static Set<SQLQueryCategory> categorizeScript(@NotNull List<SQLScriptElement> scriptElements) {
        Set<SQLQueryCategory> categories = new HashSet<>();
        for (SQLScriptElement element : scriptElements) {
            if (element instanceof SQLQuery sqlQuery) {
                categories.add(sqlQuery.getType().getCategory());
            }
        }
        return categories;
    }
}
