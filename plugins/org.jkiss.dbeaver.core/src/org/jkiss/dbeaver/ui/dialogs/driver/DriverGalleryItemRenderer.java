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
package org.jkiss.dbeaver.ui.dialogs.driver;

import org.eclipse.nebula.widgets.gallery.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Display;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * DriverGalleryItemRenderer
 */
public class DriverGalleryItemRenderer extends AbstractGalleryItemRenderer {

    /**
     * Stores colors used in drop shadows
     */
    protected ArrayList dropShadowsColors = new ArrayList();

    // Renderer parameters
    private boolean dropShadows = false;

    private int dropShadowsSize = 0;

    private int dropShadowsAlphaStep = 20;

    private Color selectionForegroundColor;
    private Color selectionBackgroundColor;
    private Color foregroundColor, backgroundColor;

    private boolean showLabels = true;
    private boolean showRoundedSelectionCorners = true;

    private int selectionRadius = 5;

    // Vars used during drawing (optimization)
    private boolean _drawBackground = false;
    private Color _drawBackgroundColor = null;
    private Image _drawImage = null;
    private Color _drawForegroundColor = null;

    public DriverGalleryItemRenderer() {
        // Set defaults
        foregroundColor = Display.getDefault().getSystemColor(
            SWT.COLOR_LIST_FOREGROUND);
        backgroundColor = Display.getDefault().getSystemColor(
            SWT.COLOR_LIST_BACKGROUND);

        selectionForegroundColor = Display.getDefault().getSystemColor(
            SWT.COLOR_LIST_SELECTION_TEXT);
        selectionBackgroundColor = Display.getDefault().getSystemColor(
            SWT.COLOR_LIST_SELECTION);

        // Create drop shadows
        createColors();
    }

    /**
     * Returns current label state : enabled or disabled
     *
     * @return true if labels are enabled.
     * @see DefaultGalleryItemRenderer#setShowLabels(boolean)
     */
    public boolean isShowLabels() {
        return showLabels;
    }

