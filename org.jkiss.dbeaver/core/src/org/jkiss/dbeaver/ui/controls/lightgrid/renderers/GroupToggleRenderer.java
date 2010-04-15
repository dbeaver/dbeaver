/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package  org.jkiss.dbeaver.ui.controls.lightgrid.renderers;

import  org.jkiss.dbeaver.ui.controls.lightgrid.renderers.AbstractRenderer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;

/**
 * The renderer for the expand/collapse toggle on a column group header.
 *
 * @author chris.gross@us.ibm.com
 * @since 2.0.0
 */
public class GroupToggleRenderer extends AbstractRenderer
{

    /** 
     * {@inheritDoc}
     */
    public void paint(GC gc, Object value)
    {

        gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));
        gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
        gc.fillArc(getBounds().x, getBounds().y, 8, getBounds().height + -1, 90, 180);
        gc.drawArc(getBounds().x, getBounds().y, 8, getBounds().height + -1, 90, 180);

        gc.fillRectangle(getBounds().x + 4, getBounds().y, getBounds().width - 4,
                         getBounds().height);

        int yMid = ((getBounds().height - 1) / 2);

        if (isHover())
        {
            gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        }
        else
        {
            gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));
        }

        if (isExpanded())
        {
            gc.drawLine(getBounds().x + 5, getBounds().y + yMid, getBounds().x + 8, getBounds().y
                                                                                    + yMid - 3);
            gc.drawLine(getBounds().x + 6, getBounds().y + yMid, getBounds().x + 8, getBounds().y
                                                                                    + yMid - 2);

            gc.drawLine(getBounds().x + 5, getBounds().y + yMid, getBounds().x + 8, getBounds().y
                                                                                    + yMid + 3);
            gc.drawLine(getBounds().x + 6, getBounds().y + yMid, getBounds().x + 8, getBounds().y
                                                                                    + yMid + 2);

            gc.drawLine(getBounds().x + 9, getBounds().y + yMid, getBounds().x + 12, getBounds().y
                                                                                     + yMid - 3);
            gc.drawLine(getBounds().x + 10, getBounds().y + yMid, getBounds().x + 12, getBounds().y
                                                                                      + yMid - 2);

            gc.drawLine(getBounds().x + 9, getBounds().y + yMid, getBounds().x + 12, getBounds().y
                                                                                     + yMid + 3);
            gc.drawLine(getBounds().x + 10, getBounds().y + yMid, getBounds().x + 12, getBounds().y
                                                                                      + yMid + 2);
        }
        else
        {
            gc.drawLine(getBounds().x + getBounds().width - 5, getBounds().y + yMid,
                        getBounds().x + getBounds().width - 8, getBounds().y + yMid - 3);
            gc.drawLine(getBounds().x + getBounds().width - 6, getBounds().y + yMid,
                        getBounds().x + getBounds().width - 8, getBounds().y + yMid - 2);

            gc.drawLine(getBounds().x + getBounds().width - 5, getBounds().y + yMid,
                        getBounds().x + getBounds().width - 8, getBounds().y + yMid + 3);
            gc.drawLine(getBounds().x + getBounds().width - 6, getBounds().y + yMid,
                        getBounds().x + getBounds().width - 8, getBounds().y + yMid + 2);

            gc.drawLine(getBounds().x + getBounds().width - 9, getBounds().y + yMid,
                        getBounds().x + getBounds().width - 12, getBounds().y + yMid - 3);
            gc.drawLine(getBounds().x + getBounds().width - 10, getBounds().y + yMid,
                        getBounds().x + getBounds().width - 12, getBounds().y + yMid - 2);

            gc.drawLine(getBounds().x + getBounds().width - 9, getBounds().y + yMid,
                        getBounds().x + getBounds().width - 12, getBounds().y + yMid + 3);
            gc.drawLine(getBounds().x + getBounds().width - 10, getBounds().y + yMid,
                        getBounds().x + getBounds().width - 12, getBounds().y + yMid + 2);
        }

    }

    /** 
     * {@inheritDoc}
     */
    public Point computeSize(GC gc, int wHint, int hHint, Object value)
    {
        return new Point(0, 0);
    }
}
