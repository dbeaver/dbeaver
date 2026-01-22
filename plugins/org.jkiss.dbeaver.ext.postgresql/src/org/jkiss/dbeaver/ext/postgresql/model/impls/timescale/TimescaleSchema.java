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
package org.jkiss.dbeaver.ext.postgresql.model.impls.timescale;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableBase;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;
import java.sql.SQLException;

public class TimescaleSchema extends PostgreSchema {

    public TimescaleSchema(PostgreDatabase owner, String name, ResultSet dbResult) throws SQLException {
        super(owner, name, dbResult);
    }

    @Override
    public void collectObjectStatistics(@NotNull DBRProgressMonitor monitor, boolean totalSizeOnly, boolean forceRefresh) throws DBException {
        super.collectObjectStatistics(monitor, totalSizeOnly, forceRefresh);
        try (DBCSession session = DBUtils.openMetaSession(monitor, this, "Read hypertable statistics")) {
            try (JDBCPreparedStatement stmt = ((JDBCSession) session).prepareStatement(
                """
                SELECT c.oid,
                       (size_info).total_bytes as total_rel_size,
                       (size_info).table_bytes as rel_size
                FROM pg_class c
                JOIN timescaledb_information.hypertables h
                  ON h.hypertable_schema = ? AND h.hypertable_name = c.relname
                CROSS JOIN LATERAL hypertable_detailed_size(c.oid) AS size_info
                WHERE c.relnamespace = ?"""
            )) {
                stmt.setString(1, getName());
                stmt.setLong(2, getObjectId());
                try (JDBCResultSet dbResult = stmt.executeQuery()) {
                    while (dbResult.next()) {
                        long tableId = dbResult.getLong(1);
                        PostgreTableBase table = getTable(monitor, tableId);
                        if (table instanceof TimescaleTable timescaleTable) {
                            timescaleTable.fetchStatistics(dbResult);
                        }
                    }
                }
            } catch (SQLException e) {
                throw new DBCException("Error reading schema relation statistics", e);
            }
        }
    }
}
