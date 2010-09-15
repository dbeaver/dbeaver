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
import org.jkiss.dbeaver.registry.DataExporterDescriptor;

public class DataExportWizard extends Wizard implements IExportWizard {

    private static final String RS_EXPORT_WIZARD_DIALOG_SETTINGS = "DataExportWizard";

    private IResultSetProvider resultSetProvider;
    private DataExportPageInit mainPage;
    private DataExporterDescriptor selectedExporter;

    /**
     * Creates a wizard for exporting workspace resources to a zip file.
     */
    public DataExportWizard(IResultSetProvider resultSetProvider) {
        this.resultSetProvider = resultSetProvider;
        IDialogSettings workbenchSettings = WorkbenchPlugin.getDefault().getDialogSettings();
        IDialogSettings section = workbenchSettings.getSection(RS_EXPORT_WIZARD_DIALOG_SETTINGS);//$NON-NLS-1$
        if (section == null) {
			section = workbenchSettings.addNewSection(RS_EXPORT_WIZARD_DIALOG_SETTINGS);//$NON-NLS-1$
		}
        setDialogSettings(section);
    }

    public IResultSetProvider getResultSetProvider() {
        return resultSetProvider;
    }

    public void addPages() {
        super.addPages();
        mainPage = new DataExportPageInit();
        addPage(mainPage);
        addPage(new DataExportPageSettings());
        addPage(new DataExportPageOutput());
    }

    /* (non-Javadoc)
     * Method declared on IWorkbenchWizard.
     */
    public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
        setWindowTitle(DataTransferMessages.DataTransfer_export);
        setNeedsProgressMonitor(true);
    }

    public boolean performFinish() {
        return true;
    }

    public DataExporterDescriptor getSelectedExporter() {
        return selectedExporter;
    }

    public void setSelectedExporter(DataExporterDescriptor selectedExporter) {
        this.selectedExporter = selectedExporter;
    }
}