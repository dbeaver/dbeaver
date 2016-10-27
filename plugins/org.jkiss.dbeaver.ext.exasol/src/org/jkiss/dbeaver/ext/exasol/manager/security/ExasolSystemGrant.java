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

import java.sql.ResultSet;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.access.DBAPrivilege;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

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
	@Property(hidden=true)
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
