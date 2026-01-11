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
package org.jkiss.dbeaver.ext.h2.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPObjectStatisticsCollector;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.SQLException;
import java.util.List;

public class H2Schema extends GenericSchema implements DBPObjectStatisticsCollector {
    private boolean hasStatistics = false;

    public H2Schema(@NotNull GenericDataSource dataSource, @Nullable GenericCatalog catalog, @NotNull String schemaName) {
        super(dataSource, catalog, schemaName);
    }

    @Override
    public boolean isStatisticsCollected() {
        return hasStatistics;
    }

    @Override
    public void collectObjectStatistics(
        @NotNull DBRProgressMonitor monitor,
        boolean totalSizeOnly,
        boolean forceRefresh
    ) throws DBException {
        if (hasStatistics && !forceRefresh) {
            return;
        }
        List<? extends GenericTableBase> tables = getTables(monitor);
        try (DBCSession session = DBUtils.openMetaSession(monitor, this, "Read table statistics")) {
            try (JDBCPreparedStatement dbStat = ((JDBCSession) session).prepareStatement("SELECT DISK_SPACE_USED(?)")) {
                for (GenericTableBase table : tables) {
                    if (table instanceof H2Table h2Table) {
                        dbStat.setString(1, table.getFullyQualifiedName(DBPEvaluationContext.DDL));
                        try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                            if (dbResult.next()) {
                                h2Table.setTableDiskSize(dbResult.getLong(1));
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                throw new DBCException("Error reading catalog statistics", e);
            }
        } finally {
            hasStatistics = true;
        }
    }
}
