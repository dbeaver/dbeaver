/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPProjectListener;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.controls.ViewerColumnController;
import org.jkiss.dbeaver.ui.project.PrefPageProjectResourceSettings;
import org.jkiss.utils.CommonUtils;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

/**
 * ProjectExplorerView
 */
public class ProjectExplorerView extends DecoratedProjectView implements DBPProjectListener {

    //static final Log log = Log.getLog(ProjectExplorerView.class);

    public static final String VIEW_ID = "org.jkiss.dbeaver.core.projectExplorer";
    private ViewerColumnController columnController;

    public ProjectExplorerView() {
        DBWorkbench.getPlatform().getWorkspace().addProjectListener(this);
    }

    @Override
    public DBNNode getRootNode() {
        DBNProject projectNode = getModel().getRoot().getProjectNode(DBWorkbench.getPlatform().getWorkspace().getActiveProject());
        return projectNode != null ? projectNode : getModel().getRoot();
    }

    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);

        UIUtils.setHelp(parent, IHelpContextIds.CTX_PROJECT_EXPLORER);

        final TreeViewer viewer = getNavigatorViewer();
        viewer.addFilter(new ViewerFilter() {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                return !(element instanceof DBNProjectDatabases);
            }
        });

        viewer.getTree().setHeaderVisible(true);

        UIExecutionQueue.queueExec(() -> {
            createColumns(viewer);
            updateTitle();
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
    }

    private void createColumns(final TreeViewer viewer) {
        final Color shadowColor = viewer.getControl().getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW);

        final ILabelProvider mainLabelProvider = (ILabelProvider) viewer.getLabelProvider();
        columnController = new ViewerColumnController("projectExplorer", viewer);
        columnController.setForceAutoSize(true);
        columnController.addColumn("Name", "Resource name", SWT.LEFT, true, true, new ColumnLabelProvider() {
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

        columnController.addColumn("DataSource", "Datasource(s) associated with resource", SWT.LEFT, true, false, new ColumnLabelProvider() {
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
        columnController.addColumn("Preview", "Script content preview", SWT.LEFT, false, false, new LazyLabelProvider(shadowColor) {
            @Override
            public String getLazyText(Object element) {
                if (element instanceof DBNNode) {
                    return ((DBNNode) element).getNodeDescription();
                } else {
                    return null;
                }
            }
        });
        columnController.addColumn("Size", "File size", SWT.LEFT, false, false, true, null, new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof DBNResource) {
                    IResource resource = ((DBNResource) element).getResource();
                    if (resource instanceof IFile) {
                        return String.valueOf(resource.getLocation().toFile().length());
                    }
                }
                return "";
            }
        }, null);
        columnController.addColumn("Modified", "Time the file was last modified", SWT.LEFT, false, false, new ColumnLabelProvider() {
            private SimpleDateFormat sdf = new SimpleDateFormat(DBConstants.DEFAULT_TIMESTAMP_FORMAT);

            @Override
            public String getText(Object element) {
                if (element instanceof DBNResource) {
                    IResource resource = ((DBNResource) element).getResource();
                    if (resource instanceof IFile || resource instanceof IFolder) {
                        long lastModified = resource.getLocation().toFile().lastModified();
                        if (lastModified <= 0) {
                            return "";
                        }
                        return sdf.format(new Date(lastModified));
                    }
                }
                return "";
            }
        });
        columnController.addColumn("Type", "Resource type", SWT.LEFT, false, false, new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof DBNResource) {
                    IResource resource = ((DBNResource) element).getResource();
                    ProgramInfo program = ProgramInfo.getProgram(resource);
                    if (program != null) {
                        return program.getProgram().getName();
                    }
                }
                return "";
            }
        });
        UIUtils.asyncExec(() -> columnController.createColumns(true));
    }

    @Override
    protected int getTreeStyle() {
        return super.getTreeStyle() | SWT.FULL_SELECTION;
    }

    @Override
    public void dispose() {
        DBWorkbench.getPlatform().getWorkspace().removeProjectListener(this);
        super.dispose();
    }

    @Override
    public void handleProjectAdd(DBPProject project) {

    }

    @Override
    public void handleProjectRemove(DBPProject project) {

    }

    @Override
    public void handleActiveProjectChange(DBPProject oldValue, DBPProject newValue) {
        UIExecutionQueue.queueExec(() -> {
            getNavigatorTree().reloadTree(getRootNode());
            updateTitle();
        });
        //columnController.autoSizeColumns();
    }

    private void updateTitle() {
        setPartName("Project - " + getRootNode().getNodeName());
    }

    public void configureView() {
        //columnController.configureColumns();
        DBPProject activeProject = DBWorkbench.getPlatform().getWorkspace().getActiveProject();
        if (activeProject != null) {
            UIUtils.showPreferencesFor(getSite().getShell(), activeProject.getEclipseProject(), PrefPageProjectResourceSettings.PAGE_ID);
        }
    }

}
