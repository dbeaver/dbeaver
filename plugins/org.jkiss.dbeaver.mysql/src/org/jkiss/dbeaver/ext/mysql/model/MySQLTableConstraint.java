/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableConstraint;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;

import java.util.ArrayList;
import java.util.List;

/**
 * GenericPrimaryKey
 */
public class MySQLTableConstraint extends JDBCTableConstraint<MySQLTable> {
    private List<MySQLTableConstraintColumn> columns;

    public MySQLTableConstraint(MySQLTable table, String name, String remarks, DBSEntityConstraintType constraintType, boolean persisted)
    {
        super(table, name, remarks, constraintType, persisted);
    }

    public List<MySQLTableConstraintColumn> getColumns(DBRProgressMonitor monitor)
    {
        return columns;
    }

    public void addColumn(MySQLTableConstraintColumn column)
    {
        if (columns == null) {
            columns = new ArrayList<MySQLTableConstraintColumn>();
        }
        this.columns.add(column);
    }

    void setColumns(List<MySQLTableConstraintColumn> columns)
    {
        this.columns = columns;
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
