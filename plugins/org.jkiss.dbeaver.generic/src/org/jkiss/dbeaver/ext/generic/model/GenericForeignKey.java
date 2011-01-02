/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;

import java.util.ArrayList;
import java.util.List;

/**
 * GenericForeignKey
 */
public class GenericForeignKey extends GenericConstraint implements DBSForeignKey
{
    private GenericConstraint referencedKey;
    private DBSConstraintCascade deleteRule;
    private DBSConstraintCascade updateRule;
    private DBSConstraintDefferability defferability;
    private List<GenericForeignKeyColumn> columns;

    public GenericForeignKey(GenericTable table,
        String name,
        String remarks,
        GenericConstraint referencedKey,
        DBSConstraintCascade deleteRule,
        DBSConstraintCascade updateRule,
        DBSConstraintDefferability defferability)
    {
        super(table, name, remarks, DBSConstraintType.FOREIGN_KEY);
        this.referencedKey = referencedKey;
        this.deleteRule = deleteRule;
        this.updateRule = updateRule;
        this.defferability = defferability;
    }

    @Property(name = "Ref Table", viewable = true, order = 3)
    public GenericTable getReferencedTable()
    {
        return referencedKey.getTable();
    }

    @Property(name = "Ref Constraint", viewable = true, order = 4)
    public GenericConstraint getReferencedKey()
    {
        return referencedKey;
    }

    @Property(name = "On Delete", viewable = true, order = 5)
    public DBSConstraintCascade getDeleteRule()
    {
        return deleteRule;
    }

    @Property(name = "On Update", viewable = true, order = 6)
    public DBSConstraintCascade getUpdateRule()
    {
        return updateRule;
    }

    @Property(name = "Defferability", viewable = true, order = 7)
    public DBSConstraintDefferability getDefferability()
    {
        return defferability;
    }

    public GenericForeignKeyColumn getColumn(DBRProgressMonitor monitor, DBSTableColumn tableColumn)
    {
        return (GenericForeignKeyColumn)super.getColumn(monitor, tableColumn);
    }

    public List<GenericForeignKeyColumn> getColumns(DBRProgressMonitor monitor)
    {
        return columns;
    }

    void addColumn(GenericForeignKeyColumn column)
    {
        if (columns == null) {
            columns = new ArrayList<GenericForeignKeyColumn>();
        }
        this.columns.add(column);
    }

    public DBSEntity getAssociatedEntity()
    {
        return getReferencedTable();
    }
}
