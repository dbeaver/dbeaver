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
package org.jkiss.dbeaver.ext.generic.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericView;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.edit.prop.DBECommandComposite;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * Generic view manager
 */
public class GenericViewManager extends SQLObjectEditor<GenericTableBase, GenericStructContainer> {

    @Override
    public long getMakerOptions(@NotNull DBPDataSource dataSource)
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    protected void validateObjectProperties(DBRProgressMonitor monitor, ObjectChangeCommand command, Map<String, Object> options)
            throws DBException
    {
        if (CommonUtils.isEmpty(command.getObject().getName())) {
            throw new DBException("View name cannot be empty");
        }
        if (CommonUtils.isEmpty(command.getObject().getObjectDefinitionText(monitor, options))) {
            throw new DBException("View definition cannot be empty");
        }
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, GenericTableBase> getObjectsCache(GenericTableBase object)
    {
        return object.getContainer().getTableCache();
    }

    @Override
    public boolean canCreateObject(@NotNull Object container) {
        if (container instanceof DBSObject) {
            DBPDataSource dataSource = ((DBSObject) container).getDataSource();
            if (dataSource instanceof GenericDataSource) {
                return ((GenericDataSource) dataSource).getMetaModel().supportsViews((GenericDataSource) dataSource);
            }
        }
        return super.canCreateObject(container);
    }

    @Override
    protected GenericTableBase createDatabaseObject(@NotNull DBRProgressMonitor monitor, @NotNull DBECommandContext context, Object container, Object copyFrom, @NotNull Map<String, Object> options)
    {
        GenericStructContainer structContainer = (GenericStructContainer) container;
        String tableName = getNewChildName(monitor, structContainer, SQLTableManager.BASE_VIEW_NAME);
        GenericTableBase viewImpl = structContainer.getDataSource().getMetaModel().createTableOrViewImpl(structContainer, tableName,
                GenericConstants.TABLE_TYPE_VIEW,
                null);
        if (viewImpl instanceof GenericView) {
            ((GenericView) viewImpl).setObjectDefinitionText("CREATE VIEW " + viewImpl.getFullyQualifiedName(DBPEvaluationContext.DDL) + " AS SELECT 1 as A\n");
        }
        return viewImpl;
    }

    @Override
    protected void addObjectCreateActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectCreateCommand command, @NotNull Map<String, Object> options)
    {
        createOrReplaceViewQuery(actions, command);
    }

    @Override
    protected void addObjectDeleteActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actions, @NotNull ObjectDeleteCommand command, @NotNull Map<String, Object> options)
    {
        actions.add(
            new SQLDatabasePersistAction(
                "Drop view",
                "DROP " + getViewType(command.getObject()) + " " +
                command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL)) //$NON-NLS-2$
        );
    }

    protected String getViewType(GenericTableBase object) {
        return "VIEW";
    }

    private void createOrReplaceViewQuery(List<DBEPersistAction> actions, DBECommandComposite<GenericTableBase, PropertyHandler> command)
    {
        final GenericView view = (GenericView)command.getObject();
        actions.add(new SQLDatabasePersistAction("Create view", view.getDDL()));
    }

    @Override
    protected void addObjectModifyActions(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext executionContext, @NotNull List<DBEPersistAction> actionList, @NotNull ObjectChangeCommand command, @NotNull Map<String, Object> options)
    {
        final GenericView view = (GenericView)command.getObject();
        boolean hasComment = command.hasProperty(DBConstants.PROP_ID_DESCRIPTION);
        if (!hasComment || command.getProperties().size() > 1) {
            actionList.add(new SQLDatabasePersistAction("Create view", view.getDDL()));
        }
    }

    @Override
    protected void addObjectExtraActions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<DBEPersistAction> actions,
        @NotNull NestedObjectCommand<GenericTableBase, PropertyHandler> command,
        @NotNull Map<String, Object> options
    ) {
        if (command.hasProperty(DBConstants.PROP_ID_DESCRIPTION)) {
            GenericTableBase object = command.getObject();
            actions.add(new SQLDatabasePersistAction(
                "Comment view",
                "COMMENT ON " + getViewType(object) + " " + object.getFullyQualifiedName(DBPEvaluationContext.DDL) +
                    " IS " + SQLUtils.quoteString(object, CommonUtils.notEmpty(object.getDescription()))));
        }
    }
}
