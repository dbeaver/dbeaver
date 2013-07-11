/*
 * Copyright (C) 2010-2013 Serge Rieder
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
import org.eclipse.swt.graphics.Rectangle;
import org.jkiss.dbeaver.ui.controls.lightgrid.LightGrid;

/**
 * <p>
 * NOTE:  THIS WIDGET AND ITS API ARE STILL UNDER DEVELOPMENT.  THIS IS A PRE-RELEASE ALPHA 
 * VERSION.  USERS SHOULD EXPECT API CHANGES IN FUTURE VERSIONS.
 * </p> 
 * The super class for all grid cell renderers.  Contains the properties specific
 * to a grid cell.
 *
 * @author serge@jkiss.org
 * @author chris.gross@us.ibm.com
 */
public abstract class GridCellRenderer extends AbstractGridWidget
{
    private int alignment = SWT.LEFT;

    private boolean rowHover = false;

    private boolean columnHover = false;

    private boolean rowFocus = false;

    private boolean cellFocus = false;

    private boolean cellSelected = false;

    private boolean dragging = false;

    protected GridCellRenderer(LightGrid grid)
    {
        super(grid);
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
     * Returns the bounds of the text in the cell.  This is used when displaying in-place tooltips.
     * If <code>null</code> is returned here, in-place tooltips will not be displayed.  If the 
     * <code>preferred</code> argument is <code>true</code> then the returned bounds should be large
     * enough to show the entire text.  If <code>preferred</code> is <code>false</code> then the 
     * returned bounds should be be relative to the current bounds.
     * 
     * @param row item to calculate text bounds.
     * @param preferred true if the preferred width of the text should be returned.
     * @return bounds of the text.
     */
    public Rectangle getTextBounds(int row, boolean preferred)
    {
        return null;
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
}
