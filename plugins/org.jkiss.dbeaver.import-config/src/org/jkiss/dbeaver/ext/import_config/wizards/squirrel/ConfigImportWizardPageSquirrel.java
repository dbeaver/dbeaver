/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.import_config.wizards.squirrel;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.import_config.Activator;
import org.jkiss.dbeaver.ext.import_config.wizards.ConfigImportWizardPage;
import org.jkiss.dbeaver.ext.import_config.wizards.ImportData;


public class ConfigImportWizardPageSquirrel extends ConfigImportWizardPage {

    protected ConfigImportWizardPageSquirrel()
    {
        super("SQL Squirrel");
        setTitle("SQL Squirrel");
        setDescription("Import SQL Squirrel connections");
        setImageDescriptor(Activator.getImageDescriptor("icons/squirrel_big.png"));
    }

    @Override
    protected void loadConnections(ImportData importData) throws DBException
    {
        throw new DBException("SQL Squirrel installation not found");
    }
}
