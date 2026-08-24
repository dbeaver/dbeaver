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

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

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
        Path workbench = DBWorkbench.getPlatform().getWorkspace().getMetadataFolder().resolve(".plugins/org.eclipse.core.runtime/.settings");
        if (!workbench.toFile().exists() || !workbench.toFile().isDirectory() || !workbench.toFile().canRead()) {
            log.error("Error reading configuration");
            return false;
        }
        ConfigurationImportData configurationImportData = mainPage.getConfigurationImportData();
        new Job("Importing workspace configuration") {
            @NotNull
            @Override
            protected IStatus run(@NotNull IProgressMonitor monitor) {
                var zipFile = Path.of(configurationImportData.getFilePath());
                if (!Files.exists(zipFile) || !Files.isReadable(zipFile)) {
                    return Status.error("Can't read configuration file");
                }
                try (var fs = FileSystems.newFileSystem(zipFile)) {
                    var root = fs.getPath("/");
                    Files.walkFileTree(root, new CopyingFileVisitor(root, workbench));
                } catch (IOException e) {
                    return Status.error("Error importing workspace configuration", e);
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

}
