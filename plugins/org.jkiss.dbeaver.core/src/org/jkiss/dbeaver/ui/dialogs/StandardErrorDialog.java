/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

/**
 * StandardErrorDialog
 */
public class StandardErrorDialog extends ErrorDialog {

    public StandardErrorDialog(
        Shell parentShell,
        String dialogTitle,
        String message,
        IStatus status,
        int displayMask)
    {
        super(parentShell, dialogTitle, message, status, displayMask);
        setStatus(status);
    }

}