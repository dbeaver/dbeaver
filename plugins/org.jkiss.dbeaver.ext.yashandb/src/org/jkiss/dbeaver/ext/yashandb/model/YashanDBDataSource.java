/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.yashandb.model.session.YashanDBServerSessionManager;
import org.jkiss.dbeaver.ext.yashandb.model.util.YashanDBUtils;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPObjectStatisticsCollector;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.admin.sessions.DBAServerSessionManager;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCExecutionResult;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.exec.output.DBCOutputWriter;
import org.jkiss.dbeaver.model.exec.output.DBCServerOutputReader;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLState;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

public class YashanDBDataSource extends JDBCDataSource implements DBPObjectStatisticsCollector, IAdaptable {

	private static final Log log = Log.getLog(YashanDBDataSource.class);

	private YashanDBSchema publicSchema;
	public final SchemaCache schemaCache = new SchemaCache();
	final DataTypeCache dataTypeCache = new DataTypeCache();
	final TablespaceCache tablespaceCache = new TablespaceCache();
	final UserCache userCache = new UserCache();
	final RoleCache roleCache = new RoleCache();
	final ProfileCache profileCache = new ProfileCache();

	private boolean isAdmin;
	private boolean isAdminVisible;
	private boolean useRuleHint;
	private boolean resolveGeometryAsStruct = true;
	private boolean hasStatistics;

	private YashanDBOutputReader outputReader;

	private final Map<String, Boolean> availableViews = new HashMap<>();

