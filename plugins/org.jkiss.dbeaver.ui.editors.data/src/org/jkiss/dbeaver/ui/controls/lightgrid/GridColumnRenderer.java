/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.DBPImage;
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
    public static final int TOP_MARGIN = 4;
    public static final int ARROW_MARGIN = 6;
    public static final int IMAGE_SPACING = 3;

    public static final Image IMAGE_ASTERISK = DBeaverIcons.getImage(UIIcon.SORT_UNKNOWN);
    public static final Image IMAGE_DESC = DBeaverIcons.getImage(UIIcon.SORT_INCREASE);
    public static final Image IMAGE_ASC = DBeaverIcons.getImage(UIIcon.SORT_DECREASE);
    public static final Image IMAGE_FILTER = DBeaverIcons.getImage(UIIcon.DROP_DOWN);

    public static final int SORT_WIDTH = IMAGE_DESC.getBounds().width;
    public static final int SORT_HEIGHT = IMAGE_DESC.getBounds().height;
    public static final int FILTER_WIDTH = IMAGE_FILTER.getBounds().width;

    // The border was disabled because it looked goofy
    private static final boolean PAINT_COLUMN_FOCUS_BORDER = true;

    // Shifts everything to the right by 1 pixel if the column is selected or hovered. Doesn't work well the hover detection
    private static final boolean SHIFT_PAINT_ON_SELECTION = false;

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

    public void paint(GC gc, Rectangle bounds, boolean selected, boolean hovering, GridColumn element) {
        GridColumn.HintsInfo hintInfo = element.getHintInfo();

        gc.setBackground(grid.getLabelProvider().getHeaderBackground(element, selected || hovering));
        gc.fillRectangle(bounds.x, bounds.y, bounds.width, bounds.height);

        // Draw border
        boolean isFocused = element == grid.getFocusColumn();
        if (PAINT_COLUMN_FOCUS_BORDER && isFocused) {
            gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));

            gc.drawLine(bounds.x, bounds.y, bounds.x + bounds.width - 1, bounds.y);
            gc.drawLine(bounds.x, bounds.y, bounds.x, bounds.y + bounds.height - 1);

            gc.drawLine(bounds.x + bounds.width - 1, bounds.y, bounds.x + bounds.width - 1, bounds.y + bounds.height - 1);
            gc.drawLine(bounds.x, bounds.y + bounds.height - 1, bounds.x + bounds.width - 1, bounds.y + bounds.height - 1);
        } else {
            gc.setForeground(grid.getLabelProvider().getHeaderBorder(element));
            gc.drawLine(bounds.x + bounds.width - 1, bounds.y, bounds.x + bounds.width - 1, bounds.y + bounds.height - 1);
            gc.drawLine(bounds.x, bounds.y + bounds.height - 1, bounds.x + bounds.width - 1, bounds.y + bounds.height - 1);
        }
        if (hintInfo.readOnly) {
            gc.setForeground(grid.getLabelProvider().getHeaderReadOnlyColor());
            gc.setLineWidth(1);
            gc.drawLine(isFocused ? bounds.x + 1 : bounds.x, bounds.y + bounds.height - 2, bounds.x + bounds.width - 2, bounds.y + bounds.height - 2);
            gc.setLineWidth(1);
        }

        bounds.x += LEFT_MARGIN;
        bounds.y += TOP_MARGIN;
        bounds.width -= LEFT_MARGIN + RIGHT_MARGIN;
        bounds.height -= TOP_MARGIN + BOTTOM_MARGIN;

        int fontHeight = grid.fontMetrics.getHeight();

        if (SHIFT_PAINT_ON_SELECTION && (hovering || selected)) {
            bounds.x += 1;
            bounds.y += 1;
        }

        final Image columnImage = getColumnImage(element);
        if (columnImage != null) {
            Rectangle imageBounds = columnImage.getBounds();

            gc.drawImage(columnImage, bounds.x, bounds.y);

            final int width = imageBounds.width + IMAGE_SPACING;
            bounds.x += width;
            bounds.width -= width;
        }

        if (!CommonUtils.isEmpty(hintInfo.icons)) {
            int hy = bounds.y;
            if (hintInfo.icons.size() > 1) {
                hy -= TOP_MARGIN;
            }
            int maxWidth = 0;
            for (DBPImage hi : hintInfo.icons) {
                Image hintImage = DBeaverIcons.getImage(hi);
                Rectangle imageBounds = hintImage.getBounds();

                if (hintInfo.icons.size() == 1) {
                    hy = (bounds.height - imageBounds.height) / 2;
                }
                gc.drawImage(hintImage, bounds.x, hy);

                maxWidth = Math.max(maxWidth, imageBounds.width);
                hy += imageBounds.height + 1;
            }
            if (maxWidth > 0) maxWidth += IMAGE_SPACING;
            bounds.x += maxWidth;
            bounds.width -= maxWidth;
        }

        final IGridContentProvider contentProvider = grid.getContentProvider();

        { // Drop-down icon
            final boolean hasFilters = contentProvider.isElementSupportsFilter(element);

            if (hasFilters) {
                bounds.width -= getFilterControlBounds().width;
                gc.drawImage(IMAGE_FILTER, bounds.x + bounds.width, bounds.y);
                bounds.width -= IMAGE_SPACING;
            }
        }

        { // Sort icon
            final int sortOrder = contentProvider.getSortOrder(element);
            final boolean showSortIconAlways = contentProvider.isElementSupportsSort(element);

            if (showSortIconAlways || sortOrder > 0) {
                bounds.width -= getSortControlBounds().width;
                paintSort(gc, new Rectangle(bounds.x + bounds.width, bounds.y, 0, 0), sortOrder, showSortIconAlways);
                bounds.width -= IMAGE_SPACING;
            }
        }

        gc.setForeground(grid.getLabelProvider().getHeaderForeground(element, selected || hovering));

        { // Draw column name
            final String text = UITextUtils.getShortString(grid.fontMetrics, getColumnText(element), bounds.width);
            gc.setFont(getColumnFont(element));
            gc.setClipping(bounds.x, bounds.y, bounds.width, fontHeight);
            gc.drawString(text, bounds.x, bounds.y, true);
            gc.setClipping((Rectangle) null);
        }

        { // Draw column description
            String text = getColumnDescription(element);
            if (!CommonUtils.isEmpty(text)) {
                text = UITextUtils.getShortString(grid.fontMetrics, text, bounds.width);
                bounds.y += TOP_MARGIN + fontHeight;
                gc.setForeground(grid.getLabelProvider().getHeaderForeground(element, selected || hovering));
                gc.setFont(grid.commentFont);
                gc.setClipping(bounds.x, bounds.y, bounds.width, fontHeight);
                gc.drawString(text, bounds.x, bounds.y, true);
                gc.setClipping((Rectangle) null);
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
