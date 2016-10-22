/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.exasol.manager.security;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.access.DBAUser;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;
import java.sql.Timestamp;

public class ExasolUser extends ExasolGrantee
		implements DBAUser, DBPRefreshableObject, DBPSaveableObject {


	private ExasolDataSource dataSource;
	private String userName;
	private String description;
	private String dn;
	private String password;
	private String priority;
	private Timestamp created;


	public ExasolUser(ExasolDataSource dataSource, ResultSet resultSet)
	{
		super(dataSource, resultSet);
		this.dataSource = dataSource;
		if (resultSet != null) {
			this.userName = JDBCUtils.safeGetString(resultSet, "USER_NAME");
			this.description = JDBCUtils.safeGetString(resultSet,
					"USER_COMMENT");
			this.dn = JDBCUtils.safeGetString(resultSet, "DISTINGUISHED_NAME");
			this.password = JDBCUtils.safeGetString(resultSet, "PASSWORD");
			this.priority = JDBCUtils.safeGetString(resultSet, "USER_PRIORITY");
			this.created = JDBCUtils.safeGetTimestamp(resultSet, "CREATED");
		} else {
			this.userName = "user";
			this.description = "";
			this.dn = "";
			this.password = "";
			this.priority = "";
			this.created = null;
		}
	}

	@Override
	@Property(viewable = true, order = 100)
	public String getDescription()
	{
		return this.description;
	}

	@Property(viewable = true, order = 20)
	public String getPassword()
	{
		return this.password;
	}

	@Property(viewable = true, order = 30)
	public String getDn()
	{
		return this.dn;
	}

	@Property(viewable = true, order = 40)
	public String getPriority()
	{
		return this.priority;
	}

	@Property(viewable = true, order = 50)
	public Timestamp getCreated()
	{
		return this.created;
	}
	
	public void setDescription(String description)
	{
		this.description = description;
	}

	@Override
	public DBSObject getParentObject()
	{
		return this.dataSource.getContainer();
	}

	@Override
	public DBPDataSource getDataSource()
	{
		return this.dataSource;
	}

	@NotNull
	@Override
	@Property(viewable = true, order = 1)
	public String getName()
	{
		return this.userName;
	}

}
