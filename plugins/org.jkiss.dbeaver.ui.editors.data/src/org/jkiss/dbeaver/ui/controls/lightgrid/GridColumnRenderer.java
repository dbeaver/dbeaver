/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UITextUtils;
import org.jkiss.utils.CommonUtils;

/**
 * Grid column renderer
 */
class GridColumnRenderer extends AbstractRenderer {
    public static final int LEFT_MARGIN = 6;
    public static final int RIGHT_MARGIN = 6;
    public static final int BOTTOM_MARGIN = 6;
    public static final int TOP_MARGIN = 6;
    public static final int ARROW_MARGIN = 6;
    public static final int IMAGE_SPACING = 3;

    public static final Image IMAGE_ASTERISK = DBeaverIcons.getImage(UIIcon.SORT_UNKNOWN);
    public static final Image IMAGE_DESC = DBeaverIcons.getImage(UIIcon.SORT_INCREASE);
    public static final Image IMAGE_ASC = DBeaverIcons.getImage(UIIcon.SORT_DECREASE);
    public static final Image IMAGE_FILTER = DBeaverIcons.getImage(UIIcon.DROP_DOWN);

    public static final int SORT_WIDTH = IMAGE_DESC.getBounds().width;
    public static final int FILTER_WIDTH = IMAGE_FILTER.getBounds().width;

    // The border was disabled because it looked goofy
    private static final boolean PAINT_COLUMN_FOCUS_BORDER = false;

    public  GridColumnRenderer(LightGrid grid) {
        super(grid);
    }

    public static Rectangle getSortControlBounds() {
        return IMAGE_DESC.getBounds();
    }
    
    public static Rectangle getFilterControlBounds() {
    	return IMAGE_FILTER.getBounds();
    }

    @Nullable
    protected Image getColumnImage(IGridItem element) {
        return grid.getLabelProvider().getImage(element);
    }

    protected String getColumnText(IGridItem item)
    {
        return grid.getLabelProvider().getText(item);
    }

    protected String getColumnDescription(IGridColumn item)
    {
        return grid.getLabelProvider().getDescription(item);
    }

    protected Font getColumnFont(IGridColumn element) {
        Font font = grid.getLabelProvider().getFont(element);
        return font != null ? font : grid.normalFont;
    }

    public void paint(GC gc, Rectangle bounds, boolean selected, boolean hovering, IGridColumn element) {

        IGridContentProvider contentProvider = grid.getContentProvider();
        boolean hasFilters = contentProvider.isElementSupportsFilter(element);

        int sortOrder = contentProvider.getSortOrder(element);
        boolean showSortIconAlways = contentProvider.isElementSupportsSort(element);
        boolean showSortIcon = showSortIconAlways || sortOrder > 0;
        final Rectangle sortBounds = showSortIcon ? getSortControlBounds() : null;
        final Rectangle filterBounds = getFilterControlBounds();

        boolean flat = true;
        boolean drawSelected = false;

        gc.setBackground(grid.getLabelProvider().getHeaderBackground(element, selected || hovering));
        gc.setForeground(grid.getLabelProvider().getHeaderForeground(element, selected || hovering));

        gc.fillRectangle(bounds.x, bounds.y, bounds.width, bounds.height);

        int pushedDrawingOffset = 0;
        if (hovering) {
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

        if (sortOrder <= 0) {
            width -= RIGHT_MARGIN;
        } else {
            width -= ARROW_MARGIN + sortBounds.width;
        }
        if (hasFilters) {
            width -= filterBounds.width;
        }
        //gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));

        int y = bounds.y + TOP_MARGIN;

        {
            // Column name
            String text = getColumnText(element);
            text = UITextUtils.getShortString(grid.fontMetrics, text, width);
            // set the font to be used to display the text.
            gc.setFont(getColumnFont(element));
//            if (element == grid.getFocusColumnElement()) {
//                gc.drawLine(bounds.x + x + pushedDrawingOffset, bounds.y + bounds.height - pushedDrawingOffset, bounds.x + bounds.width - RIGHT_MARGIN, bounds.y + bounds.height - BOTTOM_MARGIN);
//            }

            gc.setClipping(bounds.x + x + pushedDrawingOffset, y + pushedDrawingOffset, width, grid.fontMetrics.getHeight());
            gc.drawString(text, bounds.x + x + pushedDrawingOffset, y + pushedDrawingOffset, isTransparent);
            gc.setClipping((Rectangle) null);
        }

        // Draw border
        if (PAINT_COLUMN_FOCUS_BORDER && element == grid.getFocusColumnElement()) {
            drawSelected = selected;

            if (drawSelected) {
                gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
            } else {
                gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW));
            }

            gc.drawLine(bounds.x, bounds.y, bounds.x + bounds.width - 1, bounds.y);
            gc.drawLine(bounds.x, bounds.y, bounds.x, bounds.y + bounds.height - 1);

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
            gc.drawLine(bounds.x + bounds.width - 1, bounds.y, bounds.x + bounds.width - 1, bounds.y + bounds.height - 1);
            gc.drawLine(bounds.x, bounds.y + bounds.height - 1, bounds.x + bounds.width - 1, bounds.y + bounds.height - 1);

            if (!drawSelected) {
                gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
                gc.drawLine(bounds.x + bounds.width - 2, bounds.y + 1, bounds.x + bounds.width - 2, bounds.y + bounds.height - 2);
                gc.drawLine(bounds.x + 1, bounds.y + bounds.height - 2, bounds.x + bounds.width - 2, bounds.y + bounds.height - 2);
            }

        } else {
            gc.setForeground(grid.getLabelProvider().getHeaderBorder(element));
            gc.drawLine(bounds.x + bounds.width - 1, bounds.y, bounds.x + bounds.width - 1, bounds.y + bounds.height - 1);
            gc.drawLine(bounds.x, bounds.y + bounds.height - 1, bounds.x + bounds.width - 1, bounds.y + bounds.height - 1);
        }

        // Sort icon
        if (showSortIcon) {
            sortBounds.x = bounds.x + bounds.width - sortBounds.width - filterBounds.width - IMAGE_SPACING;
            sortBounds.y = y;
            if (drawSelected) {
                sortBounds.x++;
            }
            paintSort(gc, sortBounds, sortOrder, showSortIconAlways);
        }

        // Drop-down icon
        if (hasFilters) {
            gc.drawImage(IMAGE_FILTER, bounds.x + bounds.width - filterBounds.width - IMAGE_SPACING, y);
            // (sortOrder != SWT.NONE ? IMAGE_SPACING + sortBounds.width + 1 : ARROW_MARGIN)
        }


        {
            // Draw column description
            String text = getColumnDescription(element);
            if (!CommonUtils.isEmpty(text)) {
                y += TOP_MARGIN + grid.fontMetrics.getHeight();
                text = UITextUtils.getShortString(grid.fontMetrics, text, width);
                gc.setFont(grid.normalFont);
                gc.drawString(text, bounds.x + x + pushedDrawingOffset, y + pushedDrawingOffset, isTransparent);
            }
        }

        gc.setFont(grid.normalFont);
    }

    public static void paintSort(GC gc, Rectangle bounds, int sort, boolean forcePaintDefault)
    {
        switch (sort) {
            case SWT.DEFAULT:
                if (forcePaintDefault) {
                    gc.drawImage(IMAGE_ASTERISK, bounds.x, bounds.y);
                }
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
