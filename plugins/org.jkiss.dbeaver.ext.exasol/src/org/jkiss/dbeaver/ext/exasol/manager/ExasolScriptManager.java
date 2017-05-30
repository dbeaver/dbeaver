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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.model.ExasolSchema;
import org.jkiss.dbeaver.ext.exasol.model.ExasolScript;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.utils.CommonUtils;

import java.util.List;

public class ExasolScriptManager extends SQLObjectEditor<ExasolScript, ExasolSchema> {

    @Override
    public DBSObjectCache<ExasolSchema, ExasolScript> getObjectsCache(ExasolScript object) {
        return object.getContainer().scriptCache;
    }

    @Override
    public long getMakerOptions() {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    protected void validateObjectProperties(ObjectChangeCommand command)
            throws DBException {
        if (CommonUtils.isEmpty(command.getObject().getName()))
        {
            throw new DBException("Procedure name cannot be empty");
        }
    }
    

    @Override
    protected ExasolScript createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
            ExasolSchema parent, Object copyFrom) throws DBException {
        ExasolScript newScript =  new ExasolScript(parent);
        newScript.setName("new_script");
        newScript.setObjectDefinitionText("CREATE OR REPLACE LUA SCRIPT new_script () RETURNS TABLE AS");
        return newScript;
    }
    
    private void createOrReplaceScriptQuery(List<DBEPersistAction> actions, ExasolScript script, Boolean replace)
    {
        actions.add(
                new SQLDatabasePersistAction("Create Script", "OPEN SCHEMA " + script.getSchema().getName()));
        if (replace) { 
            actions.add(
                new SQLDatabasePersistAction("Create Script", "DROP SCRIPT " + script.getFullyQualifiedName(DBPEvaluationContext.DDL)));
        }
        actions.add(
            new SQLDatabasePersistAction("Create Script: ", script.getSql() , true));
    }
    
    @Override
    protected void addObjectCreateActions(List<DBEPersistAction> actions,
            SQLObjectEditor<ExasolScript, ExasolSchema>.ObjectCreateCommand command) {
        createOrReplaceScriptQuery(actions, command.getObject(), false);
    }

    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions,
            SQLObjectEditor<ExasolScript, ExasolSchema>.ObjectDeleteCommand command) {
        actions.add(
                new SQLDatabasePersistAction("Create Script", "DROP SCRIPT " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL)));
    }
    
    @Override
    protected void addObjectModifyActions(List<DBEPersistAction> actionList,
            SQLObjectEditor<ExasolScript, ExasolSchema>.ObjectChangeCommand command)
    {
        if (command.getProperties().size() > 1 || command.getProperty("description") == null )
        {
            createOrReplaceScriptQuery(actionList, command.getObject(),true);
        }
    }
    
    @Override
    protected void addObjectExtraActions(List<DBEPersistAction> actions,
            org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor.NestedObjectCommand<ExasolScript, SQLObjectEditor<ExasolScript, ExasolSchema>.PropertyHandler> command)
    {
        if (command.getProperty("description") != null) {
            actions.add(new SQLDatabasePersistAction("Comment on Script","COMMENT ON SCRIPT " + 
                            command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL) + " IS " +
                            SQLUtils.quoteString(command.getObject(), command.getObject().getDescription())));
        }
    }

}
