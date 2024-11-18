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
package org.jkiss.dbeaver.ext.kingbase.edit;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseClass;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseMaterializedView;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseSchema;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseTableBase;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseTableColumn;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseTableContainer;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseView;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseViewBase;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

/**
 * KingbaseViewManager
 */
public class KingbaseViewManager extends KingbaseTableManagerBase implements DBEObjectRenamer<KingbaseTableBase> {

    private static final Class<? extends DBSObject>[] CHILD_TYPES = CommonUtils.array(
        KingbaseTableColumn.class);

    @NotNull
    @Override
    public Class<? extends DBSObject>[] getChildTypes() {
        return CHILD_TYPES;
    }

    @Nullable
    @Override
    public DBSObjectCache<KingbaseTableContainer, KingbaseTableBase> getObjectsCache(KingbaseTableBase object) {
        return object.getContainer().getSchema().getTableCache();
    }

    @Override
    protected void validateObjectProperties(DBRProgressMonitor monitor, ObjectChangeCommand command, Map<String, Object> options)
        throws DBException
    {
        KingbaseTableBase object = command.getObject();
        if (CommonUtils.isEmpty(object.getName())) {
            throw new DBException("View name cannot be empty");
        }
    }

    @Override
    protected String getBaseObjectName() {
        return SQLTableManager.BASE_VIEW_NAME;
    }

    @Override
    protected KingbaseViewBase createDatabaseObject(@NotNull DBRProgressMonitor monitor, @NotNull DBECommandContext context, Object container, Object copyFrom, @NotNull Map<String, Object> options) throws DBException {
        KingbaseSchema schema = (KingbaseSchema) container;
        KingbaseView newView = (KingbaseView) schema.getDataSource().getServerType().createNewRelation(
            monitor, schema, KingbaseClass.RelKind.v, null);
        setNewObjectName(monitor, schema, newView);
        return newView;
    }

    @Override
    protected void addStructObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, StructCreateCommand command, Map<String, Object> options) throws DBException {
        if (!command.hasProperty(DBConstants.PROP_ID_DESCRIPTION) || command.getProperties().size() > 1) {
            createOrReplaceViewQuery(monitor, actions, (KingbaseViewBase) command.getObject(), options);
        }
    }

    @Override
    protected void addObjectModifyActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actionList, @NotNull ObjectChangeCommand command, @NotNull Map<String, Object> options) throws DBException {
        if (!command.hasProperty(DBConstants.PROP_ID_DESCRIPTION) || command.getProperties().size() > 1) {
            createOrReplaceViewQuery(monitor, actionList, (KingbaseViewBase) command.getObject(), options);
        }
    }

    @Override
    protected void addObjectDeleteActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectDeleteCommand command, @NotNull Map<String, Object> options) {
        KingbaseViewBase view = (KingbaseViewBase) command.getObject();
        actions.add(
            new SQLDatabasePersistAction(
                "Drop view",
                "DROP " + view.getTableTypeName() +
                    " " + view.getFullyQualifiedName(DBPEvaluationContext.DDL) +
                    (CommonUtils.getOption(options, OPTION_DELETE_CASCADE) ? " CASCADE" : ""))
        );
    }

    protected void createOrReplaceViewQuery(DBRProgressMonitor monitor, List<DBEPersistAction> actions, KingbaseViewBase view, Map<String, Object> options) throws DBException {
        // Source may be empty if it wasn't yet read. Then it definitely wasn't changed
        String sql = view.getObjectDefinitionText(monitor, Map.of());
        if (!sql.toLowerCase(Locale.ENGLISH).contains("create")) {
            StringBuilder sqlBuf = new StringBuilder();
            sqlBuf.append("CREATE ");
            if (!(view instanceof KingbaseMaterializedView)) {
                sqlBuf.append("OR REPLACE ");
            }
            sqlBuf.append(view.getTableTypeName()).append(" ").append(DBUtils.getObjectFullName(view, DBPEvaluationContext.DDL));
            appendViewDeclarationPrefix(monitor, sqlBuf, view);
            sqlBuf.append("\nAS ").append(sql);
            appendViewDeclarationPostfix(monitor, sqlBuf, view);
            sql = sqlBuf.toString();
        }
        actions.add(
            new SQLDatabasePersistAction("Create view", sql));
    }

    public void appendViewDeclarationPrefix(DBRProgressMonitor monitor, StringBuilder sqlBuf, KingbaseViewBase view) throws DBException {
        String[] relOptions = view.getRelOptions();
        if (!ArrayUtils.isEmpty(relOptions)) {
            sqlBuf.append("\nWITH(").append(String.join(",", relOptions)).append(")");
        }
    }

    public void appendViewDeclarationPostfix(DBRProgressMonitor monitor, StringBuilder sqlBuf, KingbaseViewBase view) {

    }

    @Override
    public void renameObject(@NotNull DBECommandContext commandContext, @NotNull KingbaseTableBase object, @NotNull Map<String, Object> options, @NotNull String newName) throws DBException {
        processObjectRename(commandContext, object, options, newName);
    }

    @Override
    protected void addObjectRenameActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectRenameCommand command, @NotNull Map<String, Object> options) {
        KingbaseViewBase view = (KingbaseViewBase) command.getObject();
        String tableType;
        if (view.getDataSource().getServerType().supportsAlterTableForViewRename()) {
            tableType = "TABLE"; //$NON-NLS-1$
        } else {
            tableType = view.getTableTypeName();
        }
        actions.add(
            new SQLDatabasePersistAction(
                "Rename view",
                "ALTER " + tableType + " " + DBUtils.getQuotedIdentifier(view.getSchema()) //$NON-NLS-1$
                    + "." + DBUtils.getQuotedIdentifier(view.getDataSource(), command.getOldName()) +
                    " RENAME TO " + DBUtils.getQuotedIdentifier(view.getDataSource(), command.getNewName())) //$NON-NLS-1$
        );
    }

    @Override
    protected void addObjectExtraActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull NestedObjectCommand<KingbaseTableBase, PropertyHandler> command, @NotNull Map<String, Object> options) {
        KingbaseViewBase viewBase = (KingbaseViewBase) command.getObject();
        if (command.hasProperty(DBConstants.PROP_ID_DESCRIPTION)) {
            actions.add(new SQLDatabasePersistAction(
                "Comment view",
                "COMMENT ON " + viewBase.getTableTypeName() + " " + viewBase.getFullyQualifiedName(DBPEvaluationContext.DDL) +
                    " IS " + SQLUtils.quoteString(viewBase, CommonUtils.notEmpty(viewBase.getDescription()))));
        }
    }

}

