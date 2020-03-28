/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.exasol.ExasolSysTablePrefix;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ExasolCurrentUserPrivileges {

    private static final Log LOG = Log.getLog(ExasolCurrentUserPrivileges.class);

	private static final String C_QUERY_DICTIONARY = "/*snapshot execution*/ SELECT CONNECTION_NAME FROM sys.EXA_DBA_CONNECTIONS WHERE false";
    private static final String C_MAJOR_VERSION = "/*snapshot execution*/ select TO_NUMBER(\"VALUE\") AS VERSION from \"$ODBCJDBC\".DB_METADATA WHERE name LIKE 'databaseMajorVersion'";
    private static final String C_MINOR_VERSION = "/*snapshot execution*/ select TO_NUMBER(\"VALUE\") AS VERSION from \"$ODBCJDBC\".DB_METADATA WHERE name LIKE 'databaseMinorVersion'";

    private final Boolean userHasDictionaryAccess; 
    private final Integer majorVersion;
    private final Integer minorVersion;



    public ExasolCurrentUserPrivileges(DBRProgressMonitor monitor,
                                       JDBCSession session, ExasolDataSource exasolDataSource) {
    	
    	userHasDictionaryAccess = ExasolCurrentUserPrivileges.verifyPriv(C_QUERY_DICTIONARY, session);

        majorVersion = queryVersion(C_MAJOR_VERSION, session);
        minorVersion = queryVersion(C_MINOR_VERSION, session);
        
    }

    public int getExasolVersion() {
        return majorVersion;
    }

    public Boolean getatLeastV5() {
        return majorVersion >= 5;
    }

    public Boolean getatLeastV6() {
        return majorVersion >= 6;
    }
    
    public Boolean getUserHasDictionaryAccess() {
    	return userHasDictionaryAccess;
    }
    
    public Integer getMajorVersion() {
    	return majorVersion;
    }
    
    public Integer getMinorVersion() {
    	return minorVersion;
    }
    
    public String getTablePrefix(ExasolSysTablePrefix fallback) {
    	if (userHasDictionaryAccess) {
    		return ExasolSysTablePrefix.DBA.toString();
    	}
    	else {
    		return fallback.toString();
    	}
    }
    
    private static Integer queryVersion(String sql, JDBCSession session) {
    	Integer version;
        try (JDBCPreparedStatement dbStat = session.prepareStatement(sql)) {
            try (ResultSet rs = dbStat.executeQuery()) {
                rs.next();
                version = JDBCUtils.safeGetInt(rs, "VERSION");
                return version;
            }
        } catch(SQLException e) {
        	LOG.error("Error extracting Exasol version: fallback to version 5");
        	version = 5;
        }
        return version;
    	
    }

    private static Boolean verifyPriv(String sql, JDBCSession session) {
        Boolean hasPriv;
        try {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(sql)) {
                try (ResultSet rs = dbStat.executeQuery()) {

                }
            }
            hasPriv = true;
        } catch (Exception e) {
            hasPriv = false;
        }
        return hasPriv;
    }
    
    public Boolean hasPriorityGroups()
    {
    	return getatLeastV6() && getMinorVersion() >= 1  && getMajorVersion() < 7;
    }
    
    public Boolean hasPasswortPolicy()
    {
    	return getatLeastV6() && getMinorVersion() >= 1  && getMajorVersion() >= 7;
    }
    
    public Boolean hasConsumerGroups()
    {
    	return getMajorVersion() >= 7;
    }


}
