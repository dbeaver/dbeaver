/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.ext.oracle.model.plan.OracleQueryPlanner;
import org.jkiss.dbeaver.ext.oracle.model.session.OracleServerSessionManager;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.access.DBAPasswordChangeInfo;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.model.auth.DBAUserCredentialsProvider;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.*;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.impl.auth.AuthModelDatabaseNative;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLQueryResult;
import org.jkiss.dbeaver.model.sql.SQLState;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.struct.DBSStructureAssistant;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.BeanUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.StandardConstants;

import java.io.PrintWriter;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GenericDataSource
 */
public class OracleDataSource extends JDBCDataSource implements DBAUserCredentialsProvider, DBPObjectStatisticsCollector, IAdaptable {
    private static final Log log = Log.getLog(OracleDataSource.class);

    final public SchemaCache schemaCache = new SchemaCache();
    final DataTypeCache dataTypeCache = new DataTypeCache();
    final TablespaceCache tablespaceCache = new TablespaceCache();
    final UserCache userCache = new UserCache();
    final ProfileCache profileCache = new ProfileCache();
    final RoleCache roleCache = new RoleCache();

    private OracleOutputReader outputReader;
    private OracleSchema publicSchema;
    private boolean isAdmin;
    private boolean isAdminVisible;
    private String planTableName;
    private boolean useRuleHint;
    private boolean resolveGeometryAsStruct = true;
    private boolean hasStatistics;

    private final Map<String, Boolean> availableViews = new HashMap<>();

