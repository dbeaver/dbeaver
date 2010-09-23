/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export.wizard;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;

import java.util.List;

public class DataExportWizard extends Wizard implements IExportWizard {

    private static final String RS_EXPORT_WIZARD_DIALOG_SETTINGS = "DataExportWizard";

    private DataExportSettings settings;

    public DataExportWizard(List<DBSDataContainer> dataContainers) {
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

    public void addPages() {
        super.addPages();
        addPage(new DataExportPageInit());
        addPage(new DataExportPageSettings());
        addPage(new DataExportPageOutput());
        addPage(new DataExportPageFinal());
    }

    public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
        setWindowTitle("Export data");
        setNeedsProgressMonitor(true);
    }

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