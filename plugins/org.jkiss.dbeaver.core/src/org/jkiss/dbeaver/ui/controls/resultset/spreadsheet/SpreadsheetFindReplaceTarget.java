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
package org.jkiss.dbeaver.ui.controls.resultset.spreadsheet;

import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.IFindReplaceTargetExtension;
import org.eclipse.jface.text.IFindReplaceTargetExtension3;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridCell;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridPos;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetModel;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetRow;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Find/Replace target for result set viewer
 */
class SpreadsheetFindReplaceTarget implements IFindReplaceTarget, IFindReplaceTargetExtension, IFindReplaceTargetExtension3 {

    private static final Log log = Log.getLog(SpreadsheetFindReplaceTarget.class);

    private final SpreadsheetPresentation owner;
    private Pattern searchPattern;
    private Color scopeHighlightColor;
    private boolean replaceAll;

    SpreadsheetFindReplaceTarget(SpreadsheetPresentation owner)
    {
        this.owner = owner;
    }

    @Override
    public boolean canPerformFind()
    {
        return true;
    }

    @Override
    public int findAndSelect(int widgetOffset, String findString, boolean searchForward, boolean caseSensitive, boolean wholeWord)
    {
        return findAndSelect(widgetOffset, findString, searchForward, caseSensitive, wholeWord, false);
    }

    @Override
    public Point getSelection()
    {
        Collection<GridPos> selection = owner.getSpreadsheet().getSelection();
        if (selection.isEmpty()) {
            return new Point(-1, -1);
        } else {
            GridPos pos = selection.iterator().next();
            return new Point(pos.col, pos.row);
        }
    }

    @Override
    public String getSelectionText()
    {
        GridPos selection = (GridPos) owner.getSelection().getFirstElement();
        if (selection == null) {
            return "";
        }
        Spreadsheet spreadsheet = owner.getSpreadsheet();
        GridCell cell = spreadsheet.posToCell(selection);
        String value = cell == null ? "" : spreadsheet.getContentProvider().getCellText(cell.col, cell.row);
        return CommonUtils.toString(value);
    }

    @Override
    public boolean isEditable()
    {
        return !owner.getController().isReadOnly();
    }

    @Override
    public void replaceSelection(String text)
    {
        replaceSelection(text, false);
    }

    @Override
    public void beginSession()
    {
    }

    @Override
    public void endSession()
    {
    }

    @Override
    public IRegion getScope()
    {
        return null;
    }

    @Override
    public void setScope(IRegion scope)
    {
    }

    @Override
    public Point getLineSelection()
    {
        return getSelection();
    }

    @Override
    public void setSelection(int offset, int length)
    {
        owner.setSelection(
            new StructuredSelection(
                new GridPos(offset, length)));
    }

    @Override
    public void setScopeHighlightColor(Color color)
    {
        this.scopeHighlightColor = color;
    }

    @Override
    public void setReplaceAllMode(boolean replaceAll)
    {
        this.replaceAll = replaceAll;
    }

    @Override
    public int findAndSelect(int offset, String findString, boolean searchForward, boolean caseSensitive, boolean wholeWord, boolean regExSearch)
    {
        searchPattern = null;

        ResultSetModel model = owner.getController().getModel();
        if (model.isEmpty()) {
            return -1;
        }
        Spreadsheet spreadsheet = owner.getSpreadsheet();
        int rowCount = spreadsheet.getItemCount();
        int columnCount = spreadsheet.getColumnsCount();
        Collection<GridPos> selection = spreadsheet.getSelection();
        GridPos startPosition = selection.isEmpty() ? null : selection.iterator().next();
        if (startPosition == null) {
            // From the beginning
            startPosition = new GridPos(0, 0);
        }
        Pattern findPattern;
        if (regExSearch) {
            try {
                findPattern = Pattern.compile(findString, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
            } catch (PatternSyntaxException e) {
                log.warn("Bad regex pattern: " + findString);
                return -1;
            }
        } else {
            findPattern = Pattern.compile(Pattern.quote(findString), caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
        }
        int minColumnNum = owner.getController().isRecordMode() ? -1 : 0;
        for (GridPos curPosition = new GridPos(startPosition);;) {
            //Object element = contentProvider.getElement(curPosition);
            if (searchForward) {
                curPosition.col++;
                if (curPosition.col >= columnCount) {
                    curPosition.col = minColumnNum;
                    curPosition.row++;
                }
            } else {
                curPosition.col--;
                if (curPosition.col < minColumnNum) {
                    curPosition.col = columnCount - 1;
                    curPosition.row--;
                }
            }
            if (curPosition.row < 0 || curPosition.row >= rowCount) {
                if (offset == -1) {
                    // Wrap search - redo search one more time
                    offset = 0;
                    if (searchForward) {
                        curPosition = new GridPos(0, 0);
                    } else {
                        curPosition = new GridPos(columnCount - 1, rowCount - 1);
                    }
                } else {
                    // Not found
                    return -1;
                }
            }
            String cellText;
            if (owner.getController().isRecordMode() && curPosition.col == minColumnNum) {
                // Header
                cellText = spreadsheet.getLabelProvider().getText(spreadsheet.getRowElement(curPosition.row));
            } else {
                GridCell cell = spreadsheet.posToCell(curPosition);
                if (cell != null) {
                    cellText = spreadsheet.getContentProvider().getCellText(cell.col, cell.row);
                } else {
                    continue;
                }
            }
            Matcher matcher = findPattern.matcher(cellText);
            if (wholeWord ? matcher.matches() : matcher.find()) {
                if (curPosition.col == minColumnNum) {
                    curPosition.col = 0;
                }
                spreadsheet.setCellSelection(curPosition);
                spreadsheet.showSelection();
                searchPattern = findPattern;
                return curPosition.row;
            }
        }
    }

    @Override
    public void replaceSelection(String text, boolean regExReplace)
    {
        GridPos selection = (GridPos) owner.getSelection().getFirstElement();
        if (selection == null) {
            return;
        }
        GridCell cell = owner.getSpreadsheet().posToCell(selection);
        if (cell == null) {
            return;
        }
        String oldValue = owner.getSpreadsheet().getContentProvider().getCellText(cell.col, cell.row);
        String newValue = text;
        if (searchPattern != null) {
            newValue = searchPattern.matcher(oldValue).replaceAll(newValue);
        }

        boolean recordMode = owner.getController().isRecordMode();
        final DBDAttributeBinding attr = (DBDAttributeBinding)(recordMode ? cell.row : cell.col);
        final ResultSetRow row = (ResultSetRow)(recordMode ? cell.col : cell.row);
        owner.getController().getModel().updateCellValue(attr, row, newValue);
        owner.getController().updateValueView();
    }

    @Override
    public String toString()
    {
        DBSDataContainer dataContainer = owner.getController().getDataContainer();
        return "Target: " + (dataContainer == null ? null : dataContainer.getName());
    }

}
