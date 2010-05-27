/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.sql;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

/**
 * SQLQueryErrorDialog
 */
public class SQLQueryErrorDialog extends ErrorDialog {

    private boolean script;

    public SQLQueryErrorDialog(
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

    protected void buttonPressed(int buttonId) {
        if (buttonId == IDialogConstants.DETAILS_ID) {
            super.buttonPressed(buttonId);
            return;
        }
        setReturnCode(buttonId);
        close();
    }
}
