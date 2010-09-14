/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.internal.wizards.datatransfer.DataTransferMessages;
import org.eclipse.ui.internal.wizards.datatransfer.WizardArchiveFileResourceExportPage1;
import org.jkiss.dbeaver.ui.DBIcon;

import java.util.List;

public class ResultSetExportWizard extends Wizard implements IExportWizard {

    private static final String RS_EXPORT_WIZARD_DIALOG_SETTINGS = "ZipFileExportWizard";

    private ResultSetExportPageInit mainPage;

    /**
     * Creates a wizard for exporting workspace resources to a zip file.
     */
    public ResultSetExportWizard() {
        IDialogSettings workbenchSettings = WorkbenchPlugin.getDefault().getDialogSettings();
        IDialogSettings section = workbenchSettings.getSection(RS_EXPORT_WIZARD_DIALOG_SETTINGS);//$NON-NLS-1$
        if (section == null) {
			section = workbenchSettings.addNewSection(RS_EXPORT_WIZARD_DIALOG_SETTINGS);//$NON-NLS-1$
		}
        setDialogSettings(section);
    }

    /* (non-Javadoc)
     * Method declared on IWizard.
     */
    public void addPages() {
        super.addPages();
        mainPage = new ResultSetExportPageInit();
        addPage(mainPage);
    }

    /* (non-Javadoc)
     * Method declared on IWorkbenchWizard.
     */
    public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
        setWindowTitle(DataTransferMessages.DataTransfer_export);
        setDefaultPageImageDescriptor(DBIcon.EXPORT.getImageDescriptor());//$NON-NLS-1$
        setNeedsProgressMonitor(true);
    }

    public boolean performFinish() {
        return true;
    }
}