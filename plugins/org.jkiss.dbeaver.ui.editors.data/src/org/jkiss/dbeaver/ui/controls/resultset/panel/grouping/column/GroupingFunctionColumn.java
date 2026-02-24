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
import org.jkiss.dbeaver.ui.controls.resultset.panel.grouping.GroupingColumnsContainer;

public class GroupingFunctionColumn implements GroupingColumn {

    protected final String sql;

    protected final GroupingColumnsContainer columnsContainer;

    public GroupingFunctionColumn(
        @NotNull String stringFunction,
        @NotNull GroupingColumnsContainer columnsContainer,
        @NotNull DBPDataSource dataSource
    ) {
        this.sql = DBUtils.getQuotedIdentifier(dataSource, stringFunction);
        this.columnsContainer = columnsContainer;
    }

    @Override
    public boolean canBeAdded() {
        return true;
    }

    @Override
    public boolean canBeRemoved() {
        return columnsContainer.getColumnsByType(GroupingFunctionColumn.class).size() > 1;
    }

    @NotNull
    public String provideSqlFunction() {
        return sql;
    }

    public boolean isShowToUser() {
        return true;
    }

    @NotNull
    public String nameShownToUser() {
        return sql;
    }
}
