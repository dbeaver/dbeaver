/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.ext.generic.model.GenericTableForeignKey;
import org.jkiss.dbeaver.ext.generic.model.GenericTableForeignKeyColumnTable;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.struct.DBSEntityReferrer;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyDeferability;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EditForeignKeyPage;

/**
 * Generic table foreign key configurator
 */
public class GenericTableForeignKeyConfigurator implements DBEObjectConfigurator<GenericTable, GenericTableForeignKey> {
    @Override
    public GenericTableForeignKey configureObject(GenericTable table, GenericTableForeignKey foreignKey) {
        return new UITask<GenericTableForeignKey>() {
            @Override
            protected GenericTableForeignKey runTask() {
                EditForeignKeyPage editPage = new EditForeignKeyPage(
                    "Create foreign key",
                    table,
                    new DBSForeignKeyModifyRule[] {
                        DBSForeignKeyModifyRule.NO_ACTION,
                        DBSForeignKeyModifyRule.CASCADE, DBSForeignKeyModifyRule.RESTRICT,
                        DBSForeignKeyModifyRule.SET_NULL,
                        DBSForeignKeyModifyRule.SET_DEFAULT });
                if (!editPage.edit()) {
                    return null;
                }

                foreignKey.setDeleteRule(editPage.getOnDeleteRule());
                foreignKey.setUpdateRule(editPage.getOnUpdateRule());
                foreignKey.setReferencedKey((DBSEntityReferrer) editPage.getUniqueConstraint());
                foreignKey.setDeferability(DBSForeignKeyDeferability.NOT_DEFERRABLE);
                int colIndex = 1;
                for (EditForeignKeyPage.FKColumnInfo tableColumn : editPage.getColumns()) {
                    foreignKey.addColumn(
                        new GenericTableForeignKeyColumnTable(
                            foreignKey,
                            (GenericTableColumn) tableColumn.getOwnColumn(),
                            colIndex++,
                            (GenericTableColumn) tableColumn.getRefColumn()));
                }
                return foreignKey;
            }
        }.execute();
    }

}
