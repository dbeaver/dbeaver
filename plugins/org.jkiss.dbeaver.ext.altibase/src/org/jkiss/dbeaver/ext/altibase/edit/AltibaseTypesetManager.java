package org.jkiss.dbeaver.ext.altibase.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.altibase.model.AltibaseTypeset;
import org.jkiss.dbeaver.ext.generic.edit.GenericProcedureManager;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedure;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

public class AltibaseTypesetManager extends GenericProcedureManager {
    
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
            List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options) {
        createOrReplaceProcedureQuery(actions, command.getObject());
    }

    @Override
    protected GenericProcedure createDatabaseObject(
            DBRProgressMonitor monitor, DBECommandContext context, final Object container,
            Object from, Map<String, Object> options) {
        return new AltibaseTypeset(
                (GenericStructContainer) container,
                "NEW_TYPESET");
    }

    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, 
            List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options) throws DBException {
        createOrReplaceProcedureQuery(actionList, command.getObject());
    }

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, 
            List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options) {
        GenericProcedure object = command.getObject();
        String procedureName;
        procedureName = object.getFullyQualifiedName(DBPEvaluationContext.DDL);

        actions.add(
            new SQLDatabasePersistAction(
                ModelMessages.model_jdbc_drop_table,
                "DROP TYPESET" + " " + procedureName)
        );
    }
    
    @Override
    protected void validateObjectProperties(DBRProgressMonitor monitor, ObjectChangeCommand command, 
            Map<String, Object> options) throws DBException {
        if (CommonUtils.isEmpty(command.getObject().getName())) {
            throw new DBException("Procedure name cannot be empty");
        }
        if (CommonUtils.isEmpty(command.getObject().getSource())) {
            throw new DBException("Procedure body cannot be empty");
        }
    }

    private void createOrReplaceProcedureQuery(List<DBEPersistAction> actions, GenericProcedure procedure) {
        actions.add(new SQLDatabasePersistAction(
                "Create procedure", procedure.getSource()));
    }
}
