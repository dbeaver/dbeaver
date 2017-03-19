/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
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

import java.sql.Date;
import java.sql.ResultSet;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
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
	public ExasolDataSource getDataSource()
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
