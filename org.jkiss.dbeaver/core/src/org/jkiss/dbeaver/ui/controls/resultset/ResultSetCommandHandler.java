/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.jkiss.dbeaver.ui.controls.spreadsheet.Spreadsheet;
import org.jkiss.dbeaver.ui.controls.spreadsheet.SpreadsheetCommandHandler;

/**
 * ResultSetCommandHandler
 */
public class ResultSetCommandHandler extends SpreadsheetCommandHandler {

    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        Spreadsheet spreadsheet = getActiveSpreadsheet(event);
        if (spreadsheet == null) {
            return null;
        }
        if (!(spreadsheet.getController() instanceof ResultSetViewer)) {
            return null;
        }
        ResultSetViewer resultSet = (ResultSetViewer) spreadsheet.getController();
        //ResultSetViewer.ResultSetMode resultSetMode = resultSet.getMode();
        String actionId = event.getCommand().getId();
        if (actionId.equals(IWorkbenchCommandConstants.FILE_REFRESH)) {
            resultSet.refresh();
        } else if (actionId.equals(ITextEditorActionDefinitionIds.WORD_PREVIOUS)) {
            resultSet.scrollToRow(ResultSetViewer.RowPosition.PREVIOUS);
        } else if (actionId.equals(ITextEditorActionDefinitionIds.WORD_NEXT)) {
            resultSet.scrollToRow(ResultSetViewer.RowPosition.NEXT);
        } else if (actionId.equals(ITextEditorActionDefinitionIds.SELECT_WORD_PREVIOUS)) {
            resultSet.scrollToRow(ResultSetViewer.RowPosition.FIRST);
        } else if (actionId.equals(ITextEditorActionDefinitionIds.SELECT_WORD_NEXT)) {
            resultSet.scrollToRow(ResultSetViewer.RowPosition.LAST);
        }


        return null;
    }

}