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
import java.util.List;

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

	private List<ExasolRoleGrant> roles;
	private List<ExasolTableGrant> tableGrants;
	private List<ExasolViewGrant> viewGrants;
	private List<ExasolScriptGrant> scriptGrants;
	private List<ExasolConnectionGrant> connectGrants;
	private List<ExasolSystemGrant> sysGrants;
	private ExasolRetrievePermissions retrievPermissions;

	public ExasolGrantee(ExasolDataSource dataSource, ResultSet resultSet)
	{
		this.dataSource = dataSource;
		if (resultSet != null) {
			this.persisted = true;
		} else {
			this.persisted = false;
		}
		retrievPermissions = new ExasolRetrievePermissions(dataSource);
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
		roles = null;
		tableGrants = null;
		scriptGrants = null;
		viewGrants = null;
		connectGrants = null;
		return this;
	}

	@Override
	public void setPersisted(boolean persisted)
	{
		this.persisted = persisted;
	}
	
	public List<ExasolSystemGrant> getSystemgrants(DBRProgressMonitor monitor) throws DBException
	{
		if (this.sysGrants != null) {
			return this.sysGrants;
		}
		if (!isPersisted()) {
			this.sysGrants = new ArrayList<ExasolSystemGrant>();
			return this.sysGrants;
		}
		
		this.sysGrants = this.retrievPermissions.getSystemGrants(this, monitor);
		return sysGrants;
	}
	
	public List<ExasolConnectionGrant> getConnections(DBRProgressMonitor monitor) throws DBException
	{
		if (this.connectGrants != null) {
			return this.connectGrants;
		}
		if (!isPersisted()) {
			this.connectGrants = new ArrayList<ExasolConnectionGrant>();
			return this.connectGrants;
		}
		
		this.connectGrants = this.retrievPermissions.getConnectionGrants(this, monitor);
		return connectGrants;
	}

	public List<ExasolRoleGrant> getRoles(DBRProgressMonitor monitor)
			throws DBException
	{
		if (this.roles != null) {
			return this.roles;
		}
		if (!isPersisted()) {
			this.roles = new ArrayList<ExasolRoleGrant>();
			return this.roles;
		}
		
		this.roles = this.retrievPermissions.getRoleGrants(this, monitor);
		
		return this.roles;
		

	}
	
	//
	// Retrieve Grants
	//
	public List<ExasolTableGrant> getTables(DBRProgressMonitor monitor) throws DBException
	{
		if (this.tableGrants != null) {
			return this.tableGrants;
		}
		if (!isPersisted()) {
			this.tableGrants = new ArrayList<ExasolTableGrant>();
			return this.tableGrants;
		}
		 this.tableGrants = this.retrievPermissions.getTableGrants(this, monitor);
		return this.tableGrants;
		
	}
	
	public List<ExasolViewGrant> getViews(DBRProgressMonitor monitor) throws DBException
	{
		if (this.viewGrants != null) {
			return this.viewGrants;
		}
		if (!isPersisted()) {
			this.viewGrants = new ArrayList<ExasolViewGrant>();
			return this.viewGrants;
		}
		
		this.viewGrants = this.retrievPermissions.getViewGrants(this, monitor);
		return this.viewGrants;
	}
		
	public List<ExasolScriptGrant> getProcedures(DBRProgressMonitor monitor) throws DBException
	{
		if (this.scriptGrants != null) {
			return this.scriptGrants;
		}
		if (!isPersisted()) {
			this.scriptGrants = new ArrayList<ExasolScriptGrant>();
			return this.scriptGrants;
		}
		
		this.scriptGrants = this.retrievPermissions.getScript(this, monitor);
		return this.scriptGrants;
	}
	

}
