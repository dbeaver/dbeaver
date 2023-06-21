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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ConfigurationImportWizard extends Wizard implements IImportWizard {
    private static final Log log = Log.getLog(ConfigurationExportWizard.class);

    ConfigurationImportWizardPage mainPage;

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        setWindowTitle(CoreMessages.dialog_workspace_import_wizard_window_title);
        setNeedsProgressMonitor(true);
    }

    @Override
    public void addPages() {
        super.addPages();
        mainPage = new ConfigurationImportWizardPage();
        addPage(mainPage);
    }

    @Override
    public boolean performFinish() {
        Path workbench = DBWorkbench.getPlatform().getWorkspace().getMetadataFolder().resolve(".plugins\\org.eclipse.core.runtime\\.settings");
        if (!workbench.toFile().exists() || !workbench.toFile().isDirectory() || !workbench.toFile().canRead()) {
            log.error("Error reading configuration");
            return false;
        }
        ConfigurationImportData configurationImportData = mainPage.getConfigurationImportData();
        new Job("Importing workspace configuration") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                String zipFilepath = configurationImportData.getFilePath();
                File zipFile = Path.of(zipFilepath).toFile();
                if (!zipFile.exists() || !zipFile.canRead()) {
                    return Status.error("Can't read configuration file");
                }
                try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFile))) {
                    ZipEntry nextEntry = zipInputStream.getNextEntry();
                    while (nextEntry != null) {
                        String name = nextEntry.getName();
                        Path configFilePath = workbench.resolve(name);
                        if (!configFilePath.toFile().getParentFile().canWrite()) {
                            throw new IOException("Workspace directory is read-only");
                        }
                        if (configFilePath.toFile().exists()) {
                            File listFile = configFilePath.toFile();
                            if (listFile.getName().equals(name)) {
                                writeZipEntryToFile(zipInputStream, listFile);
                            }
                        } else {
                            Files.createFile(configFilePath);
                            writeZipEntryToFile(zipInputStream, configFilePath.toFile());
                        }
                        nextEntry = zipInputStream.getNextEntry();
                    }
                } catch (FileNotFoundException exception) {
                    return Status.error("File not found", exception);
                } catch (IOException exception) {
                    return Status.error("Error reading file", exception);
                }
                if (UIUtils.confirmAction(getShell(),
                    NLS.bind(CoreMessages.dialog_workspace_import_wizard_window_restart_dialog_title, GeneralUtils.getProductName()),
                    NLS.bind(CoreMessages.dialog_workspace_import_wizard_window_restart_dialog_message, GeneralUtils.getProductName())
                )) {
                    UIUtils.asyncExec(() -> PlatformUI.getWorkbench().restart());
                }

                return Status.OK_STATUS;
            }
        }.schedule();
        return true;
    }

    private void writeZipEntryToFile(@NotNull ZipInputStream zipInputStream, @NotNull File listFile) throws IOException {
        byte[] buffer = new byte[IOUtils.DEFAULT_BUFFER_SIZE];
        try (var os = Files.newOutputStream(listFile.toPath(), StandardOpenOption.TRUNCATE_EXISTING)) {
            for (int br = zipInputStream.read(buffer); br != -1; br = zipInputStream.read(buffer)) {
                os.write(buffer, 0, br);
            }
        }
        zipInputStream.closeEntry();
    }
}
