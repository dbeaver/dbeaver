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
package org.jkiss.dbeaver.ext.exasol.model;

import java.sql.Date;
import java.sql.ResultSet;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class ExasolConnection
		implements DBPRefreshableObject, DBPSaveableObject {

	private ExasolDataSource dataSource;
	private String connectionName;
	private String connectionString;
	private String userName;
	private Date created;
	private String comment;
	private Boolean persisted;

	public ExasolConnection(ExasolDataSource dataSource, ResultSet dbResult)
	{
		this.dataSource = dataSource;
		if (dbResult != null) {
			this.persisted = true;
			this.connectionName = JDBCUtils.safeGetString(dbResult,"CONNECTION_NAME");
			this.connectionString = JDBCUtils.safeGetString(dbResult,"CONNECTION_STRING");
			this.created = JDBCUtils.safeGetDate(dbResult, "CREATED");
			this.comment = JDBCUtils.safeGetString(dbResult,"CONNECTION_COMMENT");
			this.userName = JDBCUtils.safeGetString(dbResult, "USER_NAME");
		} else {
			this.connectionName = "new connection";
			this.persisted = false;
		}

	}

	@Override
	@Property(viewable = true, order = 10)
	public String getName()
	{
		return this.connectionName;
	}

	@Property(viewable = true, order = 20)
	public String getConnectionString()
	{
		return this.connectionString;
	}

	@Property(viewable = true, order = 30)
	public Date getCreated()
	{
		return this.created;
	}

	@Property(viewable = true, order = 30)
	public String getUserName()
	{
		return this.userName;
	}

	@Override
	@Property(viewable = true, order = 50)
	public String getDescription()
	{
		return this.comment;
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

	@Override
	public boolean isPersisted()
	{
		return this.persisted;
	}

	@Override
	public void setPersisted(boolean persisted)
	{
		this.persisted = persisted;

	}

	@Override
	public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor)
			throws DBException
	{
		return this;
	}

}
