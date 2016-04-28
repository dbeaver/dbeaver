/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.oracle.model.source.OracleSourceObject;
import org.jkiss.dbeaver.ext.oracle.model.source.OracleSourceObjectEx;
import org.jkiss.dbeaver.ext.oracle.model.source.OracleStatefulObject;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;

import java.sql.SQLException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Oracle utils
 */
public class OracleUtils {

    private static final Log log = Log.getLog(OracleUtils.class);

    public static String getDDL(
        DBRProgressMonitor monitor,
        String objectType,
        DBSEntity object,
        OracleDDLFormat ddlFormat) throws DBException
    {
        String objectFullName = DBUtils.getObjectFullName(object);
        OracleSchema schema = null;
        if (object instanceof OracleSchemaObject) {
            schema = ((OracleSchemaObject)object).getSchema();
        } else if (object instanceof OracleTableBase) {
            schema = ((OracleTableBase)object).getContainer();
        }
        final OracleDataSource dataSource = (OracleDataSource) object.getDataSource();
        assert(dataSource != null);
        monitor.beginTask("Load sources for " + objectType + " '" + objectFullName + "'...", 1);
        try (final JDBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Load source code for " + objectType + " '" + objectFullName + "'")) {
            JDBCUtils.executeProcedure(
                session,
                "begin DBMS_METADATA.SET_TRANSFORM_PARAM(DBMS_METADATA.SESSION_TRANSFORM,'STORAGE'," + ddlFormat.isShowStorage() + "); end;");
            JDBCUtils.executeProcedure(
                session,
                "begin DBMS_METADATA.SET_TRANSFORM_PARAM(DBMS_METADATA.SESSION_TRANSFORM,'TABLESPACE'," + ddlFormat.isShowTablespace() + ");  end;");
            JDBCUtils.executeProcedure(
                session,
                "begin DBMS_METADATA.SET_TRANSFORM_PARAM(DBMS_METADATA.SESSION_TRANSFORM,'SEGMENT_ATTRIBUTES'," + ddlFormat.isShowSegments() + ");  end;");
/*
            String curSchema = null;
            if (schema != null) {
                curSchema = getCurrentSchema(session);
                if (curSchema != null && !curSchema.equals(schema.getName())) {
                    setCurrentSchema(session, schema.getName());
                } else {
                    curSchema = null;
                }
            }
*/

            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT DBMS_METADATA.GET_DDL(?,?" +
                    (schema == null ? "" : ",?") +
                    ") TXT " +
                    "FROM DUAL")) {
                dbStat.setString(1, objectType);
                dbStat.setString(2, object.getName());
                if (schema != null) {
                    dbStat.setString(3, schema.getName());
                }
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        return dbResult.getString(1);
                    } else {
                        log.warn("No DDL for " + objectType + " '" + objectFullName + "'");
                        return "-- EMPTY DDL";
                    }
                }
            } finally {
/*
                if (curSchema != null) {
                    setCurrentSchema(session, curSchema);
                }
*/
            }
        } catch (SQLException e) {
            if (object instanceof OracleTableBase) {
                log.error("Error generating Oracle DDL. Generate default.", e);
                return JDBCUtils.generateTableDDL(monitor, (OracleTableBase)object, true);
            } else {
                throw new DBException(e, dataSource);
            }
        } finally {
            monitor.done();
        }
    }

    public static void setCurrentSchema(JDBCSession session, String schema) throws SQLException {
        JDBCUtils.executeSQL(session,
            "ALTER SESSION SET CURRENT_SCHEMA=" + DBUtils.getQuotedIdentifier(session.getDataSource(), schema));
    }

    public static String getCurrentSchema(JDBCSession session) throws SQLException {
        return JDBCUtils.queryString(
            session,
            "SELECT SYS_CONTEXT( 'USERENV', 'CURRENT_SCHEMA' ) FROM DUAL");
    }

    public static String normalizeSourceName(OracleSourceObject object, boolean body)
    {
        try {
            String source = body ?
                ((OracleSourceObjectEx)object).getObjectBodyDefinitionText(null) :
                object.getObjectDefinitionText(null);
            if (source == null) {
                return null;
            }
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                object.getSourceType() + (body ? "\\s+BODY" : "") +
                "\\s(\\s*)([\\w$\\.]+)[\\s\\(]+", java.util.regex.Pattern.CASE_INSENSITIVE);
            final Matcher matcher = pattern.matcher(source);
            if (matcher.find()) {
                String objectName = matcher.group(2);
                if (objectName.indexOf('.') == -1) {
                    if (!objectName.equalsIgnoreCase(object.getName())) {
                        object.setName(DBObjectNameCaseTransformer.transformObjectName(object, objectName));
                        object.getDataSource().getContainer().fireEvent(new DBPEvent(DBPEvent.Action.OBJECT_UPDATE, object));
                    }
                    return source;//.substring(0, matcher.start(1)) + object.getSchema().getName() + "." + objectName + source.substring(matcher.end(2));
                }
            }
            return source.trim();
        } catch (DBException e) {
            log.error(e);
            return null;
        }
    }

    public static void addSchemaChangeActions(List<DBEPersistAction> actions, OracleSourceObject object)
    {
        actions.add(0, new SQLDatabasePersistAction(
            "Set target schema",
            "ALTER SESSION SET CURRENT_SCHEMA=" + object.getSchema().getName(),
            DBEPersistAction.ActionType.INITIALIZER));
        if (object.getSchema() != object.getDataSource().getSelectedObject()) {
            actions.add(new SQLDatabasePersistAction(
                "Set current schema",
                "ALTER SESSION SET CURRENT_SCHEMA=" + object.getDataSource().getSelectedObject().getName(),
                DBEPersistAction.ActionType.FINALIZER));
        }
    }

    public static String getSource(DBRProgressMonitor monitor, OracleSourceObject sourceObject, boolean body, boolean insertCreateReplace) throws DBCException
    {
        if (sourceObject.getSourceType().isCustom()) {
            log.warn("Can't read source for custom source objects");
            return "-- ???? CUSTOM SOURCE";
        }
        final String sourceType = sourceObject.getSourceType().name();
        final OracleSchema sourceOwner = sourceObject.getSchema();
        if (sourceOwner == null) {
            log.warn("No source owner for object '" + sourceObject.getName() + "'");
            return null;
        }
        monitor.beginTask("Load sources for '" + sourceObject.getName() + "'...", 1);
        String sysViewName = OracleConstants.VIEW_DBA_SOURCE;
        if (!sourceObject.getDataSource().isViewAvailable(monitor, OracleConstants.SCHEMA_SYS, sysViewName)) {
            sysViewName = OracleConstants.VIEW_ALL_SOURCE;
        }
        try (final JDBCSession session = DBUtils.openMetaSession(monitor, sourceOwner.getDataSource(), "Load source code for " + sourceType + " '" + sourceObject.getName() + "'")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT TEXT FROM " + OracleConstants.SCHEMA_SYS + "." + sysViewName + " " +
                    "WHERE TYPE=? AND OWNER=? AND NAME=? " +
                    "ORDER BY LINE")) {
                dbStat.setString(1, body ? sourceType + " BODY" : sourceType);
                dbStat.setString(2, sourceOwner.getName());
                dbStat.setString(3, sourceObject.getName());
                dbStat.setFetchSize(DBConstants.METADATA_FETCH_SIZE);
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    StringBuilder source = null;
                    int lineCount = 0;
                    while (dbResult.next()) {
                        if (monitor.isCanceled()) {
                            break;
                        }
                        final String line = dbResult.getString(1);
                        if (source == null) {
                            source = new StringBuilder(200);
                        }
                        source.append(line);
                        lineCount++;
                        monitor.subTask("Line " + lineCount);
                    }
                    if (source == null) {
                        return null;
                    }
                    if (insertCreateReplace) {
                        return insertCreateReplace(sourceObject, body, source.toString());
                    } else {
                        return source.toString();
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBCException(e, sourceOwner.getDataSource());
        } finally {
            monitor.done();
        }
    }

    public static String getAdminViewPrefix(OracleDataSource dataSource)
    {
        return dataSource.isAdmin() ? "SYS.DBA_" : "SYS.USER_";
    }

    public static String getAdminAllViewPrefix(OracleDataSource dataSource)
    {
        return dataSource.isAdmin() ? "SYS.DBA_" : "SYS.ALL_";
    }

    static <PARENT extends DBSObject> Object resolveLazyReference(
        DBRProgressMonitor monitor,
        PARENT parent,
        DBSObjectCache<PARENT,?> cache,
        DBSObjectLazy<?> referrer,
        Object propertyId)
        throws DBException
    {
        final Object reference = referrer.getLazyReference(propertyId);
        if (reference instanceof String && monitor != null) {
            Object object = cache.getObject(
                monitor,
                parent,
                (String) reference);
            if (object != null) {
                return object;
            } else {
                log.warn("Object '" + reference + "' not found");
                return reference;
            }
        } else {
            return reference;
        }
    }

    public static boolean getObjectStatus(
        DBRProgressMonitor monitor,
        OracleStatefulObject object,
        OracleObjectType objectType)
        throws DBCException
    {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, object.getDataSource(), "Refresh state of " + objectType.getTypeName() + " '" + object.getName() + "'")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT STATUS FROM SYS.ALL_OBJECTS WHERE OBJECT_TYPE=? AND OWNER=? AND OBJECT_NAME=?")) {
                dbStat.setString(1, objectType.getTypeName());
                dbStat.setString(2, object.getSchema().getName());
                dbStat.setString(3, DBObjectNameCaseTransformer.transformObjectName(object, object.getName()));
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        return "VALID".equals(dbResult.getString("STATUS"));
                    } else {
                        log.warn(objectType.getTypeName() + " '" + object.getName() + "' not found in system dictionary");
                        return false;
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBCException(e, object.getDataSource());
        }
    }

    public static String insertCreateReplace(OracleSourceObject object, boolean body, String source) {
        String sourceType = object.getSourceType().name();
        if (body) {
            sourceType += " BODY";
        }
        Pattern srcPattern = Pattern.compile("^(" + sourceType + ")\\s+(\"{0,1}\\w+\"{0,1})", Pattern.CASE_INSENSITIVE);
        Matcher matcher = srcPattern.matcher(source);
        if (matcher.find()) {
            return
                "CREATE OR REPLACE " + matcher.group(1) + " " +
                DBUtils.getQuotedIdentifier(object.getSchema()) + "." + matcher.group(2) +
                source.substring(matcher.end());
        }
        return source;
    }
}
