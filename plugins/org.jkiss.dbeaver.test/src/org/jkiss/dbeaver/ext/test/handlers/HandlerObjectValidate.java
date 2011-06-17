/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.test.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class HandlerObjectValidate extends AbstractHandler {

    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (selection instanceof IStructuredSelection) {
            final Object element = ((IStructuredSelection) selection).getFirstElement();
            if (element instanceof DBNNode) {
                validateNode((DBNNode)element);
            }
        }
        return null;
    }

    private void validateNode(DBNNode element)
    {
        DBeaverCore.getInstance().runInProgressDialog(new NodeValidator(element));
    }

    private static class NodeValidator implements DBRRunnableWithProgress {
        private final DBNNode rootNode;

        private NodeValidator(DBNNode rootNode)
        {
            this.rootNode = rootNode;
        }

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
