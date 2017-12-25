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

import org.jkiss.dbeaver.ext.postgresql.pldbg.DebugException;
import org.jkiss.dbeaver.ext.postgresql.pldbg.DebugSession;
import org.jkiss.dbeaver.ext.postgresql.pldbg.control.DebugManager;

/**
 * @author Andrey.Hitrin
 *
 */
@SuppressWarnings("nls")
public class DebugManagerPostgres implements DebugManager<Integer,Integer> {
	
	private final Connection connection;
	
	private static final String SQL_SESSION = "select pid,usename,application_name,state,query from pg_stat_activity";
	
	private static final String SQL_OBJECT = "select  p.oid,p.proname,u.usename as owner,n.nspname, l.lanname as lang "+  
											 " from "+    
											 "	pg_catalog.pg_namespace n "+ 
											 " join pg_catalog.pg_proc p on p.pronamespace = n.oid "+ 
											 "	 join pg_user u on u.usesysid =   p.proowner "+
											 "	 join pg_language l on l.oid = p. prolang "+
											 "	where  "+
											 "   l.lanname = 'sql' "+
											 "	 and p.proname like '%?nameCtx%' "+
											 "	 and u.usename like '%?userCtx%' "+
											 "	order by  "+
											 "	 n.nspname,p.proname"; 
	
	private static final String SQL_PID = "select pg_backend_pid() pid";

	@Override
	public Integer getCurrent() throws DebugException
	{
		 try (Statement stmt = connection.createStatement()) {
			 
			 ResultSet rs = stmt.executeQuery(SQL_PID);
			 
			 return rs.getInt("pid");
			 
			 
		 } catch (SQLException e) {
		        throw new DebugException(e);
		   }
	}

	@Override
	public List<SessionInfoPostgres> getSessions() throws DebugException  
	{ 
		
		   try (Statement stmt = connection.createStatement()) {
			   
		        ResultSet rs = stmt.executeQuery(SQL_SESSION);
		        
		        List<SessionInfoPostgres> res = new  ArrayList<SessionInfoPostgres>();

		        while (rs.next()) {
		        	
		        	res.add(new SessionInfoPostgres(
		        			rs.getInt("pid") ,
		        			rs.getString("usename"), 
		        			rs.getString("application_name"),
		        			rs.getString("state"), 
		        			rs.getString("query")));
		        }
		        
		        return res;
		        
		    } catch (SQLException e) {
		        throw new DebugException(e);
		    }
		
	}



	/**
	 * @param connection
	 */
	public DebugManagerPostgres(Connection connection)
	{
		super();
		this.connection = connection;
	}

	@Override
	public List<DebugObjectPostgres> getObjects(String ownerCtx,
			String nameCtx) throws DebugException
	{
		   try (Statement stmt = connection.createStatement()) {
			   
			   
		        ResultSet rs = stmt.executeQuery(SQL_OBJECT.replaceAll("\\?nameCtx", nameCtx).replaceAll("\\?userCtx", ownerCtx).toLowerCase());
		        
		        List<DebugObjectPostgres> res = new  ArrayList<DebugObjectPostgres>();

		        while (rs.next()) {
		        	
		        	res.add(new DebugObjectPostgres(
		        			rs.getInt("oid") ,
		        			rs.getString("proname"), 
		        			rs.getString("owner"),
		        			rs.getString("nspname"), 
		        			rs.getString("lang")));

		        }
		        
		        return res;
		        
		    } catch (SQLException e) {
		        throw new DebugException(e);
		    }
	}

	@Override
	public DebugSession<SessionInfoPostgres, DebugObjectPostgres> getDebugSession(
			Integer id) throws DebugException
	{
		return null;
	}

	@Override
	public DebugSession<SessionInfoPostgres, DebugObjectPostgres> createDebugSession(
			Integer id) throws DebugException
	{
		return null;
	}

	@Override
	public boolean isSessionExists(Integer id)
	{
		// TODO Auto-generated method stub
		return false;
	}


	

}
