/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.exasol.ui.config;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTable;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTableForeignKey;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTableForeignKeyColumn;
import org.jkiss.dbeaver.ext.exasol.model.ExasolTableUniqueKey;
import org.jkiss.dbeaver.ext.exasol.ui.internal.ExasolMessages;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExasolForeignKeyConfigurator implements DBEObjectConfigurator<ExasolTableForeignKey> {
    protected static final Log log = Log.getLog(ExasolForeignKeyConfigurator.class);

    @Override
    public ExasolTableForeignKey configureObject(DBRProgressMonitor monitor, Object container, ExasolTableForeignKey foreignKey, Map<String, Object> options) {
        ExasolTable table = (ExasolTable) container;
        return new UITask<ExasolTableForeignKey>() {
            @Override
            protected ExasolTableForeignKey runTask() {
                ExasolCreateForeignKeyDialog editPage = new ExasolCreateForeignKeyDialog(ExasolMessages.dialog_create_foreign_key_title, foreignKey, options);

                if (!editPage.edit()) {
                    return null;
                }
                foreignKey.setName(editPage.getName());
                foreignKey.setReferencedConstraint((ExasolTableUniqueKey)editPage.getUniqueConstraint());
                foreignKey.setEnabled(editPage.isEnabled());

                List<ExasolTableForeignKeyColumn> columns = new ArrayList<>();
                int cnt = 0;
                for (ExasolCreateForeignKeyDialog.FKColumnInfo column : editPage.getColumns()) {
                    try {
                        ExasolTable refTable = foreignKey.getReferencedConstraint().getTable();
                        columns.add(new ExasolTableForeignKeyColumn(
                                foreignKey,
                                table.getAttribute(monitor, column.getOwnColumn().getName()),
                                refTable.getAttribute(monitor, column.getRefColumn().getName()),
                                ++cnt));
                    } catch (DBException e) {
                        log.error("Could not get Attribute Information from Table");
                        return null;
                    }
                }

                foreignKey.setColumns(columns);

                return foreignKey;
            }
        }.execute();
    }
}
