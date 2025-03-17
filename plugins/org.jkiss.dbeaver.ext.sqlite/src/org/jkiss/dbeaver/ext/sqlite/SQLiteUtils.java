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

package org.jkiss.dbeaver.ext.sqlite;

import org.jkiss.api.DriverReference;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.ext.sqlite.model.SQLiteObjectType;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPPersistedObject;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistActionComment;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBStructUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SQLiteUtils
 */
public class SQLiteUtils {

    public static final DriverReference DRIVER_REFERENCE = new DriverReference("sqlite", "sqlite_jdbc");
    private static final Log log = Log.getLog(SQLiteUtils.class);


    public static String readMasterDefinition(DBRProgressMonitor monitor, DBSObject sourceObject, SQLiteObjectType objectType, String sourceObjectName, GenericTableBase table) {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, sourceObject, "Load SQLite description")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT sql FROM " + (sourceObject.getParentObject() instanceof GenericSchema ?
                                      DBUtils.getQuotedIdentifier(sourceObject.getParentObject()) + "." : "")
                    + "sqlite_master WHERE type=? AND tbl_name=?" + (sourceObjectName != null ? " AND name=?" : "")
                    + "\n" + "UNION ALL\n" + "SELECT sql FROM "
                    + "sqlite_temp_master WHERE type=? AND tbl_name=?" + (sourceObjectName != null ? " AND name=?" : "")
                    + "\n"))
            {
                int paramIndex = 1;
                dbStat.setString(paramIndex++, objectType.name());
                dbStat.setString(paramIndex++, table.getName());
                if (sourceObjectName != null) {
                    dbStat.setString(paramIndex++, sourceObjectName);
                }
                dbStat.setString(paramIndex++, objectType.name());
                dbStat.setString(paramIndex++, table.getName());
                if (sourceObjectName != null) {
                    dbStat.setString(paramIndex++, sourceObjectName);
                }
                try (JDBCResultSet resultSet = dbStat.executeQuery()) {
                    StringBuilder sql = new StringBuilder();
                    while (resultSet.next()) {
                        String ddl = resultSet.getString(1);
                        if (ddl != null) {
                            sql.append(ddl);
                            sql.append(";\n");
                        }
                    }
                    String ddl = sql.toString();
                    //ddl = ddl.replaceAll("(?i)CREATE VIEW", "CREATE OR REPLACE VIEW");
                    return ddl;
                }
            }
        } catch (Exception e) {
            log.debug(e);
            return null;
        }
    }

    public static void createTableAlterActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull String reason,
        @NotNull GenericTableBase table,
        @NotNull Collection<DBEPersistAction> actions
    ) throws DBException {
        final Collection<? extends GenericTableColumn> attributes = CommonUtils.safeCollection(table.getAttributes(monitor)).stream()
            .filter(DBPPersistedObject::isPersisted)
            .toList();
        if (CommonUtils.isEmpty(attributes)) {
            throw new DBException("Table has no attributes");
        }
        final String columns = attributes.stream()
            .map(DBUtils::getQuotedIdentifier)
            .collect(Collectors.joining(",\n  "));

        actions.add(new SQLDatabasePersistActionComment(
            table.getDataSource(),
            reason
        ));
        GenericSchema schema = table.getSchema();
        String schemaPart = schema != null ? DBUtils.getQuotedIdentifier(schema) + "." : "";
        actions.add(new SQLDatabasePersistAction(
            "Create temporary table from original table",
            "CREATE TEMPORARY TABLE "  + schemaPart + "temp AS\nSELECT"
                + (attributes.isEmpty() ? " *" : "\n  " + columns) + "\nFROM " + DBUtils.getQuotedIdentifier(table)
        ));
        actions.add(new SQLDatabasePersistAction(
            "Drop original table",
            "\nDROP TABLE " + table.getFullyQualifiedName(DBPEvaluationContext.DML) + ";\n"
        ));
        actions.add(new SQLDatabasePersistAction(
            "Create new table",
            DBStructUtils.generateTableDDL(monitor, table, Map.of(DBPScriptObject.OPTION_DDL_ONLY_PERSISTED_ATTRIBUTES, true), false)
        ));
        actions.add(new SQLDatabasePersistAction(
            "Insert values from temporary table to new table",
            "INSERT INTO " + schemaPart + DBUtils.getQuotedIdentifier(table)
                + (attributes.isEmpty() ? "" : "\n (" + columns + ")") + "\nSELECT"
                + (attributes.isEmpty() ? " *" : "\n  " + columns) + "\nFROM temp"
        ));
        actions.add(new SQLDatabasePersistAction(
            "Drop temporary table",
            "\nDROP TABLE "  + schemaPart + "temp"
        ));
    }
}
