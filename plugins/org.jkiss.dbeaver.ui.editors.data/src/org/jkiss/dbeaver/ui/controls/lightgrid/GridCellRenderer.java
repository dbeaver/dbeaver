/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UITextUtils;
import org.jkiss.utils.CommonUtils;

/**
 * Grid cell renderer
 */
class GridCellRenderer extends AbstractRenderer {
    private static final int LEFT_MARGIN = 6;
    private static final int RIGHT_MARGIN = 6;
    private static final int TOP_MARGIN = 0;

    private static final int TEXT_TOP_MARGIN = 1;
    private static final int INSIDE_MARGIN = 3;

    static final Image LINK_IMAGE = DBeaverIcons.getImage(UIIcon.LINK);
    static final Image LINK2_IMAGE = DBeaverIcons.getImage(UIIcon.LINK2);
    static final Rectangle LINK_IMAGE_BOUNDS = new Rectangle(0, 0, 13, 13);

    // Clipping limits cell paint with cell bounds. But is an expensive GC call.
    // Generally we don't need it because we repaint whole grid left-to-right and all text tails will be overpainted by trailing cells
    private static final boolean USE_CLIPPING = false;

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

        Image image;
        Rectangle imageBounds = null;

        {
            DBPImage cellImage = grid.getCellImage(col, row);
            if (cellImage != null) {
                image = DBeaverIcons.getImage(cellImage);
                imageBounds = image.getBounds();
            } else {
                image = null;
            }

            if (image == null && isLinkState(state)) {
                image = ((state & IGridContentProvider.STATE_LINK) != 0) ? LINK_IMAGE : LINK2_IMAGE;
                imageBounds = LINK_IMAGE_BOUNDS;
            }
        }

        int columnAlign = grid.getContentProvider().getCellAlign(col, row);

        if (image != null && columnAlign != IGridContentProvider.ALIGN_RIGHT) {
            int y = bounds.y + (bounds.height - imageBounds.height) / 2;
            if (columnAlign == IGridContentProvider.ALIGN_CENTER) {
                x += (bounds.width - imageBounds.width - RIGHT_MARGIN - LEFT_MARGIN) / 2;
            }
            gc.drawImage(image, bounds.x + x, y);

            x += imageBounds.width + INSIDE_MARGIN;
        }

        int width = bounds.width - x - RIGHT_MARGIN;

        // Get cell text
        if (text != null && !text.isEmpty()) {
            // Get shortern version of string
            text = UITextUtils.getShortString(grid.fontMetrics, text, width);
            // Replace linefeeds with space
            text = CommonUtils.getSingleLineString(text);

            final Font font = grid.getContentProvider().getCellFont(col, row);
            gc.setFont(font != null ? font : grid.normalFont);

            switch (columnAlign) {
                // Center
                case IGridContentProvider.ALIGN_CENTER: {
                    Point textSize = gc.textExtent(text);
                    gc.drawString(
                        text,
                        bounds.x + (bounds.width - textSize.x) / 2,
                        bounds.y + TEXT_TOP_MARGIN + TOP_MARGIN,
                        isTransparent
                    );
                    break;
                }
                case IGridContentProvider.ALIGN_RIGHT: {
                    // Right (numbers, datetimes)
                    Point textSize = gc.textExtent(text);
                    int valueWidth = textSize.x + INSIDE_MARGIN;
                    if (imageBounds != null) {
                        valueWidth += imageBounds.width + INSIDE_MARGIN;
                    }
                    valueWidth += RIGHT_MARGIN;
                    boolean useClipping = valueWidth > bounds.width;

                    int imageMargin = 0;
                    if (image != null) {
                        // Reduce bounds by link image size
                        imageMargin = imageBounds.width + INSIDE_MARGIN;
                        if (useClipping) {
                            gc.setClipping(bounds.x, bounds.y, bounds.width - imageMargin, bounds.height);
                        }
                    } else {
                        if (useClipping) {
                            gc.setClipping(bounds);
                        }
                    }
                    gc.drawString(
                        text,
                        bounds.x + bounds.width - (textSize.x + RIGHT_MARGIN + imageMargin),
                        bounds.y + TEXT_TOP_MARGIN + TOP_MARGIN,
                        isTransparent
                    );
                    if (useClipping) {
                        gc.setClipping((Rectangle) null);
                    }
                    break;
                }
                default: {
                    gc.drawString(
                        text,
                        bounds.x + x,
                        bounds.y + TEXT_TOP_MARGIN + TOP_MARGIN,
                        isTransparent
                    );
                    break;
                }
            }
        }

