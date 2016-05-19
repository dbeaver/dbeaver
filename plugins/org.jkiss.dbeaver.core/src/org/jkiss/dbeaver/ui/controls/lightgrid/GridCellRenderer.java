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
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.TextUtils;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * Grid cell renderer
 */
class GridCellRenderer extends AbstractRenderer
{
    private static final int LEFT_MARGIN = 4;
    private static final int RIGHT_MARGIN = 4;
    private static final int TOP_MARGIN = 0;

    private static final int TEXT_TOP_MARGIN = 1;
    private static final int INSIDE_MARGIN = 3;

    static final Image LINK_IMAGE = DBeaverIcons.getImage(UIIcon.LINK);
    static final Rectangle LINK_IMAGE_BOUNDS = LINK_IMAGE.getBounds();

    protected Color colorSelected;
    protected Color colorSelectedText;
    protected Color colorLineForeground;
    protected Color colorLineFocused;

    private final RGB colorSelectedRGB;

    public GridCellRenderer(LightGrid grid)
    {
        super(grid);
        colorLineFocused = grid.getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND);
        colorSelectedText = grid.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT);
        colorSelected = grid.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION);
        colorSelectedRGB = colorSelected.getRGB();
        colorLineForeground = grid.getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW);
    }

    public void paint(GC gc, Rectangle bounds, boolean selected, boolean focus, Object col, Object row)
    {
        boolean drawBackground = true;

        if (selected) {
            Color cellBackground = grid.getCellBackground(col, row);
            if (cellBackground.equals(grid.getBackground())) {
                gc.setBackground(colorSelected);
            } else {
                RGB cellSel = UIUtils.blend(
                    cellBackground.getRGB(),
                    colorSelectedRGB,
                    50);

                gc.setBackground(DBeaverUI.getSharedTextColors().getColor(cellSel));
            }
            gc.setForeground(colorSelectedText);
        } else {
            if (grid.isEnabled()) {
                Color back = grid.getCellBackground(col, row);

                if (back != null) {
                    gc.setBackground(back);
                } else {
                    drawBackground = false;
                }
            } else {
                grid.setDefaultBackground(gc);
            }
            gc.setForeground(grid.getCellForeground(col, row));
        }

        if (drawBackground)
            gc.fillRectangle(bounds.x, bounds.y, bounds.width,
                bounds.height);


        int state = grid.getContentProvider().getCellState(col, row);
        int x = LEFT_MARGIN;

/*
        Image image = grid.getCellImage(cell);
        if (image != null) {
            Rectangle imageBounds = image.getBounds();
            int y = bounds.y + (bounds.height - imageBounds.height) / 2;

            gc.drawImage(image, bounds.x + x, y);

            x += imageBounds.width + INSIDE_MARGIN;
        }
*/
        Image image;
        Rectangle imageBounds = null;

        if ((state & IGridContentProvider.STATE_LINK) != 0) {
            image = LINK_IMAGE;
            imageBounds = LINK_IMAGE_BOUNDS;
        } else {
            DBPImage cellImage = grid.getCellImage(col, row);
            if (cellImage != null) {
                image = DBeaverIcons.getImage(cellImage);
                imageBounds = image.getBounds();
            } else {
                image = null;
            }
        }
        if (image != null) {
//            gc.drawImage(image, bounds.x + bounds.width - imageBounds.width - RIGHT_MARGIN, bounds.y + (bounds.height - imageBounds.height) / 2);
//            x += imageBounds.width + INSIDE_MARGIN;
            int y = bounds.y + (bounds.height - imageBounds.height) / 2;
            gc.drawImage(image, bounds.x + x, y);

            x += imageBounds.width + INSIDE_MARGIN;
        }

        int width = bounds.width - x - RIGHT_MARGIN;

//        if (drawAsSelected) {
//            gc.setForeground(colorSelectedText);
//        } else {
//            gc.setForeground(grid.getCellForeground(cell));
//        }

        // Get cell text
        String text = grid.getCellText(col, row);
        if (text != null && !text.isEmpty()) {
            // Get shortern version of string
            text = TextUtils.getShortString(grid.fontMetrics, text, width);
            // Replace linefeeds with space
            text = TextUtils.getSingleLineString(text);

            gc.setFont(grid.normalFont);
            gc.drawString(
                text,
                bounds.x + x,
                bounds.y + TEXT_TOP_MARGIN + TOP_MARGIN,
                true);
        }

        if (grid.isLinesVisible()) {
            if (selected) {
                //XXX: should be user definable?
                gc.setForeground(colorLineForeground);
            } else {
                gc.setForeground(grid.getLineColor());
            }
            gc.drawLine(bounds.x, bounds.y + bounds.height, bounds.x + bounds.width - 1,
                bounds.y + bounds.height);
            gc.drawLine(bounds.x + bounds.width - 1, bounds.y,
                bounds.x + bounds.width - 1, bounds.y + bounds.height);
        }

        if (focus) {

            gc.setForeground(colorLineFocused);
            gc.drawRectangle(bounds.x, bounds.y, bounds.width - 1, bounds.height);

            if (grid.isFocusControl()) {
                gc.drawRectangle(bounds.x + 1, bounds.y + 1, bounds.width - 3, bounds.height - 2);
            }
        }
    }

    public boolean isOverLink(GridColumn column, int row, int x, int y) {
        int state = grid.getContentProvider().getCellState(column.getElement(), grid.getRowElement(row));

        if ((state & IGridContentProvider.STATE_LINK) != 0) {
            Point origin = grid.getOrigin(column, row);
            int verMargin = (grid.getItemHeight() - LINK_IMAGE_BOUNDS.height) / 2;
            if (x >= origin.x + LEFT_MARGIN && x <= origin.x + LEFT_MARGIN + LINK_IMAGE_BOUNDS.width &&
                y >= origin.y + verMargin && y <= origin.y + verMargin + LINK_IMAGE_BOUNDS.height) {
                return true;
            }

        }
        return false;
    }

}
