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
package  org.jkiss.dbeaver.ui.controls.lightgrid.scroll;

import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ScrollBar;

/**
 * A null-op scrollbar proxy.  Used when the grid is not showing scrollbars.
 *
 * @author chris.gross@us.ibm.com
 * @since 2.0.0
 */
public class NullScrollBar implements IGridScrollBar
{

    @Override
    public ScrollBar getControl()
    {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getVisible()
    {
        return false;
    }

    /** 
     * {@inheritDoc}
     */
    @Override
    public void setVisible(boolean visible)
    {
    }

    /** 
     * {@inheritDoc}
     */
    @Override
    public int getSelection()
    {
        return 0;
    }

    /** 
     * {@inheritDoc}
     */
    @Override
    public void setSelection(int selection)
    {
    }

    /** 
     * {@inheritDoc}
     */
    @Override
    public void setValues(int selection, int min, int max, int thumb, int increment,
                          int pageIncrement)
    {
    }

    /** 
     * {@inheritDoc}
     */
    @Override
    public void handleMouseWheel(Event e)
    {
    }

    /** 
     * {@inheritDoc}
     */
    @Override
    public void setMinimum(int min)
    {
    }

    /** 
     * {@inheritDoc}
     */
    @Override
    public int getMinimum()
    {
        return 0;
    }

    /** 
     * {@inheritDoc}
     */
    @Override
    public void setMaximum(int max)
    {
    }

    /** 
     * {@inheritDoc}
     */
    @Override
    public int getMaximum()
    {
        return 0;
    }

    /** 
     * {@inheritDoc}
     */
    @Override
    public void setThumb(int thumb)
    {
    }

    /** 
     * {@inheritDoc}
     */
    @Override
    public int getThumb()
    {
        return 0;
    }

    /** 
     * {@inheritDoc}
     */
    @Override
    public void setIncrement(int increment)
    {
    }

    /** 
     * {@inheritDoc}
     */
    @Override
    public int getIncrement()
    {
        return 0;
    }

    /** 
     * {@inheritDoc}
     */
    @Override
    public void setPageIncrement(int page)
    {
    }

    /** 
     * {@inheritDoc}
     */
    @Override
    public int getPageIncrement()
    {
        return 0;
    }

    /** 
     * {@inheritDoc}
     */
    @Override
    public void addSelectionListener(SelectionListener listener)
    { 
    }

    /** 
     * {@inheritDoc}
     */
    @Override
    public void removeSelectionListener(SelectionListener listener)
    {
   
    }

}
