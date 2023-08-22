package org.jkiss.dbeaver.ext.dm.edit;

import java.util.Map;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.dm.model.DmObjectStatus;
import org.jkiss.dbeaver.ext.dm.model.DmTableBase;
import org.jkiss.dbeaver.ext.dm.model.DmTableForeignKey;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLForeignKeyManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;

public class DmForeignKeyManager extends SQLForeignKeyManager<DmTableForeignKey, DmTableBase>{
	
    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, DmTableForeignKey> getObjectsCache(DmTableForeignKey object)
    {
        return object.getParentObject().getSchema().foreignKeyCache;
    }

    @Override
    protected DmTableForeignKey createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final Object container, Object from, Map<String, Object> options)
    {
    	DmTableBase table = (DmTableBase) container;

        return new DmTableForeignKey(
            table,
            "",
            DmObjectStatus.ENABLED,
            null,
            DBSForeignKeyModifyRule.NO_ACTION);
    }
}
