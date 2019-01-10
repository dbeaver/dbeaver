/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.ext.exasol.model.ExasolConnection;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.access.DBAPrivilege;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;

public class ExasolConnectionGrant 
		implements DBAPrivilege {

	private Boolean adminOption;
	private String connection;
	private ExasolDataSource dataSource;
	private String grantee;
	private Boolean isPersisted;
	public ExasolConnectionGrant(ExasolDataSource dataSource,
			ResultSet dbResult) 
	{
		this.adminOption = JDBCUtils.safeGetBoolean(dbResult, "ADMIN_OPTION");
		this.connection = JDBCUtils.safeGetString(dbResult, "CONNECTION_NAME");
		this.grantee = JDBCUtils.safeGetString(dbResult, "GRANTEE");
		this.dataSource = dataSource;
		this.isPersisted = true;
	}
	
	@Property(viewable = true, order = 10)
	public ExasolConnection getConnection() throws DBException
	{
		return dataSource.getConnection(new VoidProgressMonitor(), connection);
	}
	
	@Property(viewable = true, order = 90)
	public Boolean getAdminOption()
	{
		return this.adminOption;
	}

	@Override
	@Property(hidden = true, multiline = true)
	public String getDescription()
	{
		return null;
	}

	@Override
	public DBSObject getParentObject()
	{
		return dataSource.getContainer();
	}

	@Override
	public DBPDataSource getDataSource()
	{
		return this.dataSource;
	}

	@Override
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
