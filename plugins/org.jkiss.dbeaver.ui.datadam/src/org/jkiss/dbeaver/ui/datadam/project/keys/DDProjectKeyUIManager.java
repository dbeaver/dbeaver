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
package org.jkiss.dbeaver.ui.datadam.project.keys;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.app.DBPPlatformDesktop;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.datadam.auth.DDKeyBundle;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DBDamAccessService;
import org.jkiss.dbeaver.model.datadam.auth.desktop.DBDamEncryptionKeyService;
import org.jkiss.dbeaver.model.datadam.sync.project.DDProjectManager;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.RunnableWithResult;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.datadam.DBDamTokenUIManager;
import org.jkiss.dbeaver.ui.datadam.DDImportKeyDialog;
import org.jkiss.dbeaver.ui.datadam.internal.DDTrackingUIMessages;
import org.jkiss.dbeaver.ui.datadam.project.DDProjectUIManager;
import org.jkiss.dbeaver.ui.navigator.actions.NavigatorHandlerProjectSetActive;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/** Interactive recovery for account encryption resets, before project operations start. */
public final class DDProjectKeyUIManager {
    private static final Log log = Log.getLog(DDProjectKeyUIManager.class);
    private static final DDProjectKeyUIManager INSTANCE = new DDProjectKeyUIManager();
    private final AtomicBoolean cleanupInProgress = new AtomicBoolean();
    private final AtomicBoolean activeProjectCheckRunning = new AtomicBoolean();
    // Acquired only by worker operations; UI callbacks must never wait for this lock.
    private final ReentrantLock projectOperationLock = new ReentrantLock();

    private DDProjectKeyUIManager() {
    }

    @NotNull
    public static DDProjectKeyUIManager getInstance() {
        return INSTANCE;
    }

    public boolean isCleanupInProgress() {
        return cleanupInProgress.get();
    }

