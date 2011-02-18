/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.import_config.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

public abstract class ConfigImportWizard extends Wizard implements IImportWizard {
	
	private ConfigImportWizardPage mainPage;

	public ConfigImportWizard() {
		super();
	}

	public boolean performFinish() {
        return true;
	}
	 
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("Import Configuration"); //NON-NLS-1
		setNeedsProgressMonitor(true);
		mainPage = createMainPage(); //NON-NLS-1
	}

    protected abstract ConfigImportWizardPage createMainPage();

    public void addPages() {
        super.addPages(); 
        addPage(mainPage);        
    }

}
