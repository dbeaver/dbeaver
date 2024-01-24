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
package org.jkiss.dbeaver.tools.configuration;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.TextWithOpen;
import org.jkiss.dbeaver.ui.controls.TextWithOpenFile;

import java.nio.file.Path;

public class ConfigurationImportWizardPage extends WizardPage {
    private TextWithOpen file;

    public ConfigurationImportWizardPage() {
        super(CoreMessages.dialog_workspace_import_wizard_name);
        setTitle(CoreMessages.dialog_workspace_import_wizard_title);
        setMessage(CoreMessages.dialog_workspace_import_wizard_start_message_configure_settings);
    }

    @Override
    public void createControl(Composite parent) {
        Composite composite = UIUtils.createComposite(parent, 1);
        Group importSettingsGroup = UIUtils.createControlGroup(
            composite,
            CoreMessages.dialog_workspace_import_wizard_group,
            2,
            GridData.FILL_BOTH,
            0
        );
        UIUtils.createControlLabel(importSettingsGroup, CoreMessages.dialog_workspace_import_wizard_file_select_name);
        file = new TextWithOpenFile(importSettingsGroup, CoreMessages.dialog_workspace_import_wizard_file_select_title,
            new String[]{"*.zip"});
        file.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        file.getTextControl().addModifyListener(e -> getContainer().updateButtons());
        setControl(composite);
    }

    @Override
    public boolean isPageComplete() {
        try {
            return !file.getText().isEmpty() && Path.of(file.getText()).toFile().exists() && file.getText().endsWith(
                ".zip");
        } catch (Exception exception) {
             return false;
        }
    }

    public ConfigurationImportData getConfigurationImportData() {
        return new ConfigurationImportData(file.getTextControl().getText());
    }
}
