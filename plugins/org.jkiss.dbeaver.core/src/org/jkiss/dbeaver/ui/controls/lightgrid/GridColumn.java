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
package  org.jkiss.dbeaver.ui.controls.lightgrid;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.controls.lightgrid.renderers.GridCellRenderer;
import org.jkiss.dbeaver.ui.controls.lightgrid.renderers.GridColumnRenderer;

/**
 * Instances of this class represent a column in a grid widget.
 * For grid internal use.
 *
 * @author serge@jkiss.org
 */
public class GridColumn {

	/**
	 * Default width of the column.
	 */
	private static final int DEFAULT_WIDTH = 10;

    private static final int topMargin = 3;
    private static final int bottomMargin = 3;
    private static final int leftMargin = 6;
    private static final int rightMargin = 6;
    private static final int imageSpacing = 3;
    private static final int insideMargin = 3;

	/**
	 * Parent table.
	 */
	private final LightGrid parent;
    private final Object element;

	private GridCellRenderer cellRenderer;
	private int width = DEFAULT_WIDTH;

	private boolean cellSelectionEnabled = true;

	private int minimumWidth = 0;

	private String headerTooltip = null;

    private String text;
    private Image image;

    /**
	 * Constructs a new instance of this class given its parent (which must be a
	 * <code>Grid</code>) and a style value describing its behavior and
	 * appearance. The item is added to the end of the items maintained by its
	 * parent.
	 */
	public GridColumn(LightGrid parent, Object element) {
		this(parent, -1, element);
	}

	/**
	 * Constructs a new instance of this class given its parent (which must be a
	 * <code>Grid</code>), a style value describing its behavior and appearance,
	 * and the index at which to place it in the items maintained by its parent.
	 *
	 * @param parent
	 *            an Grid control which will be the parent of the new instance
	 *            (cannot be null)
	 * @param index
	 *            the index to store the receiver in its parent
	 */
	public GridColumn(LightGrid parent, int index, Object element) {
        this.parent = parent;
        this.cellRenderer = new GridCellRenderer(parent);
        this.element = element;
        parent.newColumn(this, index);
	}

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Image getImage() {
        return image;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public Object getElement() {
        return element;
    }

    public int getIndex()
    {
        return getParent().indexOf(this);
    }

    public void dispose() {
		if (!parent.isDisposing()) {
			parent.removeColumn(this);
		}
	}

	/**
	 * Returns the cell renderer.
	 *
	 * @return cell renderer.
	 */
	public GridCellRenderer getCellRenderer() {
		return cellRenderer;
	}

	/**
	 * Returns the width of the column.
	 *
	 * @return width of column
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * Sets the width of the column.
	 *
	 * @param width
	 *            new width
	 */
	public void setWidth(int width) {
		setWidth(width, true);
	}

	void setWidth(int width, boolean redraw) {
		this.width = Math.max(minimumWidth, width);
		if (redraw) {
			parent.setScrollValuesObsolete();
			parent.redraw();
		}
	}

    public int computeHeaderHeight()
    {
        int y = topMargin + parent.sizingGC.getFontMetrics().getHeight() + bottomMargin;

        if (getImage() != null) {
            y = Math.max(y, topMargin + getImage().getBounds().height + bottomMargin);
        }

		return y;
    }

    public boolean isOverSortArrow(int x)
    {
        if (!isSortable()) {
            return false;
        }
        int arrowEnd = getBounds().width - rightMargin;
        int arrowBegin = arrowEnd - GridColumnRenderer.getSortControlBounds().width;
        return x >= arrowBegin && x <= arrowEnd;
    }

    public int computeHeaderWidth()
    {
        int x = leftMargin;
        if (getImage() != null) {
            x += getImage().getBounds().width + imageSpacing;
        }
        x += parent.sizingGC.stringExtent(getText()).x + rightMargin;
        if (isSortable()) {
            x += rightMargin + GridColumnRenderer.getSortControlBounds().width;
        }

        return x;
    }

    public boolean isSortable()
    {
        return parent.getContentProvider().getColumnSortOrder(element) != SWT.NONE;
    }

	/**
	 * Causes the receiver to be resized to its preferred size.
	 *
	 */
	public void pack() {
		int newWidth = computeHeaderWidth();
        //int columnIndex = getIndex();
        int topIndex = parent.getTopIndex();
        int bottomIndex = parent.getBottomIndex();
        if (topIndex >= 0 && bottomIndex >= topIndex) {
            int itemCount = parent.getItemCount();
            GridCell cell = new GridCell(element, element);
            for (int i = topIndex; i <= bottomIndex && i < itemCount; i++) {
                cell.row = parent.getRowElement(i);
                newWidth = Math.max(newWidth, computeCellWidth(cell));
            }
        }

		setWidth(newWidth);
		parent.redraw();
	}

    private int computeCellWidth(GridCell cell) {
        int x = 0;

        x += leftMargin;

        Image image = parent.getContentProvider().getCellImage(cell);
        if (image != null) {
            x += image.getBounds().width + insideMargin;
        }

        x += parent.sizingGC.textExtent(parent.getCellText(cell)).x + rightMargin;
        return x;
    }

	/**
	 * Returns the bounds of this column's header.
	 *
	 * @return bounds of the column header
	 */
	Rectangle getBounds() {
		Rectangle bounds = new Rectangle(0, 0, 0, 0);

		Point loc = parent.getOrigin(this, -1);
		bounds.x = loc.x;
		bounds.y = loc.y;
		bounds.width = getWidth();
		bounds.height = parent.getHeaderHeight();

		return bounds;
	}

	/**
	 * Returns true if cells in the receiver can be selected.
	 *
	 * @return the cellSelectionEnabled
	 */
	public boolean getCellSelectionEnabled() {
		return cellSelectionEnabled;
	}

	/**
	 * Sets whether cells in the receiver can be selected.
	 *
	 * @param cellSelectionEnabled
	 *            the cellSelectionEnabled to set
	 */
	public void setCellSelectionEnabled(boolean cellSelectionEnabled) {
		this.cellSelectionEnabled = cellSelectionEnabled;
	}

	/**
	 * Returns the parent grid.
	 *
	 * @return the parent grid.
	 */
	public LightGrid getParent() {
		return parent;
	}

	/**
	 * Returns the tooltip of the column header.
	 *
	 * @return the tooltip text
	 */
	@Nullable
    public String getHeaderTooltip() {
        if (headerTooltip != null) {
            return headerTooltip;
        }
        String tip = getText();
        Point ttSize = getParent().sizingGC.textExtent(tip);
        if (ttSize.x > getWidth()) {
            return tip;
        }

		return null;
	}

	/**
	 * Sets the tooltip text of the column header.
	 *
	 * @param tooltip the tooltip text
	 */
	public void setHeaderTooltip(@Nullable String tooltip) {
		this.headerTooltip = tooltip;
	}

	/**
	 * @return the minimum width
	 */
	public int getMinimumWidth() {
		return minimumWidth;
	}

	/**
	 * Set the minimum width of the column
	 *
	 * @param minimumWidth
	 *            the minimum width
	 */
	public void setMinimumWidth(int minimumWidth) {
		this.minimumWidth = Math.max(0, minimumWidth);
		if( minimumWidth > getWidth() ) {
			setWidth(minimumWidth, true);
		}
	}
}