/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceViewDescriptor;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.ui.IActionConstants;
import org.jkiss.dbeaver.ui.ICompositeDialogPage;
import org.jkiss.dbeaver.ui.preferences.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This is a sample new wizard.
 */

public class EditConnectionWizard extends ConnectionWizard
{
    @NotNull
    private DataSourceDescriptor dataSource;
    private DBPConnectionConfiguration oldData;
    @Nullable
    private ConnectionPageSettings pageSettings;
    private ConnectionPageGeneral pageGeneral;
    private ConnectionPageNetwork pageNetwork;
    private EditShellCommandsDialogPage pageEvents;
    private List<WizardPrefPage> prefPages = new ArrayList<>();

    /**
     * Constructor for SampleNewWizard.
     */
    public EditConnectionWizard(DataSourceDescriptor dataSource)
    {
        super(dataSource.getRegistry());
        this.dataSource = dataSource;
        this.oldData = new DBPConnectionConfiguration(this.dataSource.getConnectionConfiguration());
        setWindowTitle(CoreMessages.dialog_connection_wizard_title);
    }

    @NotNull
    @Override
    public DataSourceDescriptor getActiveDataSource() {
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

        boolean embedded = dataSource.getDriver().isEmbedded();
        pageGeneral = new ConnectionPageGeneral(this, dataSource);
        if (!embedded) {
            pageNetwork = new ConnectionPageNetwork(this);
        }
        pageEvents = new EditShellCommandsDialogPage(dataSource);

        addPage(pageGeneral);
        if (pageSettings != null) {
            if (!embedded) {
                pageSettings.addSubPage(pageNetwork);
            }
            pageSettings.addSubPage(pageEvents);
        }

        addPreferencePage(new PrefPageMetaData(), "Metadata", "Metadata reading preferences");
        WizardPrefPage rsPage = addPreferencePage(new PrefPageResultSetMain(), "Result Sets", "Result Set preferences");
        rsPage.addSubPage(new PrefPageResultSetBinaries(), "Binaries", "Binary data representation");
        rsPage.addSubPage(new PrefPageDataFormat(), "Data Formatting", "Data formatting preferences");
        rsPage.addSubPage(new PrefPageResultSetPresentation(), "Presentation", "ResultSets UI & presentation");
        WizardPrefPage sqlPage = addPreferencePage(new PrefPageSQLEditor(), "SQL Editor", "SQL editor settings");
        sqlPage.addSubPage(new PrefPageSQLExecute(), "SQL Processing", "SQL processing settings");
    }

    private WizardPrefPage addPreferencePage(PreferencePage prefPage, String title, String description)
    {
        WizardPrefPage wizardPage = new WizardPrefPage(prefPage, title, description);
        prefPages.add(wizardPage);
        if (prefPage instanceof IWorkbenchPropertyPage) {
            ((IWorkbenchPropertyPage) prefPage).setElement(dataSource);
        }
        addPage(wizardPage);
        return wizardPage;
    }

    public IWizardPage getPage(String name) {
        for (IWizardPage page : getPages()) {
            String pageName = page.getName();
            if (pageName.equals(name)) {
                return page;
            }
            if (page instanceof ICompositeDialogPage) {
                final IDialogPage[] subPages = ((ICompositeDialogPage) page).getSubPages();
                if (subPages != null) {
                    for (IDialogPage subPage : subPages) {
                        if (subPage instanceof IWizardPage && ((IWizardPage) subPage).getName().equals(name)) {
                            return (IWizardPage) subPage;
                        }
                    }
                }
            }
        }
        return null;
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
        if (pageSettings != null) {
            pageSettings.saveSettings(dataSource);
        }
        pageGeneral.saveSettings(dataSource);
        if (pageNetwork != null) {
            pageNetwork.saveConfigurations(dataSource);
        }
        pageEvents.saveConfigurations(dataSource);
        for (WizardPrefPage prefPage : prefPages) {
            prefPage.performFinish();
        }
    }

}