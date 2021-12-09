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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPStatefulObject;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class PostgreJob implements PostgreObject, DBPStatefulObject {
    private final PostgreDatabase database;
    private final long id;
    private final String name;
    private final String description;
    private final PostgreJobClass jobClass;
    private final boolean enabled;

    private final StepCache stepCache = new StepCache();
    private final ScheduleCache scheduleCache = new ScheduleCache();

    public PostgreJob(@NotNull DBRProgressMonitor monitor, @NotNull PostgreDatabase database, @NotNull ResultSet dbResult) throws DBException {
        this.database = database;
        this.id = JDBCUtils.safeGetLong(dbResult, "jobid");
        this.name = JDBCUtils.safeGetString(dbResult, "jobname");
        this.description = JDBCUtils.safeGetString(dbResult, "jobdesc");
        this.jobClass = database.getJobClass(monitor, JDBCUtils.safeGetLong(dbResult, "jobjclid"));
        this.enabled = JDBCUtils.safeGetBoolean(dbResult, "jobenabled");
    }

    @Override
    public long getObjectId() {
        return id;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    @Nullable
    @Override
    @Property(viewable = true, order = 2)
    public String getDescription() {
        return description;
    }

    @NotNull
    @Property(viewable = true, order = 3)
    public PostgreJobClass getJobClass() {
        return jobClass;
    }

    @Property(viewable = true, order = 4)
    public boolean isEnabled() {
        return enabled;
    }

    @Association
    @NotNull
    public List<PostgreJobStep> getSteps(@NotNull DBRProgressMonitor monitor) throws DBException {
        return stepCache.getAllObjects(monitor, this);
    }

    @Association
    @NotNull
    public List<PostgreJobSchedule> getSchedules(@NotNull DBRProgressMonitor monitor) throws DBException {
        return scheduleCache.getAllObjects(monitor, this);
    }

    @Override
    public boolean isPersisted() {
        return true;
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
        stepCache.clearCache();
        scheduleCache.clearCache();
    }

    private static class StepCache extends JDBCObjectCache<PostgreJob, PostgreJobStep> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreJob job) throws SQLException {
            final JDBCPreparedStatement stmt = session.prepareStatement("SELECT * FROM pgagent.pga_jobstep WHERE jstjobid=?");
            stmt.setLong(1, job.getObjectId());
            return stmt;
        }

        @Nullable
        @Override
        protected PostgreJobStep fetchObject(@NotNull JDBCSession session, @NotNull PostgreJob job, @NotNull JDBCResultSet dbResult) {
            return new PostgreJobStep(job, dbResult);
        }
    }

    private static class ScheduleCache extends JDBCObjectCache<PostgreJob, PostgreJobSchedule> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreJob job) throws SQLException {
            final JDBCPreparedStatement stmt = session.prepareStatement("SELECT * FROM pgagent.pga_schedule WHERE jscjobid=?");
            stmt.setLong(1, job.getObjectId());
            return stmt;
        }

        @Nullable
        @Override
        protected PostgreJobSchedule fetchObject(@NotNull JDBCSession session, @NotNull PostgreJob job, @NotNull JDBCResultSet dbResult) {
            return new PostgreJobSchedule(job, dbResult);
        }
    }
}
