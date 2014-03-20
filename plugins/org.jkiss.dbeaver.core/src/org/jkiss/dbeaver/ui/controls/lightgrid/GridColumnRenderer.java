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

package org.jkiss.dbeaver.ui.controls.lightgrid;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.DBIcon;

/**
 * Grid column renderer
 */
class GridColumnRenderer extends AbstractRenderer
{
    public static final int leftMargin = 6;
    public static final int rightMargin = 6;
    public static final int bottomMargin = 3;
    public static final int arrowMargin = 6;
    public static final int imageSpacing = 3;

    private static Image asterisk = DBIcon.SORT_UNKNOWN.getImage();
    private static Image arrowUp = DBIcon.SORT_DECREASE.getImage();
    private static Image arrowDown = DBIcon.SORT_INCREASE.getImage();

    public  GridColumnRenderer(LightGrid grid) {
        super(grid);
    }

    public static Rectangle getSortControlBounds() {
        return arrowUp.getBounds();
    }

    @Nullable
    protected Image getColumnImage() {
        return grid.getColumnLabelProvider().getImage(cell.col);
    }

    protected String getColumnText()
    {
        String text = grid.getColumnLabelProvider().getText(cell.col);
        if (text == null) {
            text = String.valueOf(cell.col);
        }
        return text;
    }
    
    protected Font getColumnFont() {
        Font font = grid.getColumnLabelProvider().getFont(cell.col);
        return font != null ? font : grid.getFont();
    }

    @Override
    public void paint(GC gc) {
        //GridColumn col = grid.getColumnByElement(cell.col);
        //AbstractRenderer arrowRenderer = col.getSortRenderer();
        int sortOrder = grid.getContentProvider().getColumnSortOrder(cell.col);
        Rectangle sortBounds = getSortControlBounds();

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

        final Rectangle bounds = getBounds();
        gc.fillRectangle(bounds.x, bounds.y, bounds.width, bounds.height);

        int pushedDrawingOffset = 0;
        if (drawSelected) {
            pushedDrawingOffset = 1;
        }

        int x = leftMargin;

        Image columnImage = getColumnImage();
        if (columnImage != null) {
            int y = bottomMargin;

            y = bounds.y + pushedDrawingOffset + bounds.height - bottomMargin - columnImage.getBounds().height;

            gc.drawImage(columnImage, bounds.x + x + pushedDrawingOffset, y);
            x += columnImage.getBounds().width + imageSpacing;
        }

        int width = bounds.width - x;

        if (sortOrder == SWT.NONE) {
            width -= rightMargin;
        } else {
            width -= arrowMargin + sortBounds.width + arrowMargin;
        }

        //gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));

        int y = bounds.y + bounds.height - bottomMargin - gc.getFontMetrics().getHeight();

        String text = getColumnText();

        text = org.jkiss.dbeaver.ui.TextUtils.getShortString(gc, text, width);

        gc.drawString(text, bounds.x + x + pushedDrawingOffset, y + pushedDrawingOffset, true);

        if (sortOrder != SWT.NONE) {
            y = bounds.y + ((bounds.height - sortBounds.height) / 2) + 1;

            if (drawSelected) {
                sortBounds.x = bounds.x + bounds.width - arrowMargin - sortBounds.width + 1;
                sortBounds.y = y;
            } else {
                y = bounds.y + ((bounds.height - sortBounds.height) / 2);
                sortBounds.x = bounds.x + bounds.width - arrowMargin - sortBounds.width;
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

        gc.setFont(grid.getFont());
    }

    private void paintSort(GC gc, Rectangle bounds, int sort)
    {
        switch (sort) {
            case SWT.DEFAULT:
                gc.drawImage(asterisk, bounds.x, bounds.y);
                break;
            case SWT.UP:
                gc.drawImage(arrowUp, bounds.x, bounds.y);
                break;
            case SWT.DOWN:
                gc.drawImage(arrowDown, bounds.x, bounds.y);
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
