/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.app.standalone.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;

public class OpenLocalFileActionExt extends AbstractHandler {

    private IWorkbenchWindow window;
    private String filterPath;

    /**
     * Creates a new action for opening a local file.
     */
    public OpenLocalFileActionExt() {
        setEnabled(true);
        this.filterPath = DialogUtils.getCurDialogFolder();
    }

    @Override
    public void dispose() {
        window = null;
        filterPath = null;
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Shell activeShell = HandlerUtil.getActiveShell(event);
        FileDialog dialog = new FileDialog(activeShell, SWT.OPEN | SWT.MULTI | SWT.SHEET);
        dialog.setText(IDEWorkbenchMessages.OpenLocalFileAction_title);
        dialog.setFilterPath(filterPath);
        dialog.open();
        String[] names = dialog.getFileNames();

        if (names != null) {
            filterPath = dialog.getFilterPath();
            DialogUtils.setCurDialogFolder(filterPath);

            int numberOfFilesNotFound = 0;
            StringBuilder notFound = new StringBuilder();
            for (String name : names) {
                IFileStore fileStore = EFS.getLocalFileSystem().getStore(new Path(filterPath));
                fileStore = fileStore.getChild(name);
                IFileInfo fetchInfo = fileStore.fetchInfo();
                if (!fetchInfo.isDirectory() && fetchInfo.exists()) {
                    IWorkbenchPage page = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage();
                    try {
                        IDE.openEditorOnFileStore(page, fileStore);
                    } catch (PartInitException e) {
                        String msg = NLS.bind(IDEWorkbenchMessages.OpenLocalFileAction_message_errorOnOpen, fileStore.getName());
                        IDEWorkbenchPlugin.log(msg, e.getStatus());
                        MessageDialog.open(MessageDialog.ERROR, activeShell, IDEWorkbenchMessages.OpenLocalFileAction_title, msg, SWT.SHEET);
                    }
                } else {
                    if (++numberOfFilesNotFound > 1)
                        notFound.append('\n');
                    notFound.append(fileStore.getName());
                }
            }

            if (numberOfFilesNotFound > 0) {
                String msgFmt = numberOfFilesNotFound == 1 ? IDEWorkbenchMessages.OpenLocalFileAction_message_fileNotFound : IDEWorkbenchMessages.OpenLocalFileAction_message_filesNotFound;
                String msg = NLS.bind(msgFmt, notFound.toString());
                MessageDialog.open(MessageDialog.ERROR, activeShell, IDEWorkbenchMessages.OpenLocalFileAction_title, msg, SWT.SHEET);
            }
        }
        return null;
    }

}