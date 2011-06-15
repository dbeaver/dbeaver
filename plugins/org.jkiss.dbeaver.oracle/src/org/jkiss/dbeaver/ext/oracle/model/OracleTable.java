/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import net.sf.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.OracleConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * OracleTable
 */
public class OracleTable extends OracleTableBase
{

    private List<OracleIndex> indexes;
    private List<OracleConstraint> constraints;
    private List<OracleForeignKey> foreignKeys;
    private List<OracleTrigger> triggers;
    private List<OraclePartition> partitions;

    public OracleTable(OracleSchema schema)
    {
        super(schema, false);
    }

    public OracleTable(
        OracleSchema schema,
        ResultSet dbResult)
    {
        super(schema, dbResult);
    }

    public boolean isView()
    {
        return false;
    }

    public List<OracleIndex> getIndexes(DBRProgressMonitor monitor)
        throws DBException
    {
        if (indexes == null) {
            // Read indexes using cache
            this.getContainer().getIndexCache().getObjects(monitor, getDataSource(), this);
        }
        return indexes;
    }

    public OracleIndex getIndex(DBRProgressMonitor monitor, String indexName)
        throws DBException
    {
        return DBUtils.findObject(getIndexes(monitor), indexName);
    }

    boolean isIndexesCached()
    {
        return indexes != null;
    }

    void setIndexes(List<OracleIndex> indexes)
    {
        this.indexes = indexes;
    }

    public List<OracleConstraint> getConstraints(DBRProgressMonitor monitor)
        throws DBException
    {
        if (constraints == null) {
            // Read constraints for ALL tables in this schema
            // It is fastest way to obtain constraints because ALL_CONSTRAINTS table doesn't contains reference table name
            // and extra join will slow query dramatically
            getContainer().getConstraintCache().getObjects(monitor, null);
        }
        return constraints;
    }

    public OracleConstraint getConstraint(DBRProgressMonitor monitor, String ukName)
        throws DBException
    {
        return DBUtils.findObject(getConstraints(monitor), ukName);
    }

    void setConstraints(DBRProgressMonitor monitor, List<OracleConstraint> constraints)
    {
        this.constraints = new ArrayList<OracleConstraint>();
        this.foreignKeys = new ArrayList<OracleForeignKey>();
        for (OracleConstraint constraint : constraints) {
            if (constraint instanceof OracleForeignKey) {
                if (((OracleForeignKey) constraint).resolveLazyReference(monitor)) {
                    this.foreignKeys.add((OracleForeignKey) constraint);
                }
            } else {
                this.constraints.add(constraint);
            }
        }
    }

    boolean isConstraintsCached()
    {
        return constraints != null;
    }

    public List<OracleForeignKey> getReferences(DBRProgressMonitor monitor)
        throws DBException
    {
        List<OracleForeignKey> refs = new ArrayList<OracleForeignKey>();
        // This is dummy implementation
        // Get references from this schema only
        final Collection<OracleConstraint> allConstraints = getContainer().getConstraintCache().getObjects(monitor, null);
        for (OracleConstraint constraint : allConstraints) {
            if (constraint instanceof OracleForeignKey && ((OracleForeignKey) constraint).getReferencedTable() == this) {
                refs.add((OracleForeignKey) constraint);
            }
        }
        return refs;
    }

    public List<OracleForeignKey> getForeignKeys(DBRProgressMonitor monitor)
        throws DBException
    {
        if (foreignKeys == null) {
            // Read constraints for ALL tables in this schema
            getContainer().getConstraintCache().getObjects(monitor, null);
        }
        return foreignKeys;
    }

    public OracleForeignKey getForeignKey(DBRProgressMonitor monitor, String fkName)
        throws DBException
    {
        return DBUtils.findObject(getForeignKeys(monitor), fkName);
    }

    public List<OracleTrigger> getTriggers(DBRProgressMonitor monitor)
        throws DBException
    {
        if (triggers == null) {
            triggers = loadTriggers(monitor);
        }
        return triggers;
    }

    public OracleTrigger getTrigger(DBRProgressMonitor monitor, String triggerName)
        throws DBException
    {
        return DBUtils.findObject(getTriggers(monitor), triggerName);
    }

    public List<OraclePartition> getPartitions(DBRProgressMonitor monitor)
        throws DBException
    {
        if (partitions == null) {
            partitions = loadPartitions(monitor);
        }
        return partitions;
    }


    public String getDDL(DBRProgressMonitor monitor)
        throws DBException
    {
        return "";
    }

    @Override
    public boolean refreshEntity(DBRProgressMonitor monitor) throws DBException
    {
        super.refreshEntity(monitor);
        getContainer().getIndexCache().clearCache();
        getContainer().getConstraintCache().clearCache();
        getContainer().getTriggerCache().clearCache();
        indexes = null;
        constraints = null;
        foreignKeys = null;
        triggers = null;
        return true;
    }

