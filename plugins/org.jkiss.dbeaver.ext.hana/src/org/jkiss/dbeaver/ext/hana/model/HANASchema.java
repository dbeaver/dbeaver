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
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPObjectStatisticsCollector;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.SQLException;

public class HANASchema extends GenericSchema implements DBPQualifiedObject, DBPObjectStatisticsCollector {

    @NotNull
    private String schemaName;
    private boolean hasStatistics;
	
    public HANASchema(@NotNull GenericDataSource dataSource, @Nullable GenericCatalog catalog, @NotNull String schemaName) {
        super(dataSource, catalog, schemaName);
        this.schemaName = schemaName;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return schemaName;
    }

    public void setName(@NotNull String schemaName) {
        this.schemaName = schemaName;
    }

    @NotNull
    @Override
    public Class<? extends DBSEntity> getPrimaryChildType(@Nullable DBRProgressMonitor monitor) throws DBException {
        return HANATable.class;
    }
    
    public boolean hasOnlySynonyms() { 
    	return HANAConstants.SCHEMA_PUBLIC.equals(getName()); 
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        return DBUtils.getFullQualifiedName(getDataSource(), this);
    }

    @Override
    public boolean isStatisticsCollected() {
        return hasStatistics;
    }

    @Override
    public synchronized DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        hasStatistics = false;
        return super.refreshObject(monitor);
    }

    void resetStatistics() {
        hasStatistics = false;
    }

    @Override
    public void collectObjectStatistics(DBRProgressMonitor monitor, boolean totalSizeOnly, boolean forceRefresh) throws DBException {
        if (hasStatistics || forceRefresh) {
            return;
        }
        try (DBCSession session = DBUtils.openMetaSession(monitor, this, "Read relation statistics")) {
            try (JDBCPreparedStatement dbStat = ((JDBCSession) session).prepareStatement(
                "SELECT TABLE_NAME, DISK_SIZE\n" +
                    "FROM SYS.M_TABLE_PERSISTENCE_STATISTICS\n" +
                    "WHERE SCHEMA_NAME = ?\n" +
                    "ORDER BY TABLE_NAME"))
            {
                dbStat.setString(1, getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String tableName = dbResult.getString("TABLE_NAME");
                        GenericTableBase table = getTable(monitor, tableName);
                        if (table instanceof HANATable) {
                            ((HANATable) table).fetchStatistics(dbResult);
                        }
                    }
                }
            } catch (SQLException e) {
                throw new DBCException("Error reading schema relation statistics", e);
            }
        } finally {
            hasStatistics = true;
        }
    }
}
