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

package org.jkiss.dbeaver.ext.config.migration.wizards.sqlworkbench;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.ext.config.migration.ImportConfigMessages;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.TextWithOpenFile;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.io.File;

public class ConfigImportWizardPageSqlWorkbenchSettings extends WizardPage {

    private TextWithOpenFile filePathText;
    private File inputFile;

    protected ConfigImportWizardPageSqlWorkbenchSettings() {
        super(ImportConfigMessages.config_import_wizard_custom_driver_settings);
        setTitle(ImportConfigMessages.config_import_wizard_custom_driver_import_settings_name);
        setDescription(ImportConfigMessages.config_import_wizard_custom_driver_import_settings_file_description);
    }

    @Override
    public void createControl(Composite parent) {
        Composite placeholder = new Composite(parent, SWT.NONE);
        placeholder.setLayout(new GridLayout(1, true));

        UIUtils.createControlLabel(placeholder, ImportConfigMessages.config_import_wizard_custom_input_file);
        filePathText = new TextWithOpenFile(placeholder, ImportConfigMessages.config_import_wizard_custom_input_file_configuration,
            new String[]{"*.xml"});
        filePathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        filePathText.setText(getDefaultPathToFile());
        setInputFileAndUpdateButtons();
        filePathText.getTextControl().addModifyListener(e -> setInputFileAndUpdateButtons());

        setControl(placeholder);
    }

    @Override
    public boolean isPageComplete() {
        return inputFile != null && inputFile.exists();
    }

    public File getInputFile() {
        return inputFile;
    }

    private String getDefaultPathToFile() {
        if (RuntimeUtils.isWindows()) {
            return RuntimeUtils.getWorkingDirectory("MySQL\\Workbench\\connections.xml");
        }
        return "";
    }

    private void setInputFileAndUpdateButtons() {
        if (filePathText.getText().trim().isEmpty()) {
            return;
        }
        inputFile = new File(filePathText.getText());
        if (!inputFile.exists()) {
            setErrorMessage(NLS.bind(ImportConfigMessages.config_import_wizard_file_doesnt_exist_error, inputFile.getAbsolutePath()));
        } else {
            setErrorMessage(null);
            getWizard().getContainer().updateButtons();
        }
    }
}
