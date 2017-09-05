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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ExasolCurrentUserPrivileges {

	private static final String C_CONNECTIONS = "SELECT CONNECTION_NAME FROM EXA_DBA_CONNECTIONS WHERE FALSE";
	private static final String C_USERS = "SELECT USER_NAME FROM EXA_DBA_USERS WHERE FALSE";
	private static final String C_OBJECT_PRIV = "SELECT OBJECT_NAME FROM EXA_DBA_OBJ_PRIVS WHERE FALSE";
	private static final String C_CONNECTION_PRIV = "SELECT GRANTEE FROM EXA_DBA_CONNECTION_PRIVS WHERE FALSE";
	private static final String C_ROLES = "SELECT ROLE_NAME FROM EXA_DBA_ROLES WHERE FALSE";
	private static final String C_ROLE_PRIVS = "SELECT ROLE_NAME FROM EXA_DBA_ROLE_PRIVS WHERE FALSE";
	private static final String C_VERSION = "select TO_NUMBER(\"VALUE\") AS VERSION from \"$ODBCJDBC\".DB_METADATA WHERE name LIKE 'databaseMajorVersion'";
	private static final String C_ALTER_USER = 
			"SELECT\r\n" + 
			"	CASE\r\n" + 
			"		WHEN SUM( ANZAHL )> 0 THEN TRUE\r\n" + 
			"		ELSE FALSE\r\n" + 
			"	END AS HAS_ALTER_USER\r\n" + 
			"FROM\r\n" + 
			"	(\r\n" + 
			"		SELECT\r\n" + 
			"			COUNT(*) AS ANZAHL\r\n" + 
			"		FROM\r\n" + 
			"			sys.EXA_ROLE_SYS_PRIVS\r\n" + 
			"		WHERE\r\n" + 
			"			PRIVILEGE = 'ALTER USER'\r\n" + 
			"	UNION ALL SELECT\r\n" + 
			"			COUNT(*) AS ANZAHL\r\n" + 
			"		FROM\r\n" + 
			"			SYS.EXA_USER_SYS_PRIVS\r\n" + 
			"		WHERE\r\n" + 
			"			PRIVILEGE = 'ALTER USER'\r\n" + 
			"	)";

	private final Boolean userIsAuthorizedForConnections;
	private final Boolean userIsAuthorizedForUsers;
	private final Boolean userIsAuthorizedForRoles;
	private final Boolean userIsAuthorizedForRolePrivs;
	private final Boolean userIsAuthorizedForObjectPrivs;
	private final Boolean userIsAuthorizedForConnectionPrivs;
	private final Boolean userIsAuthorizedForSystemPrivs;
	private final Boolean userIsAuthorizedForSessions;
	private final Boolean userHasAlterUserPriv;
	
	private final int ExasolVersion;
	

	public ExasolCurrentUserPrivileges(DBRProgressMonitor monitor,
			JDBCSession session, ExasolDataSource exasolDataSource)
			throws DBException
	{

		userIsAuthorizedForConnections = ExasolCurrentUserPrivileges.verifyPriv(C_CONNECTIONS, session);
		userIsAuthorizedForUsers = ExasolCurrentUserPrivileges.verifyPriv(C_USERS, session);
		userIsAuthorizedForRolePrivs = ExasolCurrentUserPrivileges.verifyPriv(C_ROLE_PRIVS, session);
		userIsAuthorizedForRoles = ExasolCurrentUserPrivileges.verifyPriv(C_ROLES, session);
		userIsAuthorizedForObjectPrivs = ExasolCurrentUserPrivileges.verifyPriv(C_OBJECT_PRIV, session);
		userIsAuthorizedForConnectionPrivs = ExasolCurrentUserPrivileges.verifyPriv(C_CONNECTION_PRIV, session);
		userIsAuthorizedForSystemPrivs = ExasolCurrentUserPrivileges.verifyPriv("SELECT GRANTEE,PRIVILEGE,ADMIN_OPTION FROM SYS.EXA_DBA_SYS_PRIVS WHERE FALSE", session);
		userIsAuthorizedForSessions = ExasolCurrentUserPrivileges.verifyPriv("SELECT * FROM SYS.EXA_DBA_SESSIONS", session);
		userHasAlterUserPriv = ExasolCurrentUserPrivileges.verifyPriv(C_ALTER_USER, session);
		
		JDBCPreparedStatement dbStat;
		try {
			dbStat = session.prepareStatement(C_VERSION);
			ResultSet rs = dbStat.executeQuery();
			rs.next();
			ExasolVersion = JDBCUtils.safeGetInt(rs, "VERSION");			
			rs.close();
			dbStat.close();
		} catch (SQLException e) {
			throw new DBException(e,exasolDataSource);
		}
	}
	
	public int getExasolVersion()
	{
		return ExasolVersion;
	}
	
	public Boolean getatLeastV5()
	{
		return ExasolVersion >= 5;
	}
	
	public Boolean getatLeastV6()
	{
		return ExasolVersion >= 6;
	}

	public Boolean getUserIsAuthorizedForRoles()
	{
		return userIsAuthorizedForRoles;
	}

	public Boolean getUserIsAuthorizedForRolePrivs()
	{
		return userIsAuthorizedForRolePrivs;
	}
	
	public Boolean getUserIsAuthorizedForSystemPrivs()
	{
		return userIsAuthorizedForSystemPrivs;
	}

	private static Boolean verifyPriv(String sql, JDBCSession session)
	{
		JDBCPreparedStatement dbStat;

		Boolean hasPriv;
		try {
			dbStat = session.prepareStatement(C_CONNECTIONS);
			ResultSet rs = dbStat.executeQuery();
			rs.close();
			dbStat.close();
			hasPriv = true;

		} catch (SQLException e) {
			hasPriv = false;
		}
		return hasPriv;
	}

	public Boolean getUserIsAuthorizedForConnections()
	{
		return userIsAuthorizedForConnections;
	}

	public Boolean getUserIsAuthorizedForUsers()
	{
		return userIsAuthorizedForUsers;
	}

	public Boolean getUserIsAuthorizedForObjectPrivs()
	{
		return userIsAuthorizedForObjectPrivs;
	}

	public Boolean getUserIsAuthorizedForConnectionPrivs()
	{
		return userIsAuthorizedForConnectionPrivs;
	}
	
	public Boolean isUserAuthorizedForSessions()
	{
		return userIsAuthorizedForSessions;
	}
	
	public Boolean UserHasAlterUserPriv()
	{
		return userHasAlterUserPriv;
	}
	

}
