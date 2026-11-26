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
package org.jkiss.dbeaver.ui.datadam.project.preference;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.datadam.DDProductConfig;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DBDamAccessListener;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DBDamAccessService;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DBDamEncryptionKeyService;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UIExecutionQueue;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.datadam.DBDamService;
import org.jkiss.dbeaver.ui.datadam.DDImportKeyDialog;
import org.jkiss.dbeaver.ui.datadam.DDUIUtils;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;
import org.jkiss.dbeaver.ui.datadam.project.keys.DDGeneratedEncryptionKeyDialog;
import org.jkiss.dbeaver.ui.datadam.project.keys.DDProjectKeyUIManager;

import java.lang.reflect.InvocationTargetException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/** Encryption key controls and their local account display on the Projects preference page. */
final class DDEncryptionKeysSection {
    private static final Log log = Log.getLog(DDEncryptionKeysSection.class);

    private final DBDamAccessService accessService;
    private final DBDamEncryptionKeyService keyService = DBDamEncryptionKeyService.getInstance();
    private final DDProjectKeyUIManager keyManager = DDProjectKeyUIManager.getInstance();
    private final Runnable onKeysChanged;
    private final BiConsumer<String, Throwable> onError;
    private final Composite accountRow;
    private final Text accountText;
    private final Button importKeysButton;
    private final Button generateKeysButton;
    private final Button forgetKeysButton;
    private final Button lostKeysButton;
    private final Control projectsHelp;
    private final Control statusLoading;
    private final Control statusUnavailable;
    private final Control noManagePermission;
    private final Button statusRetryButton;
    private final Composite content;
    private final DBDamAccessListener accessListener = new DBDamAccessListener() {
        @Override
        public void accessChanging() {
            UIUtils.asyncExec(() -> {
                if (!accountRow.isDisposed()) {
                    cancelJobs();
                }
            });
        }

        @Override
        public void accessChanged() {
            UIUtils.asyncExec(() -> {
                if (!accountRow.isDisposed()) {
                    onKeysChanged.run();
                }
            });
        }
    };
    @Nullable
    private AbstractJob statusJob;
    @Nullable
    private KeyStatus keyStatus;
    private boolean statusUnavailableFlag;
    @Nullable
    private AbstractJob accountViewJob;

    private record KeyStatus(boolean serverKeysPresent, boolean canManageKeys, boolean canResetKeys) {
    }

    DDEncryptionKeysSection(
        @NotNull Composite parent,
        @NotNull DBDamAccessService accessService,
        @NotNull Runnable onKeysChanged,
        @NotNull BiConsumer<String, Throwable> onError
    ) {
        this.content = parent;
        this.accessService = accessService;
        this.onKeysChanged = onKeysChanged;
        this.onError = onError;

        Composite keysGroup = UIUtils.createTitledComposite(
            parent, DDTrackingUIMessages.sync_preference_page_encryption_keys_group,
            2, GridData.FILL_HORIZONTAL, SWT.DEFAULT
        );
        GridData keysGroupLayout = (GridData) keysGroup.getParent().getLayoutData();
        keysGroupLayout.verticalIndent = 5;
        accountRow = UIUtils.createComposite(keysGroup, 2);
        GridData accountRowData = new GridData(GridData.FILL_HORIZONTAL);
        accountRowData.horizontalSpan = 2;
        accountRow.setLayoutData(accountRowData);
        GridData accountTextData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        accountTextData.widthHint = UIUtils.getFontHeight(accountRow.getDisplay().getSystemFont()) * 22;
        accountText = UIUtils.createLabelText(
            accountRow, DDTrackingUIMessages.sync_preference_page_account_label, "", SWT.READ_ONLY, accountTextData);
        Composite keyButtons = UIUtils.createComposite(keysGroup, 3);
        GridData keyButtonsData = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        keyButtonsData.horizontalSpan = 2;
        keyButtons.setLayoutData(keyButtonsData);
        importKeysButton = UIUtils.createPushButton(
            keyButtons, DDTrackingUIMessages.sync_preference_page_import_keys_button, null,
            SelectionListener.widgetSelectedAdapter(e -> importKeys())
        );
        GridData importKeysLayout = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
        importKeysLayout.horizontalIndent = 3;
        importKeysButton.setLayoutData(importKeysLayout);
        generateKeysButton = UIUtils.createPushButton(
            keyButtons, DDTrackingUIMessages.sync_preference_page_generate_keys_button, null,
            SelectionListener.widgetSelectedAdapter(e -> generateKeys())
        );
        generateKeysButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        forgetKeysButton = UIUtils.createPushButton(
            keyButtons, DDTrackingUIMessages.sync_preference_page_forget_keys_button, null,
            SelectionListener.widgetSelectedAdapter(e -> forgetKeys())
        );
        GridData forgetKeysLayout = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
        forgetKeysLayout.horizontalIndent = 3;
        forgetKeysButton.setLayoutData(forgetKeysLayout);
        lostKeysButton = UIUtils.createPushButton(
            keyButtons, DDTrackingUIMessages.import_key_dialog_reset_button, null,
            SelectionListener.widgetSelectedAdapter(e -> DDUIUtils.openCredentialReset()));
        lostKeysButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        projectsHelp = UIUtils.createWarningLabel(
            keysGroup, DDTrackingUIMessages.projects_page_sign_in_help, GridData.HORIZONTAL_ALIGN_BEGINNING, 2);
        ((GridData) projectsHelp.getLayoutData()).verticalIndent = 5;
        statusLoading = UIUtils.createInfoLabel(keysGroup,
            DDTrackingUIMessages.sync_preference_page_key_status_loading, GridData.HORIZONTAL_ALIGN_BEGINNING, 2);
        statusUnavailable = UIUtils.createWarningLabel(keysGroup,
            DDTrackingUIMessages.sync_preference_page_key_status_unavailable, GridData.HORIZONTAL_ALIGN_BEGINNING, 2);
        statusRetryButton = UIUtils.createPushButton(
            keysGroup, DDTrackingUIMessages.sync_preference_page_key_status_retry, null,
            SelectionListener.widgetSelectedAdapter(e -> refresh(accessService.getAccountId(),
                accessService.getStatus() == DBDamAccessService.Status.SIGNED_IN)));
        GridData retryLayout = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
        retryLayout.horizontalSpan = 2;
        statusRetryButton.setLayoutData(retryLayout);
        noManagePermission = UIUtils.createWarningLabel(keysGroup,
            DDTrackingUIMessages.sync_preference_page_key_manage_unavailable, GridData.HORIZONTAL_ALIGN_BEGINNING, 2);

        accessService.addAccessListener(accessListener);
    }

