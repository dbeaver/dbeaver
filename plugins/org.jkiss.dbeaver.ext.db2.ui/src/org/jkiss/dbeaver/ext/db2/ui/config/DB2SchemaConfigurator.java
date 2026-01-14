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
package org.jkiss.dbeaver.ext.db2.ui.config;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.ext.db2.ui.internal.DB2Messages;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.Map;

/**
 * DB2SchemaConfigurator
 */
public class DB2SchemaConfigurator implements DBEObjectConfigurator<DB2Schema> {

    @Override
    public DB2Schema configureObject(@NotNull DBRProgressMonitor monitor, @Nullable DBECommandContext commandContext, @Nullable Object container, @NotNull DB2Schema newSchema, @NotNull Map<String, Object> options) {
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

        // Dialog management
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
            getShell().setText(DB2Messages.dialog_schema_edit_title);
            Control container = super.createDialogArea(parent);
            Composite composite = UIUtils.createPlaceholder((Composite) container, 2);
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));

            nameText = UIUtils.createLabelText(composite, DB2Messages.dialog_schema_edit_schema_name, null);
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

