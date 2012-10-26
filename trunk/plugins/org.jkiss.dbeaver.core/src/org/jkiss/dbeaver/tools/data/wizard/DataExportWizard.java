/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.tools.data.wizard;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.core.DBeaverActivator;

import java.util.List;

public class DataExportWizard extends Wizard implements IExportWizard {

    private static final String RS_EXPORT_WIZARD_DIALOG_SETTINGS = "DataExportWizard";

    private DataExportSettings settings;

    public DataExportWizard(List<DataExportProvider> dataContainers) {
        this.settings = new DataExportSettings(dataContainers);
        IDialogSettings workbenchSettings = DBeaverActivator.getInstance().getDialogSettings();
        IDialogSettings section = workbenchSettings.getSection(RS_EXPORT_WIZARD_DIALOG_SETTINGS);//$NON-NLS-1$
        if (section == null) {
			section = workbenchSettings.addNewSection(RS_EXPORT_WIZARD_DIALOG_SETTINGS);//$NON-NLS-1$
		}
        setDialogSettings(section);

        settings.loadFrom(section);
    }

    public DataExportSettings getSettings()
    {
        return settings;
    }

    @Override
    public void addPages() {
        super.addPages();
        addPage(new DataExportPageInit());
        addPage(new DataExportPageSettings());
        addPage(new DataExportPageOutput());
        addPage(new DataExportPageFinal());
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
        setWindowTitle("Export data");
        setNeedsProgressMonitor(true);
    }

    @Override
    public boolean performFinish() {
        // Save settings
        getSettings().saveTo(getDialogSettings());

        // Run export jobs
        executeJobs();

        // Done
        return true;
    }

    private void executeJobs() {
        // Schedule jobs for data providers
        int totalJobs = settings.getDataProviders().size();
        if (totalJobs > settings.getMaxJobCount()) {
            totalJobs = settings.getMaxJobCount();
        }
        for (int i = 0; i < totalJobs; i++) {
            new DataExportJob(settings).schedule();
        }
    }

}