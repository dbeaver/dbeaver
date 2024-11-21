/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.preference.IPreferencePage;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverSubstitutionDescriptor;
import org.jkiss.dbeaver.model.navigator.DBNBrowseSettings;
import org.jkiss.dbeaver.model.secret.DBSSecretValue;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourcePageDescriptor;
import org.jkiss.dbeaver.registry.DataSourceViewDescriptor;
import org.jkiss.dbeaver.registry.DataSourceViewRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.IActionConstants;
import org.jkiss.dbeaver.ui.IDialogPageProvider;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.datasource.DataSourceHandler;
import org.jkiss.dbeaver.ui.dialogs.BaseAuthDialog;
import org.jkiss.dbeaver.ui.preferences.*;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.security.MessageDigest;
import java.util.Locale;

/**
 * Edit connection dialog
 */

public class EditConnectionWizard extends ConnectionWizard {
    private static final Log log = Log.getLog(EditConnectionWizard.class);

    @NotNull
    private final DataSourceDescriptor originalDataSource;
    @NotNull
    private final DataSourceDescriptor dataSource;
    @Nullable
    private ConnectionPageSettings pageSettings;
    private ConnectionPageGeneral pageGeneral;
    //private ConnectionPageNetwork pageNetwork;
    private ConnectionPageInitialization pageInit;
    private ConnectionPageShellCommands pageEvents;

