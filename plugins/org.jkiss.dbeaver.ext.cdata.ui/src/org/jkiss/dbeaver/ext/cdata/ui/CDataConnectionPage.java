/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.cdata.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.cdata.CDataAuthModel;
import org.jkiss.dbeaver.ext.cdata.CDataLicenseUIService;
import org.jkiss.dbeaver.ext.cdata.model.CDataConnectionHierarchy;
import org.jkiss.dbeaver.ext.cdata.model.CDataConnectionHierarchy.Property;
import org.jkiss.dbeaver.ext.cdata.model.CDataConnectionHierarchyReader;
import org.jkiss.dbeaver.ext.cdata.model.CDataConnectionUrl;
import org.jkiss.dbeaver.ext.cdata.registry.CDataDriverDescriptor;
import org.jkiss.dbeaver.ext.cdata.registry.CDataLicenseType;
import org.jkiss.dbeaver.ext.cdata.ui.internal.CDataUIMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriverConfigurationType;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.ApplicationPolicyProvider;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.IDialogPageProvider;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageWithAuth;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

public class CDataConnectionPage extends ConnectionPageWithAuth implements IDialogPageProvider {
    private static final Log log = Log.getLog(CDataConnectionPage.class);
    private static final String SUPPORT_URL = "https://portal.cdata.com/?a=support";
    private static final String PREFERENCE_PAGE_ID = "org.jkiss.dbeaver.ext.cdata.preferences";
    private Label statusLabel;
    private Button activateButton;
    private Button runButton;
    private boolean builderRunning;
    private final Runnable licenseChangeListener = () -> UIUtils.asyncExec(() -> {
        if (statusLabel != null && !statusLabel.isDisposed()) {
            refreshStatus();
        }
    });
    private Text urlText;
    private Composite generalFields;
    private Composite authenticationGroup;
    private Composite nativeAuthentication;
    private Label hierarchyStatus;
    private Link retryLink;
    private CDataConnectionEditor editor;
    private CDataConnectionHierarchy hierarchy;
    private DBPDataSourceContainer loadedDataSource;
    private boolean loadingSettings;
    private final Set<String> editedUrlProperties = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    private final Set<String> pendingUrlProperties = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    private boolean updatingUrl;

