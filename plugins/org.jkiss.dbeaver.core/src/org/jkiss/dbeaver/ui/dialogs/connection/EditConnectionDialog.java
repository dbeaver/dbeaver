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

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.MultiPageWizardDialog;

/**
 * CreateConnectionDialog
 */
public class EditConnectionDialog extends MultiPageWizardDialog
{

    public static final int TEST_BUTTON_ID = 2000;
    private Button testButton;

    public EditConnectionDialog(IWorkbenchWindow window, ConnectionWizard wizard)
    {
        super(window, wizard);
    }

    @Override
    public ConnectionWizard getWizard()
    {
        return (ConnectionWizard)super.getWizard();
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings() {
        return UIUtils.getDialogSettings("DBeaver.EditConnectionDialog");
    }

    @Override
    protected Control createContents(Composite parent)
    {
        Control contents = super.createContents(parent);
        DataSourceDescriptor activeDataSource = getWizard().getActiveDataSource();
        getShell().setText(NLS.bind( CoreMessages.dialog_connection_edit_title, activeDataSource.getName()));
        getShell().setImage(DBeaverIcons.getImage(activeDataSource.getObjectImage()));
        return contents;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        super.createButtonsForButtonBar(parent);
        testButton = createButton(parent, TEST_BUTTON_ID, CoreMessages.dialog_connection_button_test, false);
        testButton.setEnabled(false);
        //testButton.moveAbove(getButton(IDialogConstants.CANCEL_ID));
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
        if (testButton != null) {
            ConnectionPageSettings settings = getWizard().getPageSettings();
            testButton.setEnabled(settings != null && settings.isPageComplete());
        }
        super.updateButtons();
    }

    private void testConnection()
    {
        getWizard().testConnection();
    }

}
