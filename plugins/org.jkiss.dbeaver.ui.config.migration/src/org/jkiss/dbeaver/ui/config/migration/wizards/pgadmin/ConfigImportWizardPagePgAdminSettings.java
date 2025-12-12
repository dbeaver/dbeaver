/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.config.migration.wizards.pgadmin;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.config.migration.ImportConfigMessages;
import org.jkiss.dbeaver.ui.controls.TextWithOpenFile;

import java.io.File;

public class ConfigImportWizardPagePgAdminSettings extends WizardPage {

    private TextWithOpenFile filePathText;
    private File inputFile;

    protected ConfigImportWizardPagePgAdminSettings() {
        super(ImportConfigMessages.config_import_wizard_custom_driver_settings);
        setTitle(ImportConfigMessages.config_import_wizard_custom_driver_import_settings_name);
        setDescription(ImportConfigMessages.config_import_wizard_pgadmin_description);
    }

    @Override
    public void createControl(@NotNull Composite parent) {
        Composite placeholder = UIUtils.createPlaceholder(parent, 1);

        UIUtils.createControlLabel(placeholder, ImportConfigMessages.config_import_wizard_custom_input_file);
        filePathText = new TextWithOpenFile(
            placeholder,
            ImportConfigMessages.config_import_wizard_custom_input_file_configuration,
            new String[]{"*.json"}
        );
        filePathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        setInputFileAndUpdateButtons();
        filePathText.getTextControl().addModifyListener(e -> setInputFileAndUpdateButtons());

        setControl(placeholder);
    }

    @Override
    public boolean isPageComplete() {
        return inputFile != null && inputFile.exists();
    }

    @NotNull
    public File getInputFile() {
        return inputFile;
    }

    private void setInputFileAndUpdateButtons() {
        if (filePathText.getText().isBlank()) {
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
