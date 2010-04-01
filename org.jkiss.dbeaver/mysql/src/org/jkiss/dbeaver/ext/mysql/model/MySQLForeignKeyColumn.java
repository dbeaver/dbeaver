package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.dbeaver.model.struct.DBSForeignKeyColumn;
import org.jkiss.dbeaver.model.anno.Property;

/**
 * GenericConstraintColumn
 */
public class MySQLForeignKeyColumn extends MySQLConstraintColumn implements DBSForeignKeyColumn<MySQLDataSource>
{
    private MySQLTableColumn referencedColumn;

    public MySQLForeignKeyColumn(
        MySQLForeignKey constraint,
        MySQLTableColumn tableColumn,
        int ordinalPosition,
        MySQLTableColumn referencedColumn)
    {
        super(constraint, tableColumn, ordinalPosition);
        this.referencedColumn = referencedColumn;
    }

    @Property(name = "Reference Column", viewable = true, order = 3)
    public MySQLTableColumn getReferencedColumn()
    {
        return referencedColumn;
    }
}