package org.jkiss.dbeaver.core;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TrayItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.runtime.AbstractUIJob;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.SharedTextColors;

import java.lang.reflect.InvocationTargetException;

/**
 * DBeaver UI core
 */
public class DBeaverUI {

    private static DBeaverUI instance;

    private SharedTextColors sharedTextColors;
    private TrayItem trayItem;

    static void initializeUI()
    {
        instance = new DBeaverUI();
        instance.initialize();
    }

    static void disposeUI()
    {
        if (instance != null) {
            instance.dispose();
        }
    }

    public static ISharedTextColors getSharedTextColors()
    {
        return instance.sharedTextColors;
    }

    private void dispose()
    {
        this.trayItem.dispose();
        this.sharedTextColors.dispose();
    }

    private void initialize()
    {
        DBeaverIcons.initRegistry(DBeaverActivator.getInstance().getBundle());

        //progressProvider = new DBeaverProgressProvider();
        this.sharedTextColors = new SharedTextColors();

        trayItem = new TrayItem(Display.getDefault().getSystemTray(), SWT.NONE);
        trayItem.setText(DBeaverCore.getProductTitle());
        trayItem.setImage(DBIcon.DBEAVER_LOGO.getImage());
        trayItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e)
            {
            }
        });
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

    public static void runInProgressDialog(final DBRRunnableWithProgress runnable) throws InterruptedException, InvocationTargetException
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

    public static void runInProgressService(final DBRRunnableWithProgress runnable)
        throws InvocationTargetException, InterruptedException
    {
        IWorkbench workbench = PlatformUI.getWorkbench();
        if (workbench == null || workbench.getProgressService() == null) {
            runnable.run(VoidProgressMonitor.INSTANCE);
        } else {
            workbench.getProgressService().run(true, true, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException
                {
                    runnable.run(RuntimeUtils.makeMonitor(monitor));
                }
            });
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
            DBeaverCore.log.error(e.getTargetException());
        } catch (InterruptedException e) {
            // do nothing
        }
    }
}
