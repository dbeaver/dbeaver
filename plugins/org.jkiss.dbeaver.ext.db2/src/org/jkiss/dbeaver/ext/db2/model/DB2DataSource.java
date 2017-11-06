/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2016 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.db2.model;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.db2.*;
import org.jkiss.dbeaver.ext.db2.editors.DB2StructureAssistant;
import org.jkiss.dbeaver.ext.db2.editors.DB2TablespaceChooser;
import org.jkiss.dbeaver.ext.db2.info.DB2Parameter;
import org.jkiss.dbeaver.ext.db2.info.DB2XMLString;
import org.jkiss.dbeaver.ext.db2.model.fed.DB2RemoteServer;
import org.jkiss.dbeaver.ext.db2.model.fed.DB2UserMapping;
import org.jkiss.dbeaver.ext.db2.model.fed.DB2Wrapper;
import org.jkiss.dbeaver.ext.db2.model.plan.DB2PlanAnalyser;
import org.jkiss.dbeaver.ext.db2.model.security.DB2AuthIDType;
import org.jkiss.dbeaver.ext.db2.model.security.DB2Grantee;
import org.jkiss.dbeaver.ext.db2.model.security.DB2GranteeCache;
import org.jkiss.dbeaver.ext.db2.model.security.DB2Role;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanStyle;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectSimpleCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectSelector;
import org.jkiss.dbeaver.model.struct.DBSStructureAssistant;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 * DB2 DataSource
 * 
 * @author Denis Forveille
 */
public class DB2DataSource extends JDBCDataSource implements DBSObjectSelector, DBCQueryPlanner, IAdaptable {

    private static final Log                                     LOG                = Log.getLog(DB2DataSource.class);

    private static final String                                  GET_CURRENT_USER   = "VALUES(SYSTEM_USER)";
    private static final String                                  GET_CURRENT_SCHEMA = "VALUES(CURRENT SCHEMA)";
    private static final String                                  SET_CURRENT_SCHEMA = "SET CURRENT SCHEMA = %s";
    private static final String                                  GET_SESSION_USER   = "VALUES(SESSION_USER)";

    private static final String                                  C_SCHEMA           = "SELECT * FROM SYSCAT.SCHEMATA ORDER BY SCHEMANAME WITH UR";
    private static final String                                  C_DT               = "SELECT * FROM SYSCAT.DATATYPES WHERE METATYPE = 'S' ORDER BY TYPESCHEMA,TYPENAME WITH UR";
    private static final String                                  C_BP               = "SELECT * FROM SYSCAT.BUFFERPOOLS ORDER BY BPNAME WITH UR";
    private static final String                                  C_TS               = "SELECT * FROM SYSCAT.TABLESPACES ORDER BY TBSPACE WITH UR";
    private static final String                                  C_SG               = "SELECT * FROM SYSCAT.STOGROUPS ORDER BY SGNAME WITH UR";
    private static final String                                  C_RL               = "SELECT * FROM SYSCAT.ROLES ORDER BY ROLENAME WITH UR";
    private static final String                                  C_VR               = "SELECT * FROM SYSCAT.VARIABLES WHERE VARMODULENAME IS NULL ORDER BY VARNAME WITH UR";

    private static final String                                  C_SV               = "SELECT * FROM SYSCAT.SERVERS ORDER BY SERVERNAME WITH UR";
    private static final String                                  C_WR               = "SELECT * FROM SYSCAT.WRAPPERS ORDER BY WRAPNAME WITH UR";
    private static final String                                  C_UM               = "SELECT * FROM SYSCAT.USEROPTIONS WHERE OPTION = 'REMOTE_AUTHID' ORDER BY SERVERNAME,AUTHID WITH UR";

