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

import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

/**
 * Wizard page dialog
 */
public class EditWizardPageDialog extends BaseDialog {

    private DataSourceDescriptor dataSource;
    private ConnectionWizard wizard;
    private ConnectionWizardPage page;

    protected EditWizardPageDialog(ConnectionWizard wizard, ConnectionWizardPage wizardPage, DataSourceDescriptor dataSource) {
        super(wizard.getShell(), wizardPage.getTitle(), null);
        this.dataSource = dataSource;
        this.wizard = wizard;
        this.page = wizardPage;
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite dialogArea = super.createDialogArea(parent);
        page.setWizard(wizard);
        page.createControl(dialogArea);
        page.activatePage();
        return dialogArea;
    }

    @Override
    protected void okPressed() {
        page.deactivatePage();
        page.saveSettings(dataSource);
        super.okPressed();
    }

    @Override
    protected void cancelPressed() {
        super.cancelPressed();
    }
}
