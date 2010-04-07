package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.dbeaver.model.impl.meta.AbstractConstraint;
import org.jkiss.dbeaver.model.struct.DBSConstraintType;
import org.jkiss.dbeaver.model.struct.DBSConstraint;

import java.util.ArrayList;
import java.util.List;

/**
 * GenericPrimaryKey
 */
public class MySQLConstraint extends AbstractConstraint<MySQLDataSource,MySQLCatalog,MySQLTable> implements DBSConstraint<MySQLDataSource, MySQLTable> {
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

    public List<MySQLConstraintColumn> getColumns()
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