    private List<OracleTrigger> loadTriggers(DBRProgressMonitor monitor)
        throws DBException
    {
        List<OracleTrigger> tmpTriggers = new ArrayList<OracleTrigger>();
        if (!isPersisted()) {
            return tmpTriggers;
        }
        // Load only trigger's owner catalog and trigger name
        // Actual triggers are stored in catalog - we just get em from cache
        JDBCExecutionContext context = getDataSource().openContext(monitor, DBCExecutionPurpose.META, "Load table '" + getName() + "' triggers");
        try {
            JDBCPreparedStatement dbStat = context.prepareStatement(
                "SELECT " + OracleConstants.COL_TRIGGER_SCHEMA + "," + OracleConstants.COL_TRIGGER_NAME + " FROM " + OracleConstants.META_TABLE_TRIGGERS +
                " WHERE " + OracleConstants.COL_TRIGGER_EVENT_OBJECT_SCHEMA + "=? AND " + OracleConstants.COL_TRIGGER_EVENT_OBJECT_TABLE + "=? " +
                " ORDER BY " + OracleConstants.COL_TRIGGER_NAME);
            try {
                dbStat.setString(1, getContainer().getName());
                dbStat.setString(2, getName());
                JDBCResultSet dbResult = dbStat.executeQuery();
                try {
                    while (dbResult.next()) {
                        String ownerSchema = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_TRIGGER_SCHEMA);
                        String triggerName = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_TRIGGER_NAME);
                        OracleSchema triggerSchema = getDataSource().getSchema(monitor, ownerSchema);
                        if (triggerSchema == null) {
                            log.warn("Could not find catalog '" + ownerSchema + "'");
                            continue;
                        }
                        OracleTrigger trigger = triggerSchema.getTrigger(monitor, triggerName);
                        if (trigger == null) {
                            log.warn("Could not find trigger '" + triggerName + "' catalog '" + ownerSchema + "'");
                            continue;
                        }
                        tmpTriggers.add(trigger);
                    }
                    return tmpTriggers;
                }
                finally {
                    dbResult.close();
                }
            }
            finally {
                dbStat.close();
            }
        }
        catch (SQLException e) {
            throw new DBException(e);
        }
        finally {
            context.close();
        }
    }

    private List<OraclePartition> loadPartitions(DBRProgressMonitor monitor)
        throws DBException
    {
        List<OraclePartition> tmpPartitions = new ArrayList<OraclePartition>();
        if (!isPersisted()) {
            return tmpPartitions;
        }
        Map<String, OraclePartition> partitionMap = new HashMap<String, OraclePartition>();
        // Load only partition's owner catalog and partition name
        // Actual partitions are stored in catalog - we just get em from cache
        JDBCExecutionContext context = getDataSource().openContext(monitor, DBCExecutionPurpose.META, "Load table '" + getName() + "' partitions");
        try {
            JDBCPreparedStatement dbStat = context.prepareStatement(
                "SELECT * FROM " + OracleConstants.META_TABLE_PARTITIONS +
                " WHERE " + OracleConstants.COL_TABLE_SCHEMA + "=? AND " + OracleConstants.COL_TABLE_NAME + "=? " +
                " ORDER BY " + OracleConstants.COL_PARTITION_ORDINAL_POSITION + "," + OracleConstants.COL_SUBPARTITION_ORDINAL_POSITION);
            try {
                dbStat.setString(1, getContainer().getName());
                dbStat.setString(2, getName());
                JDBCResultSet dbResult = dbStat.executeQuery();
                try {
                    while (dbResult.next()) {
                        String partitionName = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_PARTITION_NAME);
                        String subPartitionName = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_SUBPARTITION_NAME);
                        if (CommonUtils.isEmpty(subPartitionName)) {
                            OraclePartition partition = new OraclePartition(this, null, partitionName, dbResult);
                            tmpPartitions.add(partition);
                        } else {
                            OraclePartition parentPartition = partitionMap.get(partitionName);
                            if (parentPartition == null) {
                                parentPartition = new OraclePartition(this, null, partitionName, dbResult);
                                tmpPartitions.add(parentPartition);
                                partitionMap.put(partitionName, parentPartition);
                            }
                            new OraclePartition(this, parentPartition, subPartitionName, dbResult);
                        }
                    }
                    return tmpPartitions;
                }
                finally {
                    dbResult.close();
                }
            }
            finally {
                dbStat.close();
            }
        }
        catch (SQLException e) {
            throw new DBException(e);
        }
        finally {
            context.close();
        }
    }


}
