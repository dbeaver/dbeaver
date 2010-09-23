/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.utils;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.runtime.DefaultProgressMonitor;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * DBeaverUtils
 */
public class DBeaverUtils
{
    static final Log log = LogFactory.getLog(DBeaverUtils.class);

    public static void showErrorDialog(
        Shell shell,
        String title,
        String message,
        Throwable error)
    {
        log.error(error);

        // Display the dialog
        ErrorDialog.openError(
            shell,
            title,
            message,
            makeExceptionStatus(error));
    }

    public static void showErrorDialog(
        Shell shell,
        String title,
        String message)
    {
        log.error(message);
        // Display the dialog
        ErrorDialog.openError(
            shell,
            title,
            null,//message,
            new Status(IStatus.ERROR, DBeaverCore.getInstance().getPluginID(), message));
    }


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
}
