/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.ui.views.navigator.database.NavigatorViewBase;

import java.util.Iterator;

public class NavigatorHandlerExpandAll extends AbstractHandler {

    public Object execute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
        if (activePart instanceof NavigatorViewBase) {
            TreeViewer navigatorViewer = ((NavigatorViewBase) activePart).getNavigatorViewer();
            ISelection selection = navigatorViewer.getSelection();
            if (selection.isEmpty()) {
                navigatorViewer.expandAll();
            } else if (selection instanceof IStructuredSelection) {
                for (Iterator iter = ((IStructuredSelection) selection).iterator(); iter.hasNext(); ) {
                    navigatorViewer.expandToLevel(iter.next(), TreeViewer.ALL_LEVELS);
                }
            }
        }
        return null;
    }
}