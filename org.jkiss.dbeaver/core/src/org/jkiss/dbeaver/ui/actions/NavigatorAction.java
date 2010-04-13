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
public abstract class NavigatorAction extends Action implements IObjectActionDelegate {
    private ISelection selection;
    private IWorkbenchWindow activeWindow;

    protected NavigatorAction()
    {
    }

    protected NavigatorAction(IWorkbenchWindow activeWindow)
    {
        this.activeWindow = activeWindow;
    }

    public ISelection getSelection()
    {
        return selection;
    }

    public IWorkbenchWindow getActiveWindow()
    {
        return activeWindow;
    }

    public void setActiveWindow(IWorkbenchWindow activeWindow)
    {
        this.activeWindow = activeWindow;
    }

    public abstract void run();

    public void run(IAction action)
    {
        this.run();
    }

    public void selectionChanged(IAction action, ISelection selection)
    {
        this.selection = selection;
    }

	public void setActivePart(IAction action, IWorkbenchPart targetPart)
    {
        activeWindow = targetPart.getSite().getWorkbenchWindow();
	}

    protected DBMNode getSelectedNode()
    {
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
        } else if (activeWindow != null) {
            NavigatorTreeView view = ViewUtils.findView(activeWindow, NavigatorTreeView.class);
            if (view != null) {
                return ViewUtils.getSelectedNode(view);
            }
        }
        return null;
    }
}
