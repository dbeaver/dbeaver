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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPPlatformDesktop;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPProjectListener;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.rcp.RCPProject;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.actions.ObjectPropertyTester;
import org.jkiss.dbeaver.ui.controls.ViewerColumnController;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;
import org.jkiss.dbeaver.ui.project.PrefPageProjectResourceSettings;
import org.jkiss.dbeaver.utils.ResourceUtils;
import org.jkiss.utils.CommonUtils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

/**
 * ProjectExplorerView
 */
public class ProjectExplorerView extends DecoratedProjectView implements DBPProjectListener {

    //static final Log log = Log.getLog(ProjectExplorerView.class);

    public static final String VIEW_ID = "org.jkiss.dbeaver.core.projectExplorer";
    private ViewerColumnController<?,?> columnController;
    private final NumberFormat sizeFormat = new DecimalFormat();
    
    private Composite treeContainer;
    private Label lockPlaceholder;
    private GridData lockPlaceholderLayoutInfo;
    private GridData treeViewLayoutInfo;

    public ProjectExplorerView() {
        DBPPlatformDesktop.getInstance().getWorkspace().addProjectListener(this);
    }

    @Override
    public DBNNode getRootNode() {
        DBNModel model = getGlobalNavigatorModel();
        DBNProject projectNode = model.getRoot().getProjectNode(DBWorkbench.getPlatform().getWorkspace().getActiveProject());
        return projectNode != null ? projectNode : model.getRoot();
    }

    @Override
    public void createPartControl(Composite parent) {
        treeContainer = UIUtils.createComposite(parent, 1);
        super.createPartControl(treeContainer);

        UIUtils.setHelp(parent, IHelpContextIds.CTX_PROJECT_EXPLORER);
        
        TreeViewer viewer = getNavigatorViewer();
        viewer.addFilter(new ViewerFilter() {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                return !(element instanceof DBNProjectDatabases);
            }
        });

        viewer.getTree().setHeaderVisible(true);

        UIExecutionQueue.queueExec(() -> {
            if (!viewer.getControl().isDisposed()) {
                createColumns(viewer);
                updateTitle();
            }
        });
        // Remove all non-resource nodes
        getNavigatorTree().getViewer().addFilter(new ViewerFilter() {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                if (parentElement == viewer.getInput() && !(element instanceof DBNResource)) {
                    return false;
                }
                return true;
            }
        });
        
        lockPlaceholder = UIUtils.createLabel(treeContainer, UIIcon.READONLY_RESOURCES);
        lockPlaceholder.setAlignment(SWT.CENTER);
        lockPlaceholder.setVisible(false);
        lockPlaceholderLayoutInfo = new GridData(SWT.CENTER, SWT.CENTER, true, true);
        lockPlaceholderLayoutInfo.exclude = true;
        lockPlaceholder.setLayoutData(lockPlaceholderLayoutInfo);
        treeViewLayoutInfo = new GridData(SWT.FILL, SWT.FILL, true, true);
        getNavigatorTree().setLayoutData(treeViewLayoutInfo);
        updateRepresentation();
    }

    private void createColumns(final TreeViewer viewer) {
        final Color shadowColor = viewer.getControl().getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW);

        final ILabelProvider mainLabelProvider = (ILabelProvider) viewer.getLabelProvider();
        columnController = new ViewerColumnController<>("projectExplorer", viewer);
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
                        Image image = mainLabelProvider.getImage(element);
                        if (element instanceof DBNResource) {
                            image = labelDecorator.decorateImage(image, element);
                        }
                        return image;
                    }

                    @Override
                    public String getToolTipText(Object element) {
                        if (mainLabelProvider instanceof IToolTipProvider) {
                            return ((IToolTipProvider) mainLabelProvider).getToolTipText(element);
                        }
                        return null;
                    }
                });

        columnController.addColumn(UINavigatorMessages.navigator_project_explorer_columns_datasource_text,
                UINavigatorMessages.navigator_project_explorer_columns_datasource_description,
                SWT.LEFT, true, false,
                new ColumnLabelProvider() {
                    @Override
                    public String getText(Object element) {
                        if (element instanceof DBNDatabaseNode) {
                            return ((DBNDatabaseNode) element).getDataSourceContainer().getName();
                        } else if (element instanceof DBNResource) {
                            Collection<DBPDataSourceContainer> containers = ((DBNResource) element).getAssociatedDataSources();
                            if (!CommonUtils.isEmpty(containers)) {
                                StringBuilder text = new StringBuilder();
                                for (DBPDataSourceContainer container : containers) {
                                    if (text.length() > 0) {
                                        text.append(", ");
                                    }
                                    text.append(container.getName());
                                }
                                return text.toString();
                            }
                        }
                        return "";
                    }

                    @Override
                    public Image getImage(Object element) {
        /*
                        DBNNode node = (DBNNode) element;
                        if (node instanceof DBNDatabaseNode) {
                            return DBeaverIcons.getImage(((DBNDatabaseNode) node).getDataSourceContainer().getDriver().getIcon());
                        } else if (node instanceof DBNResource) {
                            Collection<DBPDataSourceContainer> containers = ((DBNResource) node).getAssociatedDataSources();
                            if (containers != null && containers.size() == 1) {
                                return DBeaverIcons.getImage((containers.iterator().next().getDriver().getIcon()));
                            }
                        }
        */
                        return null;
                    }
                    @Override
                    public String getToolTipText(Object element) {
                        if (element instanceof DBNResource) {
                            Collection<DBPDataSourceContainer> containers = ((DBNResource) element).getAssociatedDataSources();
                            if (!CommonUtils.isEmpty(containers)) {
                                StringBuilder text = new StringBuilder();
                                for (DBPDataSourceContainer container : containers) {
                                    String description = container.getDescription();
                                    if (CommonUtils.isEmpty(description)) {
                                        description = container.getName();
                                    }
                                    if (!CommonUtils.isEmpty(description)) {
                                        if (text.length() > 0) {
                                            text.append(", ");
                                        }
                                        text.append(description);
                                    }
                                }
                                return text.toString();
                            }
                        }                return null;
                    }

                });
        columnController.addColumn(UINavigatorMessages.navigator_project_explorer_columns_preview_text,
                UINavigatorMessages.navigator_project_explorer_columns_preview_description, SWT.LEFT, false, false,
                new LazyLabelProvider(shadowColor) {
                    @Override
                    public String getLazyText(Object element) {
                        if (element instanceof DBNNode) {
                            return ((DBNNode) element).getNodeDescription();
                        } else {
                            return null;
                        }
                    }
                });
        columnController.addColumn(UINavigatorMessages.navigator_project_explorer_columns_size_text,
                UINavigatorMessages.navigator_project_explorer_columns_size_description,
                SWT.LEFT, false, false, true, null,
                new ColumnLabelProvider() {
                    @Override
                    public String getText(Object element) {
                        if (element instanceof DBNResource) {
                            IResource resource = ((DBNResource) element).getResource();
                            if (resource instanceof IFile && resource.exists()) {
                                return sizeFormat.format(ResourceUtils.getFileLength(resource));
                            }
                        }
                        return "";
                    }
                }, null);
        columnController.addColumn(UINavigatorMessages.navigator_project_explorer_columns_modified_text,
                UINavigatorMessages.navigator_project_explorer_columns_modified_description,
                SWT.LEFT, false, false,
                new ColumnLabelProvider() {
                    private final SimpleDateFormat sdf = new SimpleDateFormat(DBConstants.DEFAULT_TIMESTAMP_FORMAT);

                    @Override
                    public String getText(Object element) {
                        if (element instanceof DBNResource) {
                            IResource resource = ((DBNResource) element).getResource();
                            if (resource != null && resource.exists()) {
                                long lastModified = ResourceUtils.getResourceLastModified(resource);
                                if (lastModified <= 0) {
                                    return "";
                                }
                                return sdf.format(new Date(lastModified));
                            }
                        }
                        return "";
                    }
                });
        columnController.addColumn(UINavigatorMessages.navigator_project_explorer_columns_type_text,
                UINavigatorMessages.navigator_project_explorer_columns_type_description,
                SWT.LEFT, false, false,
                new ColumnLabelProvider() {
                    @Override
                    public String getText(Object element) {
                        if (element instanceof DBNResource) {
                            IResource resource = ((DBNResource) element).getResource();
                            if (resource.exists()) {
                                ProgramInfo program = ProgramInfo.getProgram(resource);
                                if (program != null) {
                                    return program.getProgram().getName();
                                }
                            }
                        }
                        return "";
                    }
                });
        
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

    @Override
    protected int getTreeStyle() {
        return super.getTreeStyle() | SWT.FULL_SELECTION;
    }

    @Override
    public void dispose() {
        DBPPlatformDesktop.getInstance().getWorkspace().removeProjectListener(this);
        super.dispose();
    }

    @Override
    public void handleProjectAdd(@NotNull DBPProject project) {

    }

    @Override
    public void handleProjectRemove(@NotNull DBPProject project) {

    }

    @Override
    public void handleActiveProjectChange(@NotNull DBPProject oldValue, @NotNull DBPProject newValue) {
        updateRepresentation();
    }
    
    private void updateRepresentation() {
        UIExecutionQueue.queueExec(() -> {
            if (getNavigatorTree().isDisposed()) {
                return;
            }
            getNavigatorTree().reloadTree(getRootNode());
            updateTitle();
            boolean viewable = ObjectPropertyTester.nodeProjectHasPermission(getRootNode(), RMConstants.PERMISSION_PROJECT_RESOURCE_VIEW);
            getNavigatorTree().setVisible(viewable);
            treeViewLayoutInfo.exclude = !viewable;
            lockPlaceholder.setVisible(!viewable);
            lockPlaceholderLayoutInfo.exclude = viewable;
            treeContainer.layout(true, true);
        });
        //columnController.autoSizeColumns();
    }

    private void updateTitle() {
        setPartName("Project - " + getRootNode().getNodeDisplayName());
    }

    public void configureView() {
        //columnController.configureColumns();
        DBPProject activeProject = DBWorkbench.getPlatform().getWorkspace().getActiveProject();
        if (activeProject instanceof RCPProject rcpProject) {
            UIUtils.showPreferencesFor(getSite().getShell(), rcpProject.getEclipseProject(), PrefPageProjectResourceSettings.PAGE_ID);
        }
    }

}