    void refresh(@Nullable UUID accountId, boolean signedIn) {
        boolean present = accountId != null;
        accountRow.setVisible(present);
        ((GridData) accountRow.getLayoutData()).exclude = !present;
        projectsHelp.setVisible(!signedIn);
        ((GridData) projectsHelp.getLayoutData()).exclude = signedIn;

        cancelJobs();
        accountText.setText(present ? accountId.toString() : DDTrackingUIMessages.sync_preference_page_unknown);
        if (present) {
            loadAccountName();
        }
        keyStatus = null;
        statusUnavailableFlag = signedIn && !DDProductConfig.isIntegrationEnabled();
        updateControls(present, signedIn);
        if (signedIn && !statusUnavailableFlag) {
            loadKeyStatus();
        }
    }

    void dispose() {
        accessService.removeAccessListener(accessListener);
        cancelJobs();
    }

    private void cancelJobs() {
        if (statusJob != null) {
            statusJob.cancel();
        }
        if (accountViewJob != null) {
            accountViewJob.cancel();
        }
    }

    private void loadKeyStatus() {
        statusJob = DBDamService.getInstance().createJob(
            "Check DataDam encryption key status",
            ignored -> {
                boolean serverKeysPresent = keyService.isKeysPresent();
                boolean canManageKeys = keyService.canManageKeys();
                boolean canResetKeys = canManageKeys && keyService.canResetKeys();
                return new KeyStatus(serverKeysPresent, canManageKeys, canResetKeys);
            },
            status -> {
                if (!accountRow.isDisposed()) {
                    keyStatus = status;
                    updateControls(accessService.getAccountId() != null,
                        accessService.getStatus() == DBDamAccessService.Status.SIGNED_IN);
                }
            },
            error -> {
                if (!accountRow.isDisposed()) {
                    log.debug("Could not check DataDam encryption key status", error);
                    statusUnavailableFlag = true;
                    updateControls(accessService.getAccountId() != null,
                        accessService.getStatus() == DBDamAccessService.Status.SIGNED_IN);
                }
            });
    }

    private void updateControls(boolean localKeysPresent, boolean signedIn) {
        KeyStatus status = keyStatus;
        boolean loaded = signedIn && status != null && !statusUnavailableFlag;
        boolean serverKeysPresent = loaded && status.serverKeysPresent();
        boolean canManageKeys = loaded && status.canManageKeys();
        boolean canResetKeys = loaded && status.canResetKeys();
        setVisible(importKeysButton, serverKeysPresent && !localKeysPresent);
        setVisible(generateKeysButton, loaded && !serverKeysPresent && canManageKeys);
        setVisible(forgetKeysButton, localKeysPresent);
        setVisible(lostKeysButton, serverKeysPresent && canResetKeys);
        setVisible(noManagePermission, loaded && !serverKeysPresent && !canManageKeys);
        setVisible(statusLoading, signedIn && !loaded && !statusUnavailableFlag);
        setVisible(statusUnavailable, signedIn && statusUnavailableFlag);
        setVisible(statusRetryButton, signedIn && statusUnavailableFlag);
        content.layout(true, true);
    }

