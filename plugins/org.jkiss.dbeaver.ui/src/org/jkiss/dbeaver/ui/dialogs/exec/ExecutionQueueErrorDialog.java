/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.dialogs.exec;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.ui.dialogs.StandardErrorDialog;

/**
 * ExecutionQueueErrorDialog
 */
class ExecutionQueueErrorDialog extends StandardErrorDialog {

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
