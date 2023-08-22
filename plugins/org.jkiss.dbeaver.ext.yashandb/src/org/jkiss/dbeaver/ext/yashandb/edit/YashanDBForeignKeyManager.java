package org.jkiss.dbeaver.ext.yashandb.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBObjectStatus;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTableBase;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTableForeignKey;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLForeignKeyManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;

import java.util.Map;

public class YashanDBForeignKeyManager extends SQLForeignKeyManager<YashanDBTableForeignKey, YashanDBTableBase> {


    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, YashanDBTableForeignKey> getObjectsCache(YashanDBTableForeignKey object) {
        return object.getParentObject().getSchema().foreignKeyCache;
    }

    @Override
    protected YashanDBTableForeignKey createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final Object container, Object from, Map<String, Object> options) {
        YashanDBTableBase table = (YashanDBTableBase) container;

        return new YashanDBTableForeignKey(
                table,
                "",
                YashanDBObjectStatus.ENABLED,
                null,
                DBSForeignKeyModifyRule.NO_ACTION);
    }

}
