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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.IFindReplaceTargetExtension;
import org.eclipse.jface.text.IFindReplaceTargetExtension3;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridPos;
import org.jkiss.dbeaver.ui.controls.lightgrid.IGridContentProvider;
import org.jkiss.utils.CommonUtils;

/**
 * Find/Replace target for result set viewer
 */
class ResultSetFindReplaceTarget implements IFindReplaceTarget, IFindReplaceTargetExtension, IFindReplaceTargetExtension3 {

    static final Log log = LogFactory.getLog(ResultSetFindReplaceTarget.class);

    private final ResultSetViewer resultSet;
    private Color scopeHighlightColor;
    private boolean replaceAll;

    ResultSetFindReplaceTarget(ResultSetViewer resultSet)
    {

        this.resultSet = resultSet;
    }

    @Override
    public boolean canPerformFind()
    {
        return UIUtils.hasFocus(resultSet.getControl());
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
        String value = resultSet.getSpreadsheet().getContentLabelProvider().getText(selection);
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
        ILabelProvider labelProvider = resultSet.getSpreadsheet().getContentLabelProvider();
        ResultSetModel model = resultSet.getModel();
        int rowCount = model.getRowCount();
        int columnCount = model.getVisibleColumns().length;
        if (rowCount <= 0 || columnCount <= 0) {
            // Empty model
            return -1;
        }
        GridPos startPosition = resultSet.getSelection().getFirstElement();
        if (startPosition == null) {
            // From the beginning
            startPosition = new GridPos(0, 0);
        }
        for (GridPos curPosition = startPosition;;) {
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
            String cellText = labelProvider.getText(curPosition);
            if (cellText.contains(findString)) {
                resultSet.setSelection(
                    new StructuredSelection(curPosition), true);
                return curPosition.row;
            }
        }
    }

    @Override
    public void replaceSelection(String text, boolean regExReplace)
    {
    }

}
