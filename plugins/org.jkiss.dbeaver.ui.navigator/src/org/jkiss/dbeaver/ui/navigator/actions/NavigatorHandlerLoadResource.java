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

package org.jkiss.dbeaver.ui.navigator.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.fs.DBNFileSystemRoot;
import org.jkiss.dbeaver.model.navigator.fs.DBNPathBase;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.utils.ByteNumberFormat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

public class NavigatorHandlerLoadResource extends AbstractHandler implements IElementUpdater {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Shell shell = HandlerUtil.getActiveShell(event);

        File[] srcFiles = DialogUtils.openFileList(shell, "Open file(s)", null);
        if (srcFiles == null) {
            return null;
        }

        DBNNode pathNodes = NavigatorUtils.getSelectedNode(HandlerUtil.getCurrentSelection(event));
        if (!(pathNodes instanceof DBNPathBase pathNode)) {
            return null;
        }

        try {
            UIUtils.runInProgressDialog(monitor -> {
                try {
                    Path targetPath = pathNode.getPath();
                    if (!(pathNode instanceof DBNFileSystemRoot) && !Files.isDirectory(targetPath)) {
                        targetPath = targetPath.getParent();
                    }

                    loadLocalFiles(monitor, srcFiles, targetPath);

                    pathNodes.refreshNode(monitor, this);
                } catch (Exception e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (Exception e) {
            DBWorkbench.getPlatformUI().showError("File save IO error", null, e);
        }

        return null;
    }

    private void loadLocalFiles(DBRProgressMonitor monitor, File[] srcFiles, Path targetPath) {
        long totalFilesSize = 0;
        for (File srcFile : srcFiles) {
            totalFilesSize += srcFile.length();
        }

        monitor.beginTask("Load resources", (int) totalFilesSize);
        for (File srcFile : srcFiles) {
            if (monitor.isCanceled()) {
                return;
            }
            monitor.subTask(srcFile.getName() + " (" + ByteNumberFormat.getInstance().format(srcFile.length()) + ")");

            Path targetFilePath = targetPath.resolve(srcFile.getName());
            try {
                byte[] buffer = new byte[10000];
                try (InputStream is = Files.newInputStream(srcFile.toPath())) {
                    try (OutputStream os = Files.newOutputStream(targetFilePath, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                        for (;;) {
                            if (monitor.isCanceled()) {
                                break;
                            }
                            int count = is.read(buffer);
                            if (count <= 0) {
                                break;
                            }
                            monitor.worked(count);
                            os.write(buffer, 0, count);
                        }
                    }
                }
            } catch (IOException e) {
                DBWorkbench.getPlatformUI().showError("IO error", null, e);
            }
        }
        monitor.done();
    }


    @Override
    public void updateElement(UIElement element, Map parameters) {

    }
}