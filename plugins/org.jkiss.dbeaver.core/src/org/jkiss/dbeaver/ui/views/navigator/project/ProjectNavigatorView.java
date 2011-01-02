/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.navigator.project;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

/**
 * ProjectNavigatorView
 */
public class ProjectNavigatorView extends ViewPart {

    static final Log log = LogFactory.getLog(ProjectNavigatorView.class);

    public static final String VIEW_ID = "org.jkiss.dbeaver.core.projectNavigator";

    private TreeViewer projectViewer;

    public ProjectNavigatorView() {
    }

    @Override
    public void createPartControl(Composite parent) {
        projectViewer = new TreeViewer(parent);
    }

    @Override
    public void setFocus() {
        projectViewer.getTree().setFocus();
    }

}
