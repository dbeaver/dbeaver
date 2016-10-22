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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class ExasolRetrievePermissions {
	
	public static final String C_TABLE_OBJECT_PRIVS = "SELECT " + 
														"	OBJECT_SCHEMA," + 
														"	OBJECT_NAME," + 
														"	GROUP_CONCAT(" + 
														"		DISTINCT PRIVILEGE" + 
														"	ORDER BY" + 
														"		OBJECT_SCHEMA," + 
														"		OBJECT_NAME" + 
														"		SEPARATOR '|'" + 
														"	) as PRIVS " + 
														" FROM" + 
														"	SYS.EXA_DBA_OBJ_PRIVS P" + 
														" WHERE" + 
														"	OBJECT_TYPE =? AND GRANTEE = ? " + 
														" GROUP BY" + 
														"	OBJECT_SCHEMA," + 
														"	OBJECT_NAME" 
														;
	
	public static final String C_CONNECTION_PRIVS = "SELECT c.*,P.ADMIN_OPTION FROM SYS.EXA_DBA_CONNECTION_PRIVS P "
			+ "INNER JOIN SYS.EXA_DBA_CONNECTIONS C on P.GRANTED_CONNECTION = C.CONNECTION_NAME  WHERE GRANTEE=?";
	public static final String C_SYS_PRIVS = "SELECT PRIVILEGE,ADMIN_OPTION FROM SYS.EXA_DBA_SYS_PRIVS WHERE GRANTEE=?";
	public static final String C_ROLES_PRIVS = "select r.*,p.ADMIN_OPTION from EXA_DBA_ROLES r INNER JOIN  EXA_DBA_ROLE_PRIVS p ON p.GRANTED_ROLE = r.ROLE_NAME WHERE GRANTEE = ?";

	private ExasolDataSource dataSource;
	
	public ExasolRetrievePermissions(ExasolDataSource dataSource)
	{
		this.dataSource = dataSource;
	}
	
	
	public List<ExasolSystemGrant> getSystemGrants(DBSObject grantee, DBRProgressMonitor monitor) throws DBException
	{
		JDBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Query System Privs");

		List<ExasolSystemGrant> grants = new ArrayList<ExasolSystemGrant>();
		try {
			JDBCPreparedStatement stmt = session.prepareStatement(C_SYS_PRIVS);
			stmt.setString(1, grantee.getName());
			
			ResultSet rs = stmt.executeQuery();
			while(rs.next()) 
			{
				grants.add(new ExasolSystemGrant(dataSource, rs, monitor));
			}
			rs.close();
			stmt.close();
			
			return grants;
			
		} catch (SQLException e) {
			throw new DBException(e,dataSource);
		}
	}
	
	public List<ExasolConnectionGrant> getConnectionGrants(DBSObject grantee,DBRProgressMonitor monitor) throws DBException
	{
		JDBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Query Connection Privs");

		List<ExasolConnectionGrant> grants = new ArrayList<ExasolConnectionGrant>();
		try {
			JDBCPreparedStatement stmt = session.prepareStatement(C_CONNECTION_PRIVS);
			stmt.setString(1, grantee.getName());
			
			ResultSet rs = stmt.executeQuery();
			while(rs.next()) 
			{
				grants.add(new ExasolConnectionGrant(dataSource, rs));
			}
			rs.close();
			stmt.close();
			
			return grants;
			
		} catch (SQLException e) {
			throw new DBException(e,dataSource);
		}
	}
	
	
	
	public List<ExasolTableGrant> getTableGrants(DBSObject grantee,DBRProgressMonitor monitor) throws DBException
	{
		JDBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Query Table Privs");

		List<ExasolTableGrant> grants = new ArrayList<ExasolTableGrant>();
		try {
			JDBCPreparedStatement stmt = session.prepareStatement(C_TABLE_OBJECT_PRIVS);
			stmt.setString(1, "TABLE");
			stmt.setString(2, grantee.getName());
			
			ResultSet rs = stmt.executeQuery();
			while(rs.next()) 
			{
				grants.add(new ExasolTableGrant(dataSource, rs, monitor, grantee));
			}
			rs.close();
			stmt.close();
			
			return grants;
			
		} catch (SQLException e) {
			throw new DBException(e,dataSource);
		}
	}
	
	public List<ExasolRoleGrant> getRoleGrants(DBSObject grantee, DBRProgressMonitor monitor) throws DBException
	{
		JDBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Query Role Privs");

		List<ExasolRoleGrant> grants = new ArrayList<ExasolRoleGrant>();
		try {
			JDBCPreparedStatement stmt = session.prepareStatement(C_ROLES_PRIVS);
			stmt.setString(1, grantee.getName());
			
			ResultSet rs = stmt.executeQuery();
			while(rs.next()) 
			{
				grants.add(new ExasolRoleGrant(dataSource, rs));
			}
			rs.close();
			stmt.close();
			
			return grants;
			
		} catch (SQLException e) {
			throw new DBException(e,dataSource);
		}
	}
	
	public List<ExasolViewGrant> getViewGrants(DBSObject grantee,DBRProgressMonitor monitor) throws DBException
	{
		JDBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Query View Privs");

		List<ExasolViewGrant> grants = new ArrayList<ExasolViewGrant>();
		try {
			JDBCPreparedStatement stmt = session.prepareStatement(C_TABLE_OBJECT_PRIVS);
			stmt.setString(1, "VIEW");
			stmt.setString(2, grantee.getName());
			
			ResultSet rs = stmt.executeQuery();
			while(rs.next()) 
			{
				grants.add(new ExasolViewGrant(dataSource, rs, monitor, grantee));
			}
			rs.close();
			stmt.close();
			
			return grants;
			
		} catch (SQLException e) {
			throw new DBException(e,dataSource);
		}
	}
	
	public List<ExasolScriptGrant> getScript(DBSObject grantee,DBRProgressMonitor monitor) throws DBException
	{
		JDBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Query Script Privs");

		List<ExasolScriptGrant> grants = new ArrayList<ExasolScriptGrant>();
		try {
			JDBCPreparedStatement stmt = session.prepareStatement(C_TABLE_OBJECT_PRIVS);
			stmt.setString(1, "SCRIPT");
			stmt.setString(2, grantee.getName());
			
			ResultSet rs = stmt.executeQuery();
			while(rs.next()) 
			{
				grants.add(new ExasolScriptGrant(dataSource, rs, monitor, grantee));
			}
			rs.close();
			stmt.close();
			
			return grants;
			
		} catch (SQLException e) {
			throw new DBException(e,dataSource);
		}
	}
	

}
