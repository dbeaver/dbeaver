/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.import_config.wizards.dbvis;

import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.ext.import_config.wizards.ConfigImportWizardPage;
import org.jkiss.dbeaver.ui.UIUtils;


public class ConfigImportWizardPageDbvis extends ConfigImportWizardPage {

    protected ConfigImportWizardPageDbvis()
    {
        super("Import DBVisualizer connections");
    }

    public void createControl(Composite parent)
    {
        Composite placeholder = UIUtils.createPlaceholder(parent, 1);
        setControl(placeholder);
    }
}
