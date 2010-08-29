/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.impl.struct.AbstractForeignKey;
import org.jkiss.dbeaver.model.struct.DBSConstraintCascade;
import org.jkiss.dbeaver.model.struct.DBSConstraintDefferability;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

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
}
