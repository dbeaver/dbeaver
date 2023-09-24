package org.jkiss.dbeaver.ext.yashandb.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBExecutionContext;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBSchema;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTableColumn;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTablePartition;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTablePhysical;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLPartitionManager;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author yangmeng
 * @date 2023/7/27 18:21
 */

public class YashanDBPartitionManager extends SQLPartitionManager<YashanDBTablePartition, YashanDBTablePhysical> {

    @Override
    protected YashanDBTablePartition createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
                                                          Object container, Object copyFrom,
                                                          Map<String, Object> options) throws DBException {
        YashanDBTablePhysical tableBase = (YashanDBTablePhysical) container;
        YashanDBTablePartition partition = new YashanDBTablePartition(tableBase, false, null);

        Collection<YashanDBTablePartition> partitions = tableBase.getPartitions(monitor);
        if(partitions != null && !partitions.isEmpty()){
            YashanDBTablePartition part = partitions.stream().findFirst().get();
            List<YashanDBTableColumn> columns = part.getColumns();
            partition.setColumns(columns);
            partition.setPartitionType(part.getPartitionType());
        }
        return partition;
    }

    @Override
    public DBSObjectCache<? extends DBSObject, YashanDBTablePartition> getObjectsCache(YashanDBTablePartition object) {
        return null;
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
                                          List<DBEPersistAction> actions, SQLObjectEditor<YashanDBTablePartition,
            YashanDBTablePhysical>.ObjectCreateCommand command, Map<String, Object> options) throws DBException {

        YashanDBTablePartition partition = command.getObject();
        DBSEntity table = partition.getTable();
        if(table == null){
            throw new DBException("Can't get command object.");
        }
        String partitionNames = partition.getPartitionNames();
        if(partitionNames == null){
            throw new DBException("Please input partition name.");
        }
        String partitionType = partition.getPartitionType();

        YashanDBExecutionContext context = (YashanDBExecutionContext) executionContext;
        String activeSchemaName = context.getActiveSchemaName();
        YashanDBSchema container = command.getObject().getParentObject().getContainer();
        String tableSchema = container.getName();

        StringBuilder createPartSQL = new StringBuilder("ALTER TABLE ");
        if(!tableSchema.equals(activeSchemaName)){
            createPartSQL.append(tableSchema).append(".");
        }
        createPartSQL.append(table.getName())
                .append(" ADD PARTITION ")
                .append(partitionNames);

        appendValues(partition, partitionType, createPartSQL);

        actions.add(0, new SQLDatabasePersistAction(ModelMessages.model_jdbc_create_new_object, createPartSQL.toString()));
    }

    private void appendValues(YashanDBTablePartition partition, String partitionType, StringBuilder createPartSQL){
        if("RANGE".equals(partitionType)){
            if(partition.getValue() == null){
                throw new RuntimeException("Error partition value, Partition Type is " + partition.getPartitionType() + ", but partition value is NULL");
            }
            createPartSQL.append(" VALUES ")
                    .append(" LESS THAN ")
                    .append(String.format("(%s)", partition.getValue()));
        }
        if("LIST".equals(partitionType)){
            if(partition.getValue() == null){
                throw new RuntimeException("Error partition value, Partition Type is " + partition.getPartitionType() + ", but partition value is NULL");
            }
            createPartSQL.append(" VALUES ")
                    .append(String.format("(%s)", partition.getValue()));
        }
    }

    @Override
    protected StringBuilder getNestedDeclaration(DBRProgressMonitor monitor, YashanDBTablePhysical owner,
                                                 DBECommandAbstract<YashanDBTablePartition> command, Map<String,
            Object> options) {
        YashanDBTablePartition partition = command.getObject();
        String partitionNames = partition.getPartitionNames();

        StringBuilder partSQL = new StringBuilder("PARTITION ");
        partSQL.append(partitionNames);
        if(partition.getPartitionType() != null && !"HASH".equals(partition.getPartitionType())){
            appendValues(partition, partition.getPartitionType(), partSQL);
        }
        return partSQL;
    }

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
                                          List<DBEPersistAction> actions, SQLObjectEditor<YashanDBTablePartition,
            YashanDBTablePhysical>.ObjectDeleteCommand command, Map<String, Object> options) throws DBException {
        YashanDBTablePartition partition = command.getObject();
        StringBuilder dropPartSQL = new StringBuilder("ALTER TABLE ");
        dropPartSQL.append(partition.getTable().getName())
                .append(" DROP PARTITION ")
                .append(partition.getName());
        actions.add(0, new SQLDatabasePersistAction(ModelMessages.model_jdbc_delete_object, dropPartSQL.toString()));
    }


}
