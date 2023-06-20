package org.jkiss.dbeaver.ext.altibase.edit;

import org.jkiss.dbeaver.ext.generic.edit.GenericTableManager;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;

public class AltibaseQueueManager extends GenericTableManager {
    
    @Override
    public boolean canCreateObject(Object container) {
        return false;
    }
    
    @Override
    public boolean canDeleteObject(GenericTableBase object) {
        return true;
    }
    
    protected String getCreateTableType(GenericTableBase table) {
        return "QUEUE";
    }
}
