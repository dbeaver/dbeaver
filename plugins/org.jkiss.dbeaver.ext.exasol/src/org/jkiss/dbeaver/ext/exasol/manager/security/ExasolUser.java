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
