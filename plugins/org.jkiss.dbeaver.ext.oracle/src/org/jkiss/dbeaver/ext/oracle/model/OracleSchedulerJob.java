/*
 * DBeaver - Universal Database Manager
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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.source.OracleStatefulObject;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPScriptObjectExt;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectState;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

/**
 * Oracle scheduler job
 */
public class OracleSchedulerJob extends OracleSchemaObject implements OracleStatefulObject, DBPScriptObjectExt {

    private static final String CAT_SETTINGS = "Settings";
    private static final String CAT_STATISTICS = "Statistics";
    private static final String CAT_EVENTS = "Events";
    private static final String CAT_ADVANCED = "Advanced";

    private String owner;
    private String jobSubName;
    private String jobStyle;
    private String jobCreator;
    private String clientId;
    private String globalUid;
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
    private String eventQueueOwner;
    private String eventQueueName;
    private String eventQueueAgent;
    private String eventCondition;
    private String eventRule;
    private String fileWatcherOwner;
    private String fileWatcherName;
    private String endDate;

    private String jobClass;
    private String enabled;
    private String autoDrop;
    private String restartable;
    private String state;
    private int jobPriority;
    private long runCount;
    private long maxRuns;
    private long failureCount;
    private long maxFailures;
    private long retryCount;
    private String lastStartDate;
    private String lastRunDuration;
    private String nextRunDate;
    private String scheduleLimit;
    private String maxRunDuration;
    private String loggingLevel;
    private String stopOnWindowClose;
    private String instanceStickiness;
    private String raiseEvents;
    private String system;
    private String jobWeight;
    private String nlsEnv;
    private String source;
    private String numberOfDestinations;
    private String destinationOwner;
    private String destination;
    private String credentialOwner;
    private String credentialName;
    private String instanceId;
    private String deferredDrop;
    private String allowRunsInRestrictedMode;
    private String comments;

    private final ArgumentsCache argumentsCache = new ArgumentsCache();

    enum JobState {
    	DISABLED,
    	RETRYSCHEDULED,
    	SCHEDULED,
    	RUNNING,
    	COMPLETED,
    	BROKEN,
    	FAILED,
    	REMOTE,
    	SUCCEEDED,
    	CHAIN_STALLED;
    }

