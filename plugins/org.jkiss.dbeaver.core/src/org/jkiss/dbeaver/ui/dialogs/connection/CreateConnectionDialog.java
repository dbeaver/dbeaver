/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardDialog;

/**
 * CreateConnectionDialog
 */
public class CreateConnectionDialog extends ActiveWizardDialog
{

    public static final int TEST_BUTTON_ID = 2000;
    private Button testButton;

    public CreateConnectionDialog(IWorkbenchWindow window, ConnectionWizard wizard)
    {
        super(window, wizard);
        setAdaptContainerSizeToPages(true);
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        super.createButtonsForButtonBar(parent);
        testButton = createButton(parent, TEST_BUTTON_ID, CoreMessages.dialog_connection_button_test, false);
        testButton.setEnabled(false);
        testButton.moveAbove(getButton(IDialogConstants.BACK_ID));
    }

    @Override
    protected void buttonPressed(int buttonId)
    {
        if (buttonId == TEST_BUTTON_ID) {
            testConnection();
            return;
        }
        super.buttonPressed(buttonId);
    }

    @Override
    public void updateButtons()
    {
        ConnectionWizard wizard = (ConnectionWizard) getWizard();
        ConnectionPageSettings settings = wizard.getPageSettings();
        testButton.setEnabled(settings != null && settings.isPageComplete());
        super.updateButtons();
    }

    private void testConnection()
    {
        ConnectionWizard wizard = (ConnectionWizard) getWizard();
        wizard.testConnection();
    }

    @Override
    public int open() {
        if (DBeaverCore.getInstance().getProjectRegistry().getActiveProject() == null) {
            DBUserInterface.getInstance().showError("No active project", "No active project, can't create new connection.\nActivate or create new project.");
            return IDialogConstants.CANCEL_ID;
        }
        return super.open();
    }
}
