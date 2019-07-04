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
package org.jkiss.dbeaver.ext.postgresql.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreMessages;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreAvailableExtension;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreExtension;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * PostgreCreateExtensionDialog
 */
public class PostgreCreateExtensionDialog extends BaseDialog
{
    private PostgreAvailableExtension extension;
    private final PostgreExtension newextension;
    private List<PostgreSchema> allSchemas;
    private String name;
    private PostgreSchema schema;
    private List<PostgreAvailableExtension> installed;

    public PostgreCreateExtensionDialog(Shell parentShell, PostgreExtension extension) {
        super(parentShell, PostgreMessages.dialog_create_extension_title, null);
        this.newextension = extension;
    }
    
    private void checkEnabled() {
            getButton(IDialogConstants.OK_ID).setEnabled(extension != null && schema != null); 
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        final Composite composite = super.createDialogArea(parent);

        final Composite group = new Composite(composite, SWT.NONE);
        group.setLayout(new GridLayout(2, false));
        group.setLayoutData(new GridData(GridData.FILL_BOTH));

        final Text databaseText = UIUtils.createLabelText(group, "Database", newextension.getDatabase().getName(), SWT.BORDER | SWT.READ_ONLY); //$NON-NLS-2$

        final Combo extensionCombo = UIUtils.createLabelCombo(group, PostgreMessages.dialog_create_extension_name, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
        final Combo schemaCombo = UIUtils.createLabelCombo(group, PostgreMessages.dialog_create_extension_schema, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);

        schemaCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                schema = allSchemas.get(schemaCombo.getSelectionIndex());
                checkEnabled();
            }
        });
        
        extensionCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                extension = installed.get(extensionCombo.getSelectionIndex());
                extensionCombo.setToolTipText(installed.get(extensionCombo.getSelectionIndex()).getDescription());
                checkEnabled();
            }
        });       

        new AbstractJob("Load schemas") {

            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                try {
                    allSchemas = new ArrayList<>(newextension.getDatabase().getSchemas(monitor));
                     UIUtils.syncExec(() -> {
                        for (PostgreSchema schema : allSchemas) {
                            schemaCombo.add(schema.getName());
                        }
                    });
                } catch (DBException e) {
                    return GeneralUtils.makeExceptionStatus(e);
                }
                return Status.OK_STATUS;
            }
        }.schedule();
        
        new AbstractJob("Load available extensions") {

            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                try {
                    installed = new ArrayList<>(newextension.getDatabase().getAvailableExtensions(monitor));
                    UIUtils.syncExec(() -> {
                        for (PostgreAvailableExtension e : installed) {
                            extensionCombo.add(e.getName());
                        }
                    });
                } catch (DBException e) {
                    return GeneralUtils.makeExceptionStatus(e);
                }
                return Status.OK_STATUS;
            }
        }.schedule();

        return composite;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        super.createButtonsForButtonBar(parent);
        getButton(IDialogConstants.OK_ID).setEnabled(false);
    }

    public PostgreAvailableExtension getExtension() {
        return extension;
    }

    public PostgreSchema getSchema() {
        return schema;
    }
    
    
}
