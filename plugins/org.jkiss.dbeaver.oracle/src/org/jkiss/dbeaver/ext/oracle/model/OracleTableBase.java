/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;
import java.util.Collection;
import java.util.List;

/**
 * OracleTable base
 */
public abstract class OracleTableBase extends JDBCTable<OracleDataSource, OracleSchema> implements DBPNamedObject2
{
    static final Log log = LogFactory.getLog(OracleTableBase.class);

    private String comment;
    private List<OracleTableColumn> columns;

    protected OracleTableBase(OracleSchema schema, boolean persisted)
    {
        super(schema, persisted);
    }

    protected OracleTableBase(OracleSchema oracleSchema, ResultSet dbResult)
    {
        super(oracleSchema, true);
        setName(JDBCUtils.safeGetString(dbResult, "TABLE_NAME"));
        this.comment = JDBCUtils.safeGetString(dbResult, "COMMENTS");
    }

    @Property(name = "Comments", viewable = true, editable = true, order = 100)
    public String getDescription()
    {
        return comment;
    }

    public String getFullQualifiedName()
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getContainer(),
            this);
    }

    @Override
    public String getTableType()
    {
        return super.getTableType();
    }

    public List<OracleTableColumn> getColumns(DBRProgressMonitor monitor)
        throws DBException
    {
        if (columns == null) {
            getContainer().tableCache.loadChildren(monitor, getContainer(), this);
        }
        return columns;
    }

    public OracleTableColumn getColumn(DBRProgressMonitor monitor, String columnName)
        throws DBException
    {
        return DBUtils.findObject(getColumns(monitor), columnName);
    }

    @Override
    public boolean refreshEntity(DBRProgressMonitor monitor) throws DBException
    {
        columns = null;
        return true;
    }

    public boolean isColumnsCached()
    {
        return columns != null;
    }

    public void setColumns(List<OracleTableColumn> columns)
    {
        this.columns = columns;
    }

    @Association
    public Collection<OracleTrigger> getTriggers(DBRProgressMonitor monitor)
        throws DBException
    {
        return getContainer().triggerCache.getObjects(monitor, getContainer(), this);
    }

    public String getDDL(DBRProgressMonitor monitor)
        throws DBException
    {
        return "";
    }

    public static OracleTableBase findTable(DBRProgressMonitor monitor, OracleDataSource dataSource, String ownerName, String tableName) throws DBException
    {
        OracleSchema refSchema = dataSource.getSchema(monitor, ownerName);
        if (refSchema == null) {
            log.warn("Referenced schema '" + ownerName + "' not found");
            return null;
        } else {
            OracleTableBase refTable = refSchema.tableCache.getObject(monitor, refSchema, tableName);
            if (refTable == null) {
                log.warn("Referenced table '" + tableName + "' not found in schema '" + ownerName + "'");
            }
            return refTable;
        }
    }

}
