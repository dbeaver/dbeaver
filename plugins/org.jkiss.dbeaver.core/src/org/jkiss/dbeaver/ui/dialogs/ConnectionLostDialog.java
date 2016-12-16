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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.utils.GeneralUtils;

/**
 * ConnectionLostDialog
 */
public class ConnectionLostDialog extends ErrorDialog {

    private final String stopButtonName;

    public ConnectionLostDialog(Shell parentShell, DBPDataSourceContainer container, Throwable error, String stopButtonName) {
        super(
            parentShell,
            "Connection lost",
            "Connection to '" + container.getName() + "' was lost and cannot be re-established.\nWhat to you want to do?",
            GeneralUtils.makeExceptionStatus(error),
            IStatus.INFO | IStatus.WARNING | IStatus.ERROR);
        this.stopButtonName = stopButtonName;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.STOP_ID, stopButtonName, true);
        createButton(parent, IDialogConstants.RETRY_ID, IDialogConstants.RETRY_LABEL, false);
        createButton(parent, IDialogConstants.IGNORE_ID, IDialogConstants.IGNORE_LABEL, false);
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
