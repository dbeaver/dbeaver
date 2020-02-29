/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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

package org.jkiss.dbeaver.tasks.ui.nativetool;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.tasks.ui.wizard.TaskConfigurationWizard;
import org.jkiss.dbeaver.tasks.ui.wizard.TaskConfigurationWizardDialog;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.dialogs.connection.ClientHomesSelector;
import org.jkiss.dbeaver.ui.internal.UIMessages;

/**
 * Tool wizard dialog
 */
public class ToolWizardDialog extends TaskConfigurationWizardDialog {

    public static final int CLIENT_CONFIG_ID = 1000;

    public ToolWizardDialog(IWorkbenchWindow window, TaskConfigurationWizard wizard) {
        super(window, wizard);
        setShellStyle(SWT.CLOSE | SWT.MAX | SWT.MIN | SWT.TITLE | SWT.BORDER | SWT.RESIZE | getDefaultOrientation());
        setHelpAvailable(false);
        setFinishButtonLabel(UIMessages.button_start);
    }

    protected IDialogSettings getDialogBoundsSettings() {
        // Do not save sizes. It breaks wizard on any UI changes.
        return null;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        if (getWizard() instanceof AbstractToolWizard<?, ?, ?>) {
            boolean nativeClientRequired = ((AbstractToolWizard) getWizard()).isNativeClientHomeRequired();
            if (nativeClientRequired) {
                parent.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

                Button configButton = createButton(parent, CLIENT_CONFIG_ID, "Client ...", false);
                //configButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

                Label spacer = new Label(parent, SWT.NONE);
                spacer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

                ((GridLayout) parent.getLayout()).numColumns++;
                ((GridLayout) parent.getLayout()).makeColumnsEqualWidth = false;
            }
        }

        super.createButtonsForButtonBar(parent);
    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == CLIENT_CONFIG_ID) {
            openClientConfiguration();
        }
        super.buttonPressed(buttonId);
    }

    private void openClientConfiguration() {
        AbstractToolWizard toolWizard = (AbstractToolWizard) getWizard();
        DBPDataSourceContainer dataSource = toolWizard.getSettings().getDataSourceContainer();
        if (dataSource != null) {
            NativeClientConfigDialog dialog = new NativeClientConfigDialog(getShell(), dataSource);
            if (dialog.open() == IDialogConstants.OK_ID) {
                toolWizard.updateErrorMessage();
                updateButtons();
            }
        }
    }

    private static class NativeClientConfigDialog extends BaseDialog {

        private final DBPDataSourceContainer dataSource;
        private ClientHomesSelector homesSelector;

        public NativeClientConfigDialog(Shell parentShell, DBPDataSourceContainer dataSource) {
            super(parentShell, "Configure local client for " + dataSource.getName(), dataSource.getDriver().getIcon());
            this.dataSource = dataSource;
        }

        @Override
        protected Composite createDialogArea(Composite parent) {
            Composite dialogArea = super.createDialogArea(parent);

            homesSelector = new ClientHomesSelector(dialogArea, "Native client");
            homesSelector.populateHomes(dataSource.getDriver(), dataSource.getConnectionConfiguration().getClientHomeId(), true);
            homesSelector.getPanel().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            return dialogArea;
        }

        @Override
        protected void okPressed() {
            String selectedHome = homesSelector.getSelectedHome();
            dataSource.getConnectionConfiguration().setClientHomeId(selectedHome);
            dataSource.getRegistry().flushConfig();

            super.okPressed();
        }
    }

}
