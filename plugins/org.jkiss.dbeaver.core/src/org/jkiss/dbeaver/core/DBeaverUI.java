/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.eclipse.swt.SWT;
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
import org.jkiss.dbeaver.model.access.DBAAuthInfo;
import org.jkiss.dbeaver.model.access.DBAPasswordChangeInfo;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProcessDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProcessListener;
import org.jkiss.dbeaver.model.runtime.load.ILoadService;
import org.jkiss.dbeaver.model.runtime.load.ILoadVisualizer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.ui.DBPPlatformUI;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.TrayIconHandler;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.datasource.DataSourceInvalidateHandler;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.ui.dialogs.BrowseObjectDialog;
import org.jkiss.dbeaver.ui.dialogs.StandardErrorDialog;
import org.jkiss.dbeaver.ui.dialogs.connection.BaseAuthDialog;
import org.jkiss.dbeaver.ui.dialogs.connection.PasswordChangeDialog;
import org.jkiss.dbeaver.ui.dialogs.driver.DriverEditDialog;
import org.jkiss.dbeaver.ui.dialogs.sql.ViewSQLDialog;
import org.jkiss.dbeaver.ui.views.process.ProcessPropertyTester;
import org.jkiss.dbeaver.ui.views.process.ShellProcessView;
import org.jkiss.dbeaver.utils.GeneralUtils;

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
        DBUserInterface.setInstance(this);

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

    /*
    public static void runWithProgress(IWorkbenchPartSite site, final DBRRunnableWithProgress runnable)
        throws InvocationTargetException, InterruptedException
    {
        IActionBars actionBars = null;
        if (site instanceof IViewSite) {
            actionBars = ((IViewSite) site).getActionBars();
        } else if (site instanceof IEditorSite) {
            actionBars = ((IEditorSite) site).getActionBars();
        }
        IStatusLineManager statusLineManager = null;
        if (actionBars != null) {
            statusLineManager = actionBars.getStatusLineManager();
        }
        if (statusLineManager == null) {
            runInProgressService(runnable);
        } else {
            IProgressMonitor progressMonitor = statusLineManager.getProgressMonitor();
            runnable.run(new DefaultProgressMonitor(progressMonitor));
        }
    }
*/

    @Override
    public void notifyAgent(String message, int status) {
        if (!ModelPreferences.getPreferences().getBoolean(DBeaverPreferences.AGENT_LONG_OPERATION_NOTIFY)) {
            // Notifications disabled
            return;
        }
        getInstance().trayItem.notify(message, status);
    }

    @Override
    public UserResponse showError(@NotNull final String title, @Nullable final String message, @NotNull final IStatus status) {
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
                s = s.getChildren()[0];
            } else {
                break;
            }
        }
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
        log.error(error);

        return showError(title, message, GeneralUtils.makeExceptionStatus(error));
    }

    @Override
    public UserResponse showError(@NotNull String title, @Nullable String message) {
        return showError(title, null, new Status(IStatus.ERROR, DBeaverCore.PLUGIN_ID, message));
    }

    @Override
    public void showMessageBox(String title, String message, boolean error) {
        UIUtils.showMessageBox(
            UIUtils.getActiveWorkbenchShell(),
            title,
            message,
            error ? SWT.ICON_ERROR : SWT.ICON_INFORMATION);
    }

    @Override
    public long getLongOperationTimeout() {
        return DBeaverCore.getGlobalPreferenceStore().getLong(DBeaverPreferences.AGENT_LONG_OPERATION_TIMEOUT);
    }

    private static UserResponse showDatabaseError(String message, DBException error)
    {
        DBPDataSource dataSource = error.getDataSource();
        DBPErrorAssistant.ErrorType errorType = dataSource == null ? DBPErrorAssistant.ErrorType.NORMAL : DBUtils.discoverErrorType(dataSource, error);
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
    public DBAAuthInfo promptUserCredentials(final String prompt, final String userName, final String userPassword, final boolean passwordOnly, boolean showSavePassword) {

        // Ask user
        return new UITask<DBAAuthInfo>() {
            @Override
            public DBAAuthInfo runTask() {
                final Shell shell = UIUtils.getActiveWorkbenchShell();
                final BaseAuthDialog authDialog = new BaseAuthDialog(shell, prompt, passwordOnly, showSavePassword);
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
    public DBAPasswordChangeInfo promptUserPasswordChange(String prompt, String userName, String oldPassword) {
        // Ask user
        return new UITask<DBAPasswordChangeInfo>() {
            @Override
            public DBAPasswordChangeInfo runTask() {
                final Shell shell = UIUtils.getActiveWorkbenchShell();
                final PasswordChangeDialog passwordChangeDialog = new PasswordChangeDialog(shell, prompt, userName, oldPassword);
                if (passwordChangeDialog.open() == IDialogConstants.OK_ID) {
                    return passwordChangeDialog.getPasswordInfo();
                } else {
                    return null;
                }
            }
        }.execute();
    }

    @Override
    public DBNNode selectObject(Object parentShell, String title, DBNNode rootNode, DBNNode selectedNode, Class<?>[] allowedTypes, Class<?>[] resultTypes, Class<?>[] leafTypes) {
        Shell shell = (parentShell instanceof Shell ? (Shell)parentShell : UIUtils.getActiveWorkbenchShell());
        return BrowseObjectDialog.selectObject(shell, title, rootNode, selectedNode, allowedTypes, resultTypes, leafTypes);
    }

    @Override
    public void openEntityEditor(DBSObject object) {
        NavigatorHandlerObjectOpen.openEntityEditor(object);
    }

    @Override
    public void openEntityEditor(DBNNode selectedNode, String defaultPageId) {
        NavigatorHandlerObjectOpen.openEntityEditor(selectedNode, defaultPageId, UIUtils.getActiveWorkbenchWindow());
    }

    @Override
    public void openSQLViewer(DBCExecutionContext context, String title, DBPImage image, String text) {
        ViewSQLDialog dialog = new ViewSQLDialog(
            UIUtils.getActiveWorkbenchWindow().getActivePage().getActivePart().getSite(),
            context,
            title,
            image,
            text
        );
        dialog.open();
    }

    @Override
    public void openConnectionEditor(DBPDataSourceContainer dataSourceContainer) {
        UIUtils.syncExec(() ->
            NavigatorHandlerObjectOpen.openConnectionEditor(
                UIUtils.getActiveWorkbenchWindow(),
                (DataSourceDescriptor) dataSourceContainer));
    }

    @Override
    public void executeProcess(final DBRProcessDescriptor processDescriptor) {
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
    public void executeInUI(Runnable runnable) {
        UIUtils.syncExec(runnable);
    }

    @Override
    public <RESULT> Job createLoadingService(ILoadService<RESULT> loadingService, ILoadVisualizer<RESULT> visualizer) {
        return LoadingJob.createService(loadingService, visualizer);
    }
}
