/*
 * Copyright (C) 2010-2015 Serge Rieder
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
    private int height = -1;

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
        grid.newColumn(this, -1);
    }

    public Object getElement() {
        return element;
    }

    public int getIndex()
    {
        return grid.indexOf(this);
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
        int delta = width - this.width;
		this.width = width;
        if (parent != null) {
            parent.width += delta;
        }
		if (redraw) {
			grid.setScrollValuesObsolete();
			grid.redraw();
		}
	}

    public boolean isOverSortArrow(int x, int y)
    {
        if (!isSortable()) {
            return false;
        }
        Rectangle bounds = getBounds();
        if (y < bounds.y || y > bounds.y + bounds.height) {
            return false;
        }
        int arrowEnd = bounds.width - rightMargin;
        Rectangle sortBounds = GridColumnRenderer.getSortControlBounds();
        int arrowBegin = arrowEnd - sortBounds.width;
        return x >= arrowBegin && x <= arrowEnd && y < bounds.y + sortBounds.height;
    }

    int getHeaderHeight(boolean includeChildren)
    {
        if (height < 0) {
            height = topMargin + grid.fontMetrics.getHeight() + bottomMargin;
            Image image = grid.getLabelProvider().getImage(element);
            if (image != null) {
                height = Math.max(height, topMargin + image.getBounds().height + bottomMargin);
            }
        }
        int childHeight = 0;
        if (includeChildren && !CommonUtils.isEmpty(children)) {
            for (GridColumn child : children) {
                childHeight = Math.max(childHeight, child.getHeaderHeight(true));
            }
        }
        return height + childHeight;
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
	void pack(boolean reflect) {
		int newWidth = computeHeaderWidth();
        if (CommonUtils.isEmpty(children)) {
            // Calculate width of visible cells
            int topIndex = grid.getTopIndex();
            int bottomIndex = grid.getBottomIndex();
            if (topIndex >= 0 && bottomIndex >= topIndex) {
                int itemCount = grid.getItemCount();
                for (int i = topIndex; i <= bottomIndex && i < itemCount; i++) {
                    newWidth = Math.max(newWidth, computeCellWidth(element, grid.getRowElement(i)));
                }
            }
        } else {
            int childrenWidth = 0;
            for (GridColumn child : children) {
                child.pack(reflect);
                childrenWidth += child.getWidth();
            }
            newWidth = Math.max(newWidth, childrenWidth);
        }
        if (reflect) {
            setWidth(newWidth, false);
        } else {
		    this.width = newWidth;
        }
	}

    private int computeCellWidth(Object col, Object row) {
        int x = 0;

        x += leftMargin;

        int state = grid.getContentProvider().getCellState(col, row);
        Rectangle imageBounds;
        if ((state & IGridContentProvider.STATE_LINK) != 0) {
            imageBounds = GridCellRenderer.LINK_IMAGE_BOUNDS;
        } else {
            Image image = grid.getContentProvider().getCellImage(col, row);
            imageBounds = image == null ? null : image.getBounds();
        }
        if (imageBounds != null) {
            x += imageBounds.width + insideMargin;
        }

        x += grid.sizingGC.textExtent(grid.getCellText(col, row)).x + rightMargin;
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

    public boolean isParent(GridColumn col) {
        for (GridColumn p = parent; p != null; p = p.parent) {
            if (p == col) {
                return true;
            }
        }
        return false;
    }

    public GridColumn getFirstLeaf() {
        if (children == null) {
            return this;
        } else {
            return children.get(0).getFirstLeaf();
        }
    }

}