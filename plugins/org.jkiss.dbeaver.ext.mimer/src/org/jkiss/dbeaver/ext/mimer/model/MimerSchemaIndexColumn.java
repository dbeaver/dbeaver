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
package org.jkiss.dbeaver.ext.mimer.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * One column of a {@link MimerSchemaIndex}, read from {@code
 * INFORMATION_SCHEMA.EXT_INDEX_COLUMN_USAGE}.
 *
 * @author Mimer Information Technology
 */
public class MimerSchemaIndexColumn implements DBSObject {

    private final MimerSchemaIndex index;
    private final String columnName;
    private final int ordinalPosition;
    private final boolean ascending;

    public MimerSchemaIndexColumn(@NotNull MimerSchemaIndex index, @NotNull JDBCResultSet dbResult) {
        this.index = index;
        this.columnName = JDBCUtils.safeGetString(dbResult, "COLUMN_NAME");
        this.ordinalPosition = JDBCUtils.safeGetInt(dbResult, "ORDINAL_POSITION");
        this.ascending = "YES".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(dbResult, "IS_ASCENDING"));
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return columnName;
    }

    @Property(viewable = true, order = 2)
    public int getOrdinalPosition() {
        return ordinalPosition;
    }

    @Property(viewable = true, order = 3)
    public boolean isAscending() {
        return ascending;
    }

    /**
     * 11.1+ only - whether this is a real key column of the index, as opposed to an {@code
     * INCLUDE}-only one (defaults {@code true} pre-11.1, matching {@link
     * MimerAccessPathColumn#isKey} - see that class for background). Resolved by matching this
     * column's name among {@link MimerSchemaIndex#resolveAccessPath}'s columns, rather than a
     * second, separate lookup.
     */
    @Property(viewable = true, order = 4)
    public boolean isKey(@NotNull DBRProgressMonitor monitor) throws DBException {
        MimerAccessPath path = index.resolveAccessPath(monitor);
        if (path != null) {
            for (MimerAccessPathColumn column : path.getColumns()) {
                if (columnName.equalsIgnoreCase(column.getName())) {
                    return column.isKey();
                }
            }
        }
        return true;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @Override
    public DBSObject getParentObject() {
        return index;
    }

    @NotNull
    @Override
    public MimerDataSource getDataSource() {
        return index.getDataSource();
    }
}
