/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCForeignKey;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSConstraintModifyRule;

import java.util.ArrayList;
import java.util.List;

/**
 * GenericForeignKey
 */
public class MySQLForeignKey extends JDBCForeignKey<MySQLTable, MySQLConstraint>
{
    private List<MySQLForeignKeyColumn> columns;

    public MySQLForeignKey(
        MySQLTable table,
        String name,
        String remarks,
        MySQLConstraint referencedKey,
        DBSConstraintModifyRule deleteRule,
        DBSConstraintModifyRule updateRule,
        boolean persisted)
    {
        super(table, name, remarks, referencedKey, deleteRule, updateRule, persisted);
    }

    public List<MySQLForeignKeyColumn> getColumns(DBRProgressMonitor monitor)
    {
        return columns;
    }

    void addColumn(MySQLForeignKeyColumn column)
    {
        if (columns == null) {
            columns = new ArrayList<MySQLForeignKeyColumn>();
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
}
