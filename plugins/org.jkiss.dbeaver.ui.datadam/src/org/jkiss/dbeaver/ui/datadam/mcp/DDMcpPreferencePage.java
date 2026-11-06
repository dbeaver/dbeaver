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
package org.jkiss.dbeaver.ui.datadam.mcp;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.datadam.DDEndpoints;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessListener;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DDDesktopAccessService;
import org.jkiss.dbeaver.model.datadam.sync.DDSyncChange;
import org.jkiss.dbeaver.model.datadam.sync.core.DDConfigurationClient;
import org.jkiss.dbeaver.model.datadam.sync.core.DDConfigurationSyncService;
import org.jkiss.dbeaver.model.datadam.sync.core.DDConfigurationSyncService.Resolution;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sync.DBPSyncTarget;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.datadam.internal.DDMcpMessages;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;

import java.util.UUID;

public final class DDMcpPreferencePage extends AbstractPrefPage implements IWorkbenchPreferencePage {

    private final DDDesktopAccessService accessService = DDDesktopAccessService.getInstance();
    private final DDMcpServerSyncUnit syncUnit = new DDMcpServerSyncUnit();
    private final Runnable keyBundleListener = this::accessChanged;
    private final DDDesktopAccessListener accessListener = new DDDesktopAccessListener() {
        @Override
        public void accessChanging() {
            cancelSynchronization();
        }

        @Override
        public void accessChanged() {
            DDMcpPreferencePage.this.accessChanged();
        }
    };

    private Composite content;
    private Button syncButton;
    private Label statusLabel;
    private boolean disposed;
    @Nullable
    private volatile SyncJob syncJob;

    public DDMcpPreferencePage() {
        noDefaultAndApplyButton();
    }

    @Override
    public void init(@NotNull IWorkbench workbench) {
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        content = UIUtils.createPlaceholder(parent, 1);
        content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        Label description = new Label(content, SWT.WRAP);
        description.setText(DDMcpMessages.mcp_sync_page_description);
        GridData descriptionLayout = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
        descriptionLayout.widthHint = 600;
        description.setLayoutData(descriptionLayout);
        syncButton = UIUtils.createPushButton(
            content, DDMcpMessages.mcp_sync_button, null,
            SelectionListener.widgetSelectedAdapter(event -> synchronize())
        );
        syncButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        statusLabel = new Label(content, SWT.WRAP);
        statusLabel.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
        accessService.addAccessListener(accessListener);
        accessService.addKeyBundleListener(keyBundleListener);
        refreshAvailability();
        return content;
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (content != null && !content.isDisposed()) {
            if (visible) {
                refreshAvailability();
            } else {
                cancelSynchronization();
            }
        }
    }

    @Override
    public void dispose() {
        disposed = true;
        accessService.removeAccessListener(accessListener);
        accessService.removeKeyBundleListener(keyBundleListener);
        cancelSynchronization();
        super.dispose();
    }

    private void accessChanged() {
        cancelSynchronization();
        UIUtils.asyncExec(this::refreshAvailability);
    }

    private void cancelSynchronization() {
        SyncJob job = syncJob;
        if (job != null) {
            job.cancel();
        }
    }

    private void refreshAvailability() {
        if (disposed || content == null || content.isDisposed()) {
            return;
        }
        String problem = availabilityProblem();
        syncButton.setEnabled(syncJob == null && problem == null);
        if (syncJob == null) {
            setStatus(problem == null ? DDMcpMessages.mcp_sync_ready : problem);
        }
    }

    @Nullable
    private String availabilityProblem() {
        if (!syncUnit.isAvailable()) {
            return DDMcpMessages.mcp_sync_unavailable;
        }
        if (accessService.getStatus() != DDDesktopAccessService.Status.SIGNED_IN) {
            return DDMcpMessages.mcp_sync_sign_in_required;
        }
        return accessService.getAccountId() == null ? DDMcpMessages.mcp_sync_keys_required : null;
    }

    private boolean isCurrentAccount(@NotNull UUID accountId) {
        return accessService.getStatus() == DDDesktopAccessService.Status.SIGNED_IN
            && accountId.equals(accessService.getAccountId());
    }

    private void synchronize() {
        if (syncJob != null) {
            return;
        }
        UUID accountId = accessService.getAccountId();
        if (accountId == null || availabilityProblem() != null) {
            refreshAvailability();
            return;
        }
        startSynchronization(DDEndpoints.getStorageBaseUrl(), accountId, null);
    }

