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
import org.jkiss.dbeaver.ext.exasol.model.ExasolSchema;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.access.DBAPrivilege;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

public abstract class ExasolBaseTableGrant implements DBAPrivilege  {
	
	
	private ExasolDataSource dataSource;
	private Boolean alterAuth=false;
	private Boolean deleteAuth=false;
	private Boolean insertAuth=false;
	private Boolean referencesAuth=false;
	private Boolean selectAuth=false;
	private Boolean updateAuth=false;
	private Boolean executeAuth=false;
	private Boolean isPersted;
	private ExasolSchema schema;
	private String name;
	private DBSObject exasolGrantee;
	
	public  ExasolBaseTableGrant(ExasolDataSource dataSource, ResultSet resultSet, DBRProgressMonitor monitor,DBSObject exasolGrantee) throws DBException
	{
		this.dataSource = dataSource;
		this.exasolGrantee = exasolGrantee;
		String grants = JDBCUtils.safeGetString(resultSet, "PRIVS");
		this.schema = dataSource.getChild(monitor, JDBCUtils.safeGetString(resultSet, "OBJECT_SCHEMA"));
		this.name = JDBCUtils.safeGetString(resultSet, "OBJECT_NAME");
		
		for(String grant: CommonUtils.splitString(grants, '|'))
		{
			switch (grant) {
			case "ALTER":
				alterAuth=true;
				break;
			case "DELETE":
				deleteAuth=true;
				break;
			case "INSERT":
				insertAuth=true;
				break;
			case "UPDATE":
				updateAuth=true;
				break;
			case "SELECT":
				selectAuth=true;
				break;
			case "REFERENCES":
				alterAuth=true;
				break;
			case "EXECUTE":
				executeAuth=true;
			default:
				break;
			}
		}
		
		this.isPersted = true;
	}


    @Property(viewable = true, order = 10)
	public ExasolSchema getSchema()
	{
		return this.schema;
	}

    @Property(viewable = true, order = 40)
	public Boolean getAlterAuth()
	{
		return alterAuth;
	}


    @Property(viewable = true, order = 50)
	public Boolean getDeleteAuth()
	{
		return deleteAuth;
	}


    @Property(viewable = true, order = 60)
	public Boolean getInsertAuth()
	{
		return insertAuth;
	}


    @Property(viewable = true, order = 70)
	public Boolean getReferencesAuth()
	{
		return referencesAuth;
	}


    @Property(viewable = true, order = 80)
	public Boolean getSelectAuth()
	{
		return selectAuth;
	}


    @Property(viewable = true, order = 90)
	public Boolean getUpdateAuth()
	{
		return updateAuth;
	}
    
    public Boolean getExecuteAuth()
    {
    	return executeAuth;
    }


	@Override
	public DBPDataSource getDataSource()
	{
		// TODO Auto-generated method stub
		return this.dataSource;
	}

	@Override
    @Property(hidden = true)
	public String getName()
	{
		return exasolGrantee.getName();
	}
	
	@Override
	public boolean isPersisted()
	{
		return this.isPersted;
	}
	
	public String getObjectName()
	{
		return this.name;
	}
	
	@Override
    @Property(hidden = true)
	public String getDescription()
	{
		// No Description available
		return "";
	}
	
	@Override
	public ExasolDataSource getParentObject()
	{
		return this.dataSource;
	}

}
