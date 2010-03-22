package org.jkiss.dbeaver.ui;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.core.DBeaverCore;

/**
 * DBeaverUtils
 */
public class DBeaverUtils
{
    static Log log = LogFactory.getLog(DBeaverUtils.class);

    public static void showErrorDialog(
        Shell shell,
        String title,
        String message,
        Throwable error)
    {
        log.error(message, error);

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
        Throwable cause = ex.getCause();
        if (cause == null) {
            return new Status(
                IStatus.ERROR,
                DBeaverCore.getInstance().getPluginID(),
                getExceptionMessage(ex),
                null);
        } else {
            return new MultiStatus(
                DBeaverCore.getInstance().getPluginID(),
                0,
                new IStatus[]{makeExceptionStatus(cause)},
                getExceptionMessage(ex),
                null);
        }
    }

    public static String getExceptionMessage(Throwable ex)
    {
        StringBuilder msg = new StringBuilder(CommonUtils.getShortClassName(ex.getClass()));
        if (ex.getMessage() != null) {
            msg.append(" - ").append(ex.getMessage());
        }
        return msg.toString();
    }
}
