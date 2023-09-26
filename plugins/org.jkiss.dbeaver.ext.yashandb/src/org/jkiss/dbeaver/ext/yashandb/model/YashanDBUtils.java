package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.yashandb.model.source.YashanDBStatefulObject;
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
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: donghy
 * @Date: 2022/08
 * @Description:
 */
public class YashanDBUtils {
    private static final Log log = Log.getLog(YashanDBUtils.class);


    /**
     * @Author: donghy
     * @createTime:  2023/2/15
     * @Param: [monitor, objectType, object, ddlFormat, options]
     * @Return: java.lang.String
     * @Description:get table/view ddl
     */
    public static String getTableOrViewDDL(
            DBRProgressMonitor monitor,
            String objectType,
            YashanDBTableBase object,
            Map<String, Object> options) throws DBException {

        String ddl = new String();
        String objectFullName = DBUtils.getObjectFullName(object, DBPEvaluationContext.DDL);
        YashanDBSchema schema = object.getContainer();

        final YashanDBDataSource dataSource = object.getDataSource();

        try(final JDBCSession session = DBUtils.openMetaSession(monitor, object, "Load source code for " + objectType + " '" + objectFullName + "'");
            JDBCPreparedStatement dbStat = session.prepareStatement("SELECT DBMS_METADATA.GET_DDL(?,?,?) FROM DUAL")
        ){
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

        }catch(SQLException | DBCException e){
            if (object instanceof YashanDBTablePhysical) {
                log.error("Error generating YashanDB DDL. Generate default.", e);
                /** if no ddl found,return default ddl. */
                return DBStructUtils.generateTableDDL(monitor, object, options, true);
            } else {
                throw new DBException(e, dataSource);
            }
        }finally {
            monitor.done();
        }
    }

    /**
     * This dataSource.isUseRuleHint() will be false always.
     */
    public static String getSysCatalogHint(YashanDBDataSource dataSource) {
        return dataSource.isUseRuleHint() ? "/*+RULE*/" : "";
    }

    /**
     * make sure the current schema is admin role.
     */
    public static String isAdminPrivilege(@NotNull JDBCSession session, @NotNull YashanDBSchema schema, String viewName) {
        try {
            JDBCPreparedStatement dbStat = session.prepareStatement("SELECT * FROM DBA_ROLE_PRIVS WHERE GRANTED_ROLE = 'DBA' ");
            JDBCResultSet resultSet = dbStat.executeQuery();

            while (resultSet.next()) {
                String granteeName = JDBCUtils.safeGetString(resultSet, "GRANTEE");
                if (schema.getName().equals(granteeName)) {
                    return "DBA_" + viewName;
                }
            }
        } catch (SQLException e) {
            // if dbStat is null.
            log.debug(schema.getName() + "does not have DBA privilege." + e.getMessage());
            return "ALL_" + viewName;
        }

        return "ALL_" + viewName;
    }

    /**
     * make sure the current schema is admin role.
     */
    public static String isAdminPriv(YashanDBDataSource dataSource, String viewName) {
        return dataSource.isAdminVisible() ? "DBA_" + viewName : "ALL_" + viewName;
    }

    public static String getAdminAllViewPrefix(DBRProgressMonitor monitor, YashanDBDataSource dataSource, String viewName) {
        boolean useDBAView = CommonUtils.toBoolean(dataSource.getContainer().getConnectionConfiguration().getProviderProperty(YashanDBConstants.PROP_ALWAYS_USE_DBA_VIEWS));
        if (useDBAView) {
            String dbaView = "DBA_" + viewName;
            if (dataSource.isViewAvailable(monitor, YashanDBConstants.SCHEMA_SYS, dbaView)) {
                return YashanDBUtils.getSysSchemaPrefix(dataSource) + dbaView;
            }
        }
        return YashanDBUtils.getSysSchemaPrefix(dataSource) + "ALL_" + viewName;
    }

    /**
     * make sure whether uses sys as schema prefix.
     */
    public static String getSysSchemaPrefix(YashanDBDataSource dataSource) {
        boolean useSysView = CommonUtils.toBoolean(dataSource.getContainer().getConnectionConfiguration().getProviderProperty(YashanDBConstants.PROP_METADATA_USE_SYS_SCHEMA));
        if (useSysView) {
            return YashanDBConstants.SCHEMA_SYS + ".";
        } else {
            return "";
        }
    }

    public static String normalizeSourceName(YashanDBSourceObject object, boolean body) {
        try {
            String source = body ?
                    ((DBPScriptObjectExt) object).getExtendedDefinitionText(null) :
                    object.getObjectDefinitionText(null, DBPScriptObject.EMPTY_OPTIONS);
            if (source == null) {
                return null;
            }
            Pattern pattern = Pattern.compile(
                    object.getSourceType() + (body ? "\\s+BODY" : "") +
                            "\\s(\\s*)([\\w$\\.]+)[\\s\\(]+", Pattern.CASE_INSENSITIVE);
            final Matcher matcher = pattern.matcher(source);
            if (matcher.find()) {
                String objectName = matcher.group(2);
                if (objectName.indexOf('.') == -1) {
                    if (!objectName.equalsIgnoreCase(object.getName())) {
                        object.setName(DBObjectNameCaseTransformer.transformObjectName(object, objectName));
                        object.getDataSource().getContainer().fireEvent(new DBPEvent(DBPEvent.Action.OBJECT_UPDATE, object));
                    }
                    return source;
                }
            }
            return source.trim();
        } catch (DBException e) {
            log.error(e);
            return null;
        }
    }

