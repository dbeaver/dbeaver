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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableRegular;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;

import java.sql.ResultSet;
import java.sql.SQLException;

public class TimescaleTable extends PostgreTableRegular {

    private static final Log log = Log.getLog(TimescaleTable.class);

    public TimescaleTable(PostgreSchema schema, ResultSet dbResult) {
        super(schema, dbResult);
    }

    @Override
    protected void readTableStatistics(@NotNull JDBCSession session) throws DBException, SQLException {
        if (!getDataSource().getServerType().supportsTableStatistics()) {
            return;
        }

        if (!isHypertable(session)) {
            super.readTableStatistics(session);
            return;
        }

        try (JDBCPreparedStatement dbStat = session.prepareStatement(
            "SELECT total_bytes as total_rel_size, table_bytes as rel_size " +
            "FROM hypertable_detailed_size(?)")) {
            dbStat.setLong(1, getObjectId());
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                if (dbResult.next()) {
                    fetchStatistics(dbResult);
                }
            }
        }
    }

    private boolean isHypertable(@NotNull JDBCSession session) throws SQLException {
        String sql =
            "SELECT 1 FROM timescaledb_information.hypertables " +
            "WHERE hypertable_schema = ? AND hypertable_name = ?";

        try (JDBCPreparedStatement stmt = session.prepareStatement(sql)) {
            stmt.setString(1, getSchema().getName());
            stmt.setString(2, getName());
            try (JDBCResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.error("Failed to check if table is a hypertable: " + e.getMessage(), e);
            return false;
        }
    }

    @Override
    protected void fetchStatistics(@NotNull JDBCResultSet dbResult) throws DBException, SQLException {
        super.fetchStatistics(dbResult);
    }
}
