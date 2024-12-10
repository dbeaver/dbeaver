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
import org.eclipse.swt.graphics.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UITextUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Grid cell renderer
 */
public class GridCellRenderer extends AbstractRenderer {
    private static final Log log = Log.getLog(GridCellRenderer.class);

    private record IconInfo(Image hintImage, Rectangle iconSize) {
    }
    private static final int LEFT_MARGIN = 6;
    private static final int RIGHT_MARGIN = 6;
    private static final int TOP_MARGIN = 0;

    private static final int TEXT_TOP_MARGIN = 1;
    private static final int INSIDE_MARGIN = 3;

    static final Image LINK_IMAGE = DBeaverIcons.getImage(UIIcon.LINK);
    static final Image LINK2_IMAGE = DBeaverIcons.getImage(UIIcon.LINK2);
    static final Rectangle LINK_IMAGE_BOUNDS = new Rectangle(0, 0, 13, 13);

    // Mapping table for special characters. The replacement string is painted with a tinted color.
    private static final String[][] SPECIAL_CHARACTERS_MAP = {
        {" ", "·"},
        {"\r\n", "¶"},
        {"\r", "¶"},
        {"\n", "¶"},
        {"\t", "»"},
        {"\u3000", "°"}, // ideographic whitespace
        {"\u200b", "█"}, // zero-width whitespace
        {"\u0000", "NUL"},
        {"\u0001", "SOH"},
        {"\u0002", "STX"},
        {"\u0003", "ETX"},
        {"\u0004", "EOT"},
        {"\u0005", "ENQ"},
        {"\u0006", "ACK"},
        {"\u0007", "BEL"},
        {"\u0008", "BS"},
        {"\u000B", "VT"},
        {"\u000C", "FF"},
        {"\u000E", "SO"},
        {"\u000F", "SI"},
        {"\u0010", "DLE"},
        {"\u0011", "DC1"},
        {"\u0012", "DC2"},
        {"\u0013", "DC3"},
        {"\u0014", "DC4"},
        {"\u0015", "NAK"},
        {"\u0016", "SYN"},
        {"\u0017", "ETB"},
        {"\u0018", "CAN"},
        {"\u0019", "EM"},
        {"\u001A", "SUB"},
        {"\u001B", "ESC"},
        {"\u001C", "FS"},
        {"\u001D", "GS"},
        {"\u001E", "RS"},
        {"\u001F", "US"},
        {"\u007F", "DEL"}
    };
    private static final int LEVEL_INDENT = 5;

    protected Color colorLineFocused;

