/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.model.impl.jdbc.struct;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKey;
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
    protected DBSForeignKeyModifyRule deleteRule;
    protected DBSForeignKeyModifyRule updateRule;

    public JDBCTableForeignKey(
        TABLE table,
        String name,
        String description,
        PRIMARY_KEY referencedKey,
        DBSForeignKeyModifyRule deleteRule,
        DBSForeignKeyModifyRule updateRule,
        boolean persisted)
    {
        super(table, name, description, DBSEntityConstraintType.FOREIGN_KEY, persisted);
        this.referencedKey = referencedKey;
        this.deleteRule = deleteRule;
        this.updateRule = updateRule;
    }

    @Property(viewable = true, order = 3)
    public TABLE getReferencedTable()
    {
        return referencedKey.getTable();
    }

    @NotNull
    @Override
    @Property(id = "reference", viewable = true, order = 4)
    public PRIMARY_KEY getReferencedConstraint()
    {
        return referencedKey;
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, listProvider = ConstraintModifyRuleListProvider.class, order = 5)
    public DBSForeignKeyModifyRule getDeleteRule()
    {
        return deleteRule;
    }

    public void setDeleteRule(DBSForeignKeyModifyRule deleteRule)
    {
        this.deleteRule = deleteRule;
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, listProvider = ConstraintModifyRuleListProvider.class, order = 6)
    public DBSForeignKeyModifyRule getUpdateRule()
    {
        return updateRule;
    }

    public void setUpdateRule(DBSForeignKeyModifyRule updateRule)
    {
        this.updateRule = updateRule;
    }

    @Override
    public TABLE getAssociatedEntity()
    {
        return getReferencedTable();
    }

    public static class ConstraintModifyRuleListProvider implements IPropertyValueListProvider<JDBCTableForeignKey> {

        @Override
        public boolean allowCustomValue()
        {
            return false;
        }

        @Override
        public Object[] getPossibleValues(JDBCTableForeignKey foreignKey)
        {
            return new DBSForeignKeyModifyRule[] {
                DBSForeignKeyModifyRule.NO_ACTION,
                DBSForeignKeyModifyRule.CASCADE,
                DBSForeignKeyModifyRule.RESTRICT,
                DBSForeignKeyModifyRule.SET_NULL,
                DBSForeignKeyModifyRule.SET_DEFAULT };
        }
    }

}
