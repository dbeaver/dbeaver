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
package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ui.UIUtils;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ViewExceptionDialog extends EditTextDialog {

    private Throwable error;

    public ViewExceptionDialog(Shell parentShell, Throwable error)
    {
        super(parentShell, "Error", error.getMessage());
        this.error = error;
        setReadonly(true);
    }

    @Override
    protected void createControlsBeforeText(Composite composite) {
        UIUtils.createControlLabel(composite, "Message");
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite composite = super.createDialogArea(parent);
        UIUtils.createControlLabel(composite, "Stacktrace");
        Text stText = new Text(composite, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.READ_ONLY);
        StringWriter sw = new StringWriter();
        PrintWriter buffer = new PrintWriter(sw, true);
        error.printStackTrace(buffer);
        stText.setText(sw.toString());
        return composite;
    }

}
