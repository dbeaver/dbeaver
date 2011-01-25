/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export.project;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

public class ProjectExportWizard extends Wizard implements IExportWizard {

    private ProjectExportWizardPage mainPage;

    public ProjectExportWizard() {
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
        setWindowTitle("Project Export Wizard"); //NON-NLS-1
        setNeedsProgressMonitor(true);
        mainPage = new ProjectExportWizardPage("Export project"); //NON-NLS-1
    }

    public void addPages() {
        super.addPages();
        addPage(mainPage);
    }

	@Override
	public boolean performFinish() {
		return true;
	}

}
