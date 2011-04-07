/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.project.BookmarksHandlerImpl;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;
import org.jkiss.dbeaver.utils.ViewUtils;

public class NavigatorHandlerAddBookmark extends NavigatorHandlerObjectBase {

    public Object execute(ExecutionEvent event) throws ExecutionException {
        final Shell activeShell = HandlerUtil.getActiveShell(event);
        final ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (!selection.isEmpty() && selection instanceof IStructuredSelection) {
            final DBNNode node = ViewUtils.getSelectedNode((IStructuredSelection) selection);
            if (node instanceof DBNDatabaseNode) {
                try {
                    final String title = EnterNameDialog.chooseName(activeShell, "Bookmark Name", node.getNodeName());
                    if (title != null) {
                        BookmarksHandlerImpl.createBookmark((DBNDatabaseNode) node, title, null);
                    }
                } catch (DBException e) {
                    UIUtils.showErrorDialog(activeShell, "New Bookmark", "Can't create new bookmark", e);
                }
            }
        }
        return null;
    }

}