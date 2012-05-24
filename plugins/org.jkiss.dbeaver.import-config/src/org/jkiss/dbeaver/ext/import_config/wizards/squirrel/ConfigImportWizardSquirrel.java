/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.import_config.wizards.squirrel;

import org.jkiss.dbeaver.ext.import_config.wizards.ConfigImportWizard;

public class ConfigImportWizardSquirrel extends ConfigImportWizard {
	
    @Override
    protected ConfigImportWizardPageSquirrel createMainPage()
    {
        return new ConfigImportWizardPageSquirrel();
    }


}
