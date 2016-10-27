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
import org.jkiss.dbeaver.ext.exasol.model.ExasolConnection;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.access.DBAPrivilege;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

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
		return dataSource.getConnection(VoidProgressMonitor.INSTANCE, connection);
	}
	
	@Property(viewable = true, order = 90)
	public Boolean getAdminOption()
	{
		return this.adminOption;
	}

	@Override
	@Property(hidden = true)
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
