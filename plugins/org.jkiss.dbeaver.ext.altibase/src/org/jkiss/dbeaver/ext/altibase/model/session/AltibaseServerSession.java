/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.altibase.model.session;

import org.jkiss.dbeaver.ext.altibase.AltibaseConstants;
import org.jkiss.dbeaver.ext.altibase.AltibaseUtils;
import org.jkiss.dbeaver.model.admin.sessions.AbstractServerSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Objects;

/**
 * Session
 */
public class AltibaseServerSession extends AbstractServerSession {

    public static final String CAT_SESSION = "Session";
    public static final String CAT_SQL = "SQL";
    public static final String CAT_WAIT = "Wait";
    
    private String sessionId;
    private String txId;
    private int stmtId;
    private String userName;
    private String sql;
    private String lockTarget;
    private String lockStatus;
    private String lockType;
    private String loginTime;
    private String loginIdleSince;
    private String autoCommit;
    private String sysdba;
    private String clientAppInfo;
    private String commName;
    private String clientType;
    private String clientProtocolVersion;
    private String clientPid;
    
    
    public AltibaseServerSession(ResultSet dbResult) {
        this.sessionId = JDBCUtils.safeGetString(dbResult, "session_id");
        this.txId = JDBCUtils.safeGetString(dbResult, "tx_id");
        this.stmtId = JDBCUtils.safeGetInt(dbResult, "stmt_id");
        this.userName = JDBCUtils.safeGetString(dbResult, "user_name");
        this.sql = JDBCUtils.safeGetString(dbResult, "sql");
        this.lockTarget = JDBCUtils.safeGetString(dbResult, "lock_target");
        this.lockStatus = JDBCUtils.safeGetString(dbResult, "lock_status");
        this.lockType = JDBCUtils.safeGetString(dbResult, "lock_type");
        this.loginTime = JDBCUtils.safeGetString(dbResult, "login_time");
        this.loginIdleSince = JDBCUtils.safeGetString(dbResult, "idle_since");
        this.autoCommit = JDBCUtils.safeGetString(dbResult, "autocommit");
        this.sysdba = JDBCUtils.safeGetString(dbResult, "sysda");
        this.clientAppInfo = JDBCUtils.safeGetString(dbResult, "client_app_info");
        this.commName = JDBCUtils.safeGetString(dbResult, "comm_name");
        this.clientType = JDBCUtils.safeGetString(dbResult, "client_type");
        this.clientProtocolVersion = JDBCUtils.safeGetString(dbResult, "client_protocol_version");
        this.clientPid = JDBCUtils.safeGetString(dbResult, "client_pid");
    }

    @Property(category = CAT_SESSION, viewable = true, order = 1)
    public String getSessionId() {
        return sessionId;
    }

    @Property(category = CAT_SESSION, viewable = true, order = 2)
    public String getTxId() {
        return txId;
    }

    @Property(category = CAT_SESSION, viewable = true, order = 3)
    public String getUserName() {
        return userName;
    }

    @Property(category = CAT_SQL, viewable = true, order = 4)
    public String getSql() {
        int effectiveLength = 10;
        String qry = (sql != null)? sql.trim():"";

        if (AltibaseUtils.isEmpty(qry) == false) {
            
            if (qry.length() > effectiveLength) {
                qry = sql.trim().substring(0, 7);
            }
            
            qry = qry.replaceAll("[\\n\\t]", " ");
            qry = qry.replaceAll("[ ]+",  " ");
        }
        
        return (qry.length() > 0? qry + "...":"");
    }

    @Property(category = CAT_WAIT, viewable = true, order = 5)
    public String getLockTarget() {
        return lockTarget;
    }

    @Property(category = CAT_WAIT, viewable = true, order = 6)
    public String getLockStatus() {
        return lockStatus;
    }

    @Property(category = CAT_WAIT, viewable = true, order = 7)
    public String getLockType() {
        return lockType;
    }

    @Property(category = CAT_SESSION, viewable = true, order = 8)
    public String getLoginTime() {
        return loginTime;
    }

    @Property(category = CAT_SESSION, viewable = true, order = 9)
    public String getIdleSince() {
        return loginIdleSince;
    }

    @Property(category = CAT_SESSION, viewable = true, order = 10)
    public String getIsAutocommit() {
        return autoCommit;
    }

    @Property(category = CAT_SESSION, viewable = true, order = 11)
    public String getIsSysdba() {
        return sysdba;
    }

    @Property(category = CAT_SESSION, viewable = true, order = 21)
    public String getClientAppInfo() {
        return clientAppInfo;
    }

    @Property(category = CAT_SESSION, viewable = true, order = 21)
    public String getCommName() {
        return commName;
    }
    
    @Property(category = CAT_SESSION, viewable = true, order = 21)
    public String getClientType() {
        return clientType;
    }
    
    @Property(category = CAT_SESSION, viewable = true, order = 21)
    public String getClientProtocolVersion() {
        return clientProtocolVersion;
    }
    
    @Property(category = CAT_SESSION, viewable = true, order = 21)
    public String getClientPID() {
        return clientPid;
    }
    
    @Override
    public String getActiveQuery() {
        return sql;
    }

    @Override
    public Object getActiveQueryId() {
        return stmtId;
    }

    @Override
    public String toString() {
        return sessionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AltibaseServerSession that = (AltibaseServerSession) o;
        return sessionId == that.sessionId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId);
    }
}
