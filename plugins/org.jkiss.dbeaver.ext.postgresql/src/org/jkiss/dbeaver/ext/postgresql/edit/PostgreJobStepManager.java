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
package org.jkiss.dbeaver.ext.postgresql.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreJob;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreJobStep;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class PostgreJobStepManager extends SQLObjectEditor<PostgreJobStep, PostgreJob> implements DBEObjectRenamer<PostgreJobStep> {
    @Override
    public long getMakerOptions(DBPDataSource dataSource) {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, PostgreJobStep> getObjectsCache(PostgreJobStep object) {
        return object.getParentObject().getStepCache();
    }

    @Override
    protected PostgreJobStep createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, Object container, Object copyFrom, Map<String, Object> options) throws DBException {
        return new PostgreJobStep(((PostgreJob) container), "New Step");
    }

    @Override
    protected void validateObjectProperties(DBRProgressMonitor monitor, ObjectChangeCommand command, Map<String, Object> options) throws DBException {
        final PostgreJobStep step = command.getObject();

        if (step.getKind() == PostgreJobStep.StepKind.s && (step.getTargetDatabase() == null && CommonUtils.isEmptyTrimmed(step.getRemoteConnectionString()))) {
            throw new DBException("Target database or remote connection string must be set when step kind is 'SQL'");
        }

        if (step.getKind() == PostgreJobStep.StepKind.b && (step.getTargetDatabase() != null || !CommonUtils.isEmptyTrimmed(step.getRemoteConnectionString()))) {
            throw new DBException("Target database or remote connection string must not be set when step kind is 'Batch'");
        }

        if (CommonUtils.isEmptyTrimmed(step.getObjectDefinitionText(monitor, options))) {
            throw new DBException("Code must not be empty");
        }
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options) throws DBException {
        final PostgreJobStep step = command.getObject();
        final StringBuilder sb = new StringBuilder();

        sb.append("INSERT INTO pgagent.pga_jobstep (jstjobid, jstname, jstdesc, jstenabled, jstkind, jstonerror, jstcode, jstconnstr, jstdbname)\nVALUES (");
        sb.append(step.getParentObject().getObjectId()).append(", ");
        sb.append(SQLUtils.quoteString(step.getDataSource(), step.getName())).append(", ");
        sb.append(SQLUtils.quoteString(step.getDataSource(), step.getDescription())).append(", ");
        sb.append(step.isEnabled()).append(", ");
        sb.append(SQLUtils.quoteString(step.getDataSource(), step.getKind().name())).append(", ");
        sb.append(SQLUtils.quoteString(step.getDataSource(), step.getOnError().name())).append(", ");
        sb.append(SQLUtils.quoteString(step.getDataSource(), step.getObjectDefinitionText(monitor, options))).append(", ");
        sb.append(SQLUtils.quoteString(step.getDataSource(), step.getRemoteConnectionString())).append(", ");
        sb.append(SQLUtils.quoteString(step.getDataSource(), step.getTargetDatabase() == null ? "" : step.getTargetDatabase().getName()));
        sb.append(")");

        actions.add(new SQLDatabasePersistAction(
            "Create step",
            sb.toString()
        ));
    }

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options) throws DBException {
        final PostgreJobStep step = command.getObject();
        actions.add(new SQLDatabasePersistAction(
            "Delete step",
            "DELETE FROM pgagent.pga_jobstep\nWHERE jstid=" + step.getObjectId()
        ));
    }

    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectChangeCommand command, Map<String, Object> options) throws DBException {
        final PostgreJobStep step = command.getObject();
        final StringJoiner values = new StringJoiner(", ");

        if (command.getProperty("description") != null) {
            values.add("jstdesc=" + SQLUtils.quoteString(step.getDataSource(), step.getDescription()));
        }

        if (command.getProperty("kind") != null) {
            values.add("jstkind=" + SQLUtils.quoteString(step.getDataSource(), step.getKind().name()));
        }

        if (command.getProperty("onError") != null) {
            values.add("jstonerror=" + SQLUtils.quoteString(step.getDataSource(), step.getOnError().name()));
        }

        if (command.getProperty("remoteConnectionString") != null) {
            values.add("jstconnstr=" + SQLUtils.quoteString(step.getDataSource(), step.getRemoteConnectionString()));
        }

        if (command.getProperty("targetDatabase") != null) {
            values.add("jstdbname=" + SQLUtils.quoteString(step.getDataSource(), step.getTargetDatabase() == null ? "" : step.getTargetDatabase().getName()));
        }

        if (command.getProperty("enabled") != null) {
            values.add("jsttenabled=" + step.isEnabled());
        }

        if (command.getProperty(DBConstants.PARAM_OBJECT_DEFINITION_TEXT) != null) {
            values.add("jstcode=" + SQLUtils.quoteString(step.getDataSource(), step.getObjectDefinitionText(monitor, options)));
        }

        if (values.length() > 0) {
            actions.add(new SQLDatabasePersistAction(
                "Alter step",
                "UPDATE pgagent.pga_jobstep SET " + values + " WHERE jstid=" + step.getObjectId()
            ));
        }
    }

    @Override
    protected void addObjectRenameActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectRenameCommand command, Map<String, Object> options) {
        final PostgreJobStep step = command.getObject();
        actions.add(new SQLDatabasePersistAction(
            "Rename step",
            "UPDATE pgagent.pga_jobstep SET jstname=" + SQLUtils.quoteString(step.getDataSource(), command.getNewName()) + " WHERE jstid=" + step.getObjectId()
        ));
    }

    @Override
    public void renameObject(@NotNull DBECommandContext commandContext, @NotNull PostgreJobStep object, @NotNull Map<String, Object> options, @NotNull String newName) throws DBException {
        processObjectRename(commandContext, object, options, newName);
    }
}
