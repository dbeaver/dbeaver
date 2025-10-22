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

package org.jkiss.dbeaver.ui.config.migration.wizards.datagrip;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.config.migration.ImportConfigMessages;
import org.jkiss.dbeaver.ui.config.migration.datagrip.api.DataGripDataSourceConfigXmlService;
import org.jkiss.dbeaver.ui.config.migration.datagrip.impl.DataGripDataSourceConfigXmlServiceImpl;
import org.jkiss.dbeaver.ui.controls.TextWithOpenFile;
import org.jkiss.dbeaver.ui.controls.TextWithOpenFolder;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigImportWizardPageDataGripSettings extends WizardPage {

    private TextWithOpenFile filePathText;
    private Path inputFile;
    DataGripDataSourceConfigXmlService dataGripDataSourceConfigXmlService = DataGripDataSourceConfigXmlServiceImpl.INSTANCE;


    public ConfigImportWizardPageDataGripSettings() {
        super(ImportConfigMessages.config_import_wizard_custom_driver_settings);
        setTitle(ImportConfigMessages.config_import_wizard_custom_driver_import_settings_name);
        setDescription(ImportConfigMessages.config_import_wizard_jetbrains_description);
    }

    @Override
    public void createControl(Composite parent) {
        Composite placeholder = new Composite(parent, SWT.NONE);
        placeholder.setLayout(new GridLayout(1, true));

        UIUtils.createControlLabel(placeholder, ImportConfigMessages.config_import_wizard_custom_input_file);
        filePathText = new TextWithOpenFolder(placeholder, ImportConfigMessages.config_import_wizard_custom_input_file_configuration);
        filePathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        String str = dataGripDataSourceConfigXmlService.tryExtractRecentProjectPath();
        filePathText.setText(replaceUserHomePath(str));
        setInputFileAndUpdateButtons();
        filePathText.getTextControl().addModifyListener(e -> setInputFileAndUpdateButtons());
        setControl(placeholder);
    }

    private void setInputFileAndUpdateButtons() {
        inputFile = Path.of(filePathText.getText());
        if (!Files.exists(inputFile)) {
            setErrorMessage(NLS.bind(
                ImportConfigMessages.config_import_wizard_file_doesnt_exist_error,
                inputFile.toAbsolutePath().toString()
            ));
        } else {
            setErrorMessage(null);
        }
        getWizard().getContainer().updateButtons();
    }

    @Override
    public boolean isPageComplete() {
        return inputFile != null && Files.exists(inputFile);
    }


    public Path getInputFile() {
        return inputFile;
    }

    private String replaceUserHomePath(String path) {
        String[] split = path.split("\\$/");
        if (split.length < 2) {
            return path;
        }
        String pathFromUserHome = split[1];
        String osDependencePath = CommonUtils.makeOsDependencePath(pathFromUserHome);
        return System.getProperty("user.home") + File.separator + osDependencePath;
    }
}
