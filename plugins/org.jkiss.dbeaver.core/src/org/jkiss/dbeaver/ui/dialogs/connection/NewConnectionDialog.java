/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardDialog;

/**
 * NewConnectionDialog.
 *
 * It is a modeless dialog. But only one instance can be opened at once.
 */
public class NewConnectionDialog extends ActiveWizardDialog {

    private static final int TEST_BUTTON_ID = 2000;

    private static volatile NewConnectionDialog dialogInstance;

    private Button testButton;

    private NewConnectionDialog(IWorkbenchWindow window, ConnectionWizard wizard) {
        super(window, wizard);
        setAdaptContainerSizeToPages(true);
    }

    @Override
    protected boolean isModalWizard() {
        return false;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        parent.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        testButton = createButton(parent, TEST_BUTTON_ID, CoreMessages.dialog_connection_button_test, false);
        testButton.setEnabled(false);
        testButton.moveAbove(getButton(IDialogConstants.BACK_ID));

        Label spacer = new Label(parent, SWT.NONE);
        spacer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        ((GridLayout) parent.getLayout()).numColumns++;
        ((GridLayout) parent.getLayout()).makeColumnsEqualWidth = false;

        super.createButtonsForButtonBar(parent);
    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == TEST_BUTTON_ID) {
            testConnection();
            return;
        }
        super.buttonPressed(buttonId);
    }

    @Override
    public void updateButtons() {
        ConnectionWizard wizard = (ConnectionWizard) getWizard();
        ConnectionPageSettings settings = wizard.getPageSettings();
        testButton.setEnabled(settings != null && settings.isPageComplete());
        super.updateButtons();
    }

    private void testConnection() {
        ConnectionWizard wizard = (ConnectionWizard) getWizard();
        wizard.testConnection();
    }

    @Override
    public int open() {
        if (DBWorkbench.getPlatform().getWorkspace().getActiveProject() == null) {
            DBWorkbench.getPlatformUI().showError("No active project", "No active project, can't create new connection.\nActivate or create new project.");
            return IDialogConstants.CANCEL_ID;
        }
        try {
            return super.open();
        } catch (Exception e) {
            DBWorkbench.getPlatformUI().showError("Internal error", "Internal error when opening new connection wizard", e);
            return IDialogConstants.CANCEL_ID;
        }
    }

    public static boolean openNewConnectionDialog(IWorkbenchWindow window) {
        return openNewConnectionDialog(window, null);
    }

    public static boolean openNewConnectionDialog(IWorkbenchWindow window, DBPDriver initialDriver) {
        if (dialogInstance != null) {
            dialogInstance.getShell().forceActive();
            return true;
        } else {
            dialogInstance = new NewConnectionDialog(window, new NewConnectionWizard(initialDriver));
            try {
                return dialogInstance.open() == IDialogConstants.OK_ID;
            } finally {
                dialogInstance = null;
            }
        }
    }

}
