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
import org.jkiss.dbeaver.ui.UIUtils;

public class DriverDownloadWizard extends Wizard implements IExportWizard {

    private static final String DRIVER_DOWNLOAD_DIALOG_SETTINGS = "DriverDownload";//$NON-NLS-1$

    private DriverDescriptor driver;

    public DriverDownloadWizard(@NotNull DriverDescriptor driver) {
        this.driver = driver;
        setWindowTitle("Setup driver files");
        loadSettings();
    }

    public DriverDescriptor getDriver() {
        return driver;
    }

    private void loadSettings()
    {
        IDialogSettings section = UIUtils.getDialogSettings(DRIVER_DOWNLOAD_DIALOG_SETTINGS);
        setDialogSettings(section);
    }

    @Override
    public void addPages() {
        super.addPages();
        addPage(new DriverDownloadAutoPage());
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
        return true;
    }



}