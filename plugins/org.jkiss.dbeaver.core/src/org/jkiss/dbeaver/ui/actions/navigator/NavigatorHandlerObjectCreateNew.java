/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.model.navigator.DBNContainer;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.ui.NavigatorUtils;

import java.util.Map;

public class NavigatorHandlerObjectCreateNew extends NavigatorHandlerObjectCreateBase implements IElementUpdater {

    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);

        if (selection instanceof IStructuredSelection) {
            final IStructuredSelection structSelection = (IStructuredSelection)selection;
            Object element = structSelection.getFirstElement();
            if (element instanceof DBNNode) {
                createNewObject(HandlerUtil.getActiveWorkbenchWindow(event), (DBNNode) element, null);
            } else {
                log.error("Can't create new object based on selected element '" + element + "'");
            }
        }
        return null;
    }

    public void updateElement(UIElement element, Map parameters)
    {
        IWorkbenchPartSite partSite = (IWorkbenchPartSite) element.getServiceLocator().getService(IWorkbenchPartSite.class);
        if (partSite != null) {
            final ISelectionProvider selectionProvider = partSite.getSelectionProvider();
            if (selectionProvider != null) {
                DBNNode node = NavigatorUtils.getSelectedNode(selectionProvider.getSelection());
                if (node != null) {
                    String objectName;
                    if (node instanceof DBNContainer) {
                        objectName = ((DBNContainer)node).getChildrenType();
                    } else {
                        objectName = node.getNodeType();
                    }
                    element.setText("Create New " + objectName);
                }
            }
        }
    }

}