/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * OracleTable
 */
public class OracleTable extends OracleTablePhysical
{
    private OracleDataType tableType;
    private boolean temporary;
    private boolean secondary;
    private boolean nested;

    public static class AdditionalInfo extends TableAdditionalInfo {
    }

    private final AdditionalInfo additionalInfo = new AdditionalInfo();

    private List<OracleConstraint> constraints;
    private List<OracleForeignKey> foreignKeys;

    public OracleTable(OracleSchema schema)
    {
        super(schema);
    }

    public OracleTable(
        DBRProgressMonitor monitor,
        OracleSchema schema,
        ResultSet dbResult)
    {
        super(schema, dbResult);
        String typeOwner = JDBCUtils.safeGetString(dbResult, "TABLE_TYPE_OWNER");
        if (!CommonUtils.isEmpty(typeOwner)) {
            tableType = OracleDataType.resolveDataType(
                monitor,
                schema.getDataSource(),
                typeOwner,
                JDBCUtils.safeGetString(dbResult, "TABLE_TYPE"));
        }
        this.temporary = JDBCUtils.safeGetBoolean(dbResult, "TEMPORARY", "Y");
        this.secondary = JDBCUtils.safeGetBoolean(dbResult, "SECONDARY", "Y");
        this.nested = JDBCUtils.safeGetBoolean(dbResult, "NESTED", "Y");
    }

    @Override
    public TableAdditionalInfo getAdditionalInfo()
    {
        return additionalInfo;
    }

    public boolean isView()
    {
        return false;
    }

    @Property(name = "Table Type", viewable = false, order = 5)
    public OracleDataType getTableType()
    {
        return tableType;
    }

    @Property(name = "Temporary", viewable = false, order = 6)
    public boolean isTemporary()
    {
        return temporary;
    }

    @Property(name = "Secondary", viewable = false, order = 6)
    public boolean isSecondary()
    {
        return secondary;
    }

    @Property(name = "Nested", viewable = false, order = 7)
    public boolean isNested()
    {
        return nested;
    }

    @Association
    public List<OracleConstraint> getConstraints(DBRProgressMonitor monitor)
        throws DBException
    {
        if (constraints == null) {
            getContainer().constraintCache.getObjects(monitor, getContainer(), this);
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
        final Collection<OracleForeignKey> allForeignKeys =
            getContainer().foreignKeyCache.getObjects(monitor, getContainer(), null);
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
            getContainer().foreignKeyCache.getObjects(monitor, getContainer(), this);
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

        constraints = null;
        foreignKeys = null;
        return true;
    }

}
