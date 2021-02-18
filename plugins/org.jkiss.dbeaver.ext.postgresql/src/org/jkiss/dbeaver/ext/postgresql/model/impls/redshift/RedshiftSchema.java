/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.model.impls.redshift;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * RedshiftExternalSchema
 */
public class RedshiftSchema extends PostgreSchema {
    private static final Log log = Log.getLog(RedshiftSchema.class);

    public RedshiftSchema(PostgreDatabase database, String name, ResultSet dbResult) throws SQLException {
        super(database, name, dbResult);
    }

    public RedshiftSchema(PostgreDatabase database, String name, PostgreRole owner) {
        super(database, name, owner);
    }

    @Override
    public String getTableColumnsQueryExtraParameters(PostgreTableContainer owner, PostgreTableBase forTable) {
        return ",format_encoding(a.attencodingtype::integer) AS \"encoding\"";
    }

    @Override
    public void collectObjectStatistics(DBRProgressMonitor monitor, boolean totalSizeOnly, boolean forceRefresh) throws DBException {
        if (hasStatistics && !forceRefresh) {
            return;
        }
        try (DBCSession session = DBUtils.openMetaSession(monitor, this, "Read relation statistics")) {
            try (JDBCPreparedStatement dbStat = ((JDBCSession)session).prepareStatement(
                "SELECT table_id, size, tbl_rows FROM SVV_TABLE_INFO WHERE \"schema\"=?"))
            {
                dbStat.setString(1, getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        long tableId = dbResult.getLong(1);
                        PostgreTableBase table = getTable(monitor, tableId);
                        if (table instanceof RedshiftTable) {
                            ((RedshiftTable) table).fetchStatistics(dbResult);
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

