/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.ui.NavigatorUtils;
import org.jkiss.dbeaver.ui.dnd.TreeNodeTransfer;

import java.util.Collection;

public class NavigatorHandlerObjectCreateCopy extends NavigatorHandlerObjectCreateBase {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);

        DBNNode curNode = NavigatorUtils.getSelectedNode(selection);
        if (curNode != null) {
            Collection<DBNNode> cbNodes = TreeNodeTransfer.getFromClipboard();
            if (cbNodes == null) {
                log.error("Clipboard contains data in unsupported format");
                return null;
            }
            for (DBNNode nodeObject : cbNodes) {
                if (nodeObject instanceof DBNDatabaseNode) {
                    createNewObject(HandlerUtil.getActiveWorkbenchWindow(event), curNode, ((DBNDatabaseNode)nodeObject));
                }
            }
        }
        return null;
    }

}