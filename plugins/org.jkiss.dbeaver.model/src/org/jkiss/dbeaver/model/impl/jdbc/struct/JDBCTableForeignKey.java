/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.model.impl.jdbc.struct;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKey;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;

import java.util.List;

/**
 * JDBCTableForeignKey
 */
public abstract class JDBCTableForeignKey<
    TABLE extends JDBCTable,
    PRIMARY_KEY extends JDBCTableConstraint<TABLE>>
    extends JDBCTableConstraint<TABLE>
    implements DBSTableForeignKey
{
    @Nullable
    protected PRIMARY_KEY referencedKey;
    protected DBSForeignKeyModifyRule deleteRule;
    protected DBSForeignKeyModifyRule updateRule;

    public JDBCTableForeignKey(
        @NotNull TABLE table,
        @NotNull String name,
        @Nullable String description,
        @Nullable PRIMARY_KEY referencedKey,
        DBSForeignKeyModifyRule deleteRule,
        DBSForeignKeyModifyRule updateRule,
        boolean persisted)
    {
        super(table, name, description, DBSEntityConstraintType.FOREIGN_KEY, persisted);
        this.referencedKey = referencedKey;
        this.deleteRule = deleteRule;
        this.updateRule = updateRule;
    }

    public JDBCTableForeignKey(
        @NotNull DBRProgressMonitor monitor,
        @NotNull TABLE table,
        @NotNull DBSEntityAssociation source,
        boolean persisted) throws DBException {
        super(table, source, persisted);

        DBSEntityConstraint srcRefConstraint = source.getReferencedConstraint();
        if (srcRefConstraint != null) {
            DBSEntity refEntity = srcRefConstraint.getParentObject();
            if (refEntity != null) {
                if (srcRefConstraint instanceof JDBCTableConstraint && refEntity.getParentObject() == table.getParentObject()) {
                    // Referenced object in the same schema as we are - let's just use it
                    this.referencedKey = (PRIMARY_KEY) srcRefConstraint;
                } else {
                    // Try to find table with the same name as referenced constraint owner
                    DBSObject refTable = table.getContainer().getChild(monitor, refEntity.getName());
                    if (refTable instanceof DBSEntity) {
                        List<DBSEntityAttribute> refAttrs = DBUtils.getEntityAttributes(monitor, referencedKey);
                        this.referencedKey = (PRIMARY_KEY) DBUtils.findEntityConstraint(monitor, (DBSEntity) refTable, refAttrs);
                    }
                }
            }
        }

        if (source instanceof DBSTableForeignKey) {
            this.deleteRule = ((DBSTableForeignKey)source).getDeleteRule();
            this.updateRule = ((DBSTableForeignKey)source).getUpdateRule();
        } else {
            this.deleteRule = DBSForeignKeyModifyRule.NO_ACTION;
            this.updateRule = DBSForeignKeyModifyRule.NO_ACTION;
        }
    }

    @Nullable
    @Property(viewable = true, order = 3)
    public TABLE getReferencedTable()
    {
        return referencedKey == null ? null : referencedKey.getTable();
    }

    @Nullable
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
