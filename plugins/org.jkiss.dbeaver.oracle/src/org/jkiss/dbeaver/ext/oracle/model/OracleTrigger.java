/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTrigger;

import java.sql.ResultSet;
import java.util.Collection;
import java.util.List;

/**
 * GenericProcedure
 */
public class OracleTrigger extends OracleSchemaObject implements DBSTrigger
{
    static final Log log = LogFactory.getLog(OracleTrigger.class);

    private OracleTableBase table;
    private List<OracleTriggerColumn> columns;

    public OracleTrigger(
        OracleSchema schema,
        OracleTableBase table,
        ResultSet dbResult)
    {
        super(schema, JDBCUtils.safeGetString(dbResult, "TRIGGER_NAME"), true);
        this.table = table;
    }

    @Property(name = "Name", viewable = true, editable = true, order = 1)
    public String getName()
    {
        return super.getName();
    }

    @Property(name = "Table", viewable = true, order = 4)
    public OracleTableBase getTable()
    {
        return table;
    }

    @Association
    public Collection<OracleTriggerColumn> getColumns(DBRProgressMonitor monitor) throws DBException
    {
        if (columns == null) {
            getSchema().triggerCache.loadChildren(monitor, getSchema(), this);
        }
        return columns;
    }

    boolean isColumnsCached()
    {
        return columns != null;
    }

    void setColumns(List<OracleTriggerColumn> columns)
    {
        this.columns = columns;
    }

}