/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
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
        LightGrid grid = getActiveGrid(event);
        return grid != null && grid.getParent() instanceof Spreadsheet ? (Spreadsheet)grid.getParent() : null;
    }

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
            spreadsheet.copySelectionToClipboard();
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