    public GridCellRenderer(LightGrid grid) {
        super(grid);
        colorLineFocused = grid.getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND);
    }

    public void paint(GC gc, Rectangle bounds, boolean selected, boolean focus, boolean hover, IGridColumn col, IGridRow row) {
        boolean drawBackground = true;

        IGridContentProvider.CellInformation cellInfo = grid.getContentProvider().getCellInfo(col, row, selected);
        if (cellInfo.background == null) {
            cellInfo.background = grid.getBackground();
        }
        if (cellInfo.foreground == null) {
            cellInfo.foreground = grid.getForeground();
        }

        if (cellInfo.background != null) {
            gc.setBackground(cellInfo.background);
        } else {
            drawBackground = false;
        }

        gc.setForeground(cellInfo.foreground);

        if (drawBackground) {
            gc.fillRectangle(bounds.x, bounds.y, bounds.width, bounds.height);
        }

        String text = grid.getCellText(cellInfo.text);
        final int state = cellInfo.state;

        Image image;
        Rectangle imageBounds = null;

        {
            DBPImage cellImage = cellInfo.image;
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

        int columnAlign = cellInfo.align;
        int x = image == null ? LEFT_MARGIN : LEFT_MARGIN / 2;

        if (columnAlign == IGridContentProvider.ALIGN_LEFT) {
            x += row.getLevel() * LEVEL_INDENT;
        }

        if (image != null && columnAlign != IGridContentProvider.ALIGN_RIGHT) {
            int y = bounds.y + (bounds.height - imageBounds.height) / 2;
            if (columnAlign == IGridContentProvider.ALIGN_CENTER) {
                x += (bounds.width - imageBounds.width - RIGHT_MARGIN - LEFT_MARGIN) / 2;
            }
            gc.drawImage(image, bounds.x + x, y);

            x += imageBounds.width + INSIDE_MARGIN;
        }

        int width = bounds.width - x - RIGHT_MARGIN;

        final String originalText = text;

        // Get cell text
        if (!text.isEmpty()) {
            // Get shortern version of string
            text = UITextUtils.getShortString(grid.fontMetrics, text, width);
            // Replace linefeeds with space
            text = CommonUtils.getSingleLineString(text);

            final Font font = cellInfo.font;
            gc.setFont(font != null ? font : grid.normalFont);

            int textTopPos = bounds.y + TEXT_TOP_MARGIN + TOP_MARGIN;
            switch (columnAlign) {
                // Center
                case IGridContentProvider.ALIGN_CENTER: {
                    Point textSize = gc.textExtent(text);
                    gc.drawString(
                        text,
                        bounds.x + (bounds.width - textSize.x) / 2,
                        textTopPos,
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
                    if (imageBounds != null) {
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
                    if (CommonUtils.isBitSet(state, IGridContentProvider.STATE_DECORATED)) {
                        drawCellTextDecorated(gc, originalText, cellInfo, new Rectangle(
                            bounds.x + x,
                            textTopPos,
                            bounds.width - LEFT_MARGIN - RIGHT_MARGIN,
                            bounds.height
                        ));
                    } else {
                        gc.drawString(
                            text,
                            bounds.x + x,
                            textTopPos,
                            isTransparent
                        );
                    }

                    renderHints(gc, bounds, col, row, cellInfo, text, cellInfo.background, x, textTopPos, focus, hover);

                    break;
                }
            }
        }

        if (image != null && imageBounds != null && columnAlign == IGridContentProvider.ALIGN_RIGHT) {
            int y = bounds.y + (bounds.height - imageBounds.height) / 2;
            gc.drawImage(image, bounds.x + bounds.width - imageBounds.width - RIGHT_MARGIN, y);
        }

        if (focus || hover) {

            gc.setForeground(colorLineFocused);
            gc.drawRectangle(bounds.x + 1, bounds.y, bounds.width - 2, bounds.height - 1);

            if (grid.isFocusControl()) {
                gc.drawRectangle(bounds.x + 2, bounds.y + 1, bounds.width - 4, bounds.height - 3);
            }
        }
    }

    // Render hints
    private void renderHints(
        GC gc,
        Rectangle bounds,
        IGridColumn col,
        IGridRow row,
        IGridContentProvider.CellInformation cellInfo,
        String text,
        Color background,
        int x,
        int textTopPos,
        boolean focus,
        boolean hover
    ) {
        List<IGridHint> cellHints = grid.getContentProvider().getCellHints(col, row, cellInfo.value, 0);
        if (CommonUtils.isEmpty(cellHints)) {
            return;
        }

        boolean textHintRendered = false;
        Point textSize = gc.textExtent(text);
        int hintLeftPos = bounds.x + x + textSize.x + LEFT_MARGIN;
        // Render text
        for (IGridHint hint : cellHints) {
            if (x > bounds.x + bounds.width) {
                // No more space
                break;
            }
            if (!textHintRendered) {
                String hintText = hint.getText();
                if (!CommonUtils.isEmpty(hintText)) {
                    textHintRendered = true;
                    if (textSize.x < bounds.width - LEFT_MARGIN) {
                        final Color disabledForeground = getDisabledForeground(cellInfo);

                        gc.setForeground(disabledForeground);
                        gc.drawString(
                            hintText,
                            hintLeftPos,
                            textTopPos,
                            isTransparent
                        );
                    }
                }
            }
        }
        if (hover || focus) {
            List<IconInfo> iconList = null;
            int iconsWidth = 0;
            for (IGridHint hint : cellHints) {
                DBPImage hintIcon = hint.getIcon();
                if (hintIcon != null) {
                    Image hintImage = DBeaverIcons.getImage(hintIcon);
                    Rectangle iconSize = hintImage.getBounds();
                    iconsWidth += iconSize.width + 1;
                    if (iconList == null) {
                        iconList = new ArrayList<>();
                    }
                    iconList.add(new IconInfo(hintImage, iconSize));
                }
            }

            if (iconList != null) {
                int iconsPaddingWidth = iconsWidth + 7;
                Color oldBg = gc.getBackground(), oldFg = gc.getForeground();
                gc.setBackground(focus ? grid.getBackground() : background);
                int leftDivPos = bounds.x + bounds.width - iconsPaddingWidth;
                gc.fillRectangle(
                    leftDivPos,
                    bounds.y,
                    iconsPaddingWidth,
                    bounds.height
                );
                gc.setForeground(colorLineFocused);
                gc.setLineWidth(2);
                if (!focus) {
                    gc.setLineStyle(SWT.LINE_DOT);
                }
                gc.drawLine(
                    leftDivPos,
                    bounds.y,
                    leftDivPos,
                    bounds.y + bounds.height
                );
                gc.setLineWidth(1);
                gc.setLineStyle(SWT.LINE_SOLID);
                gc.setBackground(oldBg);
                gc.setForeground(oldFg);

                int iconRightPos = bounds.x + bounds.width - 4;
                // Render icons
                for (IconInfo iconInfo : iconList) {
                    Rectangle iconSize = iconInfo.iconSize;
                    int iconY = bounds.y + (bounds.height - iconSize.height) / 2;
                    int iconX = iconRightPos - iconSize.width;
                    gc.drawImage(
                        iconInfo.hintImage,
                        iconX,
                        iconY
                    );
                    iconRightPos -= iconSize.width + 1;
                }
            }
        }
    }

    private boolean isOverHintAction(
        IGridRow row,
        GridColumn column,
        IGridContentProvider.CellInformation cellInfo,
        Point cellOrigin,
        int x,
        int y
    ) {
        List<IGridHint> cellHints = grid.getContentProvider().getCellHints(column, row, cellInfo.value, 0);
        if (CommonUtils.isEmpty(cellHints)) {
            return false;
        }
        int iconsWidth = 0;
        for (IGridHint hint : cellHints) {
            DBPImage hintIcon = hint.getIcon();
            if (hintIcon != null) {
                Image hintImage = DBeaverIcons.getImage(hintIcon);
                Rectangle iconSize = hintImage.getBounds();
                if (y >= cellOrigin.y &&
                    y <= cellOrigin.y + grid.getItemHeight() &&
                    x >= cellOrigin.x + column.getWidth() - 4 - iconsWidth - iconSize.width &&
                    x <= cellOrigin.x + column.getWidth() - 4) {
                    return true;
                }
                iconsWidth += iconSize.width + 1;
            }
        }
        return false;
    }

    public void executeHintAction(
        IGridRow row,
        IGridColumn column,
        IGridContentProvider.CellInformation cellInfo,
        int x,
        int y,
        int state) {
        List<IGridHint> cellHints = grid.getContentProvider().getCellHints(column, row, cellInfo.value, 0);
        if (CommonUtils.isEmpty(cellHints)) {
            return;
        }
        IGridController gridController = grid.getGridController();
        if (gridController == null) {
            log.error("No grid controller");
            return;
        }
        Point cellOrigin = grid.getOrigin(column, row.getVisualPosition());
        int iconsWidth = 0;
        for (IGridHint hint : cellHints) {
            DBPImage hintIcon = hint.getIcon();
            if (hintIcon != null) {
                Image hintImage = DBeaverIcons.getImage(hintIcon);
                Rectangle iconSize = hintImage.getBounds();
                if (x >= cellOrigin.x + column.getWidth() - 4 - iconsWidth - iconSize.width) {
                    hint.performAction(gridController, state);
                    return;
                }
            }
        }
        log.error("Cannot detect action hint");
    }


    private static @NotNull Color getDisabledForeground(IGridContentProvider.CellInformation cellInfo) {
        return UIUtils.getSharedColor(
            UIUtils.blend(cellInfo.foreground.getRGB(), cellInfo.background.getRGB(), 50));
    }

    boolean isOverLink(GridColumn column, int row, int x, int y) {
        IGridRow rowElement = grid.getRow(row);
        if (rowElement == null) {
            return false;
        }
        IGridContentProvider contentProvider = grid.getContentProvider();
        IGridContentProvider.CellInformation cellInfo = grid.getContentProvider().getCellInfo(
            column, rowElement, false);

        int state = cellInfo.state;

        boolean isToggle = (state & IGridContentProvider.STATE_TOGGLE) != 0;
        if (isToggle) {
            if (contentProvider.isElementReadOnly(column)) {
                return false;
            }
        }
        Point origin = grid.getOrigin(column, row);
        if (isLinkState(state) || isToggle) {
            int columnAlign = cellInfo.align;
            Rectangle imageBounds;
            if (isToggle) {
                String cellText = grid.getCellText(cellInfo.text);
                GC sizingGC = new GC(grid);
                Point textSize = sizingGC.textExtent(cellText);
                sizingGC.dispose();
                imageBounds = new Rectangle(0, 0, textSize.x, textSize.y);
            } else {
                DBPImage cellImage = cellInfo.image;
                Image image;
                if (cellImage == null) {
                    image = ((state & IGridContentProvider.STATE_LINK) != 0) ? LINK_IMAGE : LINK2_IMAGE;
                } else {
                    image = DBeaverIcons.getImage(cellImage);
                }
                imageBounds = image.getBounds();
            }
            x -= rowElement.getLevel() * LEVEL_INDENT;
            int verMargin = (grid.getItemHeight() - imageBounds.height) / 2;

            int leftMargin = LEFT_MARGIN / 2;

            switch (columnAlign) {
                case IGridContentProvider.ALIGN_LEFT:
                    if (x >= origin.x + leftMargin && x <= origin.x + leftMargin + imageBounds.width &&
                        y >= origin.y + verMargin && y <= origin.y + verMargin + imageBounds.height) {
                        return true;
                    }
                    break;
                case IGridContentProvider.ALIGN_RIGHT:
                    int width = column.getWidth();
                    x += RIGHT_MARGIN;
                    if (x >= origin.x + width - (leftMargin + imageBounds.width) && x <= origin.x + width &&
                        y >= origin.y + verMargin && y <= origin.y + verMargin + imageBounds.height) {
                        return true;
                    }
                    break;
                case IGridContentProvider.ALIGN_CENTER:
                    int leftIndent = (column.getWidth() - imageBounds.width - RIGHT_MARGIN - leftMargin) / 2;
                    if (x >= origin.x + leftMargin + leftIndent && x <= origin.x + leftMargin + leftIndent + imageBounds.width &&
                        y >= origin.y + verMargin && y <= origin.y + verMargin + imageBounds.height) {
                        return true;
                    }
                    break;
            }
        }

        // Check hints
        return isOverHintAction(rowElement, column, cellInfo, origin, x, y);
    }

    public static boolean isLinkState(int state) {
        return
            (state & IGridContentProvider.STATE_LINK) != 0 ||
            (state & IGridContentProvider.STATE_HYPER_LINK) != 0;
    }

    private void drawCellTextDecorated(@NotNull GC gc, @NotNull String text, @NotNull IGridContentProvider.CellInformation cellInfo, @NotNull Rectangle bounds) {
        final Color activeForeground = cellInfo.foreground;
        final Color disabledForeground = getDisabledForeground(cellInfo);

        int start = 0;
        int index = 0;

        while (index < text.length()) {
            boolean matched = false;

            for (String[] mapping : SPECIAL_CHARACTERS_MAP) {
                final String expected = mapping[0];
                final String replacement = mapping[1];
                final boolean regionMatches = text.regionMatches(index, expected, 0, expected.length());

                if (regionMatches) {
                    if (drawCellTextSegment(gc, text.substring(start, index), bounds, activeForeground, disabledForeground, false)) {
                        return;
                    }

                    if (drawCellTextSegment(gc, replacement, bounds, disabledForeground, disabledForeground, replacement.length() > 1)) {
                        return;
                    }

                    index += expected.length();
                    start = index;
                    matched = true;
                }
            }

            if (!matched) {
                index += 1;
            }
        }

        if (start < text.length()) {
            drawCellTextSegment(gc, text.substring(start), bounds, activeForeground, disabledForeground, false);
        }
    }

    private boolean drawCellTextSegment(
        @NotNull GC gc,
        @NotNull String segment,
        @NotNull Rectangle bounds,
        @NotNull Color activeForeground,
        @NotNull Color disabledForeground,
        boolean highlight
    ) {
        if (segment.isEmpty()) {
            return false;
        }

        final Point extent = gc.textExtent(segment);

        if (extent.x > bounds.width) {
            // Since we are already performing quite expensive paint operations,
            // let's make it a bit worse by precisely calculating the length
            // of the cropped segment using binary search

            int low = 0;
            int high = segment.length();
            String clipped = segment;

            while (low <= high) {
                final int mid = (low + high) >>> 1;
                clipped = segment.substring(0, mid);
                final int val = gc.textExtent(clipped + "...").x;

                if (val < bounds.width) {
                    low = mid + 1;
                } else if (val > bounds.width) {
                    high = mid - 1;
                } else {
                    break;
                }
            }

            drawTextAndAdvance(gc, clipped, activeForeground, bounds, false);
            drawTextAndAdvance(gc, "...", disabledForeground, bounds, false);

            return true;
        } else {
            drawTextAndAdvance(gc, segment, activeForeground, bounds, highlight);

            return false;
        }
    }

    private void drawTextAndAdvance(
        @NotNull GC gc,
        @NotNull String text,
        @NotNull Color foreground,
        @NotNull Rectangle bounds,
        boolean highlight
    ) {
        if (text.isEmpty()) {
            return;
        }

        gc.setTextAntialias(SWT.ON);
        gc.setForeground(foreground);

        final Point extent = gc.stringExtent(text);

        if (highlight) {
            extent.x += 2;

            gc.drawRoundRectangle(bounds.x, bounds.y, extent.x, extent.y - 1, 2, 2);

            bounds.x += 2;
            bounds.width -= 2;
        }

        gc.drawString(text, bounds.x, bounds.y, true);

        bounds.x += extent.x;
        bounds.width -= extent.x;
    }
}
