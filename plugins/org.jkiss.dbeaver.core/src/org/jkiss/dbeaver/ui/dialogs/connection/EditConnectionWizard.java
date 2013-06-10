/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceViewDescriptor;
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.dbeaver.ui.IActionConstants;
import org.jkiss.dbeaver.ui.preferences.PrefPageDataFormat;
import org.jkiss.dbeaver.ui.preferences.WizardPrefPage;

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
    private EditTunnelDialogPage pageTunnels;
    private EditEventsDialogPage pageEvents;
//    private WizardPrefPage pageDataFormats;

    /**
     * Constructor for SampleNewWizard.
     */
    public EditConnectionWizard(DataSourceDescriptor dataSource)
    {
        super(dataSource.getRegistry());
        this.dataSource = dataSource;
        this.oldData = new DBPConnectionInfo(this.dataSource.getConnectionInfo());
        setWindowTitle(CoreMessages.dialog_connection_wizard_title);
    }

    @Override
    public DataSourceDescriptor getDataSourceDescriptor()
    {
        return dataSource;
    }

    @Override
    public DriverDescriptor getSelectedDriver()
    {
        return dataSource.getDriver();
    }

    @Override
    public ConnectionPageSettings getPageSettings()
    {
        return this.pageSettings;
    }

    /**
     * Adding the page to the wizard.
     */
    @Override
    public void addPages()
    {
        DataSourceViewDescriptor view = dataSource.getDriver().getProviderDescriptor().getView(IActionConstants.EDIT_CONNECTION_POINT);
        if (view != null) {
            pageSettings = new ConnectionPageSettings(this, view, dataSource);
            addPage(pageSettings);
        }

        pageFinal = new ConnectionPageFinal(this, dataSource);
        pageTunnels = new EditTunnelDialogPage(dataSource.getDriver(), dataSource.getConnectionInfo());
        pageEvents = new EditEventsDialogPage(dataSource.getConnectionInfo());
//        pageDataFormats = new WizardPrefPage(new PrefPageDataFormat(dataSource), "Data Formatting", "Data formatting preferences");

        addPage(pageFinal);
        addPage(pageTunnels);
        addPage(pageEvents);
//        addPage(pageDataFormats);
    }

    /**
     * This method is called when 'Finish' button is pressed in
     * the wizard. We will create an operation and run it
     * using wizard as execution context.
     */
    @Override
    public boolean performFinish()
    {
        super.performFinish();
        dataSource.setUpdateDate(new Date());
        pageFinal.saveSettings(dataSource);
        pageTunnels.saveConfigurations();
        pageEvents.saveConfigurations();
        dataSource.getRegistry().updateDataSource(dataSource);
        return true;
    }

    @Override
    public boolean performCancel()
    {
        dataSource.setConnectionInfo(oldData);
        return true;
    }

    /**
     * We will accept the selection in the workbench to see if
     * we can initialize from it.
     */
    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection)
    {
    }

    @Override
    protected void saveSettings()
    {
        super.saveSettings();
        pageTunnels.saveConfigurations();
        pageEvents.saveConfigurations();
    }

}