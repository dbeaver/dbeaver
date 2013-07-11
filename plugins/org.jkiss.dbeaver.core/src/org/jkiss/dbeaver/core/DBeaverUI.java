package org.jkiss.dbeaver.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.runtime.AbstractUIJob;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.SharedTextColors;
import org.jkiss.dbeaver.ui.TrayIconHandler;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;
import org.osgi.framework.Bundle;

import java.lang.reflect.InvocationTargetException;

/**
 * DBeaver UI core
 */
public class DBeaverUI {

    static final Log log = LogFactory.getLog(DBeaverUI.class);

    private static DBeaverUI instance;

    private SharedTextColors sharedTextColors;
    private TrayIconHandler trayItem;

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

    public static ISharedTextColors getSharedTextColors()
    {
        return getInstance().sharedTextColors;
    }

    private void dispose()
    {
        //this.trayItem.dispose();
        this.sharedTextColors.dispose();

        if (trayItem != null) {
            trayItem.hide();
        }
    }

    private void initialize()
    {
        Bundle coreBundle = DBeaverActivator.getInstance().getBundle();
        DBeaverIcons.initRegistry(coreBundle);

        this.sharedTextColors = new SharedTextColors();

        this.trayItem = new TrayIconHandler();
    }

    public static AbstractUIJob runUIJob(String jobName, final DBRRunnableWithProgress runnableWithProgress)
    {
        AbstractUIJob job = new AbstractUIJob(jobName) {
            @Override
            public IStatus runInUIThread(DBRProgressMonitor monitor)
            {
                try {
                    runnableWithProgress.run(monitor);
                } catch (InvocationTargetException e) {
                    return RuntimeUtils.makeExceptionStatus(e);
                } catch (InterruptedException e) {
                    return Status.CANCEL_STATUS;
                }
                return Status.OK_STATUS;
            }
        };
        job.schedule();
        return job;
    }

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
        return null;
    }

    public static Shell getActiveWorkbenchShell()
    {
        IWorkbenchWindow window = getActiveWorkbenchWindow();
        if (window != null) {
            return window.getShell();
        }
        return null;
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

    public static IRunnableContext getDefaultRunnableContext()
    {
        IWorkbench workbench = PlatformUI.getWorkbench();
        if (workbench != null && workbench.getActiveWorkbenchWindow() != null) {
            return workbench.getActiveWorkbenchWindow();
        } else {
            return new IRunnableContext() {
                @Override
                public void run(boolean fork, boolean cancelable, IRunnableWithProgress runnable) throws InvocationTargetException, InterruptedException
                {
                    runnable.run(new NullProgressMonitor());
                }
            };
        }
    }

    public static void runInProgressService(final DBRRunnableWithProgress runnable)
        throws InvocationTargetException, InterruptedException
    {
        getDefaultRunnableContext().run(true, true, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException
                {
                    runnable.run(RuntimeUtils.makeMonitor(monitor));
                }
            });
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
            log.error(e.getTargetException());
        } catch (InterruptedException e) {
            // do nothing
        }
    }

    public static void notifyAgent(String message, int status)
    {
        if (!DBeaverCore.getGlobalPreferenceStore().getBoolean(PrefConstants.AGENT_LONG_OPERATION_NOTIFY)) {
            // Notifications disabled
            return;
        }
        getInstance().trayItem.notify(message, status);
    }

}
