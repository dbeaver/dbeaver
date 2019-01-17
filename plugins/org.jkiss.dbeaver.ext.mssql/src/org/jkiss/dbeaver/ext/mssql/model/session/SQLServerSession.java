/*
 * DBeaver - Universal Database Manager
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
package org.jkiss.dbeaver.ext.mssql.model.session;

import org.jkiss.dbeaver.model.admin.sessions.DBAServerSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.util.Date;

/**
 * SQL Server session
 */
public class SQLServerSession implements DBAServerSession {
    private static final String CAT_CLIENT = "Client";
    private static final String CAT_TIMING = "Timings";
    private static final String CAT_STATISTICS = "Statistics";

    private long id;

    private Date loginTime;
    private Date lastRequestStart;
    private Date lastRequestEnd;

    private String hostName;
    private String programName;
    private String hostPID;
    private String clientVersion;
    private String clientInterface;
    private String loginName;
    private String ntDomain;
    private String ntUserName;
    private String status;

    private long cpuTime;
    private long memoryUsage;
    private long totalScheduledTime;
    private long totalElapsedTime;

    private long readsNum;
    private long writesNum;

    private String language;
    private long rowCount;
    private String fileName;
    private String databaseName;

    private String sqlText;

    public SQLServerSession(ResultSet dbResult) {
        this.id = JDBCUtils.safeGetInt(dbResult, "session_id");

        loginTime = JDBCUtils.safeGetTimestamp(dbResult, "login_time");;
        lastRequestStart = JDBCUtils.safeGetTimestamp(dbResult, "last_request_start");
        lastRequestEnd = JDBCUtils.safeGetTimestamp(dbResult, "last_request_end");

        hostName = JDBCUtils.safeGetString(dbResult, "host_name");
        programName = JDBCUtils.safeGetString(dbResult, "program_name");
        hostPID = JDBCUtils.safeGetString(dbResult, "host_process_id");
        clientVersion = JDBCUtils.safeGetString(dbResult, "client_version");
        clientInterface = JDBCUtils.safeGetString(dbResult, "client_interface");
        loginName = JDBCUtils.safeGetString(dbResult, "login_name");
        ntDomain = JDBCUtils.safeGetString(dbResult, "nt_domain");
        ntUserName = JDBCUtils.safeGetString(dbResult, "nt_user_name");
        status = JDBCUtils.safeGetString(dbResult, "status");

        cpuTime = JDBCUtils.safeGetLong(dbResult, "cpu_time");
        memoryUsage = JDBCUtils.safeGetLong(dbResult, "memory_usage");
        totalScheduledTime = JDBCUtils.safeGetLong(dbResult, "total_scheduled_time");
        totalElapsedTime = JDBCUtils.safeGetLong(dbResult, "total_elapsed_time");

        readsNum = JDBCUtils.safeGetLong(dbResult, "reads");
        writesNum = JDBCUtils.safeGetLong(dbResult, "writes");

        language = JDBCUtils.safeGetString(dbResult, "language");
        rowCount = JDBCUtils.safeGetLong(dbResult, "row_count");
        fileName = JDBCUtils.safeGetString(dbResult, "file_name");
        databaseName = JDBCUtils.safeGetString(dbResult, "database_name");

        sqlText = JDBCUtils.safeGetString(dbResult, "sql_text");
    }

    @Property(viewable = true, order = 1)
    public long getId() {
        return id;
    }

    @Property(viewable = false, category = CAT_TIMING, order = 10)
    public Date getLoginTime() {
        return loginTime;
    }

    @Property(viewable = false, category = CAT_TIMING, order = 11)
    public Date getLastRequestStart() {
        return lastRequestStart;
    }

    @Property(viewable = false, category = CAT_TIMING, order = 12)
    public Date getLastRequestEnd() {
        return lastRequestEnd;
    }

    @Property(viewable = false, category = CAT_CLIENT, order = 20)
    public String getHostName() {
        return hostName;
    }

    @Property(viewable = true, category = CAT_CLIENT, order = 21)
    public String getProgramName() {
        return programName;
    }

    @Property(viewable = true, category = CAT_CLIENT, order = 22)
    public String getHostPID() {
        return hostPID;
    }

    @Property(viewable = false, category = CAT_CLIENT, order = 23)
    public String getClientVersion() {
        return clientVersion;
    }

    @Property(viewable = false, category = CAT_CLIENT, order = 24)
    public String getClientInterface() {
        return clientInterface;
    }

    @Property(viewable = true, category = CAT_CLIENT, order = 25)
    public String getLoginName() {
        return loginName;
    }

    @Property(viewable = true, category = CAT_CLIENT, order = 26)
    public String getNtDomain() {
        return ntDomain;
    }

    @Property(viewable = false, category = CAT_CLIENT, order = 27)
    public String getNtUserName() {
        return ntUserName;
    }

    @Property(viewable = true, order = 5)
    public String getStatus() {
        return status;
    }

    @Property(viewable = true, category = CAT_STATISTICS, order = 40)
    public long getCpuTime() {
        return cpuTime;
    }

    @Property(viewable = true, category = CAT_STATISTICS, order = 41)
    public long getMemoryUsage() {
        return memoryUsage;
    }

    @Property(viewable = false, category = CAT_STATISTICS, order = 42)
    public long getTotalScheduledTime() {
        return totalScheduledTime;
    }

    @Property(viewable = false, category = CAT_STATISTICS, order = 43)
    public long getTotalElapsedTime() {
        return totalElapsedTime;
    }

    @Property(viewable = true, category = CAT_STATISTICS, order = 44)
    public long getReadsNum() {
        return readsNum;
    }

    @Property(viewable = true, category = CAT_STATISTICS, order = 45)
    public long getWritesNum() {
        return writesNum;
    }

    @Property(viewable = false, category = CAT_STATISTICS, order = 46)
    public long getRowCount() {
        return rowCount;
    }

    @Property(viewable = false, category = CAT_CLIENT, order = 30)
    public String getFileName() {
        return fileName;
    }

    @Property(viewable = false, category = CAT_CLIENT, order = 31)
    public String getDatabaseName() {
        return databaseName;
    }

    @Property(viewable = false, category = CAT_CLIENT, order = 32)
    public String getLanguage() {
        return language;
    }

    @Override
    @Property(viewable = false, order = 9)
    public String getActiveQuery()
    {
        return sqlText;
    }

    @Override
    public String toString()
    {
        if (databaseName != null) {
            return id + "@" + databaseName;
        } else {
            return String.valueOf(id);
        }
    }
}
