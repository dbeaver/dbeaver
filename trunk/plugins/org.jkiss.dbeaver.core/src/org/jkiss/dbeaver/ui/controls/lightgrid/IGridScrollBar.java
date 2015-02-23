/*
 * Copyright (C) 2010-2015 Serge Rieder
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

package org.jkiss.dbeaver.ui.controls.lightgrid;

import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Event;

/**
 * Used by Grid to externalize the scrollbars from the table itself.
 * 
 * @author chris.gross@us.ibm.com
 * @version 1.0.0
 */
public interface IGridScrollBar
{

    public int getWidth();

    public boolean getVisible();

    public void setVisible(boolean visible);

    public int getSelection();

    public void setSelection(int selection);

    /**
     * Sets the receiver's selection, minimum value, maximum value, thumb,
     * increment and page increment all at once.
     * 
     * @param selection selection
     * @param min minimum
     * @param max maximum
     * @param thumb thumb
     * @param increment increment
     * @param pageIncrement page increment
     */
    public void setValues(int selection, int min, int max, int thumb, int increment, int pageIncrement);

    public void handleMouseWheel(Event e);
    
    public void setMinimum(int min);
    
    public int getMinimum();
    
    public void setMaximum(int max);
    
    public int getMaximum();
    
    public void setThumb(int thumb);
    
    public int getThumb();
    
    public void setIncrement(int increment);
    
    public int getIncrement();
    
    public void setPageIncrement(int page);
    
    public int getPageIncrement();
    
    public void addSelectionListener(SelectionListener listener);
    
    public void removeSelectionListener(SelectionListener listener);
}
