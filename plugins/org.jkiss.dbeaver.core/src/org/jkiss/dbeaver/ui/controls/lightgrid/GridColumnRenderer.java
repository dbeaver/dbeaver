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
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.TextUtils;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.utils.CommonUtils;

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
    public static final Image IMAGE_DESC = DBeaverIcons.getImage(UIIcon.SORT_DECREASE);
    public static final Image IMAGE_ASC = DBeaverIcons.getImage(UIIcon.SORT_INCREASE);
    public static final int SORT_WIDTH = 16;

    public  GridColumnRenderer(LightGrid grid) {
        super(grid);
    }

    public static Rectangle getSortControlBounds() {
        return IMAGE_DESC.getBounds();
    }

    @Nullable
    protected Image getColumnImage(Object element) {
        return grid.getLabelProvider().getImage(element);
    }

    protected String getColumnText(Object element)
    {
        return grid.getLabelProvider().getText(element);
    }

    protected String getColumnDescription(Object element)
    {
        return grid.getLabelProvider().getDescription(element);
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
            gc.setBackground(grid.getContentProvider().getCellHeaderSelectionBackground());
        } else {
            gc.setBackground(grid.getContentProvider().getCellHeaderBackground());
        }
        gc.setForeground(grid.getContentProvider().getCellHeaderForeground());

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

        {
            // Column name
            String text = getColumnText(element);
            text = TextUtils.getShortString(grid.fontMetrics, text, width);
            gc.setFont(grid.normalFont);
            gc.drawString(text, bounds.x + x + pushedDrawingOffset, y + pushedDrawingOffset, true);
        }

        if (sortOrder != SWT.NONE) {
            if (drawSelected) {
                sortBounds.x = bounds.x + bounds.width - ARROW_MARGIN - sortBounds.width + 1;
                sortBounds.y = y;
            } else {
                sortBounds.x = bounds.x + bounds.width - ARROW_MARGIN - sortBounds.width;
                sortBounds.y = y;
            }
            paintSort(gc, sortBounds, sortOrder);
        }

        {
            // Draw column description
            String text = getColumnDescription(element);
            if (!CommonUtils.isEmpty(text)) {
                y += TOP_MARGIN + grid.fontMetrics.getHeight();
                text = TextUtils.getShortString(grid.fontMetrics, text, width);
                gc.setFont(grid.normalFont);
                gc.drawString(text, bounds.x + x + pushedDrawingOffset, y + pushedDrawingOffset, true);
            }
        }

        // Draw border
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
                gc.drawImage(IMAGE_ASC, bounds.x, bounds.y);
                break;
            case SWT.DOWN:
                gc.drawImage(IMAGE_DESC, bounds.x, bounds.y);
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
