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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.yashandb.model.util.YashanDBUtils;
import org.jkiss.dbeaver.model.access.DBARole;
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

public class YashanDBRole extends YashanDBGrantee implements DBARole {

	private String name;
	private boolean sysMaintained;
	private final UserCache userCache = new UserCache();

	public YashanDBRole(YashanDBDataSource dataSource, ResultSet resultSet) {
		super(dataSource);
		this.name = JDBCUtils.safeGetString(resultSet, "ROLE");
		this.sysMaintained = JDBCUtils.safeGetBoolean(resultSet, "SYS_MAINTAINED", YashanDBConstants.YES);
	}

	@NotNull
	@Override
	@Property(viewable = true, order = 2)
	public String getName() {
		return name;
	}

	@Property(viewable = true, order = 3)
	public boolean isSysMaintained() {
		return sysMaintained;
	}

	@Association
	public Collection<YashanDBPrivUser> getUserPrivs(DBRProgressMonitor monitor) throws DBException {
		return userCache.getAllObjects(monitor, this);
	}

	@Nullable
	@Override
	public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
		userCache.clearCache();
		return super.refreshObject(monitor);
	}

	static class UserCache extends JDBCObjectCache<YashanDBRole, YashanDBPrivUser> {
		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull YashanDBRole owner)
				throws SQLException {
			final JDBCPreparedStatement dbStat = session
					.prepareStatement("SELECT * FROM " + YashanDBUtils.isAdminPriv(owner.getDataSource(), "ROLE_PRIVS")
							+ " WHERE GRANTED_ROLE=? ORDER BY GRANTEE");
			dbStat.setString(1, owner.getName());
			return dbStat;
		}

		@Override
		protected YashanDBPrivUser fetchObject(@NotNull JDBCSession session, @NotNull YashanDBRole owner,
				@NotNull JDBCResultSet resultSet) throws SQLException, DBException {
			return new YashanDBPrivUser(owner, resultSet);
		}
	}

}
