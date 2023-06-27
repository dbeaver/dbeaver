/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.altibase.edit;

import java.util.List;
import java.util.Map;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.altibase.AltibaseConstants;
import org.jkiss.dbeaver.ext.altibase.model.AltibaseProcedureStandAlone;
import org.jkiss.dbeaver.ext.altibase.model.AltibaseSchema;
import org.jkiss.dbeaver.ext.generic.edit.GenericProcedureManager;
import org.jkiss.dbeaver.ext.generic.model.GenericPackage;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedure;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor.ObjectCreateCommand;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.utils.CommonUtils;

public class AltibaseProcedureManager extends GenericProcedureManager  {
    
    @Override
    public boolean canCreateObject(Object container) {
        return true;
    }
    
    @Override
    public boolean canEditObject(GenericProcedure object) {
        return true;
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, 
            List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options){
        createOrReplaceProcedureQuery(actions, command.getObject());
    }
    
    @Override
    protected GenericProcedure createDatabaseObject(
        DBRProgressMonitor monitor, DBECommandContext context, final Object container,
        Object from, Map<String, Object> options) {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, 
            List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options) throws DBException {
        createOrReplaceProcedureQuery(actionList, command.getObject());
    }

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, 
            List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options) {
        
        final AltibaseProcedureStandAlone object = (AltibaseProcedureStandAlone) command.getObject();
        DBSProcedureType procType = object.getProcedureType();
        String procTypeName = (procType == DBSProcedureType.UNKNOWN)? AltibaseConstants.OBJ_TYPE_TYPESET:procType.name();
        
        actions.add(
                new SQLDatabasePersistAction("Drop procedure",
                    "DROP " + procTypeName + " " + object.getFullyQualifiedName(DBPEvaluationContext.DDL))
            );
    }
    
    @Override
    protected void validateObjectProperties(DBRProgressMonitor monitor, ObjectChangeCommand command, Map<String, Object> options) throws DBException {
        if (CommonUtils.isEmpty(command.getObject().getName())) {
            throw new DBException("Procedure name cannot be empty");
        }
        if (CommonUtils.isEmpty(command.getObject().getSource())) {
            throw new DBException("Procedure body cannot be empty");
        }
    }

    private void createOrReplaceProcedureQuery(List<DBEPersistAction> actions, GenericProcedure procedure)
    {
        actions.add(
                new SQLDatabasePersistAction("Create procedure", procedure.getSource()));
    }
    
    /*
     * 
     *     @Override
    public long getMakerOptions(DBPDataSource dataSource) {
        return FEATURE_EDITOR_ON_CREATE;
    }
    
    @Override
    protected AltibaseProcedureStandAlone createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
            Object container, Object copyFrom, Map<String, Object> options) throws DBException {

        return null;
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
            List<DBEPersistAction> actions,
            SQLObjectEditor<GenericProcedure, GenericStructContainer>.ObjectCreateCommand command,
            Map<String, Object> options) throws DBException {
        // TODO Auto-generated method stub
        
    }

    */
}
