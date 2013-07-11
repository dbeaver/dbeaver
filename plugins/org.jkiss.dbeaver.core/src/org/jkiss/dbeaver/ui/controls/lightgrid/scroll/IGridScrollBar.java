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

package  org.jkiss.dbeaver.ui.controls.lightgrid.scroll;

import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ScrollBar;

/**
 * Used by Grid to externalize the scrollbars from the table itself.
 * 
 * @author chris.gross@us.ibm.com
 * @version 1.0.0
 */
public interface IGridScrollBar
{

    public ScrollBar getControl();
    /**
     * Returns the scrollbar's visibility.
     * 
     * @return true if the scrollbar is visible.
     */
    public boolean getVisible();

    /**
     * Sets the scrollbar's visibility.
     * 
     * @param visible visibilty
     */
    public void setVisible(boolean visible);

    /**
     * Returns the selection.
     * 
     * @return the selection.
     */
    public int getSelection();

    /**
     * Sets the selection.
     * 
     * @param selection selection to set
     */
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
    public void setValues(int selection, int min, int max, int thumb, int increment,
                          int pageIncrement);


    /**
     * @param e
     */
    public void handleMouseWheel(Event e);
    
    /**
     * @param min
     */
    public void setMinimum(int min);
    
    /**
     * @return min
     */
    public int getMinimum();
    
    /**
     * @param max
     */
    public void setMaximum(int max);
    
    /**
     * @return max
     */
    public int getMaximum();
    
    /**
     * @param thumb
     */
    public void setThumb(int thumb);
    
    /**
     * @return thumb
     */
    public int getThumb();
    
    /**
     * @param increment
     */
    public void setIncrement(int increment);
    
    /**
     * @return increment
     */
    public int getIncrement();
    
    /**
     * @param page
     */
    public void setPageIncrement(int page);
    
    /**
     * @return page increment
     */
    public int getPageIncrement();
    
    /**
     * @param listener
     */
    public void addSelectionListener(SelectionListener listener);
    
    /**
     * @param listener
     */
    public void removeSelectionListener(SelectionListener listener);
}
