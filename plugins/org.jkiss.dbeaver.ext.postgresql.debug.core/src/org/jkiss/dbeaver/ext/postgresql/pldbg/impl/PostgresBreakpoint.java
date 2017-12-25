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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.jkiss.dbeaver.ext.postgresql.pldbg.Breakpoint;
import org.jkiss.dbeaver.ext.postgresql.pldbg.DebugException;


/**
 * @author Andrey.Hitrin
 *
 */
@SuppressWarnings("nls")
public class PostgresBreakpoint implements Breakpoint {
	
	private SessionInfoPostgres sessionInfo;
	
	private DebugObjectPostgres obj;
	
	private final Connection connection;
	
	private long lineNo = -1;
	
	private int target;
	
	private static final String SQL_SET = "select * from pldbg_set_global_breakpoint(?sessionid, ?obj, ?line, ?target)";
	
	private boolean active = false;
	
	public PostgresBreakpoint(Connection connection,SessionInfoPostgres sessionInfo,DebugObjectPostgres obj, long lineNo, int target)
	{
		super();
		this.sessionInfo = sessionInfo;
		this.obj = obj;
		this.lineNo = lineNo;
		this.target = target;
		this.connection = connection;
	}

	@Override
	public DebugObjectPostgres getObj()
	{
		return obj;
	}

	@Override
	public long getLineNo()
	{
		return lineNo;
	}

	@Override
	public void activate() throws DebugException{
		
		if (! active) {
		
			 try (Statement stmt = connection.createStatement()) {
				 
				 stmt.executeQuery(SQL_SET.replaceAll("\\?sessionid",String.valueOf(target))
						                  .replaceAll("\\?obj",String.valueOf(obj.getID()))
						                  .replaceAll("\\?line",String.valueOf(lineNo))
						                  .replaceAll("\\?target",(sessionInfo == null ? "null" : String.valueOf(sessionInfo.pid)))
						           );
				 active = true;
			 
			 
		 }catch (SQLException e) {
		        throw new DebugException(e);
		 }
		
	   }
	}

	@Override
	public void drop() throws DebugException
	{
		// TODO Auto-generated method stub
		
	}
	
	

}
