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
package org.jkiss.dbeaver.ext.tibero.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractTriggerColumn;
import org.jkiss.dbeaver.model.meta.Property;

public class TiberoTriggerColumn extends AbstractTriggerColumn {

    private final TiberoTrigger<?> trigger;
    private final String name;
    private final TiberoTableColumn tableColumn;
    private final boolean columnList;

    public TiberoTriggerColumn(
        @NotNull TiberoTrigger<?> trigger,
        @NotNull TiberoTableColumn tableColumn,
        @NotNull org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet dbResult
    ) {
        this.trigger = trigger;
        this.tableColumn = tableColumn;
        this.name = JDBCUtils.safeGetString(dbResult, "TRIGGER_COLUMN_NAME");
        this.columnList = JDBCUtils.safeGetBoolean(dbResult, "COLUMN_LIST", "YES");
    }

    @Override
    public TiberoTrigger<?> getTrigger() {
        return trigger;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    @Override
    @Property(viewable = true, order = 2)
    public TiberoTableColumn getTableColumn() {
        return tableColumn;
    }

    @Property(viewable = true, order = 3)
    public boolean isColumnList() {
        return columnList;
    }

    @Override
    public int getOrdinalPosition() {
        return 0;
    }

    @Nullable
    @Override
    public String getDescription() {
        return tableColumn.getDescription();
    }

    @Override
    public TiberoTrigger<?> getParentObject() {
        return trigger;
    }

    @NotNull
    @Override
    public TiberoDataSource getDataSource() {
        return trigger.getDataSource();
    }

    @Override
    public String toString() {
        return getName();
    }
}
