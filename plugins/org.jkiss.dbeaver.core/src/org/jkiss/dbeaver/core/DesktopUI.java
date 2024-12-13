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
package org.jkiss.dbeaver.core;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.ModalContext;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.HTMLTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.access.DBAPasswordChangeInfo;
import org.jkiss.dbeaver.model.connection.DBPAuthInfo;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.fs.DBNPathBase;
import org.jkiss.dbeaver.model.runtime.*;
import org.jkiss.dbeaver.model.runtime.load.ILoadService;
import org.jkiss.dbeaver.model.runtime.load.ILoadVisualizer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.DBeaverNotifications;
import org.jkiss.dbeaver.runtime.ui.console.ConsoleUserInterface;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.actions.datasource.DataSourceInvalidateHandler;
import org.jkiss.dbeaver.ui.dialogs.*;
import org.jkiss.dbeaver.ui.dialogs.connection.PasswordChangeDialog;
import org.jkiss.dbeaver.ui.dialogs.driver.DriverEditHelpers;
import org.jkiss.dbeaver.ui.dialogs.exec.ExecutionQueueErrorJob;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.dbeaver.ui.navigator.actions.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.ui.navigator.dialogs.ObjectBrowserDialog;
import org.jkiss.dbeaver.ui.navigator.project.FileSystemExplorerView;
import org.jkiss.dbeaver.ui.notifications.NotificationUtils;
import org.jkiss.dbeaver.ui.views.process.ProcessPropertyTester;
import org.jkiss.dbeaver.ui.views.process.ShellProcessView;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * DBeaver UI core
 */
public class DesktopUI extends ConsoleUserInterface {

    private static final Log log = Log.getLog(DesktopUI.class);

    private TrayIconHandler trayItem;
    private WorkbenchContextListener contextListener;

    public static DesktopUI getInstance() {
        return (DesktopUI) DBWorkbench.getPlatformUI();
    }

    static void disposeUI() {
        DesktopUI instance = getInstance();
        if (instance != null) {
            try {
                instance.dispose();
            } catch (Throwable e) {
                log.error(e);
            }
        }
    }

    private void dispose() {
        if (trayItem != null) {
            trayItem.hide();
        }
    }

