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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.config.migration.ImportConfigMessages;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * DataGrip import settings page.
 * 
 * This page allows the user to select the DataGrip exported ZIP file to import connections from.
 */
public class ConfigImportWizardPageDataGripSettings extends WizardPage {

    private static final Log log = Log.getLog(ConfigImportWizardPageDataGripSettings.class);

    private Text filePathText;
    private Button browseButton;
    private File inputFile;
    private File extractedTempDir; // For ZIP extraction

    protected ConfigImportWizardPageDataGripSettings() {
        super("DataGrip Settings");
        setTitle("DataGrip Settings");
        setDescription("Select DataGrip exported settings ZIP file");
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
            "4. Select the ZIP file below\n\n" +
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

        UIUtils.createControlLabel(fileGroup, "DataGrip exported ZIP file");
        
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
            "Supported file format:\n" +
            "• DataGrip exported ZIP file (settings-YYYYMMDD-HHMMSS.zip)"
        );
        GridData formatData = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
        formatData.widthHint = 500;
        formatLabel.setLayoutData(formatData);

        setControl(composite);
        updatePageComplete();
    }

    private void browseForFile() {
        FileDialog fileDialog = new FileDialog(getShell(), SWT.OPEN);
        fileDialog.setText("Select DataGrip Export ZIP File");
        fileDialog.setFilterNames(new String[]{"DataGrip Export ZIP (*.zip)"});
        fileDialog.setFilterExtensions(new String[]{"*.zip"});
        
        String selectedFile = fileDialog.open();
        if (selectedFile != null) {
            File file = new File(selectedFile);
            if (file.getName().toLowerCase().endsWith(".zip")) {
                // Handle ZIP file
                try {
                    handleZipFile(file);
                } catch (Exception e) {
                    log.error("Failed to extract ZIP file", e);
                    setErrorMessage("Failed to extract ZIP file: " + e.getMessage());
                }
            } else {
                setErrorMessage("Please select a ZIP file exported from DataGrip");
            }
        }
    }

    /**
     * Extracts a DataGrip ZIP export file to a temporary directory
     */
    private void handleZipFile(File zipFile) throws IOException {
        // Clean up any previous extraction
        cleanupTempDirectory();
        
        // Create temporary directory
        extractedTempDir = Files.createTempDirectory("dbeaver-datagrip-import").toFile();
        
        // Extract ZIP file
        try (ZipFile zip = new ZipFile(zipFile)) {
            java.util.Enumeration<? extends ZipEntry> entries = zip.entries();
            
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                File entryFile = new File(extractedTempDir, entry.getName());
                
                if (entry.isDirectory()) {
                    entryFile.mkdirs();
                } else {
                    // Ensure parent directories exist
                    entryFile.getParentFile().mkdirs();
                    
                    // Extract file
                    try (InputStream in = zip.getInputStream(entry);
                         FileOutputStream out = new FileOutputStream(entryFile)) {
                        IOUtils.copyStream(in, out);
                    }
                }
            }
        }
        
        // Find the dataSources.xml file in the extracted content
        File dataSourcesFile = findDataSourcesFile(extractedTempDir);
        if (dataSourcesFile != null) {
            setInputFile(dataSourcesFile);
        } else {
            throw new IOException("No dataSources.xml file found in the ZIP archive");
        }
    }

    /**
     * Recursively searches for dataSources.xml file in the extracted directory
     */
    private File findDataSourcesFile(File dir) {
        // Check common paths first
        File dataSourcesFile = new File(dir, "settings/options/dataSources.xml");
        if (dataSourcesFile.exists()) {
            return dataSourcesFile;
        }
        
        dataSourcesFile = new File(dir, "options/dataSources.xml");
        if (dataSourcesFile.exists()) {
            return dataSourcesFile;
        }
        
        dataSourcesFile = new File(dir, "dataSources.xml");
        if (dataSourcesFile.exists()) {
            return dataSourcesFile;
        }
        
        // Recursive search
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    File found = findDataSourcesFile(file);
                    if (found != null) {
                        return found;
                    }
                } else if ("dataSources.xml".equals(file.getName())) {
                    return file;
                }
            }
        }
        
        return null;
    }

    /**
     * Cleans up temporary extraction directory
     */
    private void cleanupTempDirectory() {
        if (extractedTempDir != null && extractedTempDir.exists()) {
            try {
                deleteDirectory(extractedTempDir);
            } catch (Exception e) {
                log.warn("Failed to cleanup temp directory: " + extractedTempDir.getAbsolutePath(), e);
            }
            extractedTempDir = null;
        }
    }

    /**
     * Recursively deletes a directory and all its contents
     */
    private void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
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
            errorMessage = "Please select a DataGrip exported ZIP file";
        } else if (!inputFile.exists()) {
            errorMessage = "Selected file does not exist";
        } else if (!inputFile.isFile()) {
            errorMessage = "Please select a file, not a directory";
        } else if (!inputFile.getName().toLowerCase().endsWith(".zip")) {
            errorMessage = "Please select a ZIP file exported from DataGrip";
        } else {
            isComplete = true;
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

    @Override
    public void dispose() {
        // Clean up temporary directory when wizard is closed
        cleanupTempDirectory();
        super.dispose();
    }
}
