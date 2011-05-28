/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.ext.ui.DBeaverExtensions;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceViewDescriptor;
import org.jkiss.dbeaver.registry.DriverDescriptor;

import java.util.Date;

/**
 * This is a sample new wizard.
 */

public class EditConnectionWizard extends ConnectionWizard
{
    private DataSourceDescriptor dataSource;
    private DBPConnectionInfo oldData;
    private ConnectionPageSettings pageSettings;
    private ConnectionPageFinal pageFinal;

    /**
     * Constructor for SampleNewWizard.
     */
    public EditConnectionWizard(DataSourceDescriptor dataSource)
    {
        super(dataSource.getRegistry());
        this.dataSource = dataSource;
        this.oldData = new DBPConnectionInfo(this.dataSource.getConnectionInfo());
        setWindowTitle("Edit connection");
    }

    public DataSourceDescriptor getDataSourceDescriptor()
    {
        return dataSource;
    }

    public DriverDescriptor getSelectedDriver()
    {
        return dataSource.getDriver();
    }

    public ConnectionPageSettings getPageSettings()
    {
        return this.pageSettings;
    }

    /**
     * Adding the page to the wizard.
     */
    public void addPages()
    {
        DataSourceViewDescriptor view = dataSource.getDriver().getProviderDescriptor().getView(DBeaverExtensions.EDIT_CONNECTION_POINT);
        if (view != null) {
            pageSettings = new ConnectionPageSettings(this, view, dataSource);
            addPage(pageSettings);
        }

        pageFinal = new ConnectionPageFinal(this, dataSource);
        addPage(pageFinal);
    }

    /**
     * This method is called when 'Finish' button is pressed in
     * the wizard. We will create an operation and run it
     * using wizard as execution context.
     */
    public boolean performFinish()
    {
        super.performFinish();
        dataSource.setUpdateDate(new Date());
        pageFinal.saveSettings(dataSource);
        dataSource.getRegistry().updateDataSource(dataSource);
        return true;
    }

    public boolean performCancel()
    {
        dataSource.setConnectionInfo(oldData);
        return true;
    }

    /**
     * We will accept the selection in the workbench to see if
     * we can initialize from it.
     */
    public void init(IWorkbench workbench, IStructuredSelection selection)
    {
    }

}