/*
 * Copyright (C) 2010-2012 Serge Rieder
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
package org.jkiss.dbeaver.ui.controls.spreadsheet;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.ISources;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.jkiss.dbeaver.ui.ICommandIds;
import org.jkiss.dbeaver.ui.controls.lightgrid.LightGrid;

/**
 * Standard command handler
 */
public class SpreadsheetCommandHandler extends AbstractHandler {

    public static LightGrid getActiveGrid(ExecutionEvent event)
    {
        Object control = HandlerUtil.getVariable(event, ISources.ACTIVE_FOCUS_CONTROL_NAME);
        if (!(control instanceof LightGrid)) {
            return null;
        }
        return (LightGrid)control;
    }

    public static Spreadsheet getActiveSpreadsheet(ExecutionEvent event)
    {
        return Spreadsheet.getFromGrid(getActiveGrid(event));
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        Spreadsheet spreadsheet = getActiveSpreadsheet(event);
        if (spreadsheet == null) {
            return null;
        }
        LightGrid grid = spreadsheet.getGrid();

        String actionId = event.getCommand().getId();
        if (actionId.equals(IWorkbenchCommandConstants.EDIT_SELECT_ALL)) {
            grid.selectAll();
            return null;
        }
        if (actionId.equals(IWorkbenchCommandConstants.EDIT_COPY)) {
            spreadsheet.copySelectionToClipboard(false);
            return null;
        } else if (actionId.equals(ICommandIds.CMD_COPY_SPECIAL)) {
            spreadsheet.copySelectionToClipboard(true);
            return null;
        }
        Event keyEvent = new Event();
        keyEvent.doit = true;
        if (actionId.equals(ITextEditorActionDefinitionIds.LINE_START)) {
            keyEvent.keyCode = SWT.HOME;
        } else if (actionId.equals(ITextEditorActionDefinitionIds.SELECT_LINE_START)) {
            keyEvent.keyCode = SWT.HOME;
            keyEvent.stateMask = SWT.MOD2;
        } else if (actionId.equals(ITextEditorActionDefinitionIds.LINE_END)) {
            keyEvent.keyCode = SWT.END;
        } else if (actionId.equals(ITextEditorActionDefinitionIds.SELECT_LINE_END)) {
            keyEvent.keyCode = SWT.END;
            keyEvent.stateMask = SWT.MOD2;
        } else if (actionId.equals(ITextEditorActionDefinitionIds.TEXT_START)) {
            keyEvent.keyCode = SWT.HOME;
            keyEvent.stateMask = SWT.MOD1;
        } else if (actionId.equals(ITextEditorActionDefinitionIds.SELECT_TEXT_START)) {
            keyEvent.keyCode = SWT.HOME;
            keyEvent.stateMask = SWT.MOD1 | SWT.MOD2;
        } else if (actionId.equals(ITextEditorActionDefinitionIds.TEXT_END)) {
            keyEvent.keyCode = SWT.END;
            keyEvent.stateMask = SWT.MOD1;
        } else if (actionId.equals(ITextEditorActionDefinitionIds.SELECT_TEXT_END)) {
            keyEvent.keyCode = SWT.END;
            keyEvent.stateMask = SWT.MOD1 | SWT.MOD2;
        }
        grid.onKeyDown(keyEvent);
        return null;
    }

}
