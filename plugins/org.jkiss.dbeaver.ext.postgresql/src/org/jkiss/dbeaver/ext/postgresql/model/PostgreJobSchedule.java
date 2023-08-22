/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.ArrayUtils;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;

public class PostgreJobSchedule implements PostgreObject, DBPNamedObject2, DBPRefreshableObject, DBPSaveableObject {
    private final PostgreJob job;
    private final long id;
    private String name;
    private String description;
    private boolean enabled;
    private /* final */ Timestamp start;
    private /* final */ Timestamp end;
    private /* final */ boolean[] minutes;
    private /* final */ boolean[] hours;
    private /* final */ boolean[] weekDays;
    private /* final */ boolean[] monthDays;
    private /* final */ boolean[] months;
    private boolean persisted;

    public PostgreJobSchedule(@NotNull PostgreJob job, ResultSet dbResult) {
        this.job = job;
        this.id = JDBCUtils.safeGetLong(dbResult, "jscid");
        this.name = JDBCUtils.safeGetString(dbResult, "jscname");
        this.description = JDBCUtils.safeGetString(dbResult, "jscdesc");
        this.enabled = JDBCUtils.safeGetBoolean(dbResult, "jscenabled");
        this.start = JDBCUtils.safeGetTimestamp(dbResult, "jscstart");
        this.end = JDBCUtils.safeGetTimestamp(dbResult, "jscend");
        this.minutes = ArrayUtils.unbox(PostgreUtils.safeGetBooleanArray(dbResult, "jscminutes"));
        this.hours = ArrayUtils.unbox(PostgreUtils.safeGetBooleanArray(dbResult, "jschours"));
        this.weekDays = ArrayUtils.unbox(PostgreUtils.safeGetBooleanArray(dbResult, "jscweekdays"));
        this.monthDays = ArrayUtils.unbox(PostgreUtils.safeGetBooleanArray(dbResult, "jscmonthdays"));
        this.months = ArrayUtils.unbox(PostgreUtils.safeGetBooleanArray(dbResult, "jscmonths"));
        this.persisted = true;
    }

    public PostgreJobSchedule(@NotNull PostgreJob job, @NotNull String name) {
        this.job = job;
        this.id = 0;
        this.name = name;
        this.description = "";
        this.enabled = true;
        this.start = Timestamp.from(Instant.now());
        this.end = null;
        this.minutes = new boolean[60];
        this.hours = new boolean[24];
        this.weekDays = new boolean[7];
        this.monthDays = new boolean[32];
        this.months = new boolean[12];
        this.persisted = false;
    }

    @Override
    public long getObjectId() {
        return id;
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, updatable = true, order = 1)
    public String getName() {
        return name;
    }

    @Override
    public void setName(@NotNull String name) {
        this.name = name;
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, updatable = true, order = 2)
    public String getDescription() {
        return description;
    }

    public void setDescription(@NotNull String description) {
        this.description = description;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 3)
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @NotNull
    @Property(viewable = true, order = 4, specific = true)
    public Timestamp getStart() {
        return start;
    }

    @Nullable
    @Property(viewable = true, order = 5, specific = true)
    public Timestamp getEnd() {
        return end;
    }

    @NotNull
    public boolean[] getMinutes() {
        return minutes;
    }

    @NotNull
    public boolean[] getHours() {
        return hours;
    }

    @NotNull
    public boolean[] getWeekDays() {
        return weekDays;
    }

    @NotNull
    public boolean[] getMonthDays() {
        return monthDays;
    }

    @NotNull
    public boolean[] getMonths() {
        return months;
    }

    @Override
    public boolean isPersisted() {
        return persisted;
    }

    @Override
    public void setPersisted(boolean persisted) {
        this.persisted = persisted;
    }

    @NotNull
    @Override
    public PostgreJob getParentObject() {
        return job;
    }

    @NotNull
    @Override
    public PostgreDataSource getDataSource() {
        return job.getDataSource();
    }

    @NotNull
    @Override
    public PostgreDatabase getDatabase() {
        return job.getDatabase();
    }

    @Nullable
    @Override
    public PostgreJobSchedule refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        return job.getScheduleCache().refreshObject(monitor, job, this);
    }
}
