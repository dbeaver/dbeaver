/*
 * Copyright (C) 2010-2014 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.controls.lightgrid;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.jkiss.code.Nullable;

/**
 * Grid row header renderer.
 */
class GridRowRenderer extends AbstractRenderer {
    private static final int leftMargin = 6;
    private static final int rightMargin = 8;
    private final Color DEFAULT_BACKGROUND;
    private final Color DEFAULT_FOREGROUND;
    private final Color DEFAULT_FOREGROUND_TEXT;

    public GridRowRenderer(LightGrid grid) {
        super(grid);
        DEFAULT_BACKGROUND = getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
        DEFAULT_FOREGROUND = getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);
        DEFAULT_FOREGROUND_TEXT = getDisplay().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND);
    }



    @Override
    public void paint(GC gc) {
        String text = getHeaderText();

        gc.setFont(getDisplay().getSystemFont());

        Color background = getHeaderBackground();
        if (background == null) {
            background = DEFAULT_BACKGROUND;
        }
        gc.setBackground(background);

        if (isSelected()) {
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

        int x = leftMargin;

        Image image = getHeaderImage();

        if (image != null) {
            gc.drawImage(image, x, bounds.y + (bounds.height - image.getBounds().height) / 2);
            x += image.getBounds().width + 5;
        }

        int width = bounds.width - x;

        width -= rightMargin;

        Color foreground = getHeaderForeground();
        if (foreground == null) {
            foreground = DEFAULT_FOREGROUND_TEXT;
        }

        gc.setForeground(foreground);


        int y = bounds.y;
        int selectionOffset = 0;

        y += (bounds.height - gc.stringExtent(text).y) / 2;
        gc.drawString(org.jkiss.dbeaver.ui.TextUtils.getShortString(gc, text, width), bounds.x + x + selectionOffset, y + selectionOffset, true);
    }

    @Nullable
    private Image getHeaderImage() {
        return grid.getLabelProvider().getImage(cell.row);
    }

    protected String getHeaderText() {
        String text = grid.getLabelProvider().getText(cell.row);
        if (text == null) {
            text = String.valueOf(cell.row);
        }
        return text;
    }

    @Nullable
    protected Color getHeaderBackground() {
        return grid.getLabelProvider().getBackground(cell.row);
    }

    @Nullable
    protected Color getHeaderForeground() {
        return grid.getLabelProvider().getForeground(cell.row);
    }

}
