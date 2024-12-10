/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.controls.lightgrid;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
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
 * @author serge@dbeaver.com
 */
public class GridColumn implements IGridColumn {

    /**
     * Default width of the column.
     */
    private static final int DEFAULT_WIDTH = 10;

    static final int topMargin = 6;
    static final int bottomMargin = 6;
    private static final int leftMargin = 6;
    private static final int rightMargin = 6;
    private static final int imageSpacing = 3;
    private static final int insideMargin = 3;

    private final LightGrid grid;
    private final Object element;
    private final GridColumn parent;
    private List<GridColumn> children;

    private final int level;
    private int width = DEFAULT_WIDTH;
    private int height = -1;
    private int pinIndex = -1;

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

    @Override
    public Object getElement() {
        return element;
    }

    @Override
    public int getIndex() {
        return grid.indexOf(this);
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public int getRelativeIndex() {
        return parent == null ? -1 : parent.getChildren().indexOf(this);
    }

    /**
     * Returns the width of the column.
     *
     * @return width of column
     */
    @Override
    public int getWidth() {
        return width;
    }

    /**
     * Sets the width of the column.
     *
     * @param width new width
     */
    public void setWidth(int width) {
        setWidth(width, true);
    }

    void setWidth(int width, boolean redraw) {
        int delta = width - this.width;
        this.width = width;
        for (GridColumn pc = parent; pc != null; pc = pc.parent) {
            pc.width += delta;
        }
        if (redraw) {
            grid.setScrollValuesObsolete();
            grid.redraw();
        }
    }

    @Override
    public boolean isPinned() {
        return pinIndex >= 0 || parent != null && parent.isPinned();
    }

    public int getPinIndex() {
        return parent == null ? pinIndex : parent.getPinIndex();
    }

    public void setPinIndex(int pinIndex) {
        this.pinIndex = pinIndex;
    }

    public boolean isOverFilterButton(int x, int y) {
        if (!isFilterable()) {
            return false;
        }

        Rectangle bounds = getBounds();

        Rectangle filterBounds = GridColumnRenderer.getFilterControlBounds();
        filterBounds.x = bounds.width - filterBounds.width - GridColumnRenderer.RIGHT_MARGIN;
        filterBounds.y = bounds.y + GridColumnRenderer.TOP_MARGIN;

        return filterBounds.contains(x, y);
    }

    public boolean isOverSortArrow(int x, int y) {
        IGridContentProvider contentProvider = grid.getContentProvider();
        if (contentProvider.getSortOrder(this) <= 0 && !contentProvider.isElementSupportsSort(this)) {
            return false;
        }

        Rectangle bounds = getBounds();

        Rectangle sortBounds = GridColumnRenderer.getSortControlBounds();
        sortBounds.x = bounds.width - sortBounds.width - GridColumnRenderer.RIGHT_MARGIN;
        sortBounds.y = bounds.y + GridColumnRenderer.TOP_MARGIN;

        if (isFilterable()) {
            sortBounds.x -= GridColumnRenderer.getFilterControlBounds().width + GridColumnRenderer.IMAGE_SPACING;
        }

        return sortBounds.contains(x, y);
    }

    public boolean isOverIcon(int x, int y) {
        Rectangle bounds = getBounds();

        Image image = grid.getLabelProvider().getImage(this);
        if (image == null) {
            return false;
        }

        Rectangle imgBounds = image.getBounds();
        imgBounds.x += bounds.x + GridColumnRenderer.LEFT_MARGIN;
        imgBounds.y += bounds.y + GridColumnRenderer.TOP_MARGIN;

        return imgBounds.contains(x, y);
    }

    int getHeaderHeight(boolean includeChildren, boolean forceRefresh) {
        if (forceRefresh) {
            height = -1;
        }
        if (height < 0) {
            height = topMargin + grid.fontMetrics.getHeight() + bottomMargin;
            Image image = grid.getLabelProvider().getImage(this);
            if (image != null) {
                height = Math.max(height, topMargin + image.getBounds().height + bottomMargin);
            }
            final String description = grid.getLabelProvider().getDescription(this);
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

    int computeHeaderWidth(GC gc) {
        int x = leftMargin;
        final IGridLabelProvider labelProvider = grid.getLabelProvider();
        Image image = labelProvider.getImage(this);
        if (image != null) {
            x += image.getBounds().width + imageSpacing;
        }
        {
            int textWidth;
            if (Boolean.TRUE.equals(labelProvider.getGridOption(IGridLabelProvider.OPTION_EXCLUDE_COLUMN_NAME_FOR_WIDTH_CALC))) {
                textWidth = gc.stringExtent("X").x;
            } else {
                String text = labelProvider.getText(this);
                String description = labelProvider.getDescription(this);
                textWidth = gc.stringExtent(text).x;
                if (!CommonUtils.isEmpty(description)) {
                    int descWidth = gc.stringExtent(description).x;
                    if (descWidth > textWidth) {
                        textWidth = descWidth;
                    }
                }
            }
            x += textWidth + rightMargin;
        }
        if (isSortable()) {
            x += rightMargin + GridColumnRenderer.getSortControlBounds().width + GridColumnRenderer.IMAGE_SPACING;
        }

        x += GridColumnRenderer.getFilterControlBounds().width;

        if (!CommonUtils.isEmpty(children)) {
            int childWidth = 0;
            for (GridColumn child : children) {
                childWidth += child.computeHeaderWidth(gc);
            }
            return Math.max(x, childWidth);
        }

        return x;
    }

    public boolean isSortable() {
        return grid.getContentProvider().getSortOrder(this) != SWT.NONE;
    }

    public boolean isFilterable() {
        return grid.getContentProvider().isElementSupportsFilter(this);
    }

    /**
     * Causes the receiver to be resized to its preferred size.
     */
    void pack(GC gc, boolean reflect) {
        int newWidth = computeHeaderWidth(gc);
        if (CommonUtils.isEmpty(children)) {
            // Calculate width of visible cells
            int topIndex = grid.getTopIndex();
            int bottomIndex = grid.getBottomIndex();
            int maxValueWidth = 0;
            if (topIndex >= 0 && bottomIndex >= topIndex) {
                int itemCount = grid.getItemCount();
                for (int i = topIndex; i <= bottomIndex && i < itemCount; i++) {
                    maxValueWidth = Math.max(maxValueWidth, computeCellWidth(gc, grid.getRow(i)));
                    newWidth = Math.max(newWidth, maxValueWidth);
                }
            }
            // Respect hints
            int columnHintsWidth = grid.getContentProvider().getColumnHintsWidth(this);
            if (columnHintsWidth > 0) {
                if (columnHintsWidth > 16) columnHintsWidth= 16;
                int newValueWidth = maxValueWidth + columnHintsWidth * gc.stringExtent("x").x;
                if (newValueWidth > newWidth) {
                    newWidth = newValueWidth;
                }
            }

        } else {
            int childrenWidth = 0;
            for (GridColumn child : children) {
                child.pack(gc, reflect);
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

    private int computeCellWidth(GC gc, IGridRow row) {
        int x = 0;

        x += leftMargin;

        IGridContentProvider.CellInformation cellInfo = grid.getContentProvider().getCellInfo(
            this, row, false);

        String cellText = grid.getCellText(cellInfo.text);
        int state = cellInfo.state;
        Rectangle imageBounds;
        if (GridCellRenderer.isLinkState(state)) {
            imageBounds = GridCellRenderer.LINK_IMAGE_BOUNDS;
        } else {
            DBPImage image = cellInfo.image;
            imageBounds = image == null ? null : DBeaverIcons.getImage(image).getBounds();
        }
        if (imageBounds != null) {
            x += imageBounds.width + insideMargin;
        }

        x += gc.textExtent(cellText).x + rightMargin;
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
        String tip = grid.getLabelProvider().getToolTipText(this);
        if (tip == null) {
            tip = grid.getLabelProvider().getText(this);
        }
        return tip;
    }

    @Override
    public GridColumn getParent() {
        return parent;
    }

    @Override
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

    @Override
    public String toString() {
        return CommonUtils.toString(element);
    }

}