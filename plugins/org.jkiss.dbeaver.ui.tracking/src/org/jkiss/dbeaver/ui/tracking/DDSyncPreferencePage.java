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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.dbeaver.model.tracking.DDAccessKey;
import org.jkiss.dbeaver.model.tracking.DDSyncService;
import org.jkiss.dbeaver.model.tracking.DDWorkspace;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;
import org.jkiss.utils.CommonUtils;

import java.util.List;

public class DDSyncPreferencePage extends AbstractPrefPage implements IWorkbenchPreferencePage {

    private static final Log log = Log.getLog(DDSyncPreferencePage.class);

    public static final String SECRET_ACCESS_KEY = "datadam.access-key";

    private static final String SYNC_TITLE = "Synchronization";
    private static final String ENV_URL = "DATADAM_URL";

    private Text accountText;
    private Text workspaceText;
    private Button deleteButton;
    private Button uploadButton;
    private Button downloadButton;

    @Override
    public void init(@NotNull IWorkbench workbench) {
        //empty
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
        accountText = UIUtils.createLabelText(group, "Account", "", SWT.READ_ONLY);

        Composite buttons = UIUtils.createComposite(group, 2);
        GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.horizontalSpan = 2;
        buttons.setLayoutData(gd);
        UIUtils.createPushButton(buttons, "Import Key...", null, SelectionListener.widgetSelectedAdapter(e -> {
            DDImportKeyDialog dialog = new DDImportKeyDialog(getShell());
            if (dialog.open() == Window.OK) {
                saveKey(dialog.getKey());
                refresh();
            }
        }));
        deleteButton = UIUtils.createPushButton(buttons, "Delete", null, SelectionListener.widgetSelectedAdapter(e -> {
            if (UIUtils.confirmAction(getShell(), "Delete access key", "Delete the stored access key?")) {
                saveKey(null);
                refresh();
            }
        }));

        Composite syncGroup = UIUtils.createTitledComposite(
            composite,
            "Workspace",
            2,
            GridData.FILL_HORIZONTAL,
            SWT.DEFAULT);
        workspaceText = UIUtils.createLabelText(syncGroup, "Bound to", "", SWT.READ_ONLY);

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
            String workspaceId = service.getBoundWorkspaceId();
            if (workspaceId == null) {
                String label = askWorkspaceLabel();
                if (label == null) {
                    return;
                }
                workspaceId = service.createWorkspace(label);
            }
            List<String> uploaded = service.upload(workspaceId);
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
            String workspaceId = service.getBoundWorkspaceId();
            if (workspaceId == null) {
                workspaceId = askWorkspace(service.listWorkspaces());
                if (workspaceId == null) {
                    return;
                }
                service.bindWorkspace(workspaceId);
            }
            List<String> restored = service.download(workspaceId);
            refresh();
            DBWorkbench.getPlatformUI().showMessageBox(
                SYNC_TITLE,
                restored.isEmpty()
                    ? "Nothing to download"
                    : "Downloaded: " + String.join(", ", restored) + ".\nRestart DBeaver to apply.",
                false);
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError(SYNC_TITLE, "Download failed", e);
        }
    }

    @Nullable
    private DDSyncService createSyncService() {
        String key = readKey();
        DDAccessKey accessKey = DDAccessKey.parseOrNull(key);
        if (accessKey == null) {
            DBWorkbench.getPlatformUI().showMessageBox(SYNC_TITLE, "Import an access key first", true);
            return null;
        }
        String url = System.getenv(ENV_URL);
        if (CommonUtils.isEmpty(url)) {
            DBWorkbench.getPlatformUI().showMessageBox(SYNC_TITLE, "DataDam URL is not configured", true);
            return null;
        }
        return new DDSyncService(
            url,
            accessKey,
            DBWorkbench.getPlatform().getWorkspace());
    }

    @Nullable
    private String askWorkspaceLabel() {
        String label = EnterNameDialog.chooseName(getShell(), "Workspace name", "");
        return CommonUtils.isEmpty(label) ? null : label;
    }

    @Nullable
    private String askWorkspace(@NotNull List<DDWorkspace> workspaces) {
        if (workspaces.isEmpty()) {
            DBWorkbench.getPlatformUI().showMessageBox(SYNC_TITLE, "No synchronized workspaces found", true);
            return null;
        }
        ElementListSelectionDialog dialog = new ElementListSelectionDialog(getShell(), new LabelProvider() {
            @NotNull
            @Override
            public String getText(@NotNull Object element) {
                return ((DDWorkspace) element).label();
            }
        });
        dialog.setTitle(SYNC_TITLE);
        dialog.setMessage("Select workspace");
        dialog.setElements(workspaces.toArray());
        if (dialog.open() != Window.OK) {
            return null;
        }
        return ((DDWorkspace) dialog.getFirstResult()).workspaceId();
    }

    private void saveKey(@Nullable String key) {
        try {
            DBSSecretController.getGlobalSecretController().setPrivateSecretValue(SECRET_ACCESS_KEY, key);
        } catch (DBException e) {
            log.error("Error saving access key", e);
        }
    }

    private void refresh() {
        String key = readKey();
        boolean present = !CommonUtils.isEmpty(key);
        DDAccessKey accessKey = present ? DDAccessKey.parseOrNull(key) : null;
        accountText.setText(accessKey == null ? "" : accessKey.accountId().toString());
        deleteButton.setEnabled(present);

        String workspaceId = DDSyncService.readBinding(
            DBWorkbench.getPlatform().getWorkspace().getAbsolutePath());
        workspaceText.setText(CommonUtils.notEmpty(workspaceId));
        uploadButton.setEnabled(present);
        downloadButton.setEnabled(present);
    }

    @Nullable
    private String readKey() {
        try {
            return DBSSecretController.getGlobalSecretController().getPrivateSecretValue(SECRET_ACCESS_KEY);
        } catch (DBException e) {
            log.error("Error reading access key", e);
            return null;
        }
    }
}
