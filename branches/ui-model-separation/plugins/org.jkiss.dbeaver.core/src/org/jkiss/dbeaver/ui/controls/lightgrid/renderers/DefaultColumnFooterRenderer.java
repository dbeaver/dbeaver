/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.controls.lightgrid.renderers;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridColumn;
import org.jkiss.dbeaver.ui.controls.lightgrid.LightGrid;

/**
 * The column footer renderer.
 *
 * @author Tom Schindl - tom.schindl@bestsolution.at
 * @since 2.0.0
 */
public class DefaultColumnFooterRenderer extends GridColumnRenderer {

    private static final int leftMargin = 6;
    private static final int rightMargin = 6;
    //private int topMargin = 3;
    private static final int bottomMargin = 3;
    //private int arrowMargin = 6;
    private static final int imageSpacing = 3;

    public DefaultColumnFooterRenderer(LightGrid grid)
    {
        super(grid);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void paint(GC gc)
    {
        GridColumn col = grid.getColumn(getColumn());

        // set the font to be used to display the text.
        gc.setFont(getColumnFont());

        gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));

        gc.fillRectangle(getBounds().x, getBounds().y, getBounds().width,
            getBounds().height);

        gc.drawLine(getBounds().x, getBounds().y, getBounds().x
            + getBounds().width, getBounds().y);

        int x = leftMargin;

        Image columnImage = getColumnImage();
        if (columnImage != null) {
            gc.drawImage(columnImage, getBounds().x + x,
                getBounds().y + getBounds().height - bottomMargin - columnImage.getBounds().height);
            x += columnImage.getBounds().width + imageSpacing;
        }

        int width = getBounds().width - x;

        if (!col.isSortable()) {
            width -= rightMargin;
        }


        gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));

        int y = getBounds().y + getBounds().height - bottomMargin - gc.getFontMetrics().getHeight();

        String text = org.jkiss.dbeaver.ui.TextUtils.getShortString(gc, getColumnText(), width);

        if (col.getAlignment() == SWT.RIGHT) {
            int len = gc.stringExtent(text).x;
            if (len < width) {
                x += width - len;
            }
        } else if (col.getAlignment() == SWT.CENTER) {
            int len = gc.stringExtent(text).x;
            if (len < width) {
                x += (width - len) / 2;
            }
        }


        gc.drawString(text, getBounds().x + x,
            y, true);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean notify(int event, Point point, Object value)
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Rectangle getTextBounds(Object value, boolean preferred)
    {
        GridColumn column = (GridColumn) value;

        int x = leftMargin;

        if (column.getImage() != null) {
            x += column.getImage().getBounds().width + imageSpacing;
        }


        int y = getBounds().height - bottomMargin - grid.sizingGC.getFontMetrics().getHeight();

        Rectangle bounds = new Rectangle(x, y, 0, 0);

        Point p = grid.sizingGC.stringExtent(column.getText());

        bounds.height = p.y;

        if (preferred) {
            bounds.width = p.x;
        } else {
            int width = getBounds().width - x;
            if (!column.isSortable()) {
                width -= rightMargin;
            }

            bounds.width = width;
        }

        return bounds;
    }
}
