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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;

/**
 * Composite find/replace target
 */
public class DynamicFindReplaceTarget implements IFindReplaceTarget, IFindReplaceTargetExtension, IFindReplaceTargetExtension3 {

    private IFindReplaceTarget target;

    public DynamicFindReplaceTarget()
    {
    }

    public IFindReplaceTarget getTarget()
    {
        return target;
    }

    public void setTarget(IFindReplaceTarget target)
    {
        this.target = target;
    }

    @Override
    public boolean canPerformFind()
    {
        IFindReplaceTarget t = getTarget();
        return t != null && t.canPerformFind();
    }

    @Override
    public int findAndSelect(int widgetOffset, String findString, boolean searchForward, boolean caseSensitive, boolean wholeWord)
    {
        IFindReplaceTarget t = getTarget();
        return t == null ? -1 : t.findAndSelect(widgetOffset, findString, searchForward, caseSensitive, wholeWord);
    }

    @Override
    public Point getSelection()
    {
        IFindReplaceTarget t = getTarget();
        return t == null ? new Point(0, 0) : t.getSelection();
    }

    @Override
    public String getSelectionText()
    {
        IFindReplaceTarget t = getTarget();
        return t == null ? "" : t.getSelectionText();
    }

    @Override
    public boolean isEditable()
    {
        IFindReplaceTarget t = getTarget();
        return t != null && t.isEditable();
    }

    @Override
    public void replaceSelection(String text)
    {
        IFindReplaceTarget t = getTarget();
        if (t != null) {
            t.replaceSelection(text);
        }
    }

    @Override
    public void beginSession()
    {
        IFindReplaceTarget t = getTarget();
        if (t instanceof IFindReplaceTargetExtension) {
            ((IFindReplaceTargetExtension) t).beginSession();
        }
    }

    @Override
    public void endSession()
    {
        IFindReplaceTarget t = getTarget();
        if (t instanceof IFindReplaceTargetExtension) {
            ((IFindReplaceTargetExtension) t).endSession();
        }
    }

    @Override
    public IRegion getScope()
    {
        IFindReplaceTarget t = getTarget();
        if (t instanceof IFindReplaceTargetExtension) {
            return ((IFindReplaceTargetExtension) t).getScope();
        }
        return null;
    }

    @Override
    public void setScope(IRegion scope)
    {
        IFindReplaceTarget t = getTarget();
        if (t instanceof IFindReplaceTargetExtension) {
            ((IFindReplaceTargetExtension) t).setScope(scope);
        }
    }

    @Override
    public Point getLineSelection()
    {
        IFindReplaceTarget t = getTarget();
        if (t instanceof IFindReplaceTargetExtension) {
            return ((IFindReplaceTargetExtension) t).getLineSelection();
        }
        return getSelection();
    }

    @Override
    public void setSelection(int offset, int length)
    {
        IFindReplaceTarget t = getTarget();
        if (t instanceof IFindReplaceTargetExtension) {
            ((IFindReplaceTargetExtension) t).setSelection(offset, length);
        }
    }

    @Override
    public void setScopeHighlightColor(Color color)
    {
        IFindReplaceTarget t = getTarget();
        if (t instanceof IFindReplaceTargetExtension) {
            ((IFindReplaceTargetExtension) t).setScopeHighlightColor(color);
        }
    }

    @Override
    public void setReplaceAllMode(boolean replaceAll)
    {
        IFindReplaceTarget t = getTarget();
        if (t instanceof IFindReplaceTargetExtension) {
            ((IFindReplaceTargetExtension) t).setReplaceAllMode(replaceAll);
        }
    }

    @Override
    public int findAndSelect(int offset, String findString, boolean searchForward, boolean caseSensitive, boolean wholeWord, boolean regExSearch)
    {
        IFindReplaceTarget t = getTarget();
        if (t instanceof IFindReplaceTargetExtension3) {
            return ((IFindReplaceTargetExtension3) t).findAndSelect(offset, findString, searchForward, caseSensitive, wholeWord, regExSearch);
        }
        return findAndSelect(offset, findString, searchForward, caseSensitive, wholeWord);
    }

    @Override
    public void replaceSelection(String text, boolean regExReplace)
    {
        IFindReplaceTarget t = getTarget();
        if (t instanceof IFindReplaceTargetExtension3) {
            ((IFindReplaceTargetExtension3) t).replaceSelection(text, regExReplace);
        }
    }

}
