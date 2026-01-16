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
import java.sql.SQLException;
import java.util.Map;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.yashandb.model.source.YashanDBStatefulObject;
import org.jkiss.dbeaver.ext.yashandb.model.util.YashanDBUtils;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPScriptObjectExt;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectState;

public class YashanDBSchedulerJob extends YashanDBSchemaObject implements YashanDBStatefulObject, DBPScriptObjectExt {

	private static final String CAT_SETTINGS = "Settings";
	private static final String CAT_ADVANCED = "Advanced";

	private String owner;
	private String jobSubName;
	private String jobStyle;
	private String jobCreator;
	private String clientId;
	private String programOwner;
	private String programName;
	private String jobType;
	private String jobAction;
	private long numberOfArguments;
	private String scheduleOwner;
	private String scheduleName;
	private String scheduleType;
	private String startDate;
	private String repeatInterval;
	private String endDate;
	private String enabled;
	private String autoDrop;
	private String state;
	private long runCount;
	private long maxRuns;
	private long failureCount;
	private long maxFailures;
	private long retryCount;
	private String lastStartDate;
	private String lastRunDuration;
	private String nextRunDate;
	private String maxRunDuration;
	private String comments;

	enum JobState {
		DISABLED, SCHEDULED, RUNNING, BROKEN, FAILED, SUCCEEDED;
	}

	protected YashanDBSchedulerJob(YashanDBSchema schema, ResultSet dbResult) {
		super(schema, JDBCUtils.safeGetString(dbResult, "JOB_NAME"), true);

		owner = JDBCUtils.safeGetString(dbResult, "OWNER");
		jobSubName = JDBCUtils.safeGetString(dbResult, "JOB_SUBNAME");
		jobStyle = JDBCUtils.safeGetString(dbResult, "JOB_STYLE");
		jobCreator = JDBCUtils.safeGetString(dbResult, "JOB_CREATOR");
		clientId = JDBCUtils.safeGetString(dbResult, "CLIENT_ID");
		programOwner = JDBCUtils.safeGetString(dbResult, "PROGRAM_OWNER");
		programName = JDBCUtils.safeGetString(dbResult, "PROGRAM_NAME");
		jobType = JDBCUtils.safeGetString(dbResult, "JOB_TYPE");
		jobAction = JDBCUtils.safeGetString(dbResult, "JOB_ACTION");
		numberOfArguments = JDBCUtils.safeGetLong(dbResult, "NUMBER_OF_ARGUMENTS");
		scheduleOwner = JDBCUtils.safeGetString(dbResult, "SCHEDULE_OWNER");
		scheduleName = JDBCUtils.safeGetString(dbResult, "SCHEDULE_NAME");
		scheduleType = JDBCUtils.safeGetString(dbResult, "SCHEDULE_TYPE");
		startDate = JDBCUtils.safeGetString(dbResult, "START_DATE");
		repeatInterval = JDBCUtils.safeGetString(dbResult, "REPEAT_INTERVAL");
		endDate = JDBCUtils.safeGetString(dbResult, "END_DATE");
		enabled = JDBCUtils.safeGetString(dbResult, "ENABLED");
		autoDrop = JDBCUtils.safeGetString(dbResult, "AUTO_DROP");
		state = JDBCUtils.safeGetString(dbResult, "STATE");
		runCount = JDBCUtils.safeGetLong(dbResult, "RUN_COUNT");
		maxRuns = JDBCUtils.safeGetLong(dbResult, "MAX_RUNS");
		failureCount = JDBCUtils.safeGetLong(dbResult, "FAILURE_COUNT");
		maxFailures = JDBCUtils.safeGetLong(dbResult, "MAX_FAILURES");
		retryCount = JDBCUtils.safeGetLong(dbResult, "RETRY_COUNT");
		lastStartDate = JDBCUtils.safeGetString(dbResult, "LAST_START_DATE");
		lastRunDuration = JDBCUtils.safeGetString(dbResult, "LAST_RUN_DURATION");
		nextRunDate = JDBCUtils.safeGetString(dbResult, "NEXT_RUN_DATE");
		maxRunDuration = JDBCUtils.safeGetString(dbResult, "MAX_RUN_DURATION");
		comments = JDBCUtils.safeGetString(dbResult, "COMMENTS");
	}

