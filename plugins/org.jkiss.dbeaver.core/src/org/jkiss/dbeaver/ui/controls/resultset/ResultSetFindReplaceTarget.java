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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridPos;
import org.jkiss.dbeaver.ui.editors.text.BaseTextEditor;
import org.jkiss.utils.CommonUtils;

/**
 * Find/Replace target for result set viewer
 */
class ResultSetFindReplaceTarget implements IFindReplaceTarget, IFindReplaceTargetExtension, IFindReplaceTargetExtension3 {

    static final Log log = LogFactory.getLog(ResultSetFindReplaceTarget.class);

    private final ResultSetViewer resultSet;
    private IFindReplaceTarget hostTarget;

    ResultSetFindReplaceTarget(ResultSetViewer resultSet)
    {
        IWorkbenchPartSite site = resultSet.getSite();
        IWorkbenchPart part = site.getPart();
        if (part instanceof BaseTextEditor) {
            hostTarget = ((BaseTextEditor) part).getViewer().getFindReplaceTarget();
        }
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
        return 0;
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
        return false;
    }

    @Override
    public void replaceSelection(String text)
    {
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
    }

    @Override
    public void setScopeHighlightColor(Color color)
    {
    }

    @Override
    public void setReplaceAllMode(boolean replaceAll)
    {
    }

    @Override
    public int findAndSelect(int offset, String findString, boolean searchForward, boolean caseSensitive, boolean wholeWord, boolean regExSearch)
    {
        return 0;
    }

    @Override
    public void replaceSelection(String text, boolean regExReplace)
    {
    }

}
