/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
import org.jkiss.dbeaver.ui.navigator.database.NavigatorViewBase;

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