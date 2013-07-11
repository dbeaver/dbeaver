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
        return this.target != null && this.target.canPerformFind();
    }

    @Override
    public int findAndSelect(int widgetOffset, String findString, boolean searchForward, boolean caseSensitive, boolean wholeWord)
    {
        return this.target.findAndSelect(widgetOffset, findString, searchForward, caseSensitive, wholeWord);
    }

    @Override
    public Point getSelection()
    {
        return this.target.getSelection();
    }

    @Override
    public String getSelectionText()
    {
        return this.target.getSelectionText();
    }

    @Override
    public boolean isEditable()
    {
        return this.target.isEditable();
    }

    @Override
    public void replaceSelection(String text)
    {
        this.target.replaceSelection(text);
    }

    @Override
    public void beginSession()
    {
        if (target instanceof IFindReplaceTargetExtension) {
            ((IFindReplaceTargetExtension) target).beginSession();
        }
    }

    @Override
    public void endSession()
    {
        if (target instanceof IFindReplaceTargetExtension) {
            ((IFindReplaceTargetExtension) target).endSession();
        }
    }

    @Override
    public IRegion getScope()
    {
        if (target instanceof IFindReplaceTargetExtension) {
            return ((IFindReplaceTargetExtension) target).getScope();
        }
        return null;
    }

    @Override
    public void setScope(IRegion scope)
    {
        if (target instanceof IFindReplaceTargetExtension) {
            ((IFindReplaceTargetExtension) target).setScope(scope);
        }
    }

    @Override
    public Point getLineSelection()
    {
        if (target instanceof IFindReplaceTargetExtension) {
            return ((IFindReplaceTargetExtension) target).getLineSelection();
        }
        return getSelection();
    }

    @Override
    public void setSelection(int offset, int length)
    {
        if (target instanceof IFindReplaceTargetExtension) {
            ((IFindReplaceTargetExtension) target).setSelection(offset, length);
        }
    }

    @Override
    public void setScopeHighlightColor(Color color)
    {
        if (target instanceof IFindReplaceTargetExtension) {
            ((IFindReplaceTargetExtension) target).setScopeHighlightColor(color);
        }
    }

    @Override
    public void setReplaceAllMode(boolean replaceAll)
    {
        if (target instanceof IFindReplaceTargetExtension) {
            ((IFindReplaceTargetExtension) target).setReplaceAllMode(replaceAll);
        }
    }

    @Override
    public int findAndSelect(int offset, String findString, boolean searchForward, boolean caseSensitive, boolean wholeWord, boolean regExSearch)
    {
        if (target instanceof IFindReplaceTargetExtension3) {
            return ((IFindReplaceTargetExtension3) target).findAndSelect(offset, findString, searchForward, caseSensitive, wholeWord, regExSearch);
        }
        return findAndSelect(offset, findString, searchForward, caseSensitive, wholeWord);
    }

    @Override
    public void replaceSelection(String text, boolean regExReplace)
    {
        if (target instanceof IFindReplaceTargetExtension3) {
            ((IFindReplaceTargetExtension3) target).replaceSelection(text, regExReplace);
        }
    }

}
