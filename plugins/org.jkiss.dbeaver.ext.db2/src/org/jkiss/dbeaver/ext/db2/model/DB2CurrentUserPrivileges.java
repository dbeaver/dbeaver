/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.db2.model;

import org.jkiss.dbeaver.Log;
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

    private static final Log LOG = Log.getLog(DB2CurrentUserPrivileges.class);

    private static final String SYSMON = "SYSMON";
    private static final String SYSMAINT = "SYSMAINT";
    private static final String SYSADM = "SYSADM";
    private static final String SYSCTRL = "SYSCTRL";
    private static final String DATAACCESS = "DATAACCESS";
    private static final String DBADM = "DBADM";
    private static final String SQLADM = "SQLADM";

    private static final String AUTH_APP = "T:SYSIBMADM.APPLICATIONS";
    private static final String AUTH_DBCFG = "T:SYSIBMADM.DBCFG";
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

        sb.append("SELECT DISTINCT 'T:' || TRIM(TABSCHEMA) || '.' ||TABNAME");
        sb.append("  FROM SYSCAT.TABAUTH");
        sb.append(" WHERE ((GRANTEETYPE = 'G' AND GRANTEE = 'PUBLIC') OR (GRANTEETYPE = 'U' AND GRANTEE = ?))");
        sb.append("   AND (");
        sb.append("        (TABSCHEMA = 'SYSIBMADM' AND TABNAME = 'APPLICATIONS' AND 'Y' IN (CONTROLAUTH,SELECTAUTH))");
        sb.append("     OR (TABSCHEMA = 'SYSIBMADM' AND TABNAME = 'DBCFG' AND 'Y' IN (CONTROLAUTH,SELECTAUTH))");
        sb.append("       )");
        sb.append(" WITH UR");
        SEL_OBJECTS = sb.toString();
    }

    private final List<String> listAuthorities;
    private final List<String> listObjectPrivileges;

    private final Boolean userIsAuthorisedForApplications;
    private final Boolean userIsAuthorisedForContainers;
    private final Boolean userIsAuthorisedForDBCFG;
    private final Boolean userIsAuthorisedForAdminister;

    // ------------------------
    // Constructors
    // ------------------------
    public DB2CurrentUserPrivileges(DBRProgressMonitor monitor, JDBCSession session, String currentAuthId,
        DB2DataSource db2DataSource) throws SQLException
    {
        // DF: There is no easy way to get this information from DB2 v9.1
        // WE consider the user has no system authorities
        listAuthorities = new ArrayList<>();
        if (db2DataSource.isAtLeastV9_5()) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(SEL_AUTHORITIES)) {
                dbStat.setString(1, currentAuthId);
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        listAuthorities.add(dbResult.getString(1));
                    }
                }
            }
        }

        listObjectPrivileges = new ArrayList<>();
        try (JDBCPreparedStatement dbStat = session.prepareStatement(SEL_OBJECTS)) {
            dbStat.setString(1, currentAuthId);
            dbStat.setString(2, currentAuthId);
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (dbResult.next()) {
                    listObjectPrivileges.add(dbResult.getString(1));
                }
            }
        }

        // Cache Authorities
        userIsAuthorisedForApplications = computeUserIsAuthorisedForApplications();
        userIsAuthorisedForDBCFG = computeUserIsAuthorisedForDBCFG();
        userIsAuthorisedForAdminister = userIsAuthorisedForApplications || userIsAuthorisedForDBCFG;

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

    public Boolean userIsAuthorisedForDBCFG()
    {
        return userIsAuthorisedForDBCFG;
    }

    public Boolean userIsAuthorisedForAdminister()
    {
        return userIsAuthorisedForAdminister;
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

    private Boolean computeUserIsAuthorisedForDBCFG()
    {
        // Must have one of DATAACCESS, DBADM, SQLADM or or SELECT on SYSIBMADM.APPLICATIONS or CONTROL on SYSIBMADM.APPLICATIONS
        if ((listAuthorities.contains(DATAACCESS)) || (listAuthorities.contains(DBADM)) || (listAuthorities.contains(SQLADM))) {
            return true;
        }
        if (listObjectPrivileges.contains(AUTH_DBCFG)) {
            return true;
        }

        LOG.debug("Current User is not authorized to see DB/DBM Configuration Parameters");
        return false;
    }

}