    private final DBSObjectCache<DB2DataSource, DB2Schema>       schemaCache        = new JDBCObjectSimpleCache<>(DB2Schema.class,
        C_SCHEMA);
    private final DBSObjectCache<DB2DataSource, DB2DataType>     dataTypeCache      = new JDBCObjectSimpleCache<>(DB2DataType.class,
        C_DT);
    private final DBSObjectCache<DB2DataSource, DB2Bufferpool>   bufferpoolCache    = new JDBCObjectSimpleCache<>(
        DB2Bufferpool.class, C_BP);
    private final DBSObjectCache<DB2DataSource, DB2Tablespace>   tablespaceCache    = new JDBCObjectSimpleCache<>(
        DB2Tablespace.class, C_TS);

    private final DBSObjectCache<DB2DataSource, DB2RemoteServer> remoteServerCache  = new JDBCObjectSimpleCache<>(
        DB2RemoteServer.class, C_SV);
    private final DBSObjectCache<DB2DataSource, DB2Wrapper>      wrapperCache       = new JDBCObjectSimpleCache<>(DB2Wrapper.class,
        C_WR);
    private final DBSObjectCache<DB2DataSource, DB2UserMapping>  userMappingCache   = new JDBCObjectSimpleCache<>(
        DB2UserMapping.class, C_UM);

    private final DB2GranteeCache                                groupCache         = new DB2GranteeCache(DB2AuthIDType.G);
    private final DB2GranteeCache                                userCache          = new DB2GranteeCache(DB2AuthIDType.U);

    // Those are dependent of DB2 version
    // This is ok as they will never been called as the folder/menu is hidden in plugin.xml
    private final DBSObjectCache<DB2DataSource, DB2StorageGroup> storagegroupCache  = new JDBCObjectSimpleCache<>(
        DB2StorageGroup.class, C_SG);
    private final DBSObjectCache<DB2DataSource, DB2Role>         roleCache          = new JDBCObjectSimpleCache<>(DB2Role.class,
        C_RL);
    private final DBSObjectCache<DB2DataSource, DB2Variable>     variableCache      = new JDBCObjectSimpleCache<>(DB2Variable.class,
        C_VR);

    private List<DB2Parameter>                                   listDBParameters;
    private List<DB2Parameter>                                   listDBMParameters;
    private List<DB2XMLString>                                   listXMLStrings;

    private String                                               activeSchemaName;
    private DB2CurrentUserPrivileges                             db2CurrentUserPrivileges;

    private String                                               schemaForExplainTables;

    private Double                                               version;                                                                                                                  // Database
                                                                                                                                                                                           // Version

    // -----------------------
    // Constructors
    // -----------------------

    public DB2DataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException
    {
        super(monitor, container, new DB2SQLDialect());
    }

    @Override
    protected boolean isConnectionReadOnlyBroken()
    {
        return true;
    }

    // -----------------------
    // Initialisation/Structure
    // -----------------------

    @Override
    public void initialize(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        super.initialize(monitor);

        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load data source meta info")) {

            // First try to get active schema from special register 'CURRENT SCHEMA'
            this.activeSchemaName = determineActiveSchema(session);
            this.db2CurrentUserPrivileges = new DB2CurrentUserPrivileges(monitor, session, activeSchemaName, this);

        } catch (SQLException e) {
            LOG.warn("Error reading active schema", e);
        }

