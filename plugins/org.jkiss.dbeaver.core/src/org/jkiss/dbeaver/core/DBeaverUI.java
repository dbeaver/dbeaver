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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.*;
import org.eclipse.ui.services.IDisposable;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPErrorAssistant;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.access.DBAAuthInfo;
import org.jkiss.dbeaver.model.runtime.*;
import org.jkiss.dbeaver.runtime.DummyRunnableContext;
import org.jkiss.dbeaver.runtime.RunnableContextDelegate;
import org.jkiss.dbeaver.runtime.ui.DBPPlatformUI;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.actions.datasource.DataSourceInvalidateHandler;
import org.jkiss.dbeaver.ui.dialogs.StandardErrorDialog;
import org.jkiss.dbeaver.ui.dialogs.connection.BaseAuthDialog;
import org.jkiss.dbeaver.ui.dialogs.driver.DriverEditDialog;
import org.jkiss.dbeaver.ui.views.process.ProcessPropertyTester;
import org.jkiss.dbeaver.ui.views.process.ShellProcessView;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;

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

    private SharedTextColors sharedTextColors;
    private TrayIconHandler trayItem;
    private final List<IDisposable> globalDisposables = new ArrayList<>();

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

    public static SharedTextColors getSharedTextColors() {
        return getInstance().sharedTextColors;
    }

    public static void run(
        IRunnableContext runnableContext,
        boolean fork,
        boolean cancelable,
        final DBRRunnableWithProgress runnableWithProgress)
        throws InvocationTargetException, InterruptedException {
        runnableContext.run(fork, cancelable, new IRunnableWithProgress() {
            @Override
            public void run(IProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException {
                runnableWithProgress.run(RuntimeUtils.makeMonitor(monitor));
            }
        });
    }

    private void dispose() {
        this.sharedTextColors.dispose();

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
        this.sharedTextColors = new SharedTextColors();
        this.trayItem = new TrayIconHandler();
        DBUserInterface.setInstance(this);

        // Register context listener
        WorkbenchContextListener.registerInWorkbench();

/*      // Global focus lister for debug
        Display.getCurrent().addFilter(SWT.FocusIn, new Listener() {
            @Override
            public void handleEvent(Event event) {
                System.out.println("FOCUS TO: " + event.widget);
            }
        });
*/
    }

    public static AbstractUIJob runUIJob(String jobName, final DBRRunnableWithProgress runnableWithProgress) {
        return runUIJob(jobName, 0, runnableWithProgress);
    }

    public static AbstractUIJob runUIJob(String jobName, int timeout, final DBRRunnableWithProgress runnableWithProgress) {
        AbstractUIJob job = new AbstractUIJob(jobName) {
            @Override
            public IStatus runInUIThread(DBRProgressMonitor monitor) {
                try {
                    runnableWithProgress.run(monitor);
                } catch (InvocationTargetException e) {
                    return GeneralUtils.makeExceptionStatus(e);
                } catch (InterruptedException e) {
                    return Status.CANCEL_STATUS;
                }
                return Status.OK_STATUS;
            }
        };
        job.setSystem(true);
        job.schedule(timeout);
        return job;
    }

    @NotNull
    public static IWorkbenchWindow getActiveWorkbenchWindow() {
        IWorkbench workbench = PlatformUI.getWorkbench();
        IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
        if (window != null) {
            return window;
        }
        IWorkbenchWindow[] windows = workbench.getWorkbenchWindows();
        if (windows.length > 0) {
            return windows[0];
        }
        throw new IllegalStateException("No workbench window");
    }

    public static Shell getActiveWorkbenchShell() {
        IWorkbench workbench = PlatformUI.getWorkbench();
        IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
        if (window != null) {
            return window.getShell();
        } else {
            return Display.getDefault().getActiveShell();
        }
    }

    public static Display getDisplay() {
        IWorkbench workbench = PlatformUI.getWorkbench();
        if (workbench != null) {
            return workbench.getDisplay();
        } else {
            return Display.getDefault();
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

    public static DBRRunnableContext getDefaultRunnableContext() {
        IWorkbench workbench = PlatformUI.getWorkbench();
        if (workbench != null && workbench.getActiveWorkbenchWindow() != null) {
            return new RunnableContextDelegate(workbench.getActiveWorkbenchWindow());
        } else {
            return new DBRRunnableContext() {
                @Override
                public void run(boolean fork, boolean cancelable, DBRRunnableWithProgress runnable) throws InvocationTargetException, InterruptedException {
                    runnable.run(new VoidProgressMonitor());
                }
            };
        }
    }

    /**
     * Runs task in Eclipse progress service.
     * NOTE: this call can't be canceled if it will block in IO
     */
    public static void runInProgressService(final DBRRunnableWithProgress runnable)
        throws InvocationTargetException, InterruptedException {
        getDefaultRunnableContext().run(true, true, new DBRRunnableWithProgress() {
            @Override
            public void run(DBRProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException {
                runnable.run(monitor);
            }
        });
    }

    /**
     * Runs task in Eclipse progress dialog.
     * NOTE: this call can't be canceled if it will block in IO
     */
    public static void runInProgressDialog(final DBRRunnableWithProgress runnable) throws InvocationTargetException {
        try {
            IRunnableContext runnableContext;
            IWorkbench workbench = PlatformUI.getWorkbench();
            IWorkbenchWindow workbenchWindow = workbench.getActiveWorkbenchWindow();
            if (workbenchWindow != null) {
                runnableContext = new ProgressMonitorDialog(workbench.getActiveWorkbenchWindow().getShell());
            } else {
                runnableContext = workbench.getProgressService();
            }
            runnableContext.run(true, true, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException {
                    runnable.run(RuntimeUtils.makeMonitor(monitor));
                }
            });
        } catch (InterruptedException e) {
            // do nothing
        }
    }

    public static void runInUI(IRunnableContext context, final DBRRunnableWithProgress runnable) {
        try {
            PlatformUI.getWorkbench().getProgressService().runInUI(context, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException {
                    runnable.run(RuntimeUtils.makeMonitor(monitor));
                }
            }, DBeaverActivator.getWorkspace().getRoot());
        } catch (InvocationTargetException e) {
            DBUserInterface.getInstance().showError(null, null, e.getTargetException());
        } catch (InterruptedException e) {
            // do nothing
        }
    }

    public static void runInUI(final DBRRunnableWithProgress runnable) {
        IWorkbench workbench = PlatformUI.getWorkbench();
        IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
        IRunnableContext context = window != null ? window : DummyRunnableContext.INSTANCE;
        runInUI(context, runnable);
    }

    public static void asyncExec(Runnable runnable) {
        try {
            getDisplay().asyncExec(runnable);
        } catch (Exception e) {
            log.debug(e);
        }
    }

    public static void syncExec(Runnable runnable) {
        try {
            getDisplay().syncExec(runnable);
        } catch (Exception e) {
            log.debug(e);
        }
    }

    public static <T> T syncExec(RunnableWithResult<T> runnable) {
        try {
            getDisplay().syncExec(runnable);
            return runnable.getResult();
        } catch (Exception e) {
            log.debug(e);
            return null;
        }
    }

    public static void notifyAgent(String message, int status) {
        if (!DBeaverCore.getGlobalPreferenceStore().getBoolean(DBeaverPreferences.AGENT_LONG_OPERATION_NOTIFY)) {
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
        Runnable runnable = new Runnable() {
            @Override
            public void run()
            {
                // Display the dialog
                StandardErrorDialog dialog = new StandardErrorDialog(DBeaverUI.getActiveWorkbenchShell(),
                        title, message, RuntimeUtils.stripStack(status), IStatus.ERROR);
                dialog.open();
            }
        };
        DBeaverUI.syncExec(runnable);
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
    public DBAAuthInfo promptUserCredentials(final String prompt, final String userName, final String userPassword, final boolean passwordOnly) {

        // Ask user
        return new UITask<DBAAuthInfo>() {
            @Override
            public DBAAuthInfo runTask() {
                final Shell shell = DBeaverUI.getActiveWorkbenchShell();
                final BaseAuthDialog authDialog = new BaseAuthDialog(shell, prompt, passwordOnly);
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
            asyncExec(new Runnable() {
                @Override
                public void run() {
                    try {
                        final ShellProcessView processView =
                            (ShellProcessView) DBeaverUI.getActiveWorkbenchWindow().getActivePage().showView(
                                ShellProcessView.VIEW_ID,
                                ShellProcessView.getNextId(),
                                IWorkbenchPage.VIEW_VISIBLE
                            );
                        processView.initProcess(processDescriptor);
                    } catch (PartInitException e) {
                        log.error(e);
                    }
                }
            });
        }
    }

    @Override
    public void executeInUI(Runnable runnable) {
        syncExec(runnable);
    }
}
