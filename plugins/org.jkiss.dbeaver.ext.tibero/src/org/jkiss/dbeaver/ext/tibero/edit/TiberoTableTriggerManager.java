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
package org.jkiss.dbeaver.ext.tibero.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.tibero.TiberoConstants;
import org.jkiss.dbeaver.ext.tibero.model.TiberoTableBase;
import org.jkiss.dbeaver.ext.tibero.model.TiberoTableTrigger;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

public class TiberoTableTriggerManager extends SQLObjectEditor<TiberoTableTrigger, TiberoTableBase> {

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource) {
        return 0;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, TiberoTableTrigger> getObjectsCache(TiberoTableTrigger object) {
        return object.getTable().getContainer().tableTriggerCache;
    }

    @Override
    protected TiberoTableTrigger createDatabaseObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBECommandContext context,
        @NotNull Object container,
        @Nullable Object copyFrom,
        @NotNull Map<String, Object> options
    ) {
        throw new UnsupportedOperationException("Tibero trigger creation through navigator is not supported yet");
    }

    @Override
    protected void addObjectCreateActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectCreateCommand objectCreateCommand,
        @NotNull Map<String, Object> options
    ) {
        addCreateOrReplaceAction(actions, objectCreateCommand.getObject());
    }

    @Override
    protected void addObjectDeleteActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectDeleteCommand objectDeleteCommand,
        @NotNull Map<String, Object> options
    ) {
        TiberoTableTrigger object = objectDeleteCommand.getObject();
        actions.add(new SQLDatabasePersistAction(
            "Drop trigger",
            "DROP TRIGGER " + object.getFullyQualifiedName(DBPEvaluationContext.DDL)
        ));
    }

    @Override
    protected void addObjectModifyActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectChangeCommand objectChangeCommand,
        @NotNull Map<String, Object> options
    ) {
        if (objectChangeCommand.hasProperty(TiberoConstants.PROP_OBJECT_DEFINITION)) {
            addCreateOrReplaceAction(actions, objectChangeCommand.getObject());
        }
    }

    private void addCreateOrReplaceAction(@NotNull List<DBEPersistAction> actions, @NotNull TiberoTableTrigger trigger) {
        try {
            String source = trigger.getObjectDefinitionText(new VoidProgressMonitor(), DBPScriptObject.EMPTY_OPTIONS);
            if (CommonUtils.isEmpty(source)) {
                return;
            }
            actions.add(new SQLDatabasePersistAction("Create or replace trigger", source.trim()));
        } catch (DBException e) {
            log.warn(e);
        }
    }
}
