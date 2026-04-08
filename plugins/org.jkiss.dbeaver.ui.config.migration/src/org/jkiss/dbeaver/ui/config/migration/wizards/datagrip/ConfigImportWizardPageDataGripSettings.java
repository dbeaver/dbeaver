/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.config.migration.ImportConfigMessages;
import org.jkiss.dbeaver.ui.config.migration.datagrip.api.DataGripDataSourceConfigXmlService;
import org.jkiss.dbeaver.ui.config.migration.datagrip.impl.DataGripDataSourceConfigXmlServiceImpl;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;

public class ConfigImportWizardPageDataGripSettings extends WizardPage {

    private Button projectFolderButton;
    private Button pasteConfigurationButton;
    private Composite projectFolderComposite;
    private Composite pasteConfigurationComposite;
    private GridData projectFolderCompositeLayoutData;
    private GridData pasteConfigurationCompositeLayoutData;
    private Combo filePathText;
    private Text pastedConfigurationText;
    private Path inputFile;
    private String pastedConfiguration;
    private ConfigImportWizardDataGrip.InputMode inputMode = ConfigImportWizardDataGrip.InputMode.PROJECT_FOLDER;
    DataGripDataSourceConfigXmlService dataGripDataSourceConfigXmlService = DataGripDataSourceConfigXmlServiceImpl.INSTANCE;


    public ConfigImportWizardPageDataGripSettings() {
        super(ImportConfigMessages.config_import_wizard_custom_driver_settings);
        setTitle(ImportConfigMessages.config_import_wizard_custom_driver_import_settings_name);
        setDescription(ImportConfigMessages.config_import_wizard_jetbrains_description);
    }

    @Override
    public void createControl(Composite parent) {
        Composite placeholder = new Composite(parent, SWT.NONE);
        placeholder.setLayout(new GridLayout(1, false));

        Composite modeGroup = UIUtils.createTitledComposite(
            placeholder,
            ImportConfigMessages.config_import_wizard_jetbrains_input_mode,
            2,
            GridData.FILL_HORIZONTAL
        );
        projectFolderButton = new Button(modeGroup, SWT.RADIO);
        projectFolderButton.setText(ImportConfigMessages.config_import_wizard_jetbrains_input_mode_project);
        projectFolderButton.setSelection(true);
        pasteConfigurationButton = new Button(modeGroup, SWT.RADIO);
        pasteConfigurationButton.setText(ImportConfigMessages.config_import_wizard_jetbrains_input_mode_paste);

        projectFolderComposite = UIUtils.createComposite(placeholder, 3);
        projectFolderCompositeLayoutData = new GridData(GridData.FILL_HORIZONTAL);
        projectFolderComposite.setLayoutData(projectFolderCompositeLayoutData);
        filePathText = UIUtils.createLabelCombo(
            projectFolderComposite,
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
        filePathText.addModifyListener(e -> validateConfigurationInput());
        UIUtils.createPushButton(
            projectFolderComposite,
            ImportConfigMessages.config_import_wizard_jetbrains_input_mode_project,
            ImportConfigMessages.config_import_wizard_jetbrains_project_folder,
            UIIcon.OPEN,
            new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    String file = DialogUtils.openDirectoryDialog(getShell(), ImportConfigMessages.config_import_wizard_jetbrains_project_folder, null);
                    if (file != null) {
                        filePathText.setText(file);
                    }
                }
            }
        );

        pasteConfigurationComposite = new Composite(placeholder, SWT.NONE);
        pasteConfigurationComposite.setLayout(new GridLayout(1, false));
        pasteConfigurationCompositeLayoutData = new GridData(GridData.FILL_BOTH);
        pasteConfigurationCompositeLayoutData.heightHint = 180;
        pasteConfigurationComposite.setLayoutData(pasteConfigurationCompositeLayoutData);

        Composite textComposite = UIUtils.createTitledComposite(
            pasteConfigurationComposite,
            ImportConfigMessages.config_import_wizard_jetbrains_paste_configuration,
            1,
            GridData.FILL_BOTH
        );
        pastedConfigurationText = new Text(textComposite, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.H_SCROLL);
        GridData pastedConfigurationLayoutData = new GridData(GridData.FILL_BOTH);
        pastedConfigurationLayoutData.heightHint = 180;
        pastedConfigurationText.setLayoutData(pastedConfigurationLayoutData);
        pastedConfigurationText.addModifyListener(e -> validateConfigurationInput());

        UIUtils.createInfoLabel(pasteConfigurationComposite, ImportConfigMessages.config_import_wizard_jetbrains_paste_description);

        SelectionAdapter modeSelectionListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                inputMode = pasteConfigurationButton.getSelection() ?
                    ConfigImportWizardDataGrip.InputMode.PASTED_CONFIGURATION :
                    ConfigImportWizardDataGrip.InputMode.PROJECT_FOLDER;
                updateInputModeControls();
                validateConfigurationInput();
            }
        };
        projectFolderButton.addSelectionListener(modeSelectionListener);
        pasteConfigurationButton.addSelectionListener(modeSelectionListener);

        updateInputModeControls();
        validateConfigurationInput();
        setControl(placeholder);
    }

    private void updateInputModeControls() {
        boolean isProjectFolderMode = inputMode == ConfigImportWizardDataGrip.InputMode.PROJECT_FOLDER;
        projectFolderCompositeLayoutData.exclude = !isProjectFolderMode;
        projectFolderComposite.setVisible(isProjectFolderMode);
        pasteConfigurationCompositeLayoutData.exclude = isProjectFolderMode;
        pasteConfigurationComposite.setVisible(!isProjectFolderMode);
        Composite root = getControl() instanceof Composite composite ? composite : null;
        if (root != null) {
            root.layout(true, true);
        }
    }

    @Override
    public boolean isPageComplete() {
        return getErrorMessage() == null && (
            inputMode == ConfigImportWizardDataGrip.InputMode.PROJECT_FOLDER && inputFile != null && Files.exists(inputFile) ||
                inputMode == ConfigImportWizardDataGrip.InputMode.PASTED_CONFIGURATION && pastedConfiguration != null
        );
    }

    private void validateConfigurationInput() {
        if (inputMode == ConfigImportWizardDataGrip.InputMode.PASTED_CONFIGURATION) {
            validatePastedConfiguration();
        } else {
            validateProjectFolder();
        }
        getWizard().getContainer().updateButtons();
    }

    private void validateProjectFolder() {
        pastedConfiguration = normalizePastedConfiguration(pastedConfigurationText == null ? null : pastedConfigurationText.getText());
        String selectedPath = filePathText == null ? "" : filePathText.getText().trim();
        if (selectedPath.isEmpty()) {
            inputFile = null;
            setErrorMessage(null);
            return;
        }
        try {
            inputFile = Path.of(selectedPath);
        } catch (InvalidPathException e) {
            inputFile = null;
            setErrorMessage(e.getMessage());
            return;
        }
        if (!Files.exists(inputFile)) {
            setErrorMessage(NLS.bind(
                ImportConfigMessages.config_import_wizard_file_doesnt_exist_error,
                inputFile.toAbsolutePath().toString()
            ));
            return;
        }
        setErrorMessage(null);
    }

    private void validatePastedConfiguration() {
        inputFile = null;
        pastedConfiguration = normalizePastedConfiguration(pastedConfigurationText == null ? null : pastedConfigurationText.getText());
        if (pastedConfiguration == null) {
            setErrorMessage(ImportConfigMessages.config_import_wizard_jetbrains_paste_empty_error);
            return;
        }
        try {
            if (dataGripDataSourceConfigXmlService.buildIdeaConfigPropsFromText(pastedConfiguration).isEmpty()) {
                setErrorMessage(ImportConfigMessages.config_import_wizard_jetbrains_paste_no_connections_error);
                return;
            }
            setErrorMessage(null);
        } catch (Exception e) {
            setErrorMessage(e.getMessage());
        }
    }

    @Nullable
    public Path getInputFile() {
        return inputFile;
    }

    @NotNull
    public ConfigImportWizardDataGrip.InputMode getInputMode() {
        return inputMode;
    }

    @Nullable
    private String normalizePastedConfiguration(@Nullable String pastedConfigurationText) {
        return pastedConfigurationText == null || pastedConfigurationText.isBlank() ? null : pastedConfigurationText;
    }

    @Nullable
    public String getPastedConfiguration() {
        return pastedConfiguration;
    }

}
