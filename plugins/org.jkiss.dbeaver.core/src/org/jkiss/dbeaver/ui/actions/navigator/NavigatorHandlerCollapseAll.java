/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.ext.IDatabaseEditor;
import org.jkiss.dbeaver.ext.IDatabaseEditorInput;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.ui.views.navigator.database.DatabaseNavigatorView;
import org.jkiss.dbeaver.utils.ViewUtils;

public class NavigatorHandlerCollapseAll extends AbstractHandler {

    public Object execute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
        if (activePart instanceof DatabaseNavigatorView) {
            ((DatabaseNavigatorView)activePart).getNavigatorViewer().collapseAll();
        }
        return null;
    }
}