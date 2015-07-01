/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui;

import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.IFindReplaceTargetExtension;
import org.eclipse.jface.text.IFindReplaceTargetExtension3;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.jkiss.dbeaver.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Find/Replace target for StyledText
 */
public class StyledTextFindReplaceTarget implements IFindReplaceTarget, IFindReplaceTargetExtension, IFindReplaceTargetExtension3 {

    static final Log log = Log.getLog(StyledTextFindReplaceTarget.class);

    private final StyledText text;
    private Pattern searchPattern;
    private Color scopeHighlightColor;
    private boolean replaceAll;

    public StyledTextFindReplaceTarget(StyledText text) {
        this.text = text;
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
        return text.getSelection();
    }

    @Override
    public String getSelectionText()
    {
        return text.getSelectionText();
    }

    @Override
    public boolean isEditable()
    {
        return text.getEditable();
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
        text.setSelection(offset, length);
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
        if (offset == -1) {
            offset = 0;
        }
        int textLength = text.getCharCount();
        if (textLength <= 0 || offset >= textLength) {
            return -1;
        }
        String searchIn = text.getText();
        Matcher matcher = findPattern.matcher(searchIn);
        if (matcher.find(offset)) {
            text.setSelection(matcher.start(), matcher.end());
            return matcher.start();
        }
        return -1;
    }

    @Override
    public void replaceSelection(String str, boolean regExReplace)
    {
/*
        GridPos selection = (GridPos) owner.getSelection().getFirstElement();
        if (selection == null) {
            return;
        }
        GridCell cell = owner.getSpreadsheet().posToCell(selection);
        if (cell == null) {
            return;
        }
        String oldValue = owner.getSpreadsheet().getContentProvider().getCellText(cell.col, cell.row);
        String newValue = str;
        if (searchPattern != null) {
            newValue = searchPattern.matcher(oldValue).replaceAll(newValue);
        }

        boolean recordMode = owner.getController().isRecordMode();
        final DBDAttributeBinding attr = (DBDAttributeBinding)(recordMode ? cell.row : cell.col);
        final ResultSetRow row = (ResultSetRow)(recordMode ? cell.col : cell.row);
        owner.getController().getModel().updateCellValue(attr, row, newValue);
        owner.getController().updateValueView();
*/
    }

}
