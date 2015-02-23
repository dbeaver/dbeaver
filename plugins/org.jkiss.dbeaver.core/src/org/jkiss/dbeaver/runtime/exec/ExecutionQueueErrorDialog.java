/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.runtime.exec;

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
