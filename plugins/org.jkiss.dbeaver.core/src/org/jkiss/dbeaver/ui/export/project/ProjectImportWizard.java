/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export.project;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

public class ProjectImportWizard extends Wizard implements IImportWizard {

    private ProjectImportWizardPage mainPage;

    public ProjectImportWizard() {
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
        setWindowTitle("Project Import Wizard"); //NON-NLS-1
        setNeedsProgressMonitor(true);
        mainPage = new ProjectImportWizardPage("Import project"); //NON-NLS-1
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