    /**
     * Enables / disables labels at the bottom of each item.
     *
     * @param showLabels
     * @see DefaultGalleryItemRenderer#isShowLabels()
     */
    public void setShowLabels(boolean showLabels) {
        this.showLabels = showLabels;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.nebula.widgets.gallery.AbstractGalleryItemRenderer#draw(org
     * .eclipse.swt.graphics.GC, org.eclipse.nebula.widgets.gallery.GalleryItem,
     * int, int, int, int, int)
     */
    public void draw(GC gc, GalleryItem item, int index, int x, int y,
                     int width, int height) {
        _drawImage = item.getImage();
        _drawForegroundColor = getForeground(item);

        // Set up the GC
        gc.setFont(getFont(item));

        // Create some room for the label.
        int useableHeight = height;
        int fontHeight = 0;
        if (item.getText() != null && !EMPTY_STRING.equals(item.getText())
            && this.showLabels) {
            fontHeight = gc.getFontMetrics().getHeight();
            useableHeight -= fontHeight + 2;
        }

        int imageWidth = 0;
        int imageHeight = 0;
        int xShift = 0;
        int yShift = 0;
        Point size = null;

        if (_drawImage != null) {
            Rectangle itemImageBounds = _drawImage.getBounds();
            imageWidth = itemImageBounds.width;
            imageHeight = itemImageBounds.height;

            size = RendererHelper.getBestSize(imageWidth, imageHeight, width
                - 8 - 2 * this.dropShadowsSize, useableHeight - 8 - 2
                * this.dropShadowsSize);

            xShift = RendererHelper.getShift(width, size.x);
            yShift = RendererHelper.getShift(useableHeight, size.y);

            if (dropShadows) {
                Color c = null;
                for (int i = this.dropShadowsSize - 1; i >= 0; i--) {
                    c = (Color) dropShadowsColors.get(i);
                    gc.setForeground(c);

                    gc.drawLine(x + width + i - xShift - 1, y + dropShadowsSize
                        + yShift, x + width + i - xShift - 1, y
                        + useableHeight + i - yShift);
                    gc.drawLine(x + xShift + dropShadowsSize, y + useableHeight
                        + i - yShift - 1, x + width + i - xShift, y - 1
                        + useableHeight + i - yShift);
                }
            }
        }

        // Draw background (rounded rectangles)

        // Checks if background has to be drawn
        _drawBackground = selected;
        _drawBackgroundColor = null;
        if (!_drawBackground && item.getBackground(true) != null) {
            _drawBackgroundColor = getBackground(item);

            if (!RendererHelper.isColorsEquals(_drawBackgroundColor,
                gallery.getBackground())) {
                _drawBackground = true;
            }
        }

        if (_drawBackground) {
            // Set colors
            if (selected) {
                gc.setBackground(selectionBackgroundColor);
                gc.setForeground(selectionBackgroundColor);
            } else if (_drawBackgroundColor != null) {
                gc.setBackground(_drawBackgroundColor);
            }

            // Draw
            if (showRoundedSelectionCorners) {
                gc.fillRoundRectangle(x, y, width, useableHeight,
                    selectionRadius, selectionRadius);
            } else {
                gc.fillRectangle(x, y, width, height);
            }

            if (item.getText() != null && !EMPTY_STRING.equals(item.getText())
                && showLabels) {
                gc.fillRoundRectangle(x, y + height - fontHeight, width,
                    fontHeight, selectionRadius, selectionRadius);
            }
        }

        // Draw image
        if (_drawImage != null && size != null) {
            if (size.x > 0 && size.y > 0) {
                gc.drawImage(_drawImage, 0, 0, imageWidth, imageHeight, x
                    + xShift, y + yShift, size.x, size.y);
                drawAllOverlays(gc, item, x, y, size, xShift, yShift);
            }

        }

        // Draw label
        if (item.getText() != null && !EMPTY_STRING.equals(item.getText())
            && showLabels) {
            // Set colors
            if (selected) {
                // Selected : use selection colors.
                gc.setForeground(selectionForegroundColor);
                gc.setBackground(selectionBackgroundColor);
            } else {
                // Not selected, use item values or defaults.

                // Background
                if (_drawBackgroundColor != null) {
                    gc.setBackground(_drawBackgroundColor);
                } else {
                    gc.setBackground(backgroundColor);
                }

                // Foreground
                if (_drawForegroundColor != null) {
                    gc.setForeground(_drawForegroundColor);
                } else {
                    gc.setForeground(foregroundColor);
                }
            }

            // Create label
            String text = RendererHelper.createLabel(item.getText(), gc,
                width - 10);

            // Center text
            int textWidth = gc.textExtent(text).x;
            int textxShift = RendererHelper.getShift(width, textWidth);

            // Draw
            gc.drawText(text, x + textxShift, y + height - fontHeight, true);
        }
    }

    public void setDropShadowsSize(int dropShadowsSize) {
        this.dropShadowsSize = dropShadowsSize;
        this.dropShadowsAlphaStep = (dropShadowsSize == 0) ? 0
            : (200 / dropShadowsSize);

        freeDropShadowsColors();
        createColors();
        // TODO: force redraw

    }

    private void createColors() {
        if (dropShadowsSize > 0) {
            int step = 125 / dropShadowsSize;
            // Create new colors
            for (int i = dropShadowsSize - 1; i >= 0; i--) {
                int value = 255 - i * step;
                Color c = new Color(Display.getDefault(), value, value, value);
                dropShadowsColors.add(c);
            }
        }
    }

    private void freeDropShadowsColors() {
        // Free colors :
        {
            Iterator i = this.dropShadowsColors.iterator();
            while (i.hasNext()) {
                Color c = (Color) i.next();
                if (c != null && !c.isDisposed())
                    c.dispose();
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
        freeDropShadowsColors();
    }

    public Color getForegroundColor() {
        return foregroundColor;
    }

    public void setForegroundColor(Color foregroundColor) {
        this.foregroundColor = foregroundColor;
    }

    public Color getSelectionForegroundColor() {
        return selectionForegroundColor;
    }

    public void setSelectionForegroundColor(Color selectionForegroundColor) {
        this.selectionForegroundColor = selectionForegroundColor;
    }

    public Color getSelectionBackgroundColor() {
        return selectionBackgroundColor;
    }

    public void setSelectionBackgroundColor(Color selectionBackgroundColor) {
        this.selectionBackgroundColor = selectionBackgroundColor;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public boolean isShowRoundedSelectionCorners() {
        return this.showRoundedSelectionCorners;
    }

    public void setShowRoundedSelectionCorners(
        boolean showRoundedSelectionCorners) {
        this.showRoundedSelectionCorners = showRoundedSelectionCorners;
    }

}
