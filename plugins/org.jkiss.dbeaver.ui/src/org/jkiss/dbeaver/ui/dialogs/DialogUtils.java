/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.fs.DBFUtils;
import org.jkiss.dbeaver.model.navigator.fs.DBNPathBase;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.TextWithOpen;
import org.jkiss.dbeaver.ui.internal.UIMessages;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.util.Arrays;

/**
 * DialogUtils
 */
public class DialogUtils {

    private static final Log log = Log.getLog(DialogUtils.class);

    private static final String DIALOG_FOLDER_PROPERTY = "dialog.default.folder";
    
    public static final String APPLY_AND_CLOSE_BUTTON_LABEL = JFaceResources.getString("PreferencesDialog.okButtonLabel");

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

    @Nullable
    public static File selectFileForSave(@NotNull Shell parentShell, @NotNull String title, @Nullable String[] filterExt, @Nullable String fileName) {
        return selectFileForSave(parentShell, title, filterExt, null, fileName);
    }

    @Nullable
    public static File selectFileForSave(@NotNull Shell parentShell, @NotNull String title, @Nullable String[] filterExt, @Nullable String[] filterNames, @Nullable String fileName) {
        FileDialog fileDialog = new FileDialog(parentShell, SWT.SAVE);
        fileDialog.setText(title);
        fileDialog.setOverwrite(true);
        if (filterExt != null) {
            fileDialog.setFilterExtensions(filterExt);
        }
        if (filterNames != null) {
            fileDialog.setFilterNames(filterNames);
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

    public static File[] openFileList(Shell parentShell, String title, String[] filterExt)
    {
        FileDialog fileDialog = new FileDialog(parentShell, SWT.OPEN | SWT.MULTI);
        if (title != null) {
            fileDialog.setText(title);
        }
        if (filterExt != null) {
            fileDialog.setFilterExtensions(filterExt);
        }
        String fileName = openFileDialog(fileDialog);
        if (CommonUtils.isEmpty(fileName)) {
            return null;
        }
        File filterPath = new File(fileDialog.getFilterPath());
        String[] fileNames = fileDialog.getFileNames();
        return Arrays.stream(fileNames).map(fn -> new File(filterPath, fn)).toArray(File[]::new);
    }

    public static String openFileDialog(FileDialog fileDialog)
    {
        if (curDialogFolder != null) {
            fileDialog.setFilterPath(curDialogFolder);
        }
        String filePath = fileDialog.open();
        if (!CommonUtils.isEmpty(filePath)) {
            setCurDialogFolder(fileDialog.getFilterPath());
            filePath = fixMissingFileExtension(fileDialog, filePath);
        }
        return filePath;
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
    public static Text createOutputFolderChooser(final Composite parent, @Nullable String label, @Nullable DBPProject project, boolean multiFS, @Nullable ModifyListener changeListener) {
        return createOutputFolderChooser(parent, label, null, project, multiFS, changeListener);
    }

    @NotNull
    public static Text createOutputFolderChooser(final Composite parent, @Nullable String label, @Nullable String value, @Nullable DBPProject project, boolean multiFS, @Nullable ModifyListener changeListener) {
        return createOutputFolderChooser(parent, label, null, value, project, multiFS, changeListener);
    }

    @NotNull
    public static Text createOutputFolderChooser(
        @NotNull Composite parent,
        @Nullable String label,
        @Nullable String tooltip,
        @Nullable String value,
        @Nullable DBPProject project,
        boolean multiFS,
        @Nullable ModifyListener changeListener
    ) {
        if (multiFS) {
            multiFS = project != null && DBFUtils.supportsMultiFileSystems(project);
        }
        final String message = label != null ? label : UIMessages.output_label_directory;
        UIUtils.createControlLabel(parent, message).setToolTipText(tooltip);
        final TextWithOpen directoryText = new TextWithOpen(parent, multiFS) {
            @Override
            protected void openBrowser(boolean remoteFS) {
                String fileName;
                if (remoteFS && project != null) {
                    DBNPathBase pathNode = DBWorkbench.getPlatformUI().openFileSystemSelector(
                        CommonUtils.toString(label, "Output folder"),
                        true, SWT.SAVE, false, null, getText());
                    fileName = pathNode == null ? null :
                        DBFUtils.getUriFromPath(pathNode.getPath()).toString();
                    if (fileName != null) {
                        setText(fileName);
                    }
                } else {
                    fileName = openDirectoryDialog(parent.getShell(), message, getText());
                }
                if (fileName != null) {
                    setText(fileName);
                }
            }

            @Override
            public DBPProject getProject() {
                return project;
            }
        };
        directoryText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        if (value != null) {
            directoryText.getTextControl().setText(value);
        }
        if (changeListener != null) {
            directoryText.getTextControl().addModifyListener(changeListener);
        }

        return directoryText.getTextControl();
    }

    @Nullable
    public static String openDirectoryDialog(@NotNull Shell shell, @NotNull String message, @Nullable String directory) {
        final DirectoryDialog dialog = new DirectoryDialog(shell);
        dialog.setMessage("Choose target directory");
        dialog.setText(message);

        if (CommonUtils.isEmpty(directory)) {
            directory = curDialogFolder;
        }
        if (!CommonUtils.isEmpty(directory)) {
            dialog.setFilterPath(directory);
        }
        directory = dialog.open();
        if (directory != null) {
            setCurDialogFolder(directory);
        }

        return directory;
    }

    public static TreeViewer createFilteredTree(Composite parent, int treeStyle, PatternFilter filter, String initialText) {
        FilteredTree filteredTree;
        try {
            filteredTree = new FilteredTree(parent, treeStyle, filter, true, true);
        } catch (Throwable e) {
            // Fast hash lookup is not supported on old Eclipse versions. Use old constructor
            filteredTree = new FilteredTree(parent, treeStyle, filter, true);
        }
        if (initialText != null) {
            filteredTree.setInitialText(initialText);
        }
        return filteredTree.getViewer();
    }

    /* SWT 2021-06-02 bug: file extension is not appended on Windows */
    @NotNull
    private static String fixMissingFileExtension(@NotNull FileDialog dialog, @NotNull String filePath) {
        if (CommonUtils.isBitSet(dialog.getStyle(), SWT.SAVE) && new File(filePath).getName().indexOf('.') < 0 && RuntimeUtils.isWindows()) {
            final String[] filters = dialog.getFilterExtensions();
            if (dialog.getFilterIndex() >= 0 && dialog.getFilterIndex() < filters.length) {
                final String filter = filters[dialog.getFilterIndex()];
                if (!filter.equals("*") && !filter.equals("*.*") && filter.indexOf('.') >= 0) {
                    return filePath + filter.substring(filter.lastIndexOf('.'));
                }
            }
        }
        return filePath;
    }
}
