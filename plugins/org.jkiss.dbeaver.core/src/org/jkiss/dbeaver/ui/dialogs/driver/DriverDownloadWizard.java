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
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.dbeaver.registry.DriverFileDescriptor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.util.List;

public class DriverDownloadWizard extends Wizard implements IExportWizard {

    private static final String DRIVER_DOWNLOAD_DIALOG_SETTINGS = "DriverDownload";//$NON-NLS-1$

    private DriverDescriptor driver;
    private List<DriverFileDescriptor> files;
    private boolean forceDownload;
    private DriverDownloadPage downloadPage;

    public DriverDownloadWizard(@NotNull DriverDescriptor driver, List<DriverFileDescriptor> files, boolean forceDownload) {
        this.driver = driver;
        this.files = files;
        this.forceDownload = forceDownload;
        setWindowTitle("Setup driver files");
        setNeedsProgressMonitor(hasPredefinedFiles());
        loadSettings();
    }

    DriverDescriptor getDriver() {
        return driver;
    }

    List<DriverFileDescriptor> getFiles() {
        return files;
    }

    public boolean isForceDownload() {
        return forceDownload;
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

    @Override
    public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
        setWindowTitle("Driver settings");
        setNeedsProgressMonitor(true);
        setHelpAvailable(true);
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