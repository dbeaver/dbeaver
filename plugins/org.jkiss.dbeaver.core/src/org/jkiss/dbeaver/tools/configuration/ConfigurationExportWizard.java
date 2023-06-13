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
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ConfigurationExportWizard extends Wizard implements IExportWizard {
    private static final Log log = Log.getLog(ConfigurationExportWizard.class);
    private ConfigurationExportWizardPage pageMain;


    @Override
    public void addPages() {
        super.addPages();
        pageMain = new ConfigurationExportWizardPage();
        addPage(pageMain);
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        setWindowTitle(CoreMessages.dialog_workspace_export_wizard_window_title);
        setNeedsProgressMonitor(true);
    }

    @Override
    public boolean performFinish() {
        Path workbench = DBWorkbench.getPlatform().getWorkspace().getMetadataFolder().resolve(".plugins\\org.eclipse.core.runtime\\.settings");
        if (!workbench.toFile().exists() || !workbench.toFile().isDirectory() || !workbench.toFile().canRead()) {
            log.error("Error reading workspace configuration");
            return false;
        }
        ConfigurationExportData exportData = pageMain.getExportData();
        String path = exportData.getFile();
        if (!exportData.getFile().endsWith(".zip")) {
            path += ".zip";
        }
        Path zipFile = Path.of(path);
        new Job("Copying workspace configuration") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                Path parent = zipFile.getParent();
                if (parent != null && !parent.toFile().canWrite()) {
                    return Status.error("Can't create a file, because the export destination is read-only");
                }
                if (zipFile.toFile().exists()) {
                    boolean delete = zipFile.toFile().delete();
                    if (!delete) {
                        return Status.error("Error deleting previous ZIP file contents");
                    }
                }
                try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile.toFile()))) {
                    Files.walkFileTree(workbench, new SimpleFileVisitor<>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            zos.putNextEntry(new ZipEntry(file.toFile().getName()));
                            File confFile = file.toFile();
                            try (FileInputStream fis = new FileInputStream(confFile)) {
                                byte[] writeBuffer = new byte[IOUtils.DEFAULT_BUFFER_SIZE];
                                for (int br = fis.read(writeBuffer); br != -1; br =
                                    fis.read(writeBuffer)) {
                                    zos.write(writeBuffer, 0, br);
                                }
                                zos.flush();
                            }
                            zos.closeEntry();
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                            zos.finish();
                            return super.postVisitDirectory(dir, exc);
                        }
                    });
                } catch (IOException e) {
                    log.error("Error copying file configuration:" + e);
                    Status.error(e.getMessage());
                }
            return Status.OK_STATUS;
            }
        }.schedule();

        return true;
    }
}
