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
package org.jkiss.dbeaver.ui.controls.lightgrid;

import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ScrollBar;

/**
 * A null-op scrollbar proxy.  Used when the grid is not showing scrollbars.
 *
 * @author chris.gross@us.ibm.com
 * @since 2.0.0
 */
class NullScrollBar implements IGridScrollBar
{

    @Override
    public int getWidth()
    {
        return 0;
    }

    @Override
    public boolean getVisible()
    {
        return false;
    }

    @Override
    public void setVisible(boolean visible)
    {
    }

    @Override
    public int getSelection()
    {
        return 0;
    }

    @Override
    public void setSelection(int selection)
    {
    }

    @Override
    public void setValues(int selection, int min, int max, int thumb, int increment, int pageIncrement)
    {
    }

    @Override
    public void handleMouseWheel(Event e)
    {
    }

    @Override
    public void setMinimum(int min)
    {
    }

    @Override
    public int getMinimum()
    {
        return 0;
    }

    @Override
    public void setMaximum(int max)
    {
    }

    @Override
    public int getMaximum()
    {
        return 0;
    }

    @Override
    public void setThumb(int thumb)
    {
    }

    @Override
    public int getThumb()
    {
        return 0;
    }

    @Override
    public void setIncrement(int increment)
    {
    }

    @Override
    public int getIncrement()
    {
        return 0;
    }

    @Override
    public void setPageIncrement(int page)
    {
    }

    @Override
    public int getPageIncrement()
    {
        return 0;
    }

    @Override
    public void addSelectionListener(SelectionListener listener)
    { 
    }

    @Override
    public void removeSelectionListener(SelectionListener listener)
    {
   
    }

}
