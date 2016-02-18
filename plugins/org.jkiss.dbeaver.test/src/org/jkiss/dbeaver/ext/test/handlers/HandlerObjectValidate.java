/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.test.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.ui.UIUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class HandlerObjectValidate extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (selection instanceof IStructuredSelection) {
            final Object element = ((IStructuredSelection) selection).getFirstElement();
            if (element instanceof DBNNode) {
                validateNode(HandlerUtil.getActiveShell(event), (DBNNode)element);
            }
        }
        return null;
    }

    private void validateNode(Shell shell, DBNNode element)
    {
        try {
            DBeaverUI.runInProgressService(new NodeValidator(element));
        } catch (InterruptedException e) {
            // skip
        } catch (InvocationTargetException e) {
            UIUtils.showErrorDialog(shell, "Validation failed", null, e.getTargetException());
        }
    }

    private static class NodeValidator implements DBRRunnableWithProgress {
        private final DBNNode rootNode;

        private NodeValidator(DBNNode rootNode)
        {
            this.rootNode = rootNode;
        }

        @Override
        public void run(DBRProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException
        {
            try {
                processNode(monitor, rootNode);
            } catch (DBException e) {
                throw new InvocationTargetException(e);
            }
        }

        private void processNode(DBRProgressMonitor monitor, DBNNode node)
            throws DBException
        {
            if (!node.allowsChildren()) {
                return;
            }
            final List<? extends DBNNode> children = node.getChildren(monitor);
            if (children != null) {
                for (DBNNode child : children) {
                    processNode(monitor, child);
                }
            }
        }
    }

}
