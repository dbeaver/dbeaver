/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableRegular;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * RedshiftTable base
 */
public class RedshiftTable extends PostgreTableRegular
{
    private static final Log log = Log.getLog(RedshiftTable.class);

    public RedshiftTable(PostgreSchema catalog) {
        super(catalog);
    }

    public RedshiftTable(PostgreSchema catalog, ResultSet dbResult) {
        super(catalog, dbResult);
    }

    protected void readTableStatistics(JDBCSession session) throws SQLException {
        try (JDBCPreparedStatement dbStat = session.prepareStatement(
            "SELECT size, tbl_rows FROM SVV_TABLE_INFO WHERE \"schema\"=? AND table_id=?"))
        {
            dbStat.setString(1, getSchema().getName());
            dbStat.setLong(2, getObjectId());
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                if (dbResult.next()) {
                    fetchStatistics(dbResult);
                }
            }
        }
    }

    protected void fetchStatistics(JDBCResultSet dbResult) throws SQLException {
        diskSpace = dbResult.getLong("size") * 1024 * 1024;
        rowCountEstimate = rowCount = dbResult.getLong("tbl_rows");
    }

}