    /**
     * Constructor for SampleNewWizard.
     */
    public EditConnectionWizard(@NotNull DataSourceDescriptor dataSource) {
        this.originalDataSource = dataSource;
        this.dataSource = new DataSourceDescriptor(dataSource, dataSource.getRegistry());
        if (!this.dataSource.isSavePassword()) {
            this.dataSource.resetPassword();
        }

        setWindowTitle(CoreMessages.dialog_connection_wizard_title);
        setDriverSubstitution(dataSource.getDriverSubstitution());

        addPropertyChangeListener(event -> {
            if (ConnectionPageAbstract.PROP_DRIVER_SUBSTITUTION.equals(event.getProperty())) {
                ((Dialog) getContainer()).close();

                UIUtils.asyncExec(() -> EditConnectionDialog.openEditConnectionDialog(
                    UIUtils.getActiveWorkbenchWindow(),
                    getOriginalDataSource(),
                    null,
                    wizard -> wizard.setDriverSubstitution((DBPDriverSubstitutionDescriptor) event.getNewValue())
                ));
            }
        });
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
    public DBPDriver getSelectedDriver() {
        return dataSource.getDriver();
    }

    @Override
    DBPProject getSelectedProject() {
        return dataSource.getRegistry().getProject();
    }

    @Override
    DBNBrowseSettings getSelectedNavigatorSettings() {
        return dataSource.getNavigatorSettings();
    }

    @Nullable
    @Override
    public ConnectionPageSettings getPageSettings() {
        return this.pageSettings;
    }

    /**
     * Adding the page to the wizard.
     */
    @Override
    public void addPages() {
        if (dataSource.getDriver().isNotAvailable()) {
            addPage(new ConnectionPageDeprecation(dataSource.getDriver()));
            return;
        }

        DataSourceViewDescriptor view = DataSourceViewRegistry.getInstance().findView(
            dataSource.getDriver().getProviderDescriptor(),
            IActionConstants.EDIT_CONNECTION_POINT);
        if (view != null) {
            pageSettings = new ConnectionPageSettings(this, view, getDriverSubstitution());
            addPage(pageSettings);
        }

        boolean embedded = dataSource.getDriver().isEmbedded();
        pageGeneral = new ConnectionPageGeneral(this, dataSource);

//        if (!embedded) {
//            pageNetwork = new ConnectionPageNetwork(this);
//        }
        pageInit = new ConnectionPageInitialization(dataSource);
        pageEvents = new ConnectionPageShellCommands(dataSource);

        addPage(pageGeneral);
        if (pageSettings != null) {
            pageSettings.addSubPage(pageInit);
            pageSettings.addSubPage(pageEvents);
        }

        if (!embedded && pageSettings != null) {
            PrefPageConnectionClient pageClientSettings = new PrefPageConnectionClient();
            pageSettings.addSubPage(
                createPreferencePage(pageClientSettings, CoreMessages.dialog_connection_edit_wizard_connections, CoreMessages.dialog_connection_edit_wizard_connections_description));
        }
        if (pageSettings != null) {
            PrefPageTransactions pageClientTransactions = new PrefPageTransactions();
            pageSettings.addSubPage(
                createPreferencePage(pageClientTransactions, CoreMessages.dialog_connection_edit_wizard_transactions, CoreMessages.dialog_connection_edit_wizard_transactions_description));
        }

        addPreferencePage(new PrefPageMetaData(), CoreMessages.dialog_connection_edit_wizard_metadata,  CoreMessages.dialog_connection_edit_wizard_metadata_description);
        addPreferencePage(new PrefPageErrorHandle(), CoreMessages.pref_page_error_handle_name,  CoreMessages.pref_page_error_handle_description);

        for (DataSourcePageDescriptor page : DataSourceViewRegistry.getInstance().getRootDataSourcePages(dataSource)) {
            addDataSourcePage(null, page);
        }
    }

    private void addDataSourcePage(WizardPrefPage parent, DataSourcePageDescriptor page) {
        IPreferencePage pageInstance;
        try {
            pageInstance = page.createPage();
        } catch (Exception e) {
            log.error("Error instantiating connection preference page '" + page.getId() + "'", e);
            return;
        }
        WizardPrefPage thisWizardPage = null;
        if (parent != null) {
            if (page.getTitle() != null) {
                thisWizardPage = parent.addSubPage(pageInstance, page.getTitle(), page.getDescription());
            } else {
                parent.addSubPage(page.getId(), pageInstance);
            }
        } else {
            if (pageInstance instanceof IWizardPage) {
                addPage((IWizardPage) pageInstance);
            } else {
                thisWizardPage = addPreferencePage(pageInstance, page.getTitle(), page.getDescription());
            }
        }
        if (thisWizardPage != null) {
            for (DataSourcePageDescriptor childPage : DataSourceViewRegistry.getInstance().getChildDataSourcePages(dataSource, page.getId())) {
                addDataSourcePage(thisWizardPage, childPage);
            }
        }
    }

    @Override
    protected IAdaptable getActiveElement() {
        return dataSource;
    }

    public IWizardPage getPage(String name) {
        for (IWizardPage page : getPages()) {
            String pageName = page.getName();
            if (pageName.equals(name)) {
                return page;
            }
            if (page instanceof IDialogPageProvider) {
                final IDialogPage[] subPages = ((IDialogPageProvider) page).getDialogPages(false, true);
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

    @NotNull
    @Override
    protected PersistResult persistDataSource() {
        if (dataSource.getDriver().isNotAvailable()) {
            return PersistResult.UNCHANGED;
        }

        DBPDataSourceRegistry registry = originalDataSource.getRegistry();
        DataSourceDescriptor dsCopy = new DataSourceDescriptor(originalDataSource, registry);
        DataSourceDescriptor dsChanged = new DataSourceDescriptor(dataSource, dataSource.getRegistry());
        try {
            saveSettings(dsChanged);

            if (dsCopy.equalSettings(dsChanged)) {
                // No changes
                return PersistResult.UNCHANGED;
            }

            // Check locked datasources
            if (!CommonUtils.isEmpty(dataSource.getLockPasswordHash())) {
                if (!isOnlyUserCredentialChanged(dsCopy, dsChanged)) {
                    if (!checkLockPassword()) {
                        return PersistResult.ERROR;
                    }
                }
            }
        } finally {
            dsCopy.dispose();
            dsChanged.dispose();
        }

        // Save
        saveSettings(originalDataSource);

        // Set selected shared creds (creds may be resolved during auth model interactions)
        DBSSecretValue selectedSharedCredentials = dataSource.getSelectedSharedCredentials();
        if (selectedSharedCredentials != null) {
            selectedSharedCredentials.setValue(originalDataSource.saveToSecret());
            originalDataSource.setSelectedSharedCredentials(selectedSharedCredentials);
        }

        if (originalDataSource.persistConfiguration()) {
            return PersistResult.CHANGED;
        } else {
            return PersistResult.ERROR;
        }
    }

    /**
     * This method is called when 'Finish' button is pressed in
     * the wizard. We will create an operation and run it
     * using wizard as execution context.
     */
    @Override
    public boolean performFinish() {
        PersistResult result = persistDataSource();
        if (result == PersistResult.CHANGED && originalDataSource.isConnected()) {
            if (UIUtils.confirmAction(getShell(), CoreMessages.dialog_connection_edit_wizard_conn_change_title,
                NLS.bind(CoreMessages.dialog_connection_edit_wizard_conn_change_question,
                originalDataSource.getName()) )
            ) {
                DataSourceHandler.reconnectDataSource(null, originalDataSource);
            }
        }
        return result != PersistResult.ERROR;
    }

    private boolean isOnlyUserCredentialChanged(DataSourceDescriptor dsCopy, DataSourceDescriptor dsChanged) {
        dsCopy.getConnectionConfiguration().setUserName(null);
        dsCopy.getConnectionConfiguration().setUserPassword(null);
        dsChanged.getConnectionConfiguration().setUserName(null);
        dsChanged.getConnectionConfiguration().setUserPassword(null);
        return dsCopy.equalSettings(dsChanged);
    }

    private boolean checkLockPassword() {
        BaseAuthDialog dialog = new BaseAuthDialog(getShell(), CoreMessages.dialog_connection_edit_wizard_lock_pwd_title, true, false);
        if (dialog.open() == IDialogConstants.OK_ID) {
            final String userPassword = dialog.getUserPassword();
            if (!CommonUtils.isEmpty(userPassword)) {
                try {
                    final byte[] md5hash = MessageDigest.getInstance("MD5").digest(userPassword.getBytes(GeneralUtils.DEFAULT_ENCODING));
                    final String hexString = CommonUtils.toHexString(md5hash).toLowerCase(Locale.ENGLISH).trim();
                    if (hexString.equals(dataSource.getLockPasswordHash())) {
                        return true;
                    }
                    UIUtils.showMessageBox(getShell(), CoreMessages.dialog_connection_edit_wizard_bad_pwd_title, CoreMessages.dialog_connection_edit_wizard_bad_pwd_msg, SWT.ICON_ERROR);
                } catch (Throwable e) {
                    DBWorkbench.getPlatformUI().showError(CoreMessages.dialog_connection_edit_wizard_error_md5_title, CoreMessages.dialog_connection_edit_wizard_error_md5_msg, e);
                }
            }
        }
        return false;
    }

    @Override
    public boolean performCancel() {
        return super.performCancel();
    }

    /**
     * We will accept the selection in the workbench to see if
     * we can initialize from it.
     */
    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
    }

    @Override
    protected void saveSettings(DataSourceDescriptor dataSource) {
        if (dataSource.getDriver().isNotAvailable()) {
            return;
        }

        if (isPageActive(pageSettings)) {
            pageSettings.saveSettings(dataSource);
        }
        pageGeneral.saveSettings(dataSource);
        pageInit.saveSettings(dataSource);
        pageEvents.saveSettings(dataSource);
        for (IDialogPage page : getPages()) {
            setPageDataSourceElement(dataSource, page);
        }
        for (WizardPrefPage wpp : getPrefPages()) {
            setPageDataSourceElement(dataSource, wpp);
        }

        super.savePrefPageSettings();

        // Reset password if "Save password" was disabled
        if (!dataSource.isSavePassword()) {
            dataSource.resetPassword();
        }
    }

    private void setPageDataSourceElement(DataSourceDescriptor dataSource, IDialogPage page) {
        if (page instanceof WizardPrefPage) {
            WizardPrefPage[] subPages = ((WizardPrefPage)page).getDialogPages(false, true);
            if (subPages != null) {
                for (IDialogPage dp : subPages) {
                    setPageDataSourceElement(dataSource, dp);
                }
            }

            page = ((WizardPrefPage) page).getPreferencePage();
        } else if (page instanceof IDialogPageProvider) {
            IDialogPage[] subPages = ((IDialogPageProvider)page).getDialogPages(false, true);
            if (subPages != null) {
                for (IDialogPage dp : subPages) {
                    setPageDataSourceElement(dataSource, dp);
                }
            }
        }
        if (page instanceof IWorkbenchPropertyPage) {
            ((IWorkbenchPropertyPage) page).setElement(dataSource);
        }
    }

    private void savePageSettings(WizardPrefPage prefPage) {
        if (isPageActive(prefPage)) {
            prefPage.performFinish();
        }
/*
        final WizardPrefPage[] subPages = prefPage.getDialogPages();
        if (subPages != null) {
            for (WizardPrefPage subPage : subPages) {
                if (isPageActive(subPage)) {
                    subPage.performFinish();
                }
            }
        }
*/
    }

    @Override
    public void dispose() {
        super.dispose();
        // Dispose temp datasource
        this.dataSource.dispose();
    }
}