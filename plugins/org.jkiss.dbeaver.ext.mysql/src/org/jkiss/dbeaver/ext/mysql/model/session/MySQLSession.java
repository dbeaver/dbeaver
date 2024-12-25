/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mysql.model.session;

import org.jkiss.dbeaver.model.admin.sessions.AbstractServerSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.IPropertyValueValidator;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.util.Map;
import java.util.Objects;

/**
 * MySQL session
 */
public class MySQLSession extends AbstractServerSession {

    static final String CAT_PERFORMANCE = "Performance";

    private final long pid;
    private String user;
    private String host;
    private final String db;
    private String command;
    private long time;
    private String state;
    private String info;

    private String statementLatency;
    private String lockLatency;
    private long rowsExamined;
    private long rowsSent;
    private long rowsAffected;
    private long tmpTables;
    private long tmpDiskTables;
    private String fullScan;
    private String lastStatement;
    private String lastStatementLatency;
    private String currentMemory;
    private String trxLatency;
    private String trxState;
    private String trxAutocommit;
    private long progress;
    private String source;
    private String programName;

    private boolean readPerformanceStats;

    public MySQLSession(ResultSet dbResult, Map<String, Object> options) {
        this.pid = JDBCUtils.safeGetLong(dbResult, "id");
        this.user = JDBCUtils.safeGetString(dbResult, "user");
        this.host = JDBCUtils.safeGetString(dbResult, "host");
        this.db = JDBCUtils.safeGetString(dbResult, "db");
        this.command = JDBCUtils.safeGetString(dbResult, "command");
        this.time = JDBCUtils.safeGetLong(dbResult, "time");
        this.state = JDBCUtils.safeGetString(dbResult, "state");
        this.info = JDBCUtils.safeGetString(dbResult, "info");
        this.readPerformanceStats = CommonUtils.getOption(options, MySQLSessionManager.OPTION_SHOW_PERFORMANCE);
        if (readPerformanceStats) {
            this.statementLatency = JDBCUtils.safeGetString(dbResult, "statement_latency");
            this.lockLatency = JDBCUtils.safeGetString(dbResult, "lock_latency");
            this.rowsExamined = JDBCUtils.safeGetLong(dbResult, "rows_examined");
            this.rowsSent = JDBCUtils.safeGetLong(dbResult, "rows_sent");
            this.rowsAffected = JDBCUtils.safeGetLong(dbResult, "rows_affected");
            this.tmpTables = JDBCUtils.safeGetLong(dbResult, "tmp_tables");
            this.tmpDiskTables = JDBCUtils.safeGetLong(dbResult, "tmp_disk_tables");
            this.fullScan = JDBCUtils.safeGetString(dbResult, "full_scan");
            this.lastStatement = JDBCUtils.safeGetString(dbResult, "last_statement");
            this.lastStatementLatency = JDBCUtils.safeGetString(dbResult, "last_statement_latency");
            this.currentMemory = JDBCUtils.safeGetString(dbResult, "current_memory");
            this.trxLatency = JDBCUtils.safeGetString(dbResult, "trx_latency");
            this.trxState = JDBCUtils.safeGetString(dbResult, "trx_state");
            this.trxAutocommit = JDBCUtils.safeGetString(dbResult, "trx_autocommit");
            this.progress = JDBCUtils.safeGetLong(dbResult, "progress");
            this.source = JDBCUtils.safeGetString(dbResult, "source");
            this.programName = JDBCUtils.safeGetString(dbResult, "program_name");
        }
    }

    @Property(viewable = true, order = 1)
    public long getPid()
    {
        return pid;
    }

    @Property(viewable = true, order = 2)
    public String getUser()
    {
        return user;
    }

    @Property(viewable = true, order = 3)
    public String getHost()
    {
        return host;
    }

    @Property(viewable = true, order = 4)
    public String getDb()
    {
        return db;
    }

    @Property(viewable = true, order = 5)
    public String getCommand()
    {
        return command;
    }

