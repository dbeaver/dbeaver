package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.dbeaver.model.anno.Property;
import org.jkiss.dbeaver.model.impl.meta.AbstractForeignKey;
import org.jkiss.dbeaver.model.struct.DBSConstraintCascade;
import org.jkiss.dbeaver.model.struct.DBSConstraintDefferability;

import java.util.ArrayList;
import java.util.List;

/**
 * GenericForeignKey
 */
public class MySQLForeignKey extends AbstractForeignKey<MySQLDataSource, MySQLTable, MySQLConstraint>
{
    private DBSConstraintDefferability defferability;
    private List<MySQLForeignKeyColumn> columns;

    public MySQLForeignKey(
        MySQLTable table,
        String name,
        String remarks,
        MySQLConstraint referencedKey,
        DBSConstraintCascade deleteRule,
        DBSConstraintCascade updateRule,
        DBSConstraintDefferability defferability)
    {
        super(table, name, remarks, referencedKey, deleteRule, updateRule);
        this.defferability = defferability;
    }

    @Property(name = "Defferability", viewable = true, order = 7)
    public DBSConstraintDefferability getDefferability()
    {
        return defferability;
    }

    public List<MySQLForeignKeyColumn> getColumns()
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
}
