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

import java.sql.SQLException;
import java.sql.Statement;

import org.jkiss.dbeaver.ext.postgresql.pldbg.Breakpoint;
import org.jkiss.dbeaver.ext.postgresql.pldbg.DebugException;


@SuppressWarnings("nls")
public class PostgresBreakpoint implements Breakpoint {
	
	private final DebugObjectPostgres obj;
	
	private final DebugSessionPostgres session;
	
	private final BreakpointPropertiesPostgres properties;
	
	private static final String SQL_SET = "select * from pldbg_set_global_breakpoint(?sessionid, ?obj, ?line, ?target)";
	
	public PostgresBreakpoint(DebugSessionPostgres session,DebugObjectPostgres obj, BreakpointPropertiesPostgres properties) throws DebugException
	{
		if (session.isAttached()) {
			throw new DebugException("Unable create breakpoint on waiting session");
		}
		this.session = session;
		this.obj = obj;
	    this.properties = properties;
	    try (Statement stmt = session.getConnection().createStatement()) {
			   
			     stmt.executeQuery(SQL_SET.replaceAll("\\?sessionid",String.valueOf(session.getSessionId()))
		                  .replaceAll("\\?obj",String.valueOf(obj.getID()))
		                  .replaceAll("\\?line",properties.isOnStart() ? "-1" : String.valueOf(properties.getLineNo()))
		                  .replaceAll("\\?target",properties.isAll() ? "null" : String.valueOf(properties.getTargetId())));
		          
		        
		    } catch (SQLException e) {
		    	throw new DebugException(e);
		    }
			 	
	}

	@Override
	public DebugObjectPostgres getObj()
	{
		return obj;
	}


	@Override
	public void drop() throws DebugException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public BreakpointPropertiesPostgres getProperties() {		
		return properties;
	}

	@Override
	public String toString() {
		return "PostgresBreakpoint [obj=" + obj + ", session id =" + session.getSessionId() + ", properties=" + properties + "]";
	}
	
	

}
