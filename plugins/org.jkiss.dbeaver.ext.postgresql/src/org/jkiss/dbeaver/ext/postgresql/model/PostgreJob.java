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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.DBPStatefulObject;
import org.jkiss.dbeaver.model.DBPSystemInfoObject;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectLookupCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class PostgreJob implements PostgreObject, DBPStatefulObject, DBPRefreshableObject, DBPSaveableObject, DBPSystemInfoObject {
    private static final Log log = Log.getLog(PostgreJob.class);

    private final PostgreDatabase database;
    private long id;
    private String name;
    private String description;
    private String hostAgent;
    private PostgreJobClass jobClass;
    private boolean enabled;
    private boolean persisted;

    private final StepCache stepCache = new StepCache();
    private final ScheduleCache scheduleCache = new ScheduleCache();

    public PostgreJob(@NotNull DBRProgressMonitor monitor, @NotNull PostgreDatabase database, @NotNull ResultSet dbResult) throws DBException {
        this.database = database;
        this.id = JDBCUtils.safeGetLong(dbResult, "jobid");
        this.name = JDBCUtils.safeGetString(dbResult, "jobname");
        this.description = JDBCUtils.safeGetString(dbResult, "jobdesc");
        this.hostAgent = JDBCUtils.safeGetString(dbResult, "jobhostagent");
        this.jobClass = database.getJobClass(monitor, JDBCUtils.safeGetLong(dbResult, "jobjclid"));
        this.enabled = JDBCUtils.safeGetBoolean(dbResult, "jobenabled");
        this.persisted = true;
    }

    public PostgreJob(@NotNull DBRProgressMonitor monitor, @NotNull PostgreDatabase database, @NotNull String name) throws DBException {
        this.database = database;
        this.id = 0;
        this.name = name;
        this.description = "";
        this.hostAgent = "";
        this.jobClass = database.getJobClass(monitor, 1);
        this.enabled = true;
        this.persisted = false;
    }

    @Override
    public long getObjectId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, updatable = true, order = 1)
    public String getName() {
        return name;
    }

    public void setName(@NotNull String name) {
        this.name = name;
    }

    @Nullable
    @Override
    @Property(viewable = true, editable = true, updatable = true, order = 2)
    public String getDescription() {
        return description;
    }

    public void setDescription(@NotNull String description) {
        this.description = description;
    }

    @NotNull
    @Property(viewable = true, editable = true, updatable = true, order = 3)
    public String getHostAgent() {
        return hostAgent;
    }

    public void setHostAgent(@NotNull String hostAgent) {
        this.hostAgent = hostAgent;
    }

    @NotNull
    @Property(viewable = true, editable = true, updatable = true, listProvider = JobClassListProvider.class, order = 4)
    public PostgreJobClass getJobClass() {
        return jobClass;
    }

    public void setJobClass(@NotNull PostgreJobClass jobClass) {
        this.jobClass = jobClass;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 5)
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Association
    @NotNull
    public List<PostgreJobStep> getSteps(@NotNull DBRProgressMonitor monitor) throws DBException {
        return stepCache.getAllObjects(monitor, this);
    }

    @Nullable
    public PostgreJobStep getStep(@NotNull DBRProgressMonitor monitor, @NotNull String name) throws DBException {
        return stepCache.getObject(monitor, this, name);
    }

    @NotNull
    public StepCache getStepCache() {
        return stepCache;
    }

    @Association
    @NotNull
    public List<PostgreJobSchedule> getSchedules(@NotNull DBRProgressMonitor monitor) throws DBException {
        return scheduleCache.getAllObjects(monitor, this);
    }

    @Nullable
    public PostgreJobSchedule getSchedule(@NotNull DBRProgressMonitor monitor, @NotNull String name) throws DBException {
        return scheduleCache.getObject(monitor, this, name);
    }

    @NotNull
    public ScheduleCache getScheduleCache() {
        return scheduleCache;
    }

    @Override
    public boolean isPersisted() {
        return persisted;
    }

    @Override
    public void setPersisted(boolean persisted) {
        this.persisted = persisted;
    }

    @Nullable
    @Override
    public DBSObject getParentObject() {
        return database;
    }

    @NotNull
    @Override
    public PostgreDataSource getDataSource() {
        return database.getDataSource();
    }

    @NotNull
    @Override
    public PostgreDatabase getDatabase() {
        return database;
    }

    @NotNull
    @Override
    public DBSObjectState getObjectState() {
        return enabled ? DBSObjectState.ACTIVE : DBSObjectState.NORMAL;
    }

    @Override
    public void refreshObjectState(@NotNull DBRProgressMonitor monitor) {
        // not implemented
    }

    @Nullable
    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        final PostgreDatabase database = getDatabase();

        stepCache.clearCache();
        scheduleCache.clearCache();

        return database.jobCache.refreshObject(monitor, database, this);
    }

    public static class StepCache extends JDBCObjectLookupCache<PostgreJob, PostgreJobStep> {
        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull PostgreJob job, @Nullable PostgreJobStep step, @Nullable String objectName) throws SQLException {
            final StringBuilder sql = new StringBuilder("SELECT * FROM pgagent.pga_jobstep WHERE jstjobid=?");
            if (step != null) {
                sql.append(" AND jstid=?");
            }
            final JDBCPreparedStatement stmt = session.prepareStatement(sql.toString());
            stmt.setLong(1, job.getObjectId());
            if (step != null) {
                stmt.setLong(2, step.getObjectId());
            }
            return stmt;
        }

        @Nullable
        @Override
        protected PostgreJobStep fetchObject(@NotNull JDBCSession session, @NotNull PostgreJob job, @NotNull JDBCResultSet dbResult) {
            return new PostgreJobStep(job, dbResult);
        }
    }

    public static class ScheduleCache extends JDBCObjectLookupCache<PostgreJob, PostgreJobSchedule> {
        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull PostgreJob job, @Nullable PostgreJobSchedule schedule, @Nullable String objectName) throws SQLException {
            final StringBuilder sql = new StringBuilder("SELECT * FROM pgagent.pga_schedule WHERE jscjobid=?");
            if (schedule != null) {
                sql.append(" AND jscid=?");
            }
            final JDBCPreparedStatement stmt = session.prepareStatement(sql.toString());
            stmt.setLong(1, job.getObjectId());
            if (schedule != null) {
                stmt.setLong(2, schedule.getObjectId());
            }
            return stmt;
        }

        @Nullable
        @Override
        protected PostgreJobSchedule fetchObject(@NotNull JDBCSession session, @NotNull PostgreJob job, @NotNull JDBCResultSet dbResult) {
            return new PostgreJobSchedule(job, dbResult);
        }
    }

    public static class JobClassListProvider implements IPropertyValueListProvider<PostgreJob> {
        @Override
        public boolean allowCustomValue() {
            return false;
        }

        @Override
        public Object[] getPossibleValues(@NotNull PostgreJob object) {
            // Classes are already loaded at this moment, so we are free to use void monitor here
            return object.getDatabase().getJobClassCache().getCachedObjects().toArray();
        }
    }
}