    protected OracleSchedulerJob(OracleSchema schema, ResultSet dbResult) {
        super(schema, JDBCUtils.safeGetString(dbResult, "JOB_NAME"), true);

        owner = JDBCUtils.safeGetString(dbResult, "OWNER");
        jobSubName = JDBCUtils.safeGetString(dbResult, "JOB_SUBNAME");
        jobStyle = JDBCUtils.safeGetString(dbResult, "JOB_STYLE");
        jobCreator = JDBCUtils.safeGetString(dbResult, "JOB_CREATOR");
        clientId = JDBCUtils.safeGetString(dbResult, "CLIENT_ID");
        globalUid = JDBCUtils.safeGetString(dbResult, "GLOBAL_UID");
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
        eventQueueOwner = JDBCUtils.safeGetString(dbResult, "EVENT_QUEUE_OWNER");
        eventQueueName = JDBCUtils.safeGetString(dbResult, "EVENT_QUEUE_NAME");
        eventQueueAgent = JDBCUtils.safeGetString(dbResult, "EVENT_QUEUE_AGENT");
        eventCondition = JDBCUtils.safeGetString(dbResult, "EVENT_CONDITION");
        eventRule = JDBCUtils.safeGetString(dbResult, "EVENT_RULE");
        fileWatcherOwner = JDBCUtils.safeGetString(dbResult, "FILE_WATCHER_OWNER");
        fileWatcherName = JDBCUtils.safeGetString(dbResult, "FILE_WATCHER_NAME");
        endDate = JDBCUtils.safeGetString(dbResult, "END_DATE");
        jobClass = JDBCUtils.safeGetString(dbResult, "JOB_CLASS");
        enabled = JDBCUtils.safeGetString(dbResult, "ENABLED");
        autoDrop = JDBCUtils.safeGetString(dbResult, "AUTO_DROP");
        restartable = JDBCUtils.safeGetString(dbResult, "RESTARTABLE");
        state = JDBCUtils.safeGetString(dbResult, "STATE");
        jobPriority = JDBCUtils.safeGetInt(dbResult, "JOB_PRIORITY");
        runCount = JDBCUtils.safeGetLong(dbResult, "RUN_COUNT");
        maxRuns = JDBCUtils.safeGetLong(dbResult, "MAX_RUNS");
        failureCount = JDBCUtils.safeGetLong(dbResult, "FAILURE_COUNT");
        maxFailures = JDBCUtils.safeGetLong(dbResult, "MAX_FAILURES");
        retryCount = JDBCUtils.safeGetLong(dbResult, "RETRY_COUNT");
        lastStartDate = JDBCUtils.safeGetString(dbResult, "LAST_START_DATE");
        lastRunDuration = JDBCUtils.safeGetString(dbResult, "LAST_RUN_DURATION");
        nextRunDate = JDBCUtils.safeGetString(dbResult, "NEXT_RUN_DATE");
        scheduleLimit = JDBCUtils.safeGetString(dbResult, "SCHEDULE_LIMIT");
        maxRunDuration = JDBCUtils.safeGetString(dbResult, "MAX_RUN_DURATION");
        loggingLevel = JDBCUtils.safeGetString(dbResult, "LOGGING_LEVEL");
        stopOnWindowClose = JDBCUtils.safeGetString(dbResult, "STOP_ON_WINDOW_CLOSE");
        instanceStickiness = JDBCUtils.safeGetString(dbResult, "INSTANCE_STICKINESS");
        raiseEvents = JDBCUtils.safeGetString(dbResult, "RAISE_EVENTS");
        system = JDBCUtils.safeGetString(dbResult, "SYSTEM");
        jobWeight = JDBCUtils.safeGetString(dbResult, "JOB_WEIGHT");
        nlsEnv = JDBCUtils.safeGetString(dbResult, "NLS_ENV");
        source = JDBCUtils.safeGetString(dbResult, "SOURCE");
        numberOfDestinations = JDBCUtils.safeGetString(dbResult, "NUMBER_OF_DESTINATIONS");
        destinationOwner = JDBCUtils.safeGetString(dbResult, "DESTINATION_OWNER");
        destination = JDBCUtils.safeGetString(dbResult, "DESTINATION");
        credentialOwner = JDBCUtils.safeGetString(dbResult, "CREDENTIAL_OWNER");
        credentialName = JDBCUtils.safeGetString(dbResult, "CREDENTIAL_NAME");
        instanceId = JDBCUtils.safeGetString(dbResult, "INSTANCE_ID");
        deferredDrop = JDBCUtils.safeGetString(dbResult, "DEFERRED_DROP");
        allowRunsInRestrictedMode = JDBCUtils.safeGetString(dbResult, "ALLOW_RUNS_IN_RESTRICTED_MODE");
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

    @Property(category = CAT_ADVANCED, viewable = false, order = 14)
    public String getGlobalUid() {
        return globalUid;
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

    @Property(category = CAT_EVENTS, viewable = false, order = 25)
    public String getEventQueueOwner() {
        return eventQueueOwner;
    }

    @Property(category = CAT_EVENTS, viewable = false, order = 26)
    public String getEventQueueName() {
        return eventQueueName;
    }

    @Property(category = CAT_EVENTS, viewable = false, order = 27)
    public String getEventQueueAgent() {
        return eventQueueAgent;
    }

    @Property(category = CAT_EVENTS, viewable = false, order = 28)
    public String getEventCondition() {
        return eventCondition;
    }

    @Property(category = CAT_EVENTS, viewable = false, order = 29)
    public String getEventRule() {
        return eventRule;
    }

    @Property(category = CAT_EVENTS, viewable = false, order = 30)
    public String getFileWatcherOwner() {
        return fileWatcherOwner;
    }

    @Property(category = CAT_EVENTS, viewable = false, order = 31)
    public String getFileWatcherName() {
        return fileWatcherName;
    }

    @Property(viewable = false, order = 33)
    public String getJobClass() {
        return jobClass;
    }

    @Property(category = CAT_SETTINGS, viewable = false, order = 34)
    public String getEnabled() {
        return enabled;
    }

    @Property(category = CAT_SETTINGS, viewable = false, order = 35)
    public String getAutoDrop() {
        return autoDrop;
    }

    @Property(category = CAT_SETTINGS, viewable = false, order = 36)
    public String getRestartable() {
        return restartable;
    }

    @Property(viewable = false, order = 37)
    public String getState() {
        return state;
    }

    @Property(category = CAT_SETTINGS, viewable = false, order = 38)
    public int getJobPriority() {
        return jobPriority;
    }

    @Property(category = CAT_STATISTICS, viewable = false, order = 39)
    public long getRunCount() {
        return runCount;
    }

    @Property(category = CAT_STATISTICS, viewable = false, order = 40)
    public long getMaxRuns() {
        return maxRuns;
    }

    @Property(category = CAT_STATISTICS, viewable = false, order = 41)
    public long getFailureCount() {
        return failureCount;
    }

    @Property(category = CAT_STATISTICS, viewable = false, order = 42)
    public long getMaxFailures() {
        return maxFailures;
    }

    @Property(category = CAT_STATISTICS, viewable = false, order = 43)
    public long getRetryCount() {
        return retryCount;
    }

    @Property(category = CAT_STATISTICS, viewable = false, order = 44)
    public String getLastStartDate() {
        return lastStartDate;
    }

    @Property(category = CAT_STATISTICS, viewable = false, order = 45)
    public String getLastRunDuration() {
        return lastRunDuration;
    }

    @Property(category = CAT_SETTINGS, viewable = false, order = 46)
    public String getNextRunDate() {
        return nextRunDate;
    }

    @Property(category = CAT_SETTINGS, viewable = false, order = 47)
    public String getScheduleLimit() {
        return scheduleLimit;
    }

    //@Property(viewable = false, order = 48)
    public String getMaxRunDuration() {
        return maxRunDuration;
    }

    @Property(category = CAT_SETTINGS, viewable = false, order = 49)
    public String getLoggingLevel() {
        return loggingLevel;
    }

    @Property(category = CAT_SETTINGS, viewable = false, order = 50)
    public String getStopOnWindowClose() {
        return stopOnWindowClose;
    }

    @Property(category = CAT_ADVANCED, viewable = false, order = 51)
    public String getInstanceStickiness() {
        return instanceStickiness;
    }

    @Property(category = CAT_ADVANCED, viewable = false, order = 52)
    public String getRaiseEvents() {
        return raiseEvents;
    }

    @Property(category = CAT_SETTINGS, viewable = false, order = 53)
    public String getSystem() {
        return system;
    }

    @Property(category = CAT_ADVANCED, viewable = false, order = 54)
    public String getJobWeight() {
        return jobWeight;
    }

    @Property(category = CAT_ADVANCED, viewable = false, order = 55)
    public String getNlsEnv() {
        return nlsEnv;
    }

    @Property(category = CAT_ADVANCED, viewable = false, order = 56)
    public String getSource() {
        return source;
    }

    //@Property(viewable = false, order = 57)
    public String getNumberOfDestinations() {
        return numberOfDestinations;
    }

    @Property(category = CAT_ADVANCED, viewable = false, order = 58)
    public String getDestinationOwner() {
        return destinationOwner;
    }

    @Property(category = CAT_ADVANCED, viewable = false, order = 59)
    public String getDestination() {
        return destination;
    }

    @Property(category = CAT_ADVANCED, viewable = false, order = 60)
    public String getCredentialOwner() {
        return credentialOwner;
    }

    @Property(category = CAT_ADVANCED, viewable = false, order = 61)
    public String getCredentialName() {
        return credentialName;
    }

    @Property(category = CAT_ADVANCED, viewable = false, order = 62)
    public String getInstanceId() {
        return instanceId;
    }

    @Property(category = CAT_ADVANCED, viewable = false, order = 63)
    public String getDeferredDrop() {
        return deferredDrop;
    }

    @Property(category = CAT_ADVANCED, viewable = false, order = 64)
    public String getAllowRunsInRestrictedMode() {
        return allowRunsInRestrictedMode;
    }

    @Property(viewable = false, order = 200)
    @Nullable
    @Override
    public String getDescription() {
        return comments;
    }

    @Association
    public Collection<OracleSchedulerJobArgument> getArguments(DBRProgressMonitor monitor) throws DBException
    {
        return argumentsCache.getAllObjects(monitor, this);
    }

    static class ArgumentsCache extends JDBCObjectCache<OracleSchedulerJob, OracleSchedulerJobArgument> {

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OracleSchedulerJob job) throws SQLException
        {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT * FROM "+ OracleUtils.getSysSchemaPrefix(job.getDataSource()) + "ALL_SCHEDULER_JOB_ARGS " +
                            "WHERE OWNER=? AND JOB_NAME=? " +
                            "ORDER BY ARGUMENT_POSITION");
            dbStat.setString(1, job.getSchema().getName());
            dbStat.setString(2, job.getName());
            return dbStat;
        }

        @Override
        protected OracleSchedulerJobArgument fetchObject(@NotNull JDBCSession session, @NotNull OracleSchedulerJob job, @NotNull JDBCResultSet resultSet) throws SQLException, DBException
        {
            return new OracleSchedulerJobArgument(job, resultSet);
        }

    }

