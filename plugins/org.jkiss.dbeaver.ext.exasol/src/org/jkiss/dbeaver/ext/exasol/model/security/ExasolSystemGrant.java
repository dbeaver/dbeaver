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
package org.jkiss.dbeaver.ext.exasol.model.security;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.access.DBAPrivilege;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;

public class ExasolSystemGrant implements DBAPrivilege {

	
	private ExasolDataSource dataSource;
	private Boolean adminOption;
	private String sysPrivilege;
	private Boolean isPersisted;
	private String grantee;

	public ExasolSystemGrant(ExasolDataSource dataSource, ResultSet resultSet) throws DBException
	{
		this.dataSource = dataSource;
		this.sysPrivilege = JDBCUtils.safeGetString(resultSet, "PRIVILEGE");
		this.adminOption = JDBCUtils.safeGetBoolean(resultSet, "ADMIN_OPTION");
		this.isPersisted = true;
		this.grantee = JDBCUtils.safeGetString(resultSet, "GRANTEE");
	}

	@Property(viewable = true, order = 10)
	public String getSystemPrivilege()
	{
		return this.sysPrivilege;
	}
	
	@Property(viewable = true, order = 20)
	public Boolean getAdminOption()
	{
		return this.adminOption;
	}
	
	@Override
	@Property(hidden=true, multiline = true)
	public String getDescription()
	{
		return "";
	}

	@Override
	public DBSObject getParentObject()
	{
		return dataSource.getContainer();
	}

	@Override
	public DBPDataSource getDataSource()
	{
		return dataSource;
	}

	@Override
	@Property(hidden=true)
	public String getName()
	{
		return grantee;
	}

	@Override
	public boolean isPersisted()
	{
		return isPersisted;
	}

}
