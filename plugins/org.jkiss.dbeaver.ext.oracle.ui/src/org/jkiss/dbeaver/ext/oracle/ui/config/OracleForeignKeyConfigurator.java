/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.oracle.ui.config;

import org.jkiss.dbeaver.ext.oracle.model.OracleTableColumn;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableConstraint;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableForeignKey;
import org.jkiss.dbeaver.ext.oracle.model.OracleTableForeignKeyColumn;
import org.jkiss.dbeaver.ext.oracle.ui.internal.OracleUIMessages;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EditForeignKeyPage;

/**
 * Oracle foreign key manager
 */
public class OracleForeignKeyConfigurator implements DBEObjectConfigurator<OracleTableForeignKey> {

    @Override
    public OracleTableForeignKey configureObject(DBRProgressMonitor monitor, Object table, OracleTableForeignKey foreignKey) {
        return UITask.run(() -> {
            EditForeignKeyPage editPage = new EditForeignKeyPage(
                OracleUIMessages.edit_oracle_foreign_key_manager_dialog_title,
                foreignKey,
                new DBSForeignKeyModifyRule[]{
                    DBSForeignKeyModifyRule.NO_ACTION,
                    DBSForeignKeyModifyRule.CASCADE, DBSForeignKeyModifyRule.RESTRICT,
                    DBSForeignKeyModifyRule.SET_NULL,
                    DBSForeignKeyModifyRule.SET_DEFAULT});
            editPage.setSupportsCustomName(true);
            if (!editPage.edit()) {
                return null;
            }

            foreignKey.setReferencedConstraint((OracleTableConstraint) editPage.getUniqueConstraint());
            foreignKey.setName(editPage.getName());
            foreignKey.setDeleteRule(editPage.getOnDeleteRule());
            int colIndex = 1;
            for (EditForeignKeyPage.FKColumnInfo tableColumn : editPage.getColumns()) {
                foreignKey.addColumn(
                    new OracleTableForeignKeyColumn(
                        foreignKey,
                        (OracleTableColumn) tableColumn.getOwnColumn(),
                        colIndex++));
            }
            return foreignKey;
        });
    }

}
