/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.dbeaver.ext.exasol.ExasolSysTablePrefix;
import org.jkiss.dbeaver.ext.exasol.model.app.ExasolServerSessionManager;
import org.jkiss.dbeaver.ext.exasol.model.cache.ExasolDataTypeCache;
import org.jkiss.dbeaver.ext.exasol.model.plan.ExasolQueryPlanner;
import org.jkiss.dbeaver.ext.exasol.model.security.*;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.DBPErrorAssistant;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.access.DBAUserPasswordManager;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformType;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformer;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectSimpleCache;
import org.jkiss.dbeaver.model.impl.sql.QueryTransformerLimit;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSStructureAssistant;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExasolDataSource extends JDBCDataSource implements IAdaptable {

    private static final Log LOG = Log.getLog(ExasolDataSource.class);

	private DBSObjectCache<ExasolDataSource, ExasolSchema> schemaCache;
	private DBSObjectCache<ExasolDataSource, ExasolVirtualSchema> virtualSchemaCache;
	
	private ExasolCurrentUserPrivileges exasolCurrentUserPrivileges;
	private DBSObjectCache<ExasolDataSource, ExasolUser> userCache = null;
	private DBSObjectCache<ExasolDataSource, ExasolRole> roleCache = null;
	
	private DBSObjectCache<ExasolDataSource, ExasolConnection> connectionCache = null;
	
	private DBSObjectCache<ExasolDataSource, ExasolPriorityGroup> priorityGroupCache = null;
	private DBSObjectCache<ExasolDataSource, ExasolConsumerGroup> consumerGroupCache = null;

	private ExasolDataTypeCache dataTypeCache = new ExasolDataTypeCache();
	
	private DBSObjectCache<ExasolDataSource, ExasolRoleGrant> roleGrantCache = null;
	private DBSObjectCache<ExasolDataSource, ExasolSystemGrant> systemGrantCache = null;
	private DBSObjectCache<ExasolDataSource, ExasolConnectionGrant> connectionGrantCache = null;
	private DBSObjectCache<ExasolDataSource, ExasolBaseObjectGrant> baseTableGrantCache = null;
	private DBSObjectCache<ExasolDataSource, ExasolSecurityPolicy> securityPolicyCache = null;
	
	private Properties addMetaProps = new Properties();
	
	private int driverMajorVersion = 5;

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
		
		try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load data source meta info")) {

			this.exasolCurrentUserPrivileges = new ExasolCurrentUserPrivileges(monitor, session, this);

			this.driverMajorVersion = session.getMetaData().getDriverMajorVersion();

		} catch (SQLException e) {
			LOG.warn("Error reading active schema", e);
		}
		String schemaSQL = "/*snapshot execution*/ select schema_name as object_name,schema_owner as OWNER,CAST(NULL AS TIMESTAMP) AS created, schema_comment as OBJECT_COMMENT, SCHEMA_OBJECT_ID from SYS.EXA_SCHEMAS s  ";
		
		if (exasolCurrentUserPrivileges.getAtLeastV6()) {
			
			//additional where clause to filter virtual schemas
			schemaSQL += " where not  schema_is_virtual ";
			
			String vsAdapterExpressionV8 = "'\"' || ADAPTER_SCRIPT_SCHEMA || '\".\"' || ADAPTER_SCRIPT_NAME || '\"' AS ADAPTER_SCRIPT";

			//build virtual schema cache for >V6 databases
			virtualSchemaCache = new JDBCObjectSimpleCache<>(
					ExasolVirtualSchema.class,
					"/*snapshot execution*/ select" + 
					"	s.SCHEMA_NAME as OBJECT_NAME," + 
					"	s.SCHEMA_OWNER AS OWNER," + 
					"CAST(NULL AS TIMESTAMP) AS created, " +
					"	" + (this.exasolCurrentUserPrivileges.getAtLeastV8() ? vsAdapterExpressionV8 : "ADAPTER_SCRIPT") + "," +
					"	LAST_REFRESH," + 
					"	LAST_REFRESH_BY," + 
					"	ADAPTER_NOTES," + 
					"	SCHEMA_COMMENT AS OBJECT_COMMENT, s.SCHEMA_OBJECT_ID" + 
					" from" + 
					"		EXA_VIRTUAL_SCHEMAS s" + 
					"	INNER JOIN" + 
					"		sys.EXA_SCHEMAS o" + 
					"	ON" + 
					"		o.schema_name = s.SCHEMA_NAME" +
					" ORDER BY S.SCHEMA_NAME"
					);
		}
		
		schemaSQL += " union all select distinct SCHEMA_NAME as \"OBJECT_NAME\", 'SYS' as owner, cast(null as timestamp) as created, '' as \"OBJECT_COMMENT\", null as SCHEMA_OBJECT_ID from SYS.EXA_SYSCAT "
				+ "order by object_name";
		schemaCache = new JDBCObjectSimpleCache<>(
				ExasolSchema.class, schemaSQL);

		try {
			this.dataTypeCache.getAllObjects(monitor, this);
		} catch (DBException e) {
			LOG.warn("Error reading types info", e);
			this.dataTypeCache
					.setCache(Collections.emptyList());
		}

		String priorityColUser = " USER_PRIORITY,\n";
		String priorityColRole = " ROLE_PRIORITY AS USER_PRIORITY,\n";
		if (exasolCurrentUserPrivileges.hasConsumerGroups())  {
			priorityColUser = " USER_CONSUMER_GROUP as USER_PRIORITY,\n";
			priorityColRole = " ROLE_CONSUMER_GROUP AS USER_PRIORITY,\n";
		}
			
		this.userCache = new JDBCObjectSimpleCache<>(ExasolUser.class,
				"/*snapshot execution*/ SELECT\n"
				+ "	USER_NAME,\n"
				+ "	CREATED,\n"
				+ (this.exasolCurrentUserPrivileges.getUserHasDictionaryAccess() ? "	DISTINGUISHED_NAME,\n" : "")
				+ (this.exasolCurrentUserPrivileges.getUserHasDictionaryAccess() ? "	KERBEROS_PRINCIPAL,\n" : "")
				+ (this.exasolCurrentUserPrivileges.getUserHasDictionaryAccess() ? "	PASSWORD,\n" : "")
				+ priorityColUser
				+ "	PASSWORD_STATE,\n"
				+ "	PASSWORD_STATE_CHANGED,\n"
				+ "	PASSWORD_EXPIRY,\n"
				+ "	PASSWORD_EXPIRY_DAYS,\n"
				+ "	PASSWORD_GRACE_DAYS,\n"
				+ "	PASSWORD_EXPIRY_POLICY,\n"
				+ "	FAILED_LOGIN_ATTEMPTS,\n"
				+ "	USER_COMMENT\n"
				+ "FROM SYS."+ this.exasolCurrentUserPrivileges.getTablePrefix(ExasolSysTablePrefix.USER)  +"_USERS ORDER BY USER_NAME");
			
			
		this.roleCache = new JDBCObjectSimpleCache<>(ExasolRole.class, "SELECT ROLE_NAME,CREATED,"+ priorityColRole + " ROLE_COMMENT FROM SYS." + this.exasolCurrentUserPrivileges.getTablePrefix(ExasolSysTablePrefix.SESSION)  +"_ROLES ORDER BY ROLE_NAME");
		
		this.connectionCache = new JDBCObjectSimpleCache<>(
				ExasolConnection.class, "/*snapshot execution*/ SELECT * FROM SYS."+ this.exasolCurrentUserPrivileges.getTablePrefix(ExasolSysTablePrefix.SESSION)  +"_CONNECTIONS ORDER BY CONNECTION_NAME");

		if (exasolCurrentUserPrivileges.hasPasswordPolicy())
		{
			this.securityPolicyCache = new JDBCObjectSimpleCache<>(ExasolSecurityPolicy.class,
					"/*snapshot execution*/ SELECT SYSTEM_VALUE FROM sys.EXA_PARAMETERS WHERE PARAMETER_NAME = 'PASSWORD_SECURITY_POLICY'"
					);
		}
		
		if (exasolCurrentUserPrivileges.hasConsumerGroups()) {
			this.consumerGroupCache = new JDBCObjectSimpleCache<>(ExasolConsumerGroup.class,
					"/*snapshot execution*/ " +
					"SELECT\n" + 
					"CONSUMER_GROUP_NAME,\n" + 
					"CONSUMER_GROUP_ID,\n" + 
					"PRECEDENCE,\n" + 
					"CPU_WEIGHT,\n" + 
					"GROUP_TEMP_DB_RAM_LIMIT,\n" + 
					"USER_TEMP_DB_RAM_LIMIT,\n" + 
					"SESSION_TEMP_DB_RAM_LIMIT,\n" + 
					"CREATED,\n" + 
					"CONSUMER_GROUP_COMMENT\n" + 
					"FROM\n" + 
					"sys.EXA_CONSUMER_GROUPS ecg\n"
			);
			
		}
		
		if (exasolCurrentUserPrivileges.hasPriorityGroups()) {
			this.priorityGroupCache = new JDBCObjectSimpleCache<>(
				ExasolPriorityGroup.class, "/*snapshot execution*/ SELECT * FROM SYS.EXA_PRIORITY_GROUPS ORDER BY PRIORITY_GROUP_NAME"
				);
			
		} else {
			this.priorityGroupCache = new DBSObjectCache<ExasolDataSource, ExasolPriorityGroup>() {
				
				List<ExasolPriorityGroup> groups;
				
				
				@Override
				public void setCache(@NotNull List<ExasolPriorityGroup> objects) {
				}
				
				@Override
				public void removeObject(@NotNull ExasolPriorityGroup object, boolean resetFullCache) {
				}

				@Override
				public void renameObject(@NotNull ExasolPriorityGroup object, @NotNull String oldName, @NotNull String newName) {
				}

				@Override
				public boolean isFullyCached() {
					return true;
				}
				
				@Override
				public ExasolPriorityGroup getObject(@NotNull DBRProgressMonitor monitor, @NotNull ExasolDataSource owner, @NotNull String name) {
					return getCachedObject(name);
				}
				
				@NotNull
				@Override
				public List<ExasolPriorityGroup> getCachedObjects() {
					return groups;
				}
				
				@Override
				public ExasolPriorityGroup getCachedObject(@NotNull String name) {
					for(ExasolPriorityGroup p: groups)
					{
						if (p.getName().equals(name))
							return p;
					}
					return null;
				}
				
				@NotNull
                @Override
				public Collection<ExasolPriorityGroup> getAllObjects(@NotNull DBRProgressMonitor monitor, ExasolDataSource owner)
						throws DBException {
					groups = new ArrayList<>();
					groups.add(new ExasolPriorityGroup(owner, "HIGH", "Default High Group", 900));
					groups.add(new ExasolPriorityGroup(owner, "MEDIUM", "Default Medium Group", 900));
					groups.add(new ExasolPriorityGroup(owner, "LOW", "Default LOW Group", 900));
					return groups;
				}
				
				@Override
				public void clearCache() {
					groups = new ArrayList<>();
				}
				
				@Override
				public void cacheObject(@NotNull ExasolPriorityGroup object) {
					
				}
			};
			this.priorityGroupCache.getAllObjects(monitor, this);
		}
		
		if (exasolCurrentUserPrivileges.getUserHasDictionaryAccess())
		{
			this.connectionGrantCache =  new JDBCObjectSimpleCache<>(
					ExasolConnectionGrant.class,"/*snapshot execution*/ SELECT c.*,P.ADMIN_OPTION,P.GRANTEE FROM SYS.EXA_DBA_CONNECTION_PRIVS P "
							+ "INNER JOIN SYS.EXA_DBA_CONNECTIONS C on P.GRANTED_CONNECTION = C.CONNECTION_NAME ORDER BY P.GRANTEE,C.CONNECTION_NAME ");
		}
		
		if (exasolCurrentUserPrivileges.getUserHasDictionaryAccess())
		{
			this.baseTableGrantCache = new JDBCObjectSimpleCache<>(
					ExasolBaseObjectGrant.class,"/*snapshot execution*/ SELECT " + 
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
		
		if (exasolCurrentUserPrivileges.getUserHasDictionaryAccess())
		{
			this.systemGrantCache = new JDBCObjectSimpleCache<>(
					ExasolSystemGrant.class,
					"/*snapshot execution*/ SELECT GRANTEE,PRIVILEGE,ADMIN_OPTION FROM SYS.EXA_DBA_SYS_PRIVS ORDER BY GRANTEE,PRIVILEGE");
		}
		
		if (exasolCurrentUserPrivileges.getUserHasDictionaryAccess())
		{
			this.roleGrantCache = new JDBCObjectSimpleCache<>(
					ExasolRoleGrant.class,
					"/*snapshot execution*/ select r.*,p.ADMIN_OPTION,p.GRANTEE from EXA_DBA_ROLES r "
					+ "INNER JOIN  EXA_DBA_ROLE_PRIVS p ON p.GRANTED_ROLE = r.ROLE_NAME ORDER BY P.GRANTEE,R.ROLE_NAME"
					);
		}

	}
	
    private Pattern ERROR_POSITION_PATTERN = Pattern.compile("(.+)\\[line ([0-9]+), column ([0-9]+)\\]");

    
    
    int getDriverMajorVersion()
    {
    	return this.driverMajorVersion;
    }
    
    @NotNull
    @Override
    protected Properties getAllConnectionProperties(@NotNull DBRProgressMonitor monitor, JDBCExecutionContext context, String purpose,
                                                    DBPConnectionConfiguration connectionInfo) throws DBCException {
    	
    	Properties props =  super.getAllConnectionProperties(monitor, context, purpose, connectionInfo);
    	
    	if (addMetaProps == null)
    		addMetaProps = new Properties();
    	
		addMetaProps.clear();
    	
    	return props;
    	
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
                return positions.toArray(new ErrorPosition[0]);
            }
        }
        return null;
    }

	@Override
	protected JDBCExecutionContext createExecutionContext(JDBCRemoteInstance instance, String type) {
		return new ExasolExecutionContext(instance, type);
	}

	protected void initializeContextState(@NotNull DBRProgressMonitor monitor,
                                          @NotNull JDBCExecutionContext context, JDBCExecutionContext initFrom)
        throws DBException
	{
		if (initFrom != null) {
			((ExasolExecutionContext)context).setCurrentSchema(monitor, ((ExasolExecutionContext)initFrom).getDefaultSchema());
		} else {
			((ExasolExecutionContext)context).refreshDefaults(monitor, true);
		}
	}

	@Override
	public <T> T getAdapter(Class<T> adapter)
	{
		if (adapter == DBSStructureAssistant.class) {
			return adapter.cast(new ExasolStructureAssistant(this));
		} else if (adapter == DBAServerSessionManager.class) {
			return adapter.cast(new ExasolServerSessionManager(this));
		} else if (adapter == DBAUserPasswordManager.class) {
			return adapter.cast(new ExasolChangeUserPasswordManager(this));
		} else if (adapter == DBCQueryPlanner.class) {
			return adapter.cast(new ExasolQueryPlanner(this));
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
	protected DBPDataSourceInfo createDataSourceInfo(
        DBRProgressMonitor monitor, @NotNull JDBCDatabaseMetaData metaData)
	{
		final ExasolDataSourceInfo info = new ExasolDataSourceInfo(metaData);

		info.setSupportsResultSetScroll(false);

		return info;
	}

	@Override
	protected Map<String, String> getInternalConnectionProperties(
		@NotNull DBRProgressMonitor monitor,
		@NotNull DBPDriver driver,
		@NotNull JDBCExecutionContext context,
		@NotNull String purpose,
		@NotNull DBPConnectionConfiguration connectionInfo
	) throws DBCException {
		Map<String, String> props = new HashMap<>(ExasolDataSourceProvider.getConnectionsProps());
		if (CommonUtils.getBoolean(connectionInfo.getProviderProperty(ExasolConstants.DRV_USE_LEGACY_ENCRYPTION), false)) {
			props.put(ExasolConstants.DRV_LEGACY_ENCRYPTION, "1");
		}
		return props;
	}

	@Override
	public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor)
			throws DBException
	{
		super.refreshObject(monitor);
		
		this.schemaCache.clearCache();
		this.connectionCache.clearCache();
		this.userCache.clearCache();
		//caches for security
		if (this.roleCache != null) {
			this.roleCache.clearCache();
		}
		if (this.baseTableGrantCache != null) {
			this.baseTableGrantCache.clearCache();
		}
		if (this.systemGrantCache != null) {
			this.systemGrantCache.clearCache();
		}
		if (this.connectionGrantCache != null) {
			this.connectionGrantCache.clearCache();
		}
		return this;
	}
	
	// --------------------------
	// Manage Children: ExasolSchema
	// --------------------------

	@NotNull
    @Override
	public Class<? extends ExasolSchema> getPrimaryChildType(@Nullable DBRProgressMonitor monitor) throws DBException
	{
		return ExasolSchema.class;
	}

	@Override
	public Collection<ExasolSchema> getChildren(@NotNull DBRProgressMonitor monitor) throws DBException
	{
		return getSchemas(monitor);
	}

	@Override
	public ExasolSchema getChild(@NotNull DBRProgressMonitor monitor,
								 @NotNull String childName) throws DBException
	{
		if (exasolCurrentUserPrivileges.getAtLeastV6())
			return getSchema(monitor, childName) != null ? getSchema(monitor,childName) : getVirtualSchema(monitor, childName);
		return getSchema(monitor, childName);
	}

	// --------------
	// Associations
	// --------------

	@Association
	public Collection<ExasolSchema> getSchemas(@NotNull DBRProgressMonitor monitor)
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
	   	List<ExasolGrantee> grantees = new ArrayList<>();
		grantees.addAll(this.getUsers(monitor));
		grantees.addAll(this.getRoles(monitor));
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
	public Collection<ExasolPriorityGroup> getPriorityGroups(DBRProgressMonitor monitor) throws DBException
	{
		return priorityGroupCache.getAllObjects(monitor, this);
	}
	
	@Association
	public Collection<ExasolConsumerGroup> getConsumerGroups(DBRProgressMonitor monitor) throws DBException
	{
		return consumerGroupCache.getAllObjects(monitor, this);
	}
	
	public ExasolPriorityGroup getPriorityGroup(DBRProgressMonitor monitor, String name) throws DBException
	{
		return priorityGroupCache.getObject(monitor, this, name);
	}
	
	public ExasolPriority getConsumGroup(DBRProgressMonitor monitor, String name) throws DBException {
		return consumerGroupCache.getObject(monitor, this, name);
	}
	
	
	@Association
	public Collection<ExasolSecurityPolicy> getSecurityPolicies(DBRProgressMonitor monitor) throws DBException
	{
		return securityPolicyCache.getAllObjects(monitor, this);
	}
	
	public ExasolSecurityPolicy getSecurityPolicy(DBRProgressMonitor monitor, String name) throws DBException
	{
		return securityPolicyCache.getCachedObject(name);
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
	
	public DBSObjectCache<ExasolDataSource, ExasolConsumerGroup> getConsumerGroupCache()
	{
		return consumerGroupCache;
	}
	
	public DBSObjectCache<ExasolDataSource, ExasolPriorityGroup> getPriorityGroupCache()
	{
		return priorityGroupCache;
	}
	
	public DBSObjectCache<ExasolDataSource, ExasolSecurityPolicy> getSecurityPolicyCache()
	{
		return securityPolicyCache;
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
				grants.add(new ExasolTableGrant(grant));
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
	
	public ExasolDataType getDataTypeId(long id)
	{
		return dataTypeCache.getDataTypeId(id);
	}

	// -----------------------
	// Authorities
	// -----------------------

	public Boolean hasAlterUserPrivilege()
	{
		return this.exasolCurrentUserPrivileges.getUserHasDictionaryAccess();
	}
	
	public boolean isAuthorizedForUsers()
	{
		return this.exasolCurrentUserPrivileges.getUserHasDictionaryAccess();
	}

	public boolean isAuthorizedForConnections()
	{
		return this.exasolCurrentUserPrivileges
				.getUserHasDictionaryAccess();
	}
	
	public boolean isAuthorizedForRoles()
	{
		return this.exasolCurrentUserPrivileges.getUserHasDictionaryAccess();
	}
	
	public boolean isAuthorizedForRolePrivs()
	{
		return this.exasolCurrentUserPrivileges.getUserHasDictionaryAccess();
	}
	
	public boolean isUserAuthorizedForSessions()
	{
		return this.exasolCurrentUserPrivileges.getUserHasDictionaryAccess();
	}
	
	public boolean isatLeastV6()
	{
		return this.exasolCurrentUserPrivileges.getAtLeastV6();
	}

	public boolean isatLeastV5()
	{
		return this.exasolCurrentUserPrivileges.getAtLeastV5();
	}
	
	public boolean ishasPartitionColumns()
	{
		return this.exasolCurrentUserPrivileges.hasPartitionColumns();
	}
	
	public boolean ishasConsumerGroups()
	{
		return this.exasolCurrentUserPrivileges.hasConsumerGroups();
	}
	
	public boolean ishasPasswordPolicy()
	{
		return this.exasolCurrentUserPrivileges.hasPasswordPolicy();
	}
	
	public boolean ishasPriorityGroups()
	{
		return this.exasolCurrentUserPrivileges.hasPriorityGroups();
	}
	
	public boolean isAuthorizedForConnectionPrivs()
	{
		return this.exasolCurrentUserPrivileges.getUserHasDictionaryAccess();
	}
	
	public boolean isAuthorizedForObjectPrivs()
	{
		return this.exasolCurrentUserPrivileges.getUserHasDictionaryAccess();
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
            if (! addMetaProps.isEmpty())
            	clientName = clientName + "-Meta";
            
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

        // append properties if exists -> meta connection using different type
        if (! addMetaProps.isEmpty()) {
        	Set<Entry<Object, Object>> entries = addMetaProps.entrySet();
        	
            for (Entry<Object, Object> entry : entries) {
            	url.append(";").append(entry.getKey()).append("=").append(entry.getValue());
            }		
        }

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

    DBSObjectCache<ExasolDataSource, ExasolDataType> getDataTypeCache()
	{
		return dataTypeCache;
	}
    
    public ExasolCurrentUserPrivileges getUserPriviliges()
    {
    	return exasolCurrentUserPrivileges;
    }
    
    public String getTablePrefix(ExasolSysTablePrefix fallback) {
    	return exasolCurrentUserPrivileges.getTablePrefix(fallback);
    }
    
    @Override
    public DBCQueryTransformer createQueryTransformer(@NotNull DBCQueryTransformType type) {
        if (type == DBCQueryTransformType.RESULT_SET_LIMIT) {
            return new QueryTransformerLimit(false);
        } else if (type == DBCQueryTransformType.FETCH_ALL_TABLE) {
            return new QueryTransformerFetchAll();
        }
        return super.createQueryTransformer(type);
    }
    
    @Override
    public ErrorType discoverErrorType(@NotNull Throwable error) {
    	// exasol has no sqlstates 
    	String errorMessage = error.getMessage();
		if (errorMessage.contains("Feature not supported")) {
			return ErrorType.FEATURE_UNSUPPORTED;
		} else if (errorMessage.contains("insufficient privileges")) {
			return ErrorType.PERMISSION_DENIED;
		} else if (
				errorMessage.contains("Connection lost") | 
				errorMessage.contains("Connection was killed") | 
				errorMessage.contains("Process does not exist") | 
				errorMessage.contains("Successfully reconnected") | 
				errorMessage.contains("Statement handle not found") | 
				errorMessage.contains("No operations allowed on this connection because it was already closed") |
				errorMessage.contains("Connection was lost and could not be reestablished")
				)
    	{
    		return ErrorType.CONNECTION_LOST;
    	}
    	return super.discoverErrorType(error);
    }


}
