/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.internal.UIMessages;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.TextWithOpen;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;

/**
 * DialogUtils
 */
public class DialogUtils {

    private static final Log log = Log.getLog(DialogUtils.class);

    private static final String DIALOG_FOLDER_PROPERTY = "dialog.default.folder";

    public static String curDialogFolder;

    static {
        curDialogFolder = DBWorkbench.getPlatform().getPreferenceStore().getString(DIALOG_FOLDER_PROPERTY);
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
            DBWorkbench.getPlatformUI().showError("Bad file name", "Directory '" + saveDir.getAbsolutePath() + "' does not exists");
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

    public static String getCurDialogFolder()
    {
        return curDialogFolder;
    }

    public static void setCurDialogFolder(String curDialogFolder)
    {
        DBWorkbench.getPlatform().getPreferenceStore().setValue(DIALOG_FOLDER_PROPERTY, curDialogFolder);
        DialogUtils.curDialogFolder = curDialogFolder;
    }

    @NotNull
    public static Text createOutputFolderChooser(final Composite parent, @Nullable String label,
        @Nullable ModifyListener changeListener)
    {
        final String message = label != null ? label : UIMessages.output_label_directory;
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