	@Property(category = CAT_ADVANCED, viewable = false, order = 10)
	public String getOwner() {
		return owner;
	}

	@Property(viewable = true, order = 10)
	public String getJobSubName() {
		return jobSubName;
	}

	@Property(viewable = true, order = 11)
	public String getJobStyle() {
		return jobStyle;
	}

	@Property(category = CAT_ADVANCED, viewable = false, order = 12)
	public String getJobCreator() {
		return jobCreator;
	}

	@Property(category = CAT_ADVANCED, viewable = false, order = 13)
	public String getClientId() {
		return clientId;
	}

	@Property(category = CAT_ADVANCED, viewable = false, order = 15)
	public String getProgramOwner() {
		return programOwner;
	}

	@Property(viewable = false, order = 16)
	public String getProgramName() {
		return programName;
	}

	@Property(viewable = true, order = 17)
	public String getJobType() {
		return jobType;
	}

	@Property(category = CAT_SETTINGS, viewable = false, order = 18)
	public String getJobAction() {
		return jobAction;
	}

	@Property(category = CAT_SETTINGS, viewable = false, order = 19)
	public long getNumberOfArguments() {
		return numberOfArguments;
	}

	@Property(category = CAT_ADVANCED, viewable = false, order = 20)
	public String getScheduleOwner() {
		return scheduleOwner;
	}

	@Property(viewable = false, order = 21)
	public String getScheduleName() {
		return scheduleName;
	}

	@Property(viewable = true, order = 22)
	public String getScheduleType() {
		return scheduleType;
	}

	@Property(viewable = true, order = 23)
	public String getStartDate() {
		return startDate;
	}

	@Property(viewable = true, order = 24)
	public String getRepeatInterval() {
		return repeatInterval;
	}

	@Property(viewable = true, order = 32)
	public String getEndDate() {
		return endDate;
	}

	@Property(category = CAT_SETTINGS, viewable = false, order = 34)
	public String getEnabled() {
		return enabled;
	}

	@Property(category = CAT_SETTINGS, viewable = false, order = 35)
	public String getAutoDrop() {
		return autoDrop;
	}

	@Property(viewable = false, order = 37)
	public String getState() {
		return state;
	}

	@Property(category = DBConstants.CAT_STATISTICS, viewable = false, order = 39)
	public long getRunCount() {
		return runCount;
	}

	@Property(category = DBConstants.CAT_STATISTICS, viewable = false, order = 40)
	public long getMaxRuns() {
		return maxRuns;
	}

	@Property(category = DBConstants.CAT_STATISTICS, viewable = false, order = 41)
	public long getFailureCount() {
		return failureCount;
	}

	@Property(category = DBConstants.CAT_STATISTICS, viewable = false, order = 42)
	public long getMaxFailures() {
		return maxFailures;
	}

	@Property(category = DBConstants.CAT_STATISTICS, viewable = false, order = 43)
	public long getRetryCount() {
		return retryCount;
	}

	@Property(category = DBConstants.CAT_STATISTICS, viewable = false, order = 44)
	public String getLastStartDate() {
		return lastStartDate;
	}

	@Property(category = DBConstants.CAT_STATISTICS, viewable = false, order = 45)
	public String getLastRunDuration() {
		return lastRunDuration;
	}

	@Property(category = CAT_SETTINGS, viewable = false, order = 46)
	public String getNextRunDate() {
		return nextRunDate;
	}

	@Property(viewable = false, order = 48)
	public String getMaxRunDuration() {
		return maxRunDuration;
	}