    public static void addSchemaChangeActions(DBCExecutionContext executionContext, List<DBEPersistAction> actions, YashanDBSourceObject object) {
        YashanDBSchema schema = object.getSchema();
        if (schema == null) {
            return;
        }
        actions.add(0, new SQLDatabasePersistAction(
                "Set target schema",
                "ALTER SESSION SET CURRENT_SCHEMA=" + schema.getName(),
                DBEPersistAction.ActionType.INITIALIZER));
        YashanDBSchema defaultSchema = ((YashanDBExecutionContext) executionContext).getDefaultSchema();
        if (schema != defaultSchema && defaultSchema != null) {
            actions.add(new SQLDatabasePersistAction(
                    "Set current schema",
                    "ALTER SESSION SET CURRENT_SCHEMA=" + defaultSchema.getName(),
                    DBEPersistAction.ActionType.FINALIZER));
        }
    }

    public static String getSource(DBRProgressMonitor monitor, YashanDBSourceObject sourceObject, boolean body, boolean insertCreateReplace) throws DBCException {
        if (sourceObject.getSourceType().isCustom()) {
            log.warn("Can't read source for custom source objects");
            return "-- ???? CUSTOM SOURCE";
        }
        String sourceType = sourceObject.getSourceType().name().replace("_", " ");
        final YashanDBSchema sourceOwner = sourceObject.getSchema();
        if (sourceOwner == null) {
            log.warn("No source owner for object '" + sourceObject.getName() + "'");
            return null;
        }
        monitor.beginTask("Load sources for '" + sourceObject.getName() + "'...", 1);
        String sysViewName = YashanDBConstants.VIEW_DBA_SOURCE;
        if (!sourceObject.getDataSource().isViewAvailable(monitor, YashanDBConstants.SCHEMA_SYS, sysViewName)) {
            sysViewName = YashanDBConstants.VIEW_ALL_SOURCE;
        }
        try (final JDBCSession session = DBUtils.openMetaSession(monitor, sourceOwner, "Load source code for " + sourceType + " '" + sourceObject.getName() + "'")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT TEXT FROM " + getSysSchemaPrefix(sourceObject.getDataSource()) + sysViewName + " " +
                            "WHERE TYPE=? AND OWNER=? AND NAME=? ")) {
                String sourceName = sourceObject.getName();
                sourceType = sourceType.equalsIgnoreCase("FUNCTION") ? "UDF":sourceType;

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

    public static boolean isYashanDBObject(Object object) {
        if (object == null) {
            return false;
        }
        String className = object.getClass().getName();
        return className.equals(YashanDBConstants.YASHANDB_OBJECT_CLASS);
    }

    public static Object extractYashanDBObjectValue(Object object) {
        if (object == null) {
            return null;
        }
        if (!isYashanDBObject(object)) {
            return object;
        }
        try {
            return object.getClass().getMethod("getValue").invoke(object);
        } catch (Exception e) {
            log.debug("Can't extract value from " + object.getClass().getName(), e);
        }
        return null;
    }

    public static boolean getObjectStatus(
            DBRProgressMonitor monitor,
            YashanDBStatefulObject object,
            YashanDBObjectType objectType)
            throws DBCException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, object, "Refresh state of " + objectType.getTypeName() + " '" + object.getName() + "'")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT STATUS FROM " + YashanDBUtils.isAdminPriv(object.getDataSource(), "OBJECTS") + " WHERE OBJECT_TYPE=? AND OWNER=? AND OBJECT_NAME=?")) {
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

    public static String insertCreateReplace(YashanDBSourceObject object, boolean body, String source) {
        String sourceType = object.getSourceType().name();
        if(sourceType.equalsIgnoreCase("UDF")){
            sourceType="FUNCTION";
        }
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

    static <PARENT extends DBSObject> Object resolveLazyReference(
            DBRProgressMonitor monitor,
            PARENT parent,
            DBSObjectCache<PARENT, ?> cache,
            DBSObjectLazy<?> referrer,
            Object propertyId)
            throws DBException {
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

    public static String getSysUserViewName(DBRProgressMonitor monitor, YashanDBDataSource dataSource, String viewName) {
        String dbaView = "DBA_" + viewName;
        if (dataSource.isViewAvailable(monitor, YashanDBConstants.SCHEMA_SYS, dbaView)) {
            return YashanDBUtils.getSysSchemaPrefix(dataSource) + dbaView;
        } else {
            return YashanDBUtils.getSysSchemaPrefix(dataSource) + "USER_" + viewName;
        }
    }

    @NotNull
    public static String getQuotedIdentifier(@NotNull DBPDataSource dataSource, @NotNull String str) {
        return getQuotedIdentifier(dataSource, str, true, false);
    }

    @NotNull
    public static String getQuotedIdentifier(@NotNull DBPDataSource dataSource, @NotNull String str, boolean caseSensitiveNames, boolean quoteAlways) {
        return dataSource.getSQLDialect().getQuotedIdentifier(str, caseSensitiveNames, quoteAlways);
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

    /**
     * It won't add double quotes automatically to object name when creating schema、column、table、view in YashanDB.
     * Here it will manually add double quotes by determining whether object name contains the lower letter.
     */
    public static String addDoubleQuotationMarks(String str) {
        if (str.contains("\"")) return str;
        String[] models = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"};
        for (String model : models) {
            if (str.contains(model)) {
                return "\"" + str + "\"";
            }
        }
        return str;
    }

    /**
     * It will manually add double quotes by access YashandbTablebase class getFullyQualifiedName method if object name contains the lower letter .
     */
    public static String getRightSchemaTable(String str) {
        String[] strs = str.split("\\.");
        return YashanDBUtils.addDoubleQuotationMarks(strs[0]) + "." + YashanDBUtils.addDoubleQuotationMarks(strs[1]);
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
}
