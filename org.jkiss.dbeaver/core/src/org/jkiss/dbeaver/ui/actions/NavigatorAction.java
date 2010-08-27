/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.meta.DBMNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.views.navigator.database.DatabaseNavigatorView;
import org.jkiss.dbeaver.utils.ViewUtils;

/**
 * NavigatorAction
 */
public abstract class NavigatorAction extends DataSourceAction {

    protected DBMNode getSelectedNode()
    {
        ISelection selection = getSelection();
        if (selection instanceof IStructuredSelection) {
            if (selection.isEmpty()) {
                return null;
            }
            Object selObject = ((IStructuredSelection) selection).getFirstElement();
            if (selObject instanceof DBSObject) {
                return DBeaverCore.getInstance().getMetaModel().getNodeByObject((DBSObject)selObject);
            } else {
                return null;
            }
        } else if (getWindow() != null) {
            DatabaseNavigatorView view = ViewUtils.findView(getWindow(), DatabaseNavigatorView.class);
            if (view != null) {
                return ViewUtils.getSelectedNode(view);
            }
        }
        return null;
    }
}
