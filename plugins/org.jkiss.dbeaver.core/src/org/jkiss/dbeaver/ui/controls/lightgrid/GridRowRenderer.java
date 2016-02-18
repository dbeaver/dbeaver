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
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.TextUtils;
import org.jkiss.dbeaver.ui.UIIcon;

/**
 * Grid row header renderer.
 */
class GridRowRenderer extends AbstractRenderer {

    static final Image IMG_EXPAND = DBeaverIcons.getImage(UIIcon.TREE_EXPAND);
    static final Image IMG_COLLAPSE = DBeaverIcons.getImage(UIIcon.TREE_COLLAPSE);
    static final Rectangle EXPANDED_BOUNDS = IMG_EXPAND.getBounds();

    public static final int LEFT_MARGIN = 4;
    public static final int RIGHT_MARGIN = 4;
    public static final int IMAGE_SPACING = 5;
    public static final int EXPANDER_SPACING = 2;
    public static final int LEVEL_SPACING = EXPANDED_BOUNDS.width;

    final Color DEFAULT_BACKGROUND;
    final Color DEFAULT_FOREGROUND;
    final Color DEFAULT_FOREGROUND_TEXT;

    public GridRowRenderer(LightGrid grid) {
        super(grid);
        DEFAULT_BACKGROUND = getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
        DEFAULT_FOREGROUND = getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);
        DEFAULT_FOREGROUND_TEXT = getDisplay().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND);
    }

    public void paint(GC gc, Rectangle bounds, boolean selected, int level, IGridContentProvider.ElementState state, Object element) {
        String text = grid.getLabelProvider().getText(element);

        gc.setFont(getDisplay().getSystemFont());

        Color background = grid.getLabelProvider().getBackground(element);
        if (background == null) {
            background = DEFAULT_BACKGROUND;
        }
        gc.setBackground(background);

        if (selected) {
            gc.setBackground(grid.getCellHeaderSelectionBackground());
        }

        gc.fillRectangle(bounds.x, bounds.y, bounds.width, bounds.height + 1);

        {
            gc.setForeground(DEFAULT_FOREGROUND);

            gc.drawLine(
                bounds.x + bounds.width - 1,
                bounds.y,
                bounds.x + bounds.width - 1,
                bounds.y + bounds.height - 1);
            gc.drawLine(
                bounds.x,
                bounds.y + bounds.height - 1,
                bounds.x + bounds.width - 1,
                bounds.y + bounds.height - 1);
        }

        int x = LEFT_MARGIN;
        if (level > 0) {
            x += level * LEVEL_SPACING;
        }
        if (state != IGridContentProvider.ElementState.NONE) {
            Image expandImage = state == IGridContentProvider.ElementState.EXPANDED ? IMG_COLLAPSE : IMG_EXPAND;
            gc.drawImage(expandImage, x, bounds.y + (bounds.height - EXPANDED_BOUNDS.height) / 2);
            x += EXPANDED_BOUNDS.width + EXPANDER_SPACING;
        } else if (grid.hasNodes()) {
            x += EXPANDED_BOUNDS.width + EXPANDER_SPACING;
        }

        Image image = grid.getLabelProvider().getImage(element);

        if (image != null) {
            gc.drawImage(image, x, bounds.y + (bounds.height - image.getBounds().height) / 2);
            x += image.getBounds().width + IMAGE_SPACING;
        }

        int width = bounds.width - x;

        width -= RIGHT_MARGIN;

        Color foreground = grid.getLabelProvider().getForeground(element);
        if (foreground == null) {
            foreground = DEFAULT_FOREGROUND_TEXT;
        }

        gc.setForeground(foreground);

        int y = bounds.y;
        int selectionOffset = 0;

        y += (bounds.height - gc.stringExtent(text).y) / 2;
        gc.drawString(TextUtils.getShortString(grid.fontMetrics, text, width), bounds.x + x + selectionOffset, y + selectionOffset, true);
    }

    public int computeHeaderWidth(Object element, int level) {
        int width = GridRowRenderer.LEFT_MARGIN + GridRowRenderer.RIGHT_MARGIN;
        if (grid.hasNodes()) {
            width += GridRowRenderer.EXPANDED_BOUNDS.width + EXPANDER_SPACING;
        }
        Image rowImage = grid.getLabelProvider().getImage(element);
        if (rowImage != null) {
            width += rowImage.getBounds().width;
            width += GridRowRenderer.IMAGE_SPACING;
        }
        String rowText = grid.getLabelProvider().getText(element);
        Point ext = grid.sizingGC.stringExtent(rowText);
        width += ext.x;
        width += level * GridRowRenderer.LEVEL_SPACING;
        return width;
    }

    public static boolean isOverExpander(int x, int level)
    {
        int expandBegin = LEFT_MARGIN + level * LEVEL_SPACING;
        int expandEnd = expandBegin + EXPANDED_BOUNDS.width;
        return x >= expandBegin && x <= expandEnd;
    }
}
