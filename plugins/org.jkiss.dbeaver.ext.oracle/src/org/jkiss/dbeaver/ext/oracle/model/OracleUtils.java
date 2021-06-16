/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.oracle.edit.OracleTableColumnManager;
import org.jkiss.dbeaver.ext.oracle.model.source.OracleSourceObject;
import org.jkiss.dbeaver.ext.oracle.model.source.OracleStatefulObject;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;
import org.jkiss.dbeaver.model.struct.DBStructUtils;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
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
        OracleTableBase object,
        OracleDDLFormat ddlFormat,
        Map<String, Object> options) throws DBException
    {
        String objectFullName = DBUtils.getObjectFullName(object, DBPEvaluationContext.DDL);

        OracleSchema schema = object.getContainer();
/*
        if (object instanceof OracleSchemaObject) {
            schema = ((OracleSchemaObject)object).getSchema();
        } else if (object instanceof OracleTableBase) {
            schema = ((OracleTableBase)object).getContainer();
        }
*/
        final OracleDataSource dataSource = object.getDataSource();

        monitor.beginTask("Load sources for " + objectType + " '" + objectFullName + "'...", 1);
        try (final JDBCSession session = DBUtils.openMetaSession(monitor, object, "Load source code for " + objectType + " '" + objectFullName + "'")) {
            if (dataSource.isAtLeastV9()) {
                try {
                    // Do not add semicolon in the end
//                    JDBCUtils.executeProcedure(
//                        session,
//                        "begin DBMS_METADATA.SET_TRANSFORM_PARAM(DBMS_METADATA.SESSION_TRANSFORM,'SQLTERMINATOR',true); end;");
                    JDBCUtils.executeProcedure(
                        session,
                        "begin\n" +
                                "DBMS_METADATA.SET_TRANSFORM_PARAM(DBMS_METADATA.SESSION_TRANSFORM,'SQLTERMINATOR',true);\n" +
                                "DBMS_METADATA.SET_TRANSFORM_PARAM(DBMS_METADATA.SESSION_TRANSFORM,'STORAGE'," + ddlFormat.isShowStorage() + ");\n" +
                                "DBMS_METADATA.SET_TRANSFORM_PARAM(DBMS_METADATA.SESSION_TRANSFORM,'TABLESPACE'," + ddlFormat.isShowTablespace() + ");\n" +
                                "DBMS_METADATA.SET_TRANSFORM_PARAM(DBMS_METADATA.SESSION_TRANSFORM,'SEGMENT_ATTRIBUTES'," + ddlFormat.isShowSegments() + ");\n" +
                                "DBMS_METADATA.SET_TRANSFORM_PARAM(DBMS_METADATA.SESSION_TRANSFORM,'EMIT_SCHEMA'," + CommonUtils.getOption(options, DBPScriptObject.OPTION_FULLY_QUALIFIED_NAMES, true) + ");\n" +
                            "end;");
                } catch (SQLException e) {
                    log.error("Can't apply DDL transform parameters", e);
                }
            }

            String ddl;
            // Read main object DDL
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT DBMS_METADATA.GET_DDL(?,?" + (schema == null ? "" : ",?") + ") TXT FROM DUAL")) {
                dbStat.setString(1, objectType);
                dbStat.setString(2, object.getName());
                if (schema != null) {
                    dbStat.setString(3, schema.getName());
                }
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        Object ddlValue = dbResult.getObject(1);
                        if (ddlValue instanceof Clob) {
                            StringWriter buf = new StringWriter();
                            try (Reader clobReader = ((Clob) ddlValue).getCharacterStream()) {
                                IOUtils.copyText(clobReader, buf);
                            } catch (IOException e) {
                                e.printStackTrace(new PrintWriter(buf, true));
                            }
                            ddl = buf.toString();

                        } else {
                            ddl = CommonUtils.toString(ddlValue);
                        }
                    } else {
                        log.warn("No DDL for " + objectType + " '" + objectFullName + "'");
                        return "-- EMPTY DDL";
                    }
                }
            }
            ddl = ddl.trim();

            if (!CommonUtils.isEmpty(object.getIndexes(monitor))) {
                // Add index info to main DDL. For some reasons, GET_DDL returns columns, constraints, but not indexes
                try (JDBCPreparedStatement dbStat = session.prepareStatement(
                        "SELECT DBMS_METADATA.GET_DEPENDENT_DDL('INDEX',?" + (schema == null ? "" : ",?") + ") TXT FROM DUAL")) {
                    dbStat.setString(1, object.getName());
                    if (schema != null) {
                        dbStat.setString(2, schema.getName());
                    }
                    try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                        if (dbResult.next()) {
                            ddl += "\n\n" + dbResult.getString(1).trim();
                        }
                    }
                } catch (Exception e) {
                    // No dependent index DDL or something went wrong
                    log.debug("Error reading dependent index DDL", e);
                }
            }

            if (ddlFormat != OracleDDLFormat.COMPACT) {
                // Add object and objects columns info to main DDL
                ddl = addCommentsToDDL(monitor, object, ddl);
            }
            return ddl;

        } catch (SQLException e) {
            if (object instanceof OracleTablePhysical) {
                log.error("Error generating Oracle DDL. Generate default.", e);
                return DBStructUtils.generateTableDDL(monitor, object, options, true);
            } else {
                throw new DBException(e, dataSource);
            }
        } finally {
            monitor.done();
        }
    }

    private static String addCommentsToDDL(DBRProgressMonitor monitor, OracleTableBase object, String ddl) {
        StringBuilder ddlBuilder = new StringBuilder(ddl);
        String objectFullName = object.getFullyQualifiedName(DBPEvaluationContext.DDL);

        String objectComment = object.getComment(monitor);
        if (!CommonUtils.isEmpty(objectComment)) {
            String objectTypeName = "TABLE";
            if (object instanceof OracleMaterializedView) {
                objectTypeName = "MATERIALIZED VIEW";
            }
            ddlBuilder.append("\n\n").append("COMMENT ON ").append(objectTypeName).append(" ").append(objectFullName).append(" IS ").
                    append(SQLUtils.quoteString(object.getDataSource(), objectComment)).append(SQLConstants.DEFAULT_STATEMENT_DELIMITER);
        }

        try {
            List<OracleTableColumn> attributes = object.getAttributes(monitor);
            if (!CommonUtils.isEmpty(attributes)) {
                List<DBEPersistAction> actions = new ArrayList<>();
                if (CommonUtils.isEmpty(objectComment)) {
                    ddlBuilder.append("\n");
                }
                for (OracleTableColumn column : CommonUtils.safeCollection(attributes)) {
                    String columnComment = column.getComment(monitor);
                    if (!CommonUtils.isEmpty(columnComment)) {
                        OracleTableColumnManager.addColumnCommentAction(actions, column, column.getTable());
                    }
                }
                if (!CommonUtils.isEmpty(actions)) {
                    for (DBEPersistAction action : actions) {
                        ddlBuilder.append("\n").append(action.getScript()).append(SQLConstants.DEFAULT_STATEMENT_DELIMITER);
                    }
                }
            }
        } catch (DBException e) {
            log.debug("Error reading object columns", e);
        }

        return ddlBuilder.toString();
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
                ((DBPScriptObjectExt)object).getExtendedDefinitionText(null) :
                object.getObjectDefinitionText(null, DBPScriptObject.EMPTY_OPTIONS);
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

    public static void addSchemaChangeActions(DBCExecutionContext executionContext, List<DBEPersistAction> actions, OracleSourceObject object)
    {
        OracleSchema schema = object.getSchema();
        if (schema == null) {
            return;
        }
        actions.add(0, new SQLDatabasePersistAction(
            "Set target schema",
            "ALTER SESSION SET CURRENT_SCHEMA=" + schema.getName(),
            DBEPersistAction.ActionType.INITIALIZER));
        OracleSchema defaultSchema = ((OracleExecutionContext)executionContext).getDefaultSchema();
        if (schema != defaultSchema && defaultSchema != null) {
            actions.add(new SQLDatabasePersistAction(
                "Set current schema",
                "ALTER SESSION SET CURRENT_SCHEMA=" + defaultSchema.getName(),
                DBEPersistAction.ActionType.FINALIZER));
        }
    }

    public static String getSysSchemaPrefix(OracleDataSource dataSource) {
        boolean useSysView = CommonUtils.toBoolean(dataSource.getContainer().getConnectionConfiguration().getProviderProperty(OracleConstants.PROP_METADATA_USE_SYS_SCHEMA));
        if (useSysView) {
            return OracleConstants.SCHEMA_SYS + ".";
        } else {
            return "";
        }
    }

    public static String getSource(DBRProgressMonitor monitor, OracleSourceObject sourceObject, boolean body, boolean insertCreateReplace) throws DBCException
    {
        if (sourceObject.getSourceType().isCustom()) {
            log.warn("Can't read source for custom source objects");
            return "-- ???? CUSTOM SOURCE";
        }
        final String sourceType = sourceObject.getSourceType().name().replace("_", " ");
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
        try (final JDBCSession session = DBUtils.openMetaSession(monitor, sourceOwner, "Load source code for " + sourceType + " '" + sourceObject.getName() + "'")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT TEXT FROM " + getSysSchemaPrefix(sourceObject.getDataSource()) + sysViewName + " " +
                    "WHERE TYPE=? AND OWNER=? AND NAME=? " +
                    "ORDER BY LINE")) {
                String sourceName;
                if (sourceObject instanceof OracleJavaClass) {
                    sourceName = ((OracleJavaClass) sourceObject).getSourceName();
                } else {
                    sourceName = sourceObject.getName();
                }
                dbStat.setString(1, body ? sourceType + " BODY" : sourceType);
                dbStat.setString(2, sourceOwner.getName());
                dbStat.setString(3, sourceName);
                dbStat.setFetchSize(DBConstants.METADATA_FETCH_SIZE);
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    StringBuilder source = null;
                    int lineCount = 0;
                    while (dbResult.next()) {
                        if (monitor.isCanceled()) {
                            break;
                        }
                        String line = dbResult.getString(1);
                        if (source == null) {
                            source = new StringBuilder(200);
                        }
                        if (line == null) {
                            line = "";
                        }
                        source.append(line);
                        if (sourceObject instanceof OracleJavaClass && !line.endsWith("\n")) {
                            // Java source
                            source.append("\n");
                        }
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
            } catch (SQLException e) {
                throw new DBCException(e, session.getExecutionContext());
            }
        } finally {
            monitor.done();
        }
    }

    public static String getSysUserViewName(DBRProgressMonitor monitor, OracleDataSource dataSource, String viewName)
    {
        String dbaView = "DBA_" + viewName;
        if (dataSource.isViewAvailable(monitor, OracleConstants.SCHEMA_SYS, dbaView)) {
            return OracleUtils.getSysSchemaPrefix(dataSource) + dbaView;
        } else {
            return OracleUtils.getSysSchemaPrefix(dataSource) + "USER_" + viewName;
        }
    }

    public static String getAdminAllViewPrefix(DBRProgressMonitor monitor, OracleDataSource dataSource, String viewName)
    {
        boolean useDBAView = CommonUtils.toBoolean(dataSource.getContainer().getConnectionConfiguration().getProviderProperty(OracleConstants.PROP_ALWAYS_USE_DBA_VIEWS));
        if (useDBAView) {
            String dbaView = "DBA_" + viewName;
            if (dataSource.isViewAvailable(monitor, OracleConstants.SCHEMA_SYS, dbaView)) {
                return OracleUtils.getSysSchemaPrefix(dataSource) + dbaView;
            }
        }
        return OracleUtils.getSysSchemaPrefix(dataSource) + "ALL_" + viewName;
    }

    public static String getSysCatalogHint(OracleDataSource dataSource)
    {
        return dataSource.isUseRuleHint() ? "/*+RULE*/" : "";
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
        if (reference instanceof String) {
            Object object;
            if (monitor != null) {
                object = cache.getObject(
                    monitor,
                    parent,
                    (String) reference);
            } else {
                object = cache.getCachedObject((String) reference);
            }
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
        try (JDBCSession session = DBUtils.openMetaSession(monitor, object, "Refresh state of " + objectType.getTypeName() + " '" + object.getName() + "'")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT STATUS FROM " + OracleUtils.getAdminAllViewPrefix(monitor, object.getDataSource(), "OBJECTS") + " WHERE OBJECT_TYPE=? AND OWNER=? AND OBJECT_NAME=?")) {
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
            } catch (SQLException e) {
                throw new DBCException(e, session.getExecutionContext());
            }
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

    public static String formatWord(String word)
	{
		if (word == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder(word.length());
		sb.append(Character.toUpperCase(word.charAt(0)));
		for (int i = 1; i < word.length(); i++) {
			char c = word.charAt(i);
			if ((c == 'i' || c == 'I') && sb.charAt(i - 1) == 'I') {
				sb.append('I');
			} else {
				sb.append(Character.toLowerCase(c));
			}
		}
		return sb.toString();
	}

    public static String formatSentence(String sent)
	{
		if (sent == null) {
			return "";
		}
		StringBuilder result = new StringBuilder();
		StringTokenizer st = new StringTokenizer(sent, " \t\n\r-,.\\/", true);
		while (st.hasMoreTokens()) {
			String word = st.nextToken();
			if (word.length() > 0) {
				result.append(formatWord(word));
			}
		}

		return result.toString();
	}
}
