/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.grid.internal;

import org.jkiss.dbeaver.ui.controls.grid.AbstractRenderer;
import org.jkiss.dbeaver.ui.controls.grid.Grid;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;

/**
 * The empty row header renderer.
 *
 * @author chris.gross@us.ibm.com
 * @since 2.0.0
 */
public class DefaultEmptyRowHeaderRenderer extends AbstractRenderer
{

    /** 
     * {@inheritDoc}
     */
    public void paint(GC gc, Object value)
    {

        gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

        gc.fillRectangle(getBounds().x, getBounds().y, getBounds().width, getBounds().height + 1);

        gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW));

        Grid grid = (Grid) value;
        
        if (!grid.getCellSelectionEnabled())
        {
        
            gc.drawLine(getBounds().x, getBounds().y, getBounds().x + getBounds().width - 1,
                        getBounds().y);
            gc.drawLine(getBounds().x, getBounds().y, getBounds().x, getBounds().y + getBounds().height
                                                                     - 1);
    
            gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
            gc.drawLine(getBounds().x + 1, getBounds().y + 1,
                        getBounds().x + getBounds().width - 2, getBounds().y + 1);
            gc.drawLine(getBounds().x + 1, getBounds().y + 1, getBounds().x + 1,
                        getBounds().y + getBounds().height - 2);
    
            gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));
            gc.drawLine(getBounds().x + getBounds().width - 1, getBounds().y, getBounds().x
                                                                              + getBounds().width - 1,
                        getBounds().y + getBounds().height - 1);
            gc.drawLine(getBounds().x, getBounds().y + getBounds().height - 1, getBounds().x
                                                                               + getBounds().width - 1,
                        getBounds().y + getBounds().height - 1);
    
            gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
            gc.drawLine(getBounds().x + getBounds().width - 2, getBounds().y + 1,
                        getBounds().x + getBounds().width - 2, getBounds().y + getBounds().height
                                                               - 2);
            gc.drawLine(getBounds().x + 1, getBounds().y + getBounds().height - 2,
                        getBounds().x + getBounds().width - 2, getBounds().y + getBounds().height
                                                               - 2);
        }
        else
        {
            gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));

            gc.drawLine(getBounds().x + getBounds().width - 1, getBounds().y, getBounds().x
                                                                              + getBounds().width - 1,
                        getBounds().y + getBounds().height - 1);
            gc.drawLine(getBounds().x, getBounds().y + getBounds().height - 1, getBounds().x
                                                                               + getBounds().width - 1,
                        getBounds().y + getBounds().height - 1);
        }

    }

    /** 
     * {@inheritDoc}
     */
    public Point computeSize(GC gc, int wHint, int hHint, Object value)
    {
        return new Point(wHint, hHint);
    }

}
