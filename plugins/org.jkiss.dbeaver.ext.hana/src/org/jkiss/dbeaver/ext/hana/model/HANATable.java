/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.hana.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPObjectStatistics;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.ByteNumberFormat;

import java.sql.SQLException;
import java.util.List;

public class HANATable extends GenericTable implements DBPObjectStatistics {

    private long tableSize = -1;

    public HANATable(
        GenericStructContainer container,
        @Nullable String tableName,
        @Nullable String tableType,
        @Nullable JDBCResultSet dbResult)
    {
        super(container, tableName, tableType, dbResult);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public List<? extends HANATableColumn> getAttributes(@NotNull DBRProgressMonitor monitor) throws DBException {
        return (List<? extends HANATableColumn>) super.getAttributes(monitor);
    }

    @Override
    public HANATableColumn getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName) throws DBException {
        return (HANATableColumn) super.getAttribute(monitor, attributeName);
    }

    @Override
    public boolean hasStatistics() {
        return tableSize != -1;
    }

    @Override
    public long getStatObjectSize() {
        return tableSize;
    }

    void fetchStatistics(JDBCResultSet dbResult) throws SQLException {
        tableSize = dbResult.getLong("DISK_SIZE");
    }

    @Property(category = DBConstants.CAT_STATISTICS, formatter = ByteNumberFormat.class)
    public Long getTableSize(DBRProgressMonitor monitor) throws DBException {
        if (tableSize == -1) {
            ((HANASchema) getSchema()).collectObjectStatistics(monitor, false, false);
        }
        return tableSize;
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (tableSize != -1) {
            tableSize = -1;
            ((HANASchema) getSchema()).resetStatistics();
        }
        return super.refreshObject(monitor);
    }

    @Nullable
    @Override
    public DBPPropertySource getStatProperties() {
        return null;
    }
}
