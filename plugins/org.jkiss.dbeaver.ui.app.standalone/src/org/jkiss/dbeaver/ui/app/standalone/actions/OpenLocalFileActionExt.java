/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.file.FileTypeHandlerDescriptor;
import org.jkiss.dbeaver.ui.editors.file.FileTypeHandlerRegistry;
import org.jkiss.utils.ArrayUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class OpenLocalFileActionExt extends AbstractHandler {

    private String filterPath;
    private String filterExtension;

    /**
     * Creates a new action for opening a local file.
     */
    public OpenLocalFileActionExt() {
        setEnabled(true);
        this.filterPath = DialogUtils.getCurDialogFolder();
    }

    @Override
    public void dispose() {
        filterPath = null;
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Shell activeShell = HandlerUtil.getActiveShell(event);

        Set<String> extensions = new LinkedHashSet<>();
        for (FileTypeHandlerDescriptor dhd : FileTypeHandlerRegistry.getInstance().getHandlers()) {
            extensions.add(Arrays.stream(dhd.getExtensions()).map(e -> "*." + e).collect(Collectors.joining(";")));
        }
        extensions.add("*.*");

        FileDialog dialog = new FileDialog(activeShell, SWT.OPEN | SWT.MULTI | SWT.SHEET);
        dialog.setText(IDEWorkbenchMessages.OpenLocalFileAction_title);
        dialog.setFilterPath(filterPath);
        String[] dialogExtensions = extensions.toArray(new String[0]);
        dialog.setFilterExtensions(dialogExtensions);
        if (filterExtension != null) {
            int extIndex = ArrayUtils.indexOf(dialogExtensions, filterExtension);
            if (extIndex >= 0) {
                dialog.setFilterIndex(extIndex);
            }
        }
        if (dialog.open() == null) {
            return null;
        }
        filterExtension = dialog.getFilterExtensions()[dialog.getFilterIndex()];

        String[] names = dialog.getFileNames();

        if (names != null) {
            filterPath = dialog.getFilterPath();
            DialogUtils.setCurDialogFolder(filterPath);

            int numberOfFilesNotFound = 0;
            StringBuilder notFound = new StringBuilder();

            List<Path> fileList = new ArrayList<>();
            for (String name : names) {
                Path filePath = Path.of(filterPath).resolve(name);
                if (!Files.exists(filePath)) {
                    if (++numberOfFilesNotFound > 1)
                        notFound.append('\n');
                    notFound.append(filePath);
                } else {
                    fileList.add(filePath);
                }
            }
            String[] fileNames = fileList.stream().map(p -> p.toAbsolutePath().toString()).toArray(String[]::new);
            EditorUtils.openExternalFiles(fileNames, null);

            if (numberOfFilesNotFound > 0) {
                String msgFmt = numberOfFilesNotFound == 1 ? IDEWorkbenchMessages.OpenLocalFileAction_message_fileNotFound : IDEWorkbenchMessages.OpenLocalFileAction_message_filesNotFound;
                String msg = NLS.bind(msgFmt, notFound.toString());
                MessageDialog.open(MessageDialog.ERROR, activeShell, IDEWorkbenchMessages.OpenLocalFileAction_title, msg, SWT.SHEET);
            }
        }
        return null;
    }

}