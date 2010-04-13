/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    chris.gross@us.ibm.com - initial API and implementation
 *    Marty Jones<martybjones@gmail.com> - custom header/footer font in bug 293743
 *******************************************************************************/
package org.jkiss.dbeaver.ui.controls.grid.internal;

import org.jkiss.dbeaver.ui.controls.grid.GridColumn;
import org.jkiss.dbeaver.ui.controls.grid.GridFooterRenderer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

/**
 * The column footer renderer.
 *
 * @author Tom Schindl - tom.schindl@bestsolution.at
 * @since 2.0.0
 */
public class DefaultColumnFooterRenderer extends GridFooterRenderer
{

    int leftMargin = 6;

    int rightMargin = 6;

    int topMargin = 3;

    int bottomMargin = 3;

    int arrowMargin = 6;

    int imageSpacing = 3;

    /**
     * {@inheritDoc}
     */
    public Point computeSize(GC gc, int wHint, int hHint, Object value)
    {
        GridColumn column = (GridColumn)value;

        gc.setFont(column.getFooterFont());

        int x = 0;

        x += leftMargin;

        x += gc.stringExtent(column.getText()).x + rightMargin;

        int y = 0;

        y += topMargin;

        y += gc.getFontMetrics().getHeight();

        y += bottomMargin;

        if (column.getFooterImage() != null)
        {
            x += column.getFooterImage().getBounds().width + imageSpacing;

            y = Math.max(y, topMargin + column.getFooterImage().getBounds().height + bottomMargin);
        }

        return new Point(x, y);
    }

    /**
     * {@inheritDoc}
     */
    public void paint(GC gc, Object value)
    {
        GridColumn column = (GridColumn)value;

        // set the font to be used to display the text.
        gc.setFont(column.getFooterFont());

        gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));

        gc.fillRectangle(getBounds().x, getBounds().y, getBounds().width,
                         getBounds().height);

        gc.drawLine(getBounds().x, getBounds().y, getBounds().x
                + getBounds().width, getBounds().y);

        int x = leftMargin;

        if (column.getFooterImage() != null)
        {
                gc.drawImage(column.getFooterImage(), getBounds().x + x,
                        getBounds().y + getBounds().height - bottomMargin - column.getFooterImage().getBounds().height);
            x += column.getFooterImage().getBounds().width + imageSpacing;
        }

        int width = getBounds().width - x;

        if (column.getSort() == SWT.NONE)
        {
            width -= rightMargin;
        }


        gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));

        int y = getBounds().y + getBounds().height - bottomMargin - gc.getFontMetrics().getHeight();

        String text = TextUtils.getShortString(gc, column.getFooterText(), width);

        if (column.getAlignment() == SWT.RIGHT)
        {
            int len = gc.stringExtent(text).x;
            if (len < width)
            {
                x += width - len;
            }
        }
        else if (column.getAlignment() == SWT.CENTER)
        {
            int len = gc.stringExtent(text).x;
            if (len < width)
            {
                x += (width - len) / 2;
            }
        }


        gc.drawString(text, getBounds().x + x,
                      y,true);

    }

    /**
     * {@inheritDoc}
     */
    public boolean notify(int event, Point point, Object value)
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public Rectangle getTextBounds(Object value, boolean preferred)
    {
        GridColumn column = (GridColumn)value;

        int x = leftMargin;

        if (column.getImage() != null)
        {
            x += column.getImage().getBounds().width + imageSpacing;
        }



        GC gc = new GC(column.getParent());
        gc.setFont(column.getFooterFont());
        int y = getBounds().height - bottomMargin - gc.getFontMetrics().getHeight();

        Rectangle bounds = new Rectangle(x,y,0,0);

        Point p = gc.stringExtent(column.getText());

        bounds.height = p.y;

        if (preferred)
        {
            bounds.width = p.x;
        }
        else
        {
            int width = getBounds().width - x;
            if (column.getSort() == SWT.NONE)
            {
                width -= rightMargin;
            }

            bounds.width = width;
        }


        gc.dispose();

        return bounds;
    }
}
