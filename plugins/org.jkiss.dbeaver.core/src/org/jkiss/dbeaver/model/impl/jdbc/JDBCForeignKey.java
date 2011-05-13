/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc;

import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCConstraint;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;

/**
 * JDBCForeignKey
 */
public abstract class JDBCForeignKey<
    TABLE extends JDBCTable,
    PRIMARY_KEY extends JDBCConstraint<TABLE>>
    extends JDBCConstraint<TABLE>
    implements DBSForeignKey
{
    protected PRIMARY_KEY referencedKey;
    protected DBSConstraintModifyRule deleteRule;
    protected DBSConstraintModifyRule updateRule;

    public JDBCForeignKey(
        TABLE table,
        String name,
        String description,
        PRIMARY_KEY referencedKey,
        DBSConstraintModifyRule deleteRule,
        DBSConstraintModifyRule updateRule,
        boolean persisted)
    {
        super(table, name, description, DBSConstraintType.FOREIGN_KEY, persisted);
        this.referencedKey = referencedKey;
        this.deleteRule = deleteRule;
        this.updateRule = updateRule;
    }

    @Property(name = "Ref Table", viewable = true, order = 3)
    public TABLE getReferencedTable()
    {
        return referencedKey.getTable();
    }

    @Property(id = "reference", name = "Ref Object", viewable = true, order = 4)
    public PRIMARY_KEY getReferencedKey()
    {
        return referencedKey;
    }

    @Property(name = "On Delete", viewable = true, order = 5)
    public DBSConstraintModifyRule getDeleteRule()
    {
        return deleteRule;
    }

    @Property(name = "On Update", viewable = true, order = 6)
    public DBSConstraintModifyRule getUpdateRule()
    {
        return updateRule;
    }

    public DBSForeignKeyColumn getColumn(DBRProgressMonitor monitor, DBSTableColumn tableColumn)
    {
        return (DBSForeignKeyColumn)super.getColumn(monitor, tableColumn);
    }

    public DBSEntity getAssociatedEntity()
    {
        return getReferencedTable();
    }

}