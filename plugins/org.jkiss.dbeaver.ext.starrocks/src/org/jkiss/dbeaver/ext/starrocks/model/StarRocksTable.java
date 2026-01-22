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
package org.jkiss.dbeaver.ext.starrocks.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.format.SQLFormatUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

/**
 * StarRocks Table - represents a table within a StarRocks database.
 */
public class StarRocksTable extends StarRocksTableBase {

    private static final String COL_CREATE_TABLE = "Create Table"; //$NON-NLS-1$

    private String ddl;

    public StarRocksTable(
        GenericStructContainer container,
        @Nullable String tableName,
        @Nullable String tableType,
        @Nullable JDBCResultSet dbResult
    ) {
        super(container, tableName, tableType, dbResult);
    }

    @Override
    public boolean isView() {
        return false;
    }

    @Override
    public String getDDL() {
        return ddl;
    }

    @NotNull
    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(@NotNull DBRProgressMonitor monitor, @NotNull Map<String, Object> options) throws DBException {
        if (CommonUtils.getOption(options, DBPScriptObject.OPTION_REFRESH)) {
            ddl = null;
        }
        if (ddl == null && isPersisted()) {
            loadDDL(monitor);
        }
        return ddl != null ? ddl : "";
    }

    private void loadDDL(DBRProgressMonitor monitor) throws DBCException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table DDL")) { //$NON-NLS-1$
            // Switch to the correct catalog context
            StarRocksCatalog catalog = getStarRocksCatalog();
            if (catalog != null) {
                try (Statement stmt = session.getOriginal().createStatement()) {
                    stmt.execute("SET CATALOG " + DBUtils.getQuotedIdentifier(getDataSource(), catalog.getName())); //$NON-NLS-1$
                }
            }

            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SHOW CREATE TABLE " + getFullyQualifiedName(DBPEvaluationContext.DDL))) { //$NON-NLS-1$
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        String definition = JDBCUtils.safeGetString(dbResult, COL_CREATE_TABLE);
                        if (definition != null) {
                            ddl = SQLFormatUtils.formatSQL(getDataSource(), definition);
                        }
                    }
                }
            } catch (SQLException e) {
                throw new DBCException(e, session.getExecutionContext());
            }
        } catch (SQLException e) {
            throw new DBCException("Error loading table DDL", e); //$NON-NLS-1$
        }
    }
}
