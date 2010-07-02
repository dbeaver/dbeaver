package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCConstraint;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSConstraintType;

import java.util.ArrayList;
import java.util.List;

/**
 * GenericPrimaryKey
 */
public class MySQLConstraint extends JDBCConstraint<MySQLDataSource,MySQLTable> {
    private DBSConstraintType constraintType;
    private List<MySQLConstraintColumn> columns;

    public MySQLConstraint(DBSConstraintType constraintType, MySQLTable table, String name, String remarks)
    {
        super(table, name, remarks);
        this.constraintType = constraintType;
    }

    public DBSConstraintType getConstraintType()
    {
        return constraintType;
    }

    public List<MySQLConstraintColumn> getColumns(DBRProgressMonitor monitor)
    {
        return columns;
    }

    void addColumn(MySQLConstraintColumn column)
    {
        if (columns == null) {
            columns = new ArrayList<MySQLConstraintColumn>();
        }
        this.columns.add(column);
    }
}
