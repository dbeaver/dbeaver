/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime;

import net.sf.jkiss.utils.BeanUtils;
import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.jexl2.Expression;
import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.jexl2.JexlException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISaveablePart;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * RuntimeUtils
 */
public class RuntimeUtils
{
    static final Log log = LogFactory.getLog(RuntimeUtils.class);

    private static JexlEngine jexlEngine;

    public static IStatus makeExceptionStatus(Throwable ex)
    {
        return makeExceptionStatus(IStatus.ERROR, ex);
    }

    public static IStatus makeExceptionStatus(int severity, Throwable ex)
    {
        Throwable cause = ex.getCause();
        if (cause == null) {
            return new Status(
                severity,
                DBeaverCore.getInstance().getPluginID(),
                getExceptionMessage(ex),
                null);
        } else {
            return new MultiStatus(
                DBeaverCore.getInstance().getPluginID(),
                0,
                new IStatus[]{makeExceptionStatus(severity, cause)},
                getExceptionMessage(ex),
                null);
        }
    }

    public static IStatus makeExceptionStatus(String message, Throwable ex)
    {
        return new MultiStatus(
            DBeaverCore.getInstance().getPluginID(),
            0,
            new IStatus[]{makeExceptionStatus(ex)},
            message,
            null);
    }

    public static String getExceptionMessage(Throwable ex)
    {
        StringBuilder msg = new StringBuilder(/*CommonUtils.getShortClassName(ex.getClass())*/);
        if (ex.getMessage() != null) {
            msg.append(ex.getMessage());
        } else {
            msg.append(CommonUtils.getShortClassName(ex.getClass()));
        }
        return msg.toString().trim();
    }

    public static DBRProgressMonitor makeMonitor(IProgressMonitor monitor)
    {
        return new DefaultProgressMonitor(monitor);
    }

    public static void run(
        IRunnableContext runnableContext,
        boolean fork,
        boolean cancelable,
        final DBRRunnableWithProgress runnableWithProgress)
        throws InvocationTargetException, InterruptedException
    {
        runnableContext.run(fork, cancelable, new IRunnableWithProgress() {
            public void run(IProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException
            {
                runnableWithProgress.run(makeMonitor(monitor));
            }
        });
    }

    public static void savePreferenceStore(IPreferenceStore store)
    {
        if (store instanceof IPersistentPreferenceStore) {
            try {
                ((IPersistentPreferenceStore)store).save();
            } catch (IOException e) {
                log.warn(e);
            }
        } else {
            log.debug("Could not save prefernce store '" + store + "' - not a persistent one");
        }
    }

    public static void setDefaultPreferenceValue(IPreferenceStore store, String name, Object value)
    {
        if (!store.contains(name)) {
            store.setValue(name, value.toString());
        }
        store.setDefault(name, value.toString());
    }

    public static File getUserHomeDir()
    {
        String userHome = System.getProperty("user.home");
        if (userHome == null) {
            userHome = ".";
        }
        return new File(userHome);
    }

    public static File getBetaDir()
    {
        return new File(getUserHomeDir(), ".dbeaver-beta/");
    }

    public static String getCurrentDate()
    {
        return new SimpleDateFormat("yyyyMMdd").format(new Date());
/*
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        final int month = c.get(Calendar.MONTH) + 1;
        final int day = c.get(Calendar.DAY_OF_MONTH);
        return "" + c.get(Calendar.YEAR) + (month < 10 ? "0" + month : month) + (day < 10 ? "0" + day : day);
*/
    }

    public static String getCurrentTimeStamp()
    {
        return new SimpleDateFormat("yyyyMMddhhmm").format(new Date());
/*
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        final int month = c.get(Calendar.MONTH) + 1;
        return "" + c.get(Calendar.YEAR) + (month < 10 ? "0" + month : month) + c.get(Calendar.DAY_OF_MONTH) + c.get(Calendar.HOUR_OF_DAY) + c.get(Calendar.MINUTE);
*/
    }

    public static boolean validateAndSave(DBRProgressMonitor monitor, ISaveablePart saveable)
    {
        if (!saveable.isDirty()) {
            return true;
        }
        SaveRunner saveRunner = new SaveRunner(monitor, saveable);
        Display.getDefault().syncExec(saveRunner);
        return saveRunner.getResult();
    }

    public static Expression parseExpression(String exprString) throws DBException
    {
        synchronized (RuntimeUtils.class) {
            if (jexlEngine == null) {
                jexlEngine = new JexlEngine(null, null, null, log);
                jexlEngine.setCache(100);
            }
        }
        try {
            return jexlEngine.createExpression(exprString);
        } catch (JexlException e) {
            throw new DBException(e);
        }
    }

    private static class SaveRunner implements Runnable {
        private final DBRProgressMonitor monitor;
        private final ISaveablePart saveable;
        private boolean result;

        private SaveRunner(DBRProgressMonitor monitor, ISaveablePart saveable)
        {
            this.monitor = monitor;
            this.saveable = saveable;
        }

        public boolean getResult()
        {
            return result;
        }

        public void run()
        {
            int choice = -1;
            if (saveable instanceof ISaveablePart2) {
                choice = ((ISaveablePart2)saveable).promptToSaveOnClose();
            }
            if (choice == -1 || choice == ISaveablePart2.DEFAULT) {
                Shell shell;
                String saveableName;
                if (saveable instanceof IWorkbenchPart) {
                    shell = ((IWorkbenchPart) saveable).getSite().getShell();
                    saveableName = ((IWorkbenchPart) saveable).getTitle();
                } else {
                    shell = DBeaverCore.getActiveWorkbenchShell();
                    saveableName = "Object";
                }
                int confirmResult = ConfirmationDialog.showConfirmDialog(
                    shell,
                    PrefConstants.CONFIRM_EDITOR_CLOSE,
                    ConfirmationDialog.QUESTION_WITH_CANCEL,
                    saveableName);
                switch (confirmResult) {
                    case IDialogConstants.YES_ID: choice = ISaveablePart2.YES; break;
                    case IDialogConstants.NO_ID: choice = ISaveablePart2.NO; break;
                    default: choice = ISaveablePart2.CANCEL; break;
                }
            }
            switch (choice) {
                case ISaveablePart2.YES : //yes
                    saveable.doSave(monitor.getNestedMonitor());
                    result = !saveable.isDirty();
                    break;
                case ISaveablePart2.NO : //no
                    result = true;
                    break;
                case ISaveablePart2.CANCEL : //cancel
                default :
                    result = false;
                    break;
            }
        }
    }

}
