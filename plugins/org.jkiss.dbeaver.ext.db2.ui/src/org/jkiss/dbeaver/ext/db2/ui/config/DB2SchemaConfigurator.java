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
package org.jkiss.dbeaver.ext.db2.ui.config;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * DB2SchemaConfigurator
 */
public class DB2SchemaConfigurator implements DBEObjectConfigurator<DB2Schema> {

    @Override
    public DB2Schema configureObject(DBRProgressMonitor monitor, Object container, DB2Schema newSchema) {
        return new UITask<DB2Schema>() {
            @Override
            protected DB2Schema runTask() {
            	NewSchemaDialog dialog = new NewSchemaDialog(UIUtils.getActiveWorkbenchShell());
                if (dialog.open() != IDialogConstants.OK_ID) {
                    return null;
                }
                String schemaName = dialog.getSchemaName();
                if (schemaName.length() == 0) {
                    return null;
                }
                newSchema.setName(schemaName);
                return newSchema;
            }
        }.execute();
    }
    
    // --------
    // Dialog
    // --------

    private static class NewSchemaDialog extends Dialog {

        private String schemaName;

        public String getSchemaName()
        {
            return schemaName;
        }

        // Dialog managment
        private Text nameText;

        public NewSchemaDialog(Shell parentShell)
        {
            super(parentShell);
        }

        @Override
        protected boolean isResizable()
        {
            return true;
        }

        @Override
        protected Control createDialogArea(Composite parent)
        {
            getShell().setText("New Schema Name");
            Control container = super.createDialogArea(parent);
            Composite composite = UIUtils.createPlaceholder((Composite) container, 2);
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));

            nameText = UIUtils.createLabelText(composite, "Schema Name", null);
            nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            return parent;
        }

        @Override
        protected void okPressed()
        {
            this.schemaName = nameText.getText().trim().toUpperCase();
            super.okPressed();
        }

    }
}

