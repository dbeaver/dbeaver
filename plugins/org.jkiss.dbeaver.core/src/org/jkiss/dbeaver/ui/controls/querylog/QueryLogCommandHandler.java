/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.querylog;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.ISources;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.ui.ICommandIds;

/**
 * QueryLogViewer command handler
 */
public class QueryLogCommandHandler extends AbstractHandler {

    public static QueryLogViewer getActiveQueryLog(ExecutionEvent event)
    {
        Control control = (Control)HandlerUtil.getVariable(event, ISources.ACTIVE_FOCUS_CONTROL_NAME);
        if (control != null) {
            Object controlData = control.getData();
            if (controlData instanceof QueryLogViewer) {
                return (QueryLogViewer) controlData;
            }
        }
        return null;
    }

    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        QueryLogViewer logViewer = getActiveQueryLog(event);
        if (logViewer == null) {
            return null;
        }

        String actionId = event.getCommand().getId();
        if (actionId.equals(IWorkbenchCommandConstants.EDIT_SELECT_ALL)) {
            logViewer.selectAll();
            return null;
        }
        if (actionId.equals(IWorkbenchCommandConstants.EDIT_COPY)) {
            logViewer.copySelectionToClipboard(false);
            return null;
        } else if (actionId.equals(ICommandIds.CMD_COPY_SPECIAL)) {
            logViewer.copySelectionToClipboard(true);
            return null;
        }
        return null;
    }

}
