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
package org.jkiss.dbeaver.ext.db2.ui.config;

import org.jkiss.dbeaver.ext.db2.model.DB2TableColumn;
import org.jkiss.dbeaver.ext.db2.model.DB2TableForeignKey;
import org.jkiss.dbeaver.ext.db2.model.DB2TableKeyColumn;
import org.jkiss.dbeaver.ext.db2.model.DB2TableUniqueKey;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2DeleteUpdateRule;
import org.jkiss.dbeaver.ext.db2.ui.internal.DB2Messages;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EditForeignKeyPage;

import java.util.ArrayList;
import java.util.List;

/**
 * DB2 foreign key configurator
 */
public class DB2ForeignKeyConfigurator implements DBEObjectConfigurator<DB2TableForeignKey> {
	private static final DBSForeignKeyModifyRule[] FK_RULES;
	
	static {
        List<DBSForeignKeyModifyRule> rules = new ArrayList<>(DB2DeleteUpdateRule.values().length);
        for (DB2DeleteUpdateRule db2DeleteUpdateRule : DB2DeleteUpdateRule.values()) {
            rules.add(db2DeleteUpdateRule.getRule());
        }
        FK_RULES = rules.toArray(new DBSForeignKeyModifyRule[] {});
    }

    @Override
    public DB2TableForeignKey configureObject(DBRProgressMonitor monitor, Object container, DB2TableForeignKey foreignKey) {
        return new UITask<DB2TableForeignKey>() {
            @Override
            protected DB2TableForeignKey runTask() {
            	EditForeignKeyPage editDialog = new EditForeignKeyPage(
                        DB2Messages.edit_db2_foreign_key_manager_dialog_title, foreignKey, FK_RULES);
                    if (!editDialog.edit()) {
                        return null;
                    }

                    DBSForeignKeyModifyRule deleteRule = editDialog.getOnDeleteRule();
                    DBSForeignKeyModifyRule updateRule = editDialog.getOnUpdateRule();
                    DB2TableUniqueKey ukConstraint = (DB2TableUniqueKey) editDialog.getUniqueConstraint();

                    foreignKey.setReferencedConstraint(ukConstraint);
                    foreignKey.setDb2DeleteRule(DB2DeleteUpdateRule.getDB2RuleFromDBSRule(deleteRule));
                    foreignKey.setDb2UpdateRule(DB2DeleteUpdateRule.getDB2RuleFromDBSRule(updateRule));

                    List<DB2TableKeyColumn> columns = new ArrayList<>(editDialog.getColumns().size());
                    DB2TableKeyColumn column;
                    int colIndex = 1;
                    for (EditForeignKeyPage.FKColumnInfo tableColumn : editDialog.getColumns()) {
                        column = new DB2TableKeyColumn(foreignKey, (DB2TableColumn) tableColumn.getOwnColumn(), colIndex++);
                        columns.add(column);
                    }

                    foreignKey.setColumns(columns);

                    return foreignKey;
            }
        }.execute();
    }

}
