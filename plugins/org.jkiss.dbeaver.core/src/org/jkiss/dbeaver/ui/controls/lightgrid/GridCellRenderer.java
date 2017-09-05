/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    static final Image LINK2_IMAGE = DBeaverIcons.getImage(UIIcon.LINK2);
    static final Rectangle LINK_IMAGE_BOUNDS = new Rectangle(0, 0, 13, 13);

    protected Color colorLineFocused;

    public GridCellRenderer(LightGrid grid)
    {
        super(grid);
        colorLineFocused = grid.getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND);
    }

    public void paint(GC gc, Rectangle bounds, boolean selected, boolean focus, Object col, Object row)
    {
        boolean drawBackground = true;

        //if (grid.isEnabled()) {
            Color back = grid.getCellBackground(col, row, selected);

            if (back != null) {
                gc.setBackground(back);
            } else {
                drawBackground = false;
            }
        /*} else {
            grid.setDefaultBackground(gc);
        }*/
        gc.setForeground(grid.getCellForeground(col, row, selected));

        if (drawBackground) {
            gc.fillRectangle(bounds.x, bounds.y, bounds.width, bounds.height);
        }

        String text = grid.getCellText(col, row);
        final int state = grid.getContentProvider().getCellState(col, row, text);
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

        if (isLinkState(state)) {
            image = ((state & IGridContentProvider.STATE_LINK) != 0) ? LINK_IMAGE : LINK2_IMAGE;
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
        if (text != null && !text.isEmpty()) {
            // Get shortern version of string
            text = TextUtils.getShortString(grid.fontMetrics, text, width);
            // Replace linefeeds with space
            text = TextUtils.getSingleLineString(text);

            gc.setFont(grid.normalFont);

            int columnAlign = grid.getContentProvider().getColumnAlign(col);

            switch (columnAlign) {
                // Center
                case IGridContentProvider.ALIGN_CENTER:
                    break;
                case IGridContentProvider.ALIGN_RIGHT:
                    // Right (numbers, datetimes)
                    Point textSize = gc.textExtent(text);
                    gc.drawString(
                            text,
                            bounds.x + bounds.width - (textSize.x + RIGHT_MARGIN),
                            bounds.y + TEXT_TOP_MARGIN + TOP_MARGIN,
                            true);
                    break;
                default:
                    gc.drawString(
                            text,
                            bounds.x + x,
                            bounds.y + TEXT_TOP_MARGIN + TOP_MARGIN,
                            true);
                    break;
            }
        }

        if (grid.isLinesVisible()) {
            if (selected) {
                gc.setForeground(grid.getLineSelectedColor());
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
        int state = grid.getContentProvider().getCellState(column.getElement(), grid.getRowElement(row), null);

        if (isLinkState(state)) {
            Point origin = grid.getOrigin(column, row);
            int verMargin = (grid.getItemHeight() - LINK_IMAGE_BOUNDS.height) / 2;
            if (x >= origin.x + LEFT_MARGIN && x <= origin.x + LEFT_MARGIN + LINK_IMAGE_BOUNDS.width &&
                y >= origin.y + verMargin && y <= origin.y + verMargin + LINK_IMAGE_BOUNDS.height) {
                return true;
            }

        }
        return false;
    }

    public static boolean isLinkState(int state) {
        return
            (state & IGridContentProvider.STATE_LINK) != 0 ||
            (state & IGridContentProvider.STATE_HYPER_LINK) != 0;
    }
}
