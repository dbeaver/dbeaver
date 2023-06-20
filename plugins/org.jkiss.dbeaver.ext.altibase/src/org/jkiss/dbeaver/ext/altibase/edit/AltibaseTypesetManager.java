package org.jkiss.dbeaver.ext.altibase.edit;

import java.util.List;
import java.util.Map;

import org.jkiss.dbeaver.ext.generic.edit.GenericProcedureManager;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedure;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class AltibaseTypesetManager extends GenericProcedureManager {
    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, 
            List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options)
    {
        GenericProcedure object = command.getObject();
        String procedureName;
        procedureName = object.getFullyQualifiedName(DBPEvaluationContext.DDL);

        actions.add(
            new SQLDatabasePersistAction(
                ModelMessages.model_jdbc_drop_table,
                "DROP TYPESET" + " " + procedureName)
        );
    }
}
