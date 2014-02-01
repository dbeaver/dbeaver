/*
 * Copyright (C) 2010-2014 Serge Rieder
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
 * The column header renderer.
 */
public class DefaultColumnHeaderRenderer extends GridColumnRenderer {

    private static final int leftMargin = 6;
    private static final int rightMargin = 6;
    //private int topMargin = 3;
    private static final int bottomMargin = 3;
    private static final int arrowMargin = 6;
    private static final int imageSpacing = 3;

    public DefaultColumnHeaderRenderer(LightGrid grid) {
        super(grid);

    }

    @Override
    public void paint(GC gc) {
        GridColumn col = grid.getColumn(getColumn());
        AbstractRenderer arrowRenderer = col.getSortRenderer();

        // set the font to be used to display the text.
        gc.setFont(getColumnFont());

        boolean flat = true;

        boolean drawSelected = ((isMouseDown() && isHover()));

        if (flat && isSelected()) {
            gc.setBackground(grid.getCellHeaderSelectionBackground());
            //gc.setForeground(grid.getCellHeaderSelectionForeground());
        } else {
            gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
            //gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND));
        }
        gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND));

        gc.fillRectangle(getBounds().x, getBounds().y, getBounds().width, getBounds().height);

        int pushedDrawingOffset = 0;
        if (drawSelected) {
            pushedDrawingOffset = 1;
        }

        int x = leftMargin;

        Image columnImage = getColumnImage();
        if (columnImage != null) {
            int y = bottomMargin;

            if (col.getHeaderControl() == null) {
                y = getBounds().y + pushedDrawingOffset + getBounds().height - bottomMargin - columnImage.getBounds().height;
            }

            gc.drawImage(columnImage, getBounds().x + x + pushedDrawingOffset, y);
            x += columnImage.getBounds().width + imageSpacing;
        }

        int width = getBounds().width - x;

        if (!col.isSortable()) {
            width -= rightMargin;
        } else {
            width -= arrowMargin + arrowRenderer.getSize().x + arrowMargin;
        }

        //gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));

        int y;

        if (col.getHeaderControl() == null) {
            y = getBounds().y + getBounds().height - bottomMargin - gc.getFontMetrics().getHeight();
        } else {
            y = getBounds().y + getBounds().height - bottomMargin - gc.getFontMetrics().getHeight() - computeControlSize(col).y;
        }

        String text = getColumnText();

        text = org.jkiss.dbeaver.ui.TextUtils.getShortString(gc, text, width);

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

        gc.drawString(text, getBounds().x + x + pushedDrawingOffset, y + pushedDrawingOffset, true);

        if (col.isSortable()) {
            if (col.getHeaderControl() == null) {
                y = getBounds().y
                    + ((getBounds().height - arrowRenderer.getBounds().height) / 2)
                    + 1;
            } else {
                y = getBounds().y
                    + ((getBounds().height - computeControlSize(col).y - arrowRenderer.getBounds().height) / 2)
                    + 1;
            }

            arrowRenderer.setSelected(col.getSort() == SWT.UP);
            if (drawSelected) {
                arrowRenderer
                    .setLocation(
                        getBounds().x + getBounds().width - arrowMargin
                            - arrowRenderer.getBounds().width + 1, y
                    );
            } else {
                if (col.getHeaderControl() == null) {
                    y = getBounds().y
                        + ((getBounds().height - arrowRenderer.getBounds().height) / 2);
                } else {
                    y = getBounds().y
                        + ((getBounds().height - computeControlSize(col).y - arrowRenderer.getBounds().height) / 2);
                }
                arrowRenderer
                    .setLocation(
                        getBounds().x + getBounds().width - arrowMargin
                            - arrowRenderer.getBounds().width, y);
            }
            arrowRenderer.paint(gc);
        }

        if (!flat) {

            if (drawSelected) {
                gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
            } else {
                gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW));
            }

            gc.drawLine(getBounds().x, getBounds().y, getBounds().x + getBounds().width - 1,
                getBounds().y);
            gc.drawLine(getBounds().x, getBounds().y, getBounds().x, getBounds().y + getBounds().height
                - 1);

            if (!drawSelected) {
                gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
                gc.drawLine(getBounds().x + 1, getBounds().y + 1,
                    getBounds().x + getBounds().width - 2, getBounds().y + 1);
                gc.drawLine(getBounds().x + 1, getBounds().y + 1, getBounds().x + 1,
                    getBounds().y + getBounds().height - 2);
            }

            if (drawSelected) {
                gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
            } else {
                gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));
            }
            gc.drawLine(getBounds().x + getBounds().width - 1, getBounds().y, getBounds().x
                + getBounds().width - 1,
                getBounds().y + getBounds().height - 1);
            gc.drawLine(getBounds().x, getBounds().y + getBounds().height - 1, getBounds().x
                + getBounds().width - 1,
                getBounds().y + getBounds().height - 1);

            if (!drawSelected) {
                gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
                gc.drawLine(getBounds().x + getBounds().width - 2, getBounds().y + 1,
                    getBounds().x + getBounds().width - 2, getBounds().y + getBounds().height
                        - 2);
                gc.drawLine(getBounds().x + 1, getBounds().y + getBounds().height - 2,
                    getBounds().x + getBounds().width - 2, getBounds().y + getBounds().height
                        - 2);
            }

        } else {
            gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));

            gc.drawLine(getBounds().x + getBounds().width - 1, getBounds().y, getBounds().x
                + getBounds().width - 1,
                getBounds().y + getBounds().height - 1);
            gc.drawLine(getBounds().x, getBounds().y + getBounds().height - 1, getBounds().x
                + getBounds().width - 1,
                getBounds().y + getBounds().height - 1);
        }

        gc.setFont(grid.getFont());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean notify(int event, Point point, Object value) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Rectangle getTextBounds(Object value, boolean preferred) {
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
            } else {
                width -= arrowMargin + column.getSortRenderer().getSize().x + arrowMargin;
            }
            bounds.width = width;
        }

        return bounds;
    }

    /**
     * @return the bounds reserved for the control
     */
    @Override
    public Rectangle getControlBounds(Object value, boolean preferred) {
        Rectangle bounds = getBounds();
        GridColumn column = (GridColumn) value;
        Point controlSize = computeControlSize(column);

        int y = getBounds().y + getBounds().height - bottomMargin - controlSize.y;

        return new Rectangle(bounds.x + 3, y, bounds.width - 6, controlSize.y);
    }

    private Point computeControlSize(GridColumn column) {
        if (column.getHeaderControl() != null) {
            return column.getHeaderControl().computeSize(SWT.DEFAULT, SWT.DEFAULT);
        }
        return new Point(0, 0);
    }

}
