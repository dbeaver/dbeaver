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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.ui.editors.MultiPageAbstractEditor;

/**
 * ResultSetCommandHandler
 */
public class ResultSetCommandHandler extends AbstractHandler {

    public static final String CMD_TOGGLE_MODE = "org.jkiss.dbeaver.core.resultset.toggleMode";
    public static final String CMD_SWITCH_PRESENTATION = "org.jkiss.dbeaver.core.resultset.switchPresentation";
    public static final String CMD_ROW_FIRST = "org.jkiss.dbeaver.core.resultset.row.first";
    public static final String CMD_ROW_PREVIOUS = "org.jkiss.dbeaver.core.resultset.row.previous";
    public static final String CMD_ROW_NEXT = "org.jkiss.dbeaver.core.resultset.row.next";
    public static final String CMD_ROW_LAST = "org.jkiss.dbeaver.core.resultset.row.last";
    public static final String CMD_ROW_EDIT = "org.jkiss.dbeaver.core.resultset.row.edit";
    public static final String CMD_ROW_EDIT_INLINE = "org.jkiss.dbeaver.core.resultset.row.edit.inline";
    public static final String CMD_ROW_ADD = "org.jkiss.dbeaver.core.resultset.row.add";
    public static final String CMD_ROW_COPY = "org.jkiss.dbeaver.core.resultset.row.copy";
    public static final String CMD_ROW_DELETE = "org.jkiss.dbeaver.core.resultset.row.delete";
    public static final String CMD_APPLY_CHANGES = "org.jkiss.dbeaver.core.resultset.applyChanges";
    public static final String CMD_REJECT_CHANGES = "org.jkiss.dbeaver.core.resultset.rejectChanges";

    @Nullable
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        ResultSetViewer resultSet = getActiveResultSet(HandlerUtil.getActivePart(event));
        if (resultSet == null) {
            return null;
        }
        String actionId = event.getCommand().getId();
        IResultSetPresentation presentation = resultSet.getActivePresentation();
        if (actionId.equals(IWorkbenchCommandConstants.FILE_REFRESH)) {
            resultSet.refresh();
        } else if (actionId.equals(CMD_TOGGLE_MODE)) {
            resultSet.toggleMode();
        } else if (actionId.equals(CMD_SWITCH_PRESENTATION)) {
            resultSet.switchPresentation();
        } else if (actionId.equals(CMD_ROW_PREVIOUS) || actionId.equals(ITextEditorActionDefinitionIds.WORD_PREVIOUS)) {
            presentation.scrollToRow(IResultSetPresentation.RowPosition.PREVIOUS);
        } else if (actionId.equals(CMD_ROW_NEXT) || actionId.equals(ITextEditorActionDefinitionIds.WORD_NEXT)) {
            presentation.scrollToRow(IResultSetPresentation.RowPosition.NEXT);
        } else if (actionId.equals(CMD_ROW_FIRST) || actionId.equals(ITextEditorActionDefinitionIds.SELECT_WORD_PREVIOUS)) {
            presentation.scrollToRow(IResultSetPresentation.RowPosition.FIRST);
        } else if (actionId.equals(CMD_ROW_LAST) || actionId.equals(ITextEditorActionDefinitionIds.SELECT_WORD_NEXT)) {
            presentation.scrollToRow(IResultSetPresentation.RowPosition.LAST);
        } else if (actionId.equals(CMD_ROW_EDIT)) {
            if (presentation instanceof IResultSetEditor) {
                ((IResultSetEditor) presentation).openValueEditor(false);
            }
        } else if (actionId.equals(CMD_ROW_EDIT_INLINE)) {
            if (presentation instanceof IResultSetEditor) {
                ((IResultSetEditor) presentation).openValueEditor(true);
            }
        } else if (actionId.equals(CMD_ROW_ADD)) {
            resultSet.addNewRow(false);
        } else if (actionId.equals(CMD_ROW_COPY)) {
            resultSet.addNewRow(true);
        } else if (actionId.equals(CMD_ROW_DELETE) || actionId.equals(IWorkbenchCommandConstants.EDIT_DELETE)) {
            resultSet.deleteSelectedRows();
        } else if (actionId.equals(CMD_APPLY_CHANGES)) {
            resultSet.applyChanges(null);
        } else if (actionId.equals(CMD_REJECT_CHANGES)) {
            resultSet.rejectChanges();
        } else if (actionId.equals(IWorkbenchCommandConstants.EDIT_COPY)) {
            ResultSetUtils.copyToClipboard(
                presentation.copySelectionToString(
                    false, false, false, null, DBDDisplayFormat.EDIT));
        } else if (actionId.equals(IWorkbenchCommandConstants.EDIT_PASTE)) {
            if (presentation instanceof IResultSetEditor) {
                ((IResultSetEditor) presentation).pasteFromClipboard();
            }
        } else if (actionId.equals(IWorkbenchCommandConstants.EDIT_CUT)) {
            ResultSetUtils.copyToClipboard(
                presentation.copySelectionToString(
                    false, false, true, null, DBDDisplayFormat.EDIT)
            );
        } else if (actionId.equals(ITextEditorActionDefinitionIds.SMART_ENTER)) {
            if (presentation instanceof IResultSetEditor) {
                ((IResultSetEditor) presentation).openValueEditor(false);
            }
        }


        return null;
    }

    public static ResultSetViewer getActiveResultSet(IWorkbenchPart activePart) {
        //IWorkbenchPart activePart = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();
        if (activePart instanceof IResultSetContainer) {
            return ((IResultSetContainer) activePart).getResultSetViewer();
        } else if (activePart instanceof MultiPageAbstractEditor) {
            return getActiveResultSet(((MultiPageAbstractEditor) activePart).getActiveEditor());
        }
        return null;
    }

}