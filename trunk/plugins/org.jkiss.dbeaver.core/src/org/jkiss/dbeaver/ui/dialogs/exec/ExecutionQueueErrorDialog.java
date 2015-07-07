/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.dialogs.exec;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

/**
 * ExecutionQueueErrorDialog
 */
class ExecutionQueueErrorDialog extends ErrorDialog {

    private boolean script;

    public ExecutionQueueErrorDialog(
        Shell parentShell,
        String dialogTitle,
        String message,
        IStatus status,
        int displayMask,
        boolean script)
    {
        super(parentShell, dialogTitle, message, status, displayMask);
        this.script = script;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        // create OK and Details buttons
        createButton(
            parent,
            IDialogConstants.STOP_ID,
            IDialogConstants.STOP_LABEL,
            true);
        createButton(
            parent,
            IDialogConstants.RETRY_ID,
            IDialogConstants.RETRY_LABEL,
            false);
        if (script) {
            createButton(
                parent,
                IDialogConstants.SKIP_ID,
                IDialogConstants.SKIP_LABEL,
                false);
            createButton(
                parent,
                IDialogConstants.IGNORE_ID,
                IDialogConstants.IGNORE_LABEL,
                false);
        }
        createDetailsButton(parent);
    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == IDialogConstants.DETAILS_ID) {
            super.buttonPressed(buttonId);
            return;
        }
        setReturnCode(buttonId);
        close();
    }
}
