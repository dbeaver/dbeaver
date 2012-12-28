/*
 * Copyright (C) 2010-2012 Serge Rieder
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
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
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.runtime.DBRProcessDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.runtime.DBRShellCommand;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;
import org.jkiss.dbeaver.ui.views.process.ShellProcessView;
import org.jkiss.dbeaver.utils.AbstractPreferenceStore;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * RuntimeUtils
 */
public class RuntimeUtils {
    static final Log log = LogFactory.getLog(RuntimeUtils.class);

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
                null);
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
                null);
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
        if (!store.contains(name)) {
            store.setValue(name, value.toString());
        }
        store.setDefault(name, value.toString());
    }

    public static Object getPreferenceValue(IPreferenceStore store, String propName, Class<?> valueType)
    {
        try {
            if (!store.contains(propName)) {
                if (store instanceof AbstractPreferenceStore && ((AbstractPreferenceStore)store).getParentStore() != null) {
                    return getPreferenceValue(((AbstractPreferenceStore)store).getParentStore(), propName, valueType);
                }
                return null;
            }
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
            } else if (valueType == Long.class) {
                return new Long(value);
            } else if (valueType == Long.TYPE) {
                return Long.parseLong(value);
            } else if (valueType == Integer.class) {
                return new Integer(value);
            } else if (valueType == Integer.TYPE) {
                return Integer.parseInt(value);
            } else if (valueType == Short.class) {
                return new Short(value);
            } else if (valueType == Short.TYPE) {
                return Short.parseShort(value);
            } else if (valueType == Byte.class) {
                return new Byte(value);
            } else if (valueType == Byte.TYPE) {
                return Byte.parseByte(value);
            } else if (valueType == Double.class) {
                return new Double(value);
            } else if (valueType == Double.TYPE) {
                return Double.parseDouble(value);
            } else if (valueType == Float.class) {
                return new Float(value);
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

    public static boolean isTypeSupported(Class<?> type, Class[] supportedTypes)
    {
        if (type == null || CommonUtils.isEmpty(supportedTypes)) {
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
        } else {
            // Direct execute
            try {
                processDescriptor.execute();
            } catch (DBException e) {
                UIUtils.showErrorDialog(shell, "Execute process", processDescriptor.getName(), e);
            }
        }
        return processDescriptor;
    }

    public static String getNativeBinaryName(String binName)
    {
        return DBeaverCore.getInstance().getLocalSystem().isWindows() ? binName + ".exe" : binName;
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
            final String fileExtension = CommonUtils.getString(resource.getFileExtension());
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
                    PrefConstants.CONFIRM_EDITOR_CLOSE,
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

}
