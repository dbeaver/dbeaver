/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016 Karl Griesser (fullref@gmail.com)
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
package org.jkiss.dbeaver.ext.exasol.model;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.exasol.ExasolConstants;
import org.jkiss.dbeaver.ext.exasol.ExasolDataSourceProvider;
import org.jkiss.dbeaver.ext.exasol.ExasolSQLDialect;
import org.jkiss.dbeaver.ext.exasol.manager.security.*;
import org.jkiss.dbeaver.ext.exasol.model.plan.ExasolPlanAnalyser;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.DBPErrorAssistant;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
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
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectSelector;
import org.jkiss.dbeaver.model.struct.DBSStructureAssistant;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExasolDataSource extends JDBCDataSource
		implements DBSObjectSelector, DBCQueryPlanner, IAdaptable {

    private static final Log LOG = Log.getLog(ExasolDataSource.class);

	private static final String GET_CURRENT_SCHEMA = "SELECT CURRENT_SCHEMA";
	private static final String SET_CURRENT_SCHEMA = "OPEN SCHEMA \"%s\"";

	private DBSObjectCache<ExasolDataSource, ExasolSchema> schemaCache;
	private DBSObjectCache<ExasolDataSource, ExasolVirtualSchema> virtualSchemaCache;

	private ExasolCurrentUserPrivileges exasolCurrentUserPrivileges;
	private DBSObjectCache<ExasolDataSource, ExasolUser> userCache = null;
	private DBSObjectCache<ExasolDataSource, ExasolRole> roleCache = null;
	
	private DBSObjectCache<ExasolDataSource, ExasolConnection> connectionCache = null;

	private DBSObjectCache<ExasolDataSource, ExasolDataType> dataTypeCache = new JDBCObjectSimpleCache<>(
			ExasolDataType.class, "SELECT * FROM EXA_SQL_TYPES");
	
	private DBSObjectCache<ExasolDataSource, ExasolRoleGrant> roleGrantCache = null;
	private DBSObjectCache<ExasolDataSource, ExasolSystemGrant> systemGrantCache = null;
	private DBSObjectCache<ExasolDataSource, ExasolConnectionGrant> connectionGrantCache = null;
	private DBSObjectCache<ExasolDataSource, ExasolBaseObjectGrant> baseTableGrantCache = null;
	
	private int driverMajorVersion = 5;
	

	private String activeSchemaName;

	// -----------------------
	// Constructors
	// -----------------------

	public ExasolDataSource(DBRProgressMonitor monitor,
			DBPDataSourceContainer container) throws DBException
	{
		super(monitor, container, new ExasolSQLDialect());
	}

	// -----------------------
	// Initialization/Structure
	// -----------------------

	@Override
	public void initialize(@NotNull DBRProgressMonitor monitor)
			throws DBException
	{
		super.initialize(monitor);
		
		try (JDBCSession session = DBUtils.openMetaSession(monitor, this,
				"Load data source meta info")) {

			// First try to get active schema from special register 'CURRENT
			// SCHEMA'
			this.activeSchemaName = determineActiveSchema(session);
			this.exasolCurrentUserPrivileges = new ExasolCurrentUserPrivileges(
					monitor, session, this);
			
			this.driverMajorVersion = session.getMetaData().getDriverMajorVersion();

		} catch (SQLException e) {
			LOG.warn("Error reading active schema", e);
		}


		String schemaSQL = "select b.object_name,b.owner,b.created,b.object_comment from EXA_ALL_OBJECTS b  "
				+ "inner join EXA_SCHEMAS s on b.object_name = s.schema_name  where b.object_type = 'SCHEMA' ";
		
		if (exasolCurrentUserPrivileges.getatLeastV6()) {
			
			//additional where clause to filter virtual schemas
			schemaSQL += " and not schema_is_virtual ";
			
			//build virtual schema cache for >V6 databases
			virtualSchemaCache = new JDBCObjectSimpleCache<>(
					ExasolVirtualSchema.class,
					"select" + 
					"	SCHEMA_NAME as OBJECT_NAME," + 
					"	SCHEMA_OWNER AS OWNER," + 
					"	ADAPTER_SCRIPT," + 
					"	LAST_REFRESH," + 
					"	LAST_REFRESH_BY," + 
					"	ADAPTER_NOTES," + 
					"	OBJECT_COMMENT," + 
					"	CREATED" + 
					" from" + 
					"		EXA_VIRTUAL_SCHEMAS s" + 
					"	INNER JOIN" + 
					"		EXA_ALL_OBJECTS o" + 
					"	ON" + 
					"		o.OBJECT_NAME = s.SCHEMA_NAME AND" + 
					"		o.OBJECT_TYPE = 'SCHEMA'"
					);
		}
		
		schemaSQL += " union all select distinct SCHEMA_NAME as \"OBJECT_NAME\", 'SYS' as owner, cast(null as timestamp) as created, '' as \"OBJECT_COMMENT\" from SYS.EXA_SYSCAT "
				+ "order by b.object_name";
		schemaCache = new JDBCObjectSimpleCache<>(
				ExasolSchema.class, schemaSQL);

		try {
			this.dataTypeCache.getAllObjects(monitor, this);
		} catch (DBException e) {
			LOG.warn("Error reading types info", e);
			this.dataTypeCache
					.setCache(Collections.<ExasolDataType> emptyList());
		}

		if (exasolCurrentUserPrivileges.getUserIsAuthorizedForUsers()) {
			this.userCache = new JDBCObjectSimpleCache<>(ExasolUser.class,
					"select * from EXA_DBA_USERS ORDER BY USER_NAME");
		}
		if (exasolCurrentUserPrivileges.getUserIsAuthorizedForRoles()) {
			this.roleCache = new JDBCObjectSimpleCache<>(ExasolRole.class, "SELECT * FROM EXA_DBA_ROLES ORDER BY ROLE_NAME");
		}
		
		if (exasolCurrentUserPrivileges.getUserIsAuthorizedForConnections()) {
			this.connectionCache = new JDBCObjectSimpleCache<>(
					ExasolConnection.class, "SELECT * FROM SYS.EXA_DBA_CONNECTIONS ORDER BY CONNECTION_NAME");
		} else {
			this.connectionCache = new JDBCObjectSimpleCache<>(
					ExasolConnection.class, "SELECT * FROM SYS.EXA_SESSION_CONNECTIONS ORDER BY CONNECTION_NAME");
		}
		
		if (exasolCurrentUserPrivileges.getUserIsAuthorizedForConnectionPrivs())
		{
			this.connectionGrantCache =  new JDBCObjectSimpleCache<>(
					ExasolConnectionGrant.class,"SELECT c.*,P.ADMIN_OPTION,P.GRANTEE FROM SYS.EXA_DBA_CONNECTION_PRIVS P "
							+ "INNER JOIN SYS.EXA_DBA_CONNECTIONS C on P.GRANTED_CONNECTION = C.CONNECTION_NAME ORDER BY P.GRANTEE,C.CONNECTION_NAME ");
		}
		
		if (exasolCurrentUserPrivileges.getUserIsAuthorizedForObjectPrivs())
		{
			this.baseTableGrantCache = new JDBCObjectSimpleCache<>(
					ExasolBaseObjectGrant.class,"SELECT " + 
							"	OBJECT_SCHEMA," + 
							"	OBJECT_TYPE," + 
							"	GRANTEE," + 
							"	OBJECT_NAME," + 
							"	GROUP_CONCAT(" + 
							"		DISTINCT PRIVILEGE" + 
							"	ORDER BY" + 
							"		OBJECT_SCHEMA," + 
							"		OBJECT_NAME" + 
							"		SEPARATOR '|'" + 
							"	) as PRIVS " + 
							" FROM" + 
							"	SYS.EXA_DBA_OBJ_PRIVS P" + 
							" GROUP BY" + 
							"	OBJECT_SCHEMA," + 
							"	OBJECT_TYPE," + 
							"	GRANTEE," + 
							"	OBJECT_NAME ORDER BY GRANTEE,OBJECT_SCHEMA,OBJECT_TYPE,OBJECT_NAME");

		}
		
		if (exasolCurrentUserPrivileges.getUserIsAuthorizedForSystemPrivs())
		{
			this.systemGrantCache = new JDBCObjectSimpleCache<>(
					ExasolSystemGrant.class,
					"SELECT GRANTEE,PRIVILEGE,ADMIN_OPTION FROM SYS.EXA_DBA_SYS_PRIVS ORDER BY GRANTEE,PRIVILEGE");
		}
		
		if (exasolCurrentUserPrivileges.getUserIsAuthorizedForRolePrivs())
		{
			this.roleGrantCache = new JDBCObjectSimpleCache<>(
					ExasolRoleGrant.class,
					"select r.*,p.ADMIN_OPTION,p.GRANTEE from EXA_DBA_ROLES r "
					+ "INNER JOIN  EXA_DBA_ROLE_PRIVS p ON p.GRANTED_ROLE = r.ROLE_NAME ORDER BY P.GRANTEE,R.ROLE_NAME"
					);
		}

	}
	
    private Pattern ERROR_POSITION_PATTERN = Pattern.compile("(.+)\\[line ([0-9]+), column ([0-9]+)\\]");

    
    int getDriverMajorVersion()
    {
    	return this.driverMajorVersion;
    }
    
    @Nullable
    @Override
    public ErrorPosition[] getErrorPosition(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext context, @NotNull String query, @NotNull Throwable error) {
        while (error instanceof DBException) {
            if (error.getCause() == null) {
                return null;
            }
            error = error.getCause();
        }
        String message = error.getMessage();
        if (!CommonUtils.isEmpty(message)) {
            Matcher matcher = ERROR_POSITION_PATTERN.matcher(message);
            List<ErrorPosition> positions = new ArrayList<>();
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
	

	protected void initializeContextState(@NotNull DBRProgressMonitor monitor,
			@NotNull JDBCExecutionContext context, boolean setActiveObject)
			throws DBCException
	{
		if (setActiveObject) {
			setCurrentSchema(monitor, context, getDefaultObject());
		}
	}

	private String determineActiveSchema(JDBCSession session)
			throws SQLException
	{
		// First try to get active schema from special register 'CURRENT SCHEMA'
		String defSchema = JDBCUtils.queryString(session, GET_CURRENT_SCHEMA);
		if (defSchema == null) {
			return "";
		}

		return defSchema.trim();
	}

	@Override
	public <T> T getAdapter(Class<T> adapter)
	{
		if (adapter == DBSStructureAssistant.class) {
			return adapter.cast(new ExasolStructureAssistant(this));
		}
		return super.getAdapter(adapter);
	}

	@Override
	public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope)
			throws DBException
	{
		// TODO DF: No idea what to do with this method, what it is used for...
	}

	// -----------------------
	// Connection related Info
	// -----------------------

	@Override
	protected String getConnectionUserName(
			@NotNull DBPConnectionConfiguration connectionInfo)
	{
		return connectionInfo.getUserName();
	}

	@NotNull
	@Override
	public ExasolDataSource getDataSource()
	{
		return this;
	}

	@Override
	protected DBPDataSourceInfo createDataSourceInfo(
			@NotNull JDBCDatabaseMetaData metaData)
	{
		final ExasolDataSourceInfo info = new ExasolDataSourceInfo(metaData);

		info.setSupportsResultSetScroll(false);

		return info;
	}

	@Override
	protected Map<String, String> getInternalConnectionProperties(
        DBRProgressMonitor monitor, DBPDriver driver, String purpose, DBPConnectionConfiguration connectionInfo) throws DBCException
	{
		Map<String, String> props = new HashMap<>();
		props.putAll(ExasolDataSourceProvider.getConnectionsProps());
		return props;
	}

	@Override
	public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor)
			throws DBException
	{
		super.refreshObject(monitor);

		this.schemaCache.clearCache();
		if (this.userCache != null) 
				this.userCache.clearCache();
		this.dataTypeCache.clearCache();
		
		if (this.roleCache != null)
			this.roleCache.clearCache();
		if (this.connectionCache != null)
			this.connectionCache.clearCache();

		//caches for security
		if (this.connectionGrantCache != null)
			this.connectionGrantCache.clearCache();
		
		if (this.baseTableGrantCache != null)
			this.baseTableGrantCache.clearCache();
		
		if (this.systemGrantCache != null)
			this.systemGrantCache.clearCache();
		
		if (this.roleCache != null)
			this.roleCache.clearCache();

		this.initialize(monitor);

		return this;
	}
	
	// --------------------------
	// Manage Children: ExasolSchema
	// --------------------------

	@Override
	public boolean supportsDefaultChange()
	{
		return true;
	}

	@Override
	public Class<? extends ExasolSchema> getChildType(
			@NotNull DBRProgressMonitor monitor) throws DBException
	{
		return ExasolSchema.class;
	}

	@Override
	public Collection<ExasolSchema> getChildren(
			@NotNull DBRProgressMonitor monitor) throws DBException
	{
		Collection<ExasolSchema> totalList = getSchemas(monitor);
		if (exasolCurrentUserPrivileges.getatLeastV6())
			totalList.addAll(getVirtualSchemas(monitor));
		return totalList;
	}

	@Override
	public ExasolSchema getChild(@NotNull DBRProgressMonitor monitor,
			@NotNull String childName) throws DBException
	{
		if (exasolCurrentUserPrivileges.getatLeastV6())
			return getSchema(monitor, childName) != null ? getSchema(monitor,childName) : getVirtualSchema(monitor, childName);
		return getSchema(monitor, childName);
	}

	@Override
	public ExasolSchema getDefaultObject()
	{
		return activeSchemaName == null ? null : schemaCache.getCachedObject(activeSchemaName);
	}

	@Override
	public void setDefaultObject(@NotNull DBRProgressMonitor monitor,
			@NotNull DBSObject object) throws DBException
	{
		final ExasolSchema oldSelectedEntity = getDefaultObject();

		if (!(object instanceof ExasolSchema)) {
			throw new IllegalArgumentException(
					"Invalid object type: " + object);
		}

		for (JDBCExecutionContext context : getDefaultInstance().getAllContexts()) {
			setCurrentSchema(monitor, context, (ExasolSchema) object);
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
	public boolean refreshDefaultObject(@NotNull DBCSession session)
			throws DBException
	{
		try {
			final String newSchemaName = determineActiveSchema(
					(JDBCSession) session);
			if (!CommonUtils.equalObjects(newSchemaName, activeSchemaName)) {
				final ExasolSchema newSchema = schemaCache
						.getCachedObject(newSchemaName);
				if (newSchema != null) {
					setDefaultObject(session.getProgressMonitor(), newSchema);
					return true;
				}
			}
			return false;
		} catch (Exception e) {
			throw new DBException(e, this);
		}
	}

	private void setCurrentSchema(DBRProgressMonitor monitor,
			JDBCExecutionContext executionContext, ExasolSchema object)
			throws DBCException
	{
		if (object == null) {
			LOG.debug("Null current schema");
			return;
		}
		try (JDBCSession session = executionContext.openSession(monitor,
				DBCExecutionPurpose.UTIL, "Set active schema")) {
			JDBCUtils.executeSQL(session,
					String.format(SET_CURRENT_SCHEMA, object.getName()));
		} catch (SQLException e) {
			throw new DBCException(e, this);
		}
	}

	// --------------
	// Associations
	// --------------

	@Association
	public Collection<ExasolSchema> getSchemas(DBRProgressMonitor monitor)
			throws DBException
	{
		return schemaCache.getAllObjects(monitor, this);
	}

	public ExasolSchema getSchema(DBRProgressMonitor monitor, String name)
			throws DBException
	{
		return schemaCache.getObject(monitor, this, name);
	}

	@Association
	public Collection<ExasolVirtualSchema> getVirtualSchemas(DBRProgressMonitor monitor) 
			throws DBException
	{
		return virtualSchemaCache.getAllObjects(monitor, this);
	}

	public ExasolVirtualSchema getVirtualSchema(DBRProgressMonitor monitor, String name)
			throws DBException
	{
		return virtualSchemaCache.getObject(monitor, this, name);
	}

	
	public Collection<ExasolGrantee> getAllGrantees(DBRProgressMonitor monitor) throws DBException
	{
	   ArrayList<ExasolGrantee> grantees = new ArrayList<>();
	   
	   for (ExasolUser user : this.getUsers(monitor)) {
	       grantees.add((ExasolGrantee) user);
	   }
	   
	   for (ExasolRole role : this.getRoles(monitor))
	   {
	       grantees.add((ExasolGrantee) role);
	   }
	   
	   return grantees;
	}
	
    @Association
	public Collection<ExasolUser> getUsers(DBRProgressMonitor monitor)
			throws DBException
	{
		return userCache.getAllObjects(monitor, this);
	}

	public ExasolUser getUser(DBRProgressMonitor monitor, String name)
			throws DBException
	{
		return userCache.getObject(monitor, this, name);
	}

    @Association
	public Collection<ExasolRole> getRoles(DBRProgressMonitor monitor)
			throws DBException
	{
		return roleCache.getAllObjects(monitor, this);
		
	}
	
	public ExasolRole getRole(DBRProgressMonitor monitor, String name)
			throws DBException
	{
		return roleCache.getObject(monitor, this, name);
	}

    @Association
	public Collection<ExasolConnection> getConnections(
			DBRProgressMonitor monitor) throws DBException
	{
		return connectionCache.getAllObjects(monitor, this);
	}
	
	public ExasolConnection getConnection(DBRProgressMonitor monitor,
			String name) throws DBException
	{
		return connectionCache.getObject(monitor, this, name);
	}
	
	

    @Association
	public Collection<ExasolBaseObjectGrant> getBaseTableGrants(DBRProgressMonitor monitor) throws DBException
	{
		return baseTableGrantCache.getAllObjects(monitor, this);
	}
	
	public Collection<ExasolTableGrant> getTableGrants(DBRProgressMonitor monitor) throws DBException
	{
		Collection<ExasolTableGrant> grants = new ArrayList<>();
		
		for (ExasolBaseObjectGrant grant: this.getBaseTableGrants(monitor))
		{
			//only add tables
			if (grant.getType() == ExasolTableObjectType.TABLE)
				grants.add(new ExasolTableGrant(monitor, grant));
		}
		return grants;
	}

	public Collection<ExasolViewGrant> getViewGrants(DBRProgressMonitor monitor) throws DBException
	{
		Collection<ExasolViewGrant> grants = new ArrayList<>();
		
		for (ExasolBaseObjectGrant grant: this.getBaseTableGrants(monitor))
		{
			//only add tables
			if (grant.getType() == ExasolTableObjectType.VIEW)
				grants.add(new ExasolViewGrant(grant));
		}
		return grants;
	}
	
	public Collection<ExasolScriptGrant> getScriptGrants(DBRProgressMonitor monitor) throws DBException
	{
		Collection<ExasolScriptGrant> grants = new ArrayList<>();
		
		for (ExasolBaseObjectGrant grant: this.getBaseTableGrants(monitor))
		{
			//only add tables
			if (grant.getType() == ExasolTableObjectType.SCRIPT)
				grants.add(new ExasolScriptGrant(grant));
		}
		return grants;
	}
	
	public Collection<ExasolSchemaGrant> getSchemaGrants(DBRProgressMonitor monitor) throws DBException
	{
		Collection<ExasolSchemaGrant> grants = new ArrayList<>();
		
		for (ExasolBaseObjectGrant grant: this.getBaseTableGrants(monitor))
		{
			//only add tables
			if (grant.getType() == ExasolTableObjectType.SCHEMA)
				grants.add(new ExasolSchemaGrant(grant));
		}
		return grants;
	}
	
	
	public Collection<ExasolConnectionGrant> getConnectionGrants(DBRProgressMonitor monitor) throws DBException
	{
		return connectionGrantCache.getAllObjects(monitor, this);
	}
	
	public Collection<ExasolSystemGrant> getSystemGrants(DBRProgressMonitor monitor) throws DBException
	{
		return this.systemGrantCache.getAllObjects(monitor, this);
	}
	
	public Collection<ExasolRoleGrant> getRoleGrants(DBRProgressMonitor monitor) throws DBException
	{
		return this.roleGrantCache.getAllObjects(monitor, this);
	}
	

	@Association
	public Collection<ExasolDataType> getDataTypes(DBRProgressMonitor monitor)
			throws DBException
	{
		return dataTypeCache.getAllObjects(monitor, this);
	}

	public ExasolDataType getDataType(DBRProgressMonitor monitor, String name)
			throws DBException
	{
		return dataTypeCache.getObject(monitor, this, name);
	}

	// -----------------------
	// Authorities
	// -----------------------

	public Boolean hasAlterUserPrivilege()
	{
		return this.exasolCurrentUserPrivileges.UserHasAlterUserPriv();
	}
	
	public boolean isAuthorizedForUsers()
	{
		return this.exasolCurrentUserPrivileges.getUserIsAuthorizedForUsers();
	}

	public boolean isAuthorizedForConnections()
	{
		return this.exasolCurrentUserPrivileges
				.getUserIsAuthorizedForConnections();
	}
	
	public boolean isAuthorizedForRoles()
	{
		return this.exasolCurrentUserPrivileges.getUserIsAuthorizedForRoles();
	}
	
	public boolean isAuthorizedForRolePrivs()
	{
		return this.exasolCurrentUserPrivileges.getUserIsAuthorizedForRolePrivs();
	}
	
	public boolean isUserAuthorizedForSessions()
	{
		return this.exasolCurrentUserPrivileges.isUserAuthorizedForSessions();
	}
	
	public boolean isatLeastV6()
	{
		return this.exasolCurrentUserPrivileges.getatLeastV6();
	}

	public boolean isatLeastV5()
	{
		return this.exasolCurrentUserPrivileges.getatLeastV5();
	}
	
	public boolean isAuthorizedForConnectionPrivs()
	{
		return this.exasolCurrentUserPrivileges.getUserIsAuthorizedForConnectionPrivs();
	}
	
	public boolean isAuthorizedForObjectPrivs()
	{
		return this.exasolCurrentUserPrivileges.getUserIsAuthorizedForObjectPrivs();
	}

	// -------------------------
	// Standards Getters
	// -------------------------

   public DBSObjectCache<ExasolDataSource, ExasolConnection> getConnectionCache()
    {
        return connectionCache;
    }
   
	public DBSObjectCache<ExasolDataSource, ExasolUser> getUserCache()
	{
		return userCache;
	}
	
	public DBSObjectCache<ExasolDataSource, ExasolRole> getRoleCache()
	{
		return roleCache;
	}

	public DBSObjectCache<ExasolDataSource, ExasolSchema> getSchemaCache()
	{
		return schemaCache;
	}
	
	

	@Override
	public Collection<? extends DBSDataType> getLocalDataTypes()
	{
		try {
			return getDataTypes(new VoidProgressMonitor());
		} catch (DBException e) {
			LOG.error("DBException occured when reading system dataTypes: ", e);
			return null;
		}
	}
	
    @Override
    public String getConnectionURL(DBPConnectionConfiguration connectionInfo) {
        //Default Port
        String port = ":8563";
        if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
            port = ":" + connectionInfo.getHostPort();
        }
        Map<String, String> properties = connectionInfo.getProperties();

        StringBuilder url = new StringBuilder(128);
        url.append("jdbc:exa:").append(connectionInfo.getHostName()).append(port);

        //check if we got an backup host list
        String backupHostList = connectionInfo.getProviderProperty(ExasolConstants.DRV_BACKUP_HOST_LIST);

        if (!CommonUtils.isEmpty(backupHostList))
            url.append(",").append(backupHostList).append(port);

        if (!url.toString().toUpperCase().contains("CLIENTNAME")) {
            // Client info can only be provided in the url with the exasol driver
            String clientName = Platform.getProduct().getName();

            Object propClientName = properties.get(ExasolConstants.DRV_CLIENT_NAME);
            if (propClientName != null)
                clientName = propClientName.toString();
            url.append(";clientname=").append(clientName);
        }
        
        if (!url.toString().toUpperCase().contains("CLIENTVERSION"))
        {
        	String clientVersion=Platform.getProduct().getDefiningBundle().getVersion().toString();
            Object propClientName = properties.get(ExasolConstants.DRV_CLIENT_VERSION);
            if (propClientName != null)
            	clientVersion = propClientName.toString();
            url.append(";clientversion=").append(clientVersion);
        }
        Object querytimeout = properties.get(ExasolConstants.DRV_QUERYTIMEOUT);
        if (querytimeout != null)
            url.append(";").append(ExasolConstants.DRV_QUERYTIMEOUT).append("=").append(querytimeout);

        Object connecttimeout = properties.get(ExasolConstants.DRV_CONNECT_TIMEOUT);
        if (connecttimeout != null)
            url.append(";").append(ExasolConstants.DRV_CONNECT_TIMEOUT).append("=").append(connecttimeout);

        return url.toString();
    }
	

	@Override
	public DBSDataType getLocalDataType(String typeName)
	{
		try {
			return getDataType(new VoidProgressMonitor(), typeName);
		} catch (DBException e) {
			LOG.error("DBException occured when reading system dataType: "
					+ typeName, e);
			return null;
		}
	}

	@NotNull
	@Override
	public DBCPlan planQueryExecution(@NotNull DBCSession session, @NotNull String query)
			throws DBCException
	{
		ExasolPlanAnalyser plan = new ExasolPlanAnalyser(this, query);
		plan.explain(session);
		return plan;
	}

    @NotNull
	@Override
    public DBCPlanStyle getPlanStyle() {
        return DBCPlanStyle.PLAN;
    }

    DBSObjectCache<ExasolDataSource, ExasolDataType> getDataTypeCache()
	{
		return dataTypeCache;
	}
    
    public ExasolCurrentUserPrivileges getUserPriviliges()
    {
    	return exasolCurrentUserPrivileges;
    }
    

}
