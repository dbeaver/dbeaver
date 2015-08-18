/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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

import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.oracle.OracleDataSourceProvider;
import org.jkiss.dbeaver.ext.oracle.model.plan.OraclePlanAnalyser;
import org.jkiss.dbeaver.ext.oracle.oci.OCIUtils;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.*;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.impl.jdbc.*;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GenericDataSource
 */
public class OracleDataSource extends JDBCDataSource
    implements DBSObjectSelector, DBCServerOutputReader, DBCQueryPlanner, IAdaptable
{
    static final Log log = Log.getLog(OracleDataSource.class);

    //private final static Map<String, OCIClassLoader> ociClassLoadersCache = new HashMap<String, OCIClassLoader>();

    final public SchemaCache schemaCache = new SchemaCache();
    final DataTypeCache dataTypeCache = new DataTypeCache();
    final TablespaceCache tablespaceCache = new TablespaceCache();
    final UserCache userCache = new UserCache();
    final ProfileCache profileCache = new ProfileCache();
    final RoleCache roleCache = new RoleCache();

    private OracleSchema publicSchema;
    private String activeSchemaName;
    private boolean isAdmin;
    private boolean isAdminVisible;
    private String planTableName;

    public OracleDataSource(DBRProgressMonitor monitor, DBSDataSourceContainer container)
        throws DBException
    {
        super(monitor, container);
    }

    @Override
    protected Connection openConnection(DBRProgressMonitor monitor, String purpose) throws DBCException
    {
        // Set tns admin directory
        DBPClientHome clientHome = getContainer().getClientHome();
        if (clientHome != null) {
            System.setProperty("oracle.net.tns_admin", new File(clientHome.getHomePath(), OCIUtils.TNSNAMES_FILE_PATH).getAbsolutePath());
        }

        Connection connection = super.openConnection(monitor, purpose);
/*
        OracleConnection oracleConnection = (OracleConnection)connection.getConnection();

        try {
            oracleConnection.setClientInfo("ApplicationName", DBeaverCore.getProductTitle() + " - " + purpose);
        } catch (Throwable e) {
            // just ignore
            log.debug(e);
        }
*/

        return connection;
    }

    protected void initializeContextState(DBRProgressMonitor monitor, JDBCExecutionContext context, boolean setActiveObject) throws DBCException {
        // Enable DBMS output
        enableServerOutput(
            monitor,
            context,
            isServerOutputEnabled());
        if (setActiveObject) {
            setCurrentSchema(monitor, context, getSelectedObject());
        }
    }

    @Override
    protected String getConnectionUserName(@NotNull DBPConnectionConfiguration connectionInfo)
    {
        final Object role = connectionInfo.getProperty(OracleConstants.PROP_INTERNAL_LOGON);
        return role == null ? connectionInfo.getUserName() : connectionInfo.getUserName() + " AS " + role;
    }

    @Override
    protected DBPDataSourceInfo createDataSourceInfo(JDBCDatabaseMetaData metaData)
    {
        return new JDBCDataSourceInfo(metaData);
    }

    @Override
    protected SQLDialect createSQLDialect(JDBCDatabaseMetaData metaData) {
        JDBCSQLDialect dialect = new OracleSQLDialect(this, metaData);
        for (String kw : OracleConstants.ADVANCED_KEYWORDS) {
            dialect.addSQLKeyword(kw);
        }
        return dialect;
    }

    @Override
    protected Map<String, String> getInternalConnectionProperties()
    {
        return OracleDataSourceProvider.getConnectionsProps();
    }

    public boolean isAdmin()
    {
        return isAdmin;
    }

    public boolean isAdminVisible()
    {
        return isAdmin || isAdminVisible;
    }

    @Association
    public Collection<OracleSchema> getSchemas(DBRProgressMonitor monitor) throws DBException
    {
        return schemaCache.getAllObjects(monitor, this);
    }

    public OracleSchema getSchema(DBRProgressMonitor monitor, String name) throws DBException
    {
        if (publicSchema != null && publicSchema.getName().equals(name)) {
            return publicSchema;
        }
        return schemaCache.getObject(monitor, this, name);
    }

    @Association
    public Collection<OracleTablespace> getTablespaces(DBRProgressMonitor monitor) throws DBException
    {
        return tablespaceCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<OracleUser> getUsers(DBRProgressMonitor monitor) throws DBException
    {
        return userCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<OracleUserProfile> getProfiles(DBRProgressMonitor monitor) throws DBException
    {
        return profileCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<OracleRole> getRoles(DBRProgressMonitor monitor) throws DBException
    {
        return roleCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<OracleSynonym> getPublicSynonyms(DBRProgressMonitor monitor) throws DBException
    {
        return publicSchema.getSynonyms(monitor);
    }

    @Association
    public Collection<OracleDBLink> getPublicDatabaseLinks(DBRProgressMonitor monitor) throws DBException
    {
        return publicSchema.getDatabaseLinks(monitor);
    }

    @Association
    public Collection<OracleRecycledObject> getUserRecycledObjects(DBRProgressMonitor monitor) throws DBException
    {
        return publicSchema.getRecycledObjects(monitor);
    }

    public boolean isAtLeastV10()
    {
        return getInfo().getDatabaseVersion().getMajor() >= 10;
    }

    public boolean isAtLeastV11()
    {
        return getInfo().getDatabaseVersion().getMajor() >= 11;
    }

    @Override
    public void initialize(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        super.initialize(monitor);

        this.publicSchema = new OracleSchema(this, 1, OracleConstants.USER_PUBLIC);
        {
            final JDBCSession session = getDefaultContext(true).openSession(monitor, DBCExecutionPurpose.META, "Load data source meta info");
            try {
                // Set session settings
                DBPConnectionConfiguration connectionInfo = getContainer().getConnectionConfiguration();
                Object sessionLanguage = connectionInfo.getProperty(OracleConstants.PROP_SESSION_LANGUAGE);
                if (sessionLanguage != null) {
                    try {
                        JDBCUtils.executeSQL(
                            session,
                            "ALTER SESSION SET NLS_LANGUAGE='" + sessionLanguage + "'");
                    } catch (SQLException e) {
                        log.warn("Can't set session language", e);
                    }
                }
                Object sessionTerritory = connectionInfo.getProperty(OracleConstants.PROP_SESSION_TERRITORY);
                if (sessionTerritory != null) {
                    try {
                        JDBCUtils.executeSQL(
                            session,
                            "ALTER SESSION SET NLS_TERRITORY='" + sessionTerritory + "'");
                    } catch (SQLException e) {
                        log.warn("Can't set session territory", e);
                    }
                }

                // Check DBA role
                this.isAdmin = "YES".equals(
                    JDBCUtils.queryString(
                    session,
                    "SELECT 'YES' FROM USER_ROLE_PRIVS WHERE GRANTED_ROLE='DBA'"));
                this.isAdminVisible = isAdmin;
                if (!isAdminVisible) {
                    Object showAdmin = connectionInfo.getProperty(OracleConstants.PROP_ALWAYS_SHOW_DBA);
                    if (showAdmin != null) {
                        isAdminVisible = CommonUtils.getBoolean(showAdmin, false);
                    }
                }

                // Get active schema
                this.activeSchemaName = JDBCUtils.queryString(
                    session,
                    "SELECT SYS_CONTEXT( 'USERENV', 'CURRENT_SCHEMA' ) FROM DUAL");

            } catch (SQLException e) {
                //throw new DBException(e);
                log.warn(e);
            }
            finally {
                session.close();
            }
        }
        // Cache data types
        this.dataTypeCache.getAllObjects(monitor, this);
    }

    @Override
    public boolean refreshObject(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        super.refreshObject(monitor);

        this.schemaCache.clearCache();
        this.dataTypeCache.clearCache();
        this.tablespaceCache.clearCache();
        this.userCache.clearCache();
        this.profileCache.clearCache();
        this.roleCache.clearCache();
        this.activeSchemaName = null;

        this.initialize(monitor);

        return true;
    }

    @Override
    public Collection<OracleSchema> getChildren(DBRProgressMonitor monitor)
        throws DBException
    {
        return getSchemas(monitor);
    }

    @Override
    public OracleSchema getChild(DBRProgressMonitor monitor, String childName)
        throws DBException
    {
        return getSchema(monitor, childName);
    }

    @Override
    public Class<? extends OracleSchema> getChildType(DBRProgressMonitor monitor)
        throws DBException
    {
        return OracleSchema.class;
    }

    @Override
    public void cacheStructure(DBRProgressMonitor monitor, int scope)
        throws DBException
    {
        
    }

    @Override
    public boolean supportsObjectSelect()
    {
        return true;
    }

    @Nullable
    @Override
    public OracleSchema getSelectedObject()
    {
        return activeSchemaName == null ? null : schemaCache.getCachedObject(activeSchemaName);
    }

    @Override
    public void selectObject(DBRProgressMonitor monitor, DBSObject object)
        throws DBException
    {
        final OracleSchema oldSelectedEntity = getSelectedObject();
        if (!(object instanceof OracleSchema)) {
            throw new IllegalArgumentException("Invalid object type: " + object);
        }
        for (JDBCExecutionContext context : getAllContexts()) {
            setCurrentSchema(monitor, context, (OracleSchema) object);
        }
        activeSchemaName = object.getName();

        // Send notifications
        if (oldSelectedEntity != null) {
            DBUtils.fireObjectSelect(oldSelectedEntity, false);
        }
        if (this.activeSchemaName != null) {
            DBUtils.fireObjectSelect(object, true);
        }
    }

    private void setCurrentSchema(DBRProgressMonitor monitor, JDBCExecutionContext executionContext, OracleSchema object) throws DBCException {
        if (object == null) {
            log.debug("Null current schema");
            return;
        }
        JDBCSession session = executionContext.openSession(monitor, DBCExecutionPurpose.UTIL, "Set active schema");
        try {
            JDBCUtils.executeSQL(session, "ALTER SESSION SET CURRENT_SCHEMA=" + object.getName());
        } catch (SQLException e) {
            throw new DBCException(e, session.getDataSource());
        }
        finally {
            session.close();
        }
    }

    @Override
    public DBCPlan planQueryExecution(DBCSession session, String query) throws DBCException
    {
        OraclePlanAnalyser plan = new OraclePlanAnalyser(this, query);
        plan.explain((JDBCSession) session);
        return plan;
    }

    @Nullable
    @Override
    public Object getAdapter(Class adapter)
    {
        if (adapter == DBSStructureAssistant.class) {
            return new OracleStructureAssistant(this);
        }
        return super.getAdapter(adapter);
    }

    @NotNull
    @Override
    public OracleDataSource getDataSource() {
        return this;
    }

    @Override
    public DBPDataKind resolveDataKind(@NotNull String typeName, int valueType)
    {
        if (typeName != null &&
            (typeName.equals(OracleConstants.TYPE_NAME_XML) || typeName.equals(OracleConstants.TYPE_FQ_XML)))
        {
            return DBPDataKind.CONTENT;
        }
        if (typeName != null) {
            DBPDataKind dataKind = OracleDataType.getDataKind(typeName);
            if (dataKind != null) {
                return dataKind;
            }
        }
        return super.resolveDataKind(typeName, valueType);
    }

    @Override
    public Collection<? extends DBSDataType> getDataTypes()
    {
        return dataTypeCache.getCachedObjects();
    }

    @Override
    public DBSDataType getDataType(String typeName)
    {
        return dataTypeCache.getCachedObject(typeName);
    }

    @Nullable
    @Override
    public DBSDataType resolveDataType(@NotNull DBRProgressMonitor monitor, @NotNull String typeFullName) throws DBException
    {
        int divPos = typeFullName.indexOf(SQLConstants.STRUCT_SEPARATOR);
        if (divPos == -1) {
            // Simple type name
            return getDataType(typeFullName);
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
        throws SQLException
    {
        String tableName = getContainer().getPreferenceStore().getString(OracleConstants.PREF_EXPLAIN_TABLE_NAME);
        if (!CommonUtils.isEmpty(tableName)) {
            return tableName;
        }
        if (planTableName == null) {
            String[] candidateNames = new String[] {"PLAN_TABLE", "TOAD_PLAN_TABLE"};
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
                // Plan table not found - try to create new one
                if (!UIUtils.confirmAction(
                    DBeaverUI.getActiveWorkbenchShell(),
                    "Oracle PLAN_TABLE missing",
                    "PLAN_TABLE not found in current user's session. " +
                        "Do you want DBeaver to create new PLAN_TABLE?"))
                {
                    return null;
                }
                planTableName = createPlanTable(session);
            }
        }
        return planTableName;
    }

    private String createPlanTable(JDBCSession session) throws SQLException
    {
        JDBCUtils.executeSQL(session, OracleConstants.PLAN_TABLE_DEFINITION);
        return "PLAN_TABLE";
    }

    @Override
    public boolean isServerOutputEnabled() {
        return getContainer().getPreferenceStore().getBoolean(OracleConstants.PREF_DBMS_OUTPUT);
    }

    @Override
    public void enableServerOutput(DBRProgressMonitor monitor, DBCExecutionContext context, boolean enable) throws DBCException {
        String sql = enable ?
            "BEGIN DBMS_OUTPUT.ENABLE(" + OracleConstants.MAXIMUM_DBMS_OUTPUT_SIZE + "); END;" :
            "BEGIN DBMS_OUTPUT.DISABLE; END;";
        DBCSession session = context.openSession(monitor, DBCExecutionPurpose.UTIL, (enable ? "Enable" : "Disable ") + "DBMS output");
        try {
            JDBCUtils.executeSQL((JDBCSession) session, sql);
        } catch (SQLException e) {
            throw new DBCException(e, this);
        }
        finally {
            session.close();
        }
    }

    @Nullable
    @Override
    public DBCQueryTransformer createQueryTransformer(DBCQueryTransformType type) {
        if (type == DBCQueryTransformType.RESULT_SET_LIMIT) {
            //return new QueryTransformerRowNum();
        }
        return super.createQueryTransformer(type);
    }

    @Override
    public void readServerOutput(DBRProgressMonitor monitor, DBCExecutionContext context, PrintWriter output) throws DBCException {
        JDBCSession session = (JDBCSession) context.openSession(monitor, DBCExecutionPurpose.UTIL, "Read DBMS output");
        try {
            final JDBCCallableStatement dbCall = session.prepareCall(
                "DECLARE " +
                    "l_line varchar2(255); " +
                    "l_done number; " +
                    "l_buffer long; " +
                "BEGIN " +
                    "LOOP " +
                        "EXIT WHEN LENGTH(l_buffer)+255 > :maxbytes OR l_done = 1; " +
                        "DBMS_OUTPUT.GET_LINE( l_line, l_done ); " +
                        "l_buffer := l_buffer || l_line || chr(10); " +
                    "END LOOP; " +
                    ":done := l_done; " +
                    ":buffer := l_buffer; " +
                "END;");
            try {
                dbCall.registerOutParameter( 2, java.sql.Types.INTEGER );
                dbCall.registerOutParameter( 3, java.sql.Types.VARCHAR );

                for(;;) {
                    dbCall.setInt( 1, 32000 );
                    dbCall.executeUpdate();
                    String outputString = dbCall.getString(3);
                    if (!CommonUtils.isEmptyTrimmed(outputString)) {
                        output.write(outputString);
                    }
                    int status = dbCall.getInt(2);
                    if ( status == 1 ) {
                        break;
                    }
                }
            }
            finally {
                dbCall.close();
            }
        } catch (SQLException e) {
            throw new DBCException(e, this);
        }
        finally {
            session.close();
        }
    }

    private Pattern ERROR_POSITION_PATTERN = Pattern.compile("(.+): line ([0-9]+), column ([0-9]+):");

    @Nullable
    @Override
    public ErrorPosition[] getErrorPosition(@NotNull Throwable error) {
        while (error instanceof DBException) {
            if (error.getCause() == null) {
                return null;
            }
            error = error.getCause();
        }
        String message = error.getMessage();
        if (!CommonUtils.isEmpty(message)) {
            Matcher matcher = ERROR_POSITION_PATTERN.matcher(message);
            List<ErrorPosition> positions = new ArrayList<ErrorPosition>();
            while (matcher.find()) {
                DBPErrorAssistant.ErrorPosition pos = new DBPErrorAssistant.ErrorPosition();
                pos.info = matcher.group(1);
                pos.line = Integer.parseInt(matcher.group(2)) - 1;
                pos.position = Integer.parseInt(matcher.group(3)) - 1;
                positions.add(pos);
            }
            if (!positions.isEmpty()) {
                return positions.toArray(new ErrorPosition[positions.size()]);
            }
        }
        return null;
    }

    static class SchemaCache extends JDBCObjectCache<OracleDataSource, OracleSchema> {
        SchemaCache()
        {
            setListOrderComparator(DBUtils.<OracleSchema>nameComparator());
        }

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OracleDataSource owner) throws SQLException
        {
            StringBuilder schemasQuery = new StringBuilder();
            boolean manyObjects = "false".equals(owner.getContainer().getConnectionConfiguration().getProperty(OracleConstants.PROP_CHECK_SCHEMA_CONTENT));
            schemasQuery.append("SELECT U.USERNAME FROM SYS.ALL_USERS U\n");

//                if (owner.isAdmin() && false) {
//                    schemasQuery.append(
//                        "WHERE (U.USER_ID IN (SELECT DISTINCT OWNER# FROM SYS.OBJ$) ");
//                } else {
            schemasQuery.append(
                "WHERE (");
            if (manyObjects) {
                schemasQuery.append("U.USERNAME IS NOT NULL");
            } else {
                schemasQuery.append("U.USERNAME IN (SELECT DISTINCT OWNER FROM SYS.ALL_OBJECTS)");
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
        protected OracleSchema fetchObject(@NotNull JDBCSession session, @NotNull OracleDataSource owner, @NotNull ResultSet resultSet) throws SQLException, DBException
        {
            return new OracleSchema(owner, resultSet);
        }

        @Override
        protected void invalidateObjects(DBRProgressMonitor monitor, OracleDataSource owner, Iterator<OracleSchema> objectIter)
        {
            setListOrderComparator(DBUtils.<OracleSchema>nameComparator());
            // Add predefined types
            if (owner.activeSchemaName != null && getCachedObject(owner.activeSchemaName) == null) {
                cacheObject(
                    new OracleSchema(owner, -1, owner.activeSchemaName));
            }
        }
    }

    static class DataTypeCache extends JDBCObjectCache<OracleDataSource, OracleDataType> {
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OracleDataSource owner) throws SQLException
        {
            return session.prepareStatement(
                "SELECT * FROM SYS.ALL_TYPES WHERE OWNER IS NULL ORDER BY TYPE_NAME");
        }
        @Override
        protected OracleDataType fetchObject(@NotNull JDBCSession session, @NotNull OracleDataSource owner, @NotNull ResultSet resultSet) throws SQLException, DBException
        {
            return new OracleDataType(owner, resultSet);
        }

        @Override
        protected void invalidateObjects(DBRProgressMonitor monitor, OracleDataSource owner, Iterator<OracleDataType> objectIter)
        {
            // Add predefined types
            for (Map.Entry<String, OracleDataType.TypeDesc> predefinedType : OracleDataType.PREDEFINED_TYPES.entrySet()) {
                if (getCachedObject(predefinedType.getKey()) == null) {
                    cacheObject(
                        new OracleDataType(owner, predefinedType.getKey(), true));
                }
            }
        }
    }

    static class TablespaceCache extends JDBCObjectCache<OracleDataSource, OracleTablespace> {
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OracleDataSource owner) throws SQLException
        {
            return session.prepareStatement(
                "SELECT * FROM " + OracleUtils.getAdminViewPrefix(owner) + "TABLESPACES ORDER BY TABLESPACE_NAME");
        }

        @Override
        protected OracleTablespace fetchObject(@NotNull JDBCSession session, @NotNull OracleDataSource owner, @NotNull ResultSet resultSet) throws SQLException, DBException
        {
            return new OracleTablespace(owner, resultSet);
        }
    }

    static class UserCache extends JDBCObjectCache<OracleDataSource, OracleUser> {
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OracleDataSource owner) throws SQLException
        {
            return session.prepareStatement(
                "SELECT * FROM " + OracleUtils.getAdminAllViewPrefix(owner) + "USERS ORDER BY USERNAME");
        }

        @Override
        protected OracleUser fetchObject(@NotNull JDBCSession session, @NotNull OracleDataSource owner, @NotNull ResultSet resultSet) throws SQLException, DBException
        {
            return new OracleUser(owner, resultSet);
        }
    }

    static class RoleCache extends JDBCObjectCache<OracleDataSource, OracleRole> {
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OracleDataSource owner) throws SQLException
        {
            return session.prepareStatement(
                "SELECT * FROM DBA_ROLES ORDER BY ROLE");
        }

        @Override
        protected OracleRole fetchObject(@NotNull JDBCSession session, @NotNull OracleDataSource owner, @NotNull ResultSet resultSet) throws SQLException, DBException
        {
            return new OracleRole(owner, resultSet);
        }
    }

    static class ProfileCache extends JDBCStructCache<OracleDataSource, OracleUserProfile, OracleUserProfile.ProfileResource> {
        protected ProfileCache()
        {
            super("PROFILE");
        }

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OracleDataSource owner) throws SQLException
        {
            return session.prepareStatement(
                "SELECT DISTINCT PROFILE FROM DBA_PROFILES ORDER BY PROFILE");
        }

        @Override
        protected OracleUserProfile fetchObject(@NotNull JDBCSession session, @NotNull OracleDataSource owner, @NotNull ResultSet resultSet) throws SQLException, DBException
        {
            return new OracleUserProfile(owner, resultSet);
        }

        @Override
        protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull OracleDataSource dataSource, @Nullable OracleUserProfile forObject) throws SQLException
        {
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
        protected OracleUserProfile.ProfileResource fetchChild(@NotNull JDBCSession session, @NotNull OracleDataSource dataSource, @NotNull OracleUserProfile parent, @NotNull ResultSet dbResult) throws SQLException, DBException
        {
            return new OracleUserProfile.ProfileResource(parent, dbResult);
        }
    }

}
