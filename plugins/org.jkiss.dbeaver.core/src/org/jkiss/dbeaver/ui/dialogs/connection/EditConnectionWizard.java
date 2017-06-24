/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceViewDescriptor;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.IActionConstants;
import org.jkiss.dbeaver.ui.ICompositeDialogPage;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.datasource.DataSourceHandler;
import org.jkiss.dbeaver.ui.preferences.*;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * This is a sample new wizard.
 */

public class EditConnectionWizard extends ConnectionWizard
{
    @NotNull
    private DataSourceDescriptor originalDataSource;
    @NotNull
    private DataSourceDescriptor dataSource;
    @Nullable
    private ConnectionPageSettings pageSettings;
    private ConnectionPageGeneral pageGeneral;
    private ConnectionPageNetwork pageNetwork;
    private EditShellCommandsDialogPage pageEvents;
    private List<WizardPrefPage> prefPages = new ArrayList<>();

    /**
     * Constructor for SampleNewWizard.
     */
    public EditConnectionWizard(@NotNull DataSourceDescriptor dataSource)
    {
        this.originalDataSource = dataSource;
        this.dataSource = new DataSourceDescriptor(dataSource);
        if (!this.dataSource.isSavePassword()) {
            this.dataSource.getConnectionConfiguration().setUserPassword(null);
        }

        setWindowTitle(CoreMessages.dialog_connection_wizard_title);
    }

    @NotNull
    @Override
    public DataSourceDescriptor getActiveDataSource() {
        return dataSource;
    }

    @NotNull
    @Override
    public DataSourceDescriptor getOriginalDataSource() {
        return originalDataSource;
    }

    @Override
    public DBPDataSourceRegistry getDataSourceRegistry() {
        return dataSource.getRegistry();
    }

    @Override
    public DriverDescriptor getSelectedDriver()
    {
        return dataSource.getDriver();
    }

    @Nullable
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
            ((IWorkbenchPropertyPage) prefPage).setElement(originalDataSource);
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
        DataSourceDescriptor dsCopy = new DataSourceDescriptor(originalDataSource);
        DataSourceDescriptor dsChanged = new DataSourceDescriptor(dataSource);
        saveSettings(dsChanged);

        if (dsCopy.equalSettings(dsChanged)) {
            // No changes
            return true;
        }

        // Check locked datasources
        if (!CommonUtils.isEmpty(dataSource.getLockPasswordHash())) {
            if (DBeaverCore.getInstance().getSecureStorage().useSecurePreferences() && !isOnlyUserCredentialChanged(dsCopy, dsChanged)) {
                if (!checkLockPassword()) {
                    return false;
                }
            }
        }

        // Save
        saveSettings(originalDataSource);
        originalDataSource.getRegistry().updateDataSource(originalDataSource);

        if (originalDataSource.isConnected()) {
            if (UIUtils.confirmAction(
                getShell(),
                "Connection changed",
                "Connection '" + originalDataSource.getName() + "' has been changed.\nDo you want to reconnect?"))
            {
                DataSourceHandler.reconnectDataSource(null, originalDataSource);
            }
        }

        return true;
    }

    private boolean isOnlyUserCredentialChanged(DataSourceDescriptor dsCopy, DataSourceDescriptor dsChanged) {
        dsCopy.getConnectionConfiguration().setUserName(null);
        dsCopy.getConnectionConfiguration().setUserPassword(null);
        dsChanged.getConnectionConfiguration().setUserName(null);
        dsChanged.getConnectionConfiguration().setUserPassword(null);
        return dsCopy.equalSettings(dsChanged);
    }

    private boolean checkLockPassword() {
        BaseAuthDialog dialog = new BaseAuthDialog(getShell(), "Enter lock password", true);
        if (dialog.open() == IDialogConstants.OK_ID) {
            final String userPassword = dialog.getUserPassword();
            if (!CommonUtils.isEmpty(userPassword)) {
                try {
                    final byte[] md5hash = MessageDigest.getInstance("MD5").digest(userPassword.getBytes(GeneralUtils.DEFAULT_ENCODING));
                    final String hexString = CommonUtils.toHexString(md5hash).toLowerCase(Locale.ENGLISH).trim();
                    if (hexString.equals(dataSource.getLockPasswordHash())) {
                        return true;
                    }
                    UIUtils.showMessageBox(getShell(), "Bad password", "Password doesn't match", SWT.ICON_ERROR);
                } catch (Throwable e) {
                    DBUserInterface.getInstance().showError("Error making MD5", "Can't generate password hash", e);
                }
            }
        }
        return false;
    }

    @Override
    public boolean performCancel()
    {
        // Just in case - cancel changes in pref pages (there shouldn't be any)
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
        if (isPageActive(pageSettings)) {
            pageSettings.saveSettings(dataSource);
        }
        pageGeneral.saveSettings(dataSource);
        if (isPageActive(pageNetwork)) {
            pageNetwork.saveConfigurations(dataSource);
        }
        pageEvents.saveConfigurations(dataSource);
        for (WizardPrefPage prefPage : prefPages) {
            savePageSettings(prefPage);
        }

        // Reset password if "Save password" was disabled
        if (!dataSource.isSavePassword()) {
            dataSource.getConnectionConfiguration().setUserPassword(null);
        }
    }

    private void savePageSettings(WizardPrefPage prefPage) {
        if (isPageActive(prefPage)) {
            prefPage.performFinish();
        }
        final WizardPrefPage[] subPages = prefPage.getSubPages();
        if (subPages != null) {
            for (WizardPrefPage subPage : subPages) {
                if (isPageActive(subPage)) {
                    subPage.performFinish();
                }
            }
        }
    }

    private static boolean isPageActive(IDialogPage page) {
        return page != null && page.getControl() != null;
    }

}