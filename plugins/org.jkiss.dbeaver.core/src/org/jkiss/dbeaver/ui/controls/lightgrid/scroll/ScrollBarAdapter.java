/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
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
    @Override
    public int getIncrement()
    {
        return scrollBar.getIncrement();
    }

    /** 
     * {@inheritDoc}
     */
    @Override
    public int getMaximum()
    {
        return scrollBar.getMaximum();
    }

    /** 
     * {@inheritDoc}
     */
    @Override
    public int getMinimum()
    {
        return scrollBar.getMinimum();
    }

    /** 
     * {@inheritDoc}
     */
    @Override
    public int getPageIncrement()
    {
        return scrollBar.getPageIncrement();
    }

    /** 
     * {@inheritDoc}
     */
    @Override
    public int getSelection()
    {
        return scrollBar.getSelection();
    }

    /** 
     * {@inheritDoc}
     */
    @Override
    public int getThumb()
    {
        return scrollBar.getThumb();
    }

    /** 
     * {@inheritDoc}
     */
    @Override
    public boolean getVisible()
    {
        return scrollBar.getVisible();
    }

    /** 
     * {@inheritDoc}
     */
    @Override
    public void setIncrement(int value)
    {
        scrollBar.setIncrement(value);
    }

    /** 
     * {@inheritDoc}
     */
    @Override
    public void setMaximum(int value)
    {
        scrollBar.setMaximum(value);
    }

    /** 
     * {@inheritDoc}
     */
    @Override
    public void setMinimum(int value)
    {
        scrollBar.setMinimum(value);
    }

    /** 
     * {@inheritDoc}
     */
    @Override
    public void setPageIncrement(int value)
    {
        scrollBar.setPageIncrement(value);
    }

    /** 
     * {@inheritDoc}
     */
    @Override
    public void setSelection(int selection)
    {
        scrollBar.setSelection(selection);
    }

    /** 
     * {@inheritDoc}
     */
    @Override
    public void setThumb(int value)
    {
        scrollBar.setThumb(value);
    }

    /** 
     * {@inheritDoc}
     */
    @Override
    public void setValues(int selection, int minimum, int maximum, int thumb, int increment,
                          int pageIncrement)
    {
        scrollBar.setValues(selection, minimum, maximum, thumb, increment, pageIncrement);
    }

    /** 
     * {@inheritDoc}
     */
    p@Override
     ublic void setVisible(boolean visible)
    {
        scrollBar.setVisible(visible);
    }

    /** 
     * {@inheritDoc}
     */
    @Override
     ublic void handleMouseWheel(Event e)
    {
        //do nothing        
    }

    /** 
     * {@inheritDoc}
     */
    @Override
     ublic void addSelectionListener(SelectionListener listener)
    {
        scrollBar.addSelectionListener(listener);
    }

    /** 
     * {@inheritDoc}
     */
    @Override
     ublic void removeSelectionListener(SelectionListener listener)
    {
        scrollBar.removeSelectionListener(listener);
    }
}
