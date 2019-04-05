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
package org.jkiss.dbeaver.ui.navigator.project;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPProjectListener;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.LazyLabelProvider;
import org.jkiss.dbeaver.ui.ProgramInfo;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ViewerColumnController;
import org.jkiss.dbeaver.ui.navigator.database.NavigatorViewBase;
import org.jkiss.utils.CommonUtils;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

/**
 * ProjectExplorerView
 */
public class ProjectExplorerView extends NavigatorViewBase implements DBPProjectListener
{

    //static final Log log = Log.getLog(ProjectExplorerView.class);

    public static final String VIEW_ID = "org.jkiss.dbeaver.core.projectExplorer";
    private ViewerColumnController columnController;

    public ProjectExplorerView() {
        DBWorkbench.getPlatform().getProjectManager().addProjectListener(this);
    }

    @Override
    public DBNNode getRootNode()
    {
        DBNProject projectNode = getModel().getRoot().getProject(DBWorkbench.getPlatform().getProjectManager().getActiveProject());
        return projectNode != null ? projectNode : getModel().getRoot();
    }

    @Override
    public void createPartControl(Composite parent)
    {
        super.createPartControl(parent);
        final TreeViewer viewer = getNavigatorViewer();
        assert viewer != null;
        viewer.getTree().setHeaderVisible(true);
        createColumns(viewer);
        UIUtils.setHelp(parent, IHelpContextIds.CTX_PROJECT_EXPLORER);

        viewer.addFilter(new ViewerFilter() {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                return !(element instanceof DBNProjectDatabases);
            }
        });
        updateTitle();
    }

    private void createColumns(final TreeViewer viewer)
    {
        final Color shadowColor = viewer.getControl().getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW);

        final ILabelProvider mainLabelProvider = (ILabelProvider) viewer.getLabelProvider();
        columnController = new ViewerColumnController("projectExplorer", viewer);
        columnController.addColumn("Name", "Resource name", SWT.LEFT, true, true, new TreeColumnViewerLabelProvider(new LabelProvider() {
            @Override
            public String getText(Object element) {
                return mainLabelProvider.getText(element);
            }

            @Override
            public Image getImage(Object element) {
                return mainLabelProvider.getImage(element);
            }
        }));

        columnController.addColumn("DataSource", "Datasource(s) associated with resource", SWT.LEFT, true, false, new TreeColumnViewerLabelProvider(new LabelProvider() {
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

        }));
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
        columnController.addColumn("Size", "File size", SWT.LEFT, false, false, true, null, new TreeColumnViewerLabelProvider(new LabelProvider() {
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
        }), null);
        columnController.addColumn("Modified", "Time the file was last modified", SWT.LEFT, false, false, new TreeColumnViewerLabelProvider(new LabelProvider() {
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
        }));
        columnController.addColumn("Type", "Resource type", SWT.LEFT, false, false, new TreeColumnViewerLabelProvider(new LabelProvider() {
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
        }));
        columnController.createColumns(false);
    }

    @Override
    protected int getTreeStyle()
    {
        return super.getTreeStyle() | SWT.FULL_SELECTION;
    }

    @Override
    public void dispose()
    {
        DBWorkbench.getPlatform().getProjectManager().removeProjectListener(this);
        super.dispose();
    }

    @Override
    public void handleActiveProjectChange(IProject oldValue, IProject newValue)
    {
        UIUtils.asyncExec(() -> {
            getNavigatorTree().reloadTree(getRootNode());
            updateTitle();
            UIUtils.packColumns(getNavigatorTree().getViewer().getTree(), true, null);
        });
    }

    private void updateTitle()
    {
        setPartName("Project - " + getRootNode().getNodeName());
    }

    public void configureView() {
        columnController.configureColumns();
    }

}
