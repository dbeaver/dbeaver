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

package org.jkiss.dbeaver.ext.postgresql.ui.config;

import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.ext.postgresql.PostgreMessages;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableColumn;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableForeignKey;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableForeignKeyColumn;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKey;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.object.struct.EditForeignKeyPage;

/**
 * Postgre index configurator
 */
public class PostgreForeignKeyConfigurator implements DBEObjectConfigurator<PostgreTableForeignKey> {


    @Override
    public PostgreTableForeignKey configureObject(DBRProgressMonitor monitor, Object table, PostgreTableForeignKey foreignKey) {
        return new UITask<PostgreTableForeignKey>() {
            @Override
            protected PostgreTableForeignKey runTask() {
                EditPGForeignKeyPage editPage = new EditPGForeignKeyPage(
                    PostgreMessages.postgre_foreign_key_manager_header_edit_foreign_key,
                    foreignKey);
                if (!editPage.edit()) {
                    return null;
                }

                foreignKey.setReferencedConstraint(editPage.getUniqueConstraint());
                foreignKey.setDeleteRule(editPage.getOnDeleteRule());
                foreignKey.setUpdateRule(editPage.getOnUpdateRule());
                int colIndex = 1;
                for (EditForeignKeyPage.FKColumnInfo tableColumn : editPage.getColumns()) {
                    foreignKey.addColumn(
                        new PostgreTableForeignKeyColumn(
                            foreignKey,
                            (PostgreTableColumn) tableColumn.getOwnColumn(),
                            colIndex++,
                            (PostgreTableColumn) tableColumn.getRefColumn()));
                }
                foreignKey.setDeferrable(editPage.isDeferrable);
                foreignKey.setDeferred(editPage.isDeferred);
                return foreignKey;
            }
        }.execute();
    }


    private static class EditPGForeignKeyPage extends EditForeignKeyPage {

        private boolean isDeferrable;
        private boolean isDeferred;

        public EditPGForeignKeyPage(String title, DBSTableForeignKey foreignKey) {
            super(title, foreignKey, new DBSForeignKeyModifyRule[] {
                DBSForeignKeyModifyRule.NO_ACTION,
                DBSForeignKeyModifyRule.CASCADE, DBSForeignKeyModifyRule.RESTRICT,
                DBSForeignKeyModifyRule.SET_NULL,
                DBSForeignKeyModifyRule.SET_DEFAULT });
        }

        @Override
        protected Composite createPageContents(Composite parent) {
            Composite panel = super.createPageContents(parent);

            final Composite defGroup = UIUtils.createComposite(panel, 2);
            {
                // Cascades
                defGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                final Button deferrableCheck = UIUtils.createCheckbox(defGroup, PostgreMessages.postgre_foreign_key_manager_checkbox_deferrable, false);
                deferrableCheck.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        isDeferrable = deferrableCheck.getSelection();
                    }
                });
                final Button deferredCheck = UIUtils.createCheckbox(defGroup, PostgreMessages.postgre_foreign_key_manager_checkbox_deferred, false);
                deferredCheck.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        isDeferred = deferredCheck.getSelection();
                    }
                });
            }
            addPhysicalKeyComponent(defGroup);

            return panel;
        }
    }

}
