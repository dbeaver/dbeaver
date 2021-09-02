/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.oceanbase.oracle.model.session;

import java.sql.ResultSet;
import java.util.Objects;

import org.jkiss.dbeaver.model.admin.sessions.AbstractServerSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

public class OceanbaseOracleServerSession extends AbstractServerSession {
	private final String CAT_SESSION = "Session";

	private long sid;
	private String user;
	private String tenant;
	private String host;
	private String db;
	private String command;
	private long time;
	private String state;
	private String info;
	private String ip;
	private int port;

	public OceanbaseOracleServerSession(ResultSet dbResult) {
		this.sid = JDBCUtils.safeGetLong(dbResult, "ID");
		this.user = JDBCUtils.safeGetString(dbResult, "USER");
		this.tenant = JDBCUtils.safeGetString(dbResult, "TENANT");
		this.host = JDBCUtils.safeGetString(dbResult, "HOST");
		this.db = JDBCUtils.safeGetString(dbResult, "DB");
		this.command = JDBCUtils.safeGetString(dbResult, "COMMAND");
		this.time = JDBCUtils.safeGetLong(dbResult, "time");
		this.state = JDBCUtils.safeGetString(dbResult, "STATE");
		this.info = JDBCUtils.safeGetString(dbResult, "INFO");
		this.ip = JDBCUtils.safeGetString(dbResult, "IP");
		this.port = JDBCUtils.safeGetInt(dbResult, "PORT");
	}

	@Override
	public String getActiveQuery() {
		return info;
	}

	@Override
	public String toString() {
		return sid + " - " + info;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		OceanbaseOracleServerSession that = (OceanbaseOracleServerSession) o;
		return sid == that.sid;
	}

	@Override
	public int hashCode() {
		return Objects.hash(sid);
	}

	@Property(category = CAT_SESSION, viewable = true, order = 1)
	public long getSid() {
		return sid;
	}

	@Property(category = CAT_SESSION, viewable = true, order = 2)
	public String getUser() {
		return user;
	}

	@Property(category = CAT_SESSION, viewable = true, order = 3)
	public String getTenant() {
		return tenant;
	}

	@Property(category = CAT_SESSION, viewable = true, order = 4)
	public String getHost() {
		return host;
	}

	@Property(category = CAT_SESSION, viewable = true, order = 5)
	public String getDb() {
		return db;
	}

	@Property(category = CAT_SESSION, viewable = true, order = 6)
	public String getCommand() {
		return command;
	}

	@Property(category = CAT_SESSION, viewable = true, order = 7)
	public long getTime() {
		return time;
	}

	@Property(category = CAT_SESSION, viewable = true, order = 8)
	public String getState() {
		return state;
	}

	@Property(category = CAT_SESSION, viewable = true, order = 9)
	public String getInfo() {
		return info;
	}

	@Property(category = CAT_SESSION, viewable = true, order = 10)
	public String getIp() {
		return ip;
	}

	@Property(category = CAT_SESSION, viewable = true, order = 11)
	public int getPort() {
		return port;
	}

}
