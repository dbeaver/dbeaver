/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractTriggerColumn;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;

import java.sql.ResultSet;

/**
 * OracleTriggerColumn
 */
public class OracleTriggerColumn extends AbstractTriggerColumn
{
    static final Log log = LogFactory.getLog(OracleTriggerColumn.class);

    private OracleTrigger trigger;
    private String name;
    private OracleTableColumn tableColumn;
    private boolean columnList;

    public OracleTriggerColumn(
        DBRProgressMonitor monitor,
        OracleTrigger trigger,
        OracleTableColumn tableColumn,
        ResultSet dbResult) throws DBException
    {
        this.trigger = trigger;
        this.tableColumn = tableColumn;
        this.name = JDBCUtils.safeGetString(dbResult, "COLUMN_NAME");
        this.columnList = JDBCUtils.safeGetBoolean(dbResult, "COLUMN_LIST", "YES");
    }

    OracleTriggerColumn(OracleTrigger trigger, OracleTriggerColumn source)
    {
        this.trigger = trigger;
        this.tableColumn = source.tableColumn;
        this.columnList = source.columnList;
    }

    @Override
    public OracleTrigger getTrigger()
    {
        return trigger;
    }

    @Override
    @Property(name = "Name", viewable = true, order = 1)
    public String getName()
    {
        return name;
    }

    @Override
    @Property(name = "Column", viewable = true, order = 2)
    public OracleTableColumn getTableColumn()
    {
        return tableColumn;
    }

    @Override
    public int getOrdinalPosition()
    {
        return 0;
    }

    @Override
    public String getDescription()
    {
        return tableColumn.getDescription();
    }

    @Override
    public OracleTrigger getParentObject()
    {
        return trigger;
    }

    @Override
    public OracleDataSource getDataSource()
    {
        return trigger.getDataSource();
    }

}
