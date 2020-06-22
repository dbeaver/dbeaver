/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

package org.jkiss.dbeaver.ext.mysql.ui.config;

import org.jkiss.dbeaver.ext.mysql.model.MySQLTableColumn;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableUniqueKey;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableForeignKey;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableForeignKeyColumn;
import org.jkiss.dbeaver.ext.mysql.ui.internal.MySQLUIMessages;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EditForeignKeyPage;

/**
 * MySQL index configurator
 */
public class MySQLForeignKeyConfigurator implements DBEObjectConfigurator<MySQLTableForeignKey> {


    @Override
    public MySQLTableForeignKey configureObject(DBRProgressMonitor monitor, Object table, MySQLTableForeignKey foreignKey) {
        return UITask.run(() -> {
            EditForeignKeyPage editPage = new EditForeignKeyPage(
                MySQLUIMessages.edit_foreign_key_manager_title,
                foreignKey,
                new DBSForeignKeyModifyRule[] {
                    DBSForeignKeyModifyRule.NO_ACTION,
                    DBSForeignKeyModifyRule.CASCADE, DBSForeignKeyModifyRule.RESTRICT,
                    DBSForeignKeyModifyRule.SET_NULL,
                    DBSForeignKeyModifyRule.SET_DEFAULT });
            if (!editPage.edit()) {
                return null;
            }

            foreignKey.setReferencedKey((MySQLTableUniqueKey) editPage.getUniqueConstraint());
            foreignKey.setDeleteRule(editPage.getOnDeleteRule());
            foreignKey.setUpdateRule(editPage.getOnUpdateRule());
            int colIndex = 1;
            for (EditForeignKeyPage.FKColumnInfo tableColumn : editPage.getColumns()) {
                foreignKey.addColumn(
                    new MySQLTableForeignKeyColumn(
                        foreignKey,
                        (MySQLTableColumn) tableColumn.getOwnColumn(),
                        colIndex++,
                        (MySQLTableColumn) tableColumn.getRefColumn()));
            }
            return foreignKey;
        });
    }

}
