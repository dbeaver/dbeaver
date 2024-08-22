package org.jkiss.dbeaver.ext.cubrid.edit;

import java.util.List;
import java.util.Map;

import org.jkiss.dbeaver.ext.cubrid.model.CubridProcedure;
import org.jkiss.dbeaver.ext.generic.edit.GenericProcedureManager;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedure;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;

public class CubridProcedureManager extends GenericProcedureManager {

    @Override
    public boolean canCreateObject(Object container) {
        return true;
    }

    @Override
    protected GenericProcedure createDatabaseObject(
        DBRProgressMonitor monitor, DBECommandContext context, final Object container,
        Object from, Map<String, Object> options){
        String type = options.get("container").toString();
        DBSProcedureType procedureType = type.equals("Functions")
                ? DBSProcedureType.FUNCTION : DBSProcedureType.PROCEDURE;
        return new CubridProcedure((GenericStructContainer) container, procedureType);
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options) throws DBCException {
        CubridProcedure procedure = (CubridProcedure) command.getObject();
        actions.add(new SQLDatabasePersistAction("Create Procedure", procedure.getSource()));
    }
}
