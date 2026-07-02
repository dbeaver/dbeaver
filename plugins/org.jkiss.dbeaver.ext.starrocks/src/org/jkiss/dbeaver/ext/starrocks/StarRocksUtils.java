/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.starrocks;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.starrocks.model.StarRocksCatalog;
import org.jkiss.dbeaver.ext.starrocks.model.StarRocksDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.format.SQLFormatUtils;

import java.sql.SQLException;
import java.sql.Statement;

public class StarRocksUtils {

    /**
     * Temporarily sets the catalog context for the current session.
     *
     * @param session the JDBC session to set the catalog for
     * @param dataSource the StarRocks data source
     * @param catalogName the catalog name to switch to
     * @throws SQLException if the SET CATALOG command fails
     */
    public static void setCatalogContext(
        @NotNull JDBCSession session,
        @NotNull StarRocksDataSource dataSource,
        @NotNull String catalogName
    ) throws SQLException {
        try (Statement stmt = session.getOriginal().createStatement()) {
            stmt.execute("SET CATALOG " + DBUtils.getQuotedIdentifier(dataSource, catalogName)); //$NON-NLS-1$
        }
    }

    /**
     * Loads and formats DDL using SHOW CREATE commands for StarRocks tables and views.
     *
     * @param monitor progress monitor
     * @param table the table or view to load DDL for
     * @param sessionTitle title for the metadata session
     * @param showCommand the SHOW CREATE command (e.g., "SHOW CREATE TABLE", "SHOW CREATE VIEW")
     * @param columnName the result column name (e.g., "Create Table", "Create View")
     * @return formatted DDL string, or null if not found
     * @throws DBCException if DDL loading fails
     */
    @Nullable
    public static String loadShowCreateDDL(
        @NotNull DBRProgressMonitor monitor,
        @NotNull GenericTableBase table,
        @NotNull String sessionTitle,
        @NotNull String showCommand,
        @NotNull String columnName
    ) throws DBCException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, table, sessionTitle)) {
            // Switch to the correct catalog context if this is a StarRocks catalog
            GenericCatalog catalog = table.getCatalog();
            if (catalog instanceof StarRocksCatalog) {
                setCatalogContext(session, (StarRocksDataSource) table.getDataSource(), catalog.getName());
            }

            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                    showCommand + " " + table.getFullyQualifiedName(DBPEvaluationContext.DDL))) { //$NON-NLS-1$
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        String definition = JDBCUtils.safeGetString(dbResult, columnName);
                        if (definition != null) {
                            return SQLFormatUtils.formatSQL(table.getDataSource(), definition);
                        }
                    }
                }
            } catch (SQLException e) {
                throw new DBCException(e, session.getExecutionContext());
            }
        } catch (SQLException e) {
            throw new DBCException("Error loading DDL: " + e.getMessage(), e); //$NON-NLS-1$
        }
        return null;
    }
}
