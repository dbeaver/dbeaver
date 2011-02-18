/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.import_config.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;


public abstract class ConfigImportWizardPage extends WizardPage {

    protected ConfigImportWizardPage(String pageName)
    {
        super(pageName);
    }

    public void createControl(Composite parent)
    {

    }
}
