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

package org.jkiss.dbeaver.ext.oceanbase.oracle.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataSource;
import org.jkiss.dbeaver.ext.oracle.model.OracleGrantee;
import org.jkiss.dbeaver.ext.oracle.model.OraclePrivSystem;
import org.jkiss.dbeaver.ext.oracle.model.OracleUser;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class OceanbaseOracleUser extends OracleUser {
	private final SystemPrivCache systemPrivCache = new SystemPrivCache();

	private long id;
	private String name;
	private String externalName;
	private String status;
	private Timestamp createDate;
	private Timestamp lockDate;
	private Timestamp expiryDate;
	private transient String password;

	public OceanbaseOracleUser(OceanbaseOracleDataSource dataSource) {
		super(dataSource);
	}

	public OceanbaseOracleUser(OracleDataSource dataSource, ResultSet resultSet) {
		super(dataSource);
		this.id = JDBCUtils.safeGetLong(resultSet, "USERID");
		this.name = JDBCUtils.safeGetString(resultSet, "USERNAME");
		this.externalName = JDBCUtils.safeGetString(resultSet, "EXTERNAL_NAME");
		this.status = JDBCUtils.safeGetString(resultSet, "ACCOUNT_STATUS");

		this.createDate = JDBCUtils.safeGetTimestamp(resultSet, "CREATED");
		this.lockDate = JDBCUtils.safeGetTimestamp(resultSet, "LOCK_DATE");
		this.expiryDate = JDBCUtils.safeGetTimestamp(resultSet, "EXPIRY_DATE");
	}

	@Property(order = 1)
	public long getId() {
		return id;
	}

	@NotNull
	@Override
	@Property(viewable = true, order = 2)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Property(order = 3)
	public String getExternalName() {
		return externalName;
	}

	@Property(viewable = true, order = 4)
	public String getStatus() {
		return status;
	}

	@Property(viewable = true, order = 5)
	public Timestamp getCreateDate() {
		return createDate;
	}

	@Property(order = 6)
	public Timestamp getLockDate() {
		return lockDate;
	}

	@Property(order = 7)
	public Timestamp getExpiryDate() {
		return expiryDate;
	}

	/**
	 * Passwords are never read from database. It is used to create/alter
	 * schema/user
	 * 
	 * @return password or null
	 */
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@Nullable
	@Override
	public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
		systemPrivCache.clearCache();
		return super.refreshObject(monitor);
	}

	static class SystemPrivCache extends JDBCObjectCache<OracleGrantee, OraclePrivSystem> {
		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OracleGrantee owner)
				throws SQLException {
			StringBuilder sql = new StringBuilder("SELECT * FROM DBA_SYS_PRIVS WHERE GRANTEE ");
			if (owner.getName().equalsIgnoreCase("SYS")) {
				sql.append("= ?");
			} else {
				sql.append("IN (SELECT GRANTED_ROLE FROM DBA_ROLE_PRIVS WHERE GRANTEE=?) ORDER BY PRIVILEGE");
			}
			JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
			dbStat.setString(1, owner.getName());
			return dbStat;
		}

		@Override
		protected OraclePrivSystem fetchObject(@NotNull JDBCSession session, @NotNull OracleGrantee owner,
				@NotNull JDBCResultSet resultSet) throws SQLException, DBException {
			return new OraclePrivSystem(owner, resultSet);
		}
	}

	@Association
	public Collection<OraclePrivSystem> getSystemPrivs(DBRProgressMonitor monitor) throws DBException {
		return systemPrivCache.getAllObjects(monitor, this);
	}

}
