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
import org.jkiss.dbeaver.model.tracking.sync.DDSyncBinding;
import org.jkiss.dbeaver.model.tracking.sync.DDSyncService;
import org.jkiss.dbeaver.model.tracking.sync.core.DDContainer;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;
import org.jkiss.utils.CommonUtils;

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
    private Text workspaceText;
    private Button deleteButton;
    private Button uploadButton;
    private Button downloadButton;

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
        UIUtils.createPushButton(buttons, "Log In...", null, SelectionListener.widgetSelectedAdapter(e -> logIn()));
        deleteButton = UIUtils.createPushButton(buttons, "Log Out", null, SelectionListener.widgetSelectedAdapter(e -> {
            if (UIUtils.confirmAction(getShell(), "Log out", "Forget the keys stored on this computer?")) {
                logOut();
            }
        }));

        Composite syncGroup = UIUtils.createTitledComposite(
            composite,
            "Workspace",
            2,
            GridData.FILL_HORIZONTAL,
            SWT.DEFAULT);
        workspaceText = UIUtils.createLabelText(syncGroup, "Bound to", "", SWT.READ_ONLY, idFieldLayout());

        Composite syncButtons = UIUtils.createComposite(syncGroup, 2);
        GridData syncGd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        syncGd.horizontalSpan = 2;
        syncButtons.setLayoutData(syncGd);
        uploadButton = UIUtils.createPushButton(
            syncButtons, "Upload", null, SelectionListener.widgetSelectedAdapter(e -> upload()));
        downloadButton = UIUtils.createPushButton(
            syncButtons, "Download", null, SelectionListener.widgetSelectedAdapter(e -> download()));

        refresh();
        return composite;
    }

    private void upload() {
        DDSyncService service = createSyncService();
        if (service == null) {
            return;
        }
        try {
            DDSyncBinding binding = service.getBinding();
            String containerId;
            if (binding == null) {
                String label = askContainerLabel();
                if (label == null) {
                    return;
                }
                containerId = service.createContainer(label);
            } else {
                containerId = binding.containerId();
            }
            List<String> uploaded = service.upload(containerId);
            refresh();
            DBWorkbench.getPlatformUI().showMessageBox(
                SYNC_TITLE,
                uploaded.isEmpty() ? "Nothing to upload" : "Uploaded: " + String.join(", ", uploaded),
                false);
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError(SYNC_TITLE, "Upload failed", e);
        }
    }

    private void download() {
        DDSyncService service = createSyncService();
        if (service == null) {
            return;
        }
        try {
            DDSyncBinding binding = service.getBinding();
            if (binding == null) {
                DDContainer selected = askContainer(service.listContainers());
                if (selected == null) {
                    return;
                }
                service.bind(selected.id(), selected.label());
                binding = service.getBinding();
            }
            String containerId = binding.containerId();
            List<String> restored = service.download(containerId);
            refresh();
            DBWorkbench.getPlatformUI().showMessageBox(
                SYNC_TITLE,
                restored.isEmpty()
                    ? "Nothing to download"
                    : "Downloaded: " + String.join(", ", restored),
                false);
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError(SYNC_TITLE, "Download failed", e);
        }
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
            DBWorkbench.getPlatform().getWorkspace());
    }

    @NotNull
    private static GridData idFieldLayout() {
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.widthHint = UIUtils.getFontHeight(Display.getCurrent().getSystemFont()) * 22;
        return gd;
    }

    @Nullable
    private String askContainerLabel() {
        String label = EnterNameDialog.chooseName(getShell(), "Workspace name", "");
        return CommonUtils.isEmpty(label) ? null : label;
    }

    @Nullable
    private DDContainer askContainer(@NotNull List<DDContainer> containers) {
        if (containers.isEmpty()) {
            DBWorkbench.getPlatformUI().showMessageBox(SYNC_TITLE, "No synchronized workspaces found", true);
            return null;
        }
        ElementListSelectionDialog dialog = new ElementListSelectionDialog(getShell(), new LabelProvider() {
            @NotNull
            @Override
            public String getText(@NotNull Object element) {
                return ((DDContainer) element).label();
            }
        });
        dialog.setTitle(SYNC_TITLE);
        dialog.setMessage("Select workspace");
        dialog.setElements(containers.toArray());
        if (dialog.open() != Window.OK) {
            return null;
        }
        return (DDContainer) dialog.getFirstResult();
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
        try {
            DDCryptoState state = new DDBrowserLogin(siteUrl).login();
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
            DDKeyStore.save(DDKeyStore.unpack(state, dialog.getKey()));
            refresh();
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError(SYNC_TITLE, "Login failed", e);
        }
    }

    private void logOut() {
        try {
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
        deleteButton.setEnabled(present);

        DDSyncBinding binding = DDSyncService.readBinding(
            DBWorkbench.getPlatform().getWorkspace().getAbsolutePath());
        workspaceText.setText(binding == null
            ? ""
            : CommonUtils.isEmpty(binding.label()) ? binding.containerId() : binding.label());
        uploadButton.setEnabled(present);
        downloadButton.setEnabled(present);

        savedUrl = DBWorkbench.getPlatform().getPreferenceStore().getString(PREF_SERVER_URL);
        if (savedUrl == null) {
            savedUrl = "";
        }
        urlText.setText(CommonUtils.isEmpty(savedUrl)
            ? CommonUtils.notEmpty(System.getenv(ENV_URL))
            : savedUrl);
    }

}
