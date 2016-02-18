/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.core;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.*;
import org.eclipse.ui.services.IDisposable;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.access.DBAAuthInfo;
import org.jkiss.dbeaver.model.runtime.*;
import org.jkiss.dbeaver.runtime.RunnableContextDelegate;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.dbeaver.runtime.ui.DBUICallback;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.AbstractUIJob;
import org.jkiss.dbeaver.ui.SharedTextColors;
import org.jkiss.dbeaver.ui.TrayIconHandler;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.BaseAuthDialog;
import org.jkiss.dbeaver.ui.views.process.ProcessPropertyTester;
import org.jkiss.dbeaver.ui.views.process.ShellProcessView;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * DBeaver UI core
 */
public class DBeaverUI implements DBUICallback {

    static final Log log = Log.getLog(DBeaverUI.class);

    private static DBeaverUI instance;

    private SharedTextColors sharedTextColors;
    private TrayIconHandler trayItem;
    private final List<IDisposable> globalDisposables = new ArrayList<>();

    public static DBeaverUI getInstance()
    {
        if (instance == null) {
            instance = new DBeaverUI();
            instance.initialize();
        }
        return instance;
    }

    static void disposeUI()
    {
        if (instance != null) {
            instance.dispose();
        }
    }

    public static SharedTextColors getSharedTextColors()
    {
        return getInstance().sharedTextColors;
    }

    public static void run(
        IRunnableContext runnableContext,
        boolean fork,
        boolean cancelable,
        final DBRRunnableWithProgress runnableWithProgress)
        throws InvocationTargetException, InterruptedException
    {
        runnableContext.run(fork, cancelable, new IRunnableWithProgress() {
            @Override
            public void run(IProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException
            {
                runnableWithProgress.run(RuntimeUtils.makeMonitor(monitor));
            }
        });
    }

    private void dispose()
    {
        //this.trayItem.dispose();
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

    private void initialize()
    {
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

    public static AbstractUIJob runUIJob(String jobName, final DBRRunnableWithProgress runnableWithProgress)
    {
        return runUIJob(jobName, 0, runnableWithProgress);
    }

    public static AbstractUIJob runUIJob(String jobName, int timeout, final DBRRunnableWithProgress runnableWithProgress)
    {
        AbstractUIJob job = new AbstractUIJob(jobName) {
            @Override
            public IStatus runInUIThread(DBRProgressMonitor monitor)
            {
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
    public static IWorkbenchWindow getActiveWorkbenchWindow()
    {
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

    public static Shell getActiveWorkbenchShell()
    {
        return getActiveWorkbenchWindow().getShell();
    }

    public static Display getDisplay()
    {
        Shell shell = getActiveWorkbenchShell();
        if (shell != null)
            return shell.getDisplay();
        else
            return Display.getDefault();
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

    public static DBRRunnableContext getDefaultRunnableContext()
    {
        IWorkbench workbench = PlatformUI.getWorkbench();
        if (workbench != null && workbench.getActiveWorkbenchWindow() != null) {
            return new RunnableContextDelegate(workbench.getActiveWorkbenchWindow());
        } else {
            return new DBRRunnableContext() {
                @Override
                public void run(boolean fork, boolean cancelable, DBRRunnableWithProgress runnable) throws InvocationTargetException, InterruptedException
                {
                    runnable.run(VoidProgressMonitor.INSTANCE);
                }
            };
        }
    }

    /**
     * Runs task in Eclipse progress service.
     * NOTE: this call can't be canceled if it will block in IO
     */
    public static void runInProgressService(final DBRRunnableWithProgress runnable)
        throws InvocationTargetException, InterruptedException
    {
        getDefaultRunnableContext().run(true, true, new DBRRunnableWithProgress() {
                @Override
                public void run(DBRProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException
                {
                    runnable.run(monitor);
                }
            });
    }

    /**
     * Runs task in Eclipse progress dialog.
     * NOTE: this call can't be canceled if it will block in IO
     */
    public static void runInProgressDialog(final DBRRunnableWithProgress runnable) throws InvocationTargetException
    {
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
                    throws InvocationTargetException, InterruptedException
                {
                    runnable.run(RuntimeUtils.makeMonitor(monitor));
                }
            });
        } catch (InterruptedException e) {
            // do nothing
        }
    }

    public static void runInUI(IRunnableContext context, final DBRRunnableWithProgress runnable)
    {
        try {
            PlatformUI.getWorkbench().getProgressService().runInUI(context, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException
                {
                    runnable.run(RuntimeUtils.makeMonitor(monitor));
                }
            }, DBeaverActivator.getWorkspace().getRoot());
        } catch (InvocationTargetException e) {
            UIUtils.showErrorDialog(null, null, null, e.getTargetException());
        } catch (InterruptedException e) {
            // do nothing
        }
    }

    public static void notifyAgent(String message, int status)
    {
        if (!DBeaverCore.getGlobalPreferenceStore().getBoolean(DBeaverPreferences.AGENT_LONG_OPERATION_NOTIFY)) {
            // Notifications disabled
            return;
        }
        getInstance().trayItem.notify(message, status);
    }

    @Override
    public void showError(@NotNull String title, @Nullable String message, @NotNull IStatus status) {
        UIUtils.showErrorDialog(null, title, message, status);
    }

    @Override
    public void showError(@NotNull String title, @Nullable String message, @NotNull Throwable e) {
        UIUtils.showErrorDialog(null, title, message, e);
    }

    @Override
    public void showError(@NotNull String title, @Nullable String message) {
        UIUtils.showErrorDialog(null, title, message);
    }

    @Override
    public DBAAuthInfo promptUserCredentials(String prompt, String userName, String userPassword) {
        // Ask user
        final Shell shell = DBeaverUI.getActiveWorkbenchShell();
        final BaseAuthDialog authDialog = new BaseAuthDialog(shell, prompt);
        authDialog.setUserName(userName);
        authDialog.setUserPassword(userPassword);
        final RunnableWithResult<Boolean> binder = new RunnableWithResult<Boolean>() {
            @Override
            public void run()
            {
                result = (authDialog.open() == IDialogConstants.OK_ID);
            }
        };
        UIUtils.runInUI(shell, binder);
        if (binder.getResult() != null && binder.getResult()) {
            return authDialog.getAuthInfo();
        } else {
            return null;
        }
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
            getActiveWorkbenchShell().getDisplay().asyncExec(new Runnable() {
                @Override
                public void run()
                {
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
}
