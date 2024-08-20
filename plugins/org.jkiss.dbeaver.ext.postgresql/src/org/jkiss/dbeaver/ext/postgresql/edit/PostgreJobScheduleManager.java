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
package org.jkiss.dbeaver.ext.postgresql.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreJob;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreJobSchedule;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class PostgreJobScheduleManager extends SQLObjectEditor<PostgreJobSchedule, PostgreJob> implements DBEObjectRenamer<PostgreJobSchedule> {
    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource) {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, PostgreJobSchedule> getObjectsCache(PostgreJobSchedule object) {
        return object.getParentObject().getScheduleCache();
    }

    @Override
    protected PostgreJobSchedule createDatabaseObject(@NotNull DBRProgressMonitor monitor, @NotNull DBECommandContext context, Object container, Object copyFrom, @NotNull Map<String, Object> options) throws DBException {
        final PostgreJob job = (PostgreJob) container;
        final String name = DBUtils.makeNewObjectName(monitor, "Schedule{0}", job, PostgreJobSchedule.class, PostgreJob::getSchedule, context);
        return new PostgreJobSchedule(job, name);
    }

    @Override
    protected StringBuilder getNestedDeclaration(DBRProgressMonitor monitor, PostgreJob owner, DBECommandAbstract<PostgreJobSchedule> command, Map<String, Object> options) {
        return new StringBuilder(getCreateDDL(command.getObject(), true));
    }

    @Override
    protected void addObjectCreateActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectCreateCommand command, @NotNull Map<String, Object> options) throws DBException {
        actions.add(new SQLDatabasePersistAction(
            "Create schedule",
            getCreateDDL(command.getObject(), false)
        ));
    }

    @Override
    protected void addObjectDeleteActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectDeleteCommand command, @NotNull Map<String, Object> options) throws DBException {
        final PostgreJobSchedule schedule = command.getObject();
        actions.add(new SQLDatabasePersistAction(
            "Delete schedule",
            "DELETE FROM pgagent.pga_schedule\nWHERE jscid=" + schedule.getObjectId()
        ));
    }

    @Override
    protected void addObjectModifyActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectChangeCommand command, @NotNull Map<String, Object> options) throws DBException {
        final PostgreJobSchedule schedule = command.getObject();
        final StringJoiner values = new StringJoiner(",\n\t");

        if (command.hasProperty("description")) {
            values.add("jscdesc=" + SQLUtils.quoteString(schedule.getDataSource(), CommonUtils.notEmpty(schedule.getDescription())));
        }

        if (command.getProperty("enabled") != null) {
            values.add("jscenabled=" + schedule.isEnabled());
        }

        if (values.length() > 0) {
            actions.add(new SQLDatabasePersistAction(
                "Alter schedule",
                "UPDATE pgagent.pga_schedule\nSET\n\t" + values + "\nWHERE jscid=" + schedule.getObjectId()
            ));
        }
    }

    @Override
    protected void addObjectRenameActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectRenameCommand command, @NotNull Map<String, Object> options) {
        final PostgreJobSchedule schedule = command.getObject();
        actions.add(new SQLDatabasePersistAction(
            "Rename schedule",
            "UPDATE pgagent.pga_schedule\nSET\n\tjscname=" + SQLUtils.quoteString(schedule.getDataSource(), command.getNewName()) + "\nWHERE jscid=" + schedule.getObjectId()
        ));
    }

    @Override
    public void renameObject(@NotNull DBECommandContext commandContext, @NotNull PostgreJobSchedule object, @NotNull Map<String, Object> options, @NotNull String newName) throws DBException {
        processObjectRename(commandContext, object, options, newName);
    }

    @NotNull
    private String getCreateDDL(@NotNull PostgreJobSchedule schedule, boolean nested) {
        final StringJoiner values = new StringJoiner(", ", "(", ")");

        values.add(nested ? "(SELECT jobid FROM job)" : String.valueOf(schedule.getParentObject().getObjectId()));
        values.add(SQLUtils.quoteString(schedule, schedule.getName()));
        values.add(SQLUtils.quoteString(schedule, schedule.getDescription()));
        values.add(String.valueOf(schedule.isEnabled()));
        values.add(SQLUtils.quoteString(schedule, schedule.getStart().toString()));
        values.add(schedule.getEnd() != null ? SQLUtils.quoteString(schedule, schedule.getEnd().toString()) : SQLConstants.NULL_VALUE);
        values.add(toCompactArray(schedule.getMinutes()));
        values.add(toCompactArray(schedule.getHours()));
        values.add(toCompactArray(schedule.getWeekDays()));
        values.add(toCompactArray(schedule.getMonthDays()));
        values.add(toCompactArray(schedule.getMonths()));

        return "INSERT INTO pgagent.pga_schedule (jscjobid, jscname, jscdesc, jscenabled, jscstart, jscend, jscminutes, jschours, jscweekdays, jscmonthdays, jscmonths)\nVALUES " + values;
    }

    @NotNull
    private String toCompactArray(@NotNull boolean[] values) {
        final StringJoiner joiner = new StringJoiner(",", "'{", "}'");
        for (boolean value : values) {
            joiner.add(value ? "t" : "f");
        }
        return joiner.toString();
    }
}
