/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.resultset.export;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.wizards.datatransfer.DataTransferMessages;
import org.jkiss.dbeaver.ext.IResultSetProvider;

public class ResultSetExportWizard extends Wizard implements IExportWizard {

    private static final String RS_EXPORT_WIZARD_DIALOG_SETTINGS = "ResultSetExportWizard";

    private IResultSetProvider resultSetProvider;
    private ResultSetExportPageInit mainPage;

    /**
     * Creates a wizard for exporting workspace resources to a zip file.
     */
    public ResultSetExportWizard(IResultSetProvider resultSetProvider) {
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
        mainPage = new ResultSetExportPageInit();
        addPage(mainPage);
        addPage(new ResultSetExportPageSettings());
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
}