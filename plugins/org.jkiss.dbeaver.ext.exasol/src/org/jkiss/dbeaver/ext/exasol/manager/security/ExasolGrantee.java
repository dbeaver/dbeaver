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
import java.util.ArrayList;
import java.util.Collection;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

public abstract class ExasolGrantee
		implements DBPSaveableObject, DBPRefreshableObject {
	
	private ExasolDataSource dataSource;
	private boolean persisted;


	public ExasolGrantee(ExasolDataSource dataSource, ResultSet resultSet)
	{
		this.dataSource = dataSource;
		if (resultSet != null) {
			this.persisted = true;
		} else {
			this.persisted = false;
		}
	}

	@Override
	public boolean isPersisted()
	{
		return this.persisted;
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
	public DBSObject refreshObject(DBRProgressMonitor monitor)
			throws DBException
	{
		return this;
	}

	@Override
	public void setPersisted(boolean persisted)
	{
		this.persisted = persisted;
	}
	
	public Collection<ExasolSystemGrant> getSystemgrants(DBRProgressMonitor monitor) throws DBException
	{
		Collection<ExasolSystemGrant> sysGrants = new ArrayList<>();
		
		for(ExasolSystemGrant grant: dataSource.getSystemGrants(monitor))
		{
			if (grant.getName().equals(this.getName()))
				sysGrants.add(grant);
		}
		return sysGrants;
	}
	
	public Collection<ExasolConnectionGrant> getConnections(DBRProgressMonitor monitor) throws DBException
	{
		Collection<ExasolConnectionGrant> conGrants = new ArrayList<>(); 
		for(ExasolConnectionGrant grant: this.dataSource.getConnectionGrants(monitor))
		{
			if (grant.getName().equals(this.getName()))
				conGrants.add(grant);
		}
		return conGrants;
			
		
	}

	public Collection<ExasolRoleGrant> getRoles(DBRProgressMonitor monitor)
			throws DBException
	{
		Collection<ExasolRoleGrant> roleGrants = new ArrayList<>();
		for (ExasolRoleGrant grant: this.dataSource.getRoleGrants(monitor))
		{
			if (grant.getName().equals(this.getName()))
				roleGrants.add(grant);
				
		}
		return roleGrants;
		

	}
	
	//
	// Retrieve Grants
	//
	public Collection<ExasolTableGrant> getTables(DBRProgressMonitor monitor) throws DBException
	{
		Collection<ExasolTableGrant> grants = new ArrayList<>();
		
		for(ExasolTableGrant grant: this.dataSource.getTableGrants(monitor))
		{
			if (grant.getName().equals(this.getName()))
			{
				grants.add(grant);
			}
		}
		return grants;
		
	}
	
	public Collection<ExasolViewGrant> getViews(DBRProgressMonitor monitor) throws DBException
	{
		Collection<ExasolViewGrant> grants = new ArrayList<>();
		
		for(ExasolViewGrant grant: this.dataSource.getViewGrants(monitor))
		{
			if (grant.getName().equals(this.getName()))
			{
				grants.add(grant);
			}
		}
		return grants;
		
	}

	public Collection<ExasolScriptGrant> getProcedures(DBRProgressMonitor monitor) throws DBException
	{
		Collection<ExasolScriptGrant> grants = new ArrayList<>();
		
		for(ExasolScriptGrant grant: this.dataSource.getScriptGrants(monitor))
		{
			if (grant.getName().equals(this.getName()))
			{
				grants.add(grant);
			}
		}
		return grants;
		
	}

	public Collection<ExasolSchemaGrant> getSchemas(DBRProgressMonitor monitor) throws DBException
	{
		Collection<ExasolSchemaGrant> grants = new ArrayList<>();
		
		for(ExasolSchemaGrant grant: this.dataSource.getSchemaGrants(monitor))
		{
			if (grant.getName().equals(this.getName()))
			{
				grants.add(grant);
			}
		}
		return grants;
		
	}

}
