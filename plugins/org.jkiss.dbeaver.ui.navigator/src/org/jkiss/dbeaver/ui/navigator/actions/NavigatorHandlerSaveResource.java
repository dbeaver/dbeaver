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
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.navigator.fs.DBNPathBase;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.ByteNumberFormat;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NavigatorHandlerSaveResource extends AbstractHandler implements IElementUpdater {

    public static final int DIRECTORY_FILE_SIZE = 1000;
    public static final int FILES_SIZE_MONITOR_DIV = 1;

    private static class PathInfo {
        Path path;
        long size;
        boolean directory;

        public PathInfo(Path path, long size, boolean directory) {
            this.path = path;
            this.size = size;
            this.directory = directory;
        }
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Shell shell = HandlerUtil.getActiveShell(event);
        List<DBNPathBase> pathNodes = NavigatorUtils.getSelectedNodes(HandlerUtil.getCurrentSelection(event)).stream()
            .filter(n -> n instanceof DBNPathBase)
            .map(n -> (DBNPathBase) n).toList();
        if (pathNodes.isEmpty()) {
            return null;
        } else if (pathNodes.size() == 1 && !pathNodes.get(0).allowsChildren()) {
            // Single file
            saveSingleFile(shell, pathNodes.get(0));
        } else {
            // Set of files or/and folders
            saveMultipleResources(shell, pathNodes);
        }
        return null;
    }

    private void saveSingleFile(Shell shell, DBNPathBase pathNode) {
        FileDialog saveDialog = new FileDialog(shell, SWT.SAVE);
        saveDialog.setFileName(pathNode.getName());
        String targetPath = DialogUtils.openFileDialog(saveDialog);
        if (targetPath == null) {
            return;
        }

        Path targetFile = Path.of(targetPath);
        if (Files.exists(targetFile)) {
            if (!UIUtils.confirmAction(shell, "File exists", "Overwrite file '" + targetFile.toAbsolutePath() + "'?")) {
                return;
            }
        }
        try {
            UIUtils.runInProgressDialog(monitor -> {
                try {
                    Path sourcePath = pathNode.getPath();
                    try (InputStream is = Files.newInputStream(sourcePath)) {
                        try (OutputStream os = Files.newOutputStream(targetFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                            ContentUtils.copyStreams(is, Files.size(sourcePath), os, monitor);
                        }
                    }
                } catch (Exception e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (Exception e) {
            DBWorkbench.getPlatformUI().showError("File save IO error", null, e);
        }
    }

    private void saveMultipleResources(Shell shell, List<DBNPathBase> nodes) {
        String targetPath = DialogUtils.openDirectoryDialog(shell, "Choose target folder", null);
        if (targetPath == null) {
            return;
        }

        {
            // Filter duplicates (if both parent and child nodes are in the list then parent is removed
            List<DBNPathBase> dupList = new ArrayList<>();
            for (DBNPathBase node : nodes) {
                for (DBNPathBase n : nodes) {
                    if (node.isChildOf(n)) {
                        dupList.add(n);
                        break;
                    }
                }
            }
            nodes = nodes.stream().filter(n -> !dupList.contains(n)).collect(Collectors.toList());
        }

        saveMultipleResources(shell, Path.of(targetPath), nodes);

    }

    private void saveMultipleResources(Shell shell, Path targetFolder, List<DBNPathBase> nodes) {
        try {
            UIUtils.runInProgressDialog(monitor -> {
                try {
                    Map<DBNPathBase, List<PathInfo>> sourceResources = collectSourceFiles(nodes, monitor);
                    if (monitor.isCanceled()) {
                        return;
                    }

                    saveSourceFiles(monitor, shell, sourceResources, targetFolder);
                } catch (Exception e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (Exception e) {
            DBWorkbench.getPlatformUI().showError("File save IO error", null, e);
        }
    }

    private void saveSourceFiles(DBRProgressMonitor monitor, Shell shell, Map<DBNPathBase, List<PathInfo>> sourceResources, Path targetFolder) {
        long totalFilesSize = 0;
        for (Map.Entry<DBNPathBase, List<PathInfo>> srcPathEntry : sourceResources.entrySet()) {
            for (PathInfo itemPath : srcPathEntry.getValue()) {
                totalFilesSize += itemPath.size;
            }
        }

        monitor.beginTask("Save resources", (int) (totalFilesSize / FILES_SIZE_MONITOR_DIV));
        for (Map.Entry<DBNPathBase, List<PathInfo>> srcPathEntry : sourceResources.entrySet()) {
            Path srcPath = srcPathEntry.getKey().getPath();
            Path srcParent = srcPath.getParent();
            String basePath = (srcParent == null ? srcPath : srcParent).toAbsolutePath().toString();
            for (PathInfo itemPath : srcPathEntry.getValue()) {
                if (monitor.isCanceled()) {
                    return;
                }

                String srcFilePath = URLDecoder.decode(itemPath.path.toAbsolutePath().toString(), StandardCharsets.UTF_8);
                monitor.subTask(srcFilePath + (itemPath.directory ? "" : " (" + ByteNumberFormat.getInstance().format(itemPath.size) + ")"));

                if (srcFilePath.startsWith(basePath)) {
                    srcFilePath = srcFilePath.substring(basePath.length());
                }
                srcFilePath = CommonUtils.removeLeadingSlash(srcFilePath);

                Path targetPath = targetFolder.resolve(srcFilePath);
                try {
                    if (itemPath.directory) {
                        if (!Files.exists(targetPath)) {
                            Files.createDirectories(targetPath);
                        }
                        monitor.worked(DIRECTORY_FILE_SIZE / FILES_SIZE_MONITOR_DIV);
                    } else {
                        byte[] buffer = new byte[10000];
                        try (InputStream is = Files.newInputStream(itemPath.path)) {
                            try (OutputStream os = Files.newOutputStream(targetPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                                for (;;) {
                                    if (monitor.isCanceled()) {
                                        break;
                                    }
                                    int count = is.read(buffer);
                                    if (count <= 0) {
                                        break;
                                    }
                                    monitor.worked(count / FILES_SIZE_MONITOR_DIV);
                                    os.write(buffer, 0, count);
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    DBWorkbench.getPlatformUI().showError("IO error", null, e);
                }
            }

            monitor.worked(1);
        }
        monitor.done();
    }

    @NotNull
    private Map<DBNPathBase, List<PathInfo>> collectSourceFiles(List<DBNPathBase> nodes, DBRProgressMonitor monitor) throws IOException {
        monitor.beginTask("Collect statistics", nodes.size());
        Map<DBNPathBase, List<PathInfo>> sourceResources = new LinkedHashMap<>();
        for (DBNPathBase node : nodes) {
            monitor.subTask(node.getNodeDisplayName());
            Path path = node.getPath();

            if (Files.isDirectory(path)) {
                // Collect contents recursively
                Files.walkFileTree(path, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        if (monitor.isCanceled()) {
                            return FileVisitResult.TERMINATE;
                        }
                        monitor.subTask("Scan directory " + dir.toAbsolutePath());
                        sourceResources.computeIfAbsent(node, p -> new ArrayList<>()).add(makePathInfo(dir, true));
                        return FileVisitResult.CONTINUE;
                    }
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if (monitor.isCanceled()) {
                            return FileVisitResult.TERMINATE;
                        }
                        sourceResources.computeIfAbsent(node, p -> new ArrayList<>()).add(makePathInfo(file, false));
                        return FileVisitResult.CONTINUE;
                    }
                });
            } else {
                sourceResources.computeIfAbsent(node, p -> new ArrayList<>()).add(makePathInfo(path, false));
            }
            monitor.worked(1);
        }
        monitor.done();
        return sourceResources;
    }

    private PathInfo makePathInfo(Path path, boolean directory) throws IOException {
        return new PathInfo(path, directory ? 0 : Files.size(path), directory);
    }

    @Override
    public void updateElement(UIElement element, Map parameters) {

    }
}