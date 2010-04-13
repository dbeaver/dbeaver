/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.grid.renderers;

import org.jkiss.dbeaver.ui.controls.grid.Grid;
import org.jkiss.dbeaver.ui.controls.grid.renderers.GridCellRenderer;
import org.jkiss.dbeaver.ui.controls.grid.GridItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;

/**
 * The empty cell renderer.
 *
 * @author chris.gross@us.ibm.com
 * @since 2.0.0
 */
public class DefaultEmptyCellRenderer extends GridCellRenderer
{

    /** 
     * {@inheritDoc}
     */
    public void paint(GC gc, Object value)
    {

        Grid table = null;
        if (value instanceof Grid)
            table = (Grid)value;

        GridItem item;
        if (value instanceof GridItem)
        {
            item = (GridItem)value;
            table = item.getParent();
        }

        boolean drawBackground = true;
        
        if (isSelected())
        {
            gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION));
            gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));
        }
        else
        {
            if (table.isEnabled())
            {
                drawBackground = false;
            }
            else
            {
                gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
            }
            gc.setForeground(table.getForeground());
        }

        if (drawBackground)
            gc.fillRectangle(getBounds().x, getBounds().y, getBounds().width + 1,
                         getBounds().height);

        if (table.getLinesVisible())
        {
            gc.setForeground(table.getLineColor());
            gc.drawLine(getBounds().x, getBounds().y + getBounds().height, getBounds().x
                                                                           + getBounds().width,
                        getBounds().y + getBounds().height);
            gc.drawLine(getBounds().x + getBounds().width - 1, getBounds().y, getBounds().x
                                                                              + getBounds().width
                                                                              - 1,
                        getBounds().y + getBounds().height);
        }
    }

    /** 
     * {@inheritDoc}
     */
    public Point computeSize(GC gc, int wHint, int hHint, Object value)
    {
        return new Point(wHint, hHint);
    }

    /** 
     * {@inheritDoc}
     */
    public boolean notify(int event, Point point, Object value)
    {
        return false;
    }
    
}
