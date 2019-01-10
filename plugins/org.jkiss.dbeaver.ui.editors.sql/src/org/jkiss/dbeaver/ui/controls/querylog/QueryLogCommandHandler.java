/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.controls.querylog;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.ISources;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.ui.IActionConstants;

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
        switch (actionId) {
            case IWorkbenchCommandConstants.EDIT_SELECT_ALL:
                logViewer.selectAll();
                return null;
            case IWorkbenchCommandConstants.EDIT_COPY:
                logViewer.copySelectionToClipboard(false);
                return null;
            case IActionConstants.CMD_COPY_SPECIAL:
                logViewer.copySelectionToClipboard(true);
                return null;
            case IWorkbenchCommandConstants.FILE_REFRESH:
                logViewer.refresh();
                return null;
        }
        return null;
    }

}
