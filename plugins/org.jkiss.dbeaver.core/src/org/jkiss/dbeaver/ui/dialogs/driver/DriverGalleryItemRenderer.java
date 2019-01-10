/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.dialogs.driver;

import org.eclipse.nebula.widgets.gallery.AbstractGalleryItemRenderer;
import org.eclipse.nebula.widgets.gallery.Gallery;
import org.eclipse.nebula.widgets.gallery.GalleryItem;
import org.eclipse.nebula.widgets.gallery.RendererHelper;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

/**
 * DriverGalleryItemRenderer
 */
public class DriverGalleryItemRenderer extends AbstractGalleryItemRenderer {

    public static final int IMAGE_DRAW_WIDTH = 50;
    public static final int ITEM_MARGIN = 5;
    private Color selectionForegroundColor;
    private Color selectionBackgroundColor;
    private Color foregroundColor, backgroundColor;

    private boolean showRoundedSelectionCorners = true;

    private int selectionRadius = 10;

    // Vars used during drawing (optimization)
    private boolean drawBackground = false;
    private Color drawBackgroundColor = null;
    private Color drawForegroundColor = null;

    private Font normalFont, boldFont;

    public DriverGalleryItemRenderer(Composite panel) {
        // Set defaults
        foregroundColor = Display.getDefault().getSystemColor(SWT.COLOR_LIST_FOREGROUND);
        backgroundColor = Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND);

        selectionForegroundColor = Display.getDefault().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT);
        selectionBackgroundColor = Display.getDefault().getSystemColor(SWT.COLOR_LIST_SELECTION);

        normalFont = panel.getFont();
        boldFont = UIUtils.makeBoldFont(normalFont);
    }

    public void draw(GC gc, GalleryItem item, int index, int x, int y, int width, int height) {
        final Image itemImage = item.getImage();
        drawForegroundColor = getForeground(item);

        // Set up the GC
        gc.setFont(getFont(item));

        // Create some room for the label.
        int useableHeight = height;
        String itemText = item.getText();
        String itemDescription = item.getText(1);
        String itemCategory = item.getText(2);

        // Draw background (rounded rectangles)

        // Checks if background has to be drawn
        drawBackground = selected;
        drawBackgroundColor = null;
        if (!drawBackground && item.getBackground(true) != null) {
            drawBackgroundColor = getBackground(item);

            if (!RendererHelper.isColorsEquals(drawBackgroundColor,
                gallery.getBackground())) {
                drawBackground = true;
            }
        }

        if (drawBackground) {
            // Set colors
            if (selected) {
                gc.setBackground(selectionBackgroundColor);
                gc.setForeground(selectionBackgroundColor);
            } else if (drawBackgroundColor != null) {
                gc.setBackground(drawBackgroundColor);
            }

            // Draw
            if (showRoundedSelectionCorners) {
                gc.fillRoundRectangle(x, y, width, useableHeight, selectionRadius, selectionRadius);
            } else {
                gc.fillRectangle(x, y, width, height);
            }
        }
        if (false) {
            // Draw rectangle
            gc.setForeground(drawForegroundColor);
            gc.drawRoundRectangle(x + 1, y + 1, width - 2, useableHeight - 2, selectionRadius, selectionRadius);
        }

        // Draw image
        if (itemImage != null) {
            Rectangle itemImageBounds = itemImage.getBounds();
            int imageWidth = itemImageBounds.width;
            int imageHeight = itemImageBounds.height;

            int imageDrawWidth = IMAGE_DRAW_WIDTH;
            int imageDrawHeight = IMAGE_DRAW_WIDTH;

            gc.drawImage(itemImage, 0, 0, imageWidth, imageHeight, x + ITEM_MARGIN, y + ITEM_MARGIN, imageDrawWidth, imageDrawHeight);
        }

        // Draw label
        if (itemText != null && !EMPTY_STRING.equals(itemText)) {
            // Set colors
            if (selected) {
                // Selected : use selection colors.
                gc.setForeground(selectionForegroundColor);
                gc.setBackground(selectionBackgroundColor);
            } else {
                // Not selected, use item values or defaults.

                // Background
                if (drawBackgroundColor != null) {
                    gc.setBackground(drawBackgroundColor);
                } else {
                    gc.setBackground(backgroundColor);
                }

                // Foreground
                if (drawForegroundColor != null) {
                    gc.setForeground(drawForegroundColor);
                } else {
                    gc.setForeground(foregroundColor);
                }
            }

            // Create label
            String text = RendererHelper.createLabel(itemText, gc, width - IMAGE_DRAW_WIDTH - ITEM_MARGIN);

            // Center text
            //int textWidth = gc.textExtent(text).x;
            //int textxShift = RendererHelper.getShift(width, textWidth);

            // Draw
            int textY = y + ITEM_MARGIN;
            int textX = x + IMAGE_DRAW_WIDTH + ITEM_MARGIN * 2;

            gc.setFont(boldFont);
            gc.drawText(text, textX, textY, true);

            gc.setFont(normalFont);
            textY += gc.getFontMetrics().getHeight() + ITEM_MARGIN;
            if (!CommonUtils.isEmpty(itemCategory)) {
                gc.drawText(itemCategory, textX, textY, true);
            }

            textY += gc.getFontMetrics().getHeight();
            if (!CommonUtils.isEmpty(itemDescription)) {
                gc.drawText(itemDescription, textX, textY, true);
            }
        }
    }

    /**
     * Returns the font used for drawing all item labels or <tt>null</tt> if
     * system font is used.
     *
     * @return the font
     * @see {@link Gallery#getFont()} for setting font for a specific
     * GalleryItem.
     */
    public Font getFont() {
        if (gallery != null) {
            return gallery.getFont();
        }
        return null;
    }

    /**
     * Set the font for drawing all item labels or <tt>null</tt> to use system
     * font.
     *
     * @param font the font to set
     * @see {@link Gallery#setFont(Font)} for setting font for a specific
     * GalleryItem.
     */
    public void setFont(Font font) {
        if (gallery != null) {
            gallery.setFont(font);
        }
    }

    public void dispose() {
        if (boldFont != null) {
            UIUtils.dispose(boldFont);
            boldFont = null;
        }
    }

}
