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
package org.jkiss.dbeaver.ext.postgresql.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreMessages;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreAvailableExtension;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreExtension;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ListContentProvider;
import org.jkiss.dbeaver.ui.controls.ViewerColumnController;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * PostgreCreateExtensionDialog
 */
public class PostgreCreateExtensionDialog extends BaseDialog
{
    private static final String DIALOG_ID = "DBeaver.PostgreCreateExtensionDialog";//$NON-NLS-1$

    private PostgreAvailableExtension extension;
    private final PostgreExtension newextension;
    private List<PostgreSchema> allSchemas;
    private PostgreSchema schema;
    private TableViewer extTable;

    public PostgreCreateExtensionDialog(Shell parentShell, PostgreExtension extension) {
        super(parentShell, PostgreMessages.dialog_create_extension_title, null);
        this.newextension = extension;
    }

    protected IDialogSettings getDialogBoundsSettings()
    {
        return UIUtils.getDialogSettings(DIALOG_ID);
    }

    private void checkEnabled() {
            getButton(IDialogConstants.OK_ID).setEnabled(extension != null && schema != null); 
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        final Composite composite = super.createDialogArea(parent);

        final Composite group = new Composite(composite, SWT.NONE);
        group.setLayout(new GridLayout(2, false));
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 600;
        gd.heightHint = 200;
        gd.verticalIndent = 0;
        gd.horizontalIndent = 0;
        group.setLayoutData(gd);

        final Text databaseText = UIUtils.createLabelText(group,PostgreMessages.dialog_create_extension_database, newextension.getDatabase().getName(), SWT.BORDER | SWT.READ_ONLY); //$NON-NLS-2$
        final Combo schemaCombo = UIUtils.createLabelCombo(group, PostgreMessages.dialog_create_extension_schema, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY);
        final Label lblExtension = UIUtils.createLabel(group, PostgreMessages.dialog_create_extension_name);
        
        extTable = new TableViewer(group, SWT.BORDER | SWT.UNDERLINE_SINGLE | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);

        {
            final Table table = extTable.getTable();
            table.setLayoutData(new GridData(GridData.FILL_BOTH));
            table.setLinesVisible(true);
            table.setHeaderVisible(true);
            table.addControlListener(new ControlAdapter() {
                @Override
                public void controlResized(ControlEvent e)
                {
                    UIUtils.packColumns(table);
                    UIUtils.maxTableColumnsWidth(table);
                    table.removeControlListener(this);
                }
            });
            
         }
        
        ViewerColumnController columnController = new ViewerColumnController("AvailabelExtensionDialog", extTable);
        columnController.addColumn(PostgreMessages.dialog_create_extension_column_name, null, SWT.NONE, true, true, new CellLabelProvider() {
            @Override
            public void update(ViewerCell cell)
            {
                cell.setText(((PostgreAvailableExtension) cell.getElement()).getName());
            }});
        columnController.addColumn(PostgreMessages.dialog_create_extension_column_version, null, SWT.NONE, true, true, new CellLabelProvider() {
            @Override
            public void update(ViewerCell cell)
            {
                cell.setText(((PostgreAvailableExtension) cell.getElement()).getVersion());
            }});
        columnController.addColumn(PostgreMessages.dialog_create_extension_column_description, null, SWT.NONE, true, true, new CellLabelProvider() {
            @Override
            public void update(ViewerCell cell)
            {
                cell.setText(((PostgreAvailableExtension) cell.getElement()).getDescription());
            }});
        columnController.createColumns();
        
        extTable.addSelectionChangedListener(event -> {
            IStructuredSelection selection = (IStructuredSelection) event.getSelection();
            if (!selection.isEmpty()) {
                extension = (PostgreAvailableExtension) selection.getFirstElement();//installed.get(extensionCombo.getSelectionIndex());
                checkEnabled();
            }

        });
        extTable.setContentProvider(new ListContentProvider());
       
         schemaCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                schema = allSchemas.get(schemaCombo.getSelectionIndex());
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
                        schema = DBUtils.findObject(allSchemas, PostgreConstants.PUBLIC_SCHEMA_NAME);
                        if (schema != null) {
                            schemaCombo.setText(schema.getName());
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
                    final List<PostgreAvailableExtension> installed = new ArrayList<>(newextension.getDatabase().getAvailableExtensions(monitor));
                    UIUtils.syncExec(() -> {                        
                        extTable.setInput(installed);
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
