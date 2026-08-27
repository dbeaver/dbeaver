/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.tracking;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.tracking.auth.DDBrowserLogin;
import org.jkiss.dbeaver.model.tracking.auth.DDBundleCredentials;
import org.jkiss.dbeaver.model.tracking.auth.DDCryptoState;
import org.jkiss.dbeaver.model.tracking.auth.DDKeyBundle;
import org.jkiss.dbeaver.model.tracking.auth.DDKeyStore;
import org.jkiss.dbeaver.model.tracking.sync.DDLocalSyncConflictException;
import org.jkiss.dbeaver.model.tracking.sync.DDSyncBinding;
import org.jkiss.dbeaver.model.tracking.sync.DDSyncConflict;
import org.jkiss.dbeaver.model.tracking.sync.DDSyncResult;
import org.jkiss.dbeaver.model.tracking.sync.DDSyncService;
import org.jkiss.dbeaver.model.tracking.sync.core.DDConfigurationNotFoundException;
import org.jkiss.dbeaver.model.tracking.sync.core.DDConfigurationSummary;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;
import org.jkiss.dbeaver.ui.tracking.internal.DDTrackingUIMessages;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class DDSyncPreferencePage extends AbstractPrefPage implements IWorkbenchPreferencePage {

    private static final Log log = Log.getLog(DDSyncPreferencePage.class);


    private static final String SYNC_TITLE = DDTrackingUIMessages.sync_preference_page_title;
    private static final String ENV_URL = "DATADAM_URL";
    private static final String PREF_SERVER_URL = "datadam.server-url";
    private static final int GATEWAY_PORT = 9000;
    private static final int ACCOUNT_PORT = 9001;

    private Text accountText;
    private Text urlText;
    private Text configurationText;
    private Button loginButton;
    private Button deleteButton;
    private Button uploadButton;
    private Button downloadButton;
    private Button downloadOptionsButton;
    private Button autoSyncButton;
    private Label conflictsLabel;
    private Button takeRemoteButton;
    private Button keepLocalButton;

    private String savedUrl = "";
    private List<DDSyncConflict> conflicts = List.of();
    private long conflictRefreshId;

    @Override
    public void init(@NotNull IWorkbench workbench) {
        //empty
    }

    @NotNull
    @Override
    protected Control createContents(@NotNull Composite parent) {
        Control contents = super.createContents(parent);
        updateApplyState();
        return contents;
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite composite = UIUtils.createPlaceholder(parent, 1);
        Composite group = UIUtils.createTitledComposite(
            composite,
            DDTrackingUIMessages.sync_preference_page_access_key_group,
            2,
            GridData.FILL_HORIZONTAL,
            SWT.DEFAULT);
        UIUtils.createControlLabel(group, DDTrackingUIMessages.sync_preference_page_server_url_label);
        Composite urlPanel = UIUtils.createComposite(group, 2);
        urlPanel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        urlText = new Text(urlPanel, SWT.BORDER);
        urlText.setLayoutData(idFieldLayout());
        urlText.addModifyListener(e -> updateApplyState());
        UIUtils.createPushButton(
            urlPanel,
            DDTrackingUIMessages.sync_preference_page_default_button,
            null,
            SelectionListener.widgetSelectedAdapter(e -> {
                urlText.setText(CommonUtils.notEmpty(System.getenv(ENV_URL)));
                updateApplyState();
            }));

        accountText = UIUtils.createLabelText(
            group, DDTrackingUIMessages.sync_preference_page_account_label, "", SWT.READ_ONLY, idFieldLayout());

        Composite buttons = UIUtils.createComposite(group, 2);
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.horizontalSpan = 2;
        buttons.setLayoutData(gd);
        loginButton = UIUtils.createPushButton(
            buttons, DDTrackingUIMessages.sync_preference_page_log_in_button, null,
            SelectionListener.widgetSelectedAdapter(e -> logIn()));
        deleteButton = UIUtils.createPushButton(
            buttons, DDTrackingUIMessages.sync_preference_page_log_out_button, null,
            SelectionListener.widgetSelectedAdapter(e -> {
                if (UIUtils.confirmAction(
                    getShell(),
                    DDTrackingUIMessages.sync_preference_page_log_out_confirm_title,
                    DDTrackingUIMessages.sync_preference_page_log_out_confirm_message)
                ) {
                    logOut();
                }
            }));

        Composite syncGroup = UIUtils.createTitledComposite(
            composite,
            DDTrackingUIMessages.sync_preference_page_configuration_group,
            2,
            GridData.FILL_HORIZONTAL,
            SWT.DEFAULT);
        configurationText = UIUtils.createLabelText(
            syncGroup, DDTrackingUIMessages.sync_preference_page_bound_to_label, "", SWT.READ_ONLY, idFieldLayout());

        Composite syncButtons = UIUtils.createComposite(syncGroup, 3);
        GridData syncGd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        syncGd.horizontalSpan = 2;
        syncButtons.setLayoutData(syncGd);
        uploadButton = UIUtils.createPushButton(
            syncButtons, DDTrackingUIMessages.sync_preference_page_upload_button, null,
            SelectionListener.widgetSelectedAdapter(e -> upload()));
        downloadButton = UIUtils.createPushButton(
            syncButtons, DDTrackingUIMessages.sync_preference_page_download_button, null,
            SelectionListener.widgetSelectedAdapter(e -> download()));
        downloadOptionsButton = UIUtils.createPushButton(
            syncButtons, DDTrackingUIMessages.sync_preference_page_download_options_button, null,
            SelectionListener.widgetSelectedAdapter(e -> downloadOptions()));

        autoSyncButton = UIUtils.createCheckbox(
            syncGroup, DDTrackingUIMessages.sync_preference_page_auto_sync_checkbox, DDAutoSyncCoordinator.isEnabled());
        GridData autoSyncGd = new GridData();
        autoSyncGd.horizontalSpan = 2;
        autoSyncButton.setLayoutData(autoSyncGd);
        autoSyncButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(
            e -> DDAutoSyncCoordinator.setEnabled(autoSyncButton.getSelection())));

        conflictsLabel = UIUtils.createLabel(syncGroup, "");
        GridData conflictsGd = new GridData(GridData.FILL_HORIZONTAL);
        conflictsGd.horizontalSpan = 2;
        conflictsLabel.setLayoutData(conflictsGd);

        Composite conflictButtons = UIUtils.createComposite(syncGroup, 2);
        GridData conflictButtonsGd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        conflictButtonsGd.horizontalSpan = 2;
        conflictButtons.setLayoutData(conflictButtonsGd);
        takeRemoteButton = UIUtils.createPushButton(
            conflictButtons, DDTrackingUIMessages.sync_preference_page_take_remote_button, null,
            SelectionListener.widgetSelectedAdapter(e -> resolveConflicts(true)));
        keepLocalButton = UIUtils.createPushButton(
            conflictButtons, DDTrackingUIMessages.sync_preference_page_keep_local_button, null,
            SelectionListener.widgetSelectedAdapter(e -> resolveConflicts(false)));

        refresh();
        return composite;
    }

    private void upload() {
        DDSyncService service = createSyncService();
        if (service == null) {
            return;
        }
        try {
            DDSyncResult result;
            if (service.getBinding() == null) {
                result = createNewConfiguration(service);
                if (result == null) {
                    return;
                }
            } else {
                try {
                    result = runInProgress(service::upload);
                } catch (DDConfigurationNotFoundException e) {
                    result = createNewConfiguration(service);
                    if (result == null) {
                        return;
                    }
                }
            }
            refresh();
            showChanged(
                DDTrackingUIMessages.sync_preference_page_nothing_to_upload,
                DDTrackingUIMessages.sync_preference_page_uploaded_label,
                result);
        } catch (DDLocalSyncConflictException e) {
            refresh();
            DBWorkbench.getPlatformUI().showMessageBox(
                SYNC_TITLE,
                DDTrackingUIMessages.sync_preference_page_upload_conflict + ": " + e.getMessage(),
                true);
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError(SYNC_TITLE, DDTrackingUIMessages.sync_preference_page_upload_failed, e);
        }
    }

    @Nullable
    private DDSyncResult createNewConfiguration(@NotNull DDSyncService service) throws DBException {
        DDCreateConfigurationDialog dialog = new DDCreateConfigurationDialog(getShell(), service.getAvailableParts());
        if (dialog.open() != Window.OK) {
            return null;
        }
        return runInProgress(() -> service.createConfiguration(dialog.getName(), dialog.getSelectedKeys()));
    }

    private void download() {
        DDSyncService service = createSyncService();
        if (service == null) {
            return;
        }
        if (service.getBinding() == null) {
            DBWorkbench.getPlatformUI().showMessageBox(
                SYNC_TITLE, DDTrackingUIMessages.sync_preference_page_nothing_bound, true);
            return;
        }
        try {
            DDSyncResult result = runInProgress(service::download);
            refresh();
            showChanged(
                DDTrackingUIMessages.sync_preference_page_nothing_to_download,
                DDTrackingUIMessages.sync_preference_page_downloaded_label,
                result);
        } catch (DDLocalSyncConflictException e) {
            refresh();
            DBWorkbench.getPlatformUI().showMessageBox(
                SYNC_TITLE,
                DDTrackingUIMessages.sync_preference_page_download_conflict + ": " + e.getMessage(),
                true);
        } catch (DDConfigurationNotFoundException e) {
            DBWorkbench.getPlatformUI().showMessageBox(
                SYNC_TITLE, DDTrackingUIMessages.sync_preference_page_configuration_not_found, true);
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError(SYNC_TITLE, DDTrackingUIMessages.sync_preference_page_download_failed, e);
        }
    }

    private void downloadOptions() {
        DDSyncService service = createSyncService();
        if (service == null) {
            return;
        }
        try {
            DDConfigurationSummary selected = askConfiguration(runInProgress(service::listConfigurations));
            if (selected == null) {
                return;
            }
            DDSyncResult result = runInProgress(() -> service.downloadAndBind(selected.configurationId()));
            refresh();
            showChanged(
                DDTrackingUIMessages.sync_preference_page_nothing_to_download,
                DDTrackingUIMessages.sync_preference_page_downloaded_label,
                result);
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError(SYNC_TITLE, DDTrackingUIMessages.sync_preference_page_download_failed, e);
        }
    }

    private void resolveConflicts(boolean takeRemote) {
        DDSyncService service = createSyncService();
        if (service == null) {
            return;
        }
        List<String> resolved = new ArrayList<>();
        try {
            runInProgress(() -> {
                for (DDSyncConflict conflict : conflicts) {
                    if (takeRemote) {
                        service.forceDownload(conflict.key());
                    } else {
                        service.forceUpload(conflict.key());
                    }
                    resolved.add(conflict.name());
                }
            });
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError(SYNC_TITLE, DDTrackingUIMessages.sync_preference_page_conflict_resolve_failed, e);
        } finally {
            refresh();
        }
        if (!resolved.isEmpty()) {
            DBWorkbench.getPlatformUI().showMessageBox(
                SYNC_TITLE,
                DDTrackingUIMessages.sync_preference_page_conflict_resolved_label + ": " + String.join(", ", resolved),
                false);
        }
    }

    @NotNull
    private <T> T runInProgress(@NotNull DBSupplier<T> supplier) throws DBException {
        AtomicReference<T> holder = new AtomicReference<>();
        try {
            UIUtils.runInProgressDialog(monitor -> {
                try {
                    holder.set(supplier.get());
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (InvocationTargetException e) {
            throw unwrap(e);
        }
        return holder.get();
    }

    private void runInProgress(@NotNull DBRunnable runnable) throws DBException {
        try {
            UIUtils.runInProgressDialog(monitor -> {
                try {
                    runnable.run();
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (InvocationTargetException e) {
            throw unwrap(e);
        }
    }

    @NotNull
    private static DBException unwrap(@NotNull InvocationTargetException e) {
        Throwable target = e.getTargetException();
        return target instanceof DBException dbException
            ? dbException
            : new DBException(String.valueOf(target.getMessage()), target);
    }

    @FunctionalInterface
    private interface DBSupplier<T> {
        T get() throws DBException;
    }

    @FunctionalInterface
    private interface DBRunnable {
        void run() throws DBException;
    }

    private void showChanged(@NotNull String emptyMessage, @NotNull String label, @NotNull DDSyncResult result) {
        DBWorkbench.getPlatformUI().showMessageBox(
            SYNC_TITLE,
            result.changedParts().isEmpty() ? emptyMessage : label + ": " + String.join(", ", result.changedParts()),
            false);
    }

    @Nullable
    private DDConfigurationSummary askConfiguration(@NotNull List<DDConfigurationSummary> configurations) {
        if (configurations.isEmpty()) {
            DBWorkbench.getPlatformUI().showMessageBox(
                SYNC_TITLE, DDTrackingUIMessages.sync_preference_page_no_configurations_found, true);
            return null;
        }
        ElementListSelectionDialog dialog = new ElementListSelectionDialog(getShell(), new LabelProvider() {
            @NotNull
            @Override
            public String getText(@NotNull Object element) {
                return ((DDConfigurationSummary) element).name();
            }
        });
        dialog.setTitle(SYNC_TITLE);
        dialog.setMessage(DDTrackingUIMessages.sync_preference_page_select_configuration);
        dialog.setElements(configurations.toArray());
        if (dialog.open() != Window.OK) {
            return null;
        }
        return (DDConfigurationSummary) dialog.getFirstResult();
    }

    @Nullable
    private DDSyncService createSyncService() {
        DDKeyBundle bundle = DDKeyStore.load();
        if (bundle == null) {
            DBWorkbench.getPlatformUI().showMessageBox(SYNC_TITLE, DDTrackingUIMessages.sync_preference_page_log_in_first, true);
            return null;
        }
        String url = getGatewayUrl();
        if (CommonUtils.isEmpty(url)) {
            DBWorkbench.getPlatformUI().showMessageBox(
                SYNC_TITLE, DDTrackingUIMessages.sync_preference_page_url_not_configured, true);
            return null;
        }
        return new DDSyncService(
            url,
            new DDBundleCredentials(bundle),
            DBWorkbench.getPlatform().getWorkspace(),
            bundle.accountId());
    }

    @NotNull
    private static GridData idFieldLayout() {
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.widthHint = UIUtils.getFontHeight(Display.getCurrent().getSystemFont()) * 22;
        return gd;
    }

    @NotNull
    public static String getServerUrl() {
        String url = DBWorkbench.getPlatform().getPreferenceStore().getString(PREF_SERVER_URL);
        return CommonUtils.isEmpty(url) ? CommonUtils.notEmpty(System.getenv(ENV_URL)) : url;
    }

    @NotNull
    public static String getGatewayUrl() {
        return withPort(getServerUrl(), GATEWAY_PORT);
    }

    @NotNull
    public static String getAccountUrl() {
        return withPort(getServerUrl(), ACCOUNT_PORT);
    }

    @NotNull
    private static String withPort(@NotNull String url, int port) {
        if (CommonUtils.isEmpty(url)) {
            return url;
        }
        String normalized = CommonUtils.removeTrailingSlash(url);
        try {
            java.net.URI uri = java.net.URI.create(normalized);
            if (uri.getHost() != null) {
                if (uri.getPort() != -1) {
                    return normalized;
                }
                return new java.net.URI(
                    uri.getScheme(),
                    uri.getUserInfo(),
                    uri.getHost(),
                    port,
                    uri.getPath(),
                    uri.getQuery(),
                    uri.getFragment()
                ).toString();
            }
        } catch (IllegalArgumentException | URISyntaxException e) {
            // ignore and fall back
        }
        return normalized + ":" + port;
    }

    private void updateApplyState() {
        Button applyButton = getApplyButton();
        if (applyButton != null && !applyButton.isDisposed()) {
            applyButton.setEnabled(!savedUrl.equals(urlText.getText().trim()));
        }
    }

    @Override
    protected void performApply() {
        String url = urlText.getText().trim();
        DBWorkbench.getPlatform().getPreferenceStore().setValue(PREF_SERVER_URL, url);
        savedUrl = url;
        updateApplyState();
    }

    @Override
    public boolean performOk() {
        performApply();
        return super.performOk();
    }

    private void logIn() {
        String siteUrl = getAccountUrl();
        if (CommonUtils.isEmpty(siteUrl)) {
            DBWorkbench.getPlatformUI().showMessageBox(
                SYNC_TITLE, DDTrackingUIMessages.sync_preference_page_url_not_configured, true);
            return;
        }
        DDCryptoState[] result = new DDCryptoState[1];
        try {
            UIUtils.runInProgressDialog(monitor -> {
                try {
                    result[0] = new DDBrowserLogin(siteUrl).login();
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError(
                SYNC_TITLE, DDTrackingUIMessages.sync_preference_page_login_failed, e.getTargetException());
            return;
        }

        DDCryptoState state = result[0];
        if (!state.cryptoConfigured()) {
            DBWorkbench.getPlatformUI().showMessageBox(
                SYNC_TITLE,
                DDTrackingUIMessages.sync_preference_page_encryption_not_configured,
                true);
            return;
        }
        DDImportKeyDialog dialog = new DDImportKeyDialog(getShell());
        if (dialog.open() != Window.OK) {
            return;
        }
        DDKeyBundle[] keyBundle = new DDKeyBundle[1];
        try {
            UIUtils.runInProgressDialog(monitor -> {
                try {
                    keyBundle[0] = DDKeyStore.unpack(state, dialog.getPhrase());
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            });
            DDKeyStore.save(keyBundle[0]);
            DDTrackingInitializer.start();
            refresh();
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError(SYNC_TITLE, DDTrackingUIMessages.sync_preference_page_login_failed, e);
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError(
                SYNC_TITLE, DDTrackingUIMessages.sync_preference_page_login_failed, e.getTargetException());
        }
    }

    private void logOut() {
        try {
            DDTrackingInitializer.stop();
            DDKeyStore.clear();
            refresh();
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError(SYNC_TITLE, DDTrackingUIMessages.sync_preference_page_cannot_forget_keys, e);
        }
    }

    private void refresh() {
        DDKeyBundle bundle = DDKeyStore.load();
        boolean present = bundle != null;
        accountText.setText(present ? bundle.accountId() : "");
        loginButton.setEnabled(!present);
        deleteButton.setEnabled(present);

        DDSyncBinding binding = DDSyncService.readBinding(
            DBWorkbench.getPlatform().getWorkspace().getAbsolutePath());
        boolean boundToCurrentAccount = binding != null && present && bundle.accountId().equals(binding.accountId());
        configurationText.setText(!boundToCurrentAccount
            ? ""
            : CommonUtils.isEmpty(binding.name()) ? binding.configurationId() : binding.name());
        uploadButton.setEnabled(present);
        downloadButton.setEnabled(present && boundToCurrentAccount);
        downloadOptionsButton.setEnabled(present);
        autoSyncButton.setSelection(DDAutoSyncCoordinator.isEnabled());

        savedUrl = DBWorkbench.getPlatform().getPreferenceStore().getString(PREF_SERVER_URL);
        if (savedUrl == null) {
            savedUrl = "";
        }
        urlText.setText(CommonUtils.isEmpty(savedUrl)
            ? CommonUtils.notEmpty(System.getenv(ENV_URL))
            : savedUrl);

        refreshConflicts(present && boundToCurrentAccount);
    }

    private void refreshConflicts(boolean boundToCurrentAccount) {
        long refreshId = ++conflictRefreshId;
        renderConflicts(List.of());
        DDSyncService service = boundToCurrentAccount ? createSyncServiceSilently() : null;
        if (service == null) {
            return;
        }
        AbstractJob job = new AbstractJob("DataDam conflict check") {
            @NotNull
            @Override
            protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                try {
                    List<DDSyncConflict> found = service.getConflicts();
                    UIUtils.asyncExec(() -> {
                        if (refreshId == conflictRefreshId && !conflictsLabel.isDisposed()) {
                            renderConflicts(found);
                        }
                    });
                } catch (DBException e) {
                    log.debug("Error checking synchronization conflicts", e);
                }
                return Status.OK_STATUS;
            }
        };
        job.setSystem(true);
        job.schedule();
    }

    private void renderConflicts(@NotNull List<DDSyncConflict> found) {
        conflicts = found;
        boolean hasConflicts = !conflicts.isEmpty();
        conflictsLabel.setText(hasConflicts
            ? DDTrackingUIMessages.sync_preference_page_conflicts_label + ": "
                + conflicts.stream().map(DDSyncConflict::name).collect(Collectors.joining(", "))
            : "");
        takeRemoteButton.setEnabled(hasConflicts);
        keepLocalButton.setEnabled(hasConflicts);
    }

    @Nullable
    private static DDSyncService createSyncServiceSilently() {
        DDKeyBundle bundle = DDKeyStore.load();
        if (bundle == null) {
            return null;
        }
        String url = getGatewayUrl();
        if (CommonUtils.isEmpty(url)) {
            return null;
        }
        return new DDSyncService(
            url,
            new DDBundleCredentials(bundle),
            DBWorkbench.getPlatform().getWorkspace(),
            bundle.accountId());
    }

}
