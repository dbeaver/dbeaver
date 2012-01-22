/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCTableForeignKey;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;

import java.util.ArrayList;
import java.util.List;

/**
 * GenericTableForeignKey
 */
public class GenericTableForeignKey extends JDBCTableForeignKey<GenericTable, GenericPrimaryKey>
{
    private DBSConstraintDefferability defferability;
    private List<GenericTableForeignKeyColumnTable> columns;

    public GenericTableForeignKey(GenericTable table,
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

    public GenericTableForeignKeyColumnTable getColumn(DBRProgressMonitor monitor, DBSTableColumn tableColumn)
    {
        return (GenericTableForeignKeyColumnTable)super.getColumn(monitor, tableColumn);
    }

    public List<GenericTableForeignKeyColumnTable> getColumns(DBRProgressMonitor monitor)
    {
        return columns;
    }

    public void addColumn(GenericTableForeignKeyColumnTable column)
    {
        if (columns == null) {
            columns = new ArrayList<GenericTableForeignKeyColumnTable>();
        }
        this.columns.add(column);
    }

    void setColumns(List<GenericTableForeignKeyColumnTable> columns)
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
