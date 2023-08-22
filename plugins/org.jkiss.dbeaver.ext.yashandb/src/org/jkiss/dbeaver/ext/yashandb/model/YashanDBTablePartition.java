package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraint;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.model.struct.rdb.DBSTablePartition;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @Author: donghy
 * @Date: 2022/08
 * @Description:
 */
public class YashanDBTablePartition extends YashanDBPartitionBase<YashanDBTablePhysical> implements DBSTablePartition {


    //TMP, SAIXI测试
    private String partitionNames;

    //TMP
    private String partitionType;

    private List<YashanDBTableColumn> columns;

    public List<YashanDBTableColumn> getColumns() {
        return columns;
    }

    public void setColumns(List<YashanDBTableColumn> columns) {
        this.columns = columns;
    }

    public YashanDBTablePartition(
            YashanDBTablePhysical yashandbTable,
            boolean subpartition,
            ResultSet dbResult) {
        super(yashandbTable, subpartition, dbResult);
    }

    @Association
    public Collection<YashanDBTablePartition> getSubPartitions(DBRProgressMonitor monitor) throws DBException {
        return getParentObject().getSubPartitions(monitor, this);
    }


    @Override
    public DBSEntity getTable() {
        return parent;
    }

    public String getPartitionNames() {
        return partitionNames;
    }

    public void setPartitionNames(String partitionNames) {
        this.partitionNames = partitionNames;
    }

    public String getPartitionType() {
        return partitionType;
    }

    public void setPartitionType(String partitionType) {
        this.partitionType = partitionType;
    }

    public void addColumn(YashanDBTableColumn column)
    {
        if (columns == null) {
            columns = new ArrayList<>();
        }
        this.columns.add(column);
    }
}