    public OracleDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container)
        throws DBException {
        super(monitor, container, new OracleSQLDialect());
        this.outputReader = new OracleOutputReader();

        OracleConfigurator configurator = GeneralUtils.adapt(this, OracleConfigurator.class);
        if (configurator != null) {
            resolveGeometryAsStruct = configurator.resolveGeometryAsStruct();
        }
    }

    @Override
    public Object getDataSourceFeature(String featureId) {
        switch (featureId) {
            case DBConstants.FEATURE_MAX_STRING_LENGTH:
                return 4000;
        }

        return super.getDataSourceFeature(featureId);
    }

    public boolean isViewAvailable(@NotNull DBRProgressMonitor monitor, @Nullable String schemaName, @NotNull String viewName) {
        viewName = viewName.toUpperCase();
        Boolean available;
        synchronized (availableViews) {
            available = availableViews.get(viewName);
        }
        if (available == null) {
            try {
                try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Check view existence")) {
                    String viewNameQuoted = DBUtils.getQuotedIdentifier(this, viewName);
                    try (final JDBCPreparedStatement dbStat = session.prepareStatement(
                        "SELECT 1 FROM " +
                            (schemaName == null ? viewNameQuoted : DBUtils.getQuotedIdentifier(this, schemaName) + "." + viewNameQuoted) +
                            " WHERE 1<>1"))
                    {
                        dbStat.setFetchSize(1);
                        dbStat.execute();
                        available = true;
                    }
                }
            } catch (SQLException e) {
                available = false;
            }
            synchronized (availableViews) {
                availableViews.put(viewName, available);
            }
        }
        return available;
    }

    @Override
    protected Connection openConnection(@NotNull DBRProgressMonitor monitor, @Nullable JDBCExecutionContext context, @NotNull String purpose) throws DBCException {
/*
        // Set tns admin directory
        DBPConnectionConfiguration connectionInfo = getContainer().getActualConnectionConfiguration();
        String tnsPathProp = CommonUtils.toString(connectionInfo.getProviderProperty(OracleConstants.PROP_TNS_PATH));
        if (!CommonUtils.isEmpty(tnsPathProp)) {
            System.setProperty(OracleConstants.VAR_ORACLE_NET_TNS_ADMIN, tnsPathProp);
        } else {
            DBPNativeClientLocation clientHome = getContainer().getNativeClientHome();
            if (clientHome != null) {
                System.setProperty(OracleConstants.VAR_ORACLE_NET_TNS_ADMIN, new File(clientHome.getPath(), OCIUtils.TNSNAMES_FILE_PATH).getAbsolutePath());
            }
        }
*/

        try {
            return super.openConnection(monitor, context, purpose);
        } catch (DBCException e) {
            if (e.getErrorCode() == OracleConstants.EC_PASSWORD_EXPIRED) {
                // Here we could try to ask for expired password change
                // This is supported  for thin driver since Oracle 12.2
                if (changeExpiredPassword(monitor, context, purpose)) {
                    // Retry
                    return openConnection(monitor, context, purpose);
                }
            }
            throw e;
        }
    }

    private boolean changeExpiredPassword(DBRProgressMonitor monitor, JDBCExecutionContext context, String purpose) {
        // Ref: https://stackoverflow.com/questions/21733300/oracle-password-expiry-and-grace-period-handling-using-java-oracle-jdbc

        DBPConnectionConfiguration connectionInfo = getContainer().getActualConnectionConfiguration();
        DBAPasswordChangeInfo passwordInfo = DBWorkbench.getPlatformUI().promptUserPasswordChange("Password has expired. Set new password.", connectionInfo.getUserName(), connectionInfo.getUserPassword());
        if (passwordInfo == null) {
            return false;
        }

        // Obtain connection
        try {
            if (passwordInfo.getNewPassword() == null) {
                throw new DBException("You can't set empty password");
            }
            Properties connectProps = getAllConnectionProperties(monitor, context, purpose, connectionInfo);
            connectProps.setProperty("oracle.jdbc.newPassword", passwordInfo.getNewPassword());

            final String url = getConnectionURL(connectionInfo);
            monitor.subTask("Connecting for expired password change");
            Driver driverInstance = getDriverInstance(monitor);
            try (Connection connection = driverInstance.connect(url, connectProps)) {
                if (connection == null) {
                    throw new DBCException("Null connection returned");
                }
            }

            connectionInfo.setUserPassword(passwordInfo.getNewPassword());
            getContainer().getConnectionConfiguration().setUserPassword(passwordInfo.getNewPassword());
            getContainer().getRegistry().flushConfig();
            return true;
        }
        catch (Exception e) {
            DBWorkbench.getPlatformUI().showError("Error changing password", "Error changing expired password", e);
            return false;
        }
    }

    @Override
    protected JDBCExecutionContext createExecutionContext(JDBCRemoteInstance instance, String type) {
        return new OracleExecutionContext(instance, type);
    }

    protected void initializeContextState(@NotNull DBRProgressMonitor monitor, @NotNull JDBCExecutionContext context, JDBCExecutionContext initFrom) throws DBException {
        if (outputReader == null) {
            outputReader = new OracleOutputReader();
        }
        // Enable DBMS output
        outputReader.enableServerOutput(
            monitor,
            context,
            outputReader.isServerOutputEnabled());
        if (initFrom != null) {
            ((OracleExecutionContext)context).setCurrentSchema(monitor, ((OracleExecutionContext)initFrom).getDefaultSchema());
        } else {
            ((OracleExecutionContext)context).refreshDefaults(monitor, true);
        }

        {
            DBPConnectionConfiguration connectionInfo = getContainer().getConnectionConfiguration();

            try (JDBCSession session = context.openSession(monitor, DBCExecutionPurpose.META, "Set connection parameters")) {
                // Set session settings
                String sessionLanguage = connectionInfo.getProviderProperty(OracleConstants.PROP_SESSION_LANGUAGE);
                if (sessionLanguage != null) {
                    try {
                        JDBCUtils.executeSQL(
                            session,
                            "ALTER SESSION SET NLS_LANGUAGE='" + sessionLanguage + "'");
                    } catch (Throwable e) {
                        log.warn("Can't set session language", e);
                    }
                }
                String sessionTerritory = connectionInfo.getProviderProperty(OracleConstants.PROP_SESSION_TERRITORY);
                if (sessionTerritory != null) {
                    try {
                        JDBCUtils.executeSQL(
                            session,
                            "ALTER SESSION SET NLS_TERRITORY='" + sessionTerritory + "'");
                    } catch (Throwable e) {
                        log.warn("Can't set session territory", e);
                    }
                }
                String nlsDateFormat = connectionInfo.getProviderProperty(OracleConstants.PROP_SESSION_NLS_DATE_FORMAT);
                if (nlsDateFormat != null) {
                    try {
                        JDBCUtils.executeSQL(
                            session,
                            "ALTER SESSION SET NLS_DATE_FORMAT='" + nlsDateFormat + "'");
                    } catch (Throwable e) {
                        log.warn("Can't set session NLS date format", e);
                    }
                }

                if (JDBCExecutionContext.TYPE_METADATA.equals(context.getContextName())) {
                    if (CommonUtils.toBoolean(connectionInfo.getProviderProperty(OracleConstants.PROP_USE_META_OPTIMIZER))) {
                        // See #5633
                        try {
                            JDBCUtils.executeSQL(session, "ALTER SESSION SET \"_optimizer_push_pred_cost_based\" = FALSE");
                            JDBCUtils.executeSQL(session, "ALTER SESSION SET \"_optimizer_squ_bottomup\" = FALSE");
                            JDBCUtils.executeSQL(session, "ALTER SESSION SET \"_optimizer_cost_based_transformation\" = 'OFF'");
                        } catch (Throwable e) {
                            log.warn("Can't set session optimizer parameters", e);
                        }
                    }
                }
            }
        }
    }

    public OracleSchema getDefaultSchema() {
        return (OracleSchema) DBUtils.getDefaultContext(this, true).getContextDefaults().getDefaultSchema();
    }

    @Override
    public String getConnectionUserName(@NotNull DBPConnectionConfiguration connectionInfo) {
        String userName = connectionInfo.getUserName();
        String authModelId = connectionInfo.getAuthModelId();
        if (!CommonUtils.isEmpty(authModelId) && !AuthModelDatabaseNative.ID.equals(authModelId)) {
            return userName;
        }
        // FIXME: left for backward compatibility. Replaced by auth model. Remove in future.
        if (!CommonUtils.isEmpty(userName) && userName.contains(" AS ")) {
            return userName;
        }
        final String role = connectionInfo.getProviderProperty(OracleConstants.PROP_INTERNAL_LOGON);
        return role == null ? userName : userName + " AS " + role;
    }

    @Override
    public String getConnectionUserPassword(@NotNull DBPConnectionConfiguration connectionInfo) {
        return connectionInfo.getUserPassword();
    }

    @Override
    protected DBPDataSourceInfo createDataSourceInfo(DBRProgressMonitor monitor, @NotNull JDBCDatabaseMetaData metaData) {
        return new OracleDataSourceInfo(this, metaData);
    }

    @Override
    public ErrorType discoverErrorType(@NotNull Throwable error) {
        Throwable rootCause = GeneralUtils.getRootCause(error);
        if (rootCause instanceof SQLException && ((SQLException) rootCause).getErrorCode() == OracleConstants.EC_FEATURE_NOT_SUPPORTED) {
            return ErrorType.FEATURE_UNSUPPORTED;
        }
        return super.discoverErrorType(error);
    }

    @Override
    protected Map<String, String> getInternalConnectionProperties(DBRProgressMonitor monitor, DBPDriver driver, JDBCExecutionContext context, String purpose, DBPConnectionConfiguration connectionInfo) throws DBCException {
        Map<String, String> connectionsProps = new HashMap<>();
        if (!getContainer().getPreferenceStore().getBoolean(ModelPreferences.META_CLIENT_NAME_DISABLE)) {
            // Program name
            String appName = DBUtils.getClientApplicationName(getContainer(), context, purpose);
            appName = appName.replace('(', '_').replace(')', '_'); // Replace brackets - Oracle don't like them
            connectionsProps.put("v$session.program", CommonUtils.truncateString(appName, 48));
        }
        // FIXME: left for backward compatibility. Replaced by auth model. Remove in future.
        if (CommonUtils.toBoolean(connectionInfo.getProviderProperty(OracleConstants.OS_AUTH_PROP))) {
            connectionsProps.put("v$session.osuser", System.getProperty(StandardConstants.ENV_USER_NAME));
        }
        return connectionsProps;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public boolean isAdminVisible() {
        return isAdmin || isAdminVisible;
    }

    public boolean isUseRuleHint() {
        return useRuleHint;
    }

    @Association
    public Collection<OracleSchema> getSchemas(DBRProgressMonitor monitor) throws DBException {
        return schemaCache.getAllObjects(monitor, this);
    }

    public OracleSchema getSchema(DBRProgressMonitor monitor, String name) throws DBException {
        if (publicSchema != null && publicSchema.getName().equals(name)) {
            return publicSchema;
        }
        return schemaCache.getObject(monitor, this, name);
    }

    @Association
    public Collection<OracleTablespace> getTablespaces(DBRProgressMonitor monitor) throws DBException {
        return tablespaceCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<OracleUser> getUsers(DBRProgressMonitor monitor) throws DBException {
        return userCache.getAllObjects(monitor, this);
    }

    @Association
    public OracleUser getUser(DBRProgressMonitor monitor, String name) throws DBException {
        return userCache.getObject(monitor, this, name);
    }

    @Association
    public Collection<OracleUserProfile> getProfiles(DBRProgressMonitor monitor) throws DBException {
        return profileCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<OracleRole> getRoles(DBRProgressMonitor monitor) throws DBException {
        return roleCache.getAllObjects(monitor, this);
    }

    public OracleGrantee getGrantee(DBRProgressMonitor monitor, String name) throws DBException {
        OracleUser user = userCache.getObject(monitor, this, name);
        if (user != null) {
            return user;
        }
        return roleCache.getObject(monitor, this, name);
    }

    @Association
    public Collection<OracleSynonym> getPublicSynonyms(DBRProgressMonitor monitor) throws DBException {
        return publicSchema.getSynonyms(monitor);
    }

    @Association
    public Collection<OracleDBLink> getPublicDatabaseLinks(DBRProgressMonitor monitor) throws DBException {
        return publicSchema.getDatabaseLinks(monitor);
    }

    @Association
    public Collection<OracleRecycledObject> getUserRecycledObjects(DBRProgressMonitor monitor) throws DBException {
        return publicSchema.getRecycledObjects(monitor);
    }

    public boolean isAtLeastV9() {
        return getInfo().getDatabaseVersion().getMajor() >= 9;
    }

    public boolean isAtLeastV10() {
        return getInfo().getDatabaseVersion().getMajor() >= 10;
    }

    public boolean isAtLeastV11() {
        return getInfo().getDatabaseVersion().getMajor() >= 11;
    }

    public boolean isAtLeastV12() {
        return getInfo().getDatabaseVersion().getMajor() >= 12;
    }

    @Override
    public void initialize(@NotNull DBRProgressMonitor monitor)
        throws DBException {
        super.initialize(monitor);

        DBPConnectionConfiguration connectionInfo = getContainer().getConnectionConfiguration();

        {
            String useRuleHintProp = connectionInfo.getProviderProperty(OracleConstants.PROP_USE_RULE_HINT);
            if (useRuleHintProp != null) {
                useRuleHint = CommonUtils.getBoolean(useRuleHintProp, false);
            }
        }

        this.publicSchema = new OracleSchema(this, 1, OracleConstants.USER_PUBLIC);
        {
            try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load data source meta info")) {
                // Check DBA role
                this.isAdmin = "YES".equals(
                    JDBCUtils.queryString(
                        session,
                        "SELECT 'YES' FROM USER_ROLE_PRIVS WHERE GRANTED_ROLE='DBA'"));
                this.isAdminVisible = isAdmin;
                if (!isAdminVisible) {
                    String showAdmin = connectionInfo.getProviderProperty(OracleConstants.PROP_ALWAYS_SHOW_DBA);
                    if (showAdmin != null) {
                        isAdminVisible = CommonUtils.getBoolean(showAdmin, false);
                    }
                }
            } catch (SQLException e) {
                //throw new DBException(e);
                log.warn(e);
            }
        }
        // Cache data types
        {
            List<OracleDataType> dtList = new ArrayList<>();
            for (Map.Entry<String, OracleDataType.TypeDesc> predefinedType : OracleDataType.PREDEFINED_TYPES.entrySet()) {
                OracleDataType dataType = new OracleDataType(this, predefinedType.getKey(), true);
                dtList.add(dataType);
            }
            this.dataTypeCache.setCache(dtList);
        }
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor)
        throws DBException {
        super.refreshObject(monitor);

        this.schemaCache.clearCache();
        //this.dataTypeCache.clearCache();
        this.tablespaceCache.clearCache();
        this.userCache.clearCache();
        this.profileCache.clearCache();
        this.roleCache.clearCache();

        this.initialize(monitor);

        return this;
    }

    @Override
    public Collection<OracleSchema> getChildren(@NotNull DBRProgressMonitor monitor)
        throws DBException {
        return getSchemas(monitor);
    }

    @Override
    public OracleSchema getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName)
        throws DBException {
        return getSchema(monitor, childName);
    }

    @Override
    public Class<? extends OracleSchema> getChildType(@NotNull DBRProgressMonitor monitor)
        throws DBException {
        return OracleSchema.class;
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope)
        throws DBException {

    }

    @Nullable
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == DBSStructureAssistant.class) {
            return adapter.cast(new OracleStructureAssistant(this));
        } else if (adapter == DBCServerOutputReader.class) {
            return adapter.cast(outputReader);
        } else if (adapter == DBAServerSessionManager.class) {
            return adapter.cast(new OracleServerSessionManager(this));
        } else if (adapter == DBCQueryPlanner.class) {
            return adapter.cast(new OracleQueryPlanner(this));
        }
        return super.getAdapter(adapter);
    }

    @Override
    public void cancelStatementExecute(DBRProgressMonitor monitor, JDBCStatement statement) throws DBException {
        if (driverSupportsQueryCancel()) {
            super.cancelStatementExecute(monitor, statement);
        } else {
            // Oracle server doesn't support single query cancel?
            // But we could try to cancel all
            try {
                Connection connection = statement.getConnection().getOriginal();
                BeanUtils.invokeObjectMethod(connection, "cancel");
            } catch (Throwable e) {
                throw new DBException("Can't cancel session queries", e, this);
            }
        }
    }

    private boolean driverSupportsQueryCancel() {
        return true;
    }

    @NotNull
    @Override
    public OracleDataSource getDataSource() {
        return this;
    }

    @NotNull
    @Override
    public DBPDataKind resolveDataKind(@NotNull String typeName, int valueType) {
        if ((typeName.equals(OracleConstants.TYPE_NAME_XML) || typeName.equals(OracleConstants.TYPE_FQ_XML))) {
            return DBPDataKind.CONTENT;
        }
        if ((typeName.equals(OracleConstants.TYPE_NAME_GEOMETRY) || typeName.equals(OracleConstants.TYPE_FQ_GEOMETRY))) {
            return resolveGeometryAsStruct ? DBPDataKind.STRUCT : DBPDataKind.OBJECT;
        }
        DBPDataKind dataKind = OracleDataType.getDataKind(typeName);
        if (dataKind != null) {
            return dataKind;
        }
        return super.resolveDataKind(typeName, valueType);
    }

    @Override
    public Collection<? extends DBSDataType> getLocalDataTypes() {
        return dataTypeCache.getCachedObjects();
    }

    @Override
    public OracleDataType getLocalDataType(String typeName) {
        return dataTypeCache.getCachedObject(typeName);
    }

    @Nullable
    @Override
    public OracleDataType resolveDataType(@NotNull DBRProgressMonitor monitor, @NotNull String typeFullName) throws DBException {
        int divPos = typeFullName.indexOf(SQLConstants.STRUCT_SEPARATOR);
        if (divPos == -1) {
            // Simple type name
            return getLocalDataType(typeFullName);
        } else {
            String schemaName = typeFullName.substring(0, divPos);
            String typeName = typeFullName.substring(divPos + 1);
            OracleSchema schema = getSchema(monitor, schemaName);
            if (schema == null) {
                return null;
            }
            return schema.getDataType(monitor, typeName);
        }
    }

    @Nullable
    public String getPlanTableName(JDBCSession session)
        throws DBException
    {
        if (planTableName == null) {
            String[] candidateNames;
            String tableName = getContainer().getPreferenceStore().getString(OracleConstants.PREF_EXPLAIN_TABLE_NAME);
            if (!CommonUtils.isEmpty(tableName)) {
                candidateNames = new String[]{tableName};
            } else {
                candidateNames = new String[]{"PLAN_TABLE", "TOAD_PLAN_TABLE"};
            }
            for (String candidate : candidateNames) {
                try {
                    JDBCUtils.executeSQL(session, "SELECT 1 FROM " + candidate);
                } catch (SQLException e) {
                    // No such table
                    continue;
                }
                planTableName = candidate;
                break;
            }
            if (planTableName == null) {
                final String newPlanTableName = candidateNames[0];
                // Plan table not found - try to create new one
                if (!DBWorkbench.getPlatformUI().confirmAction(
                    "Oracle PLAN_TABLE missing",
                    "PLAN_TABLE not found in current user's session. " +
                        "Do you want DBeaver to create new PLAN_TABLE (" + newPlanTableName + ")?")) {
                    return null;
                }
                planTableName = createPlanTable(session, newPlanTableName);
            }
        }
        return planTableName;
    }

    private String createPlanTable(JDBCSession session, String tableName) throws DBException {
        try {
            JDBCUtils.executeSQL(session, OracleConstants.PLAN_TABLE_DEFINITION.replace("${TABLE_NAME}", tableName));
        } catch (SQLException e) {
            throw new DBException("Error creating PLAN table", e, this);
        }
        return tableName;
    }

    @Nullable
    @Override
    public DBCQueryTransformer createQueryTransformer(@NotNull DBCQueryTransformType type) {
        if (type == DBCQueryTransformType.RESULT_SET_LIMIT) {
            //return new QueryTransformerRowNum();
        }
        return super.createQueryTransformer(type);
    }

    private Pattern ERROR_POSITION_PATTERN = Pattern.compile(".+\\s+line ([0-9]+), column ([0-9]+)");
    private Pattern ERROR_POSITION_PATTERN_2 = Pattern.compile(".+\\s+at line ([0-9]+)");
    private Pattern ERROR_POSITION_PATTERN_3 = Pattern.compile(".+\\s+at position\\: ([0-9]+)");

    @Nullable
    @Override
    public ErrorPosition[] getErrorPosition(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext context, @NotNull String query, @NotNull Throwable error) {
        while (error instanceof DBException) {
            if (error.getCause() == null) {
                break;
            }
            error = error.getCause();
        }
        String message = error.getMessage();
        if (!CommonUtils.isEmpty(message)) {
            List<ErrorPosition> positions = new ArrayList<>();
            Matcher matcher = ERROR_POSITION_PATTERN.matcher(message);
            while (matcher.find()) {
                DBPErrorAssistant.ErrorPosition pos = new DBPErrorAssistant.ErrorPosition();
                pos.info = matcher.group(1);
                pos.line = Integer.parseInt(matcher.group(1)) - 1;
                pos.position = Integer.parseInt(matcher.group(2)) - 1;
                positions.add(pos);
            }
            if (positions.isEmpty()) {
                matcher = ERROR_POSITION_PATTERN_2.matcher(message);
                while (matcher.find()) {
                    DBPErrorAssistant.ErrorPosition pos = new DBPErrorAssistant.ErrorPosition();
                    pos.info = matcher.group(1);
                    pos.line = Integer.parseInt(matcher.group(1)) - 1;
                    positions.add(pos);
                }
            }
            if (positions.isEmpty()) {
                matcher = ERROR_POSITION_PATTERN_3.matcher(message);
                while (matcher.find()) {
                    DBPErrorAssistant.ErrorPosition pos = new DBPErrorAssistant.ErrorPosition();
                    pos.info = matcher.group(1);
                    pos.position = Integer.parseInt(matcher.group(1)) - 1;
                    positions.add(pos);
                }
            }

            if (!positions.isEmpty()) {
                return positions.toArray(new ErrorPosition[positions.size()]);
            }
        }
        if (error.getCause() != null) {
            // Maybe OracleDatabaseException
            try {
                Object errorPosition = BeanUtils.readObjectProperty(error.getCause(), "errorPosition");
                if (errorPosition instanceof Number) {
                    DBPErrorAssistant.ErrorPosition pos = new DBPErrorAssistant.ErrorPosition();
                    pos.position = ((Number) errorPosition).intValue();
                    return new ErrorPosition[]{pos};
                }
            } catch (Exception e) {
                // Nope, its not it
            }

        }
        if (error instanceof SQLException && SQLState.SQL_42000.getCode().equals(((SQLException) error).getSQLState())) {
            try (JDBCSession session = (JDBCSession) context.openSession(monitor, DBCExecutionPurpose.UTIL, "Extract last error position")) {
                try (CallableStatement stat = session.prepareCall(
                    "declare\n" +
                        "  l_cursor integer default dbms_sql.open_cursor; \n" +
                        "begin \n" +
                        "  begin \n" +
                        "  dbms_sql.parse(  l_cursor, ?, dbms_sql.native ); \n" +
                        "    exception \n" +
                        "      when others then ? := dbms_sql.last_error_position; \n" +
                        "    end; \n" +
                        "    dbms_sql.close_cursor( l_cursor );\n" +
                        "end;")) {
                    stat.setString(1, query);
                    stat.registerOutParameter(2, Types.INTEGER);
                    stat.execute();
                    int errorPos = stat.getInt(2);
                    if (errorPos <= 0) {
                        return null;
                    }

                    DBPErrorAssistant.ErrorPosition pos = new DBPErrorAssistant.ErrorPosition();
                    pos.position = errorPos;
                    return new ErrorPosition[]{pos};

                } catch (SQLException e) {
                    // Something went wrong
                    log.debug("Can't extract parse error info: " + e.getMessage());
                }
            }
        }
        return null;
    }

    ///////////////////////////////////////////////
    // Statistics

    @Override
    public boolean isStatisticsCollected() {
        return hasStatistics;
    }

    @Override
    public void collectObjectStatistics(DBRProgressMonitor monitor, boolean totalSizeOnly, boolean forceRefresh) throws DBException {
        if (hasStatistics && !forceRefresh) {
            return;
        }
        try (final JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load tablespace '" + getName() + "' statistics")) {
            // Tablespace stats
            try (JDBCStatement dbStat = session.createStatement()) {
                try (JDBCResultSet dbResult = dbStat.executeQuery("SELECT TS.TABLESPACE_NAME,SUM(F.BYTES) AVAILABLE_SPACE,SUM(S.BYTES) USED_SPACE \n" +
                    "FROM SYS.DBA_TABLESPACES TS,DBA_DATA_FILES F,DBA_SEGMENTS S\n" +
                    "WHERE F.TABLESPACE_NAME(+)=TS.TABLESPACE_NAME AND S.TABLESPACE_NAME(+)=TS.TABLESPACE_NAME\n" +
                    "GROUP BY TS.TABLESPACE_NAME")) {
                    while (dbResult.next()) {
                        String tsName = dbResult.getString(1);
                        OracleTablespace tablespace = tablespaceCache.getObject(monitor, getDataSource(), tsName);
                        if (tablespace != null) {
                            tablespace.fetchSizes(dbResult);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBException("Can't read tablespace statistics", e, getDataSource());
        } finally {
            hasStatistics = true;
        }
    }

    private class OracleOutputReader implements DBCServerOutputReader {
        @Override
        public boolean isServerOutputEnabled() {
            return getContainer().getPreferenceStore().getBoolean(OracleConstants.PREF_DBMS_OUTPUT);
        }

        @Override
        public boolean isAsyncOutputReadSupported() {
            return false;
        }

        public void enableServerOutput(DBRProgressMonitor monitor, DBCExecutionContext context, boolean enable) throws DBCException {
            String sql = enable ?
                "BEGIN DBMS_OUTPUT.ENABLE(" + OracleConstants.MAXIMUM_DBMS_OUTPUT_SIZE + "); END;" :
                "BEGIN DBMS_OUTPUT.DISABLE; END;";
            try (DBCSession session = context.openSession(monitor, DBCExecutionPurpose.UTIL, (enable ? "Enable" : "Disable ") + "DBMS output")) {
                JDBCUtils.executeSQL((JDBCSession) session, sql);
            } catch (SQLException e) {
                throw new DBCException(e, context);
            }
        }

        @Override
        public void readServerOutput(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext context, @Nullable SQLQueryResult queryResult, @Nullable DBCStatement statement, @NotNull PrintWriter output) throws DBCException {
            try (JDBCSession session = (JDBCSession) context.openSession(monitor, DBCExecutionPurpose.UTIL, "Read DBMS output")) {
                try (CallableStatement getLineProc = session.getOriginal().prepareCall("{CALL DBMS_OUTPUT.GET_LINE(?, ?)}")) {
                    getLineProc.registerOutParameter(1, java.sql.Types.VARCHAR);
                    getLineProc.registerOutParameter(2, java.sql.Types.INTEGER);
                    int status = 0;
                    while (status == 0) {
                        getLineProc.execute();
                        status = getLineProc.getInt(2);
                        if (status == 0) {
                            String str = getLineProc.getString(1);
                            if (str != null) {
                                output.write(str);
                            }
                            output.write('\n');
                        }
                    }
                } catch (SQLException e) {
                    throw new DBCException(e, context);
                }
            }
        }
    }

    static class SchemaCache extends JDBCObjectCache<OracleDataSource, OracleSchema> {
        SchemaCache() {
            setListOrderComparator(DBUtils.<OracleSchema>nameComparator());
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OracleDataSource owner) throws SQLException {
            StringBuilder schemasQuery = new StringBuilder();
            // PROP_CHECK_SCHEMA_CONTENT set to true when option "Hide empty schemas" is set
            boolean showAllSchemas = ! CommonUtils.toBoolean(owner.getContainer().getConnectionConfiguration().getProviderProperty(OracleConstants.PROP_CHECK_SCHEMA_CONTENT));
            schemasQuery.append("SELECT U.* FROM ").append(OracleUtils.getAdminAllViewPrefix(session.getProgressMonitor(), owner, "USERS")).append(" U\n");

//                if (owner.isAdmin() && false) {
//                    schemasQuery.append(
//                        "WHERE (U.USER_ID IN (SELECT DISTINCT OWNER# FROM SYS.OBJ$) ");
//                } else {
            
            schemasQuery.append(
                "WHERE (");
            if (showAllSchemas) {
                schemasQuery.append("U.USERNAME IS NOT NULL");
            } else {
                schemasQuery.append("U.USERNAME IN (SELECT DISTINCT OWNER FROM ").append(OracleUtils.getAdminAllViewPrefix(session.getProgressMonitor(), owner, "OBJECTS")).append(")");
            }
//                }

            DBSObjectFilter schemaFilters = owner.getContainer().getObjectFilter(OracleSchema.class, null, false);
            if (schemaFilters != null) {
                JDBCUtils.appendFilterClause(schemasQuery, schemaFilters, "U.USERNAME", false);
            }
            schemasQuery.append(")");
            //if (!CommonUtils.isEmpty(owner.activeSchemaName)) {
            //schemasQuery.append("\nUNION ALL SELECT '").append(owner.activeSchemaName).append("' AS USERNAME FROM DUAL");
            //}
            //schemasQuery.append("\nORDER BY USERNAME");

            JDBCPreparedStatement dbStat = session.prepareStatement(schemasQuery.toString());

            if (schemaFilters != null) {
                JDBCUtils.setFilterParameters(dbStat, 1, schemaFilters);
            }
            return dbStat;
        }

        @Override
        protected OracleSchema fetchObject(@NotNull JDBCSession session, @NotNull OracleDataSource owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new OracleSchema(owner, resultSet);
        }

        @Override
        protected void invalidateObjects(DBRProgressMonitor monitor, OracleDataSource owner, Iterator<OracleSchema> objectIter) {
            setListOrderComparator(DBUtils.<OracleSchema>nameComparator());
        }
    }

    static class DataTypeCache extends JDBCObjectCache<OracleDataSource, OracleDataType> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OracleDataSource owner) throws SQLException {
            return session.prepareStatement(
                "SELECT " + OracleUtils.getSysCatalogHint(owner.getDataSource()) + " * FROM " +
                    OracleUtils.getAdminAllViewPrefix(session.getProgressMonitor(), owner, "TYPES") + " WHERE OWNER IS NULL ORDER BY TYPE_NAME");
        }

        @Override
        protected OracleDataType fetchObject(@NotNull JDBCSession session, @NotNull OracleDataSource owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new OracleDataType(owner, resultSet);
        }
    }

    static class TablespaceCache extends JDBCObjectCache<OracleDataSource, OracleTablespace> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OracleDataSource owner) throws SQLException {
            return session.prepareStatement(
                "SELECT * FROM " + OracleUtils.getSysUserViewName(session.getProgressMonitor(), owner, "TABLESPACES") + " ORDER BY TABLESPACE_NAME");
        }

        @Override
        protected OracleTablespace fetchObject(@NotNull JDBCSession session, @NotNull OracleDataSource owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new OracleTablespace(owner, resultSet);
        }
    }

    static class UserCache extends JDBCObjectCache<OracleDataSource, OracleUser> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OracleDataSource owner) throws SQLException {
            return session.prepareStatement(
                "SELECT * FROM " + OracleUtils.getAdminAllViewPrefix(session.getProgressMonitor(), owner, "USERS") + " ORDER BY USERNAME");
        }

        @Override
        protected OracleUser fetchObject(@NotNull JDBCSession session, @NotNull OracleDataSource owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new OracleUser(owner, resultSet);
        }
    }

    static class RoleCache extends JDBCObjectCache<OracleDataSource, OracleRole> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OracleDataSource owner) throws SQLException {
            return session.prepareStatement(
                "SELECT * FROM DBA_ROLES ORDER BY ROLE");
        }

        @Override
        protected OracleRole fetchObject(@NotNull JDBCSession session, @NotNull OracleDataSource owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new OracleRole(owner, resultSet);
        }
    }

    static class ProfileCache extends JDBCStructCache<OracleDataSource, OracleUserProfile, OracleUserProfile.ProfileResource> {
        protected ProfileCache() {
            super("PROFILE");
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OracleDataSource owner) throws SQLException {
            return session.prepareStatement(
                "SELECT DISTINCT PROFILE FROM DBA_PROFILES ORDER BY PROFILE");
        }

        @Override
        protected OracleUserProfile fetchObject(@NotNull JDBCSession session, @NotNull OracleDataSource owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new OracleUserProfile(owner, resultSet);
        }

        @Override
        protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull OracleDataSource dataSource, @Nullable OracleUserProfile forObject) throws SQLException {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT RESOURCE_NAME,RESOURCE_TYPE,LIMIT FROM DBA_PROFILES " +
                    (forObject == null ? "" : "WHERE PROFILE=? ") +
                    "ORDER BY RESOURCE_NAME");
            if (forObject != null) {
                dbStat.setString(1, forObject.getName());
            }
            return dbStat;
        }

        @Override
        protected OracleUserProfile.ProfileResource fetchChild(@NotNull JDBCSession session, @NotNull OracleDataSource dataSource, @NotNull OracleUserProfile parent, @NotNull JDBCResultSet dbResult) throws SQLException, DBException {
            return new OracleUserProfile.ProfileResource(parent, dbResult);
        }
    }

}