        try {
            this.dataTypeCache.getAllObjects(monitor, this);
        } catch (DBException e) {
            LOG.warn("Error reading types info", e);
            this.dataTypeCache.setCache(Collections.<DB2DataType> emptyList());
        }
    }

    protected void initializeContextState(@NotNull DBRProgressMonitor monitor, @NotNull JDBCExecutionContext context,
        boolean setActiveObject) throws DBCException
    {
        if (setActiveObject) {
            setCurrentSchema(monitor, context, getDefaultObject());
        }
    }

    private String determineActiveSchema(JDBCSession session) throws SQLException
    {
        // First try to get active schema from special register 'CURRENT SCHEMA'
        String defSchema = JDBCUtils.queryString(session, GET_CURRENT_SCHEMA);
        if (defSchema == null) {
            LOG.warn(GET_CURRENT_SCHEMA
                + " returned null! How can it be? Trying to set active schema to special register 'SYSTEM_USER'");

            // Then try to get active schema from special register 'SYSTEM_USER'
            defSchema = JDBCUtils.queryString(session, GET_CURRENT_USER);
            if (defSchema == null) {
                LOG.warn(
                    "Special registers 'CURRENT SCHEMA' and 'SYSTEM_USER' both returned null. Use connection username as active schema");
                defSchema = getContainer().getActualConnectionConfiguration().getUserName();
            }
        }

        return defSchema.trim();
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public <T> T getAdapter(Class<T> adapter)
    {
        if (adapter == DBSStructureAssistant.class) {
            return adapter.cast(new DB2StructureAssistant(this));
        }
        return super.getAdapter(adapter);
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException
    {
        // TODO DF: No idea what to do with this method, what it is used for...
    }

    // -----------------------
    // Connection related Info
    // -----------------------

    @Override
    protected String getConnectionUserName(@NotNull DBPConnectionConfiguration connectionInfo)
    {
        return connectionInfo.getUserName();
    }

    @NotNull
    @Override
    public DB2DataSource getDataSource()
    {
        return this;
    }

    @Override
    protected DBPDataSourceInfo createDataSourceInfo(@NotNull JDBCDatabaseMetaData metaData)
    {
        final DB2DataSourceInfo info = new DB2DataSourceInfo(metaData);

        // Compute Database version
        version = DB2Constants.DB2v9_1; // Be defensive, assume lowest possible version
        try {
            version = Integer.valueOf(metaData.getDatabaseMajorVersion()).doubleValue();
            version += Integer.valueOf(metaData.getDatabaseMinorVersion()).doubleValue() / 10;
        } catch (SQLException e) {
            LOG.warn("SQLException when reading database version. Set it to lowest supported version : " + DB2Constants.DB2v9_1
                + " : " + e.getMessage());
        }
        LOG.debug(getName() + " is version v" + version);

        // disable result set scroll
        // (it doesn't work for some queries and some column types so I have to disable it for ALL queries).

        // DF: DB2 v10 supports "Scrollable Resultsets" with the following restrictions (from the DB2 v10.5 infocenter)
        // Restriction: If the ResultSet is scrollable, and the ResultSet is used to select columns from a table on a DB2 for
        // Linux, UNIX, and Windows server,
        // the SELECT list of the SELECT statement that defines the ResultSet cannot include columns with the following data types:
        // - LONG VARCHAR
        // - LONG VARGRAPHIC
        // - BLOB
        // - CLOB
        // - XML
        // - A distinct type that is based on any of the previous data types in this list
        // - A structured type
        // So it is not usable for "generic" select statements that may include such columns (ge the "data" tab on tabl view or
        // queries run from the SQL editor)

        info.setSupportsResultSetScroll(false);

        return info;
    }

    @Override
    protected Map<String, String> getInternalConnectionProperties(DBRProgressMonitor monitor, String purpose) throws DBCException
    {
        Map<String, String> props = new HashMap<>();
        props.putAll(DB2DataSourceProvider.getConnectionsProps());
        if (getContainer().isConnectionReadOnly()) {
            props.put(DB2Constants.PROP_READ_ONLY, "true");
        }
        return props;
    }

    @Override
    protected Connection openConnection(@NotNull DBRProgressMonitor monitor, @NotNull String purpose) throws DBCException {
        Connection db2Connection = super.openConnection(monitor, purpose);

        if (!getContainer().getPreferenceStore().getBoolean(ModelPreferences.META_CLIENT_NAME_DISABLE)) {
            // Provide client info
            try {
                db2Connection.setClientInfo("ApplicationName",
                    CommonUtils.truncateString(DBUtils.getClientApplicationName(getContainer(), purpose), 255));
            } catch (Throwable e) {
                // just ignore
            }
        }

        return db2Connection;
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        super.refreshObject(monitor);

        this.userCache.clearCache();
        this.groupCache.clearCache();
        this.roleCache.clearCache();
        this.variableCache.clearCache();

        this.tablespaceCache.clearCache();
        this.storagegroupCache.clearCache();
        this.bufferpoolCache.clearCache();
        this.schemaCache.clearCache();
        this.dataTypeCache.clearCache();

        this.remoteServerCache.clearCache();
        this.wrapperCache.clearCache();
        this.userMappingCache.clearCache();

        this.listDBMParameters = null;
        this.listDBParameters = null;

        this.initialize(monitor);

        return this;
    }

    @Override
    public Collection<DB2DataType> getLocalDataTypes()
    {
        try {
            return getDataTypes(new VoidProgressMonitor());
        } catch (DBException e) {
            LOG.error("DBException occurred when reading system dataTypes: ", e);
            return null;
        }
    }

    @Override
    public DB2DataType getLocalDataType(String typeName)
    {
        try {
            return getDataType(new VoidProgressMonitor(), typeName);
        } catch (DBException e) {
            LOG.error("DBException occurred when reading system dataTYpe : " + typeName, e);
            return null;
        }
    }

    // --------------------------
    // Manage Children: DB2Schema
    // --------------------------

    @Override
    public boolean supportsDefaultChange()
    {
        return true;
    }

    @Override
    public Class<? extends DB2Schema> getChildType(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        return DB2Schema.class;
    }

    @Override
    public Collection<DB2Schema> getChildren(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        return getSchemas(monitor);
    }

    @Override
    public DB2Schema getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException
    {
        return getSchema(monitor, childName);
    }

    @Override
    public DB2Schema getDefaultObject()
    {
        return activeSchemaName == null ? null : schemaCache.getCachedObject(activeSchemaName);
    }

    @Override
    public void setDefaultObject(@NotNull DBRProgressMonitor monitor, @NotNull DBSObject object) throws DBException
    {
        final DB2Schema oldSelectedEntity = getDefaultObject();

        if (!(object instanceof DB2Schema)) {
            throw new IllegalArgumentException("Invalid object type: " + object);
        }

        for (JDBCExecutionContext context : getAllContexts()) {
            setCurrentSchema(monitor, context, (DB2Schema) object);
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

    @Override
    public boolean refreshDefaultObject(@NotNull DBCSession session) throws DBException
    {
        try {
            final String newSchemaName = determineActiveSchema((JDBCSession) session);
            if (!CommonUtils.equalObjects(newSchemaName, activeSchemaName)) {
                final DB2Schema newSchema = schemaCache.getCachedObject(newSchemaName);
                if (newSchema != null) {
                    setDefaultObject(session.getProgressMonitor(), newSchema);
                    return true;
                }
            }
            return false;
        } catch (SQLException e) {
            throw new DBException(e, this);
        }
    }

    private void setCurrentSchema(DBRProgressMonitor monitor, JDBCExecutionContext executionContext, DB2Schema object)
        throws DBCException
    {
        if (object == null) {
            LOG.debug("Null current schema");
            return;
        }
        try (JDBCSession session = executionContext.openSession(monitor, DBCExecutionPurpose.UTIL, "Set active schema")) {
            JDBCUtils.executeSQL(session, String.format(SET_CURRENT_SCHEMA, object.getName()));
        } catch (SQLException e) {
            throw new DBCException(e, this);
        }
    }

    // --------------
    // Plan Tables
    // --------------

    @NotNull
    @Override
    public DBCPlan planQueryExecution(@NotNull DBCSession session, @NotNull String query) throws DBCException
    {
        String ptSchemaname = getExplainTablesSchemaName(session);
        if (ptSchemaname == null) {
            throw new DBCException(DB2Messages.dialog_explain_no_tables_found_ex);
        }
        DB2PlanAnalyser plan = new DB2PlanAnalyser(query, ptSchemaname);
        plan.explain((JDBCSession) session);
        return plan;
    }

    @NotNull
    @Override
    public DBCPlanStyle getPlanStyle() {
        return DBCPlanStyle.PLAN;
    }

    private String getExplainTablesSchemaName(DBCSession session) throws DBCException
    {
        // // Schema for explain tables has already been verified. Use it as-is
        // if (CommonUtils.isNotEmpty(schemaForExplainTables)) {
        // return schemaForExplainTables;
        // }

        DBRProgressMonitor monitor = session.getProgressMonitor();

        // Verify explain table from current authorization id
        String sessionUserSchema;
        try {
            sessionUserSchema = JDBCUtils.queryString((JDBCSession) session, GET_SESSION_USER).trim();
        } catch (SQLException e) {
            throw new DBCException(e, session.getDataSource());
        }
        Boolean ok = DB2Utils.checkExplainTables(monitor, this, sessionUserSchema);
        if (ok) {
            LOG.debug("Valid explain tables found in " + sessionUserSchema);
            schemaForExplainTables = sessionUserSchema;
            return schemaForExplainTables;
        }

        // Verify explain table from SYSTOOLS
        ok = DB2Utils.checkExplainTables(monitor, this, DB2Constants.EXPLAIN_SCHEMA_NAME_DEFAULT);
        if (ok) {
            LOG.debug("Valid explain tables found in " + DB2Constants.EXPLAIN_SCHEMA_NAME_DEFAULT);
            schemaForExplainTables = DB2Constants.EXPLAIN_SCHEMA_NAME_DEFAULT;
            return schemaForExplainTables;
        }

        // No valid explain tables found, propose to create them in current authId
        String msg = String.format(DB2Messages.dialog_explain_ask_to_create, sessionUserSchema);
        if (!UIUtils.confirmAction(DB2Messages.dialog_explain_no_tables, msg)) {
            return null;
        }

        // Ask the user in what tablespace to create the Explain tables
        try {
            final List<String> listTablespaces = DB2Utils.getListOfUsableTsForExplain(monitor, (JDBCSession) session);

            // NO Usable Tablespace found: End of the game..
            if (listTablespaces.isEmpty()) {
                DBUserInterface.getInstance().showError(DB2Messages.dialog_explain_no_tablespace_found_title,
                    DB2Messages.dialog_explain_no_tablespace_found_title);
                return null;
            }

            // Build a dialog with the list of usable tablespaces for the user to choose
            String tablespaceName = new UITask<String>() {
                @Override
                protected String runTask() {
                    final DB2TablespaceChooser tsChooserDialog = new DB2TablespaceChooser(
                        DBeaverUI.getActiveWorkbenchShell(),
                        listTablespaces);
                    if (tsChooserDialog.open() == IDialogConstants.OK_ID) {
                        return tsChooserDialog.getSelectedTablespace();
                    } else {
                        return null;
                    }
                }
            }.execute();

            if (tablespaceName == null) {
                return null;
            }

            // Try to create explain tables within current authorizartionID in given tablespace
            DB2Utils.createExplainTables(session.getProgressMonitor(), this, sessionUserSchema, tablespaceName);

            // Hourra!
            schemaForExplainTables = sessionUserSchema;
        } catch (SQLException e) {
            throw new DBCException(e, session.getDataSource());
        }

        return sessionUserSchema;
    }

    // --------------
    // Associations
    // --------------

    @Association
    public Collection<DB2Schema> getSchemas(DBRProgressMonitor monitor) throws DBException
    {
        return schemaCache.getAllObjects(monitor, this);
    }

    public DB2Schema getSchema(DBRProgressMonitor monitor, String name) throws DBException
    {
        return schemaCache.getObject(monitor, this, name);
    }

    @Association
    public Collection<DB2DataType> getDataTypes(DBRProgressMonitor monitor) throws DBException
    {
        return dataTypeCache.getAllObjects(monitor, this);
    }

    public DB2DataType getDataType(DBRProgressMonitor monitor, String name) throws DBException
    {
        return dataTypeCache.getObject(monitor, this, name);
    }

    @Association
    public Collection<DB2Tablespace> getTablespaces(DBRProgressMonitor monitor) throws DBException
    {
        return tablespaceCache.getAllObjects(monitor, this);
    }

    public DB2Tablespace getTablespace(DBRProgressMonitor monitor, String name) throws DBException
    {
        return tablespaceCache.getObject(monitor, this, name);
    }

    @Association
    public Collection<DB2StorageGroup> getStorageGroups(DBRProgressMonitor monitor) throws DBException
    {
        return storagegroupCache.getAllObjects(monitor, this);
    }

    public DB2StorageGroup getStorageGroup(DBRProgressMonitor monitor, String name) throws DBException
    {
        return storagegroupCache.getObject(monitor, this, name);
    }

    @Association
    public Collection<DB2Bufferpool> getBufferpools(DBRProgressMonitor monitor) throws DBException
    {
        return bufferpoolCache.getAllObjects(monitor, this);
    }

    public DB2Bufferpool getBufferpool(DBRProgressMonitor monitor, String name) throws DBException
    {
        return bufferpoolCache.getObject(monitor, this, name);
    }

    @Association
    public Collection<DB2Wrapper> getWrappers(DBRProgressMonitor monitor) throws DBException
    {
        return wrapperCache.getAllObjects(monitor, this);
    }

    public DB2Wrapper getWrapper(DBRProgressMonitor monitor, String name) throws DBException
    {
        return wrapperCache.getObject(monitor, this, name);
    }

    @Association
    public Collection<DB2UserMapping> getUserMappings(DBRProgressMonitor monitor) throws DBException
    {
        return userMappingCache.getAllObjects(monitor, this);
    }

    public DB2UserMapping getUserMapping(DBRProgressMonitor monitor, String name) throws DBException
    {
        return userMappingCache.getObject(monitor, this, name);
    }

    @Association
    public Collection<DB2RemoteServer> getRemoteServers(DBRProgressMonitor monitor) throws DBException
    {
        return remoteServerCache.getAllObjects(monitor, this);
    }

    public DB2RemoteServer getRemoteServer(DBRProgressMonitor monitor, String name) throws DBException
    {
        return remoteServerCache.getObject(monitor, this, name);
    }

    @Association
    public Collection<DB2Grantee> getUsers(DBRProgressMonitor monitor) throws DBException
    {
        return userCache.getAllObjects(monitor, this);
    }

    public DB2Grantee getUser(DBRProgressMonitor monitor, String name) throws DBException
    {
        return userCache.getObject(monitor, this, name);
    }

    @Association
    public Collection<DB2Grantee> getGroups(DBRProgressMonitor monitor) throws DBException
    {
        return groupCache.getAllObjects(monitor, this);
    }

    public DB2Grantee getGroup(DBRProgressMonitor monitor, String name) throws DBException
    {
        return groupCache.getObject(monitor, this, name);
    }

    @Association
    public Collection<DB2Role> getRoles(DBRProgressMonitor monitor) throws DBException
    {
        return roleCache.getAllObjects(monitor, this);
    }

    public DB2Role getRole(DBRProgressMonitor monitor, String name) throws DBException
    {
        return roleCache.getObject(monitor, this, name);
    }

    @Association
    public Collection<DB2Variable> getVariables(DBRProgressMonitor monitor) throws DBException
    {
        return variableCache.getAllObjects(monitor, this);
    }

    public DB2Variable getVariable(DBRProgressMonitor monitor, String name) throws DBException
    {
        return variableCache.getObject(monitor, this, name);
    }

    // -------------
    // Dynamic Data
    // -------------

    public List<DB2Parameter> getDbParameters(DBRProgressMonitor monitor) throws DBException
    {
        if (listDBParameters == null) {
            try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load Database Parameters")) {
                listDBParameters = DB2Utils.readDBCfg(monitor, session);
            } catch (SQLException e) {
                LOG.warn(e);
            }
        }
        return listDBParameters;
    }

    public List<DB2Parameter> getDbmParameters(DBRProgressMonitor monitor) throws DBException
    {
        if (listDBMParameters == null) {
            try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load Instance Parameters")) {
                listDBMParameters = DB2Utils.readDBMCfg(monitor, session);
            } catch (SQLException e) {
                LOG.warn(e);
            }
        }
        return listDBMParameters;
    }

    public List<DB2XMLString> getXmlStrings(DBRProgressMonitor monitor) throws DBException
    {
        if (listXMLStrings == null) {
            try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load Global XMLStrings")) {
                listXMLStrings = DB2Utils.readXMLStrings(monitor, session);
            } catch (SQLException e) {
                LOG.warn(e);
            }
        }
        return listXMLStrings;
    }

    // --------------------------
    // Authorities
    // --------------------------

    public boolean isAuthorisedForApplications()
    {
        return db2CurrentUserPrivileges != null && db2CurrentUserPrivileges.userIsAuthorisedForApplications();
    }

    public boolean isAuthorisedForContainers()
    {
        return db2CurrentUserPrivileges != null && db2CurrentUserPrivileges.userIsAuthorisedForContainers();
    }

    public boolean isAuthorisedForDBCFG()
    {
        return db2CurrentUserPrivileges != null && db2CurrentUserPrivileges.userIsAuthorisedForDBCFG();
    }

    public boolean isAuthorisedForAdminister()
    {
        return db2CurrentUserPrivileges != null && db2CurrentUserPrivileges.userIsAuthorisedForAdminister();
    }

    // -------------------------
    // Version Testing
    // -------------------------

    public boolean isAtLeastV9_5()
    {
        return version >= DB2Constants.DB2v9_5;
    }

    public boolean isAtLeastV9_7()
    {
        return version >= DB2Constants.DB2v9_7;
    }

    public boolean isAtLeastV10_1()
    {
        return version >= DB2Constants.DB2v10_1;
    }

    public boolean isAtLeastV10_5()
    {
        return version >= DB2Constants.DB2v10_5;
    }

    public boolean isAtLeastV11_1()
    {
        return version >= DB2Constants.DB2v11_1;
    }

    public Double getVersion()
    {
        return version;
    }

    // -------------------------
    // Standards Getters
    // -------------------------

    public DBSObjectCache<DB2DataSource, DB2Bufferpool> getBufferpoolCache()
    {
        return bufferpoolCache;
    }

    public DBSObjectCache<DB2DataSource, DB2RemoteServer> getRemoteServerCache()
    {
        return remoteServerCache;
    }

    public DBSObjectCache<DB2DataSource, DB2Schema> getSchemaCache()
    {
        return schemaCache;
    }

    public DBSObjectCache<DB2DataSource, DB2DataType> getDataTypeCache()
    {
        return dataTypeCache;
    }

    public DBSObjectCache<DB2DataSource, DB2Tablespace> getTablespaceCache()
    {
        return tablespaceCache;
    }

    public DBSObjectCache<DB2DataSource, DB2StorageGroup> getStorageGroupCache()
    {
        return storagegroupCache;
    }

    public DBSObjectCache<DB2DataSource, DB2Variable> getVariableCache()
    {
        return variableCache;
    }

    public DBSObjectCache<DB2DataSource, DB2Role> getRoleCache()
    {
        return roleCache;
    }

    public DBSObjectCache<DB2DataSource, DB2Wrapper> getWrapperCache()
    {
        return wrapperCache;
    }

}
