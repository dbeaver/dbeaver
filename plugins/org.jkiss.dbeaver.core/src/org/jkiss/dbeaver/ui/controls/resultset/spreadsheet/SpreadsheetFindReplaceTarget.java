/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
    private boolean sessionActive = false;

    SpreadsheetFindReplaceTarget(SpreadsheetPresentation owner)
    {
        this.owner = owner;
    }

    public boolean isSessionActive() {
        return sessionActive;
    }

    public Pattern getSearchPattern() {
        return searchPattern;
    }

    public Color getScopeHighlightColor() {
        return scopeHighlightColor;
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
        this.sessionActive = true;
        this.owner.getControl().redraw();
    }

    @Override
    public void endSession()
    {
        this.sessionActive = false;
        this.searchPattern = null;
        this.owner.getControl().redraw();
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
        int columnCount = spreadsheet.getColumnCount();
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
                spreadsheet.setFocusColumn(curPosition.col);
                spreadsheet.setFocusItem(curPosition.row);
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
        owner.getController().updatePanelsContent(false);
    }

    @Override
    public String toString()
    {
        DBSDataContainer dataContainer = owner.getController().getDataContainer();
        return "Target: " + (dataContainer == null ? null : dataContainer.getName());
    }

}