	public YashanDBDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
		super(monitor, container, new YashanDBSQLDialect());
		this.outputReader = new YashanDBOutputReader();
	}

	@Override
	public void initialize(@NotNull DBRProgressMonitor monitor) throws DBException {
		super.initialize(monitor);

		DBPConnectionConfiguration connectionInfo = getContainer().getConnectionConfiguration();

		{
			String useRuleHintProp = connectionInfo.getProviderProperty(YashanDBConstants.PROP_USE_RULE_HINT);
			if (useRuleHintProp != null) {
				useRuleHint = CommonUtils.getBoolean(useRuleHintProp, false);
			}
		}
		this.publicSchema = new YashanDBSchema(this, 1, YashanDBConstants.USER_PUBLIC);

		{
			try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load data source meta info")) {
				this.isAdmin = "YES".equals(
						JDBCUtils.queryString(session, "SELECT 'YES' FROM dba_role_privs WHERE GRANTED_ROLE='DBA'"));
				this.isAdminVisible = isAdmin;
				if (!isAdminVisible) {
					String showAdmin = connectionInfo.getProviderProperty(YashanDBConstants.PROP_ALWAYS_SHOW_DBA);
					if (showAdmin != null) {
						isAdminVisible = CommonUtils.getBoolean(showAdmin, false);
					}
				}
			} catch (SQLException e) {
				log.warn(e);
			}
		}

		dataTypeCache.setCaseSensitive(false);
		{
			List<YashanDBDataType> dtList = new ArrayList<>();
			for (Map.Entry<String, YashanDBDataType.TypeDesc> predefinedType : YashanDBDataType.PREDEFINED_TYPES
					.entrySet()) {
				YashanDBDataType dataType = new YashanDBDataType(this, predefinedType.getKey(), true);
				dtList.add(dataType);
			}
			this.dataTypeCache.setCache(dtList);
		}
	}

	@Override
	protected void initializeContextState(DBRProgressMonitor monitor, JDBCExecutionContext context,
			JDBCExecutionContext initFrom) throws DBException {
		super.initializeContextState(monitor, context, initFrom);

		if (outputReader == null) {
			outputReader = new YashanDBOutputReader();
		}

		if (outputReader != null) {
			outputReader.enableServerOutput(monitor, context, outputReader.isServerOutputEnabled());
		}
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == DBCServerOutputReader.class) {
			return adapter.cast(outputReader);
		} else if (adapter == DBAServerSessionManager.class) {
			return adapter.cast(new YashanDBServerSessionManager(this));
		}
		return super.getAdapter(adapter);
	}

	@Override
	protected JDBCExecutionContext createExecutionContext(JDBCRemoteInstance instance, String type)
			throws DBCException {
		return new YashanDBExecutionContext(instance, type);
	}

	@Override
	public DBSObject refreshObject(DBRProgressMonitor monitor) throws DBException {
		super.refreshObject(monitor);

		this.dataTypeCache.clearCache();
		this.schemaCache.clearCache();
		publicSchema.refreshObject(monitor);
		this.tablespaceCache.clearCache();
		this.userCache.clearCache();
		this.profileCache.clearCache();
		this.initialize(monitor);

		return this;
	}

	public boolean isViewAvailable(@NotNull DBRProgressMonitor monitor, @Nullable String schemaName,
			@NotNull String viewName) {
		viewName = viewName.toUpperCase();
		Boolean available;
		synchronized (availableViews) {
			available = availableViews.get(viewName);
		}
		if (available == null) {
			try {
				try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Check view existence")) {
					String viewNameQuoted = DBUtils.getQuotedIdentifier(this, viewName);
					try (final JDBCPreparedStatement dbStat = session.prepareStatement("SELECT 1 FROM "
							+ (schemaName == null ? viewNameQuoted
									: DBUtils.getQuotedIdentifier(this, schemaName) + "." + viewNameQuoted)
							+ " WHERE 1<>1")) {
						dbStat.setFetchSize(1);
						dbStat.execute();
						available = true;
					}
				}
			} catch (Exception e) {
				available = false;
			}
			synchronized (availableViews) {
				availableViews.put(viewName, available);
			}
		}
		return available;
	}

	public boolean isAdminVisible() {
		return isAdmin || isAdminVisible;
	}

	public boolean isAdmin() {
		return isAdmin;
	}

	public boolean isUseRuleHint() {
		return useRuleHint;
	}

	@NotNull
	@Override
	public DBPDataKind resolveDataKind(@NotNull String typeName, int valueType) {
		if ((typeName.equals(YashanDBConstants.TYPE_NAME_XML) || typeName.equals(YashanDBConstants.TYPE_FQ_XML))) {
			return DBPDataKind.CONTENT;
		}
		if ((typeName.equals(YashanDBConstants.TYPE_NAME_GEOMETRY)
				|| typeName.equals(YashanDBConstants.TYPE_FQ_GEOMETRY))) {
			return resolveGeometryAsStruct ? DBPDataKind.STRUCT : DBPDataKind.OBJECT;
		}
		DBPDataKind dataKind = YashanDBDataType.getDataKind(typeName);
		if (dataKind != null) {
			return dataKind;
		}
		return super.resolveDataKind(typeName, valueType);
	}

	@NotNull
	public YashanDBDataType resolveDataType(@NotNull final DBRProgressMonitor monitor,
			@NotNull final String typeFullName) throws DBException {
		final int divPos = typeFullName.indexOf(SQLConstants.STRUCT_SEPARATOR);
		if (divPos == -1) {
			return this.getLocalDataType(typeFullName);
		}
		final String schemaName = typeFullName.substring(0, divPos);
		final String typeName = typeFullName.substring(divPos + 1);
		final YashanDBSchema schema = this.getSchema(monitor, schemaName);
		if (schema == null) {
			return null;
		}
		return schema.getDataType(monitor, typeName);
	}

	@NotNull
	@Override
	public Collection<? extends DBSDataType> getLocalDataTypes() {
		return dataTypeCache.getCachedObjects();
	}

	@NotNull
	@Override
	public YashanDBDataType getLocalDataType(String typeName) {
		return dataTypeCache.getCachedObject(typeName);
	}

	@Override
	public boolean isStatisticsCollected() {
		return hasStatistics;
	}

	@Override
	public void collectObjectStatistics(DBRProgressMonitor monitor, boolean totalSizeOnly, boolean forceRefresh)
			throws DBException {
		if (hasStatistics && !forceRefresh) {
			return;
		}
		try (final JDBCSession session = DBUtils.openMetaSession(monitor, this,
				"Load tablespace '" + getName() + "' statistics")) {
			try (JDBCStatement dbStat = session.createStatement()) {
				try (JDBCResultSet dbResult = dbStat.executeQuery("SELECT\n"
						+ "\tTS.TABLESPACE_NAME, F.AVAILABLE_SPACE, S.USED_SPACE\n" + "FROM\n"
						+ "\tSYS.DBA_TABLESPACES TS\n"
						+ "\tleft join (SELECT TABLESPACE_NAME, SUM(BYTES) AVAILABLE_SPACE FROM DBA_DATA_FILES GROUP BY TABLESPACE_NAME) F "
						+ "\ton F.TABLESPACE_NAME = TS.TABLESPACE_NAME\n"
						+ "\tleft join (SELECT TABLESPACE_NAME, SUM(BYTES) USED_SPACE FROM DBA_SEGMENTS GROUP BY TABLESPACE_NAME) S\n"
						+ "\ton S.TABLESPACE_NAME = TS.TABLESPACE_NAME")) {
					while (dbResult.next()) {
						String tsName = dbResult.getString(1);
						YashanDBTablespace tablespace = tablespaceCache.getObject(monitor, getDataSource(), tsName);
						if (tablespace != null) {
							tablespace.fetchSizes(dbResult);
						}
					}
				}
			}
		} catch (SQLException e) {
			throw new DBException("Can't read tablespace statistics", e);
		} finally {
			hasStatistics = true;
		}
	}

	@Override
	public YashanDBDataSource getDataSource() {
		return this;
	}

	@Override
	public Collection<YashanDBSchema> getChildren(@NotNull DBRProgressMonitor monitor) throws DBException {
		return getSchemas(monitor);
	}

	@Override
	public YashanDBSchema getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException {
		return getSchema(monitor, childName);
	}

	@Override
	public Class<? extends DBSObject> getPrimaryChildType(DBRProgressMonitor monitor) throws DBException {
		return YashanDBSchema.class;
	}

	@Override
	public void cacheStructure(DBRProgressMonitor monitor, int scope) throws DBException {

	}

	@Association
	public Collection<YashanDBSchema> getSchemas(DBRProgressMonitor monitor) throws DBException {
		return schemaCache.getAllObjects(monitor, this);
	}

	public YashanDBSchema getSchema(DBRProgressMonitor monitor, String name) throws DBException {
		if (publicSchema != null && publicSchema.getName().equals(name)) {
			return publicSchema;
		}
		return schemaCache == null ? null : schemaCache.getObject(monitor, this, name);
	}

	@Association
	public Collection<YashanDBSynonym> getPublicSynonyms(DBRProgressMonitor monitor) throws DBException {
		return publicSchema.getSynonyms(monitor);
	}

	@Association
	public Collection<YashanDBDBLink> getPublicDatabaseLinks(DBRProgressMonitor monitor) throws DBException {
		return publicSchema.getDatabaseLinks(monitor);
	}

	@Association
	public Collection<YashanDBUserProfile> getProfiles(DBRProgressMonitor monitor) throws DBException {
		return profileCache.getAllObjects(monitor, this);
	}

	@Association
	public Collection<YashanDBTablespace> getTablespaces(DBRProgressMonitor monitor) throws DBException {
		return tablespaceCache.getAllObjects(monitor, this);
	}

	@Association
	public Collection<YashanDBRecycledObject> getUserRecycledObjects(DBRProgressMonitor monitor) throws DBException {
		return publicSchema.getRecycledObjects(monitor);
	}

	@Association
	public Collection<YashanDBUser> getUsers(DBRProgressMonitor monitor) throws DBException {
		return userCache.getAllObjects(monitor, this);
	}

	@Association
	public YashanDBUser getUser(DBRProgressMonitor monitor, String name) throws DBException {
		return userCache.getObject(monitor, this, name);
	}

	@Association
	public Collection<YashanDBRole> getRoles(DBRProgressMonitor monitor) throws DBException {
		return roleCache.getAllObjects(monitor, this);
	}

	public YashanDBGrantee getGrantee(DBRProgressMonitor monitor, String name) throws DBException {
		YashanDBUser user = userCache.getObject(monitor, this, name);
		if (user != null) {
			return user;
		}
		return roleCache.getObject(monitor, this, name);
	}

	static class SchemaCache extends JDBCObjectCache<YashanDBDataSource, YashanDBSchema> {

		SchemaCache() {
			setListOrderComparator(DBUtils.<YashanDBSchema>nameComparator());
		}

		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull YashanDBDataSource owner)
				throws SQLException {
			return session.prepareStatement("SELECT U.* FROM "
					+ YashanDBUtils.isAdminPriv(owner.getDataSource(), "USERS") + " U WHERE U.USERNAME IS NOT NULL");
		}

		@Override
		protected YashanDBSchema fetchObject(@NotNull JDBCSession session, @NotNull YashanDBDataSource owner,
				@NotNull JDBCResultSet resultSet) throws SQLException, DBException {
			return new YashanDBSchema(owner, resultSet);
		}

		@Override
		protected void invalidateObjects(DBRProgressMonitor monitor, YashanDBDataSource owner,
				Iterator<YashanDBSchema> objectIter) {
			setListOrderComparator(DBUtils.<YashanDBSchema>nameComparator());
		}
	}

	static class DataTypeCache extends JDBCObjectCache<YashanDBDataSource, YashanDBDataType> {

		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull YashanDBDataSource owner)
				throws SQLException {
			return session.prepareStatement("SELECT * FROM " + YashanDBUtils.isAdminPriv(owner.getDataSource(), "TYPES")
					+ " WHERE OWNER IS NULL ORDER BY TYPE_NAME");
		}

		@Override
		protected YashanDBDataType fetchObject(@NotNull JDBCSession session, @NotNull YashanDBDataSource owner,
				@NotNull JDBCResultSet resultSet) throws SQLException, DBException {
			return new YashanDBDataType(owner, resultSet);
		}
	}

	static class TablespaceCache extends JDBCObjectCache<YashanDBDataSource, YashanDBTablespace> {
		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull YashanDBDataSource owner)
				throws SQLException {
			return session.prepareStatement("SELECT * FROM "
					+ YashanDBUtils.isAdminPriv(owner.getDataSource(), "TABLESPACES") + " ORDER BY TABLESPACE_NAME");
		}

		@Override
		protected YashanDBTablespace fetchObject(@NotNull JDBCSession session, @NotNull YashanDBDataSource owner,
				@NotNull JDBCResultSet resultSet) throws SQLException, DBException {
			return new YashanDBTablespace(owner, resultSet);
		}
	}

	static class UserCache extends JDBCObjectCache<YashanDBDataSource, YashanDBUser> {
		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull YashanDBDataSource owner)
				throws SQLException {
			return session
					.prepareStatement("SELECT * FROM " + YashanDBUtils.isAdminPriv(owner.getDataSource(), "USERS"));

		}

		@Override
		protected YashanDBUser fetchObject(@NotNull JDBCSession session, @NotNull YashanDBDataSource owner,
				@NotNull JDBCResultSet resultSet) throws SQLException, DBException {
			return new YashanDBUser(owner, resultSet);
		}
	}

	static class RoleCache extends JDBCObjectCache<YashanDBDataSource, YashanDBRole> {
		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull YashanDBDataSource owner)
				throws SQLException {
			return session.prepareStatement(
					"SELECT * FROM " + YashanDBUtils.isAdminPriv(owner.getDataSource(), "ROLES") + " ORDER BY ROLE");
		}

		@Override
		protected YashanDBRole fetchObject(@NotNull JDBCSession session, @NotNull YashanDBDataSource owner,
				@NotNull JDBCResultSet resultSet) throws SQLException, DBException {
			return new YashanDBRole(owner, resultSet);
		}
	}

	static class ProfileCache
			extends JDBCStructCache<YashanDBDataSource, YashanDBUserProfile, YashanDBUserProfile.ProfileResource> {

		protected ProfileCache() {
			super("PROFILE");
		}

		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull YashanDBDataSource owner)
				throws SQLException {
			return session.prepareStatement("SELECT DISTINCT PROFILE FROM DBA_PROFILES ORDER BY PROFILE");
		}

		@Override
		protected YashanDBUserProfile fetchObject(@NotNull JDBCSession session, @NotNull YashanDBDataSource owner,
				@NotNull JDBCResultSet resultSet) throws SQLException, DBException {
			return new YashanDBUserProfile(owner, resultSet);
		}

		@Override
		protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session,
				@NotNull YashanDBDataSource dataSource, @Nullable YashanDBUserProfile forObject) throws SQLException {
			final JDBCPreparedStatement dbStat = session
					.prepareStatement("SELECT RESOURCE_NAME,RESOURCE_TYPE,LIMIT FROM DBA_PROFILES "
							+ (forObject == null ? "" : "WHERE PROFILE=? ") + "ORDER BY RESOURCE_NAME");
			if (forObject != null) {
				dbStat.setString(1, forObject.getName());
			}
			return dbStat;
		}

		@Override
		protected YashanDBUserProfile.ProfileResource fetchChild(@NotNull JDBCSession session,
				@NotNull YashanDBDataSource dataSource, @NotNull YashanDBUserProfile parent,
				@NotNull JDBCResultSet dbResult) throws SQLException, DBException {
			return new YashanDBUserProfile.ProfileResource(parent, dbResult);
		}
	}

	@Override
	public ErrorType discoverErrorType(Throwable error) {
		if ("58030".equalsIgnoreCase(SQLState.getStateFromException(error)))
			return ErrorType.CONNECTION_LOST;
		return super.discoverErrorType(error);
	}

	static class YashanDBOutputReader implements DBCServerOutputReader {

		@Override
		public boolean isServerOutputEnabled() {
			return true;
		}

		@Override
		public boolean isAsyncOutputReadSupported() {
			return false;
		}

		@Override
		public void readServerOutput(DBRProgressMonitor monitor, DBCExecutionContext context,
				DBCExecutionResult executionResult, DBCStatement statement, DBCOutputWriter output)
				throws DBCException {
			try (JDBCSession session = (JDBCSession) context.openSession(monitor, DBCExecutionPurpose.UTIL,
					"Read YashanDB server output")) {
				try (CallableStatement getLineProc = session.getOriginal()
						.prepareCall("{CALL DBMS_OUTPUT.GET_LINE(?, ?)}")) {
					getLineProc.registerOutParameter(1, java.sql.Types.VARCHAR);
					getLineProc.registerOutParameter(2, java.sql.Types.INTEGER);
					int status = 0;
					while (status == 0) {
						getLineProc.execute();
						status = getLineProc.getInt(2);
						if (status == 0) {
							output.println(null, getLineProc.getString(1));
						}
					}
				} catch (SQLException e) {
					throw new DBCException(e, context);
				}
			}
		}

		public void enableServerOutput(DBRProgressMonitor monitor, DBCExecutionContext context, boolean enable)
				throws DBCException {
			String sql = enable ? "BEGIN DBMS_OUTPUT.ENABLE(" + YashanDBConstants.MAXIMUM_DBMS_OUTPUT_SIZE + "); END;"
					: "BEGIN DBMS_OUTPUT.DISABLE; END;";
			try (DBCSession session = context.openSession(monitor, DBCExecutionPurpose.UTIL,
					(enable ? "Enable" : "Disable ") + "DBMS output")) {
				JDBCUtils.executeSQL((JDBCSession) session, sql);
			} catch (SQLException e) {
				throw new DBCException(e, context);
			}
		}
	}
}