    @Property(viewable = true, order = 6)
    public long getTime()
    {
        return time;
    }

    @Property(viewable = true, order = 7)
    public String getState()
    {
        return state;
    }

    @Override
    @Property(viewable = true, order = 8)
    public String getActiveQuery()
    {
        return info;
    }

    @Override
    public String getSessionId() {
        return String.valueOf(pid);
    }

    @Property(category = CAT_PERFORMANCE, visibleIf = PerformanceReadingValueValidator.class, order = 9)
    public String getStatementLatency() {
        return statementLatency;
    }

    @Property(category = CAT_PERFORMANCE, visibleIf = PerformanceReadingValueValidator.class, order = 10)
    public String getLockLatency() {
        return lockLatency;
    }

    @Property(category = CAT_PERFORMANCE, visibleIf = PerformanceReadingValueValidator.class, order = 11)
    public long getRowsExamined() {
        return rowsExamined;
    }

    @Property(category = CAT_PERFORMANCE, visibleIf = PerformanceReadingValueValidator.class, order = 12)
    public long getRowsSent() {
        return rowsSent;
    }

    @Property(category = CAT_PERFORMANCE, visibleIf = PerformanceReadingValueValidator.class, order = 13)
    public long getRowsAffected() {
        return rowsAffected;
    }

    @Property(category = CAT_PERFORMANCE, visibleIf = PerformanceReadingValueValidator.class, order = 14)
    public long getTmpTables() {
        return tmpTables;
    }

    @Property(category = CAT_PERFORMANCE, visibleIf = PerformanceReadingValueValidator.class, order = 15)
    public long getTmpDiskTables() {
        return tmpDiskTables;
    }

    @Property(category = CAT_PERFORMANCE, visibleIf = PerformanceReadingValueValidator.class, order = 16)
    public String getFullScan() {
        return fullScan;
    }

    @Property(category = CAT_PERFORMANCE, visibleIf = PerformanceReadingValueValidator.class, order = 17)
    public String getLastStatement() {
        return lastStatement;
    }

    @Property(category = CAT_PERFORMANCE, visibleIf = PerformanceReadingValueValidator.class, order = 18)
    public String getLastStatementLatency() {
        return lastStatementLatency;
    }

    @Property(category = CAT_PERFORMANCE, visibleIf = PerformanceReadingValueValidator.class, order = 19)
    public String getCurrentMemory() {
        return currentMemory;
    }

    @Property(category = CAT_PERFORMANCE, visibleIf = PerformanceReadingValueValidator.class, order = 20)
    public String getTrxLatency() {
        return trxLatency;
    }

    @Property(category = CAT_PERFORMANCE, visibleIf = PerformanceReadingValueValidator.class, order = 21)
    public String getTrxState() {
        return trxState;
    }

    @Property(category = CAT_PERFORMANCE, visibleIf = PerformanceReadingValueValidator.class, order = 22)
    public String getTrxAutocommit() {
        return trxAutocommit;
    }

    @Property(category = CAT_PERFORMANCE, visibleIf = PerformanceReadingValueValidator.class, order = 23)
    public long getProgress() {
        return progress;
    }

    @Property(category = CAT_PERFORMANCE, visibleIf = PerformanceReadingValueValidator.class, order = 24)
    public String getSource() {
        return source;
    }

    @Property(category = CAT_PERFORMANCE, visibleIf = PerformanceReadingValueValidator.class, order = 25)
    public String getProgramName() {
        return programName;
    }

    private boolean isReadPerformanceStats() {
        return readPerformanceStats;
    }

    @Override
    public String toString()
    {
        return pid + "@" + db;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MySQLSession that = (MySQLSession) o;
        return pid == that.pid && Objects.equals(db, that.db);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pid, db);
    }

    public static class PerformanceReadingValueValidator implements IPropertyValueValidator<MySQLSession, Object> {
        @Override
        public boolean isValidValue(MySQLSession session, Object value) throws IllegalArgumentException {
            return session.isReadPerformanceStats();
        }
    }
}
