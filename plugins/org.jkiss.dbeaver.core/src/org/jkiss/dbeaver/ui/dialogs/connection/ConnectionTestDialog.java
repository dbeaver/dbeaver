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
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.PropertyPageStandard;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

/**
 * Connection test results dialog
 */
public class ConnectionTestDialog extends BaseDialog {
    private final DataSourceDescriptor descriptor;
    private final String serverVersion;
    private final String clientVersion;
    private final long elapsedTime;

    public ConnectionTestDialog(Shell parentShell, DataSourceDescriptor descriptor, String serverVersion, String clientVersion, long elapsedTime) {
        super(parentShell, CoreMessages.dialog_connection_test_title, DBIcon.TREE_DATABASE);
        this.descriptor = descriptor;
        this.serverVersion = serverVersion;
        this.clientVersion = clientVersion;
        this.elapsedTime = elapsedTime;
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite composite = super.createDialogArea(parent);
        ((GridLayout) composite.getLayout()).numColumns = 3;


        {
            Label imageLabel = new Label(composite, SWT.NULL);
            imageLabel.setImage(DBeaverIcons.getImage(DBIcon.STATUS_INFO));

            Label messageLabel = new Label(composite, SWT.NONE);
            messageLabel.setText(NLS.bind(ModelMessages.dialog_connection_wizard_start_connection_monitor_connected, elapsedTime));
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 2;
            messageLabel.setLayoutData(gd);
        }

        {
            UIUtils.createEmptyLabel(composite, 1, 1);
            UIUtils.createControlLabel(composite, CoreMessages.dialog_connection_test_label_server).setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.widthHint = 300;
            Text serverText = new Text(composite, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP);
            serverText.setText(serverVersion.trim());
            serverText.setLayoutData(gd);
        }

        {
            UIUtils.createEmptyLabel(composite, 1, 1);
            UIUtils.createControlLabel(composite, CoreMessages.dialog_connection_test_label_driver).setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
            Text driverText = new Text(composite, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP);
            driverText.setText(clientVersion.trim());
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.widthHint = 300;
            driverText.setLayoutData(gd);
        }

        UIUtils.asyncExec(() -> getButton(IDialogConstants.OK_ID).setFocus());
        return composite;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.DETAILS_ID, IDialogConstants.SHOW_DETAILS_LABEL, false);
    }

    @Override
    protected void buttonPressed(int buttonId) {
        if (buttonId == IDialogConstants.DETAILS_ID) {
            close();
            new PropertiesDialog(getShell()).open();
            return;
        }
        super.buttonPressed(buttonId);
    }

    private class PropertiesDialog extends BaseDialog {
        public PropertiesDialog(Shell shell) {
            super(shell, NLS.bind(CoreMessages.dialog_connection_test_properties_title, descriptor.getName()), null);
        }

        @Override
        protected Composite createDialogArea(Composite parent) {
            final Composite composite = super.createDialogArea(parent);

            final GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
            gd.widthHint = 500;
            gd.heightHint = 500;

            final Composite propertyComposite = new Composite(composite, SWT.BORDER);
            propertyComposite.setLayout(GridLayoutFactory.fillDefaults().create());
            propertyComposite.setLayoutData(gd);

            final PropertyPageStandard propertyPage = new PropertyPageStandard();
            propertyPage.createControl(propertyComposite);
            propertyPage.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            propertyPage.selectionChanged(null, new StructuredSelection(descriptor));

            return composite;
        }

        @Override
        protected void createButtonsForButtonBar(Composite parent) {
            createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        }
    }
}
