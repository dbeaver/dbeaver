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
package org.jkiss.dbeaver.ext.exasol.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class ExasolCurrentUserPrivileges {

	private static final String C_CONNECTIONS = "SELECT CONNECTION_NAME FROM EXA_DBA_CONNECTIONS WHERE FALSE";
	private static final String C_USERS = "SELECT USER_NAME FROM EXA_DBA_USERS WHERE FALSE";
	private static final String C_ROLES = "SELECT ROLE_NAME FROM EXA_DBA_ROLES WHERE FALSE";
	private static final String C_ROLE_PRIVS = "SELECT ROLE_NAME FROM EXA_DBA_ROLE_PRIVS WHERE FALSE";

	private final Boolean userIsAuthorizedForConnections;
	private final Boolean userIsAuthorizedForUsers;
	private final Boolean userIsAuthorizedForRoles;
	private final Boolean userIsAuthorizedForRolePrivs;

	public ExasolCurrentUserPrivileges(DBRProgressMonitor monitor,
			JDBCSession session, ExasolDataSource exasolDataSource)
			throws DBException
	{

		userIsAuthorizedForConnections = ExasolCurrentUserPrivileges.verifyPriv(C_CONNECTIONS, session);
		userIsAuthorizedForUsers = ExasolCurrentUserPrivileges.verifyPriv(C_USERS, session);
		userIsAuthorizedForRolePrivs = ExasolCurrentUserPrivileges.verifyPriv(C_ROLE_PRIVS, session);
		userIsAuthorizedForRoles = ExasolCurrentUserPrivileges.verifyPriv(C_ROLES, session);

	}
	
	public Boolean getUserIsAuthorizedForRoles()
	{
		return userIsAuthorizedForRoles;
	}

	public Boolean getUserIsAuthorizedForRolePrivs()
	{
		return userIsAuthorizedForRolePrivs;
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

}
