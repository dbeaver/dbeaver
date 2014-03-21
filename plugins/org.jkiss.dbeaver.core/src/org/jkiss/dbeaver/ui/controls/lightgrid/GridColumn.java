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
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Grid column info.
 * Holds information about column width and other UI properties
 *
 * @author serge@jkiss.org
 */
class GridColumn {

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

	private final LightGrid grid;
    private final Object element;
    private final GridColumn parent;
    private List<GridColumn> children;

    private int level;
    private int width = DEFAULT_WIDTH;

	public GridColumn(LightGrid grid, Object element) {
        this.grid = grid;
        this.element = element;
        this.parent = null;
        this.level = 0;
        grid.newColumn(this, -1);
	}

    public GridColumn(GridColumn parent, Object element) {
        this.grid = parent.grid;
        this.element = element;
        this.parent = parent;
        this.level = parent.level + 1;
        parent.addChild(this);
    }

    public Object getElement() {
        return element;
    }

    public int getIndex()
    {
        return getGrid().indexOf(this);
    }

    void dispose() {
		if (!grid.isDisposing()) {
            if (parent == null) {
			    grid.removeColumn(this);
            } else {
                parent.removeChild(this);
            }
		}
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
		this.width = width;
		if (redraw) {
			grid.setScrollValuesObsolete();
			grid.redraw();
		}
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

    int computeHeaderHeight(boolean includeChildren)
    {
        int y = topMargin + grid.sizingGC.getFontMetrics().getHeight() + bottomMargin;
        Image image = grid.getLabelProvider().getImage(element);
        if (image != null) {
            y = Math.max(y, topMargin + image.getBounds().height + bottomMargin);
        }
        int childHeight = 0;
        if (includeChildren && !CommonUtils.isEmpty(children)) {
            for (GridColumn child : children) {
                childHeight = Math.max(childHeight, child.computeHeaderHeight(true));
            }
        }
        return y + childHeight;
    }

    int computeHeaderWidth()
    {
        int x = leftMargin;
        Image image = grid.getLabelProvider().getImage(element);
        String text = grid.getLabelProvider().getText(element);
        if (image != null) {
            x += image.getBounds().width + imageSpacing;
        }
        x += grid.sizingGC.stringExtent(text).x + rightMargin;
        if (isSortable()) {
            x += rightMargin + GridColumnRenderer.getSortControlBounds().width;
        }

        if (!CommonUtils.isEmpty(children)) {
            int childWidth = 0;
            for (GridColumn child : children) {
                childWidth += child.computeHeaderWidth();
            }
            return Math.max(x, childWidth);
        }

        return x;
    }

    public boolean isSortable()
    {
        return grid.getContentProvider().getSortOrder(element) != SWT.NONE;
    }

	/**
	 * Causes the receiver to be resized to its preferred size.
	 *
	 */
	void pack() {
		int newWidth = computeHeaderWidth();
        //int columnIndex = getIndex();
        int topIndex = grid.getTopIndex();
        int bottomIndex = grid.getBottomIndex();
        if (topIndex >= 0 && bottomIndex >= topIndex) {
            int itemCount = grid.getItemCount();
            GridCell cell = new GridCell(element, element);
            for (int i = topIndex; i <= bottomIndex && i < itemCount; i++) {
                cell.row = grid.getRowElement(i);
                newWidth = Math.max(newWidth, computeCellWidth(cell));
            }
        }

		setWidth(newWidth);
	}

    private int computeCellWidth(GridCell cell) {
        int x = 0;

        x += leftMargin;

        Image image = grid.getContentProvider().getCellImage(cell);
        if (image != null) {
            x += image.getBounds().width + insideMargin;
        }

        x += grid.sizingGC.textExtent(grid.getCellText(cell)).x + rightMargin;
        return x;
    }

	/**
	 * Returns the bounds of this column's header.
	 *
	 * @return bounds of the column header
	 */
	Rectangle getBounds() {
		Rectangle bounds = new Rectangle(0, 0, 0, 0);

		Point loc = grid.getOrigin(this, -1);
		bounds.x = loc.x;
		bounds.y = loc.y;
		bounds.width = getWidth();
		bounds.height = grid.getHeaderHeight();

		return bounds;
	}

	/**
	 * Returns the parent grid.
	 *
	 * @return the parent grid.
	 */
	public LightGrid getGrid() {
		return grid;
	}

	/**
	 * Returns the tooltip of the column header.
	 *
	 * @return the tooltip text
	 */
	@Nullable
    public String getHeaderTooltip() {
        String tip = grid.getLabelProvider().getTooltip(element);
        String text = grid.getLabelProvider().getText(element);
        if (tip == null) {
            tip = text;
        }
        if (!CommonUtils.equalObjects(tip, text)) {
            return tip;
        }
        Point ttSize = getGrid().sizingGC.textExtent(tip);
        if (ttSize.x > getWidth()) {
            return tip;
        }

		return null;
	}

    public GridColumn getParent() {
        return parent;
    }

    public List<GridColumn> getChildren() {
        return children;
    }

    private void addChild(GridColumn gridColumn) {
        if (children == null) {
            children = new ArrayList<GridColumn>();
        }
        children.add(gridColumn);
    }

    private void removeChild(GridColumn column) {
        children.remove(column);
    }

    public int getLevel() {
        return level;
    }
}