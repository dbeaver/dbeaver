/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.dbeaver.ext.exasol.model.ExasolFunction;
import org.jkiss.dbeaver.ext.exasol.model.ExasolSchema;
import org.jkiss.dbeaver.ext.exasol.tools.ExasolUtils;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

public class ExasolFunctionManager extends SQLObjectEditor<ExasolFunction, ExasolSchema> {

    @Override
    public DBSObjectCache<ExasolSchema, ExasolFunction> getObjectsCache(ExasolFunction object) {
        return object.getContainer().functionCache;
    }

    @Override
    public long getMakerOptions(DBPDataSource dataSource) {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    protected void validateObjectProperties(ObjectChangeCommand command, Map<String, Object> options)
            throws DBException {
        if (CommonUtils.isEmpty(command.getObject().getName()))
        {
            throw new DBException("Function name cannot be empty");
        }
    }
    

    @Override
    protected ExasolFunction createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
                                                  Object container, Object copyFrom, Map<String, Object> options) throws DBException {
        ExasolFunction newScript =  new ExasolFunction((ExasolSchema) container);
        newScript.setName("function_name");
        newScript.setObjectDefinitionText("FUNCTION function_name() RETURNS INTEGER");
        return newScript;
    }
    
    private void createOrReplaceScriptQuery(List<DBEPersistAction> actions, ExasolFunction script, Boolean replace)
    {
        actions.add(
                new SQLDatabasePersistAction("Create Script", "OPEN SCHEMA " + script.getSchema().getName()));
        if (replace) { 
            actions.add(
                new SQLDatabasePersistAction("Create Script", "CREATE OR REPLACE " + script.getSql()));
        } else {
        actions.add(
                new SQLDatabasePersistAction("Create Script", "CREATE " + script.getSql()));
        }
    }
    
    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions,
                                          ObjectCreateCommand command, Map<String, Object> options) {
        createOrReplaceScriptQuery(actions, command.getObject(), false);
    }

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions,
                                          ObjectDeleteCommand command, Map<String, Object> options) {
        actions.add(
                new SQLDatabasePersistAction("Create Script", "DROP FUNCTION " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL)));
    }
    
    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actionList,
                                          ObjectChangeCommand command, Map<String, Object> options)
    {
        if (command.getProperties().size() > 1 || command.getProperty("description") == null )
        {
            createOrReplaceScriptQuery(actionList, command.getObject(),true);
        }
    }
    
    @Override
    protected void addObjectExtraActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions,
                                         NestedObjectCommand<ExasolFunction, PropertyHandler> command, Map<String, Object> options)
    {
        if (command.getProperty("description") != null) {
            actions.add(new SQLDatabasePersistAction("Comment on Script","COMMENT ON FUNCTION " + 
                            command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL) + " IS " +
                            SQLUtils.quoteString(command.getObject(), ExasolUtils.quoteString(command.getObject().getDescription()))));
        }
    }

}