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
package org.jkiss.dbeaver.ui.navigator.project;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.navigator.DBNEmptyNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.model.navigator.DBNProjectDatabases;
import org.jkiss.dbeaver.model.navigator.fs.DBNFileSystems;
import org.jkiss.dbeaver.model.navigator.fs.DBNPath;
import org.jkiss.dbeaver.model.navigator.fs.DBNPathBase;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIExecutionQueue;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ViewerColumnController;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;
import org.jkiss.dbeaver.ui.navigator.INavigatorFilter;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseBrowserView;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorTree;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;

/**
 * Base view for
 */
public class FileSystemExplorerView extends DatabaseBrowserView {

    private static final Log log = Log.getLog(FileSystemExplorerView.class);

    private ViewerColumnController<?,?> columnController;
    private static final NumberFormat sizeFormat = new DecimalFormat();

    public static DBNFileSystems getFileSystemsNode() {
        DBNProject projectNode = getGlobalNavigatorModel().getRoot().getProjectNode(
            DBWorkbench.getPlatform().getWorkspace().getActiveProject());
        if (projectNode != null) {
            return projectNode.getExtraNode(DBNFileSystems.class);
        }
        return null;
    }

    public FileSystemExplorerView() {
        super();
    }

    @Override
    protected INavigatorFilter getNavigatorFilter() {
        return null;
    }

    @Override
    public DBNNode getRootNode() {
        return super.getRootNode();
    }

    @Override
    protected DBNNode getDefaultRootNode() {
        DBNFileSystems fsRootNode = getFileSystemsNode();
        return fsRootNode == null ? new DBNEmptyNode() : fsRootNode;
    }

    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);
        final TreeViewer viewer = getNavigatorViewer();
        viewer.addFilter(new ViewerFilter() {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                return !(element instanceof DBNProjectDatabases);
            }
        });

        viewer.getTree().setHeaderVisible(true);

        UIExecutionQueue.queueExec(() -> createColumns(viewer));
    }

    @Override
    protected void installDragAndDropSupport(DatabaseNavigatorTree navigatorTree) {
        super.installDragAndDropSupport(navigatorTree);
    }

    @Override
    public void init(IViewSite site) throws PartInitException {
        super.init(site);
    }

    protected void createColumns(final TreeViewer viewer) {
        final ILabelProvider mainLabelProvider = (ILabelProvider) viewer.getLabelProvider();
        columnController = new ViewerColumnController<>("cloudFileExplorer", viewer);
        columnController.setForceAutoSize(true);
        columnController.addColumn(UINavigatorMessages.navigator_project_explorer_columns_name_text,
            UINavigatorMessages.navigator_project_explorer_columns_name_description,
            SWT.LEFT, true, true,
            new ColumnLabelProvider() {
                @Override
                public String getText(Object element) {
                    return mainLabelProvider.getText(element);
                }

                @Override
                public Image getImage(Object element) {
                    return mainLabelProvider.getImage(element);
                }

                @Override
                public String getToolTipText(Object element) {
                    if (mainLabelProvider instanceof IToolTipProvider) {
                        return ((IToolTipProvider) mainLabelProvider).getToolTipText(element);
                    }
                    return null;
                }
            });

        columnController.addColumn(UINavigatorMessages.navigator_project_explorer_columns_size_text,
            UINavigatorMessages.navigator_project_explorer_columns_size_description,
            SWT.RIGHT, false, false, true, null,
            new ColumnLabelProvider() {
                @Override
                public String getText(Object element) {
                    if (element instanceof DBNPath dbnPath && !dbnPath.allowsChildren()) {
                        Path path = dbnPath.getPath();
                        try {
                            return sizeFormat.format(Files.size(path));
                        } catch (IOException e) {
                            log.debug(e);
                        }
                    }
                    return "";
                }
            }, null);
        columnController.addColumn(UINavigatorMessages.navigator_project_explorer_columns_modified_text,
            UINavigatorMessages.navigator_project_explorer_columns_modified_description,
            SWT.RIGHT, false, false,
            new ColumnLabelProvider() {
                private final SimpleDateFormat sdf = new SimpleDateFormat(DBConstants.DEFAULT_TIMESTAMP_FORMAT);

                @Override
                public String getText(Object element) {
                    if (element instanceof DBNPath) {
                        Path path = ((DBNPathBase) element).getPath();
                        if (path != null && Files.isRegularFile(path)) {
                            try {
                                FileTime lastModified = Files.getLastModifiedTime(path);
                                return sdf.format(lastModified.toMillis());
                            } catch (IOException e) {
                                log.debug(e);
                            }
                        }
                    }
                    return "";
                }
            });
        createExtraColumns(columnController, viewer);

        final var closure = new Object() {
            public Runnable createColumnsWhenNotBusy;
        };
        closure.createColumnsWhenNotBusy = () -> {
            if (viewer.isBusy()) {
                UIUtils.asyncExec(closure.createColumnsWhenNotBusy);
            } else {
                columnController.createColumns(true);
            }
        };
        UIUtils.asyncExec(closure.createColumnsWhenNotBusy);
    }

    protected void createExtraColumns(ViewerColumnController<?, ?> columnController, TreeViewer viewer) {

    }

}
