/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.yashandb.model.session;

import java.sql.ResultSet;
import java.sql.Timestamp;

import org.jkiss.dbeaver.model.admin.sessions.AbstractServerSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

public class YashanDBServerSession extends AbstractServerSession {

	public static final String CAT_SESSION = "Session";
	public static final String CAT_SQL = "SQL";
	public static final String CAT_PROCESS = "Process";
	public static final String CAT_IO = "IO";
	public static final String CAT_WAIT = "Wait";

	private final long sid;
	private long serial;
	private String user;
	private String schema;
	private String type;
	private String status;
	private String sqlId;
	private String sql;
	private final String event;
	private Timestamp logonTime;
	private String server;
	private String remoteHost;
	private String remoteUser;
	private String remoteProgram;
	private String module;
	private final String action;
	private final String clientInfo;
	private final String process;

	public YashanDBServerSession(ResultSet dbResult) {
		this.sid = JDBCUtils.safeGetLong(dbResult, "SID");
		this.serial = JDBCUtils.safeGetLong(dbResult, "SERIAL#");
		this.user = JDBCUtils.safeGetString(dbResult, "USERNAME");
		this.schema = JDBCUtils.safeGetString(dbResult, "SCHEMANAME");
		this.type = JDBCUtils.safeGetString(dbResult, "TYPE");
		this.status = JDBCUtils.safeGetString(dbResult, "STATUS");
		this.sqlId = JDBCUtils.safeGetString(dbResult, "SQL_ID");
		this.sql = JDBCUtils.safeGetString(dbResult, "SQL_TEXT");
		this.logonTime = JDBCUtils.safeGetTimestamp(dbResult, "LOGON_TIME");
		this.server = JDBCUtils.safeGetString(dbResult, "SERVER");
		this.remoteHost = JDBCUtils.safeGetString(dbResult, "CLI_HOSTNAME");
		this.remoteUser = JDBCUtils.safeGetString(dbResult, "CLI_OSUSER");
		this.remoteProgram = JDBCUtils.safeGetString(dbResult, "CLI_PROGRAM");
		this.module = JDBCUtils.safeGetString(dbResult, "MODULE");
		this.action = JDBCUtils.safeGetString(dbResult, "ACTION");
		this.clientInfo = JDBCUtils.safeGetString(dbResult, "CLIENT_INFO");
		this.process = JDBCUtils.safeGetString(dbResult, "PROCESS");
		this.event = JDBCUtils.safeGetString(dbResult, "WAIT_EVENT");
	}

	@Property(category = CAT_SESSION, viewable = true, order = 1)
	public long getSid() {
		return sid;
	}

	@Property(category = CAT_SESSION, viewable = false, order = 2)
	public long getSerial() {
		return serial;
	}

	@Property(category = CAT_SESSION, viewable = true, order = 3)
	public String getUser() {
		return user;
	}

	@Property(category = CAT_SESSION, viewable = true, order = 4)
	public String getSchema() {
		return schema;
	}

	@Property(category = CAT_SESSION, viewable = true, order = 5)
	public String getType() {
		return type;
	}

	@Property(category = CAT_SESSION, viewable = true, order = 6)
	public String getStatus() {
		return status;
	}

	@Property(category = CAT_SESSION, order = 10)
	public Timestamp getLogonTime() {
		return logonTime;
	}

	@Property(category = CAT_SQL, order = 20)
	public String getSql() {
		return sql;
	}

	@Property(category = CAT_SQL, order = 21)
	public String getSqlId() {
		return sqlId;
	}

	@Property(category = CAT_PROCESS, viewable = true, order = 30)
	public String getServer() {
		return server;
	}

	@Property(category = CAT_PROCESS, viewable = true, order = 30)
	public String getRemoteHost() {
		return remoteHost;
	}

	@Property(category = CAT_PROCESS, viewable = true, order = 31)
	public String getRemoteUser() {
		return remoteUser;
	}

	@Property(category = CAT_PROCESS, viewable = true, order = 32)
	public String getRemoteProgram() {
		return remoteProgram;
	}

	@Property(category = CAT_PROCESS, viewable = false, order = 32)
	public String getModule() {
		return module;
	}

	@Property(category = CAT_PROCESS, viewable = false, order = 32)
	public String getAction() {
		return action;
	}

	@Property(category = CAT_PROCESS, viewable = false, order = 32)
	public String getClientInfo() {
		return clientInfo;
	}

	@Property(category = CAT_PROCESS, viewable = false, order = 32)
	public String getProcess() {
		return process;
	}

	@Property(category = CAT_WAIT, viewable = true, order = 41)
	public String getEvent() {
		return event;
	}

	@Override
	public String getActiveQuery() {
		return sql;
	}

	@Override
	public Object getActiveQueryId() {
		return sqlId;
	}

	@Override
	public String getSessionId() {
		return "'" + this.sid + "," + this.serial + "'";
	}

	@Override
	public String toString() {
		return sid + "," + serial;
	}
}
