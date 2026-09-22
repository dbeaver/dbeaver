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
import org.jkiss.dbeaver.ext.mimer.model.MimerModule;
import org.jkiss.dbeaver.ext.mimer.model.MimerSchema;
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
 * CREATE/DROP support for Mimer SQL modules. Mimer SQL has no {@code ALTER MODULE}, so editing an
 * existing module's body drops and recreates it (the drop half never carries {@code CASCADE},
 * unlike a plain delete via the stock delete-confirmation checkbox). {@link
 * #addObjectCreateActions} executes {@link MimerModule#getObjectDefinitionText} verbatim - the
 * configurator dialog seeds it with a one-routine template, finished via the Source tab.
 *
 * @author Mimer Information Technology
 */
public class MimerModuleManager extends SQLObjectEditor<MimerModule, MimerSchema> {

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource) {
        return FEATURE_EDITOR_ON_CREATE | FEATURE_DELETE_CASCADE;
    }

    @Override
    public boolean canCreateObject(@NotNull Object container) {
        return DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_METADATA_EDITOR);
    }

    @Override
    public boolean canEditObject(@NotNull MimerModule object) {
        return true;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, MimerModule> getObjectsCache(MimerModule object) {
        return object.getSchema().getModuleCache();
    }

    @NotNull
    @Override
    protected String getBaseObjectName() {
        return "NEW_MODULE";
    }

    @Override
    protected MimerModule createDatabaseObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBECommandContext context,
        @NotNull Object container,
        @Nullable Object copyFrom,
        @NotNull Map<String, Object> options
    ) {
        return new MimerModule((MimerSchema) container, getBaseObjectName());
    }

    @Override
    protected void addObjectCreateActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectCreateCommand command,
        @NotNull Map<String, Object> options
    ) throws DBException {
        actions.add(new SQLDatabasePersistAction("Create module", command.getObject().getObjectDefinitionText(monitor, options)));
    }

    @Override
    protected void addObjectDeleteActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectDeleteCommand command,
        @NotNull Map<String, Object> options
    ) {
        MimerModule module = command.getObject();
        actions.add(MimerCascadeDropUtil.dropAction("Drop module",
            buildDropStatement(module), options, "module", module.getName(), executionContext));
    }

    /**
     * See the class Javadoc - Mimer SQL has no {@code ALTER MODULE}, so editing an existing module's
     * body means dropping and recreating it. Fires only when the body actually changed.
     */
    @Override
    protected void addObjectModifyActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actionList,
        @NotNull ObjectChangeCommand command,
        @NotNull Map<String, Object> options
    ) throws DBException {
        MimerModule module = command.getObject();
        if (command.hasProperty("objectDefinitionText")) {
            actionList.add(new SQLDatabasePersistAction("Drop module", buildDropStatement(module)));
            actionList.add(new SQLDatabasePersistAction("Create module", module.getObjectDefinitionText(monitor, options)));
        }
        if (command.hasProperty("comment")) {
            String name = "\"" + module.getSchema().getName() + "\".\"" + module.getName() + "\"";
            actionList.add(new SQLDatabasePersistAction("Comment module",
                MimerUtils.buildCommentDDL(module, "MODULE", name, module.getComment(monitor))));
        }
    }

    @NotNull
    private static String buildDropStatement(@NotNull MimerModule module) {
        // The optional CASCADE is appended by MimerCascadeDropUtil when the delete-confirmation
        // "Cascade" box is ticked; the drop-recreate edit path never cascades.
        return "DROP MODULE \"" + module.getSchema().getName() + "\".\"" + module.getName() + "\"";
    }
}
