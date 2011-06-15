/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCForeignKey;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;

import java.util.ArrayList;
import java.util.List;

/**
 * GenericForeignKey
 */
public class GenericForeignKey extends JDBCForeignKey<GenericTable, GenericPrimaryKey>
{
    private DBSConstraintDefferability defferability;
    private List<GenericForeignKeyColumn> columns;

    public GenericForeignKey(GenericTable table,
        String name,
        String remarks,
        GenericPrimaryKey referencedKey,
        DBSConstraintModifyRule deleteRule,
        DBSConstraintModifyRule updateRule,
        DBSConstraintDefferability defferability,
        boolean persisted)
    {
        super(table, name, remarks, referencedKey, deleteRule, updateRule, persisted);
        this.defferability = defferability;
    }

    public GenericDataSource getDataSource()
    {
        return getTable().getDataSource();
    }

    @Property(name = "Defferability", viewable = true, order = 7)
    public DBSConstraintDefferability getDefferability()
    {
        return defferability;
    }

    public GenericForeignKeyColumn getColumn(DBRProgressMonitor monitor, DBSTableColumn tableColumn)
    {
        return (GenericForeignKeyColumn)super.getColumn(monitor, tableColumn);
    }

    public List<GenericForeignKeyColumn> getColumns(DBRProgressMonitor monitor)
    {
        return columns;
    }

    public void addColumn(GenericForeignKeyColumn column)
    {
        if (columns == null) {
            columns = new ArrayList<GenericForeignKeyColumn>();
        }
        this.columns.add(column);
    }

    void setColumns(List<GenericForeignKeyColumn> columns)
    {
        this.columns = columns;
    }

    public String getFullQualifiedName()
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getTable().getCatalog(),
            getTable().getSchema(),
            getTable(),
            this);
    }
}
