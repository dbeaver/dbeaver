/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package  org.jkiss.dbeaver.ui.controls.lightgrid.scroll;

import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ScrollBar;

/**
 * Adapts a normal scrollbar to the IScrollBar proxy.
 *
 * @author chris.gross@us.ibm.com
 * @since 2.0.0
 */
public class ScrollBarAdapter implements IGridScrollBar
{
    /**
     * Delegates to this scrollbar.
     */
    private ScrollBar scrollBar;

    /**
     * Contructs this adapter by delegating to the given scroll bar.
     * 
     * @param scrollBar delegate
     */
    public ScrollBarAdapter(ScrollBar scrollBar)
    {
        super();
        this.scrollBar = scrollBar;
    }

    /** 
     * {@inheritDoc}
     */
    public int getIncrement()
    {
        return scrollBar.getIncrement();
    }

    /** 
     * {@inheritDoc}
     */
    public int getMaximum()
    {
        return scrollBar.getMaximum();
    }

    /** 
     * {@inheritDoc}
     */
    public int getMinimum()
    {
        return scrollBar.getMinimum();
    }

    /** 
     * {@inheritDoc}
     */
    public int getPageIncrement()
    {
        return scrollBar.getPageIncrement();
    }

    /** 
     * {@inheritDoc}
     */
    public int getSelection()
    {
        return scrollBar.getSelection();
    }

    /** 
     * {@inheritDoc}
     */
    public int getThumb()
    {
        return scrollBar.getThumb();
    }

    /** 
     * {@inheritDoc}
     */
    public boolean getVisible()
    {
        return scrollBar.getVisible();
    }

    /** 
     * {@inheritDoc}
     */
    public void setIncrement(int value)
    {
        scrollBar.setIncrement(value);
    }

    /** 
     * {@inheritDoc}
     */
    public void setMaximum(int value)
    {
        scrollBar.setMaximum(value);
    }

    /** 
     * {@inheritDoc}
     */
    public void setMinimum(int value)
    {
        scrollBar.setMinimum(value);
    }

    /** 
     * {@inheritDoc}
     */
    public void setPageIncrement(int value)
    {
        scrollBar.setPageIncrement(value);
    }

    /** 
     * {@inheritDoc}
     */
    public void setSelection(int selection)
    {
        scrollBar.setSelection(selection);
    }

    /** 
     * {@inheritDoc}
     */
    public void setThumb(int value)
    {
        scrollBar.setThumb(value);
    }

    /** 
     * {@inheritDoc}
     */
    public void setValues(int selection, int minimum, int maximum, int thumb, int increment, 
                          int pageIncrement)
    {
        scrollBar.setValues(selection, minimum, maximum, thumb, increment, pageIncrement);
    }

    /** 
     * {@inheritDoc}
     */
    public void setVisible(boolean visible)
    {
        scrollBar.setVisible(visible);
    }

    /** 
     * {@inheritDoc}
     */
    public void handleMouseWheel(Event e)
    {
        //do nothing        
    }

    /** 
     * {@inheritDoc}
     */
    public void addSelectionListener(SelectionListener listener)
    {
        scrollBar.addSelectionListener(listener);
    }

    /** 
     * {@inheritDoc}
     */
    public void removeSelectionListener(SelectionListener listener)
    {
        scrollBar.removeSelectionListener(listener);
    }
}
