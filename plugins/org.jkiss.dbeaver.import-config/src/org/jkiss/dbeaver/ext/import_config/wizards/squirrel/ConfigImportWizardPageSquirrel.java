/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.import_config.wizards.squirrel;

import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.ext.import_config.wizards.ConfigImportWizardPage;
import org.jkiss.dbeaver.ui.UIUtils;


public class ConfigImportWizardPageSquirrel extends ConfigImportWizardPage {

    protected ConfigImportWizardPageSquirrel()
    {
        super("Import SQL Squirrel connections");
    }

    public void createControl(Composite parent)
    {
        Composite placeholder = UIUtils.createPlaceholder(parent, 1);
        setControl(placeholder);
    }
}
