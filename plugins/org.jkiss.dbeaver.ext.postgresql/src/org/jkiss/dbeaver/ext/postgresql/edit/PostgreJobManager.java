/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLStructEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class PostgreJobManager extends SQLStructEditor<PostgreJob, PostgreDataSource> implements DBEObjectRenamer<PostgreJob> {
    private static final Class<?>[] CHILD_TYPES = {
        PostgreJobStep.class,
        PostgreJobSchedule.class
    };

    @Override
    public long getMakerOptions(DBPDataSource dataSource) {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @NotNull
    @Override
    public Class<?>[] getChildTypes() {
        return CHILD_TYPES;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, PostgreJob> getObjectsCache(PostgreJob object) {
        return object.getDatabase().jobCache;
    }

    @Override
    protected PostgreJob createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, Object container, Object copyFrom, Map<String, Object> options) throws DBException {
        final PostgreDatabase database = ((PostgreDataSource) container).getDefaultInstance();
        final String name = DBUtils.makeNewObjectName(monitor, "Job{0}", database, PostgreJob.class, PostgreDatabase::getJob, context);
        return new PostgreJob(monitor, database, name);
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected void addStructObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, StructCreateCommand command, Map<String, Object> options) {
        final PostgreJob job = command.getObject();
        final StringJoiner values = new StringJoiner(", ", "(", ")");

        values.add(String.valueOf(job.getJobClass().getObjectId()));
        values.add(SQLUtils.quoteString(job.getDataSource(), job.getName()));
        values.add(SQLUtils.quoteString(job.getDataSource(), job.getDescription()));
        values.add(SQLUtils.quoteString(job.getDataSource(), job.getHostAgent()));
        values.add(String.valueOf(job.isEnabled()));

        final StringBuilder nestedDeclarations = new StringBuilder();
        final Collection<NestedObjectCommand> nestedCommands = getNestedOrderedCommands(command);

        for (NestedObjectCommand<?, ?> nestedCommand : nestedCommands) {
            if (nestedCommand.getObject() == job) {
                continue;
            }

            final String nestedDeclaration = nestedCommand.getNestedDeclaration(monitor, job, options);

            if (!CommonUtils.isEmpty(nestedDeclaration)) {
                nestedDeclarations.append("\n\n").append(nestedDeclaration).append(';');
            }
        }

        final StringBuilder buffer = new StringBuilder();

        buffer.append("INSERT INTO pgagent.pga_job(jobjclid, jobname, jobdesc, jobhostagent, jobenabled)\nVALUES ");
        buffer.append(values);

        if (!nestedCommands.isEmpty()) {
            buffer.insert(0, "DO $$\nDECLARE\n\tjid INTEGER;\nBEGIN\n\n");
            buffer.append("\nRETURNING jobid INTO jid;");
        }

        buffer.append(nestedDeclarations);

        if (!nestedCommands.isEmpty()) {
            buffer.append("\n\nEND\n$$");
        }

        actions.add(new SQLDatabasePersistAction(
            "Create job",
            buffer.toString()
        ));
    }

    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectChangeCommand command, Map<String, Object> options) throws DBException {
        final PostgreJob job = command.getObject();
        final StringJoiner values = new StringJoiner(", ");

        if (command.getProperty("jobClass") != null) {
            values.add("jobjclid=" + job.getJobClass().getObjectId());
        }

        if (command.getProperty("description") != null) {
            values.add("jobdesc=" + SQLUtils.quoteString(job.getDataSource(), job.getDescription()));
        }

        if (command.getProperty("hostAgent") != null) {
            values.add("jobhostagent=" + SQLUtils.quoteString(job.getDataSource(), job.getHostAgent()));
        }

        if (command.getProperty("enabled") != null) {
            values.add("jobenabled=" + job.isEnabled());
        }

        if (values.length() > 0) {
            actions.add(new SQLDatabasePersistAction(
                "Alter job",
                "UPDATE pgagent.pga_job\nSET " + values + "\nWHERE jobid=" + job.getObjectId()
            ));
        }
    }

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options) throws DBException {
        final PostgreJob job = command.getObject();
        actions.add(new SQLDatabasePersistAction(
            "Delete job",
            "DELETE FROM pgagent.pga_job\nWHERE jobid=" + job.getObjectId()
        ));
    }

    @Override
    protected void addObjectRenameActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectRenameCommand command, Map<String, Object> options) {
        final PostgreJob job = command.getObject();
        actions.add(new SQLDatabasePersistAction(
            "Rename job",
            "UPDATE pgagent.pga_job\nSET jobname=" + SQLUtils.quoteString(job.getDataSource(), command.getNewName()) + "\nWHERE jobid=" + job.getObjectId()
        ));
    }

    @Override
    public void renameObject(@NotNull DBECommandContext commandContext, @NotNull PostgreJob object, @NotNull Map<String, Object> options, @NotNull String newName) throws DBException {
        processObjectRename(commandContext, object, options, newName);
    }
}
