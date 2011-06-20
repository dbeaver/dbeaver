/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCConstraint;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSConstraintType;

import java.util.ArrayList;
import java.util.List;

/**
 * GenericPrimaryKey
 */
public class MySQLConstraint extends JDBCConstraint<MySQLTable> {
    private List<MySQLConstraintColumn> columns;

    public MySQLConstraint(MySQLTable table, String name, String remarks, DBSConstraintType constraintType, boolean persisted)
    {
        super(table, name, remarks, constraintType, persisted);
    }

    public List<MySQLConstraintColumn> getColumns(DBRProgressMonitor monitor)
    {
        return columns;
    }

    public void addColumn(MySQLConstraintColumn column)
    {
        if (columns == null) {
            columns = new ArrayList<MySQLConstraintColumn>();
        }
        this.columns.add(column);
    }

    void setColumns(List<MySQLConstraintColumn> columns)
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
