package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.dbeaver.model.impl.meta.AbstractPrimaryKey;

import java.util.ArrayList;
import java.util.List;

/**
 * GenericPrimaryKey
 */
public class MySQLPrimaryKey extends AbstractPrimaryKey<MySQLDataSource, MySQLCatalog, MySQLTable>
{
    private List<MySQLConstraintColumn> columns;

    public MySQLPrimaryKey(MySQLTable table, String name, String remarks)
    {
        super(table, name, remarks);
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
