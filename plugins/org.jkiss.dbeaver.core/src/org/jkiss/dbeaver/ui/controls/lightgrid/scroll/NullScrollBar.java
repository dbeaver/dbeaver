/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package  org.jkiss.dbeaver.ui.controls.lightgrid.scroll;

import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Event;

/**
 * A null-op scrollbar proxy.  Used when the grid is not showing scrollbars.
 *
 * @author chris.gross@us.ibm.com
 * @since 2.0.0
 */
public class NullScrollBar implements IGridScrollBar
{

    /** 
     * {@inheritDoc}
     */
    public boolean getVisible()
    {
        return false;
    }

    /** 
     * {@inheritDoc}
     */
    public void setVisible(boolean visible)
    {
    }

    /** 
     * {@inheritDoc}
     */
    public int getSelection()
    {
        return 0;
    }

    /** 
     * {@inheritDoc}
     */
    public void setSelection(int selection)
    {
    }

    /** 
     * {@inheritDoc}
     */
    public void setValues(int selection, int min, int max, int thumb, int increment,
                          int pageIncrement)
    {
    }

    /** 
     * {@inheritDoc}
     */
    public void handleMouseWheel(Event e)
    {
    }

    /** 
     * {@inheritDoc}
     */
    public void setMinimum(int min)
    {
    }

    /** 
     * {@inheritDoc}
     */
    public int getMinimum()
    {
        return 0;
    }

    /** 
     * {@inheritDoc}
     */
    public void setMaximum(int max)
    {
    }

    /** 
     * {@inheritDoc}
     */
    public int getMaximum()
    {
        return 0;
    }

    /** 
     * {@inheritDoc}
     */
    public void setThumb(int thumb)
    {
    }

    /** 
     * {@inheritDoc}
     */
    public int getThumb()
    {
        return 0;
    }

    /** 
     * {@inheritDoc}
     */
    public void setIncrement(int increment)
    {
    }

    /** 
     * {@inheritDoc}
     */
    public int getIncrement()
    {
        return 0;
    }

    /** 
     * {@inheritDoc}
     */
    public void setPageIncrement(int page)
    {
    }

    /** 
     * {@inheritDoc}
     */
    public int getPageIncrement()
    {
        return 0;
    }

    /** 
     * {@inheritDoc}
     */
    public void addSelectionListener(SelectionListener listener)
    { 
    }

    /** 
     * {@inheritDoc}
     */
    public void removeSelectionListener(SelectionListener listener)
    {
   
    }

}
