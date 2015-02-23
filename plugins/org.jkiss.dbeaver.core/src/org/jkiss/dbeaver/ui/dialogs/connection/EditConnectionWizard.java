/*
 * Copyright (C) 2010-2015 Serge Rieder
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

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceViewDescriptor;
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.dbeaver.ui.IActionConstants;
import org.jkiss.dbeaver.ui.preferences.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This is a sample new wizard.
 */

public class EditConnectionWizard extends ConnectionWizard
{
    private DataSourceDescriptor dataSource;
    private DBPConnectionInfo oldData;
    private ConnectionPageSettings pageSettings;
    private ConnectionPageGeneral pageGeneral;
    private ConnectionPageNetwork pageNetwork;
    private EditShellCommandsDialogPage pageEvents;
    private List<WizardPrefPage> prefPages = new ArrayList<WizardPrefPage>();

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

        pageGeneral = new ConnectionPageGeneral(this, dataSource);
        pageNetwork = new ConnectionPageNetwork(this);
        pageEvents = new EditShellCommandsDialogPage(dataSource);

        addPage(pageGeneral);
        addPage(pageNetwork);
        addPage(pageEvents);

        addPreferencePage(new PrefPageMetaData(), "Metadata", "Metadata reading preferences");
        addPreferencePage(new PrefPageDataFormat(), "Data Formatting", "Data formatting preferences");
        addPreferencePage(new PrefPageResultSet(), "Result Sets", "Result Set preferences");
        addPreferencePage(new PrefPageDataEditor(), "Data Edit", "Data (resultset) edit settings");
        addPreferencePage(new PrefPageSQLExecute(), "SQL Processing", "SQL processing settings");
        addPreferencePage(new PrefPageSQLEditor(), "SQL Editor", "SQL editor settings");
    }

    private void addPreferencePage(PreferencePage prefPage, String title, String description)
    {
        WizardPrefPage wizardPage = new WizardPrefPage(prefPage, title, description);
        prefPages.add(wizardPage);
        if (prefPage instanceof IWorkbenchPropertyPage) {
            ((IWorkbenchPropertyPage) prefPage).setElement(dataSource);
        }
        addPage(wizardPage);
    }

    /**
     * This method is called when 'Finish' button is pressed in
     * the wizard. We will create an operation and run it
     * using wizard as execution context.
     */
    @Override
    public boolean performFinish()
    {
        dataSource.setUpdateDate(new Date());
        saveSettings(dataSource);
        dataSource.getRegistry().updateDataSource(dataSource);
        return true;
    }

    @Override
    public boolean performCancel()
    {
        dataSource.setConnectionInfo(oldData);
        for (WizardPrefPage prefPage : prefPages) {
            prefPage.performCancel();
        }
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
    protected void saveSettings(DataSourceDescriptor dataSource)
    {
        pageSettings.saveSettings(dataSource);
        pageGeneral.saveSettings(dataSource);
        pageNetwork.saveConfigurations(dataSource);
        pageEvents.saveConfigurations(dataSource);
        for (WizardPrefPage prefPage : prefPages) {
            prefPage.performFinish();
        }
    }

}