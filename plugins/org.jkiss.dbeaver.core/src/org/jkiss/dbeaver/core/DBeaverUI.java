/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.HTMLTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.services.IDisposable;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.access.DBAPasswordChangeInfo;
import org.jkiss.dbeaver.model.connection.DBPAuthInfo;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverDependencies;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProcessDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProcessListener;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.ILoadService;
import org.jkiss.dbeaver.model.runtime.load.ILoadVisualizer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.DBeaverNotifications;
import org.jkiss.dbeaver.runtime.ui.DBPPlatformUI;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.actions.datasource.DataSourceInvalidateHandler;
import org.jkiss.dbeaver.ui.dialogs.*;
import org.jkiss.dbeaver.ui.dialogs.connection.PasswordChangeDialog;
import org.jkiss.dbeaver.ui.dialogs.driver.DriverDownloadDialog;
import org.jkiss.dbeaver.ui.dialogs.driver.DriverEditDialog;
import org.jkiss.dbeaver.ui.dialogs.exec.ExecutionQueueErrorJob;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.dbeaver.ui.navigator.actions.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.ui.navigator.dialogs.ObjectBrowserDialog;
import org.jkiss.dbeaver.ui.views.process.ProcessPropertyTester;
import org.jkiss.dbeaver.ui.views.process.ShellProcessView;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * DBeaver UI core
 */
public class DBeaverUI implements DBPPlatformUI {

    private static final Log log = Log.getLog(DBeaverUI.class);

    private static DBeaverUI instance;

    private TrayIconHandler trayItem;
    private final List<IDisposable> globalDisposables = new ArrayList<>();
    private WorkbenchContextListener contextListener;

    public static DBeaverUI getInstance() {
        if (instance == null) {
            instance = new DBeaverUI();
            instance.initialize();
        }
        return instance;
    }

    static void disposeUI() {
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

        List<IDisposable> dispList = new ArrayList<>(globalDisposables);
        Collections.reverse(dispList);
        for (IDisposable disp : dispList) {
            try {
                disp.dispose();
            } catch (Exception e) {
                log.error(e);
            }
            globalDisposables.remove(disp);
        }
    }

    private void initialize() {
        this.trayItem = new TrayIconHandler();

        if (DBeaverCore.isStandalone()) {
            //Policy.setErrorSupportProvider(new ApplicationErrorSupportProvider());
        }

        // Register context listener
        UIUtils.asyncExec(() -> contextListener = WorkbenchContextListener.registerInWorkbench());
        
/*      // Global focus lister for debug
        Display.getCurrent().addFilter(SWT.FocusIn, new Listener() {
            @Override
            public void handleEvent(Event event) {
                System.out.println("FOCUS TO: " + event.widget);
            }
        });
*/
    }

    public void refreshPartContexts(IWorkbenchPart part) {
        if (contextListener != null) {
            contextListener.deactivatePartContexts(part);
            contextListener.activatePartContexts(part);
        }
    }

    @Override
    public void notifyAgent(String message, int status) {
        if (!ModelPreferences.getPreferences().getBoolean(DBeaverPreferences.AGENT_LONG_OPERATION_NOTIFY)) {
            // Notifications disabled
            return;
        }
        if (TrayIconHandler.isSupported()) {
            getInstance().trayItem.notify(message, status);
        } else {
            DBeaverNotifications.showNotification(
                "agentNotify",
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
    public boolean downloadDriverFiles(DBPDriver driver, DBPDriverDependencies dependencies) {
        return new UITask<Boolean>() {
            @Override
            protected Boolean runTask() {
                return DriverDownloadDialog.downloadDriverFiles(null, driver, dependencies);
            }
        }.execute();
    }

    @Override
    public UserResponse showError(@NotNull final String title, @Nullable final String message, @NotNull final IStatus status) {
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
        log.error(rootStatus.getMessage(), rootStatus.getException());

        // log.debug(message);
        Runnable runnable = () -> {
            // Display the dialog
            StandardErrorDialog dialog = new StandardErrorDialog(UIUtils.getActiveWorkbenchShell(),
                    title, message, status, IStatus.ERROR);
            dialog.open();
        };
        UIUtils.syncExec(runnable);
        return UserResponse.OK;
    }

    @Override
    public UserResponse showError(@NotNull String title, @Nullable String message, @NotNull Throwable error) {
        return showError(title, message, GeneralUtils.makeExceptionStatus(error));
    }

    @Override
    public UserResponse showError(@NotNull String title, @Nullable String message) {
        return showError(title, null, new Status(IStatus.ERROR, DBeaverCore.PLUGIN_ID, message));
    }

    @Override
    public void showMessageBox(@NotNull String title, String message, boolean error) {
        if (error) {
            showMessageBox(title, message, DBIcon.STATUS_ERROR);
        } else {
            showMessageBox(title, message, DBIcon.STATUS_INFO);
        }
    }

    @Override
    public void showWarningMessageBox(@NotNull String title, String message) {
        showMessageBox(title, message, DBIcon.STATUS_WARNING);
    }

    private static void showMessageBox(@NotNull String title, @NotNull String message, @NotNull DBPImage image) {
        UIUtils.syncExec(() -> MessageBoxBuilder.builder(UIUtils.getActiveWorkbenchShell())
            .setTitle(title)
            .setMessage(message)
            .setPrimaryImage(image)
            .setReplies(Reply.OK)
            .showMessageBox()
        );
    }

    @Override
    public boolean confirmAction(String title, String message) {
        return UIUtils.confirmAction(title, message);
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
        DBPDataSource dataSource = error.getDataSource();
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
                DriverEditDialog.showBadConfigDialog(null, message, error);
                return UserResponse.OK;
        }

        return null;
    }

    @Override
    public DBPAuthInfo promptUserCredentials(final String prompt, final String userName, final String userPassword, final boolean passwordOnly, boolean showSavePassword) {
        return promptUserCredentials(prompt,
            UIConnectionMessages.dialog_connection_auth_label_username,
            userName,
            UIConnectionMessages.dialog_connection_auth_label_password,
            userPassword,
            passwordOnly,
            showSavePassword);
    }

    @Override
    public DBPAuthInfo promptUserCredentials(String prompt, String userNameLabel, String userName, String passwordLabel, String userPassword, boolean passwordOnly, boolean showSavePassword) {
        return new UITask<DBPAuthInfo>() {
            @Override
            public DBPAuthInfo runTask() {
                final Shell shell = UIUtils.getActiveWorkbenchShell();
                final BaseAuthDialog authDialog = new BaseAuthDialog(shell, prompt, passwordOnly, showSavePassword);
                authDialog.setUserNameLabel(userNameLabel);
                authDialog.setPasswordLabel(passwordLabel);
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
    public DBNNode selectObject(@NotNull Object parentShell, String title, DBNNode rootNode, DBNNode selectedNode, Class<?>[] allowedTypes, Class<?>[] resultTypes, Class<?>[] leafTypes) {
        Shell shell = (parentShell instanceof Shell ? (Shell)parentShell : UIUtils.getActiveWorkbenchShell());
        return ObjectBrowserDialog.selectObject(shell, title, rootNode, selectedNode, allowedTypes, resultTypes, leafTypes);
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
        // Otherwise it breaks soem functions (e.g. data editor value save as it handles focus events).
        // so we can use runInProgressServie function
        runnable.run(new VoidProgressMonitor());
    }

    @NotNull
    @Override
    public <RESULT> Job createLoadingService(ILoadService<RESULT> loadingService, ILoadVisualizer<RESULT> visualizer) {
        return LoadingJob.createService(loadingService, visualizer);
    }

    @Override
    public void refreshPartState(Object part) {
        if (part instanceof IWorkbenchPart) {
            UIUtils.asyncExec(() -> DBeaverUI.getInstance().refreshPartContexts((IWorkbenchPart)part));
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
    public boolean readAndDispatchEvents() {
        Display currentDisplay = Display.getCurrent();
        if (currentDisplay != null) {
            if (!currentDisplay.readAndDispatch()) {
                currentDisplay.sleep();
            }
            return true;
        } else {
            return false;
        }
    }

}
