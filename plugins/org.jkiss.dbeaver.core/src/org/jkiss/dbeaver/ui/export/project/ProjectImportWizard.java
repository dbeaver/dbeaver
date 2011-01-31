/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export.project;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

public class ProjectImportWizard extends Wizard implements IImportWizard {

    public ProjectImportWizard() {
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
        setWindowTitle("Project Import Wizard");
        setNeedsProgressMonitor(true);
    }

    public void addPages() {
        super.addPages();
        ProjectImportData data = new ProjectImportData();
        addPage(new ProjectImportWizardPageFile(data));
        //addPage(new ProjectImportWizardPageFinal(data));
    }

	@Override
	public boolean performFinish() {
		return true;
	}

}
