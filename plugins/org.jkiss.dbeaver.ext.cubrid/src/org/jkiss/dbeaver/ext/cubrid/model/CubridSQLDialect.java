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
package org.jkiss.dbeaver.ext.cubrid.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.sql.SQLDialect;

import java.sql.SQLException;

public class CubridSQLDialect extends GenericSQLDialect
{
    public static final String CUBRID_DIALECT_ID = "cubrid";
    private static final Log log = Log.getLog(CubridSQLDialect.class);

    public CubridSQLDialect() {
        super("Cubrid", "cubrid");
    }

    @Override
    public void initDriverSettings(
            @NotNull JDBCSession session,
            @NotNull JDBCDataSource dataSource,
            @NotNull JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(session, dataSource, metaData);
        CubridDataSource source = (CubridDataSource) dataSource;
        source.setSupportMultiSchema(isSupportMultiSchema(session));
    }

    @NotNull
    public boolean isSupportMultiSchema(@NotNull JDBCSession session) {
        try {
            int major = session.getMetaData().getDatabaseMajorVersion();
            int minor = session.getMetaData().getDatabaseMinorVersion();
            if (major > 11 || (major == 11 && minor >= 2)) {
                return true;
            }
        } catch (SQLException e) {
            log.error("Can't get database version", e);
        }
        return false;
    }

    @NotNull
    @Override
    public int getSchemaUsage() {
        return SQLDialect.USAGE_ALL;
    }
}
