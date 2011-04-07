/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.navigator;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;

public class NavigatorHandlerObjectRename extends NavigatorHandlerObjectBase {

    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);

        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structSelection = (IStructuredSelection)selection;
            Object element = structSelection.getFirstElement();
            if (element instanceof DBNNode) {
                DBNNode node = (DBNNode)element;
                Shell activeShell = HandlerUtil.getActiveShell(event);
                String newName = EnterNameDialog.chooseName(activeShell, "Rename " + node.getNodeType(), node.getNodeName());
                if (!CommonUtils.isEmpty(newName) && !newName.equals(node.getNodeName())) {
                    try {
                        node.rename(VoidProgressMonitor.INSTANCE, newName);
                    } catch (DBException e) {
                        UIUtils.showErrorDialog(activeShell, "Rename", "Can't rename object '" + node.getNodeName() + "'", e);
                    }
                }
            }
        }
        return null;
    }

}