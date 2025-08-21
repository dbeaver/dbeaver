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
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.config.migration.ImportConfigMessages;
import org.jkiss.utils.CommonUtils;

import java.io.File;

/**
 * DataGrip import settings page.
 * 
 * This page allows the user to select the DataGrip settings folder or dataSources.xml file
 * to import connections from.
 */
public class ConfigImportWizardPageDataGripSettings extends WizardPage {

    private Text filePathText;
    private Button browseButton;
    private File inputFile;

    protected ConfigImportWizardPageDataGripSettings() {
        super("DataGrip Settings");
        setTitle("DataGrip Settings");
        setDescription("Select DataGrip exported settings folder or dataSources.xml file");
    }

    @Override
    public void createControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));

        // Instructions
        Label instructionsLabel = new Label(composite, SWT.WRAP);
        instructionsLabel.setText(
            "To export settings from DataGrip:\n" +
            "1. In DataGrip: File → Export Settings...\n" +
            "2. Select 'Database' or 'All Settings'\n" +
            "3. Export to a ZIP file\n" +
            "4. Extract the ZIP file\n" +
            "5. Select the extracted folder below, or directly select the dataSources.xml file\n\n" +
            "Note: Passwords are not included in DataGrip exports for security reasons."
        );
        GridData instructionsData = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
        instructionsData.widthHint = 500;
        instructionsLabel.setLayoutData(instructionsData);

        // Separator
        Label separator = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
        separator.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // File selection
        Group fileGroup = UIUtils.createControlGroup(composite, "DataGrip Settings Location", 2, GridData.FILL_HORIZONTAL, 0);

        UIUtils.createControlLabel(fileGroup, "Settings folder or dataSources.xml file:");
        
        Composite fileSelectionComposite = new Composite(fileGroup, SWT.NONE);
        GridLayout fileSelectionLayout = new GridLayout(2, false);
        fileSelectionLayout.marginWidth = 0;
        fileSelectionLayout.marginHeight = 0;
        fileSelectionComposite.setLayout(fileSelectionLayout);
        fileSelectionComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        filePathText = new Text(fileSelectionComposite, SWT.BORDER | SWT.READ_ONLY);
        filePathText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        filePathText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                updatePageComplete();
            }
        });

        browseButton = new Button(fileSelectionComposite, SWT.PUSH);
        browseButton.setText("Browse...");
        browseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                browseForFile();
            }
        });

        // File format info
        Label formatLabel = new Label(composite, SWT.WRAP);
        formatLabel.setText(
            "Expected file structure:\n" +
            "• Extracted settings folder containing: settings/options/dataSources.xml\n" +
            "• Or direct path to dataSources.xml file"
        );
        GridData formatData = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
        formatData.widthHint = 500;
        formatLabel.setLayoutData(formatData);

        setControl(composite);
        updatePageComplete();
    }

    private void browseForFile() {
        // First try to browse for a folder
        DirectoryDialog directoryDialog = new DirectoryDialog(getShell());
        directoryDialog.setText("Select DataGrip Settings Folder");
        directoryDialog.setMessage("Select the extracted DataGrip settings folder");
        
        String selectedDir = directoryDialog.open();
        if (selectedDir != null) {
            File selectedFile = new File(selectedDir);
            
            // Check if this folder contains the expected structure
            File dataSourcesFile = new File(selectedFile, "settings/options/dataSources.xml");
            if (dataSourcesFile.exists()) {
                setInputFile(selectedFile);
                return;
            }
            
            // Check if the folder itself is the settings folder
            dataSourcesFile = new File(selectedFile, "options/dataSources.xml");
            if (dataSourcesFile.exists()) {
                setInputFile(selectedFile);
                return;
            }
            
            // Check if this is the options folder
            dataSourcesFile = new File(selectedFile, "dataSources.xml");
            if (dataSourcesFile.exists()) {
                setInputFile(dataSourcesFile);
                return;
            }
        }
        
        // If folder browsing didn't work, try file browsing
        FileDialog fileDialog = new FileDialog(getShell(), SWT.OPEN);
        fileDialog.setText("Select DataGrip dataSources.xml File");
        fileDialog.setFilterNames(new String[]{"DataGrip Data Sources (*.xml)", "All Files (*.*)"});
        fileDialog.setFilterExtensions(new String[]{"*.xml", "*.*"});
        
        String selectedFile = fileDialog.open();
        if (selectedFile != null) {
            setInputFile(new File(selectedFile));
        }
    }

    private void setInputFile(File file) {
        this.inputFile = file;
        this.filePathText.setText(file.getAbsolutePath());
        updatePageComplete();
    }

    private void updatePageComplete() {
        boolean isComplete = false;
        String errorMessage = null;

        if (inputFile == null || CommonUtils.isEmpty(filePathText.getText())) {
            errorMessage = "Please select DataGrip settings folder or dataSources.xml file";
        } else if (!inputFile.exists()) {
            errorMessage = "Selected file or folder does not exist";
        } else if (inputFile.isFile()) {
            // Check if it's a dataSources.xml file
            if (!"dataSources.xml".equals(inputFile.getName())) {
                errorMessage = "Selected file must be named 'dataSources.xml'";
            } else {
                isComplete = true;
            }
        } else if (inputFile.isDirectory()) {
            // Check if the directory contains the expected structure
            File dataSourcesFile = new File(inputFile, "settings/options/dataSources.xml");
            if (!dataSourcesFile.exists()) {
                dataSourcesFile = new File(inputFile, "options/dataSources.xml");
                if (!dataSourcesFile.exists()) {
                    dataSourcesFile = new File(inputFile, "dataSources.xml");
                    if (!dataSourcesFile.exists()) {
                        errorMessage = "Selected folder does not contain dataSources.xml file in expected location";
                    } else {
                        isComplete = true;
                    }
                } else {
                    isComplete = true;
                }
            } else {
                isComplete = true;
            }
        }

        setErrorMessage(errorMessage);
        setPageComplete(isComplete);
        
        // Update wizard buttons
        if (getWizard() != null && getWizard().getContainer() != null) {
            getWizard().getContainer().updateButtons();
        }
    }

    public File getInputFile() {
        return inputFile;
    }

    @Override
    public boolean isPageComplete() {
        return inputFile != null && inputFile.exists();
    }
}
