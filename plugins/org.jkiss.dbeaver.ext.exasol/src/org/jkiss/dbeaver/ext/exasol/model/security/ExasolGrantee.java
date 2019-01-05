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

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.ext.exasol.model.ExasolPriorityGroup;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

public abstract class ExasolGrantee
		implements DBPSaveableObject, DBPRefreshableObject {
	
	private ExasolDataSource dataSource;
	private ExasolPriorityGroup priority;
	private boolean persisted;

	private static final Log log = Log.getLog(ExasolGrantee.class);
	


	public ExasolGrantee(ExasolDataSource dataSource, ResultSet resultSet)
	{
		this.dataSource = dataSource;
		if (resultSet != null) {
			this.persisted = true;
	        try {
				this.priority = dataSource.getPriorityGroup(new VoidProgressMonitor(), JDBCUtils.safeGetString(resultSet, "USER_PRIORITY"));
			} catch (DBException e) {
				this.priority = null;
			}
		} else {
			this.persisted = false;
		}
	}
	
	public ExasolGrantee(ExasolDataSource dataSource, Boolean persisted)
	{
		this.dataSource = dataSource;
		this.persisted = persisted;
	}
	
	public abstract String getName(); 

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
	public ExasolDataSource getDataSource()
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

    @Property(viewable = true, editable = true, updatable = true, order = 20, listProvider = PriorityListProvider.class)
    public ExasolPriorityGroup getPriority()
    {
    	return priority;
    }
    
    public void setPriority(ExasolPriorityGroup priority) {
		this.priority = priority;
	}
    
    
    
    public static class PriorityListProvider implements IPropertyValueListProvider<ExasolGrantee> {

		@Override
		public boolean allowCustomValue() {
			return false;
		}

		@Override
		public Object[] getPossibleValues(ExasolGrantee object) {
			ExasolDataSource dataSource = object.getDataSource();
			try {
				Collection<ExasolPriorityGroup> priorityGroups = dataSource.getPriorityGroups(new VoidProgressMonitor());
				return priorityGroups.toArray(new Object[priorityGroups.size()]);
			} catch (DBException e) {
				log.error(e);
				return new Object[0];
			}
		}
    	
    }
	
	
}
