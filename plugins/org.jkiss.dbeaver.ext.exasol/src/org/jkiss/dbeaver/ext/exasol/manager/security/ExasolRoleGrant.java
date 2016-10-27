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
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class ExasolRoleGrant implements DBAPrivilege  {

	private Boolean adminOption;
	private String role;
	private ExasolDataSource dataSource;
	private String grantee;
	
	public ExasolRoleGrant(ExasolDataSource dataSource, ResultSet resultSet)
	{
		this.role = JDBCUtils.safeGetString(resultSet, "ROLE_NAME");
		this.grantee = JDBCUtils.safeGetString(resultSet, "GRANTEE");
		this.dataSource = dataSource;
		this.adminOption = JDBCUtils.safeGetBoolean(resultSet, "ADMIN_OPTION");
	}
	
	@Property(viewable = true, order = 10)
	public ExasolRole getRole() throws DBException
	{
		return dataSource.getRole(VoidProgressMonitor.INSTANCE, role);
	}
	
	@Property(viewable = true, order = 20)
	public Boolean getAdminOption()
	{
		return this.adminOption;
	}

	@Override
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
		// TODO Auto-generated method stub
		return false;
	}

}
