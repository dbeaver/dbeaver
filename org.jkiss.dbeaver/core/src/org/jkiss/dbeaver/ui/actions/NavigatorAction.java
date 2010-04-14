/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.meta.DBMModel;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.ext.ui.IMetaModelView;
import org.jkiss.dbeaver.utils.ViewUtils;
import org.jkiss.dbeaver.model.meta.DBMNode;
import org.jkiss.dbeaver.ui.views.navigator.NavigatorTreeView;
import org.jkiss.dbeaver.core.DBeaverCore;

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
            NavigatorTreeView view = ViewUtils.findView(getWindow(), NavigatorTreeView.class);
            if (view != null) {
                return ViewUtils.getSelectedNode(view);
            }
        }
        return null;
    }
}
