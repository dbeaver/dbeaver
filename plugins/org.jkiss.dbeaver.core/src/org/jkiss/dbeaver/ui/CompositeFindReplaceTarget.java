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
public class CompositeFindReplaceTarget implements IFindReplaceTarget, IFindReplaceTargetExtension, IFindReplaceTargetExtension3 {

    private final IFindReplaceTarget[] targets;

    public CompositeFindReplaceTarget(IFindReplaceTarget[] targets)
    {
        this.targets = targets;
    }

    private IFindReplaceTarget getCurrentTarget()
    {
        for (IFindReplaceTarget target : targets) {
            if (target.canPerformFind()) {
                return target;
            }
        }
        return targets[0];
    }

    @Override
    public boolean canPerformFind()
    {
        return getCurrentTarget().canPerformFind();
    }

    @Override
    public int findAndSelect(int widgetOffset, String findString, boolean searchForward, boolean caseSensitive, boolean wholeWord)
    {
        return getCurrentTarget().findAndSelect(widgetOffset, findString, searchForward, caseSensitive, wholeWord);
    }

    @Override
    public Point getSelection()
    {
        return getCurrentTarget().getSelection();
    }

    @Override
    public String getSelectionText()
    {
        return getCurrentTarget().getSelectionText();
    }

    @Override
    public boolean isEditable()
    {
        return getCurrentTarget().isEditable();
    }

    @Override
    public void replaceSelection(String text)
    {
        getCurrentTarget().replaceSelection(text);
    }

    @Override
    public void beginSession()
    {
        IFindReplaceTarget target = getCurrentTarget();
        if (target instanceof IFindReplaceTargetExtension) {
            ((IFindReplaceTargetExtension) target).beginSession();
        }
    }

    @Override
    public void endSession()
    {
        IFindReplaceTarget target = getCurrentTarget();
        if (target instanceof IFindReplaceTargetExtension) {
            ((IFindReplaceTargetExtension) target).endSession();
        }
    }

    @Override
    public IRegion getScope()
    {
        IFindReplaceTarget target = getCurrentTarget();
        if (target instanceof IFindReplaceTargetExtension) {
            return ((IFindReplaceTargetExtension) target).getScope();
        }
        return null;
    }

    @Override
    public void setScope(IRegion scope)
    {
        IFindReplaceTarget target = getCurrentTarget();
        if (target instanceof IFindReplaceTargetExtension) {
            ((IFindReplaceTargetExtension) target).setScope(scope);
        }
    }

    @Override
    public Point getLineSelection()
    {
        IFindReplaceTarget target = getCurrentTarget();
        if (target instanceof IFindReplaceTargetExtension) {
            return ((IFindReplaceTargetExtension) target).getLineSelection();
        }
        return getSelection();
    }

    @Override
    public void setSelection(int offset, int length)
    {
        IFindReplaceTarget target = getCurrentTarget();
        if (target instanceof IFindReplaceTargetExtension) {
            ((IFindReplaceTargetExtension) target).setSelection(offset, length);
        }
    }

    @Override
    public void setScopeHighlightColor(Color color)
    {
        IFindReplaceTarget target = getCurrentTarget();
        if (target instanceof IFindReplaceTargetExtension) {
            ((IFindReplaceTargetExtension) target).setScopeHighlightColor(color);
        }
    }

    @Override
    public void setReplaceAllMode(boolean replaceAll)
    {
        IFindReplaceTarget target = getCurrentTarget();
        if (target instanceof IFindReplaceTargetExtension) {
            ((IFindReplaceTargetExtension) target).setReplaceAllMode(replaceAll);
        }
    }

    @Override
    public int findAndSelect(int offset, String findString, boolean searchForward, boolean caseSensitive, boolean wholeWord, boolean regExSearch)
    {
        IFindReplaceTarget target = getCurrentTarget();
        if (target instanceof IFindReplaceTargetExtension3) {
            return ((IFindReplaceTargetExtension3) target).findAndSelect(offset, findString, searchForward, caseSensitive, wholeWord, regExSearch);
        }
        return findAndSelect(offset, findString, searchForward, caseSensitive, wholeWord);
    }

    @Override
    public void replaceSelection(String text, boolean regExReplace)
    {
        IFindReplaceTarget target = getCurrentTarget();
        if (target instanceof IFindReplaceTargetExtension3) {
            ((IFindReplaceTargetExtension3) target).replaceSelection(text, regExReplace);
        }
    }

}
