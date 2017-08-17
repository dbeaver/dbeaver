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
package org.jkiss.dbeaver.ext.exasol.model;

import java.sql.Date;
import java.sql.ResultSet;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.tools.ExasolUtils;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class ExasolConnection
		implements DBPRefreshableObject, DBPNamedObject2, DBPSaveableObject, DBPScriptObject{

	private ExasolDataSource dataSource;
	private String connectionName;
	private String connectionString;
	private String userName;
	private String password="";
	private Date created;
	private String comment="";
	private Boolean persisted;

	
	public ExasolConnection(
	        ExasolDataSource dataSource,
	        String name,
	        String url,
            String comment,
	        String user,
	        String password
	        )
	{
	    this.persisted = false;
	    this.connectionName = name;
	    this.connectionString = url;
	    this.comment = comment;
	    this.userName = user;
	    this.password = password;
	    this.dataSource = dataSource;
	}
	
	public ExasolConnection(ExasolDataSource dataSource, ResultSet dbResult)
	{
		this.dataSource = dataSource;
		if (dbResult != null) {
			this.persisted = true;
			this.connectionName = JDBCUtils.safeGetString(dbResult,"CONNECTION_NAME");
			this.connectionString = JDBCUtils.safeGetString(dbResult,"CONNECTION_STRING");
			this.created = JDBCUtils.safeGetDate(dbResult, "CREATED");
			this.comment = JDBCUtils.safeGetString(dbResult,"CONNECTION_COMMENT");
			this.userName = JDBCUtils.safeGetString(dbResult, "USER_NAME");
		} else {
			this.connectionName = "new connection";
			this.persisted = false;
			this.password = "";
		}

	}

	@Override
	@Property(viewable = true, editable=true, order = 10)
	public String getName()
	{
		return this.connectionName;
	}
	
	@Override
	public void setName(String name)
	{
	    this.connectionName = name;
	}

	@Property(viewable = true,editable=true, updatable=true, order = 20)
	public String getConnectionString()
	{
		return this.connectionString;
	}
	
	public void setConnectionString(String url)
	{
	    this.connectionString = url;
	}

	@Property(viewable = true, order = 30)
	public Date getCreated()
	{
		return this.created;
	}

	@Property(viewable = true,editable=true, updatable=true, order = 30)
	public String getUserName()
	{
		return this.userName;
	}

   public void setUserName(String userName)
    {
        this.userName = userName;
    }

	@Override
	@Property(viewable = true, editable= true, updatable=true, order = 50)
	public String getDescription()
	{
		return this.comment;
	}
	
	public void setDescription(String comment)
	{
	    this.comment = comment;
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
	public boolean isPersisted()
	{
		return this.persisted;
	}

	@Override
	public void setPersisted(boolean persisted)
	{
		this.persisted = persisted;

	}

	@Override
	public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor)
			throws DBException
	{
        ((ExasolDataSource) getDataSource()).refreshObject(monitor);
		return this;
	}

	@Override
	public String getObjectDefinitionText(DBRProgressMonitor monitor)
			throws DBException
	{
		if (getDataSource().isAuthorizedForConnections()) {
			return ExasolUtils.getConnectionDdl(this, monitor);
		} else {
			return "User needs full access to dictionary or dba privilege to generate ddl for connections";
		}
	}
	
    @Property(viewable = true, editable= true, updatable=true, order = 35)
	public String getPassword()
	{
	    return password;
	}

    public void setPassword(String password) 
    {
        this.password = password;
    }
    
    @Override
    public String toString()
    {
    	return "Connection "+getName();
    }
}
