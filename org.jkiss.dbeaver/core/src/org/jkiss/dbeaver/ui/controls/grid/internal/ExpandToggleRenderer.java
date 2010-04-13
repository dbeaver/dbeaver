/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.grid.internal;

import org.jkiss.dbeaver.ui.controls.grid.AbstractRenderer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;

/**
 * @author chris.gross@us.ibm.com
 * @since 2.0.0
 */
public class ExpandToggleRenderer extends AbstractRenderer
{

    /**
     * 
     */
    public ExpandToggleRenderer()
    {
        super();
        setSize(11, 9);
    }

    /**
     * {@inheritDoc}
     */
    public void paint(GC gc, Object value)
    {
        Color innerColor = null;
        Color outerColor = getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);

        if (isHover())
        {
            innerColor = getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND);
        }
        else
        {
            innerColor = getDisplay().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND);
        }

        if (isExpanded())
        {
            drawLeftPointingLine(gc, innerColor, outerColor, 0);
            drawLeftPointingLine(gc, innerColor, outerColor, 5);
        }
        else
        {
            drawRightPointingLine(gc, innerColor, outerColor, 0);
            drawRightPointingLine(gc, innerColor, outerColor, 5);
        }

    }

    private void drawRightPointingLine(GC gc, Color innerColor, Color outerColor, int xOffset)
    {
        gc.setForeground(outerColor);
        gc.drawLine(getBounds().x + 1 + xOffset, getBounds().y, getBounds().x + 5 + xOffset,
                    getBounds().y + 4);
        gc.drawLine(getBounds().x + 4 + xOffset, getBounds().y + 5, getBounds().x + 1 + xOffset,
                    getBounds().y + 8);
        gc.drawPoint(getBounds().x + xOffset, getBounds().y + 7);
        gc.drawLine(getBounds().x + xOffset, getBounds().y + 6, getBounds().x + 2 + xOffset,
                    getBounds().y + 4);
        gc.drawLine(getBounds().x + 1 + xOffset, getBounds().y + 3, getBounds().x + xOffset,
                    getBounds().y + 2);
        gc.drawPoint(getBounds().x + xOffset, getBounds().y + 1);

        gc.setForeground(innerColor);
        gc.drawLine(getBounds().x + 1 + xOffset, getBounds().y + 1, getBounds().x + 4 + xOffset,
                    getBounds().y + 4);
        gc.drawLine(getBounds().x + 1 + xOffset, getBounds().y + 2, getBounds().x + 3 + xOffset,
                    getBounds().y + 4);
        gc.drawLine(getBounds().x + 3 + xOffset, getBounds().y + 5, getBounds().x + 1 + xOffset,
                    getBounds().y + 7);
        gc.drawLine(getBounds().x + 2 + xOffset, getBounds().y + 5, getBounds().x + 1 + xOffset,
                    getBounds().y + 6);
    }

    private void drawLeftPointingLine(GC gc, Color innerColor, Color outerColor, int xOffset)
    {
        gc.setForeground(outerColor);
        gc.drawLine(getBounds().x + xOffset, getBounds().y + 4, getBounds().x + 4 + xOffset,
                    getBounds().y);
        gc.drawPoint(getBounds().x + 5 + xOffset, getBounds().y + 1);
        gc.drawLine(getBounds().x + 5 + xOffset, getBounds().y + 2, getBounds().x + 3 + xOffset,
                    getBounds().y + 4);
        gc.drawPoint(getBounds().x + 4 + xOffset, getBounds().y + 5);
        gc.drawLine(getBounds().x + 5 + xOffset, getBounds().y + 6, getBounds().x + 5 + xOffset,
                    getBounds().y + 7);
        gc.drawLine(getBounds().x + 4 + xOffset, getBounds().y + 8, getBounds().x + 1 + xOffset,
                    getBounds().y + 5);

        gc.setForeground(innerColor);
        gc.drawLine(getBounds().x + 1 + xOffset, getBounds().y + 4, getBounds().x + 4 + xOffset,
                    getBounds().y + 1);
        gc.drawLine(getBounds().x + 2 + xOffset, getBounds().y + 4, getBounds().x + 4 + xOffset,
                    getBounds().y + 2);
        gc.drawLine(getBounds().x + 2 + xOffset, getBounds().y + 5, getBounds().x + 4 + xOffset,
                    getBounds().y + 7);
        gc.drawLine(getBounds().x + 2 + xOffset, getBounds().y + 4, getBounds().x + 4 + xOffset,
                    getBounds().y + 6);
    }

    /**
     * {@inheritDoc}
     */
    public Point computeSize(GC gc, int wHint, int hHint, Object value)
    {
        return new Point(11, 9);
    }

}
