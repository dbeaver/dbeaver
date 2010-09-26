/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.ext.IDatabaseEditor;
import org.jkiss.dbeaver.ext.IDatabaseEditorInput;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.ui.views.navigator.database.DatabaseNavigatorView;
import org.jkiss.dbeaver.utils.ViewUtils;

public class NavigatorHandlerLinkEditor extends AbstractHandler {

    public Object execute(ExecutionEvent event) throws ExecutionException {
        IEditorPart activeEditor = HandlerUtil.getActiveEditor(event);
        if (activeEditor instanceof IDatabaseEditor) {
            IDatabaseEditorInput editorInput = ((IDatabaseEditor) activeEditor).getEditorInput();
            if (editorInput != null) {
                DBNNode dbnNode = editorInput.getTreeNode();
                if (dbnNode != null) {
                    DatabaseNavigatorView view = ViewUtils.findView(HandlerUtil.getActiveWorkbenchWindow(event), DatabaseNavigatorView.class);
                    if (view != null) {
                        view.showNode(dbnNode);
                    }
                }
            }
        }
        return null;
    }
}