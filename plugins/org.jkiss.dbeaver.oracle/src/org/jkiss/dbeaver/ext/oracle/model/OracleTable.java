/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * OracleTable
 */
public class OracleTable extends OracleTableBase
{

    private List<OracleIndex> indexes;
    private List<OracleConstraint> constraints;
    private List<OracleForeignKey> foreignKeys;
    private List<OracleTrigger> triggers;

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

    @Association
    public List<OracleIndex> getIndexes(DBRProgressMonitor monitor)
        throws DBException
    {
        if (indexes == null) {
            // Read indexes using cache
            this.getContainer().getIndexCache().getObjects(monitor, getDataSource(), this);
        }
        return indexes;
    }

//    public OracleIndex getIndex(DBRProgressMonitor monitor, String indexName)
//        throws DBException
//    {
//        return DBUtils.findObject(getIndexes(monitor), indexName);
//    }

    boolean isIndexesCached()
    {
        return indexes != null;
    }

    void setIndexes(List<OracleIndex> indexes)
    {
        this.indexes = indexes;
    }

    @Association
    public List<OracleConstraint> getConstraints(DBRProgressMonitor monitor)
        throws DBException
    {
        if (constraints == null) {
            getContainer().getConstraintCache().getObjects(monitor, getDataSource(), this);
        }
        return constraints;
    }

    public OracleConstraint getConstraint(DBRProgressMonitor monitor, String ukName)
        throws DBException
    {
        return DBUtils.findObject(getConstraints(monitor), ukName);
    }

    void setConstraints(List<OracleConstraint> constraints)
    {
        this.constraints = constraints;
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
        final Collection<OracleForeignKey> allForeignKeys = getContainer().getForeignKeyCache().getObjects(monitor, getDataSource(), null);
        for (OracleForeignKey constraint : allForeignKeys) {
            if (constraint.getReferencedTable() == this) {
                refs.add(constraint);
            }
        }
        return refs;
    }

    @Association
    public List<OracleForeignKey> getForeignKeys(DBRProgressMonitor monitor)
        throws DBException
    {
        if (foreignKeys == null) {
            getContainer().getForeignKeyCache().getObjects(monitor, getDataSource(), this);
        }
        return foreignKeys;
    }

    void setForeignKeys(List<OracleForeignKey> constraints)
    {
        this.foreignKeys = constraints;
    }

    boolean isForeignKeysCached()
    {
        return foreignKeys != null;
    }

//    public OracleForeignKey getForeignKey(DBRProgressMonitor monitor, String fkName)
//        throws DBException
//    {
//        return DBUtils.findObject(getForeignKeys(monitor), fkName);
//    }

    @Association
    public Collection<OracleTrigger> getTriggers(DBRProgressMonitor monitor)
        throws DBException
    {
        if (triggers == null) {
            return getContainer().getTriggerCache().getObjects(monitor, getDataSource(), this);
        }
        return triggers;
    }

//    public OracleTrigger getTrigger(DBRProgressMonitor monitor, String triggerName)
//        throws DBException
//    {
//        return DBUtils.findObject(getTriggers(monitor), triggerName);
//    }

    @Association
    public List<OraclePartition> getPartitions(DBRProgressMonitor monitor)
        throws DBException
    {
        return null;
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
        getContainer().getForeignKeyCache().clearCache();
        getContainer().getTriggerCache().clearCache();

        indexes = null;
        constraints = null;
        foreignKeys = null;
        triggers = null;
        return true;
    }

}
