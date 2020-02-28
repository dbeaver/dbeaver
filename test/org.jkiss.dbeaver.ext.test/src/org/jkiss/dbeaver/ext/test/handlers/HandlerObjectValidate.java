/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;

import java.lang.reflect.InvocationTargetException;

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
            UIUtils.runInProgressService(new NodeValidator(element));
        } catch (InterruptedException e) {
            // skip
        } catch (InvocationTargetException e) {
            //DBWorkbench.getPlatform().showError("Validation failed", null, e.getTargetException());
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
            if (!node.hasChildren(false)) {
                return;
            }
            final DBNNode[] children = node.getChildren(monitor);
            if (children != null) {
                for (DBNNode child : children) {
                    processNode(monitor, child);
                }
            }
        }
    }

}