    private void startSynchronization(@NotNull String storageUrl, @NotNull UUID accountId, @Nullable Resolution resolution) {
        SyncJob job = new SyncJob(storageUrl, accountId, resolution);
        job.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(@NotNull IJobChangeEvent event) {
                IStatus result = event.getResult();
                UIUtils.asyncExec(() -> synchronizationFinished(job, result));
            }
        });
        syncJob = job;
        syncButton.setEnabled(false);
        setStatus(DDMcpMessages.mcp_sync_running);
        job.setUser(true);
        job.schedule();
    }

    private void synchronizationFinished(@NotNull SyncJob job, @NotNull IStatus result) {
        if (disposed || content.isDisposed() || syncJob != job) {
            return;
        }
        syncJob = null;
        String problem = availabilityProblem();
        syncButton.setEnabled(problem == null);
        if (result.matches(IStatus.CANCEL) || job.isCanceled() || !isCurrentAccount(job.accountId) || !getControl().isVisible()) {
            setStatus(problem == null ? DDMcpMessages.mcp_sync_canceled : problem);
        } else if (!result.isOK()) {
            setStatus(result.getMessage());
        } else {
            setStatus(switch (job.change) {
                case LOCAL -> DDMcpMessages.mcp_sync_uploaded;
                case SERVER -> DDMcpMessages.mcp_sync_downloaded;
                case UNCHANGED -> DDMcpMessages.mcp_sync_unchanged;
                case CONFLICT -> DDMcpMessages.mcp_sync_conflict_message;
            });
            if (job.change == DDSyncChange.CONFLICT) {
                resolveConflict(job);
            }
        }
    }

    private void resolveConflict(@NotNull SyncJob job) {
        MessageDialog dialog = new MessageDialog(
            getShell(), DDMcpMessages.mcp_sync_conflict_title, null, DDMcpMessages.mcp_sync_conflict_message,
            MessageDialog.QUESTION,
            new String[] {DDMcpMessages.mcp_sync_use_local, DDMcpMessages.mcp_sync_use_server, IDialogConstants.CANCEL_LABEL}, 2
        );
        int choice = dialog.open();
        if (!disposed && !content.isDisposed()) {
            String problem = availabilityProblem();
            syncButton.setEnabled(problem == null);
            if ((choice == 0 || choice == 1) && problem == null && isCurrentAccount(job.accountId) && getControl().isVisible()) {
                startSynchronization(job.storageUrl, job.accountId,
                    choice == 0 ? Resolution.USE_LOCAL_VERSION : Resolution.USE_SERVER_VERSION);
            } else {
                setStatus(problem == null ? DDMcpMessages.mcp_sync_canceled : problem);
            }
        }
    }

    private void setStatus(@NotNull String message) {
        statusLabel.setText(message);
        content.layout(true, true);
    }

    private final class SyncJob extends AbstractJob {
        private final String storageUrl;
        private final UUID accountId;
        @Nullable
        private final Resolution resolution;
        private DDSyncChange change = DDSyncChange.UNCHANGED;

        private SyncJob(@NotNull String storageUrl, @NotNull UUID accountId, @Nullable Resolution resolution) {
            super(DDMcpMessages.mcp_sync_job);
            this.storageUrl = storageUrl;
            this.accountId = accountId;
            this.resolution = resolution;
        }

        @NotNull
        @Override
        protected IStatus run(@NotNull DBRProgressMonitor monitor) {
            monitor.beginTask(getName(), IProgressMonitor.UNKNOWN);
            try {
                if (monitor.isCanceled() || !isCurrentAccount(accountId)) {
                    return Status.CANCEL_STATUS;
                }
                DDConfigurationClient client = new DDConfigurationClient(storageUrl, accessService);
                DDConfigurationSyncService service = new DDConfigurationSyncService(client, storageUrl, accountId);
                DBPSyncTarget target = new DBPSyncTarget(DBWorkbench.getPlatform().getWorkspace(), null);
                change = resolution == null ? service.synchronize(monitor, syncUnit, target)
                    : service.resolveConflict(monitor, syncUnit, target, resolution);
                return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
            } catch (DBException | RuntimeException e) {
                return monitor.isCanceled() ? Status.CANCEL_STATUS
                    : Status.error(NLS.bind(DDMcpMessages.mcp_sync_failed, e.getMessage()), e);
            } finally {
                monitor.done();
            }
        }
    }
}
