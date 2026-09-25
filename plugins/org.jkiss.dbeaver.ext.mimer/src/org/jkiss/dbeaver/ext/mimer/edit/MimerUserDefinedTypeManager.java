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
import org.jkiss.dbeaver.ext.mimer.model.MimerSchema;
import org.jkiss.dbeaver.ext.mimer.model.MimerUserDefinedType;
import org.jkiss.dbeaver.ext.mimer.model.MimerUtils;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectManager;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeItem;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNode;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.util.List;
import java.util.Map;

/**
 * CREATE/DROP support for Mimer SQL user-defined types. The "Distinct Types" and "Structured
 * Types" tree folders both bind to {@link MimerUserDefinedType} (distinguished only by {@link
 * MimerUserDefinedType#getCategory}); {@link #detectCategory} pre-seeds a new type's category
 * from whichever folder was actually clicked, same "one class, folder detection" pattern as
 * {@link MimerProcedureManager#detectProcedureType}. No ALTER beyond Comment - Mimer SQL has no
 * {@code ALTER TYPE} clause for changing a distinct type's base type or a structured type's
 * attribute list wholesale (only {@code ADD}/{@code DROP ATTRIBUTE}, not modeled here). {@code
 * DROP TYPE} supports an optional {@code CASCADE} (default {@code RESTRICT}) via the stock
 * delete-confirmation checkbox, same {@code FEATURE_DELETE_CASCADE} mechanism used elsewhere.
 *
 * @author Mimer Information Technology
 */
public class MimerUserDefinedTypeManager extends SQLObjectEditor<MimerUserDefinedType, MimerSchema> {

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource) {
        return FEATURE_SAVE_IMMEDIATELY | FEATURE_DELETE_CASCADE;
    }

    @Override
    public boolean canCreateObject(@NotNull Object container) {
        return DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_METADATA_EDITOR);
    }

    @Override
    public boolean canEditObject(@NotNull MimerUserDefinedType object) {
        return true;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, MimerUserDefinedType> getObjectsCache(MimerUserDefinedType object) {
        return object.getSchema().getUserDefinedTypeCache();
    }

    @NotNull
    @Override
    protected String getBaseObjectName() {
        return "NEW_TYPE";
    }

    @Override
    protected MimerUserDefinedType createDatabaseObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBECommandContext context,
        @NotNull Object container,
        @Nullable Object copyFrom,
        @NotNull Map<String, Object> options
    ) {
        return new MimerUserDefinedType((MimerSchema) container, detectCategory(options), getBaseObjectName());
    }

    /**
     * Reads the actually-clicked {@code DBNDatabaseFolder} from {@link
     * DBEObjectManager#OPTION_CONTAINER} and its {@code <items property="...">} binding ({@code
     * structuredTypes} vs {@code distinctTypes}) so "Create New Structured Type" pre-selects that
     * category even though the two folders share one Java class - see {@link
     * MimerProcedureManager#detectProcedureType} for the identical trick. Falls back to {@code
     * DISTINCT} if the folder can't be identified.
     */
    @NotNull
    private static String detectCategory(@NotNull Map<String, Object> options) {
        if (options.get(DBEObjectManager.OPTION_CONTAINER) instanceof DBNDatabaseFolder folder) {
            for (DBXTreeNode child : folder.getMeta().getChildren(folder)) {
                if (child instanceof DBXTreeItem item && "structuredTypes".equals(item.getPropertyName())) {
                    return "STRUCTURED";
                }
            }
        }
        return "DISTINCT";
    }

    @Override
    protected void addObjectCreateActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectCreateCommand command,
        @NotNull Map<String, Object> options
    ) {
        actions.add(new SQLDatabasePersistAction("Create type", command.getObject().buildCreateDDL()));
    }

    @Override
    protected void addObjectDeleteActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectDeleteCommand command,
        @NotNull Map<String, Object> options
    ) {
        MimerUserDefinedType type = command.getObject();
        boolean cascade = MimerCascadeDropUtil.isCascade(options);
        actions.add(MimerCascadeDropUtil.confirmedDrop("Drop type",
            "DROP TYPE \"" + type.getSchema().getName() + "\".\"" + type.getName() + "\"" + (cascade ? " CASCADE" : " RESTRICT"),
            cascade, "type", type.getName(), executionContext));
    }

    @Override
    protected void addObjectModifyActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actionList,
        @NotNull ObjectChangeCommand command,
        @NotNull Map<String, Object> options
    ) throws DBException {
        if (!command.hasProperty("comment")) {
            return;
        }
        MimerUserDefinedType type = command.getObject();
        String name = "\"" + type.getSchema().getName() + "\".\"" + type.getName() + "\"";
        actionList.add(new SQLDatabasePersistAction("Comment type",
            MimerUtils.buildCommentDDL(type, "TYPE", name, type.getComment(monitor))));
    }
}
