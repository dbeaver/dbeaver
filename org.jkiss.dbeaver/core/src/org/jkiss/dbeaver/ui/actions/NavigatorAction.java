/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.ui.views.navigator.database.DatabaseNavigatorView;
import org.jkiss.dbeaver.utils.ViewUtils;

/**
 * NavigatorAction
 */
public abstract class NavigatorAction extends DataSourceAction {

    protected DBNNode getSelectedNode()
    {
        ISelection selection = getSelection();
        if (selection instanceof IStructuredSelection) {
            return ViewUtils.getSelectedNode((IStructuredSelection) selection);
        } else if (getWindow() != null) {
            DatabaseNavigatorView view = ViewUtils.findView(getWindow(), DatabaseNavigatorView.class);
            if (view != null) {
                return ViewUtils.getSelectedNode(view);
            }
        }
        return null;
    }
}
