/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.exasol.model.lock;

import org.jkiss.dbeaver.model.admin.locks.DBAServerLockItem;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;
import java.sql.Timestamp;

public class ExasolLockItem implements DBAServerLockItem {

	private String sqlText;
	private String userName;
	private String host;
	private String osUser;
	private String osName;
	private String scopeSchema;
	private String status;
	private String client;
	private Integer resources;
	private String priority;
	private Timestamp loginTime;
	private String driver;
	private String activity;
	private String commandName;
	private String evaluation;
	private String lockType;
	
	
	@Property(viewable = true, order = 10)
	public String getLockType()
	{
		return lockType;
	}


	ExasolLockItem(ResultSet dbResult) {
		this.sqlText = JDBCUtils.safeGetString(dbResult, "SQL_TEXT");
		this.userName = JDBCUtils.safeGetString(dbResult, "USER_NAME");
		this.host = JDBCUtils.safeGetString(dbResult, "HOST");
		this.osUser = JDBCUtils.safeGetString(dbResult, "OS_USER");
		this.scopeSchema = JDBCUtils.safeGetString(dbResult, "SCOPE_SCHEMA");
		this.status = JDBCUtils.safeGetString(dbResult, "STATUS");
		this.client = JDBCUtils.safeGetString(dbResult, "CLIENT");
		this.resources = JDBCUtils.safeGetInteger(dbResult, "RESOURCES");
		this.priority = JDBCUtils.safeGetString(dbResult, "PRIORITY");
		this.loginTime = JDBCUtils.safeGetTimestamp(dbResult, "LOGIN_TIME");
		this.driver = JDBCUtils.safeGetString(dbResult, "DRIVER");
		this.activity = JDBCUtils.safeGetString(dbResult, "ACTIVITY");
		this.evaluation = JDBCUtils.safeGetString(dbResult, "EVALUATION");
		this.lockType = JDBCUtils.safeGetString(dbResult, "HAS_LOCKS");
	}
	

	
	@Property(viewable = true, order = 150)
	public String getSqlText()
	{
		return sqlText;
	}


	@Property(viewable = true, order = 20)
	public String getUserName()
	{
		return userName;
	}


	@Property(viewable = true, order = 140)
	public String getHost()
	{
		return host;
	}


	@Property(viewable = true, order = 30)
	public String getOsUser()
	{
		return osUser;
	}


	@Property(viewable = true, order = 130)
	public String getOsName()
	{
		return osName;
	}


	@Property(viewable = true, order = 120)
	public String getScopeSchema()
	{
		return scopeSchema;
	}


	@Property(viewable = true, order = 40)
	public String getStatus()
	{
		return status;
	}


	@Property(viewable = true, order = 50)
	public String getClient()
	{
		return client;
	}


	@Property(viewable = true, order = 110)
	public Integer getResources()
	{
		return resources;
	}


	@Property(viewable = true, order = 100)
	public String getPriority()
	{
		return priority;
	}


	@Property(viewable = true, order = 60)
	public Timestamp getLoginTime()
	{
		return loginTime;
	}


	public String getDriver()
	{
		return driver;
	}


	@Property(viewable = true, order = 90)
	public String getActivity()
	{
		return activity;
	}


	@Property(viewable = true, order = 70)
	public String getCommandName()
	{
		return commandName;
	}


	@Property(viewable = true, order = 80)
	public String getEvaluation()
	{
		return evaluation;
	}
	
	public Timestamp ltime()
	{
		return this.loginTime;
	}


}
