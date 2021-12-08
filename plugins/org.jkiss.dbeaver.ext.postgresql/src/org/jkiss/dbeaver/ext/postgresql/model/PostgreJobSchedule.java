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
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.IPropertyValueTransformer;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Arrays;

public class PostgreJobSchedule implements PostgreObject {
    private final PostgreJob job;
    private final long id;
    private final String name;
    private final String description;
    private final boolean enabled;
    private final Timestamp start;
    private final Timestamp end;
    private final Boolean[] minutes;
    private final Boolean[] hours;
    private final Boolean[] weekDays;
    private final Boolean[] monthDays;
    private final Boolean[] months;

    public PostgreJobSchedule(@NotNull PostgreJob job, ResultSet dbResult) {
        this.job = job;
        this.id = JDBCUtils.safeGetLong(dbResult, "jscid");
        this.name = JDBCUtils.safeGetString(dbResult, "jscname");
        this.description = JDBCUtils.safeGetString(dbResult, "jscdesc");
        this.enabled = JDBCUtils.safeGetBoolean(dbResult, "jscenabled");
        this.start = JDBCUtils.safeGetTimestamp(dbResult, "jscstart");
        this.end = JDBCUtils.safeGetTimestamp(dbResult, "jscend");
        this.minutes = JDBCUtils.safeGetArray(dbResult, "jscminutes");
        this.hours = JDBCUtils.safeGetArray(dbResult, "jschours");
        this.weekDays = JDBCUtils.safeGetArray(dbResult, "jscweekdays");
        this.monthDays = JDBCUtils.safeGetArray(dbResult, "jscmonthdays");
        this.months = JDBCUtils.safeGetArray(dbResult, "jscmonths");
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

    @Property(viewable = true, order = 3)
    public boolean isEnabled() {
        return enabled;
    }

    @NotNull
    @Property(viewable = true, order = 4)
    public Timestamp getStart() {
        return start;
    }

    @NotNull
    @Property(viewable = true, order = 5)
    public Timestamp getEnd() {
        return end;
    }

    @NotNull
    @Property(order = 10, category = DBConstants.CAT_STATISTICS, valueRenderer = ArrayValueRenderer.class)
    public Boolean[] getMinutes() {
        return minutes;
    }

    @NotNull
    @Property(order = 11, category = DBConstants.CAT_STATISTICS, valueRenderer = ArrayValueRenderer.class)
    public Boolean[] getHours() {
        return hours;
    }

    @NotNull
    @Property(order = 12, category = DBConstants.CAT_STATISTICS, valueRenderer = ArrayValueRenderer.class)
    public Boolean[] getWeekDays() {
        return weekDays;
    }

    @NotNull
    @Property(order = 13, category = DBConstants.CAT_STATISTICS, valueRenderer = ArrayValueRenderer.class)
    public Boolean[] getMonthDays() {
        return monthDays;
    }

    @NotNull
    @Property(order = 14, category = DBConstants.CAT_STATISTICS, valueRenderer = ArrayValueRenderer.class)
    public Boolean[] getMonths() {
        return months;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @Nullable
    @Override
    public DBSObject getParentObject() {
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

    public static class ArrayValueRenderer implements IPropertyValueTransformer<Object, Object> {
        @Override
        public Object transform(Object object, Object value) throws IllegalArgumentException {
            return Arrays.toString((Object[]) value);
        }
    }
}
