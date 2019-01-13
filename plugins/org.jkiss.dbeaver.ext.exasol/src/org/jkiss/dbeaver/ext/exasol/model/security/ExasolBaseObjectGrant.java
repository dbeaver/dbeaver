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
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.ext.exasol.model.ExasolSchema;
import org.jkiss.dbeaver.ext.exasol.tools.ExasolUtils;
import org.jkiss.dbeaver.model.access.DBAPrivilege;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;

public class ExasolBaseObjectGrant implements DBAPrivilege  {
	
	
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
	private String exasolGrantee;
	private ExasolTableObjectType type;
	
	
	public  ExasolBaseObjectGrant(ExasolDataSource dataSource, ResultSet resultSet) throws DBException
	{
		this.type = ExasolUtils.getTableObjectType(JDBCUtils.safeGetString(resultSet, "OBJECT_TYPE"));
		this.dataSource = dataSource;
		this.exasolGrantee = JDBCUtils.safeGetString(resultSet, "GRANTEE") ;
		String grants = JDBCUtils.safeGetString(resultSet, "PRIVS");
		if (type == ExasolTableObjectType.SCHEMA) 
		{
			this.schema = dataSource.getChild(new VoidProgressMonitor(), JDBCUtils.safeGetString(resultSet, "OBJECT_NAME"));
		} else {
			this.schema = dataSource.getChild(new VoidProgressMonitor(), JDBCUtils.safeGetString(resultSet, "OBJECT_SCHEMA"));
		}
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
				referencesAuth=true;
				break;
			case "EXECUTE":
				executeAuth=true;
			default:
				break;
			}
		}
		
		this.isPersted = true;
	}
	
	public ExasolBaseObjectGrant(ExasolBaseObjectGrant grant)
	{
		this.dataSource =  grant.getDataSource();
		this.exasolGrantee = grant.getGrantee();
		this.alterAuth = grant.getAlterAuth();
		this.deleteAuth = grant.getDeleteAuth();
		this.insertAuth = grant.getInsertAuth();
		this.updateAuth = grant.getUpdateAuth();
		this.executeAuth = grant.getExecuteAuth();
		this.referencesAuth = grant.getReferencesAuth();
		this.selectAuth = grant.getSelectAuth();
		this.type = grant.getType();
		this.name = grant.getObjectName();
		this.schema = grant.getSchema();
		this.isPersted = true;
		
	}
	
	public ExasolTableObjectType getType()
	{
		return this.type;
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
	public ExasolDataSource getDataSource()
	{
		return this.dataSource;
	}

	@Override
    @Property(hidden = true)
	public String getName()
	{
		return exasolGrantee;
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
    @Property(hidden = true, multiline = true)
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
	
	public String getGrantee()
	{
		return this.exasolGrantee;
	}

}
