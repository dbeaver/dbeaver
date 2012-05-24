/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package  org.jkiss.dbeaver.ui.controls.lightgrid.renderers;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.jkiss.dbeaver.ui.controls.lightgrid.LightGrid;

/**
 * The empty cell renderer.
 *
 * @author chris.gross@us.ibm.com
 * @since 2.0.0
 */
public class DefaultEmptyCellRenderer extends GridCellRenderer
{
    public DefaultEmptyCellRenderer(LightGrid grid)
    {
        super(grid);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void paint(GC gc)
    {

        boolean drawBackground = true;
        
        if (isSelected())
        {
            gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION));
            gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));
        }
        else
        {
            if (grid.isEnabled())
            {
                drawBackground = false;
            }
            else
            {
                gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
            }
            gc.setForeground(grid.getForeground());
        }

        if (drawBackground)
            gc.fillRectangle(getBounds().x, getBounds().y, getBounds().width + 1,
                         getBounds().height);

        if (grid.getLinesVisible())
        {
            gc.setForeground(grid.getLineColor());
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
    @Override
    public boolean notify(int event, Point point, Object value)
    {
        return false;
    }
    
}
