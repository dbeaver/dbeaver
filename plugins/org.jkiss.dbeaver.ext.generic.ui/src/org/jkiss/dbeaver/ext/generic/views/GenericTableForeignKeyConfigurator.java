/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.generic.views;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.GenericTableForeignKey;
import org.jkiss.dbeaver.ext.generic.model.GenericTableForeignKeyColumnTable;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLForeignKeyManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityReferrer;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyDeferability;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EditForeignKeyPage;

import java.util.Map;

/**
 * Generic table foreign key configurator
 */
public class GenericTableForeignKeyConfigurator implements DBEObjectConfigurator<GenericTableForeignKey> {
    @Override
    public GenericTableForeignKey configureObject(
        @NotNull DBRProgressMonitor monitor,
        @Nullable DBECommandContext commandContext,
        @Nullable Object table,
        @NotNull GenericTableForeignKey foreignKey,
        @NotNull Map<String, Object> options
    ) {
        return UITask.run(() -> {
            EditForeignKeyPage editPage = new EditForeignKeyPage(
                "Create foreign key",
                foreignKey,
                new DBSForeignKeyModifyRule[] {
                    DBSForeignKeyModifyRule.NO_ACTION,
                    DBSForeignKeyModifyRule.CASCADE, DBSForeignKeyModifyRule.RESTRICT,
                    DBSForeignKeyModifyRule.SET_NULL,
                    DBSForeignKeyModifyRule.SET_DEFAULT }, options);
            if (!editPage.edit()) {
                return null;
            }

            foreignKey.setDeleteRule(editPage.getOnDeleteRule());
            foreignKey.setUpdateRule(editPage.getOnUpdateRule());
            foreignKey.setReferencedConstraint((DBSEntityReferrer) editPage.getUniqueConstraint());
            foreignKey.setDeferability(DBSForeignKeyDeferability.NOT_DEFERRABLE);

            int colIndex = 1;
            for (EditForeignKeyPage.FKColumnInfo tableColumn : editPage.getColumns()) {
                foreignKey.addColumn(
                    new GenericTableForeignKeyColumnTable(
                        foreignKey,
                        tableColumn.getOrCreateOwnColumn(monitor, commandContext, foreignKey.getTable()),
                        colIndex++,
                        tableColumn.getRefColumn()));
            }
            SQLForeignKeyManager.updateForeignKeyName(monitor, foreignKey);
            return foreignKey;
        });
    }

}
