/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.exasol.manager;

import java.util.List;
import java.util.Map;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.model.ExasolSchema;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTableBase;
import org.jkiss.dbeaver.ext.exasol.model.ExasolView;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

public class ExasolViewManager
        extends SQLObjectEditor<ExasolView, ExasolSchema> implements DBEObjectRenamer<ExasolView> {

    @Override
    public long getMakerOptions()
    {
        return FEATURE_EDITOR_ON_CREATE;
    }
    
    @Override
    protected void validateObjectProperties(ObjectChangeCommand command)
            throws DBException
    {
        ExasolTableBase object = command.getObject();
        if (CommonUtils.isEmpty(object.getName())) {
            throw new DBException("View name cannot be empty");
            
        }
        if (CommonUtils.isEmpty(((ExasolView) object).getSource() )) {
            throw new DBException("View definition cannot be empty");
        }
    }

    @Override
    public DBSObjectCache<ExasolSchema, ExasolView> getObjectsCache(
            ExasolView object)
    {
        return object.getContainer().getViewCache();
    }

    @Override
    protected ExasolView createDatabaseObject(DBRProgressMonitor monitor,
            DBECommandContext context, ExasolSchema parent, Object copyFrom)
            throws DBException
    {
        ExasolView newView = new ExasolView(parent);
        newView.setName("new_view");
        return newView;
    }

    @Override
    protected void addObjectCreateActions(List<DBEPersistAction> actions,
                                          ObjectCreateCommand command, Map<String, Object> options)
    {
        createOrReplaceViewQuery(actions, command.getObject(), false);
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions,
                                          ObjectDeleteCommand command, Map<String, Object> options)
    {
        ExasolView view = (ExasolView) command.getObject();
        actions.add(
                new SQLDatabasePersistAction("Drop view", "DROP VIEW " + view.getFullyQualifiedName(DBPEvaluationContext.DDL))
                );
        
    }
    
    @Override
    protected void addObjectModifyActions(List<DBEPersistAction> actionList,
                                          ObjectChangeCommand command, Map<String, Object> options)
    {
        createOrReplaceViewQuery(actionList, command.getObject(), true);
    }
    
    protected void createOrReplaceViewQuery(List<DBEPersistAction> actions, ExasolView view, Boolean replace) 
    {
        if (replace) {
            actions.add(
                    new SQLDatabasePersistAction("Drop view", "DROP VIEW " + view.getFullyQualifiedName(DBPEvaluationContext.DDL))
                    );
        }
        try {
            actions.add(
                new SQLDatabasePersistAction("Create view", view.getSource()));
        } catch (DBCException e) {
        }
    }

    @Override
    public void renameObject(DBECommandContext commandContext,
            ExasolView object, String newName) throws DBException
    {
        processObjectRename(commandContext, object, newName);
    }
    
    @Override
    protected void addObjectRenameActions(List<DBEPersistAction> actions,
                                          ObjectRenameCommand command, Map<String, Object> options)
    {
        ExasolView obj = command.getObject();
        actions.add(
                new SQLDatabasePersistAction(
                    "Rename View",
                    "RENAME VIEW " +  DBUtils.getQuotedIdentifier(obj.getDataSource(), command.getOldName()) + " to " +
                        DBUtils.getQuotedIdentifier(obj.getDataSource(), command.getNewName()))
            );
    }

    
}
