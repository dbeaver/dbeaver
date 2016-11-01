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
package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.impl.ExternalContentStorage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.TextWithOpen;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;

/**
 * DialogUtils
 */
public class DialogUtils {

    private static final Log log = Log.getLog(DialogUtils.class);

    private static final String DIALOG_FOLDER_PROPERTY = "dialog.default.folder";

    public static String curDialogFolder;

    static {
        curDialogFolder = DBeaverCore.getGlobalPreferenceStore().getString(DIALOG_FOLDER_PROPERTY);
        if (CommonUtils.isEmpty(curDialogFolder)) {
            curDialogFolder = RuntimeUtils.getUserHomeDir().getAbsolutePath();
        }
    }

    public static File selectFileForSave(Shell parentShell, String valueName)
    {
        return selectFileForSave(parentShell, "Save Content As", null, valueName);
    }

    public static File selectFileForSave(Shell parentShell, String title, String[] filterExt, @Nullable String fileName)
    {
        FileDialog fileDialog = new FileDialog(parentShell, SWT.SAVE);
        fileDialog.setText(title);
        fileDialog.setOverwrite(true);
        if (filterExt != null) {
            fileDialog.setFilterExtensions(filterExt);
        }
        if (fileName != null) {
            fileDialog.setFileName(fileName);
        }

        fileName = openFileDialog(fileDialog);
        if (CommonUtils.isEmpty(fileName)) {
            return null;
        }
        final File saveFile = new File(fileName);
        File saveDir = saveFile.getParentFile();
        if (!saveDir.exists()) {
            UIUtils.showErrorDialog(parentShell, "Bad file name", "Directory '" + saveDir.getAbsolutePath() + "' does not exists");
            return null;
        }
        return saveFile;
    }

    public static File openFile(Shell parentShell)
    {
        return openFile(parentShell, null);
    }

    public static File openFile(Shell parentShell, String[] filterExt)
    {
        FileDialog fileDialog = new FileDialog(parentShell, SWT.OPEN);
        if (filterExt != null) {
            fileDialog.setFilterExtensions(filterExt);
        }
        String fileName = openFileDialog(fileDialog);
        if (CommonUtils.isEmpty(fileName)) {
            return null;
        }
        final File loadFile = new File(fileName);
        if (!loadFile.exists()) {
            MessageBox aMessageBox = new MessageBox(parentShell, SWT.ICON_WARNING | SWT.OK);
            aMessageBox.setText("File doesn't exists");
            aMessageBox.setMessage("The file "+ loadFile.getAbsolutePath() + " doesn't exists.");
            aMessageBox.open();
            return null;
        }
        return loadFile;
    }

    public static String openFileDialog(FileDialog fileDialog)
    {
        if (curDialogFolder != null) {
            fileDialog.setFilterPath(curDialogFolder);
        }
        String fileName = fileDialog.open();
        if (!CommonUtils.isEmpty(fileName)) {
            setCurDialogFolder(fileDialog.getFilterPath());
        }
        return fileName;
    }

    public static void loadFromFile(final IValueController controller)
    {
        if (!(controller.getValue() instanceof DBDContent)) {
            log.error(CoreMessages.model_jdbc_bad_content_value_ + controller.getValue());
            return;
        }

        Shell shell = UIUtils.getShell(controller.getValueSite());
        final File openFile = openFile(shell);
        if (openFile == null) {
            return;
        }
        final DBDContent value = (DBDContent)controller.getValue();
        DBeaverUI.runInUI(PlatformUI.getWorkbench().getActiveWorkbenchWindow(), new DBRRunnableWithProgress() {
            @Override
            public void run(DBRProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException {
                try {
                    DBDContentStorage storage;
                    if (ContentUtils.isTextContent(value)) {
                        storage = new ExternalContentStorage(DBeaverCore.getInstance(), openFile, GeneralUtils.UTF8_ENCODING);
                    } else {
                        storage = new ExternalContentStorage(DBeaverCore.getInstance(), openFile);
                    }
                    value.updateContents(monitor, storage);
                    controller.updateValue(value, true);
                } catch (Exception e) {
                    throw new InvocationTargetException(e);
                }
            }
        });
    }

    public static void saveToFile(IValueController controller)
    {
        if (!(controller.getValue() instanceof DBDContent)) {
            log.error(CoreMessages.model_jdbc_bad_content_value_ + controller.getValue());
            return;
        }

        Shell shell = UIUtils.getShell(controller.getValueSite());
        final File saveFile = selectFileForSave(shell, controller.getValueName());
        if (saveFile == null) {
            return;
        }
        final DBDContent value = (DBDContent)controller.getValue();
        try {
            DBeaverUI.runInProgressService(new DBRRunnableWithProgress() {
                @Override
                public void run(DBRProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException
                {
                    try {
                        DBDContentStorage storage = value.getContents(monitor);
                        if (ContentUtils.isTextContent(value)) {
                            try (Reader cr = storage.getContentReader()) {
                                ContentUtils.saveContentToFile(
                                    cr,
                                    saveFile,
                                    GeneralUtils.UTF8_ENCODING,
                                    monitor
                                );
                            }
                        } else {
                            try (InputStream cs = storage.getContentStream()) {
                                ContentUtils.saveContentToFile(cs, saveFile, monitor);
                            }
                        }
                    } catch (Exception e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        }
        catch (InvocationTargetException e) {
            UIUtils.showErrorDialog(
                shell,
                CoreMessages.model_jdbc_could_not_save_content,
                CoreMessages.model_jdbc_could_not_save_content_to_file_ + saveFile.getAbsolutePath() + "'", //$NON-NLS-2$
                e.getTargetException());
        }
        catch (InterruptedException e) {
            // do nothing
        }
    }

    public static String getCurDialogFolder()
    {
        return curDialogFolder;
    }

    public static void setCurDialogFolder(String curDialogFolder)
    {
        DBeaverCore.getGlobalPreferenceStore().setValue(DIALOG_FOLDER_PROPERTY, curDialogFolder);
        DialogUtils.curDialogFolder = curDialogFolder;
    }

    @NotNull
    public static Text createOutputFolderChooser(final Composite parent, @Nullable String label,
        @Nullable ModifyListener changeListener)
    {
        final String message = label != null ? label : CoreMessages.data_transfer_wizard_output_label_directory;
        UIUtils.createControlLabel(parent, message);
        final TextWithOpen directoryText = new TextWithOpen(parent) {
            @Override
            protected void openBrowser() {
                DirectoryDialog dialog = new DirectoryDialog(parent.getShell(), SWT.NONE);
                dialog.setMessage("Choose target directory");
                dialog.setText(message);
                String directory = getText();
                if (CommonUtils.isEmpty(directory)) {
                    directory = curDialogFolder;
                }
                if (!CommonUtils.isEmpty(directory)) {
                    dialog.setFilterPath(directory);
                }
                directory = dialog.open();
                if (directory != null) {
                    setText(directory);
                    setCurDialogFolder(directory);
                }
            }
        };
        directoryText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        if (changeListener != null) {
            directoryText.getTextControl().addModifyListener(changeListener);
        }

        return directoryText.getTextControl();
    }
}
