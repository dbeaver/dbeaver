/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.EditTextDialog;
import org.jkiss.dbeaver.ui.dialogs.ViewExceptionDialog;
import org.jkiss.utils.CommonUtils;

import java.util.List;

class StatusDetailsDialog extends EditTextDialog {

    private static final String DIALOG_ID = "DBeaver.StatusDetailsDialog";//$NON-NLS-1$

    private final ResultSetViewer resultSetViewer;
    private final List<Throwable> warnings;
    private Table warnTable;

    public StatusDetailsDialog(ResultSetViewer resultSetViewer, String message, List<Throwable> warnings) {
        super(resultSetViewer.getControl().getShell(), "Status details", message);
        this.resultSetViewer = resultSetViewer;
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
        UIUtils.createControlLabel(composite, "Message");
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
                warnItem.setText(1, ex.getMessage());
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
            warnTable.addTraverseListener(new TraverseListener() {
                @Override
                public void keyTraversed(TraverseEvent e) {
                    if (e.detail == SWT.TRAVERSE_RETURN) {
                        openWarning();
                        e.doit = false;
                    }
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
