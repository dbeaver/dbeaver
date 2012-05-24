/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls;

import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

/**
 * DefaultMenuCreator
 */
public class DefaultMenuCreator implements IMenuCreator
{

    @Override
    public void dispose()
    {
    }

    @Override
    public Menu getMenu(Control parent)
    {
        return null;
    }

    @Override
    public Menu getMenu(Menu parent)
    {
        return null;
    }
}
