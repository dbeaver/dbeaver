/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.controls.querylog;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.ISources;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.core.CoreCommands;

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

    @Override
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
        } else if (actionId.equals(CoreCommands.CMD_COPY_SPECIAL)) {
            logViewer.copySelectionToClipboard(true);
            return null;
        }
        return null;
    }

}