	public DBSObjectState getObjectState() {
		DBSObjectState objectState = null;
		
		try {
			if ( JobState.valueOf(state).equals(JobState.RUNNING) ) {
				objectState = DBSObjectState.ACTIVE;
			} else if ( JobState.valueOf(state).equals(JobState.BROKEN) ) {
				objectState = DBSObjectState.INVALID;
			} else if ( JobState.valueOf(state).equals(JobState.CHAIN_STALLED) ) {
				objectState = DBSObjectState.INVALID;
			} else if ( JobState.valueOf(state).equals(JobState.FAILED) ) {
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
        	try (final JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load action for " + OracleObjectType.JOB + " '" + this.getName() + "'")) {
        		try (JDBCPreparedStatement dbStat = session.prepareStatement(
                        "SELECT STATE FROM " + OracleUtils.getSysSchemaPrefix(getDataSource()) + "ALL_SCHEDULER_JOBS " +
                            "WHERE OWNER=? AND JOB_NAME=? ")) {
                    dbStat.setString(1, getOwner() );
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
            } catch (SQLException e) {
            	monitor.subTask("Error refreshing job state " + e.getMessage());
            } finally {
                monitor.done();
            }
        }
	}

    public DBEPersistAction[] getRunActions() {
        StringBuffer runScript = new StringBuffer();
        runScript.append("BEGIN\n");
        runScript.append("\tDBMS_SCHEDULER.RUN_JOB(JOB_NAME => '");
        runScript.append(getFullyQualifiedName(DBPEvaluationContext.DDL));
        runScript.append("', USE_CURRENT_SESSION => FALSE);");
        runScript.append("END;");
    	return new DBEPersistAction[] {
            new OracleObjectPersistAction(
                OracleObjectType.JOB,
                "Run Job",
                runScript.toString()
            )};
    }

	@Override
	public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        if (jobAction == null && monitor != null) {
        	monitor.beginTask("Load action for '" + this.getName() + "'...", 1);
        	try (final JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load action for " + OracleObjectType.JOB + " '" + this.getName() + "'")) {
        		try (JDBCPreparedStatement dbStat = session.prepareStatement(
                        "SELECT JOB_ACTION FROM " + OracleUtils.getSysSchemaPrefix(getDataSource()) + "ALL_SCHEDULER_JOBS " +
                            "WHERE OWNER=? AND JOB_NAME=? ")) {
                    dbStat.setString(1, getOwner() );
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
		// TODO Complete this so that Generate DDL includes the entire job definition, not just the action block
		return null;
	}

}