        if (image != null && columnAlign == IGridContentProvider.ALIGN_RIGHT) {
            int y = bounds.y + (bounds.height - imageBounds.height) / 2;
            gc.drawImage(image, bounds.x + bounds.width - imageBounds.width - RIGHT_MARGIN, y);
        }

        if (focus) {

            gc.setForeground(colorLineFocused);
            gc.drawRectangle(bounds.x + 1, bounds.y, bounds.width - 2, bounds.height - 1);

            if (grid.isFocusControl()) {
                gc.drawRectangle(bounds.x + 2, bounds.y + 1, bounds.width - 4, bounds.height - 3);
            }
        }
    }

    boolean isOverLink(GridColumn column, int row, int x, int y) {
        IGridContentProvider contentProvider = grid.getContentProvider();
        Object colElement = column.getElement();
        Object rowElement = grid.getRowElement(row);
        int state = contentProvider.getCellState(colElement, rowElement, null);

        boolean isToggle = (state & IGridContentProvider.STATE_TOGGLE) != 0;
        if (isToggle) {
            if (contentProvider.isElementReadOnly(colElement)) {
                return false;
            }
        }
        if (isLinkState(state) || isToggle) {
            int columnAlign = contentProvider.getCellAlign(colElement, rowElement);
            Point origin = grid.getOrigin(column, row);
            Rectangle imageBounds;
            if (isToggle) {
                String cellText = grid.getCellText(colElement, rowElement);
                Point textSize = grid.sizingGC.textExtent(cellText);
                imageBounds = new Rectangle(0, 0, textSize.x, textSize.y);
            } else {
                DBPImage cellImage = grid.getCellImage(colElement, rowElement);
                Image image;
                if (cellImage == null) {
                    image = ((state & IGridContentProvider.STATE_LINK) != 0) ? LINK_IMAGE : LINK2_IMAGE;
                } else {
                    image = DBeaverIcons.getImage(cellImage);
                }
                imageBounds = image.getBounds();
            }
            int verMargin = (grid.getItemHeight() - imageBounds.height) / 2;

            switch (columnAlign) {
                case IGridContentProvider.ALIGN_LEFT:
                    if (x >= origin.x + LEFT_MARGIN && x <= origin.x + LEFT_MARGIN + imageBounds.width &&
                        y >= origin.y + verMargin && y <= origin.y + verMargin + imageBounds.height) {
                        return true;
                    }
                    break;
                case IGridContentProvider.ALIGN_RIGHT:
                    int width = column.getWidth();
                    if (x >= origin.x + width - (LEFT_MARGIN + imageBounds.width) && x <= origin.x + width - RIGHT_MARGIN &&
                        y >= origin.y + verMargin && y <= origin.y + verMargin + imageBounds.height) {
                        return true;
                    }
                    break;
                case IGridContentProvider.ALIGN_CENTER:
                    int leftIndent = (column.getWidth() - imageBounds.width - RIGHT_MARGIN - LEFT_MARGIN) / 2;
                    if (x >= origin.x + LEFT_MARGIN + leftIndent && x <= origin.x + LEFT_MARGIN + leftIndent + imageBounds.width &&
                        y >= origin.y + verMargin && y <= origin.y + verMargin + imageBounds.height) {
                        return true;
                    }
                    break;
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
