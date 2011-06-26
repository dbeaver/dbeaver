/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.util.List;

/**
 * Table partition
 */
public class OracleTablePartition extends OraclePartitionBase<OracleTablePhysical> {

    private List<OracleTablePartition> subPartitions;

    protected OracleTablePartition(
        OracleTablePhysical oracleTable,
        boolean subpartition,
        ResultSet dbResult)
    {
        super(oracleTable, subpartition, dbResult);
    }

    @Association
    public List<OracleTablePartition> getSubPartitions()
    {
        return subPartitions;
    }

    public void setSubPartitions(List<OracleTablePartition> subPartitions)
    {
        this.subPartitions = subPartitions;
    }

    public boolean hasSubPartitions()
    {
        return !CommonUtils.isEmpty(subPartitions);
    }

}
