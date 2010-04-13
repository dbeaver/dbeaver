/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    chris.gross@us.ibm.com - initial API and implementation
 *******************************************************************************/ 
package org.jkiss.dbeaver.ui.controls.grid.internal;

import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Event;

/**
 * Used by Grid to externalize the scrollbars from the table itself.
 * 
 * @author chris.gross@us.ibm.com
 * @version 1.0.0
 */
public interface IScrollBarProxy
{

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
