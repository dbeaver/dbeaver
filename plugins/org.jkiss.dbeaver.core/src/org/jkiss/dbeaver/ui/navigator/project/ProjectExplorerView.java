/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.navigator.project;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.project.DBPProjectListener;
import org.jkiss.dbeaver.ui.*;
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
        DBeaverCore.getInstance().getProjectRegistry().addProjectListener(this);
    }

    @Override
    public DBNNode getRootNode()
    {
        DBNProject projectNode = getModel().getRoot().getProject(DBeaverCore.getInstance().getProjectRegistry().getActiveProject());
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
                DBNNode node = (DBNNode) element;
                if (node instanceof DBNDatabaseNode) {
                    return ((DBNDatabaseNode) node).getDataSourceContainer().getName();
                } else if (node instanceof DBNResource) {
                    Collection<DBPDataSourceContainer> containers = ((DBNResource) node).getAssociatedDataSources();
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
                return ((DBNNode)element).getNodeDescription();
            }
        });
        columnController.addColumn("Size", "File size", SWT.LEFT, false, false, new TreeColumnViewerLabelProvider(new LabelProvider() {
            @Override
            public String getText(Object element) {
                DBNNode node = (DBNNode) element;
                if (node instanceof DBNResource) {
                    IResource resource = ((DBNResource) node).getResource();
                    if (resource instanceof IFile) {
                        return String.valueOf(resource.getLocation().toFile().length());
                    }
                }
                return "";
            }
        }));
        columnController.addColumn("Modified", "Time the file was last modified", SWT.LEFT, false, false, new TreeColumnViewerLabelProvider(new LabelProvider() {
            private SimpleDateFormat sdf = new SimpleDateFormat(UIUtils.DEFAULT_TIMESTAMP_PATTERN);

            @Override
            public String getText(Object element) {
                DBNNode node = (DBNNode) element;
                if (node instanceof DBNResource) {
                    IResource resource = ((DBNResource) node).getResource();
                    if (resource instanceof IFile) {
                        return sdf.format(new Date(resource.getLocation().toFile().lastModified()));
                    }
                }
                return "";
            }
        }));
        columnController.addColumn("Type", "Resource type", SWT.LEFT, false, false, new TreeColumnViewerLabelProvider(new LabelProvider() {
            @Override
            public String getText(Object element) {
                DBNNode node = (DBNNode) element;
                if (node instanceof DBNResource) {
                    IResource resource = ((DBNResource) node).getResource();
                    ProgramInfo program = ProgramInfo.getProgram(resource);
                    if (program != null) {
                        return program.getProgram().getName();
                    }
                }
                return "";
            }
        }));
        columnController.createColumns();
    }

    @Override
    protected int getTreeStyle()
    {
        return super.getTreeStyle() | SWT.FULL_SELECTION;
    }

    @Override
    public void dispose()
    {
        DBeaverCore.getInstance().getProjectRegistry().removeProjectListener(this);
        super.dispose();
    }

    @Override
    public void handleActiveProjectChange(IProject oldValue, IProject newValue)
    {
        getNavigatorTree().reloadTree(getRootNode());
        //UIUtils.packColumns(getNavigatorTree().getViewer().getTree());
        updateTitle();
    }

    private void updateTitle()
    {
        setPartName("Project - " + getRootNode().getNodeName());
    }

    public void configureView() {
        columnController.configureColumns();
    }

}
