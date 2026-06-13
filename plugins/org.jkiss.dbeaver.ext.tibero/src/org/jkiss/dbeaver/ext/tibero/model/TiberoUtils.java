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

package org.jkiss.dbeaver.ext.tibero.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TiberoUtils {

    private TiberoUtils() {
    }

    @NotNull
    static String loadObjectDDL(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSObject object,
        @NotNull String objectType,
        @NotNull String schemaName
    ) throws DBException {

        try (JDBCSession session = DBUtils.openMetaSession(monitor, object, "Load Tibero DDL")) {
            try (JDBCPreparedStatement dbStat =
                     session.prepareStatement("SELECT DBMS_METADATA.GET_DDL(?, ?, ?) FROM DUAL")) {

                dbStat.setString(1, objectType);
                dbStat.setString(2, object.getName());
                dbStat.setString(3, schemaName);

                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        String ddl = dbResult.getString(1);

                        if (!CommonUtils.isEmpty(ddl)) {
                            ddl = ddl.trim();

                            if ("PROCEDURE".equalsIgnoreCase(objectType)
                                || "FUNCTION".equalsIgnoreCase(objectType)
                                || "TRIGGER".equalsIgnoreCase(objectType)
                                || "PACKAGE".equalsIgnoreCase(objectType)
                                || "PACKAGE_BODY".equalsIgnoreCase(objectType)) {

                                if (!ddl.endsWith("/")) {
                                    ddl += "\n/";
                                }
                            } else {
                                if (!ddl.endsWith(";")) {
                                    ddl += ";";
                                }
                            }

                            return ddl;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            return "-- DDL not available: " + e.getMessage();
        }

        return "-- DDL not available";
    }

    @NotNull
    static String loadObjectSource(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSObject object,
        @NotNull String sourceType,
        @NotNull String schemaName
    ) throws DBException {
        String source = loadSourceFromCatalog(monitor, object, "ALL_SOURCE", sourceType, schemaName, true);
        if (source == null) {
            source = loadSourceFromCatalog(monitor, object, "DBA_SOURCE", sourceType, schemaName, true);
        }
        if (source == null) {
            source = loadSourceFromCatalog(monitor, object, "USER_SOURCE", sourceType, schemaName, false);
        }
        if (source != null) {
            return insertCreateOrReplace(source, sourceType, schemaName, object);
        }

        return loadObjectDDL(monitor, object, getMetadataObjectType(sourceType), schemaName);
    }

    private static String loadSourceFromCatalog(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSObject object,
        @NotNull String viewName,
        @NotNull String sourceType,
        @NotNull String schemaName,
        boolean hasOwner
    ) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, object, "Load Tibero source")) {
            StringBuilder sql = new StringBuilder("SELECT TEXT FROM ")
                .append(viewName)
                .append(" WHERE ");
            if (hasOwner) {
                sql.append("OWNER = ? AND ");
            }
            sql.append("NAME = ? AND TYPE = ? ORDER BY LINE");
            try (JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString())) {
                int paramIndex = 1;
                if (hasOwner) {
                    dbStat.setString(paramIndex++, schemaName);
                }
                dbStat.setString(paramIndex++, object.getName());
                dbStat.setString(paramIndex, sourceType);
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    StringBuilder source = new StringBuilder();
                    while (dbResult.next()) {
                        source.append(JDBCUtils.safeGetString(dbResult, "TEXT"));
                    }
                    if (!source.isEmpty()) {
                        return source.toString();
                    }
                }
            }
        } catch (SQLException e) {
            return null;
        }
        return null;
    }

    @NotNull
    private static String getMetadataObjectType(@NotNull String sourceType) {
        return "PACKAGE BODY".equals(sourceType) ? "PACKAGE_BODY" : sourceType;
    }

    @NotNull
    private static String insertCreateOrReplace(
        @NotNull String source,
        @NotNull String sourceType,
        @NotNull String schemaName,
        @NotNull DBSObject object
    ) {
        String trimmed = source.trim();
        if (trimmed.regionMatches(true, 0, "CREATE ", 0, "CREATE ".length())) {
            return trimmed;
        }
        String sourceTypePattern = sourceType.replace(" ", "\\s+");
        Pattern pattern = Pattern.compile(
            "^(" + sourceTypePattern + ")\\s+(\"?[\\w$#]+\"?)(?:\\.(\"?[\\w$#]+\"?))?",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(trimmed);
        if (matcher.find()) {
            if (matcher.group(3) == null) {
                return "CREATE OR REPLACE " + matcher.group(1) + " " +
                    DBUtils.getQuotedIdentifier(object.getDataSource(), schemaName) + "." +
                    matcher.group(2) +
                    trimmed.substring(matcher.end());
            }
            return "CREATE OR REPLACE " + trimmed;
        }
        return trimmed;
    }
}
