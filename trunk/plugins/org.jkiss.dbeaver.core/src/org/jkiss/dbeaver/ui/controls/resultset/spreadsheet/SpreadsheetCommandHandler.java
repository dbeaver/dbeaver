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
package org.jkiss.dbeaver.ui.controls.resultset.spreadsheet;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.ui.ISources;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetPresentation;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetCommandHandler;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;

/**
 * Spreadsheet command handler.
 * Active when focus is in spreadsheet control
 */
public abstract class SpreadsheetCommandHandler extends AbstractHandler {

    public static final String CMD_TOGGLE_PREVIEW = "org.jkiss.dbeaver.core.resultset.grid.togglePreview";

    public static Spreadsheet getActiveSpreadsheet(ExecutionEvent event)
    {
        Object control = HandlerUtil.getVariable(event, ISources.ACTIVE_FOCUS_CONTROL_NAME);
        if (control instanceof Spreadsheet) {
            return (Spreadsheet)control;
        }

        ResultSetViewer rsv = ResultSetCommandHandler.getActiveResultSet(HandlerUtil.getActivePart(event));
        if (rsv != null) {
            IResultSetPresentation activePresentation = rsv.getActivePresentation();
            if (activePresentation instanceof SpreadsheetPresentation) {
                return ((SpreadsheetPresentation) activePresentation).getSpreadsheet();
            }
        }

        return null;
    }

}
