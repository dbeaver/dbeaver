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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;

import java.sql.SQLException;
import java.util.List;

/**
 * The temporary schema belonging to a query session. It may not be present in the database metadata cache.
 */
public class PostgreSchemaMyTemp extends PostgreSchema {
    public PostgreSchemaMyTemp(@NotNull JDBCSession session, @NotNull PostgreDatabase database) throws SQLException {
        super(database, PostgreConstants.TEMP_SCHEMA_NAME);
        try (JDBCPreparedStatement statement = session.prepareStatement(
            "SELECT n.oid,n.* FROM pg_catalog.pg_namespace n WHERE n.oid=pg_catalog.pg_my_temp_schema()")) {
            try (JDBCResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    setName(resultSet.getString("nspname"));
                    loadInfo(resultSet);
                }
            }
        }
    }

    @NotNull
    public List<PostgreTableColumn> readTableColumns(@NotNull JDBCSession session, @NotNull String tableName)
            throws SQLException, DBException {
        return isPersisted() ? getTableCache().readTemporaryTableColumns(session, tableName) : List.of();
    }
}
