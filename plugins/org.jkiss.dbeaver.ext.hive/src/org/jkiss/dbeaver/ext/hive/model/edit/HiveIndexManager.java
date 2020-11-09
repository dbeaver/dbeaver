package org.jkiss.dbeaver.ext.hive.model.edit;

import org.jkiss.dbeaver.ext.generic.edit.GenericIndexManager;
import org.jkiss.dbeaver.ext.generic.model.GenericTableIndex;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.List;
import java.util.Map;

public class HiveIndexManager extends GenericIndexManager {

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options) {
        GenericTableIndex tableIndex = command.getObject();
        actions.add(
                    new SQLDatabasePersistAction("Drop index table", "DROP INDEX " + tableIndex.getFullyQualifiedName(DBPEvaluationContext.DDL) +
                            " ON " + tableIndex.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL))); //$NON-NLS-2$

    }
}
