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
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.dbeaver.model.tracking.auth.DDBrowserLogin;
import org.jkiss.dbeaver.model.tracking.auth.DDBundleCredentials;
import org.jkiss.dbeaver.model.tracking.auth.DDCryptoState;
import org.jkiss.dbeaver.model.tracking.auth.DDKeyBundle;
import org.jkiss.dbeaver.model.tracking.auth.DDKeyStore;
import org.jkiss.dbeaver.model.tracking.sync.DDLocalSyncConflictException;
import org.jkiss.dbeaver.model.tracking.sync.DDSyncBinding;
import org.jkiss.dbeaver.model.tracking.sync.DDSyncResult;
import org.jkiss.dbeaver.model.tracking.sync.DDSyncService;
import org.jkiss.dbeaver.model.tracking.sync.core.DDConfigurationConflictException;
import org.jkiss.dbeaver.model.tracking.sync.core.DDConfigurationNotFoundException;
import org.jkiss.dbeaver.model.tracking.sync.core.DDConfigurationSummary;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class DDSyncPreferencePage extends AbstractPrefPage implements IWorkbenchPreferencePage {

    private static final Log log = Log.getLog(DDSyncPreferencePage.class);


    private static final String SYNC_TITLE = "Synchronization";
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

    private String savedUrl = "";

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
            "Access key",
            2,
            GridData.FILL_HORIZONTAL,
            SWT.DEFAULT);
        UIUtils.createControlLabel(group, "Server URL");
        Composite urlPanel = UIUtils.createComposite(group, 2);
        urlPanel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        urlText = new Text(urlPanel, SWT.BORDER);
        urlText.setLayoutData(idFieldLayout());
        urlText.addModifyListener(e -> updateApplyState());
        UIUtils.createPushButton(urlPanel, "Default", null, SelectionListener.widgetSelectedAdapter(e -> {
            urlText.setText(CommonUtils.notEmpty(System.getenv(ENV_URL)));
            updateApplyState();
        }));

        accountText = UIUtils.createLabelText(group, "Account", "", SWT.READ_ONLY, idFieldLayout());

        Composite buttons = UIUtils.createComposite(group, 2);
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.horizontalSpan = 2;
        buttons.setLayoutData(gd);
        loginButton = UIUtils.createPushButton(buttons, "Log In...", null, SelectionListener.widgetSelectedAdapter(e -> logIn()));
        deleteButton = UIUtils.createPushButton(buttons, "Log Out", null, SelectionListener.widgetSelectedAdapter(e -> {
            if (UIUtils.confirmAction(getShell(), "Log out", "Forget the keys stored on this computer?")) {
                logOut();
            }
        }));

        Composite syncGroup = UIUtils.createTitledComposite(
            composite,
            "Configuration",
            2,
            GridData.FILL_HORIZONTAL,
            SWT.DEFAULT);
        configurationText = UIUtils.createLabelText(syncGroup, "Bound to", "", SWT.READ_ONLY, idFieldLayout());

        Composite syncButtons = UIUtils.createComposite(syncGroup, 3);
        GridData syncGd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        syncGd.horizontalSpan = 2;
        syncButtons.setLayoutData(syncGd);
        uploadButton = UIUtils.createPushButton(
            syncButtons, "Upload", null, SelectionListener.widgetSelectedAdapter(e -> upload()));
        downloadButton = UIUtils.createPushButton(
            syncButtons, "Download", null, SelectionListener.widgetSelectedAdapter(e -> download()));
        downloadOptionsButton = UIUtils.createPushButton(
            syncButtons, "Download...", null, SelectionListener.widgetSelectedAdapter(e -> downloadOptions()));

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
                    result = service.upload();
                } catch (DDConfigurationNotFoundException e) {
                    result = createNewConfiguration(service);
                    if (result == null) {
                        return;
                    }
                }
            }
            refresh();
            showChanged("Nothing to upload", "Uploaded: ", result);
        } catch (DDLocalSyncConflictException e) {
            DBWorkbench.getPlatformUI().showMessageBox(
                SYNC_TITLE, "Upload was refused, local changes conflict with the server: " + e.getMessage(), true);
        } catch (DDConfigurationConflictException e) {
            DBWorkbench.getPlatformUI().showMessageBox(SYNC_TITLE, "Upload was rejected: " + e.getMessage(), true);
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError(SYNC_TITLE, "Upload failed", e);
        }
    }

    @Nullable
    private DDSyncResult createNewConfiguration(@NotNull DDSyncService service) throws DBException {
        DDCreateConfigurationDialog dialog = new DDCreateConfigurationDialog(getShell(), service.getAvailableParts());
        if (dialog.open() != Window.OK) {
            return null;
        }
        return service.createConfiguration(dialog.getName(), dialog.getSelectedKeys());
    }

    private void download() {
        DDSyncService service = createSyncService();
        if (service == null) {
            return;
        }
        if (service.getBinding() == null) {
            DBWorkbench.getPlatformUI().showMessageBox(
                SYNC_TITLE, "Nothing is bound yet. Use Download... to pick a configuration.", true);
            return;
        }
        try {
            DDSyncResult result = service.download();
            refresh();
            showChanged("Nothing to download", "Downloaded: ", result);
        } catch (DDLocalSyncConflictException e) {
            DBWorkbench.getPlatformUI().showMessageBox(
                SYNC_TITLE, "Download was refused, local changes conflict with the server: " + e.getMessage(), true);
        } catch (DDConfigurationNotFoundException e) {
            DBWorkbench.getPlatformUI().showMessageBox(SYNC_TITLE, "This configuration no longer exists", true);
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError(SYNC_TITLE, "Download failed", e);
        }
    }

    private void downloadOptions() {
        DDSyncService service = createSyncService();
        if (service == null) {
            return;
        }
        try {
            DDConfigurationSummary selected = askConfiguration(service.listConfigurations());
            if (selected == null) {
                return;
            }
            DDSyncResult result = service.downloadAndBind(selected.configurationId());
            refresh();
            showChanged("Nothing to download", "Downloaded: ", result);
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError(SYNC_TITLE, "Download failed", e);
        }
    }

    private void showChanged(@NotNull String emptyMessage, @NotNull String prefix, @NotNull DDSyncResult result) {
        DBWorkbench.getPlatformUI().showMessageBox(
            SYNC_TITLE,
            result.changedParts().isEmpty() ? emptyMessage : prefix + String.join(", ", result.changedParts()),
            false);
    }

    @Nullable
    private DDConfigurationSummary askConfiguration(@NotNull List<DDConfigurationSummary> configurations) {
        if (configurations.isEmpty()) {
            DBWorkbench.getPlatformUI().showMessageBox(SYNC_TITLE, "No account configurations found", true);
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
        dialog.setMessage("Select configuration");
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
            DBWorkbench.getPlatformUI().showMessageBox(SYNC_TITLE, "Log in first", true);
            return null;
        }
        String url = getGatewayUrl();
        if (CommonUtils.isEmpty(url)) {
            DBWorkbench.getPlatformUI().showMessageBox(SYNC_TITLE, "DataDam URL is not configured", true);
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
        return CommonUtils.removeTrailingSlash(url) + ":" + port;
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
            DBWorkbench.getPlatformUI().showMessageBox(SYNC_TITLE, "DataDam URL is not configured", true);
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
            DBWorkbench.getPlatformUI().showError(SYNC_TITLE, "Login failed", e.getTargetException());
            return;
        }

        DDCryptoState state = result[0];
        if (!state.cryptoConfigured()) {
            DBWorkbench.getPlatformUI().showMessageBox(
                SYNC_TITLE,
                "Encryption is not configured for this account. Set it up in the web browser first.",
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
            DBWorkbench.getPlatformUI().showError(SYNC_TITLE, "Login failed", e);
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError(SYNC_TITLE, "Login failed", e.getTargetException());
        }
    }

    private void logOut() {
        try {
            DDTrackingInitializer.stop();
            DDKeyStore.clear();
            refresh();
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError(SYNC_TITLE, "Cannot forget the keys", e);
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

        savedUrl = DBWorkbench.getPlatform().getPreferenceStore().getString(PREF_SERVER_URL);
        if (savedUrl == null) {
            savedUrl = "";
        }
        urlText.setText(CommonUtils.isEmpty(savedUrl)
            ? CommonUtils.notEmpty(System.getenv(ENV_URL))
            : savedUrl);
    }

}
