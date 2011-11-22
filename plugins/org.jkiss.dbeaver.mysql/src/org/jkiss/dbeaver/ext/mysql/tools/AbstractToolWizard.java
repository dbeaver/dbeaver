/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.tools;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.ext.mysql.MySQLDataSourceProvider;
import org.jkiss.dbeaver.ext.mysql.MySQLServerHome;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;

/**
 * Abstract wizard
 */
public abstract class AbstractToolWizard extends Wizard  {

    private final MySQLCatalog catalog;
    private MySQLServerHome serverHome;
    private DBPConnectionInfo connectionInfo;

    protected AbstractToolWizard(MySQLCatalog catalog)
    {
        this.catalog = catalog;
    }

    public MySQLCatalog getCatalog()
    {
        return catalog;
    }

    public DBPConnectionInfo getConnectionInfo()
    {
        return connectionInfo;
    }

    public MySQLServerHome getServerHome()
    {
        return serverHome;
    }

    @Override
    public void createPageControls(Composite pageContainer)
    {
        super.createPageControls(pageContainer);

        WizardPage currentPage = (WizardPage) getStartingPage();

        DBSDataSourceContainer container = getCatalog().getDataSource().getContainer();
        connectionInfo = container.getConnectionInfo();
        String clientHomeId = connectionInfo.getClientHomeId();
        if (clientHomeId == null) {
            currentPage.setErrorMessage("Server home is not specified for connection");
            getContainer().updateMessage();
            return;
        }
        serverHome = MySQLDataSourceProvider.getServerHome(clientHomeId);
        if (serverHome == null) {
            currentPage.setErrorMessage("Server home '" + clientHomeId + "' not found");
            getContainer().updateMessage();
        }
    }
}