    // This method is called during startup thru @ComponentReference in workbench
    public void initialize() {
        this.trayItem = new TrayIconHandler();

        new AbstractJob("Workbench listener") {
            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                if (PlatformUI.isWorkbenchRunning() && !PlatformUI.getWorkbench().isStarting()) {
                    UIUtils.asyncExec(() -> {
                        contextListener = WorkbenchContextListener.registerInWorkbench();
                    });
                } else {
                    schedule(50);
                }
                return Status.OK_STATUS;
            }
        }.schedule();
    }

    public void refreshPartContexts(IWorkbenchPart part) {
        if (contextListener != null) {
            contextListener.deactivatePartContexts(part);
            contextListener.activatePartContexts(part);
        }
    }

    @Override
    public void notifyAgent(String message, int status) {
        if (!DBWorkbench.getPlatform().getPreferenceStore().getBoolean(DBeaverPreferences.AGENT_LONG_OPERATION_NOTIFY)) {
            // Notifications disabled
            return;
        }
        if (TrayIconHandler.isSupported()) {
            UIUtils.syncExec(() -> Display.getCurrent().beep());
            getInstance().trayItem.notify(message, status);
        } else {
            DBeaverNotifications.showNotification(
                "agent.notify",
                "Agent Notification",
                message,
                status == IStatus.INFO ? DBPMessageType.INFORMATION :
                    (status == IStatus.ERROR ? DBPMessageType.ERROR : DBPMessageType.WARNING),
                null);
        }
    }

    @Override
    public boolean acceptLicense(String message, String licenseText) {
        return new UITask<Boolean>() {
            @Override
            protected Boolean runTask() {
                return AcceptLicenseDialog.acceptLicense(
                    UIUtils.getActiveWorkbenchShell(),
                    message,
                    licenseText);
            }
        }.execute();
    }

    @Override
    public UserResponse showError(@Nullable final String title, @Nullable final String message, @NotNull final IStatus status) {
        if (isHeadlessMode()) {
            return super.showError(title, message, status);
        }
        IStatus rootStatus = status;
        for (IStatus s = status; s != null; ) {
            if (s.getException() instanceof DBException) {
                UserResponse dbErrorResp = showDatabaseError(message, (DBException) s.getException());
                if (dbErrorResp != null) {
                    // If this DB error was handled by some DB-specific way then just don't care about it
                    return dbErrorResp;
                }
                break;
            }
            if (s.getChildren() != null && s.getChildren().length > 0) {
                s = rootStatus = s.getChildren()[0];
            } else {
                break;
            }
        }
        if (rootStatus == Status.CANCEL_STATUS) {
            return UserResponse.CANCEL;
        }
        log.error(rootStatus.getMessage(), rootStatus.getException());

        // log.debug(message);
        Runnable runnable = () -> {
            // Display the dialog
            StandardErrorDialog dialog = new StandardErrorDialog(
                UIUtils.getActiveWorkbenchShell(),
                Objects.requireNonNull(title, "Error"),
                message,
                status,
                IStatus.ERROR);
            dialog.open();
        };
        UIUtils.syncExec(runnable);
        return UserResponse.OK;
    }

    private static boolean isHeadlessMode() {
        return DBWorkbench.getPlatform().getApplication().isHeadlessMode();
    }

    @Override
    public UserResponse showError(@Nullable String title, @Nullable String message, Throwable error) {
        if (error == null) {
            return showError(title, message);
        }
        if (isHeadlessMode()) {
            return super.showError(title, message, error);
        }
        return showError(title, message, GeneralUtils.makeExceptionStatus(error));
    }

    @Override
    public UserResponse showError(@NotNull String title, @Nullable String message) {
        if (isHeadlessMode()) {
            return super.showError(title, message);
        }
        return showError(title, null, new Status(IStatus.ERROR, DesktopPlatform.PLUGIN_ID, message));
    }

    @Override
    public void showMessageBox(@NotNull String title, String message, boolean error) {
        if (isHeadlessMode()) {
            super.showMessageBox(title, message, error);
        } else {
            if (error) {
                showMessageBox(title, message, DBIcon.STATUS_ERROR);
            } else {
                showMessageBox(title, message, DBIcon.STATUS_INFO);
            }
        }
    }

    private static void showMessageBox(@NotNull String title, @NotNull String message, @NotNull DBPImage image) {
        UIUtils.syncExec(() -> MessageBoxBuilder.builder(Display.getCurrent().getActiveShell())
                .setTitle(title)
                .setMessage(message)
                .setPrimaryImage(image)
                .setReplies(Reply.OK)
                .showMessageBox()
        );
    }

    @Override
    public void showWarningMessageBox(@NotNull String title, String message) {
        if (isHeadlessMode()) {
            super.showWarningMessageBox(title, message);
        } else {
            showMessageBox(title, message, DBIcon.STATUS_WARNING);
        }
    }

    @Override
    public void showNotification(@NotNull String title, String message, boolean error, @Nullable Runnable feedback) {
        NotificationUtils.sendNotification(
            DBeaverNotifications.NT_GENERIC,
            title,
            message,
            error ? DBPMessageType.ERROR : DBPMessageType.INFORMATION,
            feedback
        );
    }

    @Override
    public void showWarningNotification(@NotNull String title, String message) {
        NotificationUtils.sendNotification(
            DBeaverNotifications.NT_GENERIC,
            title,
            message,
            DBPMessageType.WARNING,
            null
        );
    }

    @Override
    public boolean confirmAction(String title, String message) {
        if (isHeadlessMode()) {
            return super.confirmAction(title, message);
        }
        return UIUtils.confirmAction(title, message);
    }

    @Override
    public boolean confirmAction(String title, String message, boolean isWarning) {
        if (isHeadlessMode()) {
            return super.confirmAction(title, message, isWarning);
        }
        return UIUtils.confirmAction(null, title, message, isWarning ? DBIcon.STATUS_WARNING : DBIcon.STATUS_QUESTION);
    }

    @Override
    public boolean confirmAction(@NotNull String title, @NotNull String message, @NotNull String buttonLabel, boolean isWarning) {
        if (isHeadlessMode()) {
            return super.confirmAction(title, message, buttonLabel, isWarning);
        }
        final Reply confirm = new Reply(buttonLabel);
        final Reply[] decision = new Reply[1];

        UIUtils.syncExec(() -> {
            decision[0] = MessageBoxBuilder.builder(UIUtils.getActiveWorkbenchShell())
                .setTitle(title)
                .setMessage(message)
                .setReplies(confirm, Reply.CANCEL)
                .setDefaultReply(Reply.CANCEL)
                .setPrimaryImage(isWarning ? DBIcon.STATUS_WARNING : DBIcon.STATUS_QUESTION)
                .showMessageBox();
        });

        return decision[0] == confirm;
    }

    @NotNull
    @Override
    public UserChoiceResponse showUserChoice(
        @NotNull String title,
        @Nullable String message,
        @NotNull List<String> labels,
        @NotNull List<String> forAllLabels,
        @Nullable Integer previousChoice,
        int defaultChoice
    ) {
        if (isHeadlessMode()) {
            return super.showUserChoice(title, message, labels, forAllLabels, previousChoice, defaultChoice);
        }
        final List<Reply> reply = labels.stream()
            .map(s -> CommonUtils.isEmpty(s) ? null : new Reply(s))
            .collect(Collectors.toList());

        return UIUtils.syncExec(new RunnableWithResult<UserChoiceResponse>() {
            public UserChoiceResponse runWithResult() {
                List<Button> extraCheckboxes = new ArrayList<>(forAllLabels.size());
                Integer[] selectedCheckboxIndex = { null };
                MessageBoxBuilder mbb = MessageBoxBuilder.builder(UIUtils.getActiveWorkbenchShell())
                    .setTitle(title)
                    .setMessage(message)
                    .setReplies(reply.stream().filter(Objects::nonNull).toArray(Reply[]::new))
                    .setPrimaryImage(DBIcon.STATUS_WARNING);
                
                if (previousChoice != null && reply.get(previousChoice) != null) {
                    mbb.setDefaultReply(reply.get(previousChoice));
                }
                if (forAllLabels.size() > 0) {
                    mbb.setCustomArea(pp -> {
                        SelectionListener selectionListener = SelectionListener.widgetSelectedAdapter(e -> {
                            int chkIndex = (Integer) e.widget.getData();
                            if (extraCheckboxes.get(chkIndex).getSelection()) {
                                selectedCheckboxIndex[0] = chkIndex;
                                for (int index = 0; index < extraCheckboxes.size(); index++) {
                                    if (index != chkIndex) {
                                        extraCheckboxes.get(index).setSelection(false);
                                    }
                                }
                            }
                        });
                        for (int index = 0; index < forAllLabels.size(); index++) {
                            Button chk = UIUtils.createCheckbox(pp, forAllLabels.get(index), false);
                            chk.setData(index);
                            chk.addSelectionListener(selectionListener);
                            extraCheckboxes.add(chk);
                        }
                    });
                }
                
                Reply result = mbb.showMessageBox();
                int choiceIndex = reply.indexOf(result);
                return new UserChoiceResponse(choiceIndex, selectedCheckboxIndex[0]);
            }
        }); 
    }

    @Override
    public UserResponse showErrorStopRetryIgnore(String task, Throwable error, boolean queue) {
        return ExecutionQueueErrorJob.showError(task, error, queue);
    }

    @Override
    public long getLongOperationTimeout() {
        return DBWorkbench.getPlatform().getPreferenceStore().getLong(DBeaverPreferences.AGENT_LONG_OPERATION_TIMEOUT);
    }

    private static UserResponse showDatabaseError(String message, DBException error)
    {
        DBPDataSource dataSource = error instanceof DBDatabaseException dbe ? dbe.getDataSource() : null;
        DBPErrorAssistant.ErrorType errorType = dataSource == null ? DBPErrorAssistant.ErrorType.NORMAL : DBExecUtils.discoverErrorType(dataSource, error);
        switch (errorType) {
            case CONNECTION_LOST:
                if (dataSource.getContainer().getDataSource() == null) {
                    // Error during datasource init
                    return null;
                }
                DataSourceInvalidateHandler.showConnectionLostDialog(null, message, error);
                return UserResponse.OK;
            case DRIVER_CLASS_MISSING:
                DriverEditHelpers.showBadConfigDialog(null, message, error);
                return UserResponse.OK;
        }

        return null;
    }

    @Nullable
    @Override
    public DBPAuthInfo promptUserCredentials(
        @Nullable String prompt,
        @Nullable String description,
        @Nullable String userName,
        @Nullable String userPassword,
        boolean passwordOnly,
        boolean showSavePassword
    ) {
        return promptUserCredentials(
            prompt,
            description,
            UIConnectionMessages.dialog_connection_auth_label_username,
            userName,
            UIConnectionMessages.dialog_connection_auth_label_password,
            userPassword,
            passwordOnly,
            showSavePassword
        );
    }

    @Nullable
    @Override
    public DBPAuthInfo promptUserCredentials(
        @Nullable String prompt,
        @Nullable String description,
        @NotNull String userNameLabel,
        @Nullable String userName,
        @NotNull String passwordLabel,
        @Nullable String userPassword,
        boolean passwordOnly,
        boolean showSavePassword
    ) {
        return new UITask<DBPAuthInfo>() {
            @Override
            public DBPAuthInfo runTask() {
                final Shell shell = UIUtils.getActiveWorkbenchShell();
                final BaseAuthDialog authDialog = new BaseAuthDialog(shell, prompt, passwordOnly, showSavePassword);
                authDialog.setUserNameLabel(userNameLabel);
                authDialog.setPasswordLabel(passwordLabel);
                authDialog.setDescription(description);
                if (!passwordOnly) {
                    authDialog.setUserName(userName);
                }
                authDialog.setUserPassword(userPassword);
                if (authDialog.open() == IDialogConstants.OK_ID) {
                    return authDialog.getAuthInfo();
                } else {
                    return null;
                }
            }
        }.execute();
    }

    @Override
    public DBAPasswordChangeInfo promptUserPasswordChange(String prompt, String userName, String oldPassword, boolean userEditable, boolean oldPasswordVisible) {
        // Ask user
        return new UITask<DBAPasswordChangeInfo>() {
            @Override
            public DBAPasswordChangeInfo runTask() {
                final Shell shell = UIUtils.getActiveWorkbenchShell();
                final PasswordChangeDialog passwordChangeDialog = new PasswordChangeDialog(shell, prompt, userName, oldPassword, userEditable, oldPasswordVisible);
                if (passwordChangeDialog.open() == IDialogConstants.OK_ID) {
                    return passwordChangeDialog.getPasswordInfo();
                } else {
                    return null;
                }
            }
        }.execute();
    }

    @Override
    public String promptProperty(String prompt, String defValue) {
        return new UITask<String>() {
            @Override
            public String runTask() {
                final Shell shell = UIUtils.getActiveWorkbenchShell();
                final EnterNameDialog dialog = new EnterNameDialog(shell, prompt, defValue);
                if (dialog.open() == IDialogConstants.OK_ID) {
                    return dialog.getResult();
                } else {
                    return null;
                }
            }
        }.execute();
    }

    @Override
    public DBNNode selectObject(@NotNull Object parentShell, String title, DBNNode rootNode, DBNNode selectedNode, Class<?>[] allowedTypes, Class<?>[] resultTypes, Class<?>[] leafTypes) {
        DBNNode[] result = new DBNNode[1];
        UIUtils.syncExec(() -> {
            Shell shell = (parentShell instanceof Shell ? (Shell)parentShell : UIUtils.getActiveWorkbenchShell());
            result[0] = ObjectBrowserDialog.selectObject(shell, title, rootNode, selectedNode, allowedTypes, resultTypes, leafTypes);
        });
        return result[0];
    }

    @Override
    public void openEntityEditor(@NotNull DBSObject object) {
        UIUtils.syncExec(() -> NavigatorHandlerObjectOpen.openEntityEditor(object));
    }

    @Override
    public void openEntityEditor(@NotNull DBNNode selectedNode, String defaultPageId) {
        UIUtils.syncExec(() -> NavigatorHandlerObjectOpen.openEntityEditor(selectedNode, defaultPageId, UIUtils.getActiveWorkbenchWindow()));
    }

    @Override
    public void openConnectionEditor(@NotNull DBPDataSourceContainer dataSourceContainer) {
        UIUtils.syncExec(() ->
            NavigatorHandlerObjectOpen.openConnectionEditor(
                UIUtils.getActiveWorkbenchWindow(),
                dataSourceContainer));
    }

    @Override
    public void executeProcess(@NotNull final DBRProcessDescriptor processDescriptor) {
        processDescriptor.setProcessListener(new DBRProcessListener() {
            @Override
            public void onProcessStarted() {
                ProcessPropertyTester.firePropertyChange(ProcessPropertyTester.PROP_RUNNING);
            }

            @Override
            public void onProcessTerminated(int resultCode) {
                ProcessPropertyTester.firePropertyChange(ProcessPropertyTester.PROP_RUNNING);
            }
        });
        // Direct execute
        try {
            processDescriptor.execute();
        } catch (DBException e) {
            showError("Execute process", processDescriptor.getName(), e);
        }
        if (processDescriptor.getCommand().isShowProcessPanel()) {
            UIUtils.asyncExec(() -> {
                try {
                    final ShellProcessView processView =
                        (ShellProcessView) UIUtils.getActiveWorkbenchWindow().getActivePage().showView(
                            ShellProcessView.VIEW_ID,
                            ShellProcessView.getNextId(),
                            IWorkbenchPage.VIEW_VISIBLE
                        );
                    processView.initProcess(processDescriptor);
                } catch (PartInitException e) {
                    log.error(e);
                }
            });
        }
    }

    @Override
    public void executeWithProgress(@NotNull Runnable runnable) {
        UIExecutionQueue.queueExec(runnable);
    }

    @Override
    public void executeWithProgress(@NotNull DBRRunnableWithProgress runnable) throws InvocationTargetException, InterruptedException {
        // FIXME: we need to run with progress service bu we can't change active control focus
        // Otherwise it breaks some functions (e.g. data editor value save as it handles focus events).
        // so we can use runInProgressServie function
        runnable.run(new VoidProgressMonitor());
    }

    /**
     * Execute runnable task synchronously while displaying job indeterminate indicator and blocking the UI, when called from the UI thread
     */
    @NotNull
    @Override
    public <T> Future<T> executeWithProgressBlocking(
        @NotNull String operationDescription,
        @NotNull DBRRunnableWithResult<Future<T>> runnable
    ) {
        final AbstractJob job = new AbstractJob(operationDescription) {
            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                monitor.beginTask(operationDescription, IProgressMonitor.UNKNOWN);
                try {
                    UIExecutionQueue.blockQueue();
                    runnable.run(monitor);
                    return Status.OK_STATUS;
                } catch (Exception ex) {
                    return GeneralUtils.makeExceptionStatus(ex);
                } finally {
                    UIExecutionQueue.unblockQueue();
                    monitor.done();
                }
            }
            
            @Override
            protected void canceling() {
                runnable.cancel();
            }
        };
        job.schedule();
        
        if (UIUtils.isUIThread()) {
            Display display = UIUtils.getDisplay();
            if (!display.isDisposed()) {
                CompletableFuture<Boolean> shortWaitResult = new CompletableFuture<>();
                Runnable modalShortWait = () -> {
                    try {
                        ModalContext.run(monitor -> { 
                            try {
                                shortWaitResult.complete(!job.join(getLongOperationTime(), new NullProgressMonitor()));
                            } catch (Exception ex) {
                                shortWaitResult.completeExceptionally(ex);
                            }
                        }, true, new NullProgressMonitor(), display);
                    } catch (Exception ex) {
                        shortWaitResult.completeExceptionally(ex);
                    }
                };

                IWorkbench workbench = PlatformUI.isWorkbenchRunning() ? PlatformUI.getWorkbench() : null;
                boolean workbenchInitializing = workbench == null || workbench.isStarting() || workbench.isClosing();

                Shell[] shells = display.getShells();
                if (!workbenchInitializing) {
                    for (Shell shell : shells) {
                        shell.setEnabled(false);
                    }
                }
                try {
                    BusyIndicator.showWhile(display, modalShortWait);
                } finally {
                    if (!workbenchInitializing) {
                        for (Shell shell : display.getShells()) {
                            shell.setEnabled(true);
                        }
                    }
                }
                
                try {
                    if (shortWaitResult.get()) {
                        ProgressMonitorDialog progress = new ProgressMonitorDialog(display.getActiveShell()) {
                            @Override
                            protected void cancelPressed() {
                                job.cancel();
                                super.cancelPressed();
                            }  
                        };
                        
                        progress.run(true, runnable != null, monitor -> {
                            monitor.beginTask(operationDescription, IProgressMonitor.UNKNOWN);
                            job.join();
                            monitor.done();
                        });
                    }
                } catch (Exception ex) {
                    return CompletableFuture.failedFuture(ex);
                }
            }
        }

        try {
            job.join();
        } catch (InterruptedException ex) {
            return CompletableFuture.failedFuture(ex);
        }
        
        return job.getResult().isOK() ? runnable.getResult() : CompletableFuture.failedFuture(job.getResult().getException());
    }
    
    @NotNull
    @Override
    public <RESULT> Job createLoadingService(ILoadService<RESULT> loadingService, ILoadVisualizer<RESULT> visualizer) {
        return LoadingJob.createService(loadingService, visualizer);
    }

    @Override
    public void refreshPartState(Object part) {
        if (part instanceof IWorkbenchPart) {
            UIUtils.asyncExec(() -> DesktopUI.getInstance().refreshPartContexts((IWorkbenchPart)part));
        }
    }

    @Override
    public void copyTextToClipboard(String text, boolean htmlFormat) {
        if (CommonUtils.isEmpty(text)) {
            return;
        }
        UIUtils.syncExec(() -> {

            TextTransfer textTransfer = TextTransfer.getInstance();
            Clipboard clipboard = new Clipboard(UIUtils.getDisplay());
            if (htmlFormat) {
                HTMLTransfer htmlTransfer = HTMLTransfer.getInstance();
                clipboard.setContents(
                    new Object[]{text, text},
                    new Transfer[]{textTransfer, htmlTransfer});
            } else {
                clipboard.setContents(
                    new Object[]{text},
                    new Transfer[]{textTransfer});
            }
        });
    }

    @Override
    public void executeShellProgram(String shellCommand) {
        UIUtils.asyncExec(() -> ShellUtils.launchProgram(shellCommand));
    }

    @Override
    public void showInSystemExplorer(@NotNull String path) {
        UIUtils.asyncExec(() -> ShellUtils.showInSystemExplorer(path));
    }

    @Override
    public DBNPathBase openFileSystemSelector(
        @NotNull String title,
        boolean folder,
        int style,
        boolean binary,
        String[] filterExt,
        String defaultValue
    ) {
        DBNNode object = ObjectBrowserDialog.selectObject(
            UIUtils.getActiveWorkbenchShell(),
            title,
            FileSystemExplorerView.getFileSystemsNode(),
            null,
            null,
            new Class[] { DBNPathBase.class },
            null);
        if (object instanceof DBNPathBase path) {
            return path;
        }
        return null;
    }

    @Override
    public boolean readAndDispatchEvents() {
        if (contextListener == null) {
            // UI not initialized
            return false;
        }
        Display currentDisplay = Display.getCurrent();
        if (currentDisplay != null) {
            if (!currentDisplay.readAndDispatch()) {
                IWorkbench workbench = PlatformUI.getWorkbench();
                if (!workbench.isStarting() && !workbench.isClosing()) {
                    // Do not sleep during startup/shutdown because you may have no chance to get UI event anymore
                    currentDisplay.sleep();
                }
            }
            return true;
        } else {
            return false;
        }
    }
    
    private static long getLongOperationTime() {
        try {
            return PlatformUI.getWorkbench().getProgressService().getLongOperationTime();
        } catch (Exception ex) { // when workbench is not initialized yet during startup
            return 800; // see org.eclipse.ui.internal.progress.ProgressManager.getLongOperationTime()
        }
    }

}
