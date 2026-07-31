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
package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataSource;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchema;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableBase;
import org.jkiss.dbeaver.ext.oracle.model.OracleTablePhysical;
import org.jkiss.dbeaver.ext.oracle.model.OracleUtils;
import org.jkiss.dbeaver.ext.oracle.model.source.OracleSourceObject;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBStructUtils;

import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * YashanDBUtils
 */
public class YashanDBUtils extends OracleUtils {

    private static final Log log = Log.getLog(YashanDBUtils.class);

    private static final String DEFAULT_CUSTOM_SOURCE = "-- ??? CUSTOM SOURCE";
    private static final int MONITOR_TOTAL_WORK = 1;
    private static final String SUB_TASK_LINE_PREFIX = "Line ";

    @Nullable
    public static String getTableOrViewDDL(@NotNull DBRProgressMonitor monitor, @NotNull String objectType,
                                           @NotNull OracleTableBase object,
                                           @NotNull Map<String, Object> options) throws DBException {

        String ddl = "";
        String objectFullName = DBUtils.getObjectFullName(object, DBPEvaluationContext.DDL);
        OracleSchema schema = object.getContainer();

        try (final JDBCSession session = DBUtils.openMetaSession(monitor, object,
            "Load source code for " + objectType + " '" + objectFullName + "'");
             JDBCPreparedStatement dbStat = session
                 .prepareStatement("SELECT DBMS_METADATA.GET_DDL(?,?,?) FROM DUAL")) {
            dbStat.setString(1, objectType);
            dbStat.setString(2, object.getName());
            if (schema != null) {
                dbStat.setString(3, schema.getName());
            }
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                if (dbResult.next()) {
                    ddl = dbResult.getString(1);
                } else {
                    log.warn("No DDL for " + objectType + " '" + objectFullName + "'");
                    return "-- EMPTY DDL";
                }
            }
            ddl = ddl.trim();
            return ddl;

        } catch (SQLException | DBCException e) {
            if (object instanceof OracleTablePhysical) {
                log.error("Error generating YashanDB DDL. Generate default.", e);
                return DBStructUtils.generateTableDDL(monitor, object, options, true);
            } else {
                throw new DBException(e.getMessage());
            }
        } finally {
            monitor.done();
        }
    }

    @Nullable
    public static String getSource(@NotNull DBRProgressMonitor monitor, @NotNull OracleSourceObject sourceObject,
                                   boolean body, boolean insertCreateReplace) throws DBCException {
        String preCheckResult = preCheckSourceObject(sourceObject);
        if (preCheckResult != null) {
            return preCheckResult;
        }

        initMonitor(monitor, sourceObject);

        try {
            String sysViewName = getSysViewName(sourceObject.getDataSource(), monitor);
            String sourceContent = querySourceContent(monitor, sourceObject, body, sysViewName);
            return insertCreateReplace ? insertCreateReplace(sourceObject, body, sourceContent) : sourceContent;
        } finally {
            monitor.done();
        }
    }

    @Nullable
    public static String insertCreateReplace(@NotNull OracleSourceObject object, boolean body,
                                             @Nullable String source) {
        String sourceType = object.getSourceType().name();
        if (body) {
            sourceType += " BODY";
        }
        if (source != null) {
            Pattern srcPattern = Pattern.compile("^(" + sourceType + ")\\s+(\"{0,1}\\w+\"{0,1})",
                Pattern.CASE_INSENSITIVE);
            Matcher matcher = srcPattern.matcher(source);
            if (matcher.find()) {
                return "CREATE OR REPLACE " + matcher.group(1) + " " + DBUtils.getQuotedIdentifier(object.getSchema())
                    + "." + matcher.group(2) + source.substring(matcher.end());
            }
        }
        return source;
    }

    @Nullable
    private static String buildSourceContent(@NotNull DBRProgressMonitor monitor, @NotNull JDBCResultSet dbResult) throws SQLException {
        StringBuilder source = null;
        int lineCount = 0;
        while (dbResult.next() && !monitor.isCanceled()) {
            String line = dbResult.getString(1);
            if (source == null) {
                source = new StringBuilder(200);
            }
            source.append(line == null ? "" : line);
            lineCount++;
            monitor.subTask(SUB_TASK_LINE_PREFIX + lineCount);
        }

        return source == null ? null : source.toString();
    }

    @Nullable
    private static String preCheckSourceObject(@NotNull OracleSourceObject sourceObject) {
        if (sourceObject.getSourceType().isCustom()) {
            log.warn("Can't read source for custom source objects");
            return DEFAULT_CUSTOM_SOURCE;
        }

        OracleSchema sourceOwner = sourceObject.getSchema();
        if (sourceOwner == null) {
            String warnMsg = "No source owner for object '" + sourceObject.getName() + "'";
            log.warn(warnMsg);
            return null;
        }
        return null;
    }

    private static void initMonitor(@NotNull DBRProgressMonitor monitor, @NotNull OracleSourceObject sourceObject) {
        String taskName = "Load sources for '" + sourceObject.getName() + "'...";
        monitor.beginTask(taskName, MONITOR_TOTAL_WORK);
    }

    @NotNull
    private static String getSysViewName(@NotNull OracleDataSource dataSource, @NotNull DBRProgressMonitor monitor) {
        String sysViewName = OracleConstants.VIEW_DBA_SOURCE;
        if (!dataSource.isViewAvailable(monitor, OracleConstants.SCHEMA_SYS, sysViewName)) {
            sysViewName = OracleConstants.VIEW_ALL_SOURCE;
        }
        return sysViewName;
    }

    @Nullable
    private static String querySourceContent(@NotNull DBRProgressMonitor monitor, @NotNull OracleSourceObject sourceObject, boolean body,
                                             @NotNull String sysViewName) throws DBCException {
        OracleSchema sourceOwner = sourceObject.getSchema();
        String sourceType = getAdaptedSourceType(sourceObject.getSourceType().name());
        String sourceName = sourceObject.getName();

        String taskDesc = "Load source code for " + sourceType + " '" + sourceName + "'";
        try (JDBCSession session = DBUtils.openMetaSession(monitor, sourceOwner, taskDesc)) {
            try (JDBCPreparedStatement dbStat = buildSourceQueryStatement(sourceObject, session, sysViewName,
                sourceType, sourceOwner.getName(), sourceName, body)) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    return buildSourceContent(monitor, dbResult);
                }
            } catch (SQLException e) {
                throw new DBCException(e, session.getExecutionContext());
            }
        }
    }

    @NotNull
    private static String getAdaptedSourceType(@NotNull String originalType) {
        String sourceType = originalType.replace("_", " ");
        return sourceType.equalsIgnoreCase("FUNCTION") ? "UDF" : sourceType;
    }

    @NotNull
    private static JDBCPreparedStatement buildSourceQueryStatement(
        @NotNull OracleSourceObject sourceObject,
        @NotNull JDBCSession session,
        @NotNull String sysViewName,
        @NotNull String sourceType,
        @NotNull String ownerName,
        @NotNull String sourceName,
        boolean body)
            throws SQLException {
        String sql = "SELECT TEXT FROM " + getSysSchemaPrefix(sourceObject.getDataSource()) + sysViewName
            + " WHERE TYPE=? AND OWNER=? AND NAME=?";

        JDBCPreparedStatement dbStat = session.prepareStatement(sql);
        dbStat.setString(1, body ? sourceType + " BODY" : sourceType);
        dbStat.setString(2, ownerName);
        dbStat.setString(3, sourceName);
        dbStat.setFetchSize(DBConstants.METADATA_FETCH_SIZE);
        return dbStat;
    }
}
