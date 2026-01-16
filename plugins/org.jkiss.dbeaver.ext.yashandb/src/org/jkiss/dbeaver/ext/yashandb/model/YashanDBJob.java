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
package org.jkiss.dbeaver.ext.yashandb.model;

import java.sql.ResultSet;
import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.StringJoiner;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.utils.DurationFormat;
import org.jkiss.dbeaver.utils.DurationFormatter;
import org.jkiss.utils.CommonUtils;

public class YashanDBJob extends YashanDBSchemaObject implements YashanDBSourceObject {

	private final long job;
	private final String loginUser;
	private final String privilegedUser;
	private final String schemaUser;
	private final Date lastDate;
	private final Date thisDate;
	private final String nextDate;
	private final String interval;
	private final long totalTime;
	private final long failures;
	private final long instance;
	private final String action;
	private final boolean broken;

	public YashanDBJob(@NotNull YashanDBSchema schema, @NotNull ResultSet resultSet) {
		super(schema, String.valueOf(JDBCUtils.safeGetInt(resultSet, "JOB")), true);
		this.job = JDBCUtils.safeGetLong(resultSet, "JOB");
		this.loginUser = JDBCUtils.safeGetString(resultSet, "LOG_USER");
		this.privilegedUser = JDBCUtils.safeGetString(resultSet, "PRIV_USER");
		this.schemaUser = JDBCUtils.safeGetString(resultSet, "SCHEMA_USER");
		this.lastDate = JDBCUtils.safeGetTimestamp(resultSet, "LAST_DATE");
		this.thisDate = JDBCUtils.safeGetTimestamp(resultSet, "THIS_DATE");
		this.nextDate = JDBCUtils.safeGetString(resultSet, "NEXT_DATE");
		this.interval = JDBCUtils.safeGetString(resultSet, "INTERVAL");
		this.totalTime = JDBCUtils.safeGetLong(resultSet, "TOTAL_TIME");
		this.failures = JDBCUtils.safeGetLong(resultSet, "FAILURES");
		this.instance = JDBCUtils.safeGetLong(resultSet, "INSTANCE");
		this.action = JDBCUtils.safeGetString(resultSet, "WHAT");
		this.broken = JDBCUtils.safeGetBoolean(resultSet, "BROKEN", YashanDBConstants.Y);
	}

	public long getJob() {
		return job;
	}

	@Nullable
	public String getAction() {
		return action;
	}

	@Nullable
	@Property(viewable = true, order = 11)
	public String getLoginUser() {
		return loginUser;
	}

	@Nullable
	@Property(viewable = true, order = 12)
	public String getPrivilegedUser() {
		return privilegedUser;
	}

	@Nullable
	@Property(viewable = true, order = 13)
	public String getSchemaUser() {
		return schemaUser;
	}

	@Nullable
	@Property(viewable = true, order = 14)
	public Date getLastDate() {
		return lastDate;
	}

	@Property(viewable = true, order = 15)
	public Date getThisDate() {
		return thisDate;
	}

	@Nullable
	@Property(viewable = true, order = 16)
	public String getNextDate() {
		return broken ? null : nextDate;
	}

	@Nullable
	@Property(viewable = true, order = 17)
	public String getInterval() {
		return interval;
	}

	@Property(viewable = true, order = 18)
	public String getTotalTime() {
		return DurationFormatter.format(Duration.ofMillis(totalTime), DurationFormat.MEDIUM);
	}

	@Property(viewable = true, order = 19)
	public long getFailures() {
		return failures;
	}

	@Property(viewable = true, order = 20)
	public long getInstance() {
		return instance;
	}

	@Property(viewable = true, order = 21)
	public boolean isBroken() {
		return broken;
	}

	@Override
	public YashanDBSourceType getSourceType() {
		return YashanDBSourceType.JOB;
	}

	@Override
	public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
		if (job == 0) {
			return "";
		}
		final StringJoiner args = new StringJoiner(",\n\t");
		args.add("job => " + SQLUtils.quoteString(this, name));
		args.add("what => " + SQLUtils.quoteString(this, action));
		args.add("next_date => TO_DATE(%s)"
				.formatted(SQLUtils.quoteString(this, CommonUtils.escapeDisplayString(nextDate))));
		args.add("interval => " + SQLUtils.quoteString(this, CommonUtils.escapeDisplayString(interval)));
		String brokenStatement = String.format("DBMS_JOB.BROKEN(%s, %s);", job, broken ? "TRUE" : "FALSE");
		return """
				BEGIN
				  DBMS_JOB.SUBMIT(
				    %s
				  );
				  %s
				  COMMIT;
				END;
				""".formatted(args, brokenStatement);
	}

	@Override
	public void setObjectDefinitionText(String source) {
	}

	@NotNull
	@Override
	public DBSObjectState getObjectState() {
		return broken ? DBSObjectState.INVALID : DBSObjectState.NORMAL;
	}

	@Override
	public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException {
	}
}