    private void setVisible(@NotNull Control control, boolean visible) {
        control.setVisible(visible);
        ((GridData) control.getLayoutData()).exclude = !visible;
        if (control instanceof Button button) {
            button.setEnabled(visible);
        }
    }

    private void generateKeys() {
        generateKeysButton.setEnabled(false);
        CompletableFuture<String> generatedPhrase = new CompletableFuture<>();
        try {
            UIUtils.runInProgressDialog(monitor -> {
                monitor.beginTask(DDTrackingUIMessages.sync_preference_page_generate_keys_button,
                    DBDamEncryptionKeyService.GENERATION_WORK_UNITS);
                try {
                    generatedPhrase.complete(keyManager.generateKeys(monitor));
                } catch (InterruptedException e) {
                    generatedPhrase.cancel(false);
                    // The service returns the phrase even if cancellation arrives after registration.
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                } finally {
                    monitor.done();
                }
            });
            if (!generatedPhrase.isCancelled()) {
                String phrase = generatedPhrase.getNow(null);
                if (phrase != null) {
                    new DDGeneratedEncryptionKeyDialog(UIUtils.getActiveWorkbenchShell(), phrase).open();
                    importKeys(phrase);
                }
            }
        } catch (InvocationTargetException e) {
            onError.accept(DDTrackingUIMessages.sync_preference_page_generate_keys_failed, e.getTargetException());
        } finally {
            if (!generatedPhrase.isDone() || generatedPhrase.isCancelled()) {
                notifyKeysChanged();
            }
        }
    }

    private void notifyKeysChanged() {
        if (!accountRow.isDisposed()) {
            onKeysChanged.run();
        }
    }

    private void loadAccountName() {
        accountViewJob = new AbstractJob("Read DataDam account information") {
            @NotNull
            @Override
            protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                if (monitor.isCanceled()) {
                    return Status.CANCEL_STATUS;
                }
                var user = accessService.getLoggedUserView();
                UIUtils.asyncExec(() -> {
                    if (!accountText.isDisposed() && accountViewJob == this && !monitor.isCanceled()) {
                        accountText.setText(user.accountName() != null ? user.accountName()
                            : user.accountId() == null ? DDTrackingUIMessages.sync_preference_page_unknown
                                : user.accountId().toString());
                    }
                });
                return Status.OK_STATUS;
            }
        };
        accountViewJob.setSystem(true);
        accountViewJob.schedule();
    }

    private void importKeys() {
        DDImportKeyDialog dialog = new DDImportKeyDialog(accountRow.getShell());
        if (dialog.open() == Window.OK) {
            importKeys(dialog.getPhrase());
        }
    }

    private void importKeys(@NotNull String phrase) {
        UIExecutionQueue.blockQueue();
        try {
            UIUtils.runInProgressDialog(monitor -> {
                monitor.beginTask(DDTrackingUIMessages.import_key_dialog_title, IProgressMonitor.UNKNOWN);
                try {
                    keyManager.importKeys(phrase, monitor);
                } catch (InterruptedException e) {
                    // The user canceled the import or declined to replace local projects.
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                } finally {
                    monitor.done();
                }
            });
        } catch (InvocationTargetException e) {
            onError.accept(DDTrackingUIMessages.sync_preference_page_import_keys_failed, e.getTargetException());
        } finally {
            UIExecutionQueue.unblockQueue();
            notifyKeysChanged();
        }
    }

    private void forgetKeys() {
        if (UIUtils.confirmAction(
            accountRow.getShell(), DDTrackingUIMessages.sync_preference_page_forget_keys_confirm_title,
            DDTrackingUIMessages.sync_preference_page_project_deletion_confirm_message
        )) {
            runKeyOperation(DDTrackingUIMessages.sync_preference_page_cannot_forget_keys,
                DDProjectKeyUIManager.getInstance()::forgetKeys);
        }
    }

    private void runKeyOperation(@NotNull String errorMessage, @NotNull KeyOperation operation) {
        UIExecutionQueue.blockQueue();
        try {
            UIUtils.runInProgressDialog(monitor -> {
                try {
                    operation.run();
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (InvocationTargetException e) {
            onError.accept(errorMessage, e.getTargetException());
        } finally {
            UIExecutionQueue.unblockQueue();
            onKeysChanged.run();
        }
    }

    @FunctionalInterface
    private interface KeyOperation {
        void run() throws DBException;
    }
}
