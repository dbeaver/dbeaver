/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.tibero.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKey;

public class TiberoTableForeignKey extends TiberoTableConstraintBase implements DBSTableForeignKey {

    private TiberoTableConstraint referencedKey;
    private DBSForeignKeyModifyRule deleteRule;

    public TiberoTableForeignKey(
        @NotNull TiberoTable table,
        @NotNull String name,
        @Nullable TiberoTableConstraint referencedKey,
        @NotNull DBSForeignKeyModifyRule deleteRule,
        boolean persisted
    ) {
        super(table, name, DBSEntityConstraintType.FOREIGN_KEY, persisted);
        this.referencedKey = referencedKey;
        this.deleteRule = deleteRule;
    }

    @Nullable
    @Property(viewable = true, order = 3)
    public TiberoTable getReferencedTable() {
        return referencedKey == null ? null : referencedKey.getTable();
    }

    @Nullable
    @Override
    @Property(id = "reference", viewable = true, order = 4)
    public TiberoTableConstraint getReferencedConstraint() {
        return referencedKey;
    }

    public void setReferencedConstraint(TiberoTableConstraint referencedKey) {
        this.referencedKey = referencedKey;
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, listProvider = ConstraintModifyRuleListProvider.class, order = 5)
    public DBSForeignKeyModifyRule getDeleteRule() {
        return deleteRule;
    }

    public void setDeleteRule(DBSForeignKeyModifyRule deleteRule) {
        this.deleteRule = deleteRule;
    }

    @NotNull
    @Override
    public DBSForeignKeyModifyRule getUpdateRule() {
        return DBSForeignKeyModifyRule.NO_ACTION;
    }

    @Nullable
    @Override
    public TiberoTable getAssociatedEntity() {
        return getReferencedTable();
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(@NotNull DBPEvaluationContext context) {
        return DBUtils.getFullQualifiedName(
            getDataSource(),
            getTable().getContainer(),
            getTable(),
            this);
    }

    @NotNull
    public static DBSForeignKeyModifyRule parseDeleteRule(@Nullable String code) {
        if (code == null) {
            return DBSForeignKeyModifyRule.NO_ACTION;
        }
        return switch (code) {
            case "CASCADE" -> DBSForeignKeyModifyRule.CASCADE;
            case "SET NULL" -> DBSForeignKeyModifyRule.SET_NULL;
            default -> DBSForeignKeyModifyRule.NO_ACTION;
        };
    }

    public static class ConstraintModifyRuleListProvider implements IPropertyValueListProvider<TiberoTableForeignKey> {
        @Override
        public boolean allowCustomValue() {
            return false;
        }

        @Override
        public Object[] getPossibleValues(TiberoTableForeignKey foreignKey) {
            return new DBSForeignKeyModifyRule[] {
                DBSForeignKeyModifyRule.NO_ACTION,
                DBSForeignKeyModifyRule.CASCADE,
                DBSForeignKeyModifyRule.SET_NULL
            };
        }
    }
}
