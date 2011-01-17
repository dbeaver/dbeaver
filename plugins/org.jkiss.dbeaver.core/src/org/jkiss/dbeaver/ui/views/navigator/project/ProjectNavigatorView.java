/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.navigator.project;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.ui.views.navigator.database.DatabaseNavigatorView;
import org.jkiss.dbeaver.ui.views.navigator.database.NavigatorViewBase;

/**
 * ProjectNavigatorView
 */
public class ProjectNavigatorView extends NavigatorViewBase // CommonNavigator
{

    static final Log log = LogFactory.getLog(ProjectNavigatorView.class);

    public static final String VIEW_ID = "org.jkiss.dbeaver.core.projectNavigator";

    public ProjectNavigatorView() {
    }

    public DBNNode getRootNode()
    {
        return getModel().getRoot();
    }


}
