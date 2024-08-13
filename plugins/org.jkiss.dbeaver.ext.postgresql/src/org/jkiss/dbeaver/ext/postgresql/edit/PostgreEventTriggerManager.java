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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreEventTrigger;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
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
 * PostgreEventTriggerManager
 */
public class PostgreEventTriggerManager extends SQLTriggerManager<PostgreEventTrigger, PostgreDatabase> implements DBEObjectRenamer<PostgreEventTrigger> {

    @Override
    public boolean canCreateObject(@NotNull Object container) {
        return true;
    }

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource) {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    public DBSObjectCache<? extends DBSObject, PostgreEventTrigger> getObjectsCache(PostgreEventTrigger object) {
        return object.getDatabase().getEventTriggersCache();
    }

    @Override
    protected PostgreEventTrigger createDatabaseObject(@NotNull DBRProgressMonitor monitor, @NotNull DBECommandContext context, Object container, Object copyFrom, @NotNull Map<String, Object> options) {
        return new PostgreEventTrigger((PostgreDatabase) container, "new_event_trigger");
    }

    @Override
    protected void addObjectModifyActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectChangeCommand command, @NotNull Map<String, Object> options) {
        if (command.getProperties().size() > 1 || command.getProperty(DBConstants.PROP_ID_DESCRIPTION) == null) {
            createOrReplaceTriggerQuery(monitor, executionContext, actions, command.getObject(), false);
        }
    }

    @Override
    protected void addObjectExtraActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull NestedObjectCommand<PostgreEventTrigger, PropertyHandler> command, @NotNull Map<String, Object> options) {
        if (command.hasProperty(DBConstants.PROP_ID_DESCRIPTION)) {
            PostgreEventTrigger trigger = command.getObject();
            actions.add(new SQLDatabasePersistAction(
                "Comment event trigger",
                "COMMENT ON EVENT TRIGGER " + DBUtils.getQuotedIdentifier(trigger) +
                    " IS " + SQLUtils.quoteString(trigger, CommonUtils.notEmpty(trigger.getDescription()))));
        }
    }

    @Override
    protected void createOrReplaceTriggerQuery(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull PostgreEventTrigger trigger, boolean create) {
        if (trigger.isPersisted()) {
            actions.add(new SQLDatabasePersistAction(
                "Drop event trigger",
                "DROP EVENT TRIGGER IF EXISTS " + DBUtils.getQuotedIdentifier(trigger)
            ));
        }

        actions.add(new SQLDatabasePersistAction("Create trigger", trigger.getBody()));
    }

    @Override
    protected void addObjectDeleteActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectDeleteCommand command, @NotNull Map<String, Object> options) {
        actions.add(new SQLDatabasePersistAction(
            "Drop event trigger",
            "DROP EVENT TRIGGER " + DBUtils.getQuotedIdentifier(command.getObject())
        ));
    }

    @Override
    public void renameObject(@NotNull DBECommandContext commandContext, @NotNull PostgreEventTrigger object, @NotNull Map<String, Object> options, @NotNull String newName) throws DBException {
        processObjectRename(commandContext, object, options, newName);
    }

    @Override
    protected void addObjectRenameActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectRenameCommand command, @NotNull Map<String, Object> options) {
        PostgreDataSource dataSource = command.getObject().getDataSource();
        actions.add(new SQLDatabasePersistAction(
            "Rename event trigger",
            "ALTER EVENT TRIGGER " + DBUtils.getQuotedIdentifier(dataSource, command.getOldName()) + " RENAME TO " + DBUtils.getQuotedIdentifier(dataSource, command.getNewName())
        ));
    }
}
