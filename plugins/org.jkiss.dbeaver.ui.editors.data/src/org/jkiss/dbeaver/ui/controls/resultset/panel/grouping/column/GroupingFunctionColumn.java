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
package org.jkiss.dbeaver.ui.controls.resultset.panel.grouping.column;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;

public class GroupingFunctionColumn implements GroupingColumn {

    private static int idCounter;

    protected final String sql;

    private final String uniqueId;

    public GroupingFunctionColumn(
        @NotNull String stringFunction,
        @NotNull DBPDataSource dataSource
    ) {
        this.uniqueId = String.valueOf(idCounter++);
        this.sql = DBUtils.getQuotedIdentifier(dataSource, stringFunction);
    }

    @NotNull
    public String getSql() {
        return sql;
    }

    public boolean isShowToUser() {
        return true;
    }

    public boolean mustBeUniqueByName() {
        return false;
    }

    @NotNull
    public String getUniqueId() {
        return uniqueId;
    }

    public boolean matchById(@NotNull GroupingFunctionColumn other) {
        return uniqueId.equals(other.uniqueId);
    }
}
