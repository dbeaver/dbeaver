/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2017 Andrew Khitrin (ahitrin@gmail.com)
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

package org.jkiss.dbeaver.ext.postgresql.pldbg.impl;

import org.jkiss.dbeaver.ext.postgresql.pldbg.SessionInfo;

/**
 * @author Andrey.Hitrin
 *
 */
@SuppressWarnings("nls")
public class SessionInfoPostgres implements SessionInfo<Integer> {
	
	final int pid;
	final String user;
	final String application;
	final String state;
	final String query;
	
	@Override
	public Integer getID()
	{
		return pid;
	}

	@Override
	public boolean isDebugWait()
	{
		return false;
	}

	public SessionInfoPostgres(int pid, String user, String application,
			String state, String query)
	{
		super();
		this.pid = pid;
		this.user = user;
		this.application = application;
		this.state = state;
		this.query = query;
	}

	public int getPid()
	{
		return pid;
	}

	public String getUser()
	{
		return user;
	}

	public String getApplication()
	{
		return application;
	}

	public String getState()
	{
		return state;
	}

	public String getQuery()
	{
		return query;
	}

	@Override
	public String toString()
	{
		
		return "pid:" + String.valueOf(pid) + ", user: " + user + ", application: `" + application + "`, state: " +state+ ", query: " + query.replace('\n', '\\') ;
	}
	
	

}
