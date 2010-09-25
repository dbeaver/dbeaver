/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.model.DBPDeletableObject;

import java.util.Iterator;

public class NavigatorHandlerDelete extends AbstractHandler {

    public NavigatorHandlerDelete() {

    }

    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);
        final IWorkbenchWindow workbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);

        if (selection instanceof IStructuredSelection) {
            final IStructuredSelection structSelection = (IStructuredSelection)selection;

            workbenchWindow.getShell().getDisplay().asyncExec(new Runnable() {
                public void run() {
                    for (Iterator<?> iter = structSelection.iterator(); iter.hasNext(); ){
                        Object object = iter.next();
                        if (object instanceof DBPDeletableObject) {
                            ((DBPDeletableObject)object).deleteObject(workbenchWindow);
                        }
                    }
                }
            });
        }
        return null;
    }
}
