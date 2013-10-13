/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2013 Serge Rieder serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.db2.model.security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds and cache current user privileges and authorities
 * 
 * @author Denis Forveille
 */
public class DB2CurrentUserPrivileges {

    private static final Log LOG = LogFactory.getLog(DB2CurrentUserPrivileges.class);

    private static final String SYSMON = "SYSMON";
    private static final String SYSMAINT = "SYSMAINT";
    private static final String SYSADM = "SYSADM";
    private static final String SYSCTRL = "SYSCTRL";
    private static final String DATAACCESS = "DATAACCESS";
    private static final String DBADM = "DBADM";
    private static final String SQLADM = "SQLADM";
    private static final String AUTH_APP = "T:SYSIBMADM.APPLICATIONS";
    private static final String AUTH_CONTAINER = "R:SYSPROC.SNAP_GET_CONTAINER";

    private static final String SEL_AUTHORITIES;
    static {
        StringBuilder sb = new StringBuilder(256);
        sb.append("SELECT AUTHORITY");
        sb.append("  FROM TABLE (SYSPROC.AUTH_LIST_AUTHORITIES_FOR_AUTHID (?, 'U')) AS T ");
        sb.append(" WHERE 'Y' IN (D_USER,D_GROUP,D_PUBLIC,ROLE_USER,ROLE_GROUP,ROLE_PUBLIC,D_ROLE)");
        sb.append(" WITH UR");
        SEL_AUTHORITIES = sb.toString();
    }

    private static final String SEL_OBJECTS;
    static {
        StringBuilder sb = new StringBuilder(512);
        sb.append("SELECT 'R:' || TRIM(SCHEMA) || '.' || SPECIFICNAME");
        sb.append("  FROM SYSCAT.ROUTINEAUTH");
        sb.append(" WHERE ((GRANTEETYPE = 'G' AND GRANTEE = 'PUBLIC') OR (GRANTEETYPE = 'U' AND GRANTEE = ?)) ");
        sb.append("   AND (SCHEMA = 'SYSPROC' AND SPECIFICNAME = 'SNAP_GET_CONTAINER' AND EXECUTEAUTH IN ('Y','G'))");

        sb.append(" UNION ALL ");

        sb.append("SELECT 'T:' || TRIM(TABSCHEMA) || '.' ||TABNAME");
        sb.append("  FROM SYSCAT.TABAUTH");
        sb.append(" WHERE ((GRANTEETYPE = 'G' AND GRANTEE = 'PUBLIC') OR (GRANTEETYPE = 'U' AND GRANTEE = ?))");
        sb.append("   AND (TABSCHEMA = 'SYSIBMADM' AND TABNAME = 'APPLICATIONS' AND 'Y' IN (CONTROLAUTH,SELECTAUTH))");

        sb.append(" WITH UR");
        SEL_OBJECTS = sb.toString();

    }

    private final List<String> listAuthorities;
    private final List<String> listObjectPrivileges;

    private final Boolean userIsAuthorisedForApplications;
    private final Boolean userIsAuthorisedForContainers;

    // ------------------------
    // Constructors
    // ------------------------
    public DB2CurrentUserPrivileges(DBRProgressMonitor monitor, JDBCSession session, String currentAuthId) throws SQLException
    {
        LOG.debug("Get All User Authorities");

        listAuthorities = new ArrayList<String>();
        JDBCPreparedStatement dbStat = session.prepareStatement(SEL_AUTHORITIES);
        try {
            dbStat.setString(1, currentAuthId);
            JDBCResultSet dbResult = dbStat.executeQuery();
            try {
                while (dbResult.next()) {
                    listAuthorities.add(dbResult.getString(1));
                }
            } finally {
                dbResult.close();
            }
        } finally {
            dbStat.close();
        }

        listObjectPrivileges = new ArrayList<String>();
        dbStat = session.prepareStatement(SEL_OBJECTS);
        try {
            dbStat.setString(1, currentAuthId);
            dbStat.setString(2, currentAuthId);
            JDBCResultSet dbResult = dbStat.executeQuery();
            try {
                while (dbResult.next()) {
                    listObjectPrivileges.add(dbResult.getString(1));
                }
            } finally {
                dbResult.close();
            }
        } finally {
            dbStat.close();
        }

        // Cache Authorities
        userIsAuthorisedForApplications = computeUserIsAuthorisedForApplications();
        userIsAuthorisedForContainers = computeUserIsAuthorisedForContainers();
    }

    // ------------------------
    // Standard Getters
    // ------------------------

    public Boolean userIsAuthorisedForApplications()
    {
        return userIsAuthorisedForApplications;
    }

    public Boolean userIsAuthorisedForContainers()
    {
        return userIsAuthorisedForContainers;
    }

    // -------
    // Helpers
    // -------

    private Boolean computeUserIsAuthorisedForApplications()
    {
        // Must have one of SYSMON, SYSMAINT, SYSADM or SYSCTRL

        if ((listAuthorities.contains(SYSMON)) || (listAuthorities.contains(SYSMAINT)) || (listAuthorities.contains(SYSADM))
            || (listAuthorities.contains(SYSCTRL))) {

            // Plus one of DATAACCESS or DBADM or SQLADM or SELECT on SYSIBMADM.APPLICATIONS or CONTROL on SYSIBMADM.APPLICATIONS
            if ((listAuthorities.contains(DATAACCESS)) || (listAuthorities.contains(DBADM)) || (listAuthorities.contains(SQLADM))) {
                return true;
            }
            if (listObjectPrivileges.contains(AUTH_APP)) {
                return true;
            }

        }

        LOG.debug("Current User is not authorized to see Applications");
        return false;
    }

    private Boolean computeUserIsAuthorisedForContainers()
    {
        // Must have one of SYSMON, SYSMAINT, SYSADM or SYSCTRL
        if ((listAuthorities.contains(SYSMON)) || (listAuthorities.contains(SYSMAINT)) || (listAuthorities.contains(SYSADM))
            || (listAuthorities.contains(SYSCTRL))) {

            // Plus one of DATAACCESS or EXECUTE on SYSPROC.SNAP_GET_CONTAINER
            if (listAuthorities.contains(DATAACCESS)) {
                return true;
            }
            if (listObjectPrivileges.contains(AUTH_CONTAINER)) {
                return true;
            }
        }

        LOG.debug("Current User is not authorized to see Tablespaces Containers");
        return false;
    }
}
