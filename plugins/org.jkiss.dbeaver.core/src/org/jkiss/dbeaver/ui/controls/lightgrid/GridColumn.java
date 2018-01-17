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
package  org.jkiss.dbeaver.ui.controls.lightgrid;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.ui.DBeaverIcons;
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

    private static final int topMargin = 6;
    private static final int bottomMargin = 6;
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

    public boolean isOverFilterButton(int x, int y) {
	    if (!isFilterable()) {
	        return false;
        }
        Rectangle bounds = getBounds();
        if (y < bounds.y || y > bounds.y + bounds.height) {
            return false;
        }
        Rectangle sortBounds = isSortable() ? GridColumnRenderer.getSortControlBounds() : null;
        Rectangle filterBounds = GridColumnRenderer.getFilterControlBounds();

        int filterEnd = bounds.width - (sortBounds == null ? GridColumnRenderer.ARROW_MARGIN : sortBounds.width + GridColumnRenderer.IMAGE_SPACING);
        int filterBegin = filterEnd - filterBounds.width;

        return x >= filterBegin && x <= filterEnd && y < bounds.y + (sortBounds == null ? 0 : sortBounds.height);
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

    public boolean isOverIcon(int x, int y) {
        Rectangle bounds = getBounds();
        if (y < bounds.y || y > bounds.y + bounds.height) {
            return false;
        }
        Image image = grid.getLabelProvider().getImage(element);
        if (image == null) {
            return false;
        }
        Rectangle imgBounds = image.getBounds();
        if (x >= bounds.x + GridColumnRenderer.LEFT_MARGIN &&
            x <= bounds.x + GridColumnRenderer.LEFT_MARGIN + imgBounds.width + GridColumnRenderer.IMAGE_SPACING &&
            y > bounds.y + GridColumnRenderer.TOP_MARGIN &&
            y <= bounds.y + GridColumnRenderer.TOP_MARGIN + imgBounds.height)
        {
            return true;
        }
        return false;
    }

    int getHeaderHeight(boolean includeChildren, boolean forceRefresh)
    {
        if (forceRefresh) {
            height = -1;
        }
        if (height < 0) {
            height = topMargin + grid.fontMetrics.getHeight() + bottomMargin;
            Image image = grid.getLabelProvider().getImage(element);
            if (image != null) {
                height = Math.max(height, topMargin + image.getBounds().height + bottomMargin);
            }
            final String description = grid.getLabelProvider().getDescription(element);
            if (!CommonUtils.isEmpty(description)) {
                height += topMargin + grid.fontMetrics.getHeight();
            }
        }
        int childHeight = 0;
        if (includeChildren && !CommonUtils.isEmpty(children)) {
            for (GridColumn child : children) {
                childHeight = Math.max(childHeight, child.getHeaderHeight(true, false));
            }
        }
        return height + childHeight;
    }

    int computeHeaderWidth()
    {
        int x = leftMargin;
        final IGridLabelProvider labelProvider = grid.getLabelProvider();
        Image image = labelProvider.getImage(element);
        if (image != null) {
            x += image.getBounds().width + imageSpacing;
        }
        {
            int textWidth;
            if (Boolean.TRUE.equals(labelProvider.getGridOption(IGridLabelProvider.OPTION_EXCLUDE_COLUMN_NAME_FOR_WIDTH_CALC))) {
                textWidth = grid.sizingGC.stringExtent("X").x;
            } else {
                String text = labelProvider.getText(element);
                String description = labelProvider.getDescription(element);
                textWidth = grid.sizingGC.stringExtent(text).x;
                if (!CommonUtils.isEmpty(description)) {
                    int descWidth = grid.sizingGC.stringExtent(description).x;
                    if (descWidth > textWidth) {
                        textWidth = descWidth;
                    }
                }
            }
            x += textWidth + rightMargin;
        }
        if (isSortable()) {
            x += rightMargin + GridColumnRenderer.getSortControlBounds().width;
        }

        x+= GridColumnRenderer.getFilterControlBounds().width;
        
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

    public boolean isFilterable()
    {
        return grid.getContentProvider().isElementSupportsFilter(element);
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
            if (newWidth > childrenWidth) {
                // Header width bigger than children width
                GridColumn lastChild = children.get(children.size() - 1);
                lastChild.setWidth(lastChild.getWidth() + newWidth - childrenWidth);
            } else {
                newWidth = childrenWidth;
            }
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

        String cellText = grid.getCellText(col, row);
        int state = grid.getContentProvider().getCellState(col, row, cellText);
        Rectangle imageBounds;
        if (GridCellRenderer.isLinkState(state)) {
            imageBounds = GridCellRenderer.LINK_IMAGE_BOUNDS;
        } else {
            DBPImage image = grid.getContentProvider().getCellImage(col, row);
            imageBounds = image == null ? null : DBeaverIcons.getImage(image).getBounds();
        }
        if (imageBounds != null) {
            x += imageBounds.width + insideMargin;
        }

        x += grid.sizingGC.textExtent(cellText).x + rightMargin;
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
        String tip = grid.getLabelProvider().getToolTipText(element);
        if (tip == null) {
            tip = grid.getLabelProvider().getText(element);
        }
        return tip;
	}

    public GridColumn getParent() {
        return parent;
    }

    public List<GridColumn> getChildren() {
        return children;
    }

    private void addChild(GridColumn gridColumn) {
        if (children == null) {
            children = new ArrayList<>();
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