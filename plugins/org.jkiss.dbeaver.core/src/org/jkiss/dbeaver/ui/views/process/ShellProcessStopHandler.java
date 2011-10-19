/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.process;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.model.runtime.DBRProcessController;

public class ShellProcessStopHandler extends AbstractHandler {

    public Object execute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
        if (activePart instanceof DBRProcessController) {
            DBRProcessController view = (DBRProcessController)activePart;
            view.terminateProcess();
        }
        return null;
    }

}