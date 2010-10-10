/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.qm;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;

public class QueryManagerFilterHandler extends AbstractHandler {

    public Object execute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
        if (activePart instanceof QueryManagerView) {
            ((QueryManagerView)activePart).openFilterDialog();
        }
        return null;
    }
}