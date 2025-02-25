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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.source.OracleStatefulObject;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPScriptObject;
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
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Oracle scheduler job
 */
public class OracleSchedulerJob extends OracleSchemaObject implements OracleStatefulObject, DBPScriptObject {

    private static final String CAT_SETTINGS = "Settings";
    private static final String CAT_EVENTS = "Events";
    private static final String CAT_ADVANCED = "Advanced";

    private static final String DEFAULT_JOB_CLASS = "DEFAULT_JOB_CLASS";

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
    private boolean enabled;
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

    public enum JobState {
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

    public OracleSchedulerJob(OracleSchema schema, String name, String state, String jobAction) {
        super(schema, name, false);
        this.state = state;
        this.jobAction = jobAction;
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
        enabled = JDBCUtils.safeGetBoolean(dbResult, "ENABLED");
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

    @Property(viewable = true, order = 11, editable = true)
    public String getJobStyle() {
        return jobStyle;
    }

    public void setJobStyle(String jobStyle) {
        this.jobStyle = jobStyle;
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

    @Property(viewable = false, order = 16, editable = true, updatable = true)
    public String getProgramName() {
        return programName;
    }

    public void setProgramName(String programName) {
        this.programName = programName;
    }

    @Property(viewable = true, order = 17, required = true, editable = true, updatable = true)
    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    @Property(
        category = CAT_SETTINGS,
        viewable = false, required = true,
        order = 18, editable = true,
        updatable = true,
        length = PropertyLength.MULTILINE
    )
    public String getJobAction() {
        return jobAction;
    }

    public void setJobAction(String jobAction) {
        this.jobAction = jobAction;
    }


    @Property(category = CAT_SETTINGS, viewable = false, order = 19, editable = true, updatable = true)
    public long getNumberOfArguments() {
        return numberOfArguments;
    }

    public void setNumberOfArguments(long numberOfArguments) {
        this.numberOfArguments = numberOfArguments;
    }

    @Property(category = CAT_ADVANCED, viewable = false, order = 20)
    public String getScheduleOwner() {
        return scheduleOwner;
    }

    @Property(viewable = false, order = 21, editable = true, updatable = true)
    public String getScheduleName() {
        return scheduleName;
    }

    public void setScheduleName(String scheduleName) {
        this.scheduleName = scheduleName;
    }

    @Property(viewable = true, order = 22)
    public String getScheduleType() {
        return scheduleType;
    }

    @Property(viewable = true, order = 10, updatable = true, editable = true)
    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    @Property(viewable = true, order = 24, updatable = true, editable = true)
    public String getRepeatInterval() {
        return repeatInterval;
    }

    public void setRepeatInterval(String repeatInterval) {
        this.repeatInterval = repeatInterval;
    }

    @Property(viewable = true, order = 32, editable = true, updatable = true)
    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
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

    @Property(category = CAT_EVENTS, viewable = false, order = 28, editable = true)
    public String getEventCondition() {
        return eventCondition;
    }

    public void setEventCondition(String eventCondition) {
        this.eventCondition = eventCondition;
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

    @Property(viewable = false, order = 33, editable = true, updatable = true)
    public String getJobClass() {
        return jobClass;
    }

    public void setJobClass(String jobClass) {
        this.jobClass = jobClass;
    }

    @Property(category = CAT_SETTINGS, viewable = false, order = 34, updatable = true, editable = true)
    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Property(category = CAT_SETTINGS, viewable = false, order = 35, editable = true, updatable = true)
    public String getAutoDrop() {
        return autoDrop;
    }

    public void setAutoDrop(String autoDrop) {
        this.autoDrop = autoDrop;
    }

    @Property(category = CAT_SETTINGS, viewable = false, order = 36, updatable = true)
    public String getRestartable() {
        return restartable;
    }

    public void setRestartable(String restartable) {
        this.restartable = restartable;
    }

    @Property(viewable = false, order = 37)
    public String getState() {
        return state;
    }

    @Property(category = CAT_SETTINGS, viewable = false, order = 38, editable = true, updatable = true)
    public int getJobPriority() {
        return jobPriority;
    }

    public void setJobPriority(int jobPriority) {
        this.jobPriority = jobPriority;
    }

    @Property(category = DBConstants.CAT_STATISTICS, viewable = false, order = 39)
    public long getRunCount() {
        return runCount;
    }

    @Property(category = DBConstants.CAT_STATISTICS, viewable = false, order = 40, editable = true, updatable = true)
    public long getMaxRuns() {
        return maxRuns;
    }

    public void setMaxRuns(long maxRuns) {
        this.maxRuns = maxRuns;
    }

    @Property(category = DBConstants.CAT_STATISTICS, viewable = false, order = 41)
    public long getFailureCount() {
        return failureCount;
    }

    @Property(category = DBConstants.CAT_STATISTICS, viewable = false, order = 42, editable = true, updatable = true)
    public long getMaxFailures() {
        return maxFailures;
    }

    public void setMaxFailures(long maxFailures) {
        this.maxFailures = maxFailures;
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

    @Property(category = CAT_SETTINGS, viewable = false, order = 47, updatable = true)
    public String getScheduleLimit() {
        return scheduleLimit;
    }

    public void setScheduleLimit(String scheduleLimit) {
        this.scheduleLimit = scheduleLimit;
    }

    //@Property(viewable = false, order = 48)
    public String getMaxRunDuration() {
        return maxRunDuration;
    }

    @Property(category = CAT_SETTINGS, viewable = false, order = 49, editable = true, updatable = true)
    public String getLoggingLevel() {
        return loggingLevel;
    }

    public void setLoggingLevel(String loggingLevel) {
        this.loggingLevel = loggingLevel;
    }

    @Property(category = CAT_SETTINGS, viewable = false, order = 50, editable = true)
    public String getStopOnWindowClose() {
        return stopOnWindowClose;
    }

    public void setStopOnWindowClose(String stopOnWindowClose) {
        this.stopOnWindowClose = stopOnWindowClose;
    }

    @Property(category = CAT_ADVANCED, viewable = false, order = 51, updatable = true)
    public String getInstanceStickiness() {
        return instanceStickiness;
    }

    public void setInstanceStickiness(String instanceStickiness) {
        this.instanceStickiness = instanceStickiness;
    }

    @Property(category = CAT_ADVANCED, viewable = false, order = 52, editable = true, updatable = true)
    public String getRaiseEvents() {
        return raiseEvents;
    }

    public void setRaiseEvents(String raiseEvents) {
        this.raiseEvents = raiseEvents;
    }

    @Property(category = CAT_SETTINGS, viewable = false, order = 53)
    public String getSystem() {
        return system;
    }

    @Property(category = CAT_ADVANCED, viewable = false, order = 54, editable = true, updatable = true)
    public String getJobWeight() {
        return jobWeight;
    }

    public void setJobWeight(String jobWeight) {
        this.jobWeight = jobWeight;
    }

    @Property(category = CAT_ADVANCED, viewable = false, order = 55, editable = true)
    public String getNlsEnv() {
        return nlsEnv;
    }

    public void setNlsEnv(String nlsEnv) {
        this.nlsEnv = nlsEnv;
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

    @Property(category = CAT_ADVANCED, viewable = false, order = 59, updatable = true)
    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    @Property(category = CAT_ADVANCED, viewable = false, order = 60)
    public String getCredentialOwner() {
        return credentialOwner;
    }

    @Property(category = CAT_ADVANCED, viewable = false, order = 61, editable = true, updatable = true)
    public String getCredentialName() {
        return credentialName;
    }

    public void setCredentialName(String credentialName) {
        this.credentialName = credentialName;
    }

    @Property(category = CAT_ADVANCED, viewable = false, order = 62, editable = true, updatable = true)
    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    @Property(category = CAT_ADVANCED, viewable = false, order = 63)
    public String getDeferredDrop() {
        return deferredDrop;
    }

    @Property(category = CAT_ADVANCED, viewable = false, order = 64, editable = true)
    public String getAllowRunsInRestrictedMode() {
        return allowRunsInRestrictedMode;
    }

    public void setAllowRunsInRestrictedMode(String allowRunsInRestrictedMode) {
        this.allowRunsInRestrictedMode = allowRunsInRestrictedMode;
    }

    @Property(viewable = false, order = 200, editable = true, updatable = true)
    @Nullable
    @Override
    public String getDescription() {
        return comments;
    }

    public void setDescription(String comments) {
        this.comments = comments;
    }

    @Association
    public Collection<OracleSchedulerJobArgument> getArguments(DBRProgressMonitor monitor) throws DBException {
        return argumentsCache.getAllObjects(monitor, this);
    }

    static class ArgumentsCache extends JDBCObjectCache<OracleSchedulerJob, OracleSchedulerJobArgument> {

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OracleSchedulerJob job) throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM " + OracleUtils.getSysSchemaPrefix(job.getDataSource()) + "ALL_SCHEDULER_JOB_ARGS " +
                    "WHERE OWNER=? AND JOB_NAME=? " +
                    "ORDER BY ARGUMENT_POSITION");
            dbStat.setString(1, job.getSchema().getName());
            dbStat.setString(2, job.getName());
            return dbStat;
        }

        @Override
        protected OracleSchedulerJobArgument fetchObject(
            @NotNull JDBCSession session,
            @NotNull OracleSchedulerJob job,
            @NotNull JDBCResultSet resultSet
        ) throws SQLException, DBException {
            return new OracleSchedulerJobArgument(job, resultSet);
        }

    }

    public DBSObjectState getObjectState() {
        DBSObjectState objectState = null;

        try {
            if (JobState.valueOf(state).equals(JobState.RUNNING)) {
                objectState = DBSObjectState.ACTIVE;
            } else if (JobState.valueOf(state).equals(JobState.BROKEN)) {
                objectState = DBSObjectState.INVALID;
            } else if (JobState.valueOf(state).equals(JobState.CHAIN_STALLED)) {
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
            try (
                final JDBCSession session = DBUtils.openMetaSession(
                    monitor,
                    this,
                    "Load action for " + OracleObjectType.JOB + " '" + this.getName() + "'"
                )
            ) {
                try (
                    JDBCPreparedStatement dbStat = session.prepareStatement(
                        "SELECT STATE FROM " + OracleUtils.getSysSchemaPrefix(getDataSource()) + "ALL_SCHEDULER_JOBS " +
                            "WHERE OWNER=? AND JOB_NAME=? ")
                ) {
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
            )
        };
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        if (CommonUtils.isEmpty(jobAction) && name.equals("NEW_SCHEDULER_JOB")) {
            return "";
        }
        if (monitor != null) {
            monitor.beginTask("Load action for '" + this.getName() + "'...", 1);
            try (
                final JDBCSession session = DBUtils.openMetaSession(
                    monitor,
                    this,
                    "Load action for " + OracleObjectType.JOB + " '" + this.getName() + "'"
                )
            ) {
                try (
                    JDBCPreparedStatement dbStat = session.prepareStatement(
                        "SELECT JOB_ACTION FROM " + OracleUtils.getSysSchemaPrefix(getDataSource()) + "ALL_SCHEDULER_JOBS " +
                            "WHERE OWNER=? AND JOB_NAME=? ")
                ) {
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
                            if (!"null".equals(line)) {
                                action.append(line);
                            }
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

        final StringJoiner args = new StringJoiner(",\n\t");
        args.add("job_name => " + SQLUtils.quoteString(this, name));
        if (jobType != null) {
            args.add("job_type => " + SQLUtils.quoteString(this, jobType));
        }
        if (jobAction != null) {
            args.add("job_action => " + SQLUtils.quoteString(this, CommonUtils.escapeDisplayString(jobAction)));
        }

        if (!DEFAULT_JOB_CLASS.equals(jobClass)) {
            args.add("job_class => " + SQLUtils.quoteString(this, jobClass));
        }

        if (startDate != null) {
            args.add("start_date => TO_TIMESTAMP_TZ(" + SQLUtils.quoteString(this, startDate) + ", 'yyyy-mm-dd hh24:mi:ss.ff tzr')");
        }

        if (endDate != null) {
            args.add("end_date => TO_TIMESTAMP_TZ(" + SQLUtils.quoteString(this, endDate) + ", 'yyyy-mm-dd hh24:mi:ss.ff tzr')");
        }

        if (repeatInterval != null) {
            args.add("repeat_interval => " + SQLUtils.quoteString(this, repeatInterval));
        }

        if (comments != null) {
            args.add("comments => " + SQLUtils.quoteString(this, comments));
        }

        if (enabled) {
            args.add("enabled => TRUE");
        }

        return "BEGIN\n  DBMS_SCHEDULER.CREATE_JOB(\n\t" + args + "\n);\nEND;";
    }
}
