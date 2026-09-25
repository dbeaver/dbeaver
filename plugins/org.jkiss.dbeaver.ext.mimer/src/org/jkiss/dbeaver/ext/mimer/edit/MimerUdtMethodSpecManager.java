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
import org.jkiss.dbeaver.ext.mimer.model.MimerUdtMethodSpec;
import org.jkiss.dbeaver.ext.mimer.model.MimerUserDefinedType;
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
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * CREATE/edit/DROP for the first, signature-only phase of a Mimer SQL user-defined type method -
 * see {@link MimerUdtMethodSpec}'s Javadoc for the full two-phase picture. The "Constructor/
 * Instance/Static Methods" tree folders (mixed with real, defined {@code MimerUdtMethod} rows -
 * see {@link MimerUserDefinedType#getConstructorMethodsAndSpecs}/etc.) all bind to this class;
 * {@link #detectMethodKind} pre-seeds the create dialog's kind from whichever folder was actually
 * clicked, same "one class, folder detection" pattern as {@link
 * MimerProcedureManager#detectProcedureType}. {@link #getMakerOptions} opens the Source tab
 * immediately after creation ({@code FEATURE_EDITOR_ON_CREATE}) so declaring the signature and
 * writing the body read as one smooth flow, even though under the hood they're two separate DDL
 * statements ("Add Method Specification" then "Create Method").
 *
 * @author Mimer Information Technology
 */
public class MimerUdtMethodSpecManager extends SQLObjectEditor<MimerUdtMethodSpec, MimerUserDefinedType> {

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource) {
        return FEATURE_EDITOR_ON_CREATE | FEATURE_DELETE_CASCADE;
    }

    @Override
    public boolean canCreateObject(@NotNull Object container) {
        return DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_METADATA_EDITOR);
    }

    @Override
    public boolean canEditObject(@NotNull MimerUdtMethodSpec object) {
        return true;
    }

    @Nullable
    @Override
    public DBSObjectCache<MimerUserDefinedType, MimerUdtMethodSpec> getObjectsCache(MimerUdtMethodSpec object) {
        return object.getType().getMethodSpecCache();
    }

    @NotNull
    @Override
    protected String getBaseObjectName() {
        return "NEW_METHOD";
    }

    @Override
    protected MimerUdtMethodSpec createDatabaseObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBECommandContext context,
        @NotNull Object container,
        @Nullable Object copyFrom,
        @NotNull Map<String, Object> options
    ) {
        MimerUserDefinedType type = (MimerUserDefinedType) container;
        String kind = detectMethodKind(options);
        String name = "CONSTRUCTOR METHOD".equals(kind) ? type.getName() : getBaseObjectName();
        return new MimerUdtMethodSpec(type, kind, name);
    }

    /**
     * Reads the actually-clicked {@code DBNDatabaseFolder} from {@link
     * DBEObjectManager#OPTION_CONTAINER} and its {@code <items property="...">} binding so
     * "Create New" pre-selects the right kind even though all three folders share one Java class
     * - see {@link MimerProcedureManager#detectProcedureType} for the identical trick. Falls back
     * to {@code INSTANCE METHOD} if the folder can't be identified.
     */
    @NotNull
    private static String detectMethodKind(@NotNull Map<String, Object> options) {
        if (options.get(DBEObjectManager.OPTION_CONTAINER) instanceof DBNDatabaseFolder folder) {
            for (DBXTreeNode child : folder.getMeta().getChildren(folder)) {
                if (child instanceof DBXTreeItem item) {
                    switch (CommonUtils.notEmpty(item.getPropertyName())) {
                        case "constructorMethodsAndSpecs":
                            return "CONSTRUCTOR METHOD";
                        case "staticMethodsAndSpecs":
                            return "STATIC METHOD";
                        default:
                            // Not a recognized folder property - keep looking at the other children.
                            break;
                    }
                }
            }
        }
        return "INSTANCE METHOD";
    }

    /**
     * Emits both DDL statements together, not just the {@code ADD METHOD} spec. {@code
     * FEATURE_EDITOR_ON_CREATE} keeps the object as one still-uncommitted {@code
     * ObjectCreateCommand} for the entire time the auto-opened Source tab is being edited - the
     * Source tab's own Save doesn't trigger a separate persist of an already-existing object
     * (which would route through {@link #addObjectModifyActions} instead), it just mutates
     * {@link MimerUdtMethodSpec#setObjectDefinitionText} on the same still-pending object, and
     * the one eventual Save/Execute the user sees runs only this method. By the time it runs, the
     * Source tab has already mutated {@code command.getObject()}'s source field, so {@link
     * MimerUdtMethodSpec#buildCreateBodyDDL} reflects the real, edited body rather than the
     * placeholder.
     */
    @Override
    protected void addObjectCreateActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectCreateCommand command,
        @NotNull Map<String, Object> options
    ) {
        MimerUdtMethodSpec spec = command.getObject();
        actions.add(new SQLDatabasePersistAction("Add method specification", spec.buildAddSpecificationDDL()));
        actions.add(new SQLDatabasePersistAction("Create method", spec.buildCreateBodyDDL()));
    }

    @Override
    protected void addObjectDeleteActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectDeleteCommand command,
        @NotNull Map<String, Object> options
    ) {
        boolean cascade = MimerCascadeDropUtil.isCascade(options);
        actions.add(MimerCascadeDropUtil.confirmedDrop("Drop method specification",
            command.getObject().buildDropDDL(cascade), cascade, "method specification", command.getObject().getName(), executionContext));
    }

    /**
     * Supplies the body for an already-declared specification - {@code CREATE SPECIFIC METHOD
     * "s"."specificName" BEGIN ... END}, fired via the Source tab's Save (see {@link
     * MimerUdtMethodSpec#getObjectDefinitionText}). Once this runs, the spec has a matching
     * {@code ROUTINES} row and "graduates" to a real {@link org.jkiss.dbeaver.ext.mimer.model.MimerUdtMethod}
     * on next refresh - this manager has no further use for the object after that.
     */
    @Override
    protected void addObjectModifyActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actionList,
        @NotNull ObjectChangeCommand command,
        @NotNull Map<String, Object> options
    ) {
        if (command.hasProperty("objectDefinitionText")) {
            actionList.add(new SQLDatabasePersistAction("Create method", command.getObject().buildCreateBodyDDL()));
        }
    }
}
