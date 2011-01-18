/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.navigator.project;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNProjectDatabases;
import org.jkiss.dbeaver.ui.views.navigator.database.NavigatorViewBase;

/**
 * ProjectExplorerView
 */
public class ProjectExplorerView extends NavigatorViewBase
{

    //static final Log log = LogFactory.getLog(ProjectExplorerView.class);

    public static final String VIEW_ID = "org.jkiss.dbeaver.core.projectExplorer";

    public ProjectExplorerView() {
    }

    public DBNNode getRootNode()
    {
        return getModel().getRoot().getProject(DBeaverCore.getInstance().getProjectRegistry().getActiveProject());
    }

    @Override
    public void createPartControl(Composite parent)
    {
        super.createPartControl(parent);

        this.getNavigatorViewer().addFilter(new ViewerFilter() {
            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element)
            {
                return !(element instanceof DBNProjectDatabases);
            }
        });
    }

}
