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
package org.jkiss.dbeaver.ext.mysql.tools;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.tasks.ui.nativetool.AbstractNativeToolWizard;
import org.jkiss.dbeaver.tasks.ui.nativetool.AbstractNativeToolWizardPage;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseAuthDialog;


abstract class MySQLWizardPageSettings<WIZARD extends AbstractNativeToolWizard> extends AbstractNativeToolWizardPage<WIZARD>
{

    MySQLWizardPageSettings(WIZARD wizard, String title)
    {
        super(wizard, title);
    }

    public void createSecurityGroup(Composite parent)
    {
        final DBPConnectionConfiguration connectionInfo = wizard.getSettings().getDataSourceContainer().getActualConnectionConfiguration();
        if (connectionInfo != null) {
            Group securityGroup = UIUtils.createControlGroup(
                parent, "Security", 2, GridData.HORIZONTAL_ALIGN_BEGINNING, 0);
            Label infoLabel = new Label(securityGroup, SWT.NONE);
            infoLabel.setText("Override user credentials (" + connectionInfo.getUserName() +
                ") for objects '" + wizard.getObjectsName() + "'.\nExternal tools like 'mysqldump' may require different set of permissions.");
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 2;
            infoLabel.setLayoutData(gd);
            Button authButton = new Button(securityGroup, SWT.PUSH);
            authButton.setText("Authentication");
            authButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    BaseAuthDialog authDialog = new BaseAuthDialog(getShell(), "Authentication", false, true);
                    authDialog.setUserName(wizard.getSettings().getToolUserName());
                    authDialog.setUserPassword(wizard.getSettings().getToolUserPassword());
                    authDialog.setSavePassword(true);
                    if (authDialog.open() == IDialogConstants.OK_ID) {
                        wizard.getSettings().setToolUserName(authDialog.getUserName());
                        wizard.getSettings().setToolUserPassword(authDialog.getUserPassword());
                    }
                }
            });

            Button resetButton = new Button(securityGroup, SWT.PUSH);
            resetButton.setText("Reset to default");
            resetButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    wizard.getSettings().setToolUserName(null);
                    wizard.getSettings().setToolUserPassword(null);
                }
            });
        }
    }

}
