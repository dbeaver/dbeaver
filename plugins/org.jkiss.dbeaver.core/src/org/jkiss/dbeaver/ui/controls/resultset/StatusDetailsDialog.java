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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.EditTextDialog;
import org.jkiss.dbeaver.ui.dialogs.HelpEnabledDialog;
import org.jkiss.utils.CommonUtils;

import java.util.Arrays;
import java.util.List;

class StatusDetailsDialog extends EditTextDialog {

    private static final String DIALOG_ID = "DBeaver.StatusDetailsDialog";//$NON-NLS-1$

    private final ResultSetViewer resultSetViewer;
    private final List<Throwable> warnings;

    public StatusDetailsDialog(ResultSetViewer resultSetViewer, String message, List<Throwable> warnings) {
        super(resultSetViewer.getControl().getShell(), "Status details", message);
        this.resultSetViewer = resultSetViewer;
        this.warnings = warnings;
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
            Table warnTable = new Table(composite, SWT.BORDER);
            warnTable.setLinesVisible(true);
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.minimumHeight = 100;
            warnTable.setLayoutData(gd);
            for (Throwable ex : warnings) {
                TableItem warnItem = new TableItem(warnTable, SWT.NONE);
                warnItem.setText(ex.getMessage());
                warnItem.setData(ex);
            }
        }
        return composite;
    }

}
