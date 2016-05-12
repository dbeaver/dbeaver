/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jkiss.dbeaver.ui.controls.lightgrid;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.TextUtils;
import org.jkiss.dbeaver.ui.UIIcon;

/**
 * Grid column renderer
 */
class GridColumnRenderer extends AbstractRenderer
{
    public static final int LEFT_MARGIN = 6;
    public static final int RIGHT_MARGIN = 6;
    public static final int BOTTOM_MARGIN = 3;
    public static final int TOP_MARGIN = 3;
    public static final int ARROW_MARGIN = 6;
    public static final int IMAGE_SPACING = 3;

    public static final Image IMAGE_ASTERISK = DBeaverIcons.getImage(UIIcon.SORT_UNKNOWN);
    public static final Image IMAGE_ARROW_UP = DBeaverIcons.getImage(UIIcon.SORT_DECREASE);
    public static final Image IMAGE_ARROW_DOWN = DBeaverIcons.getImage(UIIcon.SORT_INCREASE);
    public static final int SORT_WIDTH = 16;

    public  GridColumnRenderer(LightGrid grid) {
        super(grid);
    }

    public static Rectangle getSortControlBounds() {
        return IMAGE_ARROW_UP.getBounds();
    }

    @Nullable
    protected Image getColumnImage(Object element) {
        return grid.getLabelProvider().getImage(element);
    }

    protected String getColumnText(Object element)
    {
        return grid.getLabelProvider().getText(element);
    }
    
    protected Font getColumnFont(Object element) {
        Font font = grid.getLabelProvider().getFont(element);
        return font != null ? font : grid.normalFont;
    }

    public void paint(GC gc, Rectangle bounds, boolean selected, boolean hovering, Object element) {
        //GridColumn col = grid.getColumnByElement(cell.col);
        //AbstractRenderer arrowRenderer = col.getSortRenderer();
        int sortOrder = grid.getContentProvider().getSortOrder(element);
        Rectangle sortBounds = getSortControlBounds();

        // set the font to be used to display the text.
        gc.setFont(getColumnFont(element));

        boolean flat = true;

        boolean drawSelected = false;

        if (flat && (selected || hovering)) {
            gc.setBackground(grid.getCellHeaderSelectionBackground());
            //gc.setForeground(grid.getCellHeaderSelectionForeground());
        } else {
            gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
            //gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND));
        }
        gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND));

        gc.fillRectangle(bounds.x, bounds.y, bounds.width, bounds.height);

        int pushedDrawingOffset = 0;
        if (drawSelected) {
            pushedDrawingOffset = 1;
        }

        int x = LEFT_MARGIN;

        Image columnImage = getColumnImage(element);
        if (columnImage != null) {
            int y = bounds.y + pushedDrawingOffset + TOP_MARGIN;

            gc.drawImage(columnImage, bounds.x + x + pushedDrawingOffset, y);
            x += columnImage.getBounds().width + IMAGE_SPACING;
        }

        int width = bounds.width - x;

        if (sortOrder == SWT.NONE) {
            width -= RIGHT_MARGIN;
        } else {
            width -= ARROW_MARGIN + sortBounds.width + ARROW_MARGIN;
        }

        //gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));

        int y = bounds.y + TOP_MARGIN;

        String text = getColumnText(element);

        text = TextUtils.getShortString(grid.fontMetrics, text, width);

        gc.drawString(text, bounds.x + x + pushedDrawingOffset, y + pushedDrawingOffset, true);

        if (sortOrder != SWT.NONE) {
            y = bounds.y + TOP_MARGIN;

            if (drawSelected) {
                sortBounds.x = bounds.x + bounds.width - ARROW_MARGIN - sortBounds.width + 1;
                sortBounds.y = y;
            } else {
                sortBounds.x = bounds.x + bounds.width - ARROW_MARGIN - sortBounds.width;
                sortBounds.y = y;
            }
            paintSort(gc, sortBounds, sortOrder);
        }

        if (!flat) {

            if (drawSelected) {
                gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
            } else {
                gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW));
            }

            gc.drawLine(bounds.x, bounds.y, bounds.x + bounds.width - 1,
                bounds.y);
            gc.drawLine(bounds.x, bounds.y, bounds.x, bounds.y + bounds.height
                - 1);

            if (!drawSelected) {
                gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
                gc.drawLine(bounds.x + 1, bounds.y + 1,
                    bounds.x + bounds.width - 2, bounds.y + 1);
                gc.drawLine(bounds.x + 1, bounds.y + 1, bounds.x + 1,
                    bounds.y + bounds.height - 2);
            }

            if (drawSelected) {
                gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
            } else {
                gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));
            }
            gc.drawLine(bounds.x + bounds.width - 1, bounds.y, bounds.x
                + bounds.width - 1,
                bounds.y + bounds.height - 1);
            gc.drawLine(bounds.x, bounds.y + bounds.height - 1, bounds.x
                + bounds.width - 1,
                bounds.y + bounds.height - 1);

            if (!drawSelected) {
                gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
                gc.drawLine(bounds.x + bounds.width - 2, bounds.y + 1,
                    bounds.x + bounds.width - 2, bounds.y + bounds.height
                        - 2);
                gc.drawLine(bounds.x + 1, bounds.y + bounds.height - 2,
                    bounds.x + bounds.width - 2, bounds.y + bounds.height
                        - 2);
            }

        } else {
            gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));

            gc.drawLine(bounds.x + bounds.width - 1, bounds.y, bounds.x
                + bounds.width - 1,
                bounds.y + bounds.height - 1);
            gc.drawLine(bounds.x, bounds.y + bounds.height - 1, bounds.x
                + bounds.width - 1,
                bounds.y + bounds.height - 1);
        }

        gc.setFont(grid.normalFont);
    }

    public static void paintSort(GC gc, Rectangle bounds, int sort)
    {
        switch (sort) {
            case SWT.DEFAULT:
                gc.drawImage(IMAGE_ASTERISK, bounds.x, bounds.y);
                break;
            case SWT.UP:
                gc.drawImage(IMAGE_ARROW_UP, bounds.x, bounds.y);
                break;
            case SWT.DOWN:
                gc.drawImage(IMAGE_ARROW_DOWN, bounds.x, bounds.y);
                break;
        }
/*
        if (isSelected()) {
            gc.drawLine(bounds.x, bounds.y, bounds.x + 6, bounds.y);
            gc.drawLine(bounds.x + 1, bounds.y + 1, bounds.x + 5, bounds.y + 1);
            gc.drawLine(bounds.x + 2, bounds.y + 2, bounds.x + 4, bounds.y + 2);
            gc.drawPoint(bounds.x + 3, bounds.y + 3);
        } else {
            gc.drawPoint(bounds.x + 3, bounds.y);
            gc.drawLine(bounds.x + 2, bounds.y + 1, bounds.x + 4, bounds.y + 1);
            gc.drawLine(bounds.x + 1, bounds.y + 2, bounds.x + 5, bounds.y + 2);
            gc.drawLine(bounds.x, bounds.y + 3, bounds.x + 6, bounds.y + 3);
        }
*/
    }

}
