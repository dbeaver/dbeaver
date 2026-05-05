/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.eula;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.util.prefs.Preferences;

public class EULAConfirmationDialog extends EULABaseDialog {

    public EULAConfirmationDialog(@NotNull Shell parentShell, @Nullable String eula) {
        super(parentShell, eula);
        super.setShellStyle(SWT.TITLE | SWT.APPLICATION_MODAL | SWT.RESIZE);
    }

    @Override
    protected void createButtonsForButtonBar(@NotNull Composite parent) {
        createButton(parent, IDialogConstants.YES_ID, CoreMessages.core_eula_dialog_accept, false);
        createButton(parent, IDialogConstants.NO_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected boolean canHandleShellCloseEvent() {
        //We don't want user to close this window
        return false;
    }


    @Override
    protected void buttonPressed(int buttonId) {
        switch (buttonId) {
            case IDialogConstants.NO_ID:
                System.exit(0);
                break;
            case IDialogConstants.YES_ID:
                Preferences preferences = Preferences.userNodeForPackage(DBWorkbench.getPlatform().getApplication().getClass());
                preferences.put(EULAUtils.DBEAVER_EULA, EULAUtils.getEulaVersion());
                close();
                break;
            default:
                break;
        }
        super.buttonPressed(buttonId);
    }

}
