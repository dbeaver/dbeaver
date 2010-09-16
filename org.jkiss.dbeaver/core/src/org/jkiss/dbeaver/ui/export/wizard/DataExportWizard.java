/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export.wizard;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.wizards.datatransfer.DataTransferMessages;
import org.jkiss.dbeaver.ext.IResultSetProvider;

import java.util.List;

public class DataExportWizard extends Wizard implements IExportWizard {

    private static final String RS_EXPORT_WIZARD_DIALOG_SETTINGS = "DataExportWizard";

    private DataExportSettings settings;

    public DataExportWizard(List<IResultSetProvider> resultSetProviders) {
        this.settings = new DataExportSettings(resultSetProviders);
        IDialogSettings workbenchSettings = WorkbenchPlugin.getDefault().getDialogSettings();
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
    }

    public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
        setWindowTitle(DataTransferMessages.DataTransfer_export);
        setNeedsProgressMonitor(true);
    }

    public boolean performFinish() {
        getSettings().saveTo(getDialogSettings());
        return true;
    }

}