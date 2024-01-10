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
package org.jkiss.dbeaver.ext.mssql.model.generic;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.mssql.SQLServerUtils;
import org.jkiss.dbeaver.model.DBPObjectStatisticsCollector;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.SQLException;

/**
* SQL Server schemas
*/
public class SQLServerGenericSchema extends GenericSchema implements DBPObjectStatisticsCollector {

    private static final Log log = Log.getLog(SQLServerGenericSchema.class);

    private long schemaId;
    private boolean hasStatistics;

    public SQLServerGenericSchema(GenericDataSource dataSource, GenericCatalog catalog, String schemaName, long schemaId) {
        super(dataSource, catalog, schemaName);
        this.schemaId = schemaId;
    }

    @Property(viewable = true, order = 3)
    public long getSchemaId() {
        return schemaId;
    }

    @Override
    public boolean isStatisticsCollected() {
        return hasStatistics;
    }

    @Override
    public void collectObjectStatistics(DBRProgressMonitor monitor, boolean totalSizeOnly, boolean forceRefresh) throws DBException {
        if (hasStatistics && !forceRefresh) {
            return;
        }
        boolean isSQLServer = ((SQLServerMetaModel) getDataSource().getMetaModel()).isSqlServer();
        if (!isSQLServer && !getDataSource().isServerVersionAtLeast(15, 0)) {
            hasStatistics = true;
            return;
        }
        GenericCatalog catalog = getCatalog();
        if (catalog == null) {
            log.debug("Can't read tables statistics due to lack of schemas catalog");
            return;
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table statistics")) {
            try (JDBCPreparedStatement dbStat = SQLServerUtils.prepareTableStatisticLoadStatement(
                session,
                getDataSource(),
                catalog,
                getSchemaId(),
                null,
                isSQLServer)) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String tableName = dbResult.getString("name");
                        GenericTableBase table = getTable(monitor, tableName);
                        if (table instanceof SQLServerGenericTable) {
                            ((SQLServerGenericTable) table).fetchTableStats(dbResult);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBCException("Error reading table statistics", e);
        } finally {
            hasStatistics = true;
        }
    }
}
