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
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLStructEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class PostgreJobManager extends SQLStructEditor<PostgreJob, PostgreDataSource> implements DBEObjectRenamer<PostgreJob> {
    private static final Class<? extends DBSObject>[] CHILD_TYPES = CommonUtils.array(
        PostgreJobStep.class,
        PostgreJobSchedule.class);

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource) {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @NotNull
    @Override
    public Class<? extends DBSObject>[] getChildTypes() {
        return CHILD_TYPES;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, PostgreJob> getObjectsCache(PostgreJob object) {
        return object.getDatabase().jobCache;
    }

    @Override
    protected PostgreJob createDatabaseObject(@NotNull DBRProgressMonitor monitor, @NotNull DBECommandContext context, Object container, Object copyFrom, @NotNull Map<String, Object> options) throws DBException {
        final PostgreDatabase database = ((PostgreDataSource) container).getDefaultInstance();
        final String name = DBUtils.makeNewObjectName(monitor, "Job{0}", database, PostgreJob.class, PostgreDatabase::getJob, context);
        return new PostgreJob(monitor, database, name);
    }

    @Override
    protected void addStructObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, StructCreateCommand command, Map<String, Object> options) {
        final PostgreJob job = command.getObject();
        final StringJoiner values = new StringJoiner(", ", "(", ")");

        values.add(String.valueOf(job.getJobClass().getObjectId()));
        values.add(SQLUtils.quoteString(job.getDataSource(), job.getName()));
        values.add(SQLUtils.quoteString(job.getDataSource(), job.getDescription()));
        values.add(SQLUtils.quoteString(job.getDataSource(), job.getHostAgent()));
        values.add(String.valueOf(job.isEnabled()));

        final List<String> queries = new ArrayList<>();
        final StringBuilder buffer = new StringBuilder();

        queries.add("INSERT INTO pgagent.pga_job(jobjclid, jobname, jobdesc, jobhostagent, jobenabled)\nVALUES " + values + "\nRETURNING jobid");

        for (NestedObjectCommand<?, ?> nestedCommand : getNestedOrderedCommands(command)) {
            if (nestedCommand.getObject() == job) {
                continue;
            }

            final String nestedDeclaration = nestedCommand.getNestedDeclaration(monitor, job, options);

            if (!CommonUtils.isEmpty(nestedDeclaration)) {
                queries.add(nestedDeclaration);
            }
        }

        if (queries.size() > 1) {
            buffer.append("WITH\n\n");

            for (int i = 0; i < queries.size(); i++) {
                buffer.append("job");
                if (i > 0) {
                    buffer.append("_").append(i);
                }
                buffer.append(" AS (\n").append(queries.get(i)).append(")");
                if (i < queries.size() - 1) {
                    buffer.append(",");
                }
                buffer.append("\n\n");
            }

            buffer.append("SELECT jobid FROM job");
        } else {
            buffer.append(queries.get(0));
        }

        actions.add(new SQLDatabasePersistAction("Create job", buffer.toString()) {
            @Override
            public void afterExecute(@NotNull DBCSession session, @Nullable DBCStatement stmt, @Nullable Throwable error) throws DBCException {
                if (stmt == null || error != null) {
                    return;
                }
                try (DBCResultSet resultSet = stmt.openResultSet()) {
                    if (resultSet != null && resultSet.nextRow()) {
                        command.getObject().setId(CommonUtils.toLong(resultSet.getAttributeValue(0)));
                    }
                }
            }
        });
    }

    @Override
    protected void addObjectModifyActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectChangeCommand command, @NotNull Map<String, Object> options) throws DBException {
        final PostgreJob job = command.getObject();
        final StringJoiner values = new StringJoiner(",\n\t");

        if (command.getProperty("jobClass") != null) {
            values.add("jobjclid=" + job.getJobClass().getObjectId());
        }

        if (command.hasProperty("description")) {
            values.add("jobdesc=" + SQLUtils.quoteString(job.getDataSource(), CommonUtils.notEmpty(job.getDescription())));
        }

        if (command.hasProperty("hostAgent")) {
            values.add("jobhostagent=" + SQLUtils.quoteString(job.getDataSource(), CommonUtils.notEmpty(job.getHostAgent())));
        }

        if (command.getProperty("enabled") != null) {
            values.add("jobenabled=" + job.isEnabled());
        }

        if (values.length() > 0) {
            actions.add(new SQLDatabasePersistAction(
                "Alter job",
                "UPDATE pgagent.pga_job\nSET\n\t" + values + "\nWHERE jobid=" + job.getObjectId()
            ));
        }
    }

    @Override
    protected void addObjectDeleteActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectDeleteCommand command, @NotNull Map<String, Object> options) throws DBException {
        final PostgreJob job = command.getObject();
        actions.add(new SQLDatabasePersistAction(
            "Delete job",
            "DELETE FROM pgagent.pga_job\nWHERE jobid=" + job.getObjectId()
        ));
    }

    @Override
    protected void addObjectRenameActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectRenameCommand command, @NotNull Map<String, Object> options) {
        final PostgreJob job = command.getObject();
        actions.add(new SQLDatabasePersistAction(
            "Rename job",
            "UPDATE pgagent.pga_job\nSET\n\tjobname=" + SQLUtils.quoteString(job.getDataSource(), command.getNewName()) + "\nWHERE jobid=" + job.getObjectId()
        ));
    }

    @Override
    public void renameObject(@NotNull DBECommandContext commandContext, @NotNull PostgreJob object, @NotNull Map<String, Object> options, @NotNull String newName) throws DBException {
        processObjectRename(commandContext, object, options, newName);
    }
}
