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
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.MultiPageWizardDialog;
import org.jkiss.utils.CommonUtils;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * NewConnectionDialog.
 * <p>
 * It is a modeless dialog. But only one instance can be opened for a particular datasource.
 */
public class EditConnectionDialog extends MultiPageWizardDialog {

    private static final Map<DBPDataSourceContainer, EditConnectionDialog> openDialogs = Collections.synchronizedMap(new IdentityHashMap<>());

    private static final int TEST_BUTTON_ID = 2000;
    private static String lastActivePage;

    private Button testButton;

    private EditConnectionDialog(IWorkbenchWindow window, ConnectionWizard wizard) {
        super(window, wizard);
    }

    @Override
    public ConnectionWizard getWizard() {
        return (ConnectionWizard) super.getWizard();
    }

    @Override
    protected boolean isModalWizard() {
        return false;
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings() {
        return UIUtils.getDialogSettings("DBeaver.EditConnectionDialog");
    }

    @Override
    protected Control createContents(Composite parent) {
        Control contents = super.createContents(parent);

        if (!CommonUtils.isEmpty(lastActivePage)) {
            getWizard().openSettingsPage(lastActivePage);
        }

        return contents;
    }

    @Override
    protected boolean isAutoLayoutAvailable() {
        return true;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        parent.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        testButton = createButton(parent, TEST_BUTTON_ID, "   " + CoreMessages.dialog_connection_button_test + "   ", false);
        testButton.setEnabled(false);

        Label spacer = new Label(parent, SWT.NONE);
        spacer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        ((GridLayout) parent.getLayout()).numColumns++;
        ((GridLayout) parent.getLayout()).makeColumnsEqualWidth = false;

        super.createButtonsForButtonBar(parent);
        //testButton.moveAbove(getButton(IDialogConstants.CANCEL_ID));
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
    public boolean close() {
        if (getCurrentPage() != null) {
            lastActivePage = getCurrentPage().getName();
        }
        return super.close();
    }

    @Override
    public void updateButtons() {
        if (testButton != null) {
            ConnectionPageSettings settings = getWizard().getPageSettings();
            testButton.setEnabled(settings != null && settings.isPageComplete());
        }
        super.updateButtons();
    }

    private void testConnection() {
        getWizard().testConnection();
    }

    public static boolean openEditConnectionDialog(IWorkbenchWindow window, DBPDataSourceContainer dataSource) {
        EditConnectionDialog dialog = openDialogs.get(dataSource);
        if (dialog != null) {
            dialog.getShell().forceActive();
            return true;
        }

        EditConnectionWizard wizard = new EditConnectionWizard((DataSourceDescriptor) dataSource);
        dialog = new EditConnectionDialog(window, wizard);
        openDialogs.put(dataSource, dialog);
        try {
            return dialog.open() == IDialogConstants.OK_ID;
        } finally {
            openDialogs.remove(dataSource);
        }
    }

}
