package org.jkiss.dbeaver.ext.hive.model.edit;

import org.jkiss.dbeaver.ext.generic.edit.GenericTableManager;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.List;
import java.util.Map;

public class HiveTableManager extends GenericTableManager {

    @Override
    protected void addObjectRenameActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectRenameCommand command, Map<String, Object> options) {
        actions.add(
                new SQLDatabasePersistAction(
                        "Rename table",
                        "ALTER TABLE " + DBUtils.getQuotedIdentifier(command.getObject().getSchema()) + "." + DBUtils.getQuotedIdentifier(command.getObject().getDataSource(), command.getOldName()) + //$NON-NLS-1$
                                " RENAME TO " + DBUtils.getQuotedIdentifier(command.getObject().getDataSource(), command.getNewName())) //$NON-NLS-1$
        );
    }

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options) {
        GenericTableBase table = command.getObject();
        if (table.getTableType().equals("INDEX_TABLE")) {
            actions.add(
                    new SQLDatabasePersistAction("Drop index table", "DROP INDEX " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL))); //$NON-NLS-2$
            return;
        }
        super.addObjectDeleteActions(monitor, executionContext, actions, command, options);
    }
}
