/*
 * Copyright (C) 2010-2012 Serge Rieder
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
package org.jkiss.dbeaver.ui.controls.lightgrid.renderers;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.jkiss.dbeaver.ui.TextUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.lightgrid.LightGrid;

/**
 * The renderer for a cell in Grid.
 *
 * @author serge@jkiss.org
 * @author chris.gross@us.ibm.com
 * @since 2.0.0
 */
public class DefaultCellRenderer extends GridCellRenderer {

    private static final int leftMargin = 4;
    private static final int rightMargin = 4;
    private static final int topMargin = 0;
    //private int bottomMargin = 0;
    private static final int textTopMargin = 1;
    //private int textBottomMargin = 2;
    private static final int insideMargin = 3;
    //private int treeIndent = 20;

    private Color colorSelected;
    private Color colorSelectedText;
    private Color colorBackgroundDisabled;
    private Color colorLineForeground;
    private Color colorLineFocused;

    public DefaultCellRenderer(LightGrid grid)
    {
        super(grid);
        colorSelected = grid.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION);
        colorSelectedText = grid.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT);
        colorBackgroundDisabled = grid.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
        colorLineForeground = grid.getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW);
        colorLineFocused = grid.getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void paint(GC gc)
    {
        boolean drawAsSelected = isSelected();

        boolean drawBackground = true;

        if (isCellSelected()) {
            drawAsSelected = true;//(!isCellFocus());
        }

        if (drawAsSelected) {
            gc.setBackground(colorSelected);
            gc.setForeground(colorSelectedText);
        } else {
            if (grid.isEnabled()) {
                Color back = grid.getCellBackground(column, row);

                if (back != null) {
                    gc.setBackground(back);
                } else {
                    drawBackground = false;
                }
            } else {
                gc.setBackground(colorBackgroundDisabled);
            }
            gc.setForeground(grid.getCellForeground(column, row));
        }

        if (drawBackground)
            gc.fillRectangle(bounds.x, bounds.y, bounds.width,
                bounds.height);


        int x = leftMargin;

        Image image = grid.getCellImage(column, row);
        if (image != null) {
            int y = bounds.y;

            y += (bounds.height - image.getBounds().height) / 2;

            gc.drawImage(image, bounds.x + x, y);

            x += image.getBounds().width + insideMargin;
        }

        int width = bounds.width - x - rightMargin;

        if (drawAsSelected) {
            gc.setForeground(colorSelectedText);
        } else {
            gc.setForeground(grid.getCellForeground(column, row));
        }

        // Get cell text
        String text = grid.getCellText(column, row);
        if (text != null && !text.isEmpty()) {
            // Get shortern version of string
            text = TextUtils.getShortString(gc, text, width);
            // Replace linefeeds with space
            text = text.replace('\n', UIUtils.PARAGRAPH_CHAR).replace('\r', ' ');

            if (getAlignment() == SWT.RIGHT) {
                int len = gc.stringExtent(text).x;
                if (len < width) {
                    x += width - len;
                }
            } else if (getAlignment() == SWT.CENTER) {
                int len = gc.stringExtent(text).x;
                if (len < width) {
                    x += (width - len) / 2;
                }
            }

            gc.setFont(grid.getFont());
            gc.drawString(
                text,
                bounds.x + x,
                bounds.y + textTopMargin + topMargin,
                true);
        }

        if (grid.getLinesVisible()) {
            if (isCellSelected()) {
                //XXX: should be user definable?
                gc.setForeground(colorLineForeground);
            } else {
                gc.setForeground(grid.getLineColor());
            }
            gc.drawLine(bounds.x, bounds.y + bounds.height, bounds.x + bounds.width - 1,
                bounds.y + bounds.height);
            gc.drawLine(bounds.x + bounds.width - 1, bounds.y,
                bounds.x + bounds.width - 1, bounds.y + bounds.height);
        }

        if (isCellFocus()) {

            gc.setForeground(colorLineFocused);
            gc.drawRectangle(bounds.x, bounds.y, bounds.width - 1, bounds.height);

            if (isFocus()) {
                gc.drawRectangle(bounds.x + 1, bounds.y + 1, bounds.width - 3, bounds.height - 2);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean notify(int event, Point point, Object value)
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Rectangle getTextBounds(int row, boolean preferred)
    {
        int x = leftMargin;

        Image image = grid.getCellImage(row, column);
        if (image != null) {
            x += image.getBounds().width + insideMargin;
        }

        Rectangle bounds = new Rectangle(x, topMargin + textTopMargin, 0, 0);

        Point size = grid.sizingGC.stringExtent(grid.getCellText(column, row));

        bounds.height = size.y;

        if (preferred) {
            bounds.width = size.x - 1;
        } else {
            bounds.width = bounds.width - x - rightMargin;
        }

        return bounds;
    }

}
