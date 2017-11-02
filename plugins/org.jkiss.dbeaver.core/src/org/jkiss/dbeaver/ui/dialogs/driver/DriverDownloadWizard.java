/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.dialogs.driver;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.core.CoreMessages;
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
        setWindowTitle(updateVersion ? CoreMessages.dialog_driver_download_wizard_title_upload_files : CoreMessages.dialog_driver_download_wizard_title_setup_files);
        setNeedsProgressMonitor(isAutoDownloadWizard());
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

    public void setForceDownload(boolean forceDownload) {
        this.forceDownload = forceDownload;
    }

    public boolean isUpdateVersion() {
        return updateVersion;
    }

    public DriverDownloadDialog getContainer() {
        return (DriverDownloadDialog)super.getContainer();
    }

    private void loadSettings()
    {
        IDialogSettings section = UIUtils.getDialogSettings(DRIVER_DOWNLOAD_DIALOG_SETTINGS);
        setDialogSettings(section);
    }

    @Override
    public void addPages() {
        super.addPages();
        if (isAutoDownloadWizard()) {
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
        setWindowTitle(CoreMessages.dialog_driver_download_wizard_title_setting);
        setNeedsProgressMonitor(isAutoDownloadWizard());
        setHelpAvailable(false);
    }

    @Override
    public boolean canFinish()
    {
        return true;
    }

    @Override
    public boolean performFinish() {
        return downloadPage.performFinish();
    }

    public String getFinishText() {
        if (isAutoDownloadWizard()) {
            return CoreMessages.dialog_driver_download_wizard_download;
        } else {
            return CoreMessages.dialog_driver_download_wizard_open_download;
        }
    }

    public boolean isAutoDownloadWizard() {
        return CommonUtils.isEmpty(getDriver().getDriverFileSources());
    }

}