/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.common;

import org.eclipse.jface.action.Action;

public class EmptyListAction extends Action
{
    @Override
    public String getText()
    {
        return "<Empty>";
    }

    @Override
    public boolean isEnabled()
    {
        return false;
    }
}