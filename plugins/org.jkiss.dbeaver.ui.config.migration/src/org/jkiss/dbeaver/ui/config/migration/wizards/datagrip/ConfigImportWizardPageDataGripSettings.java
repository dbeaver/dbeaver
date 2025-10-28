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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.config.migration.ImportConfigMessages;
import org.jkiss.dbeaver.ui.config.migration.datagrip.api.DataGripDataSourceConfigXmlService;
import org.jkiss.dbeaver.ui.config.migration.datagrip.impl.DataGripDataSourceConfigXmlServiceImpl;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ConfigImportWizardPageDataGripSettings extends WizardPage {

    private Combo filePathText;
    private Path inputFile;
    DataGripDataSourceConfigXmlService dataGripDataSourceConfigXmlService = DataGripDataSourceConfigXmlServiceImpl.INSTANCE;


    public ConfigImportWizardPageDataGripSettings() {
        super(ImportConfigMessages.config_import_wizard_custom_driver_settings);
        setTitle(ImportConfigMessages.config_import_wizard_custom_driver_import_settings_name);
        setDescription(ImportConfigMessages.config_import_wizard_jetbrains_description);
    }

    @Override
    public void createControl(Composite parent) {
        Composite placeholder = UIUtils.createComposite(parent, 3);

        filePathText = UIUtils.createLabelCombo(
            placeholder,
            ImportConfigMessages.config_import_wizard_custom_input_file_configuration,
            SWT.DROP_DOWN | SWT.BORDER
        );
        filePathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        List<Path> configPaths = dataGripDataSourceConfigXmlService.tryExtractRecentProjectPath();
        for (Path path : configPaths) {
            filePathText.add(path.toString());
        }
        if (!configPaths.isEmpty()) {
            filePathText.select(0);
        }
        setInputFileAndUpdateButtons();
        filePathText.addModifyListener(e -> setInputFileAndUpdateButtons());
        UIUtils.createPushButton(
            placeholder,
            "Project folder",
            ImportConfigMessages.config_import_wizard_custom_input_file_configuration,
            UIIcon.OPEN,
            new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    String file = DialogUtils.openDirectoryDialog(getShell(), "JetBrains project directory", null);
                    if (file != null) {
                        filePathText.setText(file);
                    }
                }
            }
        );
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

}
