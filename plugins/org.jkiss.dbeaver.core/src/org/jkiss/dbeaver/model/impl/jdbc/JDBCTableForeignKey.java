/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc;

import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableConstraint;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.ui.properties.IPropertyValueListProvider;

/**
 * JDBCTableForeignKey
 */
public abstract class JDBCTableForeignKey<
    TABLE extends JDBCTable,
    PRIMARY_KEY extends JDBCTableConstraint<TABLE>>
    extends JDBCTableConstraint<TABLE>
    implements DBSTableForeignKey
{
    protected PRIMARY_KEY referencedKey;
    protected DBSConstraintModifyRule deleteRule;
    protected DBSConstraintModifyRule updateRule;

    public JDBCTableForeignKey(
        TABLE table,
        String name,
        String description,
        PRIMARY_KEY referencedKey,
        DBSConstraintModifyRule deleteRule,
        DBSConstraintModifyRule updateRule,
        boolean persisted)
    {
        super(table, name, description, DBSEntityConstraintType.FOREIGN_KEY, persisted);
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
    public PRIMARY_KEY getReferencedConstraint()
    {
        return referencedKey;
    }

    @Property(name = "On Delete", viewable = true, editable = true, listProvider = ConstraintModifyRuleListProvider.class, order = 5)
    public DBSConstraintModifyRule getDeleteRule()
    {
        return deleteRule;
    }

    public void setDeleteRule(DBSConstraintModifyRule deleteRule)
    {
        this.deleteRule = deleteRule;
    }

    @Property(name = "On Update", viewable = true, editable = true, listProvider = ConstraintModifyRuleListProvider.class, order = 6)
    public DBSConstraintModifyRule getUpdateRule()
    {
        return updateRule;
    }

    public void setUpdateRule(DBSConstraintModifyRule updateRule)
    {
        this.updateRule = updateRule;
    }

    public TABLE getAssociatedEntity()
    {
        return getReferencedTable();
    }

    public static class ConstraintModifyRuleListProvider implements IPropertyValueListProvider<JDBCTableForeignKey> {

        public boolean allowCustomValue()
        {
            return false;
        }

        public Object[] getPossibleValues(JDBCTableForeignKey foreignKey)
        {
            return new DBSConstraintModifyRule[] {
                DBSConstraintModifyRule.NO_ACTION,
                DBSConstraintModifyRule.CASCADE,
                DBSConstraintModifyRule.RESTRICT,
                DBSConstraintModifyRule.SET_NULL,
                DBSConstraintModifyRule.SET_DEFAULT };
        }
    }

}