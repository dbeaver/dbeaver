/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.tools;

import org.jkiss.dbeaver.ext.mysql.MySQLDataSourceProvider;
import org.jkiss.dbeaver.ext.mysql.MySQLServerHome;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ui.dialogs.tools.AbstractScriptExecuteWizard;

import java.io.File;
import java.util.List;

class MySQLScriptExecuteWizard extends AbstractScriptExecuteWizard<MySQLCatalog> {

    private boolean isImport;
    private MySQLScriptExecuteWizardPageSettings mainPage;

    public MySQLScriptExecuteWizard(MySQLCatalog catalog, boolean isImport)
    {
        super(catalog, isImport ? "Database Import" : "Execute Script");
        this.isImport = isImport;
        this.mainPage = new MySQLScriptExecuteWizardPageSettings(this);
    }

    public boolean isImport()
    {
        return isImport;
    }

    @Override
    public void addPages()
    {
        addPage(mainPage);
        super.addPages();
    }

    @Override
    public void fillProcessParameters(List<String> cmd)
    {
        String dumpPath = new File(getClientHome().getHomePath(), "bin/mysql").getAbsolutePath();
        cmd.add(dumpPath);
    }

    @Override
    public MySQLServerHome findServerHome(String clientHomeId)
    {
        return MySQLDataSourceProvider.getServerHome(clientHomeId);
    }

    @Override
    protected List<String> getCommandLine()
    {
        return MySQLToolScript.getMySQLToolCommandLine(this);
    }
}
