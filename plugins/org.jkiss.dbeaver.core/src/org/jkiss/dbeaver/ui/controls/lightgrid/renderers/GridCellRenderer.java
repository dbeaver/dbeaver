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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ui.TextUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.lightgrid.LightGrid;

/**
 * Grid cell renderer
 */
public class GridCellRenderer extends AbstractRenderer
{
    private static final int leftMargin = 4;
    private static final int rightMargin = 4;
    private static final int topMargin = 0;
    //private int bottomMargin = 0;
    private static final int textTopMargin = 1;
    //private int textBottomMargin = 2;
    private static final int insideMargin = 3;
    protected Color colorSelected;
    protected Color colorSelectedText;
    protected Color colorBackgroundDisabled;
    protected Color colorLineForeground;
    protected Color colorLineFocused;
    private int alignment = SWT.LEFT;

    private boolean rowHover = false;

    private boolean columnHover = false;

    private boolean rowFocus = false;

    private boolean cellFocus = false;

    private boolean cellSelected = false;

    private boolean dragging = false;

    public GridCellRenderer(LightGrid grid)
    {
        super(grid);
        colorLineFocused = grid.getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND);
        colorSelectedText = grid.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT);
        colorSelected = grid.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION);
        colorLineForeground = grid.getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW);
        colorBackgroundDisabled = grid.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
    }

    /**
     * @return Returns the alignment.
     */
    public int getAlignment()
    {
        return alignment;
    }

    /**
     * @param alignment The alignment to set.
     */
    public void setAlignment(int alignment)
    {
        this.alignment = alignment;
    }

    /**
     * @return Returns the columnHover.
     */
    public boolean isColumnHover()
    {
        return columnHover;
    }

    /**
     * @param columnHover The columnHover to set.
     */
    public void setColumnHover(boolean columnHover)
    {
        this.columnHover = columnHover;
    }

    /**
     * @return Returns the rowHover.
     */
    public boolean isRowHover()
    {
        return rowHover;
    }

    /**
     * @param rowHover The rowHover to set.
     */
    public void setRowHover(boolean rowHover)
    {
        this.rowHover = rowHover;
    }

    /**
     * @return Returns the columnFocus.
     */
    public boolean isCellFocus()
    {
        return cellFocus;
    }

    /**
     * @param columnFocus The columnFocus to set.
     */
    public void setCellFocus(boolean columnFocus)
    {
        this.cellFocus = columnFocus;
    }

    /**
     * @return Returns the rowFocus.
     */
    public boolean isRowFocus()
    {
        return rowFocus;
    }

    /**
     * @param rowFocus The rowFocus to set.
     */
    public void setRowFocus(boolean rowFocus)
    {
        this.rowFocus = rowFocus;
    }

    /**
     * @return the cellSelected
     */
    public boolean isCellSelected()
    {
        return cellSelected;
    }

    /**
     * @param cellSelected the cellSelected to set
     */
    public void setCellSelected(boolean cellSelected)
    {
        this.cellSelected = cellSelected;
    }

    /**
     * Gets the dragging state.
     *
     * @return Returns the dragging state.
     */
    public boolean isDragging()
    {
    	return dragging;
    }

    /**
     * Sets the dragging state.
     *
     * @param dragging The state to set.
     */
    public void setDragging(boolean dragging)
    {
    	this.dragging = dragging;
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
            Color cellBackground = grid.getCellBackground(cell);
            if (cellBackground.equals(grid.getBackground())) {
                gc.setBackground(colorSelected);
            } else {
                RGB cellSel = LightGrid.blend(
                    cellBackground.getRGB(),
                    colorSelected.getRGB(),
                    50);

                gc.setBackground(DBeaverUI.getSharedTextColors().getColor(cellSel));
            }
            gc.setForeground(colorSelectedText);
        } else {
            if (grid.isEnabled()) {
                Color back = grid.getCellBackground(cell);

                if (back != null) {
                    gc.setBackground(back);
                } else {
                    drawBackground = false;
                }
            } else {
                gc.setBackground(colorBackgroundDisabled);
            }
            gc.setForeground(grid.getCellForeground(cell));
        }

        if (drawBackground)
            gc.fillRectangle(bounds.x, bounds.y, bounds.width,
                bounds.height);


        int x = leftMargin;

        Image image = grid.getCellImage(cell);
        if (image != null) {
            int y = bounds.y;

            y += (bounds.height - image.getBounds().height) / 2;

            gc.drawImage(image, bounds.x + x, y);

            x += image.getBounds().width + insideMargin;
        }

        int width = bounds.width - x - rightMargin;

//        if (drawAsSelected) {
//            gc.setForeground(colorSelectedText);
//        } else {
//            gc.setForeground(grid.getCellForeground(cell));
//        }

        // Get cell text
        String text = grid.getCellText(cell);
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
}
