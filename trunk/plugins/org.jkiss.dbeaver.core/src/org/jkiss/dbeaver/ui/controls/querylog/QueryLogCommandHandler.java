/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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
        } else if (actionId.equals(ICommandIds.CMD_COPY_SPECIAL)) {
            logViewer.copySelectionToClipboard(true);
            return null;
        }
        return null;
    }

}
