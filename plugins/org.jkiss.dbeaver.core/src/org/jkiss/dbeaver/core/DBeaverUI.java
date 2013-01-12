package org.jkiss.dbeaver.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
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
import org.jkiss.dbeaver.ui.preferences.PrefConstants;
import org.osgi.framework.Bundle;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;

/**
 * DBeaver UI core
 */
public class DBeaverUI {

    static final Log log = LogFactory.getLog(DBeaverUI.class);

    private static DBeaverUI instance;

    private SharedTextColors sharedTextColors;
    // AWT tray icon. SWT TrayItem do not support displayMessage function
    private TrayIcon trayItem;

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
        //this.trayItem.dispose();
        this.sharedTextColors.dispose();

        closeTrayIcon();
    }

    private void initialize()
    {
        Bundle coreBundle = DBeaverActivator.getInstance().getBundle();
        DBeaverIcons.initRegistry(coreBundle);

        this.sharedTextColors = new SharedTextColors();

        if (DBeaverCore.getGlobalPreferenceStore().getBoolean(PrefConstants.AGENT_ENABLED)) {
            createTrayIcon(coreBundle);
        }
    }

    private void createTrayIcon(Bundle coreBundle)
    {
        URL logoURL = coreBundle.getEntry(DBIcon.DBEAVER_LOGO.getPath());
        trayItem = new TrayIcon(Toolkit.getDefaultToolkit().getImage(logoURL));
        trayItem.setImageAutoSize(true);
        trayItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {

            }
        });
        trayItem.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e)
            {
            }
        });


        // Add tooltip and menu to tray icon
        trayItem.setToolTip(DBeaverCore.getProductTitle());

        // Add the trayIcon to system tray/notification
        // area
        try {
            SystemTray.getSystemTray().add(trayItem);
        } catch (AWTException e) {
            log.error(e);
        }
    }

    private void closeTrayIcon()
    {
        if (trayItem != null) {
            SystemTray.getSystemTray().remove(trayItem);
        }
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

    public static void notifyAgent(String message, int status)
    {
        if (instance.trayItem == null || !DBeaverCore.getGlobalPreferenceStore().getBoolean(PrefConstants.AGENT_LONG_OPERATION_NOTIFY)) {
            // Notifications disabled
            return;
        }
        TrayIcon.MessageType type;
        switch (status) {
            case IStatus.INFO: type = TrayIcon.MessageType.INFO; break;
            case IStatus.ERROR: type = TrayIcon.MessageType.ERROR; break;
            case IStatus.WARNING: type = TrayIcon.MessageType.WARNING; break;
            default: type = TrayIcon.MessageType.NONE; break;
        }
        instance.trayItem.displayMessage(DBeaverCore.getProductTitle(), message, type);
    }

}
