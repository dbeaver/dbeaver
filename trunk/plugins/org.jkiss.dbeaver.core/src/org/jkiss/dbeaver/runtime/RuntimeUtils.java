/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.runtime;

import org.apache.commons.jexl2.Expression;
import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.jexl2.JexlException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.core.Log;
import org.jkiss.dbeaver.model.runtime.DBRProcessDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.runtime.DBRShellCommand;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.views.process.ShellProcessView;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * RuntimeUtils
 */
public class RuntimeUtils {
    static final Log log = Log.getLog(RuntimeUtils.class);

    private static JexlEngine jexlEngine;

    @SuppressWarnings("unchecked")
    public static <T> T getObjectAdapter(Object adapter, Class<T> objectType)
    {
        return (T) Platform.getAdapterManager().getAdapter(adapter, objectType);
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
                DBeaverCore.getCorePluginID(),
                getExceptionMessage(ex),
                ex);
        } else {
            if (ex instanceof DBException && CommonUtils.equalObjects(ex.getMessage(), cause.getMessage())) {
                // Skip empty duplicate DBException
                return makeExceptionStatus(cause);
            }
            return new MultiStatus(
                DBeaverCore.getCorePluginID(),
                0,
                new IStatus[]{makeExceptionStatus(severity, cause)},
                getExceptionMessage(ex),
                ex);
        }
    }

    public static IStatus makeExceptionStatus(String message, Throwable ex)
    {
        return new MultiStatus(
            DBeaverCore.getCorePluginID(),
            0,
            new IStatus[]{makeExceptionStatus(ex)},
            message,
            null);
    }

    public static Throwable getRootCause(Throwable ex) {
        for (Throwable e = ex; ; e = e.getCause()) {
            if (e.getCause() == null) {
                return e;
            }
        }
    }

    public static IStatus getRootStatus(IStatus status) {
        IStatus[] children = status.getChildren();
        if (children == null || children.length == 0) {
            return status;
        } else {
            return getRootStatus(children[0]);
        }
    }

    public static String getStatusText(IStatus status) {
        String text = status.getMessage();
        IStatus[] children = status.getChildren();
        if (children != null && children.length > 0) {
            for (IStatus child : children) {
                text += "\n" + getStatusText(child);
            }
        }
        return text;
    }

    /**
     * Returns first non-null and non-empty message from this exception or it's cause
     */
    public static String getFirstMessage(Throwable ex)
    {
        for (Throwable e = ex; e != null; e = e.getCause()) {
            String message = e.getMessage();
            if (!CommonUtils.isEmpty(message)) {
                return message;
            }
        }
        return null;
    }

    public static String getExceptionMessage(Throwable ex)
    {
        StringBuilder msg = new StringBuilder(/*CommonUtils.getShortClassName(ex.getClass())*/);
        if (ex.getMessage() != null) {
            msg.append(ex.getMessage());
        } else {
            msg.append(ex.getClass().getSimpleName());
        }
        return msg.toString().trim();
    }

    public static DBRProgressMonitor makeMonitor(IProgressMonitor monitor)
    {
        return new DefaultProgressMonitor(monitor);
    }

    public static IRunnableContext makeContext(final DBRProgressMonitor monitor)
    {
        return new IRunnableContext() {
            @Override
            public void run(boolean fork, boolean cancelable, IRunnableWithProgress runnable) throws InvocationTargetException, InterruptedException
            {
                runnable.run(monitor.getNestedMonitor());
            }
        };
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
                runnableWithProgress.run(makeMonitor(monitor));
            }
        });
    }

    public static void savePreferenceStore(IPreferenceStore store)
    {
        if (store instanceof IPersistentPreferenceStore) {
            try {
                ((IPersistentPreferenceStore) store).save();
            } catch (IOException e) {
                log.warn(e);
            }
        } else {
            log.debug("Could not save preference store '" + store + "' - not a persistent one"); //$NON-NLS-1$
        }
    }

    public static void setDefaultPreferenceValue(IPreferenceStore store, String name, Object value)
    {
        if (CommonUtils.isEmpty(store.getDefaultString(name))) {
            store.setDefault(name, value.toString());
        }
    }

    public static Object getPreferenceValue(IPreferenceStore store, String propName, Class<?> valueType)
    {
        try {
            if (valueType == null || CharSequence.class.isAssignableFrom(valueType)) {
                final String str = store.getString(propName);
                return CommonUtils.isEmpty(str) ? null : str;
            } else if (valueType == Boolean.class || valueType == Boolean.TYPE) {
                return store.getBoolean(propName);
            } else if (valueType == Long.class || valueType == Long.TYPE) {
                return store.getLong(propName);
            } else if (valueType == Integer.class || valueType == Integer.TYPE ||
                valueType == Short.class || valueType == Short.TYPE ||
                valueType == Byte.class || valueType == Byte.TYPE) {
                return store.getInt(propName);
            } else if (valueType == Double.class || valueType == Double.TYPE) {
                return store.getDouble(propName);
            } else if (valueType == Float.class || valueType == Float.TYPE) {
                return store.getFloat(propName);
            } else if (valueType == BigInteger.class) {
                final String str = store.getString(propName);
                return str == null ? null : new BigInteger(str);
            } else if (valueType == BigDecimal.class) {
                final String str = store.getString(propName);
                return str == null ? null : new BigDecimal(str);
            }
        } catch (RuntimeException e) {
            log.error(e);
        }
        final String string = store.getString(propName);
        return CommonUtils.isEmpty(string) ? null : string;
    }

    public static void setPreferenceValue(IPreferenceStore store, String propName, Object value)
    {
        if (value == null) {
            return;
        }
        if (value instanceof CharSequence) {
            store.setValue(propName, value.toString());
        } else if (value instanceof Boolean) {
            store.setValue(propName, (Boolean) value);
        } else if (value instanceof Long) {
            store.setValue(propName, (Long) value);
        } else if (value instanceof Integer || value instanceof Short || value instanceof Byte) {
            store.setValue(propName, ((Number) value).intValue());
        } else if (value instanceof Double) {
            store.setValue(propName, (Double) value);
        } else if (value instanceof Float) {
            store.setValue(propName, (Float) value);
        } else {
            store.setValue(propName, value.toString());
        }
    }

    public static void setPreferenceDefaultValue(IPreferenceStore store, String propName, Object value)
    {
        if (value == null) {
            return;
        }
        if (value instanceof CharSequence) {
            store.setDefault(propName, value.toString());
        } else if (value instanceof Boolean) {
            store.setDefault(propName, (Boolean) value);
        } else if (value instanceof Long) {
            store.setDefault(propName, (Long) value);
        } else if (value instanceof Integer || value instanceof Short || value instanceof Byte) {
            store.setDefault(propName, ((Number) value).intValue());
        } else if (value instanceof Double) {
            store.setDefault(propName, (Double) value);
        } else if (value instanceof Float) {
            store.setDefault(propName, (Float) value);
        } else {
            store.setDefault(propName, value.toString());
        }
    }

    public static Object convertString(String value, Class<?> valueType)
    {
        try {
            if (CommonUtils.isEmpty(value)) {
                return null;
            }
            if (valueType == null || CharSequence.class.isAssignableFrom(valueType)) {
                return value;
            } else if (valueType == Boolean.class || valueType == Boolean.TYPE) {
                return Boolean.valueOf(value);
            } else if (valueType == Long.class) {
                return Long.valueOf(value);
            } else if (valueType == Long.TYPE) {
                return Long.parseLong(value);
            } else if (valueType == Integer.class) {
                return new Integer(value);
            } else if (valueType == Integer.TYPE) {
                return Integer.parseInt(value);
            } else if (valueType == Short.class) {
                return Short.valueOf(value);
            } else if (valueType == Short.TYPE) {
                return Short.parseShort(value);
            } else if (valueType == Byte.class) {
                return Byte.valueOf(value);
            } else if (valueType == Byte.TYPE) {
                return Byte.parseByte(value);
            } else if (valueType == Double.class) {
                return Double.valueOf(value);
            } else if (valueType == Double.TYPE) {
                return Double.parseDouble(value);
            } else if (valueType == Float.class) {
                return Float.valueOf(value);
            } else if (valueType == Float.TYPE) {
                return Float.parseFloat(value);
            } else if (valueType == BigInteger.class) {
                return new BigInteger(value);
            } else if (valueType == BigDecimal.class) {
                return new BigDecimal(value);
            } else {
                return value;
            }
        } catch (RuntimeException e) {
            log.error(e);
            return value;
        }
    }

    public static File getUserHomeDir()
    {
        String userHome = System.getProperty("user.home"); //$NON-NLS-1$
        if (userHome == null) {
            userHome = ".";
        }
        return new File(userHome);
    }

    public static String getCurrentDate()
    {
        return new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH).format(new Date()); //$NON-NLS-1$
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
        return new SimpleDateFormat("yyyyMMddhhmm", Locale.ENGLISH).format(new Date()); //$NON-NLS-1$
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
        UIUtils.runInUI(null, saveRunner);
        return saveRunner.getResult();
    }

    public static Expression parseExpression(String exprString) throws DBException
    {
        synchronized (RuntimeUtils.class) {
            if (jexlEngine == null) {
                jexlEngine = new JexlEngine(null, null, null, null);
                jexlEngine.setCache(100);
            }
        }
        try {
            return jexlEngine.createExpression(exprString);
        } catch (JexlException e) {
            throw new DBException("Bad expression", e);
        }
    }

    public static boolean isTypeSupported(Class<?> type, Class[] supportedTypes)
    {
        if (type == null || ArrayUtils.isEmpty(supportedTypes)) {
            return false;
        }
        for (Class<?> tmp : supportedTypes) {
            if (tmp.isAssignableFrom(type)) {
                return true;
            }
        }
        return false;
    }

    public static DBRProcessDescriptor processCommand(
        final DBRShellCommand command,
        final Map<String, Object> variables)
    {
        final Shell shell = DBeaverUI.getActiveWorkbenchShell();
        final DBRProcessDescriptor processDescriptor = new DBRProcessDescriptor(command, variables);
        // Direct execute
        try {
            processDescriptor.execute();
        } catch (DBException e) {
            UIUtils.showErrorDialog(shell, "Execute process", processDescriptor.getName(), e);
        }
        if (command.isShowProcessPanel()) {
            shell.getDisplay().asyncExec(new Runnable() {
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
        if (command.isWaitProcessFinish()) {
            processDescriptor.waitFor();
        }
        return processDescriptor;
    }

    public static String getNativeBinaryName(String binName)
    {
        return DBeaverCore.getInstance().getLocalSystem().isWindows() ? binName + ".exe" : binName;
    }

    public static IStatus stripStack(IStatus status) {
        if (status instanceof MultiStatus) {
            IStatus[] children = status.getChildren();
            if (children != null) {
                for (int i = 0; i < children.length; i++) {
                    children[i] = stripStack(children[i]);
                }
            }
            return new MultiStatus(status.getPlugin(), status.getCode(), children, status.getMessage(), null);
        } else if (status instanceof Status) {
            String messagePrefix = "";
            if (status.getException() != null) {
                messagePrefix = status.getException().getClass().getName() + ": ";
            }
            return new Status(status.getSeverity(), status.getPlugin(), status.getCode(), messagePrefix + status.getMessage(), null);
        }
        return null;
    }

    public static class ProgramInfo {
        final Program program;
        Image image;

        private ProgramInfo(Program program)
        {
            this.program = program;
        }

        public Program getProgram()
        {
            return program;
        }

        public Image getImage()
        {
            return image;
        }
    }

    private static final Map<String, ProgramInfo> programMap = new HashMap<String, ProgramInfo>();

    public static ProgramInfo getProgram(IResource resource)
    {
        if (resource instanceof IFile) {
            final String fileExtension = CommonUtils.notEmpty(resource.getFileExtension());
            ProgramInfo programInfo = programMap.get(fileExtension);
            if (programInfo == null) {
                Program program = Program.findProgram(fileExtension);
                programInfo = new ProgramInfo(program);
                if (program != null) {
                    final ImageData imageData = program.getImageData();
                    if (imageData != null) {
                        programInfo.image = new Image(DBeaverUI.getDisplay(), imageData);
                    }
                }
                programMap.put(fileExtension, programInfo);
            }
            return programInfo.program == null ? null : programInfo;
        }
        return null;
    }

    public static void pause(int ms)
    {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            log.warn("Sleep interrupted", e);
        }
    }

    public static String formatExecutionTime(long ms)
    {
        if (ms < 60000) {
            // Less than a minute, show just ms
            return String.valueOf(ms) + CoreMessages.controls_time_ms;
        }
        long sec = ms / 1000;
        long min = sec / 60;
        sec -= min * 60;
        return String.valueOf(min) + " min " + String.valueOf(sec) + " sec";
    }

    public static void launchProgram(String path)
    {
        Program.launch(path);
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

        @Override
        public void run()
        {
            int choice = -1;
            if (saveable instanceof ISaveablePart2) {
                choice = ((ISaveablePart2) saveable).promptToSaveOnClose();
            }
            if (choice == -1 || choice == ISaveablePart2.DEFAULT) {
                Shell shell;
                String saveableName;
                if (saveable instanceof IWorkbenchPart) {
                    shell = ((IWorkbenchPart) saveable).getSite().getShell();
                    saveableName = ((IWorkbenchPart) saveable).getTitle();
                } else {
                    shell = DBeaverUI.getActiveWorkbenchShell();
                    saveableName = CommonUtils.toString(saveable);
                }
                int confirmResult = ConfirmationDialog.showConfirmDialog(
                    shell,
                    DBeaverPreferences.CONFIRM_EDITOR_CLOSE,
                    ConfirmationDialog.QUESTION_WITH_CANCEL,
                    saveableName);
                switch (confirmResult) {
                    case IDialogConstants.YES_ID:
                        choice = ISaveablePart2.YES;
                        break;
                    case IDialogConstants.NO_ID:
                        choice = ISaveablePart2.NO;
                        break;
                    default:
                        choice = ISaveablePart2.CANCEL;
                        break;
                }
            }
            switch (choice) {
                case ISaveablePart2.YES: //yes
                    saveable.doSave(monitor.getNestedMonitor());
                    result = !saveable.isDirty();
                    break;
                case ISaveablePart2.NO: //no
                    result = true;
                    break;
                case ISaveablePart2.CANCEL: //cancel
                default:
                    result = false;
                    break;
            }
        }
    }

    public static File getPlatformFile(String platformURL) throws IOException
    {
        URL url = new URL(platformURL);
        URL fileURL = FileLocator.toFileURL(url);
        // Escape spaces to avoid URI syntax error
        String filePath = fileURL.toString().replace(" ", "%20");
        try {
            return new File(new URI(filePath));
        } catch (URISyntaxException e) {
            throw new IOException("Bad local file path: " + filePath, e);
        }
    }

    public static boolean runTask(final DBRRunnableWithProgress task, final long waitTime) {
        final MonitoringTask monitoringTask = new MonitoringTask(task);
        Job monitorJob = new AbstractJob("Disconnect from data sources") {
            @Override
            protected IStatus run(DBRProgressMonitor monitor)
            {
                try {
                    monitoringTask.run(monitor);
                } catch (InvocationTargetException e) {
                    return RuntimeUtils.makeExceptionStatus(e.getTargetException());
                } catch (InterruptedException e) {
                    // do nothing
                }
                return Status.OK_STATUS;
            }
        };
        monitorJob.schedule();

        // Wait for job to finish
        long startTime = System.currentTimeMillis();
        if (waitTime > 0) {
            while (!monitoringTask.finished && System.currentTimeMillis() - startTime < waitTime) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
        return monitoringTask.finished;
    }

    private static class MonitoringTask implements DBRRunnableWithProgress {
        private final DBRRunnableWithProgress task;
        volatile boolean finished;

        private MonitoringTask(DBRRunnableWithProgress task) {
            this.task = task;
        }

        public boolean isFinished() {
            return finished;
        }

        @Override
        public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
            try {
                task.run(monitor);
            } finally {
                monitor.done();
                finished = true;
            }
        }
    }

}
