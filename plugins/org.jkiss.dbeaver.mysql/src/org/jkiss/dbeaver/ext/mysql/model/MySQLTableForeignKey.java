/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCTableForeignKey;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSConstraintModifyRule;

import java.util.ArrayList;
import java.util.List;

/**
 * GenericForeignKey
 */
public class MySQLTableForeignKey extends JDBCTableForeignKey<MySQLTable, MySQLTableConstraint>
{
    private List<MySQLTableForeignKeyColumnTable> columns;

    public MySQLTableForeignKey(
        MySQLTable table,
        String name,
        String remarks,
        MySQLTableConstraint referencedKey,
        DBSConstraintModifyRule deleteRule,
        DBSConstraintModifyRule updateRule,
        boolean persisted)
    {
        super(table, name, remarks, referencedKey, deleteRule, updateRule, persisted);
    }

    public List<MySQLTableForeignKeyColumnTable> getColumns(DBRProgressMonitor monitor)
    {
        return columns;
    }

    public void addColumn(MySQLTableForeignKeyColumnTable column)
    {
        if (columns == null) {
            columns = new ArrayList<MySQLTableForeignKeyColumnTable>();
        }
        columns.add(column);
    }

    public String getFullQualifiedName()
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getTable().getContainer(),
            getTable(),
            this);
    }

    public MySQLDataSource getDataSource()
    {
        return getTable().getDataSource();
    }
}
