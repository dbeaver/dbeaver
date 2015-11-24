/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.dialogs.driver;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.registry.driver.DriverDependencies;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

public class DriverDownloadWizard extends Wizard implements IExportWizard {

    private static final String DRIVER_DOWNLOAD_DIALOG_SETTINGS = "DriverDownload";//$NON-NLS-1$

    private DriverDescriptor driver;
    private DriverDependencies dependencies;
    private boolean updateVersion;
    private boolean forceDownload;
    private DriverDownloadPage downloadPage;

    public DriverDownloadWizard(@NotNull DriverDescriptor driver, DriverDependencies dependencies, boolean updateVersion, boolean forceDownload) {
        this.driver = driver;
        this.dependencies = dependencies;
        this.updateVersion = updateVersion;
        this.forceDownload = forceDownload;
        setWindowTitle(updateVersion ? "Update driver files" : "Setup driver files");
        setNeedsProgressMonitor(hasPredefinedFiles());
        loadSettings();
    }

    DriverDescriptor getDriver() {
        return driver;
    }

    public DriverDependencies getDependencies() {
        return dependencies;
    }

    public boolean isForceDownload() {
        return forceDownload;
    }

    public boolean isUpdateVersion() {
        return updateVersion;
    }

    private void loadSettings()
    {
        IDialogSettings section = UIUtils.getDialogSettings(DRIVER_DOWNLOAD_DIALOG_SETTINGS);
        setDialogSettings(section);
    }

    @Override
    public void addPages() {
        super.addPages();
        if (hasPredefinedFiles()) {
            downloadPage = new DriverDownloadAutoPage();
        } else {
            downloadPage = new DriverDownloadManualPage();
        }
        addPage(downloadPage);
    }

    void pageActivated(Object selectedPage) {
        downloadPage.resolveLibraries();
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
        setWindowTitle("Driver settings");
        setNeedsProgressMonitor(hasPredefinedFiles());
        setHelpAvailable(false);
    }

    @Override
    public boolean canFinish()
    {
        return true;
    }

    @Override
    public boolean performFinish() {
        downloadPage.performFinish();
        return true;
    }

    public String getFinishText() {
        if (hasPredefinedFiles()) {
            return "Download";
        } else {
            return "Open Download Page";
        }
    }

    private boolean hasPredefinedFiles() {
        return CommonUtils.isEmpty(getDriver().getDriverFileSources());
    }

}