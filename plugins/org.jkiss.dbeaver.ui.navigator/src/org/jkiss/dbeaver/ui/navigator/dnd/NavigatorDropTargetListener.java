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
package org.jkiss.dbeaver.ui.navigator.dnd;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Widget;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNLocalFolder;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dnd.TreeNodeTransfer;
import org.jkiss.dbeaver.ui.navigator.INavigatorNodeContainer;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorContent;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class NavigatorDropTargetListener implements DropTargetListener {
    private final Viewer viewer;

    public NavigatorDropTargetListener(Viewer viewer) {
        this.viewer = viewer;
    }

    @Override
    public void dragEnter(DropTargetEvent event) {
        handleDragEvent(event);
    }

    @Override
    public void dragLeave(DropTargetEvent event) {
        handleDragEvent(event);
    }

    @Override
    public void dragOperationChanged(DropTargetEvent event) {
        handleDragEvent(event);
    }

    @Override
    public void dragOver(DropTargetEvent event) {
        handleDragEvent(event);
    }

    @Override
    public void drop(DropTargetEvent event) {
        handleDragEvent(event);
        if (event.detail == DND.DROP_MOVE) {
            moveNodes(event);
        }
    }

    @Override
    public void dropAccept(DropTargetEvent event) {
        handleDragEvent(event);
    }

    private void handleDragEvent(DropTargetEvent event) {
        event.detail = isDropSupported(event) ? DND.DROP_MOVE : DND.DROP_NONE;
        event.feedback = DND.FEEDBACK_SELECT | DND.FEEDBACK_SCROLL;
    }

    private boolean isDropSupported(DropTargetEvent event) {
        final Object curObject = getDropTarget(event, viewer);
        if (TreeNodeTransfer.getInstance().isSupportedType(event.currentDataType)) {
            @SuppressWarnings("unchecked")
            Collection<DBNNode> nodesToDrop = (Collection<DBNNode>) event.data;
            if (curObject instanceof DBNNode) {
                if (!CommonUtils.isEmpty(nodesToDrop)) {
                    for (DBNNode node : nodesToDrop) {
                        if (!((DBNNode) curObject).supportsDrop(node)) {
                            return false;
                        }
                    }
                    return true;
                } else {
                    return ((DBNNode) curObject).supportsDrop(null);
                }
            } else if (curObject == null) {
                // Drop to empty area
                if (!CommonUtils.isEmpty(nodesToDrop)) {
                    for (DBNNode node : nodesToDrop) {
                        if (!(node instanceof DBNDataSource)) {
                            return false;
                        }
                    }
                    return true;
                } else {
                    Widget widget = event.widget;
                    if (widget instanceof DropTarget) {
                        widget = ((DropTarget) widget).getControl();
                    }
                    return widget == viewer.getControl();
                }
            }
        }
        // Drop file - over resources
        if (FileTransfer.getInstance().isSupportedType(event.currentDataType)) {
            if (curObject instanceof IAdaptable) {
                IResource curResource = ((IAdaptable) curObject).getAdapter(IResource.class);
                return curResource != null;
            }
        }

        return false;
    }

    private void moveNodes(DropTargetEvent event) {
        final Object curObject = getDropTarget(event, viewer);
        if (TreeNodeTransfer.getInstance().isSupportedType(event.currentDataType)) {
            if (curObject instanceof DBNNode) {
                Collection<DBNNode> nodesToDrop = TreeNodeTransfer.getInstance().getObject();
                try {
                    UIUtils.runInProgressService(monitor -> {
                        try {
                            ((DBNNode) curObject).dropNodes(monitor, nodesToDrop);
                        } catch (Exception e) {
                            throw new InvocationTargetException(e);
                        }
                    });
                } catch (Exception e) {
                    DBWorkbench.getPlatformUI().showError("Drop error", "Can't drop node", e);
                    return;
                }
            } else if (curObject == null) {
                for (DBNNode node : TreeNodeTransfer.getInstance().getObject()) {
                    if (node instanceof DBNDataSource ds) {
                        // Drop datasource on a view
                        // We need target project
                        if (viewer.getInput() instanceof DatabaseNavigatorContent) {
                            DBNNode rootNode = ((DatabaseNavigatorContent) viewer.getInput()).getRootNode();
                            if (rootNode != null && rootNode.getOwnerProject() != null) {
                                ds.moveToFolder(rootNode.getOwnerProject(), null);
                            }
                        }
                    } else if (node instanceof DBNLocalFolder lf) {
                        lf.getFolder().setParent(null);
                    } else {
                        continue;
                    }
                    DBNModel.updateConfigAndRefreshDatabases(node);
                }
            }
        }
        if (FileTransfer.getInstance().isSupportedType(event.currentDataType)) {
            if (curObject instanceof IAdaptable adaptable) {
                IResource curResource = adaptable.getAdapter(IResource.class);
                if (curResource != null) {
                    if (curResource instanceof IFile) {
                        curResource = curResource.getParent();
                    }
                    if (curResource instanceof IContainer toFolder) {
                        new AbstractJob("Copy files to workspace") {
                            {
                                setUser(true);
                            }

                            @Override
                            protected IStatus run(DBRProgressMonitor monitor) {
                                String[] fileNames = (String[]) event.data;
                                monitor.beginTask("Copy files", fileNames.length);
                                try {
                                    dropFilesIntoFolder(monitor, toFolder, fileNames);
                                } catch (Exception e) {
                                    return GeneralUtils.makeExceptionStatus(e);
                                } finally {
                                    monitor.done();
                                }
                                return Status.OK_STATUS;
                            }
                        }.schedule();
                    } else {
                        DBWorkbench.getPlatformUI().showError("Drop error", "Can't drop file into '" + curResource.getName() + "'. Files can be dropped only into folders.");
                    }
                }
            }
        }
    }


    @Nullable
    private static Object getDropTarget(@NotNull DropTargetEvent event, @NotNull Viewer viewer) {
        if (event.item instanceof Item) {
            return event.item.getData();
        } else {
            Object input = viewer.getInput();
            if (input instanceof INavigatorNodeContainer dnc) {
                return dnc.getRootNode();
            } else if (input instanceof List list) {
                if (!list.isEmpty())
                    return list.get(0);
            }
        }
        return null;
    }

    private static void dropFilesIntoFolder(DBRProgressMonitor monitor, IContainer toFolder, String[] data) throws Exception {
        for (String extFileName : data) {
            Path extFile = Path.of(extFileName);
            dropFileIntoContainer(monitor, toFolder, extFile);
        }
    }

    private static void dropFileIntoContainer(DBRProgressMonitor monitor, IContainer toFolder, Path extFile) throws CoreException, IOException {
        if (Files.exists(extFile)) {
            org.eclipse.core.runtime.Path ePath = new org.eclipse.core.runtime.Path(extFile.getFileName().toString());

            if (Files.isDirectory(extFile)) {
                IFolder subFolder = toFolder.getFolder(ePath);
                if (!subFolder.exists()) {
                    subFolder.create(true, true, monitor.getNestedMonitor());
                }
                List<Path> sourceFolderContents;
                try (Stream<Path> list = Files.list(extFile)) {
                    sourceFolderContents = list.toList();
                }
                for (Path folderFile : sourceFolderContents) {
                    dropFileIntoContainer(monitor, subFolder, folderFile);
                }
            } else {
                monitor.subTask("Copy file " + extFile);
                try {
                    if (toFolder instanceof IFolder && !toFolder.exists()) {
                        ((IFolder) toFolder).create(true, true, monitor.getNestedMonitor());
                    }
                    IFile targetFile = toFolder.getFile(ePath);
                    if (targetFile.exists()) {
                        if (!UIUtils.confirmAction("File exists", "File '" + targetFile.getName() + "' exists. Do you want to overwrite it?")) {
                            return;
                        }
                    }
                    try (InputStream is = Files.newInputStream(extFile)) {
                        if (targetFile.exists()) {
                            targetFile.setContents(is, true, false, monitor.getNestedMonitor());
                        } else {
                            targetFile.create(is, true, monitor.getNestedMonitor());
                        }
                    }
                } finally {
                    monitor.worked(1);
                }
            }
        }
    }

}
