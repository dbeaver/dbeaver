/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.import_config.wizards.dbvis;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.ext.import_config.wizards.ConfigImportWizard;

public class ConfigImportWizardDbvis extends ConfigImportWizard {
	
    protected ConfigImportWizardPageDbvis createMainPage()
    {
        return new ConfigImportWizardPageDbvis();
    }


}
