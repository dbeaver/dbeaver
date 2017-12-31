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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.jkiss.dbeaver.ext.postgresql.pldbg.Breakpoint;
import org.jkiss.dbeaver.ext.postgresql.pldbg.BreakpointProperties;
import org.jkiss.dbeaver.ext.postgresql.pldbg.DebugException;
import org.jkiss.dbeaver.ext.postgresql.pldbg.DebugSession;
import org.jkiss.dbeaver.ext.postgresql.pldbg.StackFrame;
import org.jkiss.dbeaver.ext.postgresql.pldbg.Variable;


/**
 * @author Andrey.Hitrin
 *
 */
@SuppressWarnings("nls")
public class DebugSessionPostgres implements DebugSession<SessionInfoPostgres, DebugObjectPostgres,Integer> {
	
	private final SessionInfoPostgres sessionManagerInfo;
	
	private final SessionInfoPostgres sessionTargetInfo;
	
	private final Connection connection;
	
	private boolean attached = false;
	
	private boolean waiting = false;
	
	private final String title;
	
	private final int sessionId;
	
	private static final String SQL_ATTACH = "select pldbg_wait_for_target(?sessionid)";
	
	private static final String SQL_LISTEN = "select pldbg_create_listener() as sessionid";
	
	private static final String SQL_GET_VARS = "select pldbg_create_listener() as sessionid";
	
	private static final int ATTACH_TIMEOUT = 300; // seconds
	
	private List<PostgresBreakpoint> breakpoints = new ArrayList<PostgresBreakpoint>(1); 
	

	private int listen() throws DebugException{		
		
		   try (Statement stmt = connection.createStatement()) {
			   
		        ResultSet rs = stmt.executeQuery(SQL_LISTEN);
			    
			    	   
		        if (rs.next()) {
		        	
		        	 connection.setClientInfo("ApplicationName", "Debug Mode : " + String.valueOf(sessionId));
		        	
		        	return rs.getInt("sessionid");
		        	
		        	
		        } else {
		        	
		        	throw new DebugException("Unable to create debug instance");
		        	
		        }
		        
		    } catch (SQLException e) {
		        throw new DebugException(e);
		    }		
		
	}

	
	public void attach() throws DebugException{		
		
		   try (Statement stmt = connection.createStatement()) {
			   
			   connection.setAutoCommit(false);
			   
			   connection.setClientInfo("ApplicationName", "Debugger wait for " + (sessionManagerInfo == null ? " breakpoint" : String.valueOf(sessionManagerInfo.pid)));
			   
			   connection.commit();
			   
			    waiting = true;
			   
		        stmt.executeQuery(SQL_ATTACH.replaceAll("\\?sessionid",String.valueOf(sessionId))); //FIXME add TIMEOUT
		        
		        attached = true;
		        
		        waiting = false;
		        
		    } catch (SQLException e) {
		    	attached = false;
		    	waiting = false;
		        throw new DebugException(e);
		    }		
		
	}

	public DebugSessionPostgres(SessionInfoPostgres sessionManagerInfo,SessionInfoPostgres sessionTargetInfo,Connection connection) throws DebugException
	{
		super();
		this.sessionManagerInfo = sessionManagerInfo;
		this.sessionTargetInfo = sessionTargetInfo;
		this.connection = connection;
		this.title = sessionManagerInfo.application;
		sessionId = listen();
	}

	@Override
	public SessionInfoPostgres getSessionInfo()
	{
		
		return sessionManagerInfo;
	}

	@Override
	public String getTitle()
	{
		return title;
	}

	@Override
	public List<PostgresBreakpoint> getBreakpoints()
	{
		
		return breakpoints;
	}

	@Override
	public Breakpoint setBreakpoint(DebugObjectPostgres obj,BreakpointProperties properties) throws DebugException
	{
		PostgresBreakpoint bp = new PostgresBreakpoint(this,obj,(BreakpointPropertiesPostgres) properties);
		breakpoints.add(bp);
		return bp;
	}

	@Override
	public void removeBreakpoint(Breakpoint bp)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void execContinue()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void execStepInto()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void execStepOver()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void abort()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<Variable<?>> getVarables(String ctx)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setVariableVal(Variable<?> variable, Object value)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<StackFrame> getStack()
	{
		// TODO Auto-generated method stub
		return null;
	}

	

	public Connection getConnection() {
		return connection;
	}


	@Override
	public String toString() {
		return "DebugSessionPostgres [connection=" + connection + ", attached=" + attached + ", title=" + title
				+ ", sessionId=" + sessionId + ", breakpoints=" + breakpoints + "ManagerSession=("+ sessionManagerInfo.toString() +") Session=("+ sessionTargetInfo.toString()+") "+"]";
	}


	@Override
	public Integer getSessionId() {
		return sessionId;
	}


	public boolean isAttached() {
		return attached;
	}


	public boolean isWaiting() {
		return waiting;
	}

	
	
}
