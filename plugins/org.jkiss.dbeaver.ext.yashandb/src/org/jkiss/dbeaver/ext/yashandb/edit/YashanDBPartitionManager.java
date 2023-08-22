package org.jkiss.dbeaver.ext.yashandb.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTableColumn;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTablePartition;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTablePhysical;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLPartitionManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

import java.util.List;
import java.util.Map;

/**
 * @author yangmeng
 * @date 2023/7/27 18:21
 */

public class YashanDBPartitionManager extends SQLPartitionManager<YashanDBTablePartition, YashanDBTablePhysical> {

    public static boolean FIRSR = true;


    @Override
    protected YashanDBTablePartition createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
                                                          Object container, Object copyFrom,
                                                          Map<String, Object> options) throws DBException {
        YashanDBTablePhysical tableBase = (YashanDBTablePhysical) container;
        return new YashanDBTablePartition(tableBase, false, null);
    }

    @Override
    public DBSObjectCache<? extends DBSObject, YashanDBTablePartition> getObjectsCache(YashanDBTablePartition object) {
        return null;
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
                                          List<DBEPersistAction> actions, SQLObjectEditor<YashanDBTablePartition,
            YashanDBTablePhysical>.ObjectCreateCommand command, Map<String, Object> options) throws DBException {

    }

    @Override
    protected StringBuilder getNestedDeclaration(DBRProgressMonitor monitor, YashanDBTablePhysical owner,
                                                 DBECommandAbstract<YashanDBTablePartition> command, Map<String,
            Object> options) {
        StringBuilder stringBuilder = new StringBuilder();
        YashanDBTablePartition object = command.getObject();
        if (FIRSR || (Boolean) options.get("yasfrist")) {
            stringBuilder.append(" \nPARTITION BY HASH (");
            for (YashanDBTableColumn column : object.getColumns()) {
                stringBuilder.append("\"").append(column.getName()).append("\"");
            }
            stringBuilder.append(")\n");
            FIRSR = false;
            stringBuilder.append("(\n");
        }
        stringBuilder.append("PARTITION").append(" \"").append(object.getPartitionNames()).append("\" TABLESPACE " +
                "\"USERS\"").append(",\n");
        return stringBuilder;
    }

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
                                          List<DBEPersistAction> actions, SQLObjectEditor<YashanDBTablePartition,
            YashanDBTablePhysical>.ObjectDeleteCommand command, Map<String, Object> options) throws DBException {
    }


}
