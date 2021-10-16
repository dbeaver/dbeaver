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
import org.jkiss.dbeaver.ext.postgresql.model.*;
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
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * PostgreViewManager
 */
public class PostgreViewManager extends PostgreTableManagerBase implements DBEObjectRenamer<PostgreTableBase> {

    private static final Class<?>[] CHILD_TYPES = {
        PostgreTableColumn.class,
    };

    @Override
    public Class<?>[] getChildTypes() {
        return CHILD_TYPES;
    }

    @Nullable
    @Override
    public DBSObjectCache<PostgreTableContainer, PostgreTableBase> getObjectsCache(PostgreTableBase object)
    {
        return object.getContainer().getSchema().getTableCache();
    }

    @Override
    protected void validateObjectProperties(DBRProgressMonitor monitor, ObjectChangeCommand command, Map<String, Object> options)
        throws DBException
    {
        PostgreTableBase object = command.getObject();
        if (CommonUtils.isEmpty(object.getName())) {
            throw new DBException("View name cannot be empty");
        }
    }

    @Override
    protected String getBaseObjectName() {
        return SQLTableManager.BASE_VIEW_NAME;
    }

    @Override
    protected PostgreViewBase createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, Object container, Object copyFrom, Map<String, Object> options)
    {
        PostgreSchema schema = (PostgreSchema) container;
        PostgreView newView = new PostgreView(schema);
        setNewObjectName(monitor, schema, newView);
        return newView;
    }

    @Override
    protected void addStructObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, StructCreateCommand command, Map<String, Object> options) throws DBException {
        if (!command.hasProperty(DBConstants.PROP_ID_DESCRIPTION) || command.getProperties().size() > 1) {
            createOrReplaceViewQuery(monitor, actions, (PostgreViewBase) command.getObject(), options);
        }
    }

    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options) throws DBException {
        if (!command.hasProperty(DBConstants.PROP_ID_DESCRIPTION) || command.getProperties().size() > 1) {
            createOrReplaceViewQuery(monitor, actionList, (PostgreViewBase) command.getObject(), options);
        }
    }

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options)
    {
        PostgreViewBase view = (PostgreViewBase)command.getObject();
        actions.add(
            new SQLDatabasePersistAction(
                "Drop view", 
                "DROP " + view.getTableTypeName() +
                    " " + view.getFullyQualifiedName(DBPEvaluationContext.DDL) +
                    (CommonUtils.getOption(options, OPTION_DELETE_CASCADE) ? " CASCADE" : ""))
        );
    }

    protected void createOrReplaceViewQuery(DBRProgressMonitor monitor, List<DBEPersistAction> actions, PostgreViewBase view, Map<String, Object> options) throws DBException {
        if (CommonUtils.isEmpty(view.getObjectDefinitionText(monitor, options))) {
            throw new DBException("View '" + view.getName() + "' definition is empty");
        }
        // Source may be empty if it wasn't yet read. Then it definitely wasn't changed
        String sql = view.getSource().trim();
        if (!sql.toLowerCase(Locale.ENGLISH).startsWith("create")) {
            StringBuilder sqlBuf = new StringBuilder();
            sqlBuf.append("CREATE ");
            if (!(view instanceof PostgreMaterializedView)) {
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

    public void appendViewDeclarationPrefix(DBRProgressMonitor monitor, StringBuilder sqlBuf, PostgreViewBase view) throws DBException {
        String[] relOptions = view.getRelOptions();
        if (!ArrayUtils.isEmpty(relOptions)) {
            sqlBuf.append("\nWITH(").append(String.join("," , relOptions)).append(")");
        }
    }

    public void appendViewDeclarationPostfix(DBRProgressMonitor monitor, StringBuilder sqlBuf, PostgreViewBase view) {

    }

    @Override
    public void renameObject(@NotNull DBECommandContext commandContext, @NotNull PostgreTableBase object, @NotNull Map<String, Object> options, @NotNull String newName) throws DBException {
        processObjectRename(commandContext, object, options, newName);
    }

    @Override
    protected void addObjectRenameActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectRenameCommand command, Map<String, Object> options)
    {
        PostgreViewBase view = (PostgreViewBase) command.getObject();
        actions.add(
            new SQLDatabasePersistAction(
                "Rename view",
                "ALTER " + view.getTableTypeName() + " " + DBUtils.getQuotedIdentifier(view.getSchema()) + "." + DBUtils.getQuotedIdentifier(view.getDataSource(), command.getOldName()) + //$NON-NLS-1$
                    " RENAME TO " + DBUtils.getQuotedIdentifier(view.getDataSource(), command.getNewName())) //$NON-NLS-1$
        );
    }

    @Override
    protected void addObjectExtraActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, NestedObjectCommand<PostgreTableBase, PropertyHandler> command, Map<String, Object> options) {
        PostgreViewBase viewBase = (PostgreViewBase) command.getObject();
        if (command.hasProperty(DBConstants.PROP_ID_DESCRIPTION)) {
            actions.add(new SQLDatabasePersistAction(
                    "Comment view",
                    "COMMENT ON " + viewBase.getTableTypeName() + " " + viewBase.getFullyQualifiedName(DBPEvaluationContext.DDL) +
                            " IS " + SQLUtils.quoteString(viewBase, CommonUtils.notEmpty(viewBase.getDescription()))));
        }
    }

}