	@Property(viewable = false, order = 200)
	@Nullable
	@Override
	public String getDescription() {
		return comments;
	}

	public DBSObjectState getObjectState() {
		DBSObjectState objectState = null;

		try {
			if (JobState.valueOf(state).equals(JobState.RUNNING)) {
				objectState = DBSObjectState.ACTIVE;
			} else if (JobState.valueOf(state).equals(JobState.BROKEN)) {
				objectState = DBSObjectState.INVALID;
			} else if (JobState.valueOf(state).equals(JobState.FAILED)) {
				objectState = DBSObjectState.INVALID;
			} else {
				objectState = DBSObjectState.NORMAL;
			}
		} catch (IllegalArgumentException e) {
			objectState = DBSObjectState.UNKNOWN;
		}

		return objectState;
	}

	public void refreshObjectState(DBRProgressMonitor monitor) {
		if (monitor != null) {
			monitor.beginTask("Load action for '" + this.getName() + "'...", 1);
			try (final JDBCSession session = DBUtils.openMetaSession(monitor, this,
					"Load action for " + YashanDBObjectType.JOB + " '" + this.getName() + "'")) {
				try (JDBCPreparedStatement dbStat = session.prepareStatement(
						"SELECT STATE FROM " + YashanDBUtils.isAdminPriv(getDataSource(), "SCHEDULER_JOBS")
								+ "WHERE OWNER=? AND JOB_NAME=? ")) {
					dbStat.setString(1, getOwner());
					dbStat.setString(2, getName());
					dbStat.setFetchSize(DBConstants.METADATA_FETCH_SIZE);
					try (JDBCResultSet dbResult = dbStat.executeQuery()) {
						StringBuilder jobState = null;
						int lineCount = 0;
						while (dbResult.next()) {
							if (monitor.isCanceled()) {
								break;
							}
							final String line = dbResult.getString(1);
							if (jobState == null) {
								jobState = new StringBuilder(15);
							}
							jobState.append(line);
							lineCount++;
							monitor.subTask("Line " + lineCount);
						}
						if (jobState != null) {
							state = jobState.toString();
						}
					}
				}
			} catch (Exception e) {
				monitor.subTask("Error refreshing job state " + e.getMessage());
			} finally {
				monitor.done();
			}
		}
	}

	@Override
	public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
		if (jobAction == null && monitor != null) {
			monitor.beginTask("Load action for '" + this.getName() + "'...", 1);
			try (final JDBCSession session = DBUtils.openMetaSession(monitor, this,
					"Load action for " + YashanDBObjectType.JOB + " '" + this.getName() + "'")) {
				try (JDBCPreparedStatement dbStat = session.prepareStatement(
						"SELECT JOB_ACTION FROM " + YashanDBUtils.isAdminPriv(getDataSource(), "SCHEDULER_JOBS")
								+ "WHERE OWNER=? AND JOB_NAME=? ")) {
					dbStat.setString(1, getOwner());
					dbStat.setString(2, getName());
					dbStat.setFetchSize(DBConstants.METADATA_FETCH_SIZE);
					try (JDBCResultSet dbResult = dbStat.executeQuery()) {
						StringBuilder action = null;
						int lineCount = 0;
						while (dbResult.next()) {
							if (monitor.isCanceled()) {
								break;
							}
							final String line = dbResult.getString(1);
							if (action == null) {
								action = new StringBuilder(4000);
							}
							action.append(line);
							lineCount++;
							monitor.subTask("Line " + lineCount);
						}
						if (action != null) {
							jobAction = action.toString();
						}
					}
				} catch (SQLException e) {
					throw new DBCException(e, session.getExecutionContext());
				}
			} finally {
				monitor.done();
			}
		}
		return jobAction;
	}

	@Override
	public String getExtendedDefinitionText(DBRProgressMonitor monitor) throws DBException {
		return null;
	}
}