    public void scheduleActiveProjectCheck() {
        if (cleanupInProgress.get() || DBDamAccessService.getInstance().getStatus() == DBDamAccessService.Status.UNLOADED
            || !activeProjectCheckRunning.compareAndSet(false, true)) {
            return;
        }
        AbstractJob job = new AbstractJob(DDTrackingUIMessages.project_sync_title) {
            @NotNull
            @Override
            protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                projectOperationLock.lock();
                try {
                    if (cleanupInProgress.get() || monitor.isCanceled()) {
                        return Status.CANCEL_STATUS;
                    }
                    ensureActiveProject(DBPPlatformDesktop.getInstance().getWorkspace().getVisibleProjects());
                    return Status.OK_STATUS;
                } catch (DBException e) {
                    return Status.error("Could not create a local project", e);
                } finally {
                    projectOperationLock.unlock();
                    activeProjectCheckRunning.set(false);
                }
            }
        };
        job.setSystem(true);
        job.schedule();
    }

    /** Invoked after the user confirms local project deletion. */
    public void forgetKeys() throws DBException {
        var access = DBDamAccessService.getInstance();
        changeAccessAndRemoveProjects(access.getKeyBundle(), access::forgetKeys);
    }

    @NotNull
    public String generateKeys(@NotNull DBRProgressMonitor monitor) throws DBException, InterruptedException {
        return DBDamEncryptionKeyService.getInstance().generateNewKey(monitor);
    }

    /** Imports the current server key, removing local projects before replacing a different saved key. */
    public void importKeys(@NotNull String phrase, @NotNull DBRProgressMonitor monitor) throws DBException, InterruptedException {
        if (monitor.isCanceled()) {
            throw new InterruptedException("DataDam encryption key import was canceled");
        }
        var access = DBDamAccessService.getInstance();
        DDKeyBundle previous = access.getKeyBundle();
        if (previous == null) {
            access.importKeys(phrase);
            return;
        }
        DDKeyBundle replacement = access.prepareKeyImport(phrase);
        if (!previous.accountId().equals(replacement.accountId())) {
            throw new DBException("DataDam account changed during encryption key import");
        }
        if (Objects.equals(previous.apiKeyId(), replacement.apiKeyId())
            && previous.signingKey().equals(replacement.signingKey()) && previous.dataKey().equals(replacement.dataKey())) {
            if (monitor.isCanceled()) {
                throw new InterruptedException("DataDam encryption key import was canceled");
            }
            access.importKeys(phrase);
            return;
        }

        if (monitor.isCanceled()) {
            throw new InterruptedException("DataDam encryption key import was canceled");
        }
        Boolean confirmed = UIUtils.syncExec(new RunnableWithResult<>() {
            @NotNull
            @Override
            public Boolean runWithResult() {
                return UIUtils.confirmAction(UIUtils.getActiveShell(), DDTrackingUIMessages.project_keys_mismatch_title,
                    DDTrackingUIMessages.sync_preference_page_project_deletion_confirm_message);
            }
        });
        if (!Boolean.TRUE.equals(confirmed) || monitor.isCanceled()) {
            throw new InterruptedException("DataDam encryption key import was canceled");
        }
        replaceKeysAndRemoveProjects(access, previous, replacement);
    }

    /** Invoked after the user confirms sign-out and project deletion. */
    public void signOut() throws DBException {
        signOut(DBDamAccessService.getInstance().getKeyBundle());
    }

    /** Uses the bundle captured when the user was warned about local project deletion. */
    public void signOut(@Nullable DDKeyBundle keys) throws DBException {
        changeAccessAndRemoveProjects(keys, DBDamAccessService.getInstance()::signOut);
    }

    private void changeAccessAndRemoveProjects(@Nullable DDKeyBundle keys, @NotNull AccessOperation operation) throws DBException {
        if (!cleanupInProgress.compareAndSet(false, true)) {
            throw new DBException("DataDam project cleanup is already in progress");
        }
        projectOperationLock.lock();
        try {
            if (keys != null) {
                removeLocalAccountProjects(keys.accountId());
            }
            operation.run();
        } finally {
            projectOperationLock.unlock();
            finishProjectCleanup();
        }
    }

    private void finishProjectCleanup() {
        cleanupInProgress.set(false);
        DDProjectUIManager.getInstance().refreshProjectVisibility();
    }

    @FunctionalInterface
    private interface AccessOperation {
        void run() throws DBException;
    }

    /** Removes local account projects before discarding the credentials needed to retry cleanup. */
    private void removeLocalAccountProjects(@NotNull UUID accountId) throws DBException {
        DDProjectManager manager = DDProjectManager.getInstance();
        prepareProjectDeletion(manager, accountId);
        manager.deleteAccountProjects(accountId);
    }

    public synchronized boolean ensureKeysReady(@NotNull DBRProgressMonitor monitor) throws DBException {
        var access = DBDamAccessService.getInstance();
        if (!DBDamTokenUIManager.getInstance().ensureTokenReady(monitor)) {
            return false;
        }
        var bundle = access.getKeyBundle();
        if (bundle == null || monitor.isCanceled()) {
            return false;
        }
        if (access.validateProjectKeys()) {
            return !monitor.isCanceled();
        }
        if (!bundle.equals(access.getKeyBundle()) || monitor.isCanceled()) {
            return false;
        }
        Recovery recovery = UIUtils.syncExec(new RunnableWithResult<>() {
            @Override
            @Nullable
            public Recovery runWithResult() {
                int choice = new MessageDialog(UIUtils.getActiveWorkbenchShell(), DDTrackingUIMessages.project_keys_mismatch_title,
                    null, DDTrackingUIMessages.project_keys_mismatch_message, MessageDialog.WARNING,
                    new String[] { DDTrackingUIMessages.project_keys_reset_local, DDTrackingUIMessages.project_keys_enter_new,
                        IDialogConstants.CANCEL_LABEL }, 2).open();
                if (choice == 0) {
                    return new Recovery(null);
                }
                if (choice == 1) {
                    DDImportKeyDialog dialog = new DDImportKeyDialog(UIUtils.getActiveWorkbenchShell());
                    if (dialog.open() == Window.OK) {
                        return new Recovery(dialog.getPhrase());
                    }
                }
                return null;
            }
        });
        if (recovery == null || monitor.isCanceled() || !bundle.equals(access.getKeyBundle())) {
            return false;
        }
        DDKeyBundle replacement = recovery.phrase() == null ? null : access.prepareKeyImport(recovery.phrase());
        if (replacement != null && !bundle.accountId().equals(replacement.accountId())) {
            throw new DBException("DataDam account changed during recovery");
        }
        if (monitor.isCanceled() || !bundle.equals(access.getKeyBundle())) {
            return false;
        }
        replaceKeysAndRemoveProjects(access, bundle, replacement);
        // Callers may already hold project objects that were removed during recovery.
        return false;
    }

    private void replaceKeysAndRemoveProjects(
        @NotNull DBDamAccessService access, @NotNull DDKeyBundle previous, @Nullable DDKeyBundle replacement
    ) throws DBException {
        if (!cleanupInProgress.compareAndSet(false, true)) {
            throw new DBException("DataDam project cleanup is already in progress");
        }
        projectOperationLock.lock();
        try {
            DDProjectManager manager = DDProjectManager.getInstance();
            // Activate the replacement before acquiring the access lock: this step needs the UI thread.
            prepareProjectDeletion(manager, previous.accountId());
            access.recoverProjectKeys(previous, replacement, () -> manager.deleteAccountProjects(previous.accountId()));
        } finally {
            projectOperationLock.unlock();
            finishProjectCleanup();
        }
    }

    private void prepareProjectDeletion(@NotNull DDProjectManager manager, @NotNull UUID accountId) throws DBException {
        var accountProjects = manager.getAccountProjects(accountId);
        var workspace = DBPPlatformDesktop.getInstance().getWorkspace();
        DBPProject currentProject = workspace.getActiveProject();
        if (currentProject != null && !accountProjects.contains(currentProject)) {
            return;
        }
        DBPProject replacement = null;
        for (var project : workspace.getProjects()) {
            if (manager.getBindingStatus(project) == DDProjectManager.BindingStatus.UNBOUND) {
                replacement = project;
                break;
            }
        }
        ensureActiveProject(replacement == null ? List.of() : List.of(replacement));
    }

    private void ensureActiveProject(@NotNull List<? extends DBPProject> candidates) throws DBException {
        DBPWorkspace workspace = DBPPlatformDesktop.getInstance().getWorkspace();
        DBPProject currentProject = workspace.getActiveProject();
        if (currentProject != null && candidates.contains(currentProject)) {
            return;
        }
        DBPProject project;
        if (candidates.isEmpty()) {
            if (!workspace.canManageProjects()) {
                log.debug("Cannot create a local project: project management is not available");
                return;
            }
            String name = DBConstants.DEFAULT_PROJECT_NAME;
            int suffix = 1;
            while (workspace.getProject(name) != null) {
                name = DBConstants.DEFAULT_PROJECT_NAME + " " + suffix++;
            }
            log.debug("Creating local project '" + name + "': no suitable active project is available");
            project = workspace.createProject(name, null);
            log.debug("Created local project '" + project.getName() + "'");
        } else {
            project = candidates.getFirst();
        }
        UIUtils.syncExec(() -> {
            log.debug("Activating local project '" + project.getName() + "'");
            NavigatorHandlerProjectSetActive.setActiveProject(project);
        });
    }

    private record Recovery(@Nullable String phrase) {
    }
}
