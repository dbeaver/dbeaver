/*
 * Copyright (C) 2010-2013 Serge Rieder
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.IFindReplaceTargetExtension;
import org.eclipse.jface.text.IFindReplaceTargetExtension3;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridPos;
import org.jkiss.dbeaver.ui.controls.lightgrid.IGridContentProvider;
import org.jkiss.utils.CommonUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Find/Replace target for result set viewer
 */
class ResultSetFindReplaceTarget implements IFindReplaceTarget, IFindReplaceTargetExtension, IFindReplaceTargetExtension3 {

    static final Log log = LogFactory.getLog(ResultSetFindReplaceTarget.class);

    private final ResultSetViewer resultSet;
    private String searchString;
    private Color scopeHighlightColor;
    private boolean replaceAll;

    ResultSetFindReplaceTarget(ResultSetViewer resultSet)
    {

        this.resultSet = resultSet;
    }

    @Override
    public boolean canPerformFind()
    {
        return true;//resultSet.getModel().isEmpty();
    }

    @Override
    public int findAndSelect(int widgetOffset, String findString, boolean searchForward, boolean caseSensitive, boolean wholeWord)
    {
        return findAndSelect(widgetOffset, findString, searchForward, caseSensitive, wholeWord, false);
    }

    @Override
    public Point getSelection()
    {
        GridPos selection = resultSet.getSelection().getFirstElement();
        return selection == null ? new Point(-1, -1) : new Point(selection.col, selection.row);
    }

    @Override
    public String getSelectionText()
    {
        GridPos selection = resultSet.getSelection().getFirstElement();
        if (selection == null) {
            return "";
        }
        String value = resultSet.getSpreadsheet().getContentProvider().getElementText(selection);
        return CommonUtils.toString(value);
    }

    @Override
    public boolean isEditable()
    {
        return !resultSet.isReadOnly();
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
        resultSet.setSelection(
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
        IGridContentProvider contentProvider = resultSet.getSpreadsheet().getContentProvider();
        ResultSetModel model = resultSet.getModel();
        if (model.isEmpty()) {
            return -1;
        }
        int rowCount = model.getRowCount();
        int columnCount = model.getVisibleColumnCount();
        GridPos startPosition = resultSet.getSelection().getFirstElement();
        if (startPosition == null) {
            // From the beginning
            startPosition = new GridPos(0, 0);
        }
        Pattern findPattern = null;
        if (regExSearch) {
            try {
                findPattern = Pattern.compile(findString, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
            } catch (PatternSyntaxException e) {
                log.warn("Bad regex pattern: " + findString);
                return -1;
            }
        } else {
            if (!caseSensitive) {
                findString = findString.toLowerCase();
            }
        }
        for (GridPos curPosition = new GridPos(startPosition);;) {
            //Object element = contentProvider.getElement(curPosition);
            if (searchForward) {
                curPosition.col++;
                if (curPosition.col >= columnCount) {
                    curPosition.col = 0;
                    curPosition.row++;
                }
            } else {
                curPosition.col--;
                if (curPosition.col < 0) {
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
            String cellText = contentProvider.getElementText(curPosition);
            if (matchesValue(findString, findPattern, cellText, caseSensitive, wholeWord)) {
                resultSet.setSelection(
                    new StructuredSelection(curPosition), true);
                searchString = findString;
                return curPosition.row;
            }
        }
    }

    private boolean matchesValue(String findString, Pattern findPattern, String text, boolean caseSensitive, boolean wholeWord)
    {
        if (findPattern != null) {
            Matcher matcher = findPattern.matcher(text);
            return wholeWord ? matcher.matches() : matcher.find();
        }
        if (wholeWord) {
            return caseSensitive ? findString.equals(text) : findString.equalsIgnoreCase(text);
        } else {
            return caseSensitive ? text.contains(findString) : text.toLowerCase().contains(findString);
        }
    }

    @Override
    public void replaceSelection(String text, boolean regExReplace)
    {
        GridPos selection = resultSet.getSelection().getFirstElement();
        if (selection == null || !resultSet.isValidCell(selection)) {
            return;
        }
        String oldValue = resultSet.getSpreadsheet().getContentProvider().getElementText(selection);
        String newValue = text;
/*
        if (regExReplace) {
            newValue = oldValue.replaceAll(searchString, text);
        } else {
            newValue = oldValue.replace(searchString, text);
        }
*/

        selection = resultSet.translateVisualPos(selection);
        resultSet.getModel().getCellValue(selection.row, selection.col);
        resultSet.getModel().updateCellValue(selection.row, selection.col, newValue);

        resultSet.updateEditControls();
        resultSet.getSpreadsheet().redrawGrid();
        resultSet.previewValue();
    }

    @Override
    public String toString()
    {
        return "Target: " + resultSet.getDataContainer().getName();
    }

}
