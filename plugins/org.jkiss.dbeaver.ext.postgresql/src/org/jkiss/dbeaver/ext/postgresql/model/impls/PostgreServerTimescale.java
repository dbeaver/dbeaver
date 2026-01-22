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
package org.jkiss.dbeaver.ext.postgresql.model.impls;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.ext.postgresql.model.impls.timescale.TimescaleSchema;
import org.jkiss.dbeaver.ext.postgresql.model.impls.timescale.TimescaleTable;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;

import java.sql.SQLException;
/**
 * PostgreServerTimescale
 */
public class PostgreServerTimescale extends PostgreServerExtensionBase {

    public PostgreServerTimescale(PostgreDataSource dataSource) {
        super(dataSource);
    }

    @Override
    public boolean supportsEntityMetadataInResults() {
        return true;
    }

    @Override
    public boolean supportsCopyFromStdIn() {
        return true;
    }

    @Override
    public String getServerTypeName() {
        return "Timescale";
    }

    @Override
    public PostgreTableBase createRelationOfClass(@NotNull PostgreSchema schema, @NotNull PostgreClass.RelKind kind, @NotNull JDBCResultSet dbResult) {
        if (kind == PostgreClass.RelKind.r ||
            kind == PostgreClass.RelKind.t ||
            kind == PostgreClass.RelKind.p
        ) {
            return new TimescaleTable(schema, dbResult);
        }
        return super.createRelationOfClass(schema, kind, dbResult);
    }

    @Override
    public PostgreDatabase.SchemaCache createSchemaCache(PostgreDatabase database) {
        return new TimescaleSchemaCache();
    }

    private static class TimescaleSchemaCache extends PostgreDatabase.SchemaCache {

        @Override
        protected PostgreSchema fetchObject(@NotNull JDBCSession session, @NotNull PostgreDatabase owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            String name = JDBCUtils.safeGetString(resultSet, "nspname");
            return new TimescaleSchema(owner, name, resultSet);
        }
    }
}
