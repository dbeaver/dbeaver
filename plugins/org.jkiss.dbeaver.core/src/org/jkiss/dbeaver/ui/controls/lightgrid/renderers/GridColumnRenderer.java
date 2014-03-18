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

package  org.jkiss.dbeaver.ui.controls.lightgrid.renderers;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridColumn;
import org.jkiss.dbeaver.ui.controls.lightgrid.LightGrid;

/**
 * Grid column renderer
 */
public class GridColumnRenderer extends AbstractRenderer
{
    public static final int leftMargin = 6;
    public static final int rightMargin = 6;
    public static final int bottomMargin = 3;
    public static final int arrowMargin = 6;
    public static final int imageSpacing = 3;

    public  GridColumnRenderer(LightGrid grid) {
        super(grid);
    }

    @Nullable
    protected Image getColumnImage() {
        return grid.getColumnLabelProvider().getImage(cell.col);
    }

    protected String getColumnText()
    {
        String text = grid.getColumnLabelProvider().getText(cell.col);
        if (text == null)
        {
            text = String.valueOf(cell.col);
        }
        return text;
    }
    
    protected Color getColumnBackground() {
        return grid.getColumnLabelProvider().getBackground(cell.col);
    }
    
    protected Color getColumnForeground() {
        return grid.getColumnLabelProvider().getForeground(cell.col);
    }

    protected Font getColumnFont() {
        Font font = grid.getColumnLabelProvider().getFont(cell.col);
        return font != null ? font : grid.getFont();
    }

    @Override
    public void paint(GC gc) {
        GridColumn col = grid.getColumnByElement(cell.col);
        AbstractRenderer arrowRenderer = col.getSortRenderer();

        // set the font to be used to display the text.
        gc.setFont(getColumnFont());

        boolean flat = true;

        boolean drawSelected = ((isMouseDown() && isHover()));

        if (flat && isSelected()) {
            gc.setBackground(grid.getCellHeaderSelectionBackground());
            //gc.setForeground(grid.getCellHeaderSelectionForeground());
        } else {
            gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
            //gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND));
        }
        gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND));

        gc.fillRectangle(getBounds().x, getBounds().y, getBounds().width, getBounds().height);

        int pushedDrawingOffset = 0;
        if (drawSelected) {
            pushedDrawingOffset = 1;
        }

        int x = leftMargin;

        Image columnImage = getColumnImage();
        if (columnImage != null) {
            int y = bottomMargin;

            if (col.getHeaderControl() == null) {
                y = getBounds().y + pushedDrawingOffset + getBounds().height - bottomMargin - columnImage.getBounds().height;
            }

            gc.drawImage(columnImage, getBounds().x + x + pushedDrawingOffset, y);
            x += columnImage.getBounds().width + imageSpacing;
        }

        int width = getBounds().width - x;

        if (!col.isSortable()) {
            width -= rightMargin;
        } else {
            width -= arrowMargin + arrowRenderer.getSize().x + arrowMargin;
        }

        //gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));

        int y;

        if (col.getHeaderControl() == null) {
            y = getBounds().y + getBounds().height - bottomMargin - gc.getFontMetrics().getHeight();
        } else {
            y = getBounds().y + getBounds().height - bottomMargin - gc.getFontMetrics().getHeight() - computeControlSize(col).y;
        }

        String text = getColumnText();

        text = org.jkiss.dbeaver.ui.TextUtils.getShortString(gc, text, width);

        if (col.getAlignment() == SWT.RIGHT) {
            int len = gc.stringExtent(text).x;
            if (len < width) {
                x += width - len;
            }
        } else if (col.getAlignment() == SWT.CENTER) {
            int len = gc.stringExtent(text).x;
            if (len < width) {
                x += (width - len) / 2;
            }
        }

        gc.drawString(text, getBounds().x + x + pushedDrawingOffset, y + pushedDrawingOffset, true);

        if (col.isSortable()) {
            if (col.getHeaderControl() == null) {
                y = getBounds().y
                    + ((getBounds().height - arrowRenderer.getBounds().height) / 2)
                    + 1;
            } else {
                y = getBounds().y
                    + ((getBounds().height - computeControlSize(col).y - arrowRenderer.getBounds().height) / 2)
                    + 1;
            }

//            arrowRenderer.setSelected(col.getSort() == SWT.UP);
            if (drawSelected) {
                arrowRenderer
                    .setLocation(
                        getBounds().x + getBounds().width - arrowMargin
                            - arrowRenderer.getBounds().width + 1, y
                    );
            } else {
                if (col.getHeaderControl() == null) {
                    y = getBounds().y
                        + ((getBounds().height - arrowRenderer.getBounds().height) / 2);
                } else {
                    y = getBounds().y
                        + ((getBounds().height - computeControlSize(col).y - arrowRenderer.getBounds().height) / 2);
                }
                arrowRenderer
                    .setLocation(
                        getBounds().x + getBounds().width - arrowMargin
                            - arrowRenderer.getBounds().width, y);
            }
            arrowRenderer.paint(gc);
        }

        if (!flat) {

            if (drawSelected) {
                gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
            } else {
                gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW));
            }

            gc.drawLine(getBounds().x, getBounds().y, getBounds().x + getBounds().width - 1,
                getBounds().y);
            gc.drawLine(getBounds().x, getBounds().y, getBounds().x, getBounds().y + getBounds().height
                - 1);

            if (!drawSelected) {
                gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
                gc.drawLine(getBounds().x + 1, getBounds().y + 1,
                    getBounds().x + getBounds().width - 2, getBounds().y + 1);
                gc.drawLine(getBounds().x + 1, getBounds().y + 1, getBounds().x + 1,
                    getBounds().y + getBounds().height - 2);
            }

            if (drawSelected) {
                gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
            } else {
                gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));
            }
            gc.drawLine(getBounds().x + getBounds().width - 1, getBounds().y, getBounds().x
                + getBounds().width - 1,
                getBounds().y + getBounds().height - 1);
            gc.drawLine(getBounds().x, getBounds().y + getBounds().height - 1, getBounds().x
                + getBounds().width - 1,
                getBounds().y + getBounds().height - 1);

            if (!drawSelected) {
                gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
                gc.drawLine(getBounds().x + getBounds().width - 2, getBounds().y + 1,
                    getBounds().x + getBounds().width - 2, getBounds().y + getBounds().height
                        - 2);
                gc.drawLine(getBounds().x + 1, getBounds().y + getBounds().height - 2,
                    getBounds().x + getBounds().width - 2, getBounds().y + getBounds().height
                        - 2);
            }

        } else {
            gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));

            gc.drawLine(getBounds().x + getBounds().width - 1, getBounds().y, getBounds().x
                + getBounds().width - 1,
                getBounds().y + getBounds().height - 1);
            gc.drawLine(getBounds().x, getBounds().y + getBounds().height - 1, getBounds().x
                + getBounds().width - 1,
                getBounds().y + getBounds().height - 1);
        }

        gc.setFont(grid.getFont());
    }

    public static Point computeControlSize(GridColumn column) {
        Control headerControl = column.getHeaderControl();
        if (headerControl != null) {
            return headerControl.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        }
        return new Point(0, 0);
    }
}
