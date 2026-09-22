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
import org.jkiss.dbeaver.ext.generic.model.GenericObjectContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.mimer.model.MimerProcedure;
import org.jkiss.dbeaver.ext.mimer.model.MimerUtils;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectManager;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeItem;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNode;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.struct.cache.ListCache;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.util.List;
import java.util.Map;

/**
 * CREATE/DROP/edit for Mimer SQL procedures and functions. The "Procedures" and "Functions" tree
 * folders both bind to {@link MimerProcedure} (distinguished only by {@link DBSProcedureType});
 * {@link #detectProcedureType} pre-seeds the create dialog's Type combo from whichever folder
 * was actually clicked, since Mimer SQL (unlike Oracle/PostgreSQL) has a separate Functions folder.
 * <p>
 * Mimer SQL has no {@code ALTER FUNCTION}/{@code ALTER PROCEDURE}, so editing an existing routine's
 * body drops and recreates it - see {@link #addObjectModifyActions}. Not used for a module's own
 * routines - see {@link MimerModuleRoutine}, a separate, non-editable class for those.
 *
 * @author Mimer Information Technology
 */
public class MimerProcedureManager extends SQLObjectEditor<MimerProcedure, GenericStructContainer> {

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource) {
        return FEATURE_EDITOR_ON_CREATE | FEATURE_DELETE_CASCADE;
    }

    @Override
    public boolean canCreateObject(@NotNull Object container) {
        return DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_METADATA_EDITOR);
    }

    @Override
    public boolean canEditObject(@NotNull MimerProcedure object) {
        return true;
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public DBSObjectCache<? extends DBSObject, MimerProcedure> getObjectsCache(MimerProcedure object) {
        GenericStructContainer container = object.getContainer();
        List<MimerProcedure> procedures = (List<MimerProcedure>) (List<?>) ((GenericObjectContainer) container).getProcedureCache();
        return new ListCache<>(procedures);
    }

    @Override
    protected MimerProcedure createDatabaseObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBECommandContext context,
        @NotNull Object container,
        @Nullable Object copyFrom,
        @NotNull Map<String, Object> options
    ) {
        GenericStructContainer structContainer = (GenericStructContainer) container;
        MimerProcedure stub = new MimerProcedure(
            structContainer, "NEW_PROCEDURE", null, null, detectProcedureType(options), null);
        // The configurator dialog edits this same stub in place and the CREATE command
        // executes it - see MimerProcedure's Javadoc for why it must stay one instance.
        stub.setPersisted(false);
        return stub;
    }

    /**
     * Reads the actually-clicked {@code DBNDatabaseFolder} from {@link
     * DBEObjectManager#OPTION_CONTAINER} and its {@code <items property="...">} binding
     * ({@code proceduresOnly} vs {@code functionsOnly}) so "Create New Function" pre-selects
     * Function even though the two folders share one Java class. Falls back to {@code PROCEDURE}
     * if the folder can't be identified.
     */
    @NotNull
    private static DBSProcedureType detectProcedureType(@NotNull Map<String, Object> options) {
        if (options.get(DBEObjectManager.OPTION_CONTAINER) instanceof DBNDatabaseFolder folder) {
            for (DBXTreeNode child : folder.getMeta().getChildren(folder)) {
                if (child instanceof DBXTreeItem item && "functionsOnly".equals(item.getPropertyName())) {
                    return DBSProcedureType.FUNCTION;
                }
            }
        }
        return DBSProcedureType.PROCEDURE;
    }

    @Override
    protected void addObjectCreateActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectCreateCommand command,
        @NotNull Map<String, Object> options
    ) {
        String title = command.getObject().getProcedureType() == DBSProcedureType.FUNCTION ? "Create function" : "Create procedure";
        actions.add(new SQLDatabasePersistAction(title, command.getObject().getSource()));
    }

    @Override
    protected void addObjectDeleteActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull ObjectDeleteCommand command,
        @NotNull Map<String, Object> options
    ) {
        MimerProcedure object = command.getObject();
        actions.add(MimerCascadeDropUtil.dropAction(
            "Drop " + object.getProcedureType().name().toLowerCase(),
            buildDropSql(object), options,
            object.getProcedureType().name().toLowerCase(), object.getName(), executionContext));
    }

    /**
     * Mimer SQL has no {@code ALTER FUNCTION}/{@code ALTER PROCEDURE}, so editing an existing
     * routine's body drops and recreates it. Each branch fires only when that property actually
     * changed.
     */
    @Override
    protected void addObjectModifyActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actionList,
        @NotNull ObjectChangeCommand command,
        @NotNull Map<String, Object> options
    ) throws DBException {
        MimerProcedure object = command.getObject();
        if (command.hasProperty("objectDefinitionText")) {
            actionList.add(new SQLDatabasePersistAction(ModelMessages.model_jdbc_drop_table, buildDropSql(object)));
            String title = object.getProcedureType() == DBSProcedureType.FUNCTION ? "Create function" : "Create procedure";
            actionList.add(new SQLDatabasePersistAction(title, object.getObjectDefinitionText(monitor, options)));
        }
        if (command.hasProperty("comment")) {
            String keyword = object.getProcedureType() == DBSProcedureType.FUNCTION ? "SPECIFIC FUNCTION" : "SPECIFIC PROCEDURE";
            String name = "\"" + object.getContainer().getName() + "\".\"" + object.getUniqueName() + "\"";
            actionList.add(new SQLDatabasePersistAction("Comment " + object.getProcedureType().name().toLowerCase(),
                MimerUtils.buildCommentDDL(object, keyword, name, object.getComment(monitor))));
        }
    }

    /**
     * {@code DROP SPECIFIC PROCEDURE|FUNCTION "<schema>"."<specific-name>"}. A plain {@code DROP PROCEDURE
     * "<schema>"."<name>"} fails if a second overload exists.
     * Mimer SQL always assigns a real, unique specific name (auto-generated if the user never set
     * one - see {@link MimerProcedure#getUniqueName}), so there's no reason to fall back to the
     * plain, ambiguity-prone form even for a non-overloaded routine - this matches the same
     * "SPECIFIC" convention already used below for {@code COMMENT ON SPECIFIC PROCEDURE/FUNCTION}
     * and by {@code DROP SPECIFIC METHOD} on a UDT method. No {@code CASCADE} here; the delete
     * flow adds it via {@link MimerCascadeDropUtil} when the "Cascade" box is ticked.
     */
    @NotNull
    private String buildDropSql(@NotNull MimerProcedure object) {
        String keyword = "SPECIFIC " + object.getProcedureType().name();
        String name = "\"" + object.getContainer().getName() + "\".\"" + object.getUniqueName() + "\"";
        return "DROP " + keyword + " " + name;
    }
}
