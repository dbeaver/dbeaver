package org.jkiss.dbeaver.ext.altibase.edit;

import java.util.List;

import org.jkiss.dbeaver.ext.altibase.model.AltibaseTrigger;
import org.jkiss.dbeaver.ext.generic.edit.GenericTriggerManager;
import org.jkiss.dbeaver.ext.generic.model.GenericTrigger;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class AltibaseTriggerManager extends GenericTriggerManager {
    
    @Override
    public boolean canEditObject(GenericTrigger object) {
        return true;
    }
    
    @Override
    protected void createOrReplaceTriggerQuery(
        DBRProgressMonitor monitor,
        DBCExecutionContext executionContext,
        List<DBEPersistAction> actions,
        GenericTrigger trigger,
        boolean create
    ) {
        actions.add(
                new SQLDatabasePersistAction("Alter sequence", 
                        ((AltibaseTrigger) trigger).getSource())
                );
    }
}
