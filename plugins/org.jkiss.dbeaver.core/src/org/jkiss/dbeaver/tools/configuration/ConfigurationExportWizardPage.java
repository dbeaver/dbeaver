/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.TextWithOpenFolder;
import org.jkiss.utils.CommonUtils;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public class ConfigurationExportWizardPage extends WizardPage {

    private Text fileName;
    private TextWithOpenFolder folder;

    protected ConfigurationExportWizardPage() {
        super(CoreMessages.dialog_workspace_export_wizard_page_name);
        setTitle(CoreMessages.dialog_workspace_export_wizard_page_title);
        setMessage(CoreMessages.dialog_workspace_export_wizard_start_message_configure_settings);
    }

    @Override
    public void createControl(@NotNull Composite parent) {
        Composite placeholder = UIUtils.createPlaceholder(parent, 2);
        fileName = UIUtils.createLabelText(placeholder, CoreMessages.dialog_workspace_export_wizard_file_name, "");
        UIUtils.createControlLabel(placeholder, CoreMessages.dialog_workspace_export_wizard_file_path);
        folder = new TextWithOpenFolder(placeholder, CoreMessages.dialog_workspace_export_wizard_file_path_dialog);
        folder.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        fileName.addModifyListener(e -> updateState());
        folder.getTextControl().addModifyListener(e -> updateState());
        updateState();
        setControl(placeholder);
    }

    private void updateState()
    {
        getContainer().updateButtons();
    }

    @Override
    public boolean isPageComplete() {
        try {
            if (!CommonUtils.isEmpty(fileName.getText()) && !CommonUtils.isEmpty(fileName.getText())) {
                Path of = Path.of(folder.getText(), fileName.getText());
                return true;
            }
        } catch (InvalidPathException exception) {
            return false;
        }
        return false;
    }

    public ConfigurationExportData getExportData() {
        return new ConfigurationExportData(fileName.getText(),folder.getText());
    }
}