    @Override
    public void createControl(@NotNull Composite parent) {
        hierarchy = readConnectionHierarchy();
        Composite page = new Composite(parent, SWT.NONE);
        page.setLayout(new GridLayout(1, false));
        page.setLayoutData(new GridData(GridData.FILL_BOTH));
        Composite general = UIUtils.createTitledComposite(
            page, UIConnectionMessages.dialog_connection_server_label, 4, GridData.FILL_HORIZONTAL);
        createConnectionModeSwitcher(general, SelectionListener.widgetSelectedAdapter(event -> changeConnectionMode()));
        typeManualRadio.setText(CDataUIMessages.connection_editor_properties);
        UIUtils.createControlLabel(general, UIConnectionMessages.dialog_connection_url_label);
        urlText = new Text(general, SWT.BORDER);
        GridData urlData = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
        urlData.widthHint = 355;
        urlText.setLayoutData(urlData);
        urlText.addModifyListener(event -> {
            if (!loadingSettings && !updatingUrl) {
                propertiesChanged(null);
            }
        });
        generalFields = UIUtils.createFormPlaceholder(general, 4, 4);
        generalFields.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 4, 1));
        addControlToGroup(GROUP_CONNECTION, generalFields);
        hierarchyStatus = new Label(general, SWT.WRAP);
        GridData statusData = new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1);
        statusData.widthHint = 355;
        hierarchyStatus.setLayoutData(statusData);
        retryLink = new Link(general, SWT.NONE);
        retryLink.setText(CDataUIMessages.connection_editor_retry);
        retryLink.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1));
        retryLink.addListener(SWT.Selection, event -> reloadConnectionHierarchy());
        runButton = UIUtils.createDialogButton(general, CDataUIMessages.auth_native_url_builder_run,
            SelectionListener.widgetSelectedAdapter(event -> runNativeUrlBuilder()));
        runButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1));

        nativeAuthentication = UIUtils.createPlaceholder(page, 1);
        nativeAuthentication.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        createAuthPanel(nativeAuthentication, 1);
        authenticationGroup = new Composite(page, SWT.NONE);
        authenticationGroup.setLayout(new GridLayout(1, false));
        authenticationGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        Composite authenticationContent = UIUtils.createTitledComposite(
            authenticationGroup, UIConnectionMessages.dialog_connection_auth_group, 1, GridData.FILL_HORIZONTAL);
        Composite authenticationFields = UIUtils.createFormPlaceholder(authenticationContent, 2, 1);
        authenticationFields.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        savePasswordCheck = UIUtils.createCheckbox(authenticationContent,
            UIConnectionMessages.dialog_connection_wizard_final_checkbox_save_password, true);
        savePasswordCheck.addListener(SWT.Selection, event -> {
            editor.setPasswordEnabled(savePasswordCheck.getSelection());
            site.updateButtons();
        });
        editor = new CDataConnectionEditor(generalFields, authenticationFields, this::propertiesChanged);
        editor.setPasswordOptionsControl(savePasswordCheck);
        createLicenseGroup(page);
        createDriverPanel(page);
        setControl(page);
        CDataDriverDescriptor driver = getDriver();
        driver.addLicenseChangeListener(licenseChangeListener);
        page.addDisposeListener(event -> driver.removeLicenseChangeListener(licenseChangeListener));
        loadSettings();
    }

    @Override
    public void loadSettings() {
        loadingSettings = true;
        try {
            loadedDataSource = site.getActiveDataSource();
            editedUrlProperties.clear();
            pendingUrlProperties.clear();
            super.loadSettings();
            var configuration = loadedDataSource.getConnectionConfiguration();
            urlText.setText(configuration.getUrl() == null ? getDriver().getSampleURL() : configuration.getUrl());
            boolean canSavePassword = !ApplicationPolicyProvider.getInstance()
                .isPolicyEnabled(ApplicationPolicyProvider.POLICY_CREDENTIALS_EDIT);
            savePasswordCheck.setEnabled(canSavePassword);
            savePasswordCheck.setSelection(canSavePassword && loadedDataSource.isSavePassword());
            editor.setPasswordEnabled(savePasswordCheck.getSelection());
            if (hierarchy != null) {
                hierarchy.loadConfiguration(getDriver().getDriverInfo().jdbcName(), configuration);
                editor.setHierarchy(hierarchy);
            }
            showHierarchyStatus(hierarchy == null ? CDataUIMessages.connection_editor_fallback : "", hierarchy == null);
            selectUrlMode(hierarchy == null || configuration.getConfigurationType() == DBPDriverConfigurationType.URL);
            refreshStatus();
        } catch (DBException e) {
            hierarchy = null;
            editor.setHierarchy(null);
            super.loadSettings();
            showHierarchyStatus(CDataUIMessages.connection_editor_invalid_url, true);
            selectUrlMode(true);
        } finally {
            loadingSettings = false;
        }
        propertiesChanged(null);
    }

    @Override
    protected boolean isAuthEnabled() {
        return hierarchy == null || site.getActiveDataSource().isSharedCredentials();
    }

    @Override
    protected void saveConnectionURL(@NotNull DBPConnectionConfiguration configuration) {
        configuration.setUrl(urlText.getText().trim());
    }

    @Override
    public void saveSettings(@NotNull DBPDataSourceContainer dataSource) {
        super.saveSettings(dataSource);
        var configuration = dataSource.getConnectionConfiguration();
        configuration.setConfigurationType(isCustomURL() ? DBPDriverConfigurationType.URL : DBPDriverConfigurationType.MANUAL);
        if (hierarchy != null) {
            String userName = configuration.getUserName();
            String password = configuration.getUserPassword();
            if (isCustomURL()) {
                configuration.getProperties().keySet().removeIf(editedUrlProperties::contains);
                try {
                    hierarchy.saveUrlConfiguration(getDriver().getDriverInfo().jdbcName(), configuration);
                } catch (DBException e) {
                    setErrorMessage(CDataUIMessages.connection_editor_invalid_url);
                }
            } else {
                hierarchy.saveConfiguration(getDriver().getDriverInfo().jdbcName(), configuration);
            }
            if (dataSource.isSharedCredentials()) {
                configuration.setUserName(userName);
                configuration.setUserPassword(password);
            }
            configuration.setAuthModelId(CDataAuthModel.ID);
            if (!dataSource.isSavePassword()) {
                configuration.setUserPassword(null);
                CDataAuthModel.clearSecrets(configuration);
            }
        }
        Map<String, String> defaults = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (hierarchy != null) {
            hierarchy.getGeneralProperties().forEach(property -> defaults.put(property.name(), property.defaultValue()));
        }
        try {
            CDataConnectionUrl.updateEndpoint(getDriver().getDriverInfo().jdbcName(), configuration, defaults);
        } catch (DBException e) {
            configuration.setHostName(null);
            configuration.setHostPort(null);
        }
    }

    @Override
    public boolean isComplete() {
        return urlText != null && !urlText.getText().isBlank() && pendingUrlProperties.isEmpty() && getErrorMessage() == null
            && (isCustomURL() || editor.getValidationError() == null);
    }

    private void propertiesChanged(@Nullable Property property) {
        if (hierarchy == null || loadingSettings || updatingUrl) {
            site.updateButtons();
            return;
        }
        updatingUrl = true;
        try {
            if (isCustomURL()) {
                if (property != null) {
                    var names = CDataConnectionHierarchy.getPropertyNames(property);
                    editedUrlProperties.addAll(names);
                    pendingUrlProperties.addAll(names);
                }
                if (!pendingUrlProperties.isEmpty()) {
                    urlText.setText(hierarchy.updateUrl(
                        getDriver().getDriverInfo().jdbcName(), urlText.getText(), pendingUrlProperties));
                    pendingUrlProperties.clear();
                }
                CDataConnectionUrl.parse(urlText.getText(), getDriver().getDriverInfo().jdbcName());
                setErrorMessage(null);
            } else {
                pendingUrlProperties.clear();
                var configuration = new DBPConnectionConfiguration();
                hierarchy.saveConfiguration(getDriver().getDriverInfo().jdbcName(), configuration);
                urlText.setText(configuration.getUrl());
                setErrorMessage(editor.getValidationError());
            }
        } catch (DBException e) {
            setErrorMessage(CDataUIMessages.connection_editor_invalid_url);
        } finally {
            updatingUrl = false;
        }
        ((Composite) getControl()).layout(true, true);
        site.updateButtons();
    }

    private void changeConnectionMode() {
        if (!isCustomURL() && hierarchy != null) {
            try {
                hierarchy.loadUrl(getDriver().getDriverInfo().jdbcName(), urlText.getText());
                editor.setHierarchy(hierarchy);
            } catch (DBException e) {
                selectUrlMode(true);
                setErrorMessage(CDataUIMessages.connection_editor_invalid_url);
                site.updateButtons();
                return;
            }
        }
        updateConnectionMode();
        propertiesChanged(null);
    }

    private void selectUrlMode(boolean useUrl) {
        typeURLRadio.setSelection(useUrl);
        typeManualRadio.setSelection(!useUrl);
        updateConnectionMode();
    }

    private void updateConnectionMode() {
        setupConnectionModeSelection(urlText, isCustomURL(), GROUP_CONNECTION_ARR);
        typeManualRadio.setEnabled(hierarchy != null);
        boolean useAuthenticationFields = hierarchy != null && !site.getActiveDataSource().isSharedCredentials();
        UIUtils.setControlVisible(authenticationGroup, useAuthenticationFields);
        UIUtils.setControlVisible(nativeAuthentication, !useAuthenticationFields);
        if (getControl() instanceof Composite page) {
            page.layout(true, true);
        }
    }

    private void showHierarchyStatus(@NotNull String message, boolean retry) {
        hierarchyStatus.setText(message);
        UIUtils.setControlVisible(hierarchyStatus, !message.isEmpty());
        UIUtils.setControlVisible(retryLink, retry);
        UIUtils.setControlVisible(runButton, retry);
        hierarchyStatus.getParent().layout(true, true);
    }

    @Nullable
    private CDataConnectionHierarchy readConnectionHierarchy() {
        CDataDriverDescriptor driver = getDriver();
        AtomicReference<CDataConnectionHierarchy> loaded = new AtomicReference<>();
        try {
            UIUtils.runInProgressService(monitor -> {
                monitor.beginTask(CDataUIMessages.connection_editor_loading, 1);
                try {
                    CDataConnectionHierarchy result = CDataConnectionHierarchyReader.read(monitor, driver);
                    if (monitor.isCanceled()) {
                        throw new InterruptedException();
                    }
                    loaded.set(result);
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                } finally {
                    monitor.done();
                }
            });
        } catch (InvocationTargetException e) {
            log.debug("Unable to load CData connection hierarchy", e.getTargetException());
        } catch (InterruptedException expected) {
            return null;
        }
        return loaded.get();
    }

    private void reloadConnectionHierarchy() {
        saveSettings(site.getActiveDataSource());
        CDataConnectionHierarchy loaded = readConnectionHierarchy();
        if (urlText.isDisposed()) {
            return;
        }
        hierarchy = loaded;
        editor.setHierarchy(null);
        loadSettings();
    }

    @NotNull
    @Override
    public IDialogPage[] getDialogPages(boolean extrasOnly, boolean forceCreate) {
        return new IDialogPage[] {new CDataDriverPropertiesDialogPage(this, () -> hierarchy)};
    }

    private void createLicenseGroup(@NotNull Composite composite) {
        Composite licenseGroup = UIUtils.createTitledComposite(
            composite,
            CDataUIMessages.license_group_title,
            5,
            GridData.FILL_HORIZONTAL
        );
        statusLabel = new Label(licenseGroup, SWT.NONE);
        statusLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Button manageButton = new Button(licenseGroup, SWT.PUSH);
        manageButton.setText(CDataUIMessages.license_manage);
        manageButton.addListener(SWT.Selection, event -> manageLicenses());

        activateButton = new Button(licenseGroup, SWT.PUSH);
        activateButton.setLayoutData(new GridData());
        activateButton.setText(CDataUIMessages.license_activate);
        activateButton.addListener(SWT.Selection, event -> activatePurchasedLicense());

        Button buyButton = new Button(licenseGroup, SWT.PUSH);
        buyButton.setText(CDataUIMessages.activation_buy_button);
        buyButton.addListener(SWT.Selection, event -> UIUtils.openWebBrowser(getDriver().getDriverPurchaseURL()));

        Link supportLink = new Link(licenseGroup, SWT.NONE);
        supportLink.setText(CDataUIMessages.activation_support_link);
        supportLink.addListener(SWT.Selection, event -> UIUtils.openWebBrowser(SUPPORT_URL));
        refreshStatus();
    }

    private void manageLicenses() {
        Shell parent = getShell();
        PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(
            parent, PREFERENCE_PAGE_ID, new String[] {PREFERENCE_PAGE_ID}, null, PreferencesUtil.OPTION_NONE);
        if (dialog == null) {
            return;
        }
        Shell shell = dialog.getShell();
        Rectangle area = parent.getMonitor().getClientArea();
        Rectangle parentBounds = parent.getBounds();
        Rectangle bounds = shell.getBounds();
        bounds.width = Math.min(bounds.width, area.width);
        bounds.height = Math.min(bounds.height, area.height);
        bounds.x = Math.max(area.x, Math.min(parentBounds.x + (parentBounds.width - bounds.width) / 2,
            area.x + area.width - bounds.width));
        bounds.y = Math.max(area.y, Math.min(parentBounds.y + (parentBounds.height - bounds.height) / 2,
            area.y + area.height - bounds.height));
        shell.setBounds(bounds);
        dialog.open();
        refreshStatus();
    }

    private void runNativeUrlBuilder() {
        CDataDriverDescriptor driver = getDriver();
        builderRunning = true;
        setRunButtonEnabled(false);
        new AbstractJob(CDataUIMessages.auth_native_url_builder_job) {
            @NotNull
            @Override
            protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                try {
                    Path driverJar = driver.resolveDriver(monitor).jarPath().toAbsolutePath();
                    if (monitor.isCanceled()) {
                        UIUtils.asyncExec(() -> finishUrlBuilder(null));
                        return Status.CANCEL_STATUS;
                    }
                    new ProcessBuilder(GeneralUtils.findJavaExecutable(), "-jar", driverJar.toString())
                        .directory(driverJar.getParent().toFile())
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                        .redirectError(ProcessBuilder.Redirect.DISCARD)
                        .start()
                        .onExit()
                        .thenAccept(process -> UIUtils.asyncExec(() -> {
                            finishUrlBuilder(null);
                            if (!runButton.isDisposed() && process.exitValue() != 0) {
                                DBWorkbench.getPlatformUI().showError(
                                    CDataUIMessages.auth_native_url_builder_error,
                                    NLS.bind(CDataUIMessages.auth_native_url_builder_exit_code, process.exitValue())
                                );
                            }
                        }));
                    return Status.OK_STATUS;
                } catch (DBException | IOException e) {
                    UIUtils.asyncExec(() -> finishUrlBuilder(e));
                    return Status.error(CDataUIMessages.auth_native_url_builder_error, e);
                }
            }
        }.schedule();
    }

    private void finishUrlBuilder(@Nullable Exception error) {
        builderRunning = false;
        setRunButtonEnabled(true);
        if (!runButton.isDisposed()) {
            refreshStatus();
            if (error != null) {
                DBWorkbench.getPlatformUI().showError(
                    CDataUIMessages.auth_native_url_builder_error,
                    CDataUIMessages.auth_native_url_builder_error,
                    error
                );
            }
        }
    }

    private void setRunButtonEnabled(boolean enabled) {
        if (runButton != null && !runButton.isDisposed()) {
            runButton.setEnabled(enabled);
        }
    }

    private void activatePurchasedLicense() {
        CDataLicenseUIService uiService = DBWorkbench.getService(CDataLicenseUIService.class);
        if (uiService != null && uiService.activateLicense(getDriver(), CDataLicenseType.PURCHASED) != null) {
            refreshStatus();
        }
    }

    private void refreshStatus() {
        if (!(site.getDriver() instanceof CDataDriverDescriptor driver)) {
            return;
        }
        statusLabel.setText(NLS.bind(
            CDataUIMessages.license_status,
            CDataLicenseUIUtils.getStatusText(driver.getLicenseStatus())
        ));
        UIUtils.setControlVisible(activateButton, driver.getLicenseStatus().isTrial());
        statusLabel.getParent().layout(true, true);
    }

    @NotNull
    private CDataDriverDescriptor getDriver() {
        return (CDataDriverDescriptor) site.getDriver();
    }
}
