/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    PRIMARY_KEY extends DBSEntityReferrer>
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
        return referencedKey == null ? null : (TABLE) referencedKey.getParentObject();
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
