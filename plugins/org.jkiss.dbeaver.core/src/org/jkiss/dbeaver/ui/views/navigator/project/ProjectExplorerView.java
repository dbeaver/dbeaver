/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.views.navigator.project;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.project.DBPProjectListener;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.views.navigator.database.NavigatorViewBase;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;

/**
 * ProjectExplorerView
 */
public class ProjectExplorerView extends NavigatorViewBase implements DBPProjectListener
{

    //static final Log log = LogFactory.getLog(ProjectExplorerView.class);

    public static final String VIEW_ID = "org.jkiss.dbeaver.core.projectExplorer";

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
        final LabelProvider mainLabelProvider = (LabelProvider)viewer.getLabelProvider();
        TreeViewerColumn nameColumn = new TreeViewerColumn(viewer, SWT.LEFT);
        nameColumn.getColumn().setText("Name");
        nameColumn.setLabelProvider(new CellLabelProvider() {
            @Override
            public void update(ViewerCell cell)
            {
                cell.setImage(mainLabelProvider.getImage(cell.getElement()));
                cell.setText(mainLabelProvider.getText(cell.getElement()));
            }
        });

        TreeViewerColumn dsColumn = new TreeViewerColumn(viewer, SWT.LEFT);
        dsColumn.getColumn().setText("DataSource");
        dsColumn.setLabelProvider(new CellLabelProvider() {
            @Override
            public void update(ViewerCell cell)
            {
                DBNNode node = (DBNNode) cell.getElement();
                if (node instanceof DBNDatabaseNode) {
                    cell.setText(((DBNDatabaseNode) node).getDataSourceContainer().getName());
                } else if (node instanceof DBNResource) {
                    Collection<DBSDataSourceContainer> containers = ((DBNResource) node).getAssociatedDataSources();
                    if (!CommonUtils.isEmpty(containers)) {
                        StringBuilder text = new StringBuilder();
                        for (DBSDataSourceContainer container : containers) {
                            if (text.length() > 0) {
                                text.append(", ");
                            }
                            text.append(container.getName());
                        }
                        cell.setText(text.toString());
                    }
                } else {
                    cell.setText("");
                }
            }
        });
        viewer.getTree().setHeaderVisible(true);
        //viewer.getTree().setLinesVisible(true);
        //UIUtils.packColumns(viewer.getTree());
        UIUtils.setHelp(parent, IHelpContextIds.CTX_PROJECT_EXPLORER);

        this.getNavigatorViewer().addFilter(new ViewerFilter() {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element)
            {
                return !(element instanceof DBNProjectDatabases);
            }
        });
        updateTitle();
        viewer.getTree().addControlListener(new ControlAdapter() {
            boolean resized = false;
            @Override
            public void controlResized(ControlEvent e)
            {
                if (!resized) {
                    UIUtils.packColumns(viewer.getTree(), true, null);
                    resized = true;
                }
            }
        });
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
        UIUtils.packColumns(getNavigatorTree().getViewer().getTree());
        updateTitle();
    }

    private void updateTitle()
    {
        setPartName("Project - " + getRootNode().getNodeName());
    }

}
