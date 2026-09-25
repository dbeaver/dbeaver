/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mimer.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.mimer.model.MimerTableTrigger;
import org.jkiss.dbeaver.ext.mimer.model.MimerUtils;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.util.List;
import java.util.Map;

/**
 * CREATE/DROP for Mimer SQL table and view ({@code INSTEAD OF}) triggers, replacing generic's
 * DROP-only manager - one registration covers both since {@code GenericView extends
 * GenericTableBase} (see {@link MimerTableTrigger}). The create dialog builds a full
 * {@code CREATE TRIGGER ... BEGIN ATOMIC ... END} into the object's source, which {@link
 * #addObjectCreateActions} executes verbatim.
 * <p>
 * Mimer SQL has no {@code ALTER TRIGGER}, so editing an existing trigger drops and recreates it -
 * see {@link #addObjectModifyActions}.
 *
 * @author Mimer Information Technology
 */
public class MimerTableTriggerManager extends SQLObjectEditor<MimerTableTrigger, GenericTableBase> {

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource) {
        return FEATURE_EDITOR_ON_CREATE | FEATURE_DELETE_CASCADE;
    }

    @Override
    public boolean canCreateObject(@NotNull Object container) {
        return DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_METADATA_EDITOR);
    }

    @Override
    public boolean canEditObject(@NotNull MimerTableTrigger object) {
        return true;
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public DBSObjectCache<? extends DBSObject, MimerTableTrigger> getObjectsCache(MimerTableTrigger object) {
        return (DBSObjectCache<? extends DBSObject, MimerTableTrigger>) (DBSObjectCache<?, ?>)
            object.getContainer().getContainer().getTableTriggerCache();
    }

    @NotNull
    @Override
    protected String getBaseObjectName() {
        return "NEW_TRIGGER";
    }

    @Override
    protected MimerTableTrigger createDatabaseObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBECommandContext context,
        @NotNull Object container,
        @Nullable Object copyFrom,
        @NotNull Map<String, Object> options
    ) {
        MimerTableTrigger stub = new MimerTableTrigger((GenericTableBase) container, getBaseObjectName());
        stub.setPersisted(false);
        return stub;
    }

    @Override
    protected void addObjectCreateActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectCreateCommand command,
        @NotNull Map<String, Object> options
    ) throws DBException {
        actions.add(new SQLDatabasePersistAction("Create trigger", command.getObject().getObjectDefinitionText(monitor, options)));
    }

    @Override
    protected void addObjectDeleteActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectDeleteCommand command,
        @NotNull Map<String, Object> options
    ) {
        MimerTableTrigger trigger = command.getObject();
        actions.add(MimerCascadeDropUtil.dropAction("Drop trigger",
            buildDropStatement(trigger), options, "trigger", trigger.getName(), executionContext));
    }

    /**
     * Mimer SQL has no {@code ALTER TRIGGER}, so an edited body drops and recreates the trigger;
     * the DROP half never carries CASCADE.
     */
    @Override
    protected void addObjectModifyActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actionList,
        @NotNull ObjectChangeCommand command,
        @NotNull Map<String, Object> options
    ) throws DBException {
        MimerTableTrigger trigger = command.getObject();
        if (command.hasProperty("objectDefinitionText")) {
            actionList.add(new SQLDatabasePersistAction("Drop trigger", buildDropStatement(trigger)));
            actionList.add(new SQLDatabasePersistAction("Create trigger", trigger.getObjectDefinitionText(monitor, options)));
        }
        if (command.hasProperty("comment")) {
            String name = "\"" + trigger.getTable().getSchema().getName() + "\".\"" + trigger.getName() + "\"";
            actionList.add(new SQLDatabasePersistAction("Comment trigger",
                MimerUtils.buildCommentDDL(trigger, "TRIGGER", name, trigger.getComment(monitor))));
        }
    }

    @NotNull
    private static String buildDropStatement(@NotNull MimerTableTrigger trigger) {
        // The optional CASCADE is appended by MimerCascadeDropUtil when the delete-confirmation
        // "Cascade" box is ticked; the drop-recreate edit path never cascades.
        return "DROP TRIGGER \"" + trigger.getTable().getSchema().getName() + "\".\"" + trigger.getName() + "\"";
    }
}
