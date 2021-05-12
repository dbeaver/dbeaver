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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableReal;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTrigger;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTriggerManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * PostgreTriggerManager
 */
public class PostgreTriggerManager extends SQLTriggerManager<PostgreTrigger, PostgreTableReal> implements DBEObjectRenamer<PostgreTrigger> {//implements DBEObjectRenamer<PostgreTrigger> {

    @Override
    public boolean canCreateObject(Object container) {
        return true;
    }

    @Override
    public long getMakerOptions(DBPDataSource dataSource) {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    public DBSObjectCache<? extends DBSObject, PostgreTrigger> getObjectsCache(PostgreTrigger object) {
        return object.getParentObject().getTriggerCache();
    }

    @Override
    protected PostgreTrigger createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, Object container, Object copyFrom, Map<String, Object> options) throws DBException
    {
        return new PostgreTrigger(monitor, (PostgreTableReal) container);
    }

    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectChangeCommand command, Map<String, Object> options) {
        if (command.getProperties().size() > 1 || command.getProperty(DBConstants.PROP_ID_DESCRIPTION) == null) {
            createOrReplaceTriggerQuery(monitor, executionContext, actions, command.getObject(), false);
        }
    }

    @Override
    protected void addObjectExtraActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, NestedObjectCommand<PostgreTrigger, PropertyHandler> command, Map<String, Object> options) throws DBException {
        if (command.hasProperty(DBConstants.PROP_ID_DESCRIPTION)) {
            actions.add(new SQLDatabasePersistAction(
                "Comment trigger",
                "COMMENT ON TRIGGER " + DBUtils.getQuotedIdentifier(command.getObject()) + " ON " + command.getObject().getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) +
                    " IS " + SQLUtils.quoteString(command.getObject(), CommonUtils.notEmpty(command.getObject().getDescription()))));
        }
    }

    @Override
    protected void createOrReplaceTriggerQuery(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, PostgreTrigger trigger, boolean create) {
        if (trigger.isPersisted()) {
            actions.add(new SQLDatabasePersistAction(
                "Drop trigger",
                "DROP TRIGGER IF EXISTS " + DBUtils.getQuotedIdentifier(trigger) + " ON " + trigger.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL)
            ));
        }

        actions.add(new SQLDatabasePersistAction("Create trigger", trigger.getBody()));
    }

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options) {
        actions.add(new SQLDatabasePersistAction(
                "Drop trigger",
                "DROP TRIGGER " + DBUtils.getQuotedIdentifier(command.getObject()) + " ON " + command.getObject().getTable().getFullyQualifiedName(DBPEvaluationContext.DDL)
        ));
    }

    @Override
    public void renameObject(@NotNull DBECommandContext commandContext, @NotNull PostgreTrigger object, @NotNull Map<String, Object> options, @NotNull String newName) throws DBException {
        processObjectRename(commandContext, object, options, newName);
    }

    @Override
    protected void addObjectRenameActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectRenameCommand command, Map<String, Object> options) {
        actions.add(new SQLDatabasePersistAction(
            "Rename trigger",
            "ALTER TRIGGER " + DBUtils.getQuotedIdentifier(command.getObject()) + " ON " + command.getObject().getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) + " RENAME TO " + DBUtils.getQuotedIdentifier(command.getObject().getDataSource(), command.getNewName())
        ));
    }
}
