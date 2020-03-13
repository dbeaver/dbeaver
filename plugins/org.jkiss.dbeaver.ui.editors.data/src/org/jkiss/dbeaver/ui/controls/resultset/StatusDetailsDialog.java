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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.dbeaver.ui.dialogs.EditTextDialog;
import org.jkiss.dbeaver.ui.dialogs.ViewExceptionDialog;
import org.jkiss.utils.CommonUtils;

import java.util.List;

class StatusDetailsDialog extends EditTextDialog {

    private static final String DIALOG_ID = "DBeaver.StatusDetailsDialog";//$NON-NLS-1$

    private final List<Throwable> warnings;
    private Table warnTable;

    public StatusDetailsDialog(Shell shell, String message, List<Throwable> warnings) {
        super(shell, ResultSetMessages.dialog_title_status_details, message);
        this.warnings = warnings;
        textHeight = 100;
        setReadonly(true);
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings()
    {
        return UIUtils.getDialogSettings(DIALOG_ID);
    }

    @Override
    protected void createControlsBeforeText(Composite composite) {
        UIUtils.createControlLabel(composite, ResultSetMessages.dialog_control_label_massage);
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite composite = super.createDialogArea(parent);
        if (!CommonUtils.isEmpty(warnings)) {
            // Create warnings table
            UIUtils.createControlLabel(composite, "Warnings");
            warnTable = new Table(composite, SWT.BORDER | SWT.FULL_SELECTION);
            TableColumn exColumn = UIUtils.createTableColumn(warnTable, SWT.NONE, "Exception");
            TableColumn msgColumn = UIUtils.createTableColumn(warnTable, SWT.NONE, "Message");
            warnTable.setLinesVisible(true);
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.minimumHeight = 100;
            warnTable.setLayoutData(gd);
            for (Throwable ex : warnings) {
                TableItem warnItem = new TableItem(warnTable, SWT.NONE);
                warnItem.setText(0, ex.getClass().getName());
                if (ex.getMessage() != null) {
                    warnItem.setText(1, ex.getMessage());
                }
                warnItem.setData(ex);
            }
            exColumn.pack();
            msgColumn.pack();
            warnTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseDoubleClick(MouseEvent e) {
                    openWarning();
                }
            });
            warnTable.addTraverseListener(e -> {
                if (e.detail == SWT.TRAVERSE_RETURN) {
                    openWarning();
                    e.doit = false;
                }
            });
        }
        return composite;
    }

    private void openWarning() {
        TableItem[] selection = warnTable.getSelection();
        if (selection.length == 0) {
            return;
        }
        Throwable error = (Throwable) selection[0].getData();
        ViewExceptionDialog veDialog = new ViewExceptionDialog(getShell(), error);
        veDialog.open();
    }

}
