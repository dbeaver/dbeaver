/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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

    @Override
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