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
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.lightgrid.renderers.*;
import org.jkiss.dbeaver.ui.controls.lightgrid.scroll.IGridScrollBar;
import org.jkiss.dbeaver.ui.controls.lightgrid.scroll.NullScrollBar;
import org.jkiss.dbeaver.ui.controls.lightgrid.scroll.ScrollBarAdapter;
import org.jkiss.utils.IntKeyMap;

import java.util.*;
import java.util.List;

/**
 * LightGrid
 * initially based on Nebula grid. Refactored and mostly redone.
 *
 * @author serge@jkiss.org
 * @author chris.gross@us.ibm.com
 */
public abstract class LightGrid extends Canvas {

    public static final int MAX_TOOLTIP_LENGTH = 1000;

    public static final int Event_ChangeSort = 1000;

    /**
     * Horizontal scrolling increment, in pixels.
     */
    private static final int HORZ_SCROLL_INCREMENT = 5;

    /**
     * The area to the left and right of the column boundary/resizer that is
     * still considered the resizer area. This prevents a user from having to be
     * *exactly* over the resizer.
     */
    private static final int COLUMN_RESIZER_THRESHOLD = 4;

    /**
     * The minimum width of a column header.
     */
    private static final int MIN_COLUMN_HEADER_WIDTH = 20;

    public enum EventSource {
        MOUSE,
        KEYBOARD,
    }

    /**
     * Tracks whether the scroll values are correct. If not they will be
     * recomputed in onPaint. This allows us to get a free ride on top of the
     * OS's paint event merging to assure that we don't perform this expensive
     * operation when unnecessary.
     */
    private boolean scrollValuesObsolete = false;

    /**
     * Reference to the item in focus.
     */
    private int focusItem = -1;

    private final Set<GridPos> selectedCells = new TreeSet<GridPos>(new CellComparator());
    private final Set<GridPos> selectedCellsBeforeRangeSelect = new TreeSet<GridPos>(new CellComparator());
    private final List<GridColumn> selectedColumns = new ArrayList<GridColumn>();
    private final IntKeyMap<Boolean> selectedRows = new IntKeyMap<Boolean>();

    private boolean cellDragSelectionOccurring = false;
    private boolean cellRowDragSelectionOccurring = false;
    private boolean cellColumnDragSelectionOccurring = false;
    private boolean cellDragCTRL = false;
    private boolean followupCellSelectionEventOwed = false;

    private boolean cellSelectedOnLastMouseDown;
    private boolean cellRowSelectedOnLastMouseDown;
    private boolean cellColumnSelectedOnLastMouseDown;

    private GridColumn shiftSelectionAnchorColumn;

    private GridColumn focusColumn;
    private final GridPos focusCell = new GridPos(-1, -1);

    /**
     * List of table columns in creation/index order.
     */
    private final List<GridColumn> columns = new ArrayList<GridColumn>();
    private Object[] columnElements = new Object[0];
    private Object[] rowElements = new Object[0];

    private int maxColumnDefWidth = 1000;

    private IGridRenderer columnHeaderRenderer;
    private IGridRenderer rowHeaderRenderer;

    /**
     * Are row headers visible?
     */
    private boolean rowHeaderVisible = false;

    /**
     * Are column headers visible?
     */
    private boolean columnHeadersVisible = false;

    /**
     * Type of selection behavior. Valid values are SWT.SINGLE and SWT.MULTI.
     */
    private int selectionType = SWT.SINGLE;

    /**
     * Default height of items.
     */
    private int itemHeight = 1;

    /**
     * Width of each row header.
     */
    private int rowHeaderWidth = 0;

    /**
     * Height of each column header.
     */
    private int headerHeight = 0;

    /**
     * True if mouse is hover on a column boundary and can resize the column.
     */
    boolean hoveringOnColumnSorter = false;

    private GridColumn columnBeingSorted;

    /**
     * True if mouse is hover on a column boundary and can resize the column.
     */
    boolean hoveringOnColumnResizer = false;

    /**
     * Reference to the column being resized.
     */
    private GridColumn columnBeingResized;

    /**
     * Is the user currently resizing a column?
     */
    private boolean resizingColumn = false;

    /**
     * The mouse X position when the user starts the resize.
     */
    private int resizingStartX = 0;

    /**
     * The width of the column when the user starts the resize. This, together
     * with the resizingStartX determines the current width during resize.
     */
    private int resizingColumnStartWidth = 0;

    /**
     * Reference to the currently item that the mouse is currently hovering
     * over.
     */
    private int hoveringItem;

    /**
     * Reference to the column that the mouse is currently hovering over.
     * Includes the header and all cells (all rows) in this column.
     */
    private GridColumn hoveringColumn;

    private GridColumn hoveringColumnHeader;

    /**
     * String-based detail of what is being hovered over in a cell. This allows
     * a renderer to differentiate between hovering over different parts of the
     * cell. For example, hovering over a checkbox in the cell or hovering over
     * a tree node in the cell. The table does nothing with this string except
     * to set it back in the renderer when its painted. The renderer sets this
     * during its notify method (InternalWidget.HOVER) and the table pulls it
     * back and maintains it so it can be set back when the cell is painted. The
     * renderer determines what the hover detail means and how it affects
     * painting.
     */
    private String hoveringDetail = "";

    /**
     * Are the grid lines visible?
     */
    private boolean linesVisible = true;

    /**
     * Grid line color.
     */
    private Color lineColor;

    /**
     * Vertical scrollbar proxy.
     * <p/>
     * Note:
     * <ul>
     * <li>{@link LightGrid#getTopIndex()} is the only method allowed to call vScroll.getSelection()
     * (except #updateScrollbars() of course)</li>
     * <li>{@link LightGrid#setTopIndex(int)} is the only method allowed to call vScroll.setSelection(int)</li>
     * </ul>
     */
    private IGridScrollBar vScroll;

    /**
     * Horizontal scrollbar proxy.
     */
    private IGridScrollBar hScroll;

    /**
     * Item selected when a multiple selection using shift+click first occurs.
     * This item anchors all further shift+click selections.
     */
    private int shiftSelectionAnchorItem;

    private boolean columnScrolling = false;

    private Color cellHeaderSelectionBackground;
    private Color cellHeaderSelectionForeground;

    /**
     * Dispose listener.  This listener is removed during the dispose event to allow re-firing of
     * the event.
     */
    private Listener disposeListener;

    public GC sizingGC;

    private Color backgroundColor;

    /**
     * True if the widget is being disposed.  When true, events are not fired.
     */
    private boolean disposing = false;

    /**
     * Index of first visible item.  The value must never be read directly.  It is cached and
     * updated when appropriate.  #getTopIndex should be called for every client (even internal
     * callers).  A value of -1 indicates that the value is old and will be recomputed.
     *
     * @see #bottomIndex
     */
    private int topIndex = -1;
    /**
     * Index of last visible item.  The value must never be read directly.  It is cached and
     * updated when appropriate.  #getBottomIndex() should be called for every client (even internal
     * callers).  A value of -1 indicates that the value is old and will be recomputed.
     * <p/>
     * Note that the item with this index is often only partly visible; maybe only
     * a single line of pixels is visible. In extreme cases, bottomIndex may be the
     * same as topIndex.
     *
     * @see #topIndex
     */
    private int bottomIndex = -1;

    /**
     * Index of the first visible column. A value of -1 indicates that the value is old and will be recomputed.
     */
    private int startColumnIndex = -1;

    /**
     * Index of the the last visible column. A value of -1 indicates that the value is old and will be recomputed.
     */
    private int endColumnIndex = -1;

    /**
     * True if the last visible item is completely visible.  The value must never be read directly.  It is cached and
     * updated when appropriate.  #isShown() should be called for every client (even internal
     * callers).
     *
     * @see #bottomIndex
     */
    private boolean bottomIndexShownCompletely = false;

    /**
     * Tooltip text - overriden because we have cell specific tooltips
     */
    private String toolTipText = null;

    /**
     * This is the tooltip text currently used.  This could be the tooltip text for the currently
     * hovered cell, or the general grid tooltip.  See handleCellHover.
     */
    private String displayedToolTipText;

    /**
     * Threshold for the selection border used for drag n drop
     * in mode.
     */
    private static final int SELECTION_DRAG_BORDER_THRESHOLD = 2;

    private boolean hoveringOnSelectionDragArea = false;

    /**
     * A range of rows in a <code>Grid</code>.
     * <p/>
     * A row in this sense exists only for visible items
     * Therefore, the items at 'startIndex' and 'endIndex'
     * are always visible.
     *
     * @see LightGrid#getRowRange(int, int, boolean, boolean)
     */
    private static class RowRange {
        /**
         * index of first item in range
         */
        public int startIndex;
        /**
         * index of last item in range
         */
        public int endIndex;
        /**
         * number of rows (i.e. <em>visible</em> items) in this range
         */
        public int rows;
        /**
         * height in pixels of this range (including horizontal separator between rows)
         */
        public int height;
    }

    /**
     * Filters out unnecessary styles, adds mandatory styles and generally
     * manages the style to pass to the super class.
     *
     * @param style user specified style.
     * @return style to pass to the super class.
     */
    private static int checkStyle(int style)
    {
        int mask = SWT.BORDER | SWT.LEFT_TO_RIGHT | SWT.RIGHT_TO_LEFT | SWT.H_SCROLL | SWT.V_SCROLL
            | SWT.SINGLE | SWT.MULTI | SWT.NO_FOCUS | SWT.CHECK | SWT.VIRTUAL;
        int newStyle = style & mask;
        newStyle |= SWT.DOUBLE_BUFFERED;
        return newStyle;
    }

    /**
     * Constructs a new instance of this class given its parent and a style
     * value describing its behavior and appearance.
     * <p/>
     *
     * @param parent a composite control which will be the parent of the new
     *               instance (cannot be null)
     * @param style  the style of control to construct
     * @see SWT#SINGLE
     * @see SWT#MULTI
     */
    public LightGrid(Composite parent, int style)
    {
        super(parent, checkStyle(style));

        sizingGC = new GC(this);
        columnHeaderRenderer = new GridColumnRenderer(this);
        rowHeaderRenderer = new GridRowRenderer(this);

        setForeground(getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND));
        setLineColor(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        backgroundColor = getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND);

        if ((style & SWT.MULTI) != 0) {
            selectionType = SWT.MULTI;
        }

        if (getVerticalBar() != null) {
            getVerticalBar().setVisible(false);
            vScroll = new ScrollBarAdapter(getVerticalBar());
        } else {
            vScroll = new NullScrollBar();
        }

        if (getHorizontalBar() != null) {
            getHorizontalBar().setVisible(false);
            hScroll = new ScrollBarAdapter(getHorizontalBar());
        } else {
            hScroll = new NullScrollBar();
        }

        scrollValuesObsolete = true;

        initListeners();

        recalculateSizes();

        RGB cellSel = blend(
            getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION).getRGB(),
            new RGB(255, 255, 255),
            50);

        cellHeaderSelectionBackground = new Color(getDisplay(), cellSel);// = getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION);
        cellHeaderSelectionForeground = getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT);


        setDragDetect(false);
    }

    @NotNull
    public abstract IGridContentProvider getContentProvider();

    @NotNull
    public abstract IGridLabelProvider getColumnLabelProvider();

    @NotNull
    public abstract IGridLabelProvider getRowLabelProvider();

    public int getMaxColumnDefWidth() {
        return maxColumnDefWidth;
    }

    public void setMaxColumnDefWidth(int maxColumnDefWidth) {
        this.maxColumnDefWidth = maxColumnDefWidth;
    }

    /**
     * Refresh grid data
     */
    public void refreshData(boolean clearData)
    {
        if (clearData) {
            this.removeAll();
        }
        IGridContentProvider contentProvider = getContentProvider();
        this.columnElements = contentProvider.getElements(true);
        this.rowElements = contentProvider.getElements(false);

        if (clearData) {
            this.topIndex = -1;
            this.bottomIndex = -1;
            this.startColumnIndex = -1;
            this.endColumnIndex = -1;

            // Add columns
            for (Integer i = 0; i < columnElements.length; i++) {
                GridColumn column = new GridColumn(this);
                IGridLabelProvider labelProvider = getColumnLabelProvider();
                column.setText(labelProvider.getText(columnElements[i]));
                column.setImage(labelProvider.getImage(columnElements[i]));
                column.setHeaderTooltip(labelProvider.getTooltip(columnElements[i]));
                column.setSort(contentProvider.getColumnSortOrder(i));
            }

            if (getColumnCount() == 1) {
                // Here we going to maximize single column to entire grid's width
                // Sometimes (when new grid created and filled with data very fast our client area size is zero
                // So let's add a workaround for it and use column's width in this case
                GridColumn column = getColumn(0);
                int columnWidth = column.computeHeaderWidth();
                int gridWidth = getSize().x - getRowHeaderWidth() - getHScrollSelectionInPixels() - getVerticalBar().getSize().x;
                if (gridWidth > columnWidth) {
                    columnWidth = gridWidth;
                }
                column.setWidth(columnWidth);
            } else {
                int totalWidth = 0;
                for (GridColumn curColumn : columns) {
                    curColumn.pack();
                    totalWidth += curColumn.getWidth();
                }
                // If grid width more than screen - lets narrow too long columns
                int clientWidth = getClientArea().width;
                if (totalWidth > clientWidth) {
                    int normalWidth = 0;
                    List<GridColumn> fatColumns = new ArrayList<GridColumn>();
                    for (GridColumn curColumn : columns) {
                        if (curColumn.getWidth() > maxColumnDefWidth) {
                            fatColumns.add(curColumn);
                        } else {
                            normalWidth += curColumn.getWidth();
                        }
                    }
                    if (!fatColumns.isEmpty()) {
                        // Narrow fat columns on decWidth
                        int freeSpace = (clientWidth - normalWidth - getBorderWidth() - rowHeaderWidth - (vScroll.getControl() == null ? 0 : vScroll.getControl().getSize().x))
                            / fatColumns.size();
                        int newFatWidth = (freeSpace > maxColumnDefWidth ? freeSpace : maxColumnDefWidth);
                        for (GridColumn curColumn : fatColumns) {
                            curColumn.setWidth(newFatWidth);
                        }
                    }
                }
            }
        }

        updateScrollbars();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Color getBackground()
    {
        return backgroundColor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setBackground(Color color)
    {
        backgroundColor = color;
        redraw();
    }

    /**
     * Returns the background color of column and row headers when a cell in
     * the row or header is selected.
     *
     * @return cell header selection background color
     */
    public Color getCellHeaderSelectionBackground()
    {
        return cellHeaderSelectionBackground;
    }

    public Color getCellHeaderSelectionForeground()
    {
        return cellHeaderSelectionForeground;
    }

    /**
     * Adds the listener to the collection of listeners who will be notified
     * when the receiver's selection changes, by sending it one of the messages
     * defined in the {@code SelectionListener} interface.
     * <p/>
     * Cell selection events may have <code>Event.detail = SWT.DRAG</code> when the
     * user is drag selecting multiple cells.  A follow up selection event will be generated
     * when the drag is complete.
     *
     * @param listener the listener which should be notified
     */
    public void addSelectionListener(SelectionListener listener)
    {
        checkWidget();
        if (listener == null) {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
        }
        addListener(SWT.Selection, new TypedListener(listener));
        addListener(SWT.DefaultSelection, new TypedListener(listener));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Point computeSize(int wHint, int hHint, boolean changed)
    {
        checkWidget();

        Point prefSize = null;
        if (wHint == SWT.DEFAULT || hHint == SWT.DEFAULT) {
            prefSize = getTableSize();
            prefSize.x += 2 * getBorderWidth();
            prefSize.y += 2 * getBorderWidth();
        }

        int x = 0;
        int y = 0;

        if (wHint == SWT.DEFAULT) {
            x += prefSize.x;
            if (getVerticalBar() != null) {
                x += getVerticalBar().getSize().x;
            }
        } else {
            x = wHint;
        }

        if (hHint == SWT.DEFAULT) {
            y += prefSize.y;
            if (getHorizontalBar() != null) {
                y += getHorizontalBar().getSize().y;
            }
        } else {
            y = hHint;
        }

        return new Point(x, y);
    }

    /**
     * Deselects the item at the given zero-relative index in the receiver. If
     * the item at the index was already deselected, it remains deselected.
     * Indices that are out of range are ignored.
     * <p/>
     * If cell selection is enabled, all cells in the specified item are deselected.
     *
     * @param index the index of the item to deselect
     */
    public void deselect(int index)
    {
        checkWidget();

        if (index < 0 || index > getItemCount() - 1) {
            return;
        }

        deselectCells(getCells(index));

        redraw();
    }

    /**
     * Deselects the items at the given zero-relative indices in the receiver.
     * If the item at the given zero-relative index in the receiver is selected,
     * it is deselected. If the item at the index was not selected, it remains
     * deselected. The range of the indices is inclusive. Indices that are out
     * of range are ignored.
     * <p/>
     * If cell selection is enabled, all cells in the given range are deselected.
     *
     * @param start the start index of the items to deselect
     * @param end   the end index of the items to deselect
     */
    public void deselect(int start, int end)
    {
        checkWidget();

        for (int i = start; i <= end; i++) {
            if (i < 0) {
                continue;
            }
            if (i > getItemCount() - 1) {
                break;
            }
            deselectCells(getCells(i));
        }
        redraw();
    }

    /**
     * Deselects the items at the given zero-relative indices in the receiver.
     * If the item at the given zero-relative index in the receiver is selected,
     * it is deselected. If the item at the index was not selected, it remains
     * deselected. Indices that are out of range and duplicate indices are
     * ignored.
     * <p/>
     * If cell selection is enabled, all cells in the given items are deselected.
     *
     * @param indices the array of indices for the items to deselect
     */
    public void deselect(int[] indices)
    {
        checkWidget();
        if (indices == null) {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
            return;
        }

        for (int j : indices) {
            if (j >= 0 && j < getItemCount()) {
                deselectCells(getCells(j));
            }
        }
        redraw();
    }

    /**
     * Deselects all selected items in the receiver.  If cell selection is enabled,
     * all cells are deselected.
     */
    public void deselectAll()
    {
        checkWidget();

        deselectAllCells();
    }

    /**
     * Returns the column at the given, zero-relative index in the receiver.
     * Throws an exception if the index is out of range. If no
     * {@code GridColumn}s were created by the programmer, this method will
     * throw {@code ERROR_INVALID_RANGE} despite the fact that a single column
     * of data may be visible in the table. This occurs when the programmer uses
     * the table like a list, adding items but never creating a column.
     *
     * @param index the index of the column to return
     * @return the column at the given index
     */
    public GridColumn getColumn(int index)
    {
        checkWidget();

        if (index < 0 || index > getColumnCount() - 1) {
            SWT.error(SWT.ERROR_INVALID_RANGE);
        }

        return columns.get(index);
    }

    /**
     * Returns the column at the given point and a known item in the receiver or null if no such
     * column exists. The point is in the coordinate system of the receiver.
     *
     * @param point the point used to locate the column
     * @return the column at the given point
     */
    @Nullable
    public GridColumn getColumn(Point point)
    {
        checkWidget();
        if (point == null) {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
            return null;
        }

        GridColumn overThis = null;

        int x2 = 0;

        if (rowHeaderVisible) {
            if (point.x <= rowHeaderWidth) {
                return null;
            }

            x2 += rowHeaderWidth;
        }

        x2 -= getHScrollSelectionInPixels();

        for (GridColumn column : columns) {
            if (point.x >= x2 && point.x < x2 + column.getWidth()) {
                overThis = column;
                break;
            }

            x2 += column.getWidth();
        }

        if (overThis == null) {
            return null;
        }

        return overThis;
    }

    /**
     * Returns the number of columns contained in the receiver. If no
     * {@code GridColumn}s were created by the programmer, this value is
     * zero, despite the fact that visually, one column of items may be visible.
     * This occurs when the programmer uses the table like a list, adding items
     * but never creating a column.
     *
     * @return the number of columns
     */
    public int getColumnCount()
    {
        checkWidget();
        return columns.size();
    }

    public Collection<GridColumn> getColumns()
    {
        checkWidget();
        return columns;
    }

    /**
     * Returns the externally managed horizontal scrollbar.
     *
     * @return the external horizontal scrollbar.
     * @see #setHorizontalScrollBarProxy( org.jkiss.dbeaver.ui.controls.lightgrid.scroll.IGridScrollBar)
     */
    protected IGridScrollBar getHorizontalScrollBarProxy()
    {
        checkWidget();
        return hScroll;
    }

    /**
     * Returns the externally managed vertical scrollbar.
     *
     * @return the external vertical scrollbar.
     * @see #setVerticalScrollBarProxy( org.jkiss.dbeaver.ui.controls.lightgrid.scroll.IGridScrollBar)
     */
    protected IGridScrollBar getVerticalScrollBarProxy()
    {
        checkWidget();
        return vScroll;
    }

    /**
     * Returns the height of the column headers. If this table has column
     * groups, the returned value includes the height of group headers.
     *
     * @return height of the column header row
     */
    public int getHeaderHeight()
    {
        checkWidget();
        return headerHeight;
    }

    /**
     * Returns {@code true} if the receiver's header is visible, and
     * {@code false} otherwise.
     *
     * @return the receiver's header's visibility state
     */
    public boolean getHeaderVisible()
    {
        checkWidget();
        return columnHeadersVisible;
    }

    public int getRow(Point point)
    {
        checkWidget();

        if (point == null) {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
            return -1;
        }

        if (point.x < 0 || point.x > getClientArea().width) return -1;

        Point p = new Point(point.x, point.y);

        int y2 = 0;

        if (columnHeadersVisible) {
            if (p.y <= headerHeight) {
                return -1;
            }
            y2 += headerHeight;
        }

        int row = getTopIndex();
        int currItemHeight = getItemHeight();

        int itemCount = getItemCount();
        while (row < itemCount && y2 <= getClientArea().height) {
            if (p.y >= y2 && p.y < y2 + currItemHeight + 1) {
                return row;
            }

            y2 += currItemHeight + 1;

            row++;
        }

        return -1;
    }

    /**
     * Returns the number of items contained in the receiver.
     *
     * @return the number of items
     */
    public int getItemCount()
    {
        return rowElements.length;
    }

    /**
     * Returns the default height of the items
     *
     * @return default height of items
     * @see #setItemHeight(int)
     */
    public int getItemHeight()
    {
        return itemHeight;
    }

    /**
     * Sets the default height for this <code>Grid</code>'s items.  When
     * this method is called, all existing items are resized
     * to the specified height and items created afterwards will be
     * initially sized to this height.
     * <p/>
     * As long as no default height was set by the client through this method,
     * the preferred height of the first item in this <code>Grid</code> is
     * used as a default for all items (and is returned by {@link #getItemHeight()}).
     *
     * @param height default height in pixels
     */
    public void setItemHeight(int height)
    {
        checkWidget();
        if (height < 1)
            SWT.error(SWT.ERROR_INVALID_ARGUMENT);
        itemHeight = height;
        setScrollValuesObsolete();
        redraw();
    }

    /**
     * Returns the line color.
     *
     * @return Returns the lineColor.
     */
    public Color getLineColor()
    {
        checkWidget();
        return lineColor;
    }

    /**
     * Returns true if the lines are visible.
     *
     * @return Returns the linesVisible.
     */
    public boolean getLinesVisible()
    {
        checkWidget();
        return linesVisible;
    }

    /**
     * Returns the next visible item in the table.
     *
     * @return next visible item or null
     */
    public int getNextVisibleItem(int index)
    {
        if (index >= getItemCount()) {
            return -1;
        }
        if (index == getItemCount() - 1) {
            return index;
        } else {
            return index + 1;
        }
    }

    /**
     * Returns the previous visible item in the table. Passing null for the item
     * will return the last visible item in the table.
     *
     * @return previous visible item or if item==null last visible item
     */
    public int getPreviousVisibleItem(int index)
    {
        if (index == 0) {
            return -1;
        }
        return index - 1;
    }

    /**
     * Returns the previous visible column in the table.
     *
     * @param column column
     * @return previous visible column or null
     */
    @Nullable
    public GridColumn getPreviousVisibleColumn(GridColumn column)
    {
        checkWidget();

        int index = columns.indexOf(column);

        if (index == 0)
            return null;

        return columns.get(index - 1);
    }

    /**
     * Returns the next visible column in the table.
     *
     * @param column column
     * @return next visible column or null
     */
    @Nullable
    public GridColumn getNextVisibleColumn(GridColumn column)
    {
        checkWidget();

        int index = columns.indexOf(column);

        if (index == columns.size() - 1)
            return null;

        return columns.get(index + 1);
    }

    /**
     * Returns the number of selected cells contained in the receiver.
     *
     * @return the number of selected cells
     */
    public int getCellSelectionCount()
    {
        checkWidget();
        return selectedCells.size();
    }

    /**
     * Returns the zero-relative index of the item which is currently selected
     * in the receiver, or -1 if no item is selected.  If cell selection is enabled,
     * returns the index of first item that contains at least one selected cell.
     *
     * @return the index of the selected item
     */
    public int getSelectionIndex()
    {
        checkWidget();

        if (selectedCells.isEmpty())
            return -1;

        return selectedCells.iterator().next().row;
    }

    /**
     * Returns the zero-relative index of the item which is currently at the top
     * of the receiver. This index can change when items are scrolled or new
     * items are added or removed.
     *
     * @return the index of the top item
     */
    public int getTopIndex()
    {
        checkWidget();

        if (topIndex != -1)
            return topIndex;

        if (!vScroll.getVisible()) {
            topIndex = 0;
        } else {
            // figure out first visible row and last visible row
            topIndex = vScroll.getSelection();
        }

        return topIndex;
    }

    /**
     * Returns the zero-relative index of the item which is currently at the bottom
     * of the receiver. This index can change when items are scrolled, expanded
     * or collapsed or new items are added or removed.
     * <p/>
     * Note that the item with this index is often only partly visible; maybe only
     * a single line of pixels is visible. Use {@link #isShown(int)} to find
     * out.
     * <p/>
     * In extreme cases, getBottomIndex() may return the same value as
     * {@link #getTopIndex()}.
     *
     * @return the index of the bottom item
     */
    public int getBottomIndex()
    {
        checkWidget();

        if (bottomIndex != -1)
            return bottomIndex;

        if (getItemCount() == 0) {
            bottomIndex = 0;
        } else if (getVisibleGridHeight() < 1) {
            bottomIndex = getTopIndex();
        } else {
            RowRange range = getRowRange(getTopIndex(), getVisibleGridHeight(), false, false);

            bottomIndex = range.endIndex;
            bottomIndexShownCompletely = range.height <= getVisibleGridHeight();
        }

        return bottomIndex;
    }

    /**
     * Returns a {@link RowRange} ranging from
     * the grid item at startIndex to that at endIndex.
     * <p/>
     * This is primarily used to measure the height
     * in pixel of such a range and to count the number
     * of visible grid items within the range.
     *
     * @param startIndex index of the first item in the range or -1 to the first visible item in this grid
     * @param endIndex   index of the last item in the range or -1 to use the last visible item in this grid
     * @return
     */
    @Nullable
    private RowRange getRowRange(int startIndex, int endIndex)
    {

        // parameter preparation
        if (startIndex == -1) {
            // search first visible item
            startIndex = 0;
            if (startIndex == getItemCount()) return null;
        }
        if (endIndex == -1) {
            // search last visible item
            endIndex = getItemCount() - 1;
            if (endIndex <= 0) return null;
        }

        // fail fast
        if (startIndex < 0 || endIndex < 0 || startIndex >= getItemCount() || endIndex >= getItemCount()
            || endIndex < startIndex)
            SWT.error(SWT.ERROR_INVALID_ARGUMENT);
        RowRange range = new RowRange();
        range.startIndex = startIndex;
        range.endIndex = endIndex;

        range.rows = range.endIndex - range.startIndex + 1;
        range.height = (getItemHeight() + 1) * range.rows - 1;

        return range;
    }

    /**
     * This method can be used to build a range of grid rows
     * that is allowed to span a certain height in pixels.
     * <p/>
     * It returns a {@link RowRange} that contains information
     * about the range, especially the index of the last
     * element in the range (or if inverse == true, then the
     * index of the first element).
     * <p/>
     * Note:  Even if 'forceEndCompletelyInside' is set to
     * true, the last item will not lie completely within
     * the availableHeight, if (height of item at startIndex < availableHeight).
     *
     * @param startIndex               index of the first (if inverse==false) or
     *                                 last (if inverse==true) item in the range
     * @param availableHeight          height in pixels
     * @param forceEndCompletelyInside if true, the last item in the range will lie completely
     *                                 within the availableHeight, otherwise it may lie partly outside this range
     * @param inverse                  if true, then the first item in the range will be searched, not the last
     * @return range of grid rows
     * @see RowRange
     */
    private RowRange getRowRange(int startIndex, int availableHeight,
                                 boolean forceEndCompletelyInside, boolean inverse)
    {
        // parameter preparation
        if (startIndex == -1) {
            if (!inverse) {
                // search first visible item
                startIndex = 0;
            } else {
                // search last visible item
                startIndex = getItemCount() - 1;
            }
        }

        RowRange range = new RowRange();

        if (startIndex < 0 || startIndex >= getItemCount()) {
            // something is broken
            range.startIndex = 0;
            range.endIndex = 0;
            range.height = 0;
            range.rows = 0;
            return range;
        }

        if (availableHeight <= 0) {
            // special case: empty range
            range.startIndex = startIndex;
            range.endIndex = startIndex;
            range.rows = 0;
            range.height = 0;
            return range;
        }

        int availableRows = (availableHeight + 1) / (getItemHeight() + 1);

        if (((getItemHeight() + 1) * range.rows - 1) + 1 < availableHeight) {
            // not all available space used yet
            // - so add another row if it need not be completely within availableHeight
            if (!forceEndCompletelyInside)
                availableRows++;
        }

        int otherIndex = startIndex + ((availableRows - 1) * (!inverse ? 1 : -1));
        if (otherIndex < 0) otherIndex = 0;
        if (otherIndex >= getItemCount()) otherIndex = getItemCount() - 1;

        range.startIndex = !inverse ? startIndex : otherIndex;
        range.endIndex = !inverse ? otherIndex : startIndex;
        range.rows = range.endIndex - range.startIndex + 1;
        range.height = (getItemHeight() + 1) * range.rows - 1;

        return range;
    }

    /**
     * Returns the height of the plain grid in pixels.
     * This does <em>not</em> include the height of the column headers.
     *
     * @return height of plain grid
     */
    int getGridHeight()
    {
        RowRange range = getRowRange(-1, -1);
        return range != null ? range.height : 0;
    }

    /**
     * Returns the height of the on-screen area that is available
     * for showing the grid's rows, i.e. the client area of the
     * scrollable minus the height of the column headers (if shown).
     *
     * @return height of visible grid in pixels
     */
    int getVisibleGridHeight()
    {
        return getClientArea().height - (columnHeadersVisible ? headerHeight : 0);
    }

    /**
     * Returns the height of the screen area that is available for showing the grid columns
     *
     * @return
     */
    int getVisibleGridWidth()
    {
        return getClientArea().width - (rowHeaderVisible ? rowHeaderWidth : 0);
    }

    /**
     * Searches the receiver's list starting at the first column (index 0) until
     * a column is found that is equal to the argument, and returns the index of
     * that column. If no column is found, returns -1.
     *
     * @param column the search column
     * @return the index of the column
     */
    int indexOf(GridColumn column)
    {
        checkWidget();

        if (column == null) {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
            return -1;
        }

        if (column.getParent() != this) return -1;

        return columns.indexOf(column);
    }

    /**
     * Returns {@code true} if the receiver's row header is visible, and
     * {@code false} otherwise.
     * <p/>
     *
     * @return the receiver's row header's visibility state
     */
    public boolean isRowHeaderVisible()
    {
        checkWidget();
        return rowHeaderVisible;
    }

    /**
     * Returns true if the given cell is selected.
     *
     * @param cell cell
     * @return true if the cell is selected.
     */
    public boolean isCellSelected(GridPos cell)
    {
        checkWidget();

        if (cell == null)
            SWT.error(SWT.ERROR_NULL_ARGUMENT);

        return selectedCells.contains(cell);
    }

    /**
     * Removes all of the items from the receiver.
     */
    public void removeAll()
    {
        checkWidget();
        deselectAll();

        focusItem = -1;
        focusColumn = null;
        topIndex = -1;
        bottomIndex = -1;

        List<GridColumn> columnsCopy = new ArrayList<GridColumn>(columns);
        for (GridColumn column : columnsCopy) {
            column.dispose();
        }
        redraw();
    }

    /**
     * Removes the listener from the collection of listeners who will be
     * notified when the receiver's selection changes.
     *
     * @param listener the listener which should no longer be notified
     * @see SelectionListener
     * @see #addSelectionListener(SelectionListener)
     */
    public void removeSelectionListener(SelectionListener listener)
    {
        checkWidget();
        removeListener(SWT.Selection, listener);
        removeListener(SWT.DefaultSelection, listener);
    }

    /**
     * Selects the item at the given zero-relative index in the receiver. If the
     * item at the index was already selected, it remains selected. Indices that
     * are out of range are ignored.
     * <p/>
     * If cell selection is enabled, selects all cells at the given index.
     *
     * @param index the index of the item to select
     */
    public void select(int index)
    {
        checkWidget();

        if (index < 0 || index >= getItemCount()) return;

        selectCells(getCells(index));

        redraw();
    }

    /**
     * Selects the items in the range specified by the given zero-relative
     * indices in the receiver. The range of indices is inclusive. The current
     * selection is not cleared before the new items are selected.
     * <p/>
     * If an item in the given range is not selected, it is selected. If an item
     * in the given range was already selected, it remains selected. Indices
     * that are out of range are ignored and no items will be selected if start
     * is greater than end. If the receiver is single-select and there is more
     * than one item in the given range, then all indices are ignored.
     * <p/>
     * If cell selection is enabled, all cells within the given range are selected.
     *
     * @param start the start of the range
     * @param end   the end of the range
     * @see LightGrid#setSelection(int,int)
     */
    public void select(int start, int end)
    {
        checkWidget();

        if (selectionType == SWT.SINGLE && start != end) return;

        for (int i = start; i <= end; i++) {
            if (i < 0) {
                continue;
            }
            if (i > getItemCount() - 1) {
                break;
            }

            selectCells(getCells(i));
        }

        redraw();
    }

    /**
     * Selects the items at the given zero-relative indices in the receiver. The
     * current selection is not cleared before the new items are selected.
     * <p/>
     * If the item at a given index is not selected, it is selected. If the item
     * at a given index was already selected, it remains selected. Indices that
     * are out of range and duplicate indices are ignored. If the receiver is
     * single-select and multiple indices are specified, then all indices are
     * ignored.
     * <p/>
     * If cell selection is enabled, all cells within the given indices are
     * selected.
     *
     * @param indices the array of indices for the items to select
     * @see LightGrid#setSelection(int[])
     */
    public void select(int[] indices)
    {
        checkWidget();

        if (indices == null) {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
            return;
        }

        if (selectionType == SWT.SINGLE && indices.length > 1) return;

        for (int j : indices) {
            if (j >= 0 && j < getItemCount()) {
                selectCells(getCells(j));
            }
        }
        redraw();
    }

    /**
     * Selects all of the items in the receiver.
     * <p/>
     * If the receiver is single-select, do nothing.  If cell selection is enabled,
     * all cells are selected.
     */
    public void selectAll()
    {
        checkWidget();

        if (selectionType == SWT.SINGLE) return;

        selectAllCells();
    }

    /**
     * Sets the external horizontal scrollbar. Allows the scrolling to be
     * managed externally from the table. This functionality is only intended
     * when SWT.H_SCROLL is not given.
     * <p/>
     * Using this feature, a ScrollBar could be instantiated outside the table,
     * wrapped in IScrollBar and thus be 'connected' to the table.
     *
     * @param scroll The horizontal scrollbar to set.
     */
    protected void setHorizontalScrollBarProxy(IGridScrollBar scroll)
    {
        checkWidget();
        if (getHorizontalBar() != null) {
            return;
        }
        hScroll = scroll;

        hScroll.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onScrollSelection();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });
    }

    /**
     * Sets the external vertical scrollbar. Allows the scrolling to be managed
     * externally from the table. This functionality is only intended when
     * SWT.V_SCROLL is not given.
     * <p/>
     * Using this feature, a ScrollBar could be instantiated outside the table,
     * wrapped in IScrollBar and thus be 'connected' to the table.
     *
     * @param scroll The vertical scrollbar to set.
     */
    protected void setVerticalScrollBarProxy(IGridScrollBar scroll)
    {
        checkWidget();
        if (getVerticalBar() != null) {
            return;
        }
        vScroll = scroll;

        vScroll.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                onScrollSelection();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });
    }

    /**
     * Marks the receiver's header as visible if the argument is {@code true},
     * and marks it invisible otherwise.
     *
     * @param show the new visibility state
     */
    public void setHeaderVisible(boolean show)
    {
        checkWidget();
        this.columnHeadersVisible = show;
        redraw();
    }

    /**
     * Sets the line color.
     *
     * @param lineColor The lineColor to set.
     */
    public void setLineColor(Color lineColor)
    {
        checkWidget();
        this.lineColor = lineColor;
    }

    /**
     * Sets the line visibility.
     *
     * @param linesVisible Te linesVisible to set.
     */
    public void setLinesVisible(boolean linesVisible)
    {
        checkWidget();
        this.linesVisible = linesVisible;
        redraw();
    }

    /**
     * Marks the receiver's row header as visible if the argument is
     * {@code true}, and marks it invisible otherwise. When row headers are
     * visible, horizontal scrolling is always done by column rather than by
     * pixel.
     *
     * @param show the new visibility state
     */
    public void setRowHeaderVisible(boolean show)
    {
        checkWidget();
        this.rowHeaderVisible = show;
        setColumnScrolling(true);

        redraw();
    }

    /**
     * Selects the item at the given zero-relative index in the receiver. The
     * current selection is first cleared, then the new item is selected.
     * <p/>
     * If cell selection is enabled, all cells within the item at the given index
     * are selected.
     *
     * @param index the index of the item to select
     */
    public void setSelection(int index)
    {
        checkWidget();

        if (index >= 0 && index < getItemCount()) {
            selectedCells.clear();
            selectCells(getCells(index));
        }
    }

    /**
     * Selects the items in the range specified by the given zero-relative
     * indices in the receiver. The range of indices is inclusive. The current
     * selection is cleared before the new items are selected.
     * <p/>
     * Indices that are out of range are ignored and no items will be selected
     * if start is greater than end. If the receiver is single-select and there
     * is more than one item in the given range, then all indices are ignored.
     * <p/>
     * If cell selection is enabled, all cells within the given range are selected.
     *
     * @param start the start index of the items to select
     * @param end   the end index of the items to select
     * @see LightGrid#deselectAll()
     * @see LightGrid#select(int,int)
     */
    public void setSelection(int start, int end)
    {
        checkWidget();

        if (selectionType == SWT.SINGLE && start != end) return;

        selectedCells.clear();

        for (int i = start; i <= end; i++) {
            if (i < 0) {
                continue;
            }
            if (i > getItemCount() - 1) {
                break;
            }

            selectCells(getCells(i));
        }
        redraw();
    }

    /**
     * Selects the items at the given zero-relative indices in the receiver. The
     * current selection is cleared before the new items are selected.
     * <p/>
     * Indices that are out of range and duplicate indices are ignored. If the
     * receiver is single-select and multiple indices are specified, then all
     * indices are ignored.
     * <p/>
     * If cell selection is enabled, all cells within the given indices are selected.
     *
     * @param indices the indices of the items to select
     * @see LightGrid#deselectAll()
     * @see LightGrid#select(int[])
     */
    public void setSelection(int[] indices)
    {
        checkWidget();

        if (selectionType == SWT.SINGLE && indices.length > 1) return;

        selectedCells.clear();

        for (int j : indices) {
            if (j < 0) {
                continue;
            }
            if (j > getItemCount() - 1) {
                break;
            }

            selectCells(getCells(j));
        }
        redraw();
    }

    /**
     * Sets the zero-relative index of the item which is currently at the top of
     * the receiver. This index can change when items are scrolled or new items
     * are added and removed.
     *
     * @param index the index of the top item
     */
    public void setTopIndex(int index)
    {
        checkWidget();
        if (index < 0 || index >= getItemCount()) {
            return;
        }

        if (!vScroll.getVisible()) {
            return;
        }

        vScroll.setSelection(index);
        topIndex = -1;
        bottomIndex = -1;
        redraw();
    }

    /**
     * Shows the column. If the column is already showing in the receiver, this
     * method simply returns. Otherwise, the columns are scrolled until the
     * column is visible.
     *
     */
    public void showColumn(int column)
    {
        GridColumn col = getColumn(column);
        if (col == null) {
            return;
        }
        showColumn(col);
    }

    private void showColumn(GridColumn col)
    {
        checkWidget();

        if (!hScroll.getVisible()) {
            return;
        }

        int x = getColumnHeaderXPosition(col);

        int firstVisibleX = 0;
        if (rowHeaderVisible) {
            firstVisibleX = rowHeaderWidth;
        }

        // if its visible just return
        if (x >= firstVisibleX
            && (x + col.getWidth()) <= (firstVisibleX + (getClientArea().width - firstVisibleX))) {
            return;
        }

        if (!getColumnScrolling()) {
            if (x < firstVisibleX) {
                hScroll.setSelection(getHScrollSelectionInPixels() - (firstVisibleX - x));
            } else {
                if (col.getWidth() > getClientArea().width - firstVisibleX) {
                    hScroll.setSelection(getHScrollSelectionInPixels() + (x - firstVisibleX));
                } else {
                    x -= getClientArea().width - firstVisibleX - col.getWidth();
                    hScroll.setSelection(getHScrollSelectionInPixels() + (x - firstVisibleX));
                }
            }
        } else {
            if (x < firstVisibleX || col.getWidth() > getClientArea().width - firstVisibleX) {
                int sel = columns.indexOf(col);
                hScroll.setSelection(sel);
            } else {
                int availableWidth = getClientArea().width - firstVisibleX - col.getWidth();

                GridColumn prevCol = getPreviousVisibleColumn(col);
                GridColumn currentScrollTo = col;

                while (true) {
                    if (prevCol == null || prevCol.getWidth() > availableWidth) {
                        int sel = columns.indexOf(currentScrollTo);
                        hScroll.setSelection(sel);
                        break;
                    } else {
                        availableWidth -= prevCol.getWidth();
                        currentScrollTo = prevCol;
                        prevCol = getPreviousVisibleColumn(prevCol);
                    }
                }
            }
        }

        redraw();
    }

    /**
     * Returns true if 'item' is currently being <em>completely</em>
     * shown in this <code>Grid</code>'s visible on-screen area.
     * <p/>
     * <p>Here, "completely" only refers to the item's height, not its
     * width. This means this method returns true also if some cells
     * are horizontally scrolled away.
     *
     * @param row
     * @return true if 'item' is shown
     */
    boolean isShown(int row)
    {
        checkWidget();

        if (row == -1)
            SWT.error(SWT.ERROR_INVALID_ARGUMENT);

        int firstVisibleIndex = getTopIndex();
        int lastVisibleIndex = getBottomIndex();

        return (row >= firstVisibleIndex && row < lastVisibleIndex)
            ||
            (row == lastVisibleIndex && bottomIndexShownCompletely);
    }

    /**
     * Shows the item. If the item is already showing in the receiver, this
     * method simply returns. Otherwise, the items are scrolled until the item
     * is visible.
     *
     * @param item the item to be shown
     */
    public void showItem(int item)
    {
        checkWidget();

        updateScrollbars();

        // if no items are visible on screen then abort
        if (getVisibleGridHeight() < 1) {
            return;
        }

        // if its visible just return
        if (isShown(item)) {
            return;
        }

        int newTopIndex = item;

        if (newTopIndex >= getBottomIndex()) {
            RowRange range = getRowRange(newTopIndex, getVisibleGridHeight(), true, true);    // note: inverse==true
            newTopIndex = range.startIndex;        // note: use startIndex because of inverse==true
        }

        setTopIndex(newTopIndex);
    }

    /**
     * Shows the selection. If the selection is already showing in the receiver,
     * this method simply returns. Otherwise, the items are scrolled until the
     * selection is visible.
     *
     */
    public void showSelection()
    {
        checkWidget();

        if (scrollValuesObsolete)
            updateScrollbars();

        if (selectedCells.isEmpty()) return;

        GridPos cell = selectedCells.iterator().next();
        showItem(cell.row);
        showColumn(cell.col);
    }

    /**
     * Computes and sets the height of the header row. This method will ask for
     * the preferred size of all the column headers and use the max.
     */
    private void computeHeaderHeight()
    {

        int colHeaderHeight = 0;
        for (GridColumn column : columns) {
            colHeaderHeight = Math.max(column.computeHeaderHeight(), colHeaderHeight);
        }

        headerHeight = colHeaderHeight;
    }

    private void computeItemHeight()
    {
        itemHeight = sizingGC.getFontMetrics().getHeight() + 3;
    }

    /**
     * Returns the x position of the given column. Takes into account scroll
     * position.
     *
     * @param column given column
     * @return x position
     */
    private int getColumnHeaderXPosition(GridColumn column)
    {
        int x = 0;

        x -= getHScrollSelectionInPixels();

        if (rowHeaderVisible) {
            x += rowHeaderWidth;
        }
        for (GridColumn column2 : columns) {
            if (column2 == column) {
                break;
            }

            x += column2.getWidth();
        }

        return x;
    }

    /**
     * Returns the hscroll selection in pixels. This method abstracts away the
     * differences between column by column scrolling and pixel based scrolling.
     *
     * @return the horizontal scroll selection in pixels
     */
    private int getHScrollSelectionInPixels()
    {
        int selection = hScroll.getSelection();
        if (columnScrolling) {
            int pixels = 0;
            for (int i = 0; i < selection && i < columns.size(); i++) {
                pixels += columns.get(i).getWidth();
            }
            selection = pixels;
        }
        return selection;
    }

    /**
     * Returns the size of the preferred size of the inner table.
     *
     * @return the preferred size of the table.
     */
    private Point getTableSize()
    {
        int x = 0;
        int y = 0;

        if (columnHeadersVisible) {
            y += headerHeight;
        }

        y += getGridHeight();

        if (rowHeaderVisible) {
            x += rowHeaderWidth;
        }

        for (GridColumn column : columns) {
            x += column.getWidth();
        }

        return new Point(x, y);
    }

    /**
     * Sets the new width of the column being resized and fires the appropriate
     * listeners.
     *
     * @param x mouse x
     */
    private void handleColumnResizerDragging(int x)
    {
        int newWidth = resizingColumnStartWidth + (x - resizingStartX);
        if (newWidth < MIN_COLUMN_HEADER_WIDTH) {
            newWidth = MIN_COLUMN_HEADER_WIDTH;
        }

        if (columnScrolling) {
            int maxWidth = getClientArea().width;
            if (rowHeaderVisible)
                maxWidth -= rowHeaderWidth;
            if (newWidth > maxWidth)
                newWidth = maxWidth;
        }

        if (newWidth == columnBeingResized.getWidth()) {
            return;
        }

        columnBeingResized.setWidth(newWidth, false);
        scrollValuesObsolete = true;

        Rectangle clientArea = getClientArea();
        redraw(clientArea.x, clientArea.y, clientArea.width, clientArea.height, false);

        columnBeingResized.fireResized();

        for (int index = columns.indexOf(columnBeingResized) + 1; index < columns.size(); index++) {
            GridColumn col = columns.get(index);
            col.fireMoved();
        }
    }

    /**
     * Determines if the mouse is hovering on a column resizer and changes the
     * pointer and sets field appropriately.
     * Also checks if mouse if hovering on a column sorter control.
     *
     * @param x mouse x
     * @param y mouse y
     */
    private void handleHoverOnColumnHeader(int x, int y)
    {
        boolean overSorter = false, overResizer = false;
        if (y <= headerHeight) {
            int x2 = 0;

            if (rowHeaderVisible) {
                x2 += rowHeaderWidth;
            }

            x2 -= getHScrollSelectionInPixels();

            for (GridColumn column : columns) {
                if (column.isOverSortArrow(x - x2)) {
                    overSorter = true;
                    columnBeingSorted = column;
                    break;
                }
                x2 += column.getWidth();
                if (x2 >= (x - COLUMN_RESIZER_THRESHOLD) && x2 <= (x + COLUMN_RESIZER_THRESHOLD)) {
                    if (column.getResizeable()) {
                        overResizer = true;
                        columnBeingResized = column;
                    }
                    break;
                }
            }
        } else if (x <= rowHeaderWidth) {
            // Hover in row header
            //System.out.println("HEY " + x + " " + y);
        }
        if (overSorter != hoveringOnColumnSorter) {
            if (overSorter) {
                setCursor(columnBeingSorted.getSortRenderer().getHoverCursor());
            } else {
                columnBeingSorted = null;
                setCursor(null);
            }
            hoveringOnColumnSorter = overSorter;
        }
        if (overResizer != hoveringOnColumnResizer) {
            if (overResizer) {
                setCursor(getDisplay().getSystemCursor(SWT.CURSOR_SIZEWE));
            } else {
                columnBeingResized = null;
                if (!hoveringOnColumnSorter) {
                    setCursor(null);
                }
            }
            hoveringOnColumnResizer = overResizer;
        }
    }

    /**
     * Returns the cell at the given point in the receiver or null if no such
     * cell exists. The point is in the coordinate system of the receiver.
     *
     * @param point the point used to locate the item
     * @return the cell at the given point
     */
    @Nullable
    public GridPos getCell(Point point)
    {
        checkWidget();

        if (point == null) {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
            return null;
        }

        if (point.x < 0 || point.x > getClientArea().width) return null;

        int item = getRow(point);
        GridColumn column = getColumn(point);

        if (item >= 0 && column != null) {
            return new GridPos(columns.indexOf(column), item);
        } else {
            return null;
        }
    }

    /**
     * Paints.
     *
     * @param e paint event
     */
    private void onPaint(PaintEvent e)
    {
        final GC gc = e.gc;
        gc.setBackground(getBackground());
        this.drawBackground(gc, 0, 0, getSize().x, getSize().y);

        if (scrollValuesObsolete) {
            updateScrollbars();
            scrollValuesObsolete = false;
        }

        int x;
        int y = 0;

        if (columnHeadersVisible) {
            paintHeader(gc);
            y += headerHeight;
        }

        int availableHeight = getClientArea().height - y;
        int visibleRows = availableHeight / getItemHeight() + 1;
        if (getItemCount() > 0 && availableHeight > 0) {
            RowRange range = getRowRange(getTopIndex(), availableHeight, false, false);
            if (range.height >= availableHeight)
                visibleRows = range.rows;
            else
                visibleRows = range.rows + (availableHeight - range.height) / getItemHeight() + 1;
        }

        int firstVisibleIndex = getTopIndex();

        int row = firstVisibleIndex;
        final int hScrollSelectionInPixels = getHScrollSelectionInPixels();
        final GridPos testPos = new GridPos(-1, -1);
        final Rectangle clipping = new Rectangle(-1, -1, -1, -1);

        for (int i = 0; i < visibleRows + (firstVisibleIndex - firstVisibleIndex); i++) {

            x = 0;

            x -= hScrollSelectionInPixels;

            // get the item to draw
            if (row >= 0 && row < getItemCount()) {
                boolean cellInRowSelected = selectedRows.containsKey(row);

                if (rowHeaderVisible) {

                    // row header is actually painted later
                    x += rowHeaderWidth;
                }

                // draw regular cells for each column
                boolean isGridInFocus = this.isFocusControl();
                for (int k = 0, columnsSize = columns.size(); k < columnsSize; k++) {
                    GridColumn column = columns.get(k);

                    int width = column.getWidth();

                    if (x + width >= 0 && x < getClientArea().width) {

                        final GridCellRenderer cellRenderer = column.getCellRenderer();
                        cellRenderer.setBounds(x, y, width, getItemHeight());
                        int cellInHeaderDelta = headerHeight - y;
                        if (cellInHeaderDelta > 0) {
                            clipping.x = x - 1;
                            clipping.y = y + cellInHeaderDelta;
                            clipping.width = width + 1;
                            clipping.height = getItemHeight() + 2 - cellInHeaderDelta;
                        } else {
                            clipping.x = x - 1;
                            clipping.y = y - 1;
                            clipping.width = width + 1;
                            clipping.height = getItemHeight() + 2;
                        }
                        gc.setClipping(clipping);

                        //column.getCellRenderer().setSelected(selectedItems.contains(item));
                        cellRenderer.setFocus(isGridInFocus);
                        cellRenderer.setRowFocus(focusItem == row);
                        cellRenderer.setCellFocus(
                            focusItem == row && focusColumn == column);

                        cellRenderer.setRowHover(hoveringItem == row);
                        cellRenderer.setColumnHover(hoveringColumn == column);

                        testPos.col = column.getIndex();
                        testPos.row = row;
                        if (selectedCells.contains(testPos)) {
                            cellRenderer.setCellSelected(true);
                            //cellInRowSelected = true;
                        } else {
                            cellRenderer.setCellSelected(false);
                        }

                        cellRenderer.setRow(row);
                        cellRenderer.paint(gc);

                        gc.setClipping((Rectangle) null);
                    }

                    x += column.getWidth();
                }

                if (x < getClientArea().width) {
                    drawEmptyCell(gc, new Rectangle(x, y, getClientArea().width - x + 1, getItemHeight()), false);
                }

                x = 0;

                if (rowHeaderVisible) {

                    rowHeaderRenderer.setSelected(cellInRowSelected);
                    if (y >= headerHeight) {
                        rowHeaderRenderer.setBounds(0, y, rowHeaderWidth, getItemHeight() + 1);
                        rowHeaderRenderer.setRow(row);
                        rowHeaderRenderer.paint(gc);
                    }
                    x += rowHeaderWidth;
                }

                y += getItemHeight() + 1;
            } else {

                if (rowHeaderVisible) {
                    //row header is actually painted later
                    x += rowHeaderWidth;
                }

                for (GridColumn column : columns) {
                    drawEmptyCell(gc, new Rectangle(x, y, column.getWidth(), getItemHeight()), false);
                    x += column.getWidth();
                }
                if (x < getClientArea().width) {
                    drawEmptyCell(gc, new Rectangle(x, y, getClientArea().width - x + 1, getItemHeight()), false);
                }

                x = 0;

                if (rowHeaderVisible) {
                    drawEmptyRowHeader(gc, new Rectangle(x, y, rowHeaderWidth, getItemHeight() + 1));
                    x += rowHeaderWidth;
                }

                y += getItemHeight() + 1;
            }

            row++;
        }
    }

    /**
     * Paints the header.
     *
     * @param gc gc from paint event
     */
    private void paintHeader(GC gc)
    {
        int x = 0;
        int y;

        x -= getHScrollSelectionInPixels();

        if (rowHeaderVisible) {
            // skip left corner
            x += rowHeaderWidth;
        }

        for (int i = 0, columnsSize = columns.size(); i < columnsSize; i++) {
            GridColumn column = columns.get(i);
            if (x > getClientArea().width)
                break;

            int height = headerHeight;
            y = 0;

            columnHeaderRenderer.setHover(hoveringColumnHeader == column);

            columnHeaderRenderer.setColumn(column.getIndex());
            columnHeaderRenderer.setElement(columnElements[i]);
            columnHeaderRenderer.setBounds(x, y, column.getWidth(), height);
            columnHeaderRenderer.setSelected(selectedColumns.contains(column));

            if (x + column.getWidth() >= 0) {
                columnHeaderRenderer.paint(gc);
            }

            x += column.getWidth();
        }

        if (x < getClientArea().width) {
            drawEmptyColumnHeader(gc, new Rectangle(x, 0, getClientArea().width - x, headerHeight));
        }

        x = 0;

        if (rowHeaderVisible) {
            // paint left corner
            drawTopLeftCell(gc, new Rectangle(0, 0, rowHeaderWidth, headerHeight));
            x += rowHeaderWidth;
        }
    }

    /**
     * Manages the state of the scrollbars when new items are added or the
     * bounds are changed.
     */
    private void updateScrollbars()
    {
        Point preferredSize = getTableSize();

        Rectangle clientArea = getClientArea();

        // First, figure out if the scrollbars should be visible and turn them
        // on right away
        // this will allow the computations further down to accommodate the
        // correct client
        // area

        // Turn the scrollbars on if necessary and do it all over again if
        // necessary. This ensures
        // that if a scrollbar is turned on/off, the other scrollbars
        // visibility may be affected (more
        // area may have been added/removed.
        for (int doublePass = 1; doublePass <= 2; doublePass++) {

            if (preferredSize.y > clientArea.height) {
                vScroll.setVisible(true);
            } else {
                vScroll.setVisible(false);
                vScroll.setValues(0, 0, 1, 1, 1, 1);
            }
            if (preferredSize.x > clientArea.width) {
                hScroll.setVisible(true);
            } else {
                hScroll.setVisible(false);
                hScroll.setValues(0, 0, 1, 1, 1, 1);
            }

            // get the clientArea again with the now visible/invisible
            // scrollbars
            clientArea = getClientArea();
        }

        // if the scrollbar is visible set its values
        if (vScroll.getVisible()) {
            int max = getItemCount();
            int thumb = (getVisibleGridHeight() + 1) / (getItemHeight() + 1);

            // if possible, remember selection, if selection is too large, just
            // make it the max you can
            int selection = Math.min(vScroll.getSelection(), max);

            vScroll.setValues(selection, 0, max, thumb, 1, thumb);
        }

        // if the scrollbar is visible set its values
        if (hScroll.getVisible()) {

            if (!columnScrolling) {
                // horizontal scrolling works pixel by pixel

                int hiddenArea = preferredSize.x - clientArea.width + 1;

                // if possible, remember selection, if selection is too large,
                // just
                // make it the max you can
                int selection = Math.min(hScroll.getSelection(), hiddenArea - 1);

                hScroll.setValues(selection, 0, hiddenArea + clientArea.width - 1, clientArea.width,
                                  HORZ_SCROLL_INCREMENT, clientArea.width);
            } else {
                // horizontal scrolling is column by column

                int hiddenArea = preferredSize.x - clientArea.width + 1;

                int max = 0;
                int i = 0;

                while (hiddenArea > 0 && i < getColumnCount()) {
                    GridColumn col = columns.get(i);

                    i++;

                    hiddenArea -= col.getWidth();
                    max++;
                }

                max++;

                // max should never be greater than the number of visible cols
                int visCols = columns.size();
                max = Math.min(visCols, max);

                // if possible, remember selection, if selection is too large,
                // just
                // make it the max you can
                int selection = Math.min(hScroll.getSelection(), max);

                hScroll.setValues(selection, 0, max, 1, 1, 1);
            }
        }

    }

    /**
     * Updates cell selection.
     *
     * @param newCell                    newly clicked, navigated to cell.
     * @param stateMask                  state mask during preceeding mouse or key event.
     * @param dragging                   true if the user is dragging.
     * @param reverseDuplicateSelections true if the user is reversing selection rather than adding to.
     * @return selection event that will need to be fired or null.
     */
    @Nullable
    public Event updateCellSelection(
        GridPos newCell,
        int stateMask,
        boolean dragging,
        boolean reverseDuplicateSelections,
        EventSource eventSource)
    {
        List<GridPos> v = new ArrayList<GridPos>();
        v.add(newCell);
        return updateCellSelection(v, stateMask, dragging, reverseDuplicateSelections, eventSource);
    }

    /**
     * Updates cell selection.
     *
     * @param newCells                    newly clicked, navigated to cells.
     * @param stateMask                  state mask during preceeding mouse or key event.
     * @param dragging                   true if the user is dragging.
     * @param reverseDuplicateSelections true if the user is reversing selection rather than adding to.
     * @return selection event that will need to be fired or null.
     */
    @Nullable
    private Event updateCellSelection(
        List<GridPos> newCells,
        int stateMask,
        boolean dragging,
        boolean reverseDuplicateSelections,
        EventSource eventSource)
    {
        boolean shift = (stateMask & SWT.MOD2) == SWT.MOD2;
        boolean ctrl = (stateMask & SWT.MOD1) == SWT.MOD1;
        if (eventSource == EventSource.KEYBOARD) {
            ctrl = false;
        }

        if (!shift) {
            shiftSelectionAnchorColumn = null;
            shiftSelectionAnchorItem = -1;
        }

        if (!shift && !ctrl) {
            if (newCells.equals(selectedCells)) return null;

            selectedCells.clear();
            for (GridPos newCell : newCells) {
                addToCellSelection(newCell);
            }

        } else if (shift) {

            GridPos newCell = newCells.get(0); //shift selection should only occur with one
            //cell, ignoring others

            if ((focusColumn == null) || (focusItem < 0)) {
                return null;
            }

            shiftSelectionAnchorColumn = getColumn(newCell.col);
            shiftSelectionAnchorItem = newCell.row;

            if (ctrl) {
                selectedCells.clear();
                selectedCells.addAll(selectedCellsBeforeRangeSelect);
            } else {
                selectedCells.clear();
            }


            GridColumn currentColumn = focusColumn;
            int currentItem = focusItem;

            GridColumn endColumn = getColumn(newCell.col);
            int endItem = newCell.row;

            Point newRange = getSelectionRange(currentItem, currentColumn, endItem, endColumn);

            currentColumn = getColumn(newRange.x);
            endColumn = getColumn(newRange.y);

            GridColumn startCol = currentColumn;

            if (currentItem > endItem) {
                int temp = currentItem;
                currentItem = endItem;
                endItem = temp;
            }

            boolean firstLoop = true;

            do {
                if (!firstLoop) {
                    currentItem++;
                }

                firstLoop = false;

                boolean firstLoop2 = true;

                currentColumn = startCol;

                do {
                    if (!firstLoop2) {
                        int index = columns.indexOf(currentColumn) + 1;

                        if (index < columns.size()) {
                            currentColumn = columns.get(index);
                        } else {
                            currentColumn = null;
                        }

                        if (currentColumn != null)
                            if (columns.indexOf(currentColumn) > columns.indexOf(endColumn))
                                currentColumn = null;
                    }

                    firstLoop2 = false;

                    if (currentColumn != null) {
                        GridPos cell = new GridPos(indexOf(currentColumn), currentItem);
                        addToCellSelection(cell);
                    }
                } while (currentColumn != endColumn && currentColumn != null);
            } while (currentItem != endItem);
        } else /*if (eventSource == EventSource.MOUSE)*/ {
            // Ctrl selection works only for mouse events
            boolean reverse = reverseDuplicateSelections;
            if (!selectedCells.containsAll(newCells))
                reverse = false;

            if (dragging) {
                selectedCells.clear();
                selectedCells.addAll(selectedCellsBeforeRangeSelect);
            }

            if (reverse) {
                selectedCells.removeAll(newCells);
            } else {
                for (GridPos newCell : newCells) {
                    addToCellSelection(newCell);
                }
            }
        }

        updateSelectionCache();

        Event e = new Event();
        if (dragging) {
            e.detail = SWT.DRAG;
            followupCellSelectionEventOwed = true;
        }

        Rectangle clientArea = getClientArea();
        redraw(clientArea.x, clientArea.y, clientArea.width, clientArea.height, false);

        return e;
    }

    private void addToCellSelection(GridPos newCell)
    {
        if (newCell.col < 0 || newCell.col >= columns.size())
            return;

        if (newCell.row < 0 || newCell.row >= getItemCount())
            return;

        if (getColumn(newCell.col).getCellSelectionEnabled()) {
            selectedCells.add(newCell);
        }
    }

    void updateSelectionCache()
    {
        //Update the list of which columns have all their cells selected
        selectedColumns.clear();
        selectedRows.clear();

        IntKeyMap<Boolean> columnIndices = new IntKeyMap<Boolean>();
        for (GridPos cell : selectedCells) {
            columnIndices.put(cell.col, Boolean.TRUE);
            selectedRows.put(cell.row, Boolean.TRUE);
        }
        for (Integer columnIndex : columnIndices.keySet()) {
            selectedColumns.add(getColumn(columnIndex));
        }
    }

    /**
     * Initialize all listeners.
     */
    private void initListeners()
    {
        disposeListener = new Listener() {
            @Override
            public void handleEvent(Event e)
            {
                onDispose(e);
            }
        };
        addListener(SWT.Dispose, disposeListener);

        addPaintListener(new PaintListener() {
            @Override
            public void paintControl(PaintEvent e)
            {
                onPaint(e);
            }
        });

        addListener(SWT.Resize, new Listener() {
            @Override
            public void handleEvent(Event e)
            {
                onResize();
            }
        });

        if (getVerticalBar() != null) {
            getVerticalBar().addListener(SWT.Selection, new Listener() {
                @Override
                public void handleEvent(Event e)
                {
                    onScrollSelection();
                }
            });
        }

        if (getHorizontalBar() != null) {
            getHorizontalBar().addListener(SWT.Selection, new Listener() {
                @Override
                public void handleEvent(Event e)
                {
                    onScrollSelection();
                }
            });
        }

        addListener(SWT.KeyDown, new Listener() {
            @Override
            public void handleEvent(Event e)
            {
                onKeyDown(e);
            }
        });

        addTraverseListener(new TraverseListener() {
            @Override
            public void keyTraversed(TraverseEvent e)
            {
                e.doit = true;
            }
        });

        addMouseListener(new MouseListener() {
            @Override
            public void mouseDoubleClick(MouseEvent e)
            {
                onMouseDoubleClick(e);
            }

            @Override
            public void mouseDown(MouseEvent e)
            {
                onMouseDown(e);
            }

            @Override
            public void mouseUp(MouseEvent e)
            {
                onMouseUp(e);
            }
        });

        addMouseMoveListener(new MouseMoveListener() {
            @Override
            public void mouseMove(MouseEvent e)
            {
                onMouseMove(e);
            }
        });

        addMouseTrackListener(new MouseTrackListener() {
            @Override
            public void mouseEnter(MouseEvent e)
            {
            }

            @Override
            public void mouseExit(MouseEvent e)
            {
                onMouseExit(e);
            }

            @Override
            public void mouseHover(MouseEvent e)
            {
            }
        });

        addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e)
            {
                onFocusIn();
                redraw();
            }

            @Override
            public void focusLost(FocusEvent e)
            {
                redraw();
            }
        });

        // Special code to reflect mouse wheel events if using an external
        // scroller
        addListener(SWT.MouseWheel, new Listener() {
            @Override
            public void handleEvent(Event e)
            {
                onMouseWheel(e);
            }
        });
    }

    private void onFocusIn()
    {
        if (getItemCount() > 0 && focusItem < 0) {
            focusItem = 0;
        }
    }

    private void onDispose(Event event)
    {
        //We only want to dispose of our items and such *after* anybody else who may have been
        //listening to the dispose has had a chance to do whatever.
        removeListener(SWT.Dispose, disposeListener);
        notifyListeners(SWT.Dispose, event);
        event.type = SWT.None;

        disposing = true;

        UIUtils.dispose(cellHeaderSelectionBackground);
        UIUtils.dispose(cellHeaderSelectionForeground);

        for (GridColumn col : columns) {
            col.dispose();
        }

//        UIUtils.dispose(sizingGC);
    }

    /**
     * Mouse wheel event handler.
     *
     * @param e event
     */
    private void onMouseWheel(Event e)
    {
        if (vScroll.getVisible()) {
            vScroll.handleMouseWheel(e);
            if (getVerticalBar() == null)
                e.doit = false;
        } else if (hScroll.getVisible()) {
            hScroll.handleMouseWheel(e);
            if (getHorizontalBar() == null)
                e.doit = false;
        }
    }

    /**
     * Mouse down event handler.
     *
     * @param e event
     */
    private void onMouseDown(MouseEvent e)
    {
        // for some reason, SWT prefers the children to get focus if
        // there are any children
        // the setFocus method on Composite will not set focus to the
        // Composite if one of its
        // children can get focus instead. This only affects the grid
        // when an editor is open
        // and therefore the grid has a child. The solution is to
        // forceFocus()
        if ((getStyle() & SWT.NO_FOCUS) != SWT.NO_FOCUS) {
            forceFocus();
        }

        //if populated will be fired at end of method.
        Event selectionEvent = null;

        cellSelectedOnLastMouseDown = false;
        cellRowSelectedOnLastMouseDown = false;
        cellColumnSelectedOnLastMouseDown = false;

        if (hoveringOnColumnSorter) {
            handleHoverOnColumnHeader(e.x, e.y);
            if (hoveringOnColumnSorter) {
                return;
            }
        }
        if (hoveringOnColumnResizer) {
            if (e.button == 1) {
                resizingColumn = true;
                resizingStartX = e.x;
                resizingColumnStartWidth = columnBeingResized.getWidth();
            }
            return;
        }

        int row = getRow(new Point(e.x, e.y));

        if (isListening(SWT.DragDetect)) {
            if (hoveringOnSelectionDragArea) {
                if (dragDetect(e)) {
                    return;
                }
            }
        }

        GridColumn col = null;
        if (row >= 0) {
            {
                col = getColumn(new Point(e.x, e.y));
                boolean isSelectedCell = false;
                if (col != null)
                    isSelectedCell = selectedCells.contains(new GridPos(col.getIndex(), row));

                if (e.button == 1 || e.button == 2 || (e.button == 3 && col != null && !isSelectedCell)) {
                    if (col != null) {
                        selectionEvent = updateCellSelection(new GridPos(col.getIndex(), row), e.stateMask, false, true, EventSource.MOUSE);
                        cellSelectedOnLastMouseDown = (getCellSelectionCount() > 0);

                        if (e.stateMask != SWT.MOD2) {
                            focusColumn = col;
                            focusItem = row;
                        }
                        //showColumn(col);
                        showItem(row);
                        redraw();
                    } else if (rowHeaderVisible) {
                        if (e.x <= rowHeaderWidth) {

                            boolean shift = ((e.stateMask & SWT.MOD2) != 0);
                            boolean ctrl = false;
                            if (!shift) {
                                ctrl = ((e.stateMask & SWT.MOD1) != 0);
                            }

                            List<GridPos> cells = new ArrayList<GridPos>();

                            if (shift) {
                                getCells(row, focusItem, cells);
                            } else {
                                getCells(row, cells);
                            }

                            int newStateMask = SWT.NONE;
                            if (ctrl) newStateMask = SWT.MOD1;

                            selectionEvent = updateCellSelection(cells, newStateMask, shift, ctrl, EventSource.MOUSE);
                            cellRowSelectedOnLastMouseDown = (getCellSelectionCount() > 0);

                            if (!shift) {
                                //set focus back to the first visible column
                                focusColumn = getColumn(new Point(rowHeaderWidth + 1, e.y));

                                focusItem = row;
                            }
                            showItem(row);
                            redraw();
                        }
                    }
                }
            }
        } else if (e.button == 1 && rowHeaderVisible && e.x <= rowHeaderWidth && e.y < headerHeight) {
            // Nothing to select
            if (getItemCount() == 0) {
                return;
            }

            //click on the top left corner means select everything
            selectionEvent = selectAllCellsInternal(e.stateMask);

            focusColumn = getColumn(new Point(rowHeaderWidth + 1, 1));
            focusItem = getTopIndex();
        } else if (e.button == 1 && columnHeadersVisible && e.y <= headerHeight) {
            //column cell selection
            col = getColumn(new Point(e.x, e.y));

            if (col == null) return;

            if (getItemCount() == 0)
                return;

            List<GridPos> cells = new ArrayList<GridPos>();
            getCells(col, cells);

            selectionEvent = updateCellSelection(cells, e.stateMask, false, true, EventSource.MOUSE);
            cellColumnSelectedOnLastMouseDown = (getCellSelectionCount() > 0);

            if (getItemCount() > 0) {
                focusColumn = col;
                focusItem = 0;
            }

            showColumn(col);
            redraw();
        } else {
            // Change focus column anyway
            GridColumn column = getColumn(new Point(e.x, e.y));
            if (column == null) {
                // Clicked on top-left cell or outside of grid
                return;
            }
            focusColumn = column;
        }

        if (selectionEvent != null) {
            selectionEvent.stateMask = e.stateMask;
            selectionEvent.button = e.button;
            selectionEvent.data = new GridPos(col == null ? 0 : col.getIndex(), row);
            selectionEvent.x = e.x;
            selectionEvent.y = e.y;
            notifyListeners(SWT.Selection, selectionEvent);
        }


    }

    /**
     * Mouse double click event handler.
     *
     * @param e event
     */
    private void onMouseDoubleClick(MouseEvent e)
    {
        if (e.button == 1) {

            if (hoveringOnColumnResizer) {
                columnBeingResized.pack();
                columnBeingResized.fireResized();
                for (int index = columns.indexOf(columnBeingResized) + 1; index < columns.size(); index++) {
                    GridColumn col = columns.get(index);
                    col.fireMoved();
                }
                resizingColumn = false;
                handleHoverOnColumnHeader(e.x, e.y);
                return;
            }

            int row = getRow(new Point(e.x, e.y));
            if (row >= 0) {
                if (isListening(SWT.DefaultSelection)) {
                    Event newEvent = new Event();
                    newEvent.data = row;

                    notifyListeners(SWT.DefaultSelection, newEvent);
                }
            }
        }
    }

    /**
     * Mouse up handler.
     *
     * @param e event
     */
    private void onMouseUp(MouseEvent e)
    {
        cellSelectedOnLastMouseDown = false;

        if (hoveringOnColumnSorter) {
            handleHoverOnColumnHeader(e.x, e.y);
            if (hoveringOnColumnSorter) {
                if (e.button == 1) {
                    Event event = new Event();
                    //event.data = row;
                    //event.data = e.data;
                    event.x = e.x;
                    event.y = e.y;
                    event.data = columnBeingSorted;
                    event.stateMask = e.stateMask;
                    notifyListeners(Event_ChangeSort, event);
                    return;
                }
            }
        }
        if (resizingColumn) {
            resizingColumn = false;
            handleHoverOnColumnHeader(e.x, e.y); // resets cursor if
            // necessary
            return;
        }

        if (cellDragSelectionOccurring || cellRowDragSelectionOccurring || cellColumnDragSelectionOccurring) {
            cellDragSelectionOccurring = false;
            cellRowDragSelectionOccurring = false;
            cellColumnDragSelectionOccurring = false;
            setCursor(null);

            if (followupCellSelectionEventOwed) {
                Event se = new Event();
                se.button = e.button;
                Point point = new Point(e.x, e.y);
                GridColumn column = getColumn(point);
                se.data = new GridPos(column == null ? 0 : column.getIndex(), getRow(point));
                se.stateMask = e.stateMask;
                se.x = e.x;
                se.y = e.y;

                notifyListeners(SWT.Selection, se);
                followupCellSelectionEventOwed = false;
            }
        }
    }

    /**
     * Mouse move event handler.
     *
     * @param e event
     */
    private void onMouseMove(MouseEvent e)
    {
        //if populated will be fired at end of method.
        Event selectionEvent = null;

        if ((e.stateMask & SWT.BUTTON1) == 0) {

            handleHovering(e.x, e.y);

        } else {

            if (resizingColumn) {
                handleColumnResizerDragging(e.x);
                return;
            }
            {
                if (!cellDragSelectionOccurring && cellSelectedOnLastMouseDown) {
                    cellDragSelectionOccurring = true;
                    //XXX: make this user definable
                    setCursor(getDisplay().getSystemCursor(SWT.CURSOR_CROSS));
                    cellDragCTRL = ((e.stateMask & SWT.MOD1) != 0);
                    if (cellDragCTRL) {
                        selectedCellsBeforeRangeSelect.clear();
                        selectedCellsBeforeRangeSelect.addAll(selectedCells);
                    }
                }
                if (!cellRowDragSelectionOccurring && cellRowSelectedOnLastMouseDown) {
                    cellRowDragSelectionOccurring = true;
                    setCursor(getDisplay().getSystemCursor(SWT.CURSOR_CROSS));
                    cellDragCTRL = ((e.stateMask & SWT.MOD1) != 0);
                    if (cellDragCTRL) {
                        selectedCellsBeforeRangeSelect.clear();
                        selectedCellsBeforeRangeSelect.addAll(selectedCells);
                    }
                }

                if (!cellColumnDragSelectionOccurring && cellColumnSelectedOnLastMouseDown) {
                    cellColumnDragSelectionOccurring = true;
                    setCursor(getDisplay().getSystemCursor(SWT.CURSOR_CROSS));
                    cellDragCTRL = ((e.stateMask & SWT.MOD1) != 0);
                    if (cellDragCTRL) {
                        selectedCellsBeforeRangeSelect.clear();
                        selectedCellsBeforeRangeSelect.addAll(selectedCells);
                    }
                }

                int ctrlFlag = (cellDragCTRL ? SWT.MOD1 : SWT.NONE);

                if (cellDragSelectionOccurring && handleCellHover(e.x, e.y)) {
                    GridColumn intentColumn = hoveringColumn;
                    int intentItem = hoveringItem;

                    if (hoveringItem < 0) {
                        if (e.y > headerHeight) {
                            //then we must be hovering way to the bottom
                            intentItem = Math.min(getItemCount() - 1, getBottomIndex() + 1);
                        } else {
                            intentItem = Math.max(0, getTopIndex() - 1);
                        }
                    }


                    if (hoveringColumn == null) {
                        if (e.x > rowHeaderWidth) {
                            //then we must be hovering way to the right
                            intentColumn = columns.get(columns.size() - 1);
                        } else {
                            intentColumn = columns.get(0);
                        }
                    }

                    showColumn(intentColumn);
                    showItem(intentItem);
                    selectionEvent = updateCellSelection(new GridPos(intentColumn.getIndex(), intentItem),
                                                         ctrlFlag | SWT.MOD2, true, false, EventSource.MOUSE);
                }
                if (cellRowDragSelectionOccurring && handleCellHover(e.x, e.y)) {
                    int intentItem = hoveringItem;

                    if (hoveringItem < 0) {
                        if (e.y > headerHeight) {
                            //then we must be hovering way to the bottom
                            intentItem = getItemCount() - 1;
                        } else {
                            if (getTopIndex() > 0) {
                                intentItem = getTopIndex() - 1;
                            } else {
                                intentItem = 0;
                            }
                        }
                    }

                    List<GridPos> cells = new ArrayList<GridPos>();

                    getCells(intentItem, focusItem, cells);

                    showItem(intentItem);
                    selectionEvent = updateCellSelection(cells, ctrlFlag, true, false, EventSource.MOUSE);
                }
                if (cellColumnDragSelectionOccurring && handleCellHover(e.x, e.y)) {
                    GridColumn intentCol = hoveringColumn;

                    if (intentCol == null) {
                        if (e.y < rowHeaderWidth) {
                            //TODO: get the first col to the left
                        } else {
                            //TODO: get the first col to the right
                        }
                    }

                    if (intentCol == null) return;  //temporary

                    GridColumn iterCol = intentCol;

                    List<GridPos> newSelected = new ArrayList<GridPos>();

                    boolean decreasing = (columns.indexOf(iterCol) > columns.indexOf(focusColumn));

                    do {
                        getCells(iterCol, newSelected);

                        if (iterCol == focusColumn) {
                            break;
                        }

                        if (decreasing) {
                            iterCol = getPreviousVisibleColumn(iterCol);
                        } else {
                            iterCol = getNextVisibleColumn(iterCol);
                        }

                    } while (true);

                    selectionEvent = updateCellSelection(newSelected, ctrlFlag, true, false, EventSource.MOUSE);
                }

            }
        }

        if (selectionEvent != null) {
            selectionEvent.stateMask = e.stateMask;
            selectionEvent.button = e.button;
            Point point = new Point(e.x, e.y);
            GridColumn column = getColumn(point);
            selectionEvent.data = new GridPos(column == null ? 0 : column.getIndex(), getRow(point));
            selectionEvent.x = e.x;
            selectionEvent.y = e.y;
            notifyListeners(SWT.Selection, selectionEvent);
        }
    }

    /**
     * Handles the assignment of the correct values to the hover* field
     * variables that let the painting code now what to paint as hovered.
     *
     * @param x mouse x coordinate
     * @param y mouse y coordinate
     */
    private void handleHovering(int x, int y)
    {
        // TODO: need to clean up and refactor hover code
        handleCellHover(x, y);

        if (columnHeadersVisible) {
            handleHoverOnColumnHeader(x, y);
            //if (handleHoverOnColumnHeader(x, y)) {
//                if (hoveringItem != null || !hoveringDetail.equals("") || hoveringColumn != null
//                    || hoveringColumnHeader != null || hoverColumnGroupHeader != null)
//                {
//                    hoveringItem = null;
//                    hoveringDetail = "";
//                    hoveringColumn = null;
//                    hoveringColumnHeader = null;
//                    hoverColumnGroupHeader = null;
//
//                    Rectangle clientArea = getClientArea();
//                    redraw(clientArea.x,clientArea.y,clientArea.width,clientArea.height,false);
//                }
                //return;
            //}
        }
    }

    /**
     * Refreshes the hover* variables according to the mouse location and
     * current state of the table. This is useful is some method call, caused
     * the state of the table to change and therefore the hover effects may have
     * become out of date.
     */
    protected void refreshHoverState()
    {
        Point p = getDisplay().map(null, this, getDisplay().getCursorLocation());
        handleHovering(p.x, p.y);
    }

    /**
     * Mouse exit event handler.
     *
     * @param e event
     */
    private void onMouseExit(MouseEvent e)
    {
        hoveringItem = -1;
        hoveringDetail = "";
        hoveringColumn = null;
        redraw();
    }

    /**
     * Key down event handler.
     *
     * @param e event
     */
    public void onKeyDown(Event e)
    {
        if (focusColumn == null) {
            if (columns.size() == 0)
                return;

            focusColumn = getColumn(0);
        }

        if (e.character == '\r' && focusItem >= 0) {
            Event newEvent = new Event();
            newEvent.data = focusItem;

            notifyListeners(SWT.DefaultSelection, newEvent);
            return;
        }

        int newSelection = -1;
        GridColumn newColumnFocus = null;

        //These two variables are used because the key navigation when the shift key is down is
        //based, not off the focus item/column, but rather off the implied focus (i.e. where the
        //keyboard has extended focus to).
        int impliedFocusItem = focusItem;
        GridColumn impliedFocusColumn = focusColumn;

        boolean ctrlPressed = ((e.stateMask & SWT.MOD1) != 0);
        boolean shiftPressed = ((e.stateMask & SWT.MOD2) != 0);

        //if (shiftPressed) {
            if (shiftSelectionAnchorColumn != null) {
                impliedFocusItem = shiftSelectionAnchorItem;
                impliedFocusColumn = shiftSelectionAnchorColumn;
            }
        //}
        switch (e.keyCode) {
            case SWT.ARROW_RIGHT:
                {
                    if (impliedFocusItem >= 0 && impliedFocusColumn != null) {
                        newSelection = impliedFocusItem;

                        int index = columns.indexOf(impliedFocusColumn);

                        index++;

                        if (index < columns.size()) {
                            newColumnFocus = columns.get(index);
                        } else {
                            newColumnFocus = impliedFocusColumn;
                        }
                    }
                }
                break;
            case SWT.ARROW_LEFT:
                {
                    if (impliedFocusItem >= 0 && impliedFocusColumn != null) {
                        newSelection = impliedFocusItem;

                        int index = columns.indexOf(impliedFocusColumn);

                        if (index != 0) {
                            newColumnFocus = columns.get(index - 1);
                        } else {
                            newColumnFocus = impliedFocusColumn;
                        }
                    }
                }
                break;
            case SWT.ARROW_UP:
                if (impliedFocusItem >= 0) {
                    newSelection = getPreviousVisibleItem(impliedFocusItem);
                }

                if (impliedFocusColumn != null) {
                    newColumnFocus = impliedFocusColumn;
                }

                break;
            case SWT.ARROW_DOWN:
                if (impliedFocusItem >= 0) {
                    newSelection = getNextVisibleItem(impliedFocusItem);
                } else {
                    if (getItemCount() > 0) {
                        newSelection = 0;
                    }
                }

                if (impliedFocusColumn != null) {
                    newColumnFocus = impliedFocusColumn;
                }
                break;
            case SWT.HOME:
                if (ctrlPressed) {
                    newSelection = 0;
                } else {
                    newSelection = impliedFocusItem;
                }
                newColumnFocus = columns.get(0);

                break;
            case SWT.END:
                {
                    if (ctrlPressed && getItemCount() > 0) {
                        newSelection = getItemCount() - 1;
                    } else {
                        newSelection = impliedFocusItem;
                    }
                    newColumnFocus = columns.get(columns.size() - 1);
                }

                break;
            case SWT.PAGE_UP:
                int topIndex = getTopIndex();

                newSelection = topIndex;

                if ((impliedFocusItem >= 0 && impliedFocusItem == topIndex) || focusItem == topIndex) {
                    RowRange range = getRowRange(getTopIndex(), getVisibleGridHeight(), false, true);
                    newSelection = range.startIndex;
                }

                if (impliedFocusColumn != null) {
                    newColumnFocus = impliedFocusColumn;
                }
                //newColumnFocus = focusColumn;
                break;
            case SWT.PAGE_DOWN:
                int bottomIndex = getBottomIndex();

                newSelection = bottomIndex;

                if (!isShown(bottomIndex)) {
                    // the item at bottom index is not shown completely
                    int tmpItem = getPreviousVisibleItem(newSelection);
                    if (tmpItem >= 0)
                        newSelection = tmpItem;
                }

                if ((impliedFocusItem >= 0 && impliedFocusItem >= bottomIndex - 1) || focusItem == bottomIndex - 1) {
                    RowRange range = getRowRange(getBottomIndex(), getVisibleGridHeight(), true, false);
                    newSelection = range.endIndex;
                }

                if (impliedFocusColumn != null) {
                    newColumnFocus = impliedFocusColumn;
                }
                //newColumnFocus = focusColumn;
                break;
            default:
                break;
        }

        if (newSelection < 0) {
            return;
        }

        {
            //if (e.stateMask != SWT.MOD1) {
            GridPos newPos = new GridPos(newColumnFocus.getIndex(), newSelection);
            Event selEvent = updateCellSelection(
                newPos,
                e.stateMask,
                false,
                false,
                EventSource.KEYBOARD);
            //}

            if (!shiftPressed)
                focusColumn = newColumnFocus;
                showColumn(newColumnFocus);

            if (!shiftPressed) {
                if (newSelection < 0) {
                    focusItem = -1;
                } else {
                    focusItem = newSelection;
                }
            }
            showItem(newSelection);

            if (selEvent != null) {
                selEvent.stateMask = e.stateMask;
                selEvent.character = e.character;
                selEvent.keyCode = e.keyCode;
                selEvent.data = newPos;
                notifyListeners(SWT.Selection, selEvent);
            }

            redraw();
        }
    }

    /**
     * Resize event handler.
     */
    private void onResize()
    {

        //CGross 1/2/08 - I don't really want to be doing this....
        //I shouldn't be changing something you user configured...
        //leaving out for now
//        if (columnScrolling)
//        {
//        	int maxWidth = getClientArea().width;
//        	if (rowHeaderVisible)
//        		maxWidth -= rowHeaderWidth;
//
//        	for (Iterator cols = columns.iterator(); cols.hasNext();) {
//				GridColumn col = (GridColumn) cols.next();
//				if (col.getWidth() > maxWidth)
//					col.setWidth(maxWidth);
//			}
//        }

        scrollValuesObsolete = true;
        topIndex = -1;
        bottomIndex = -1;
    }

    /**
     * Scrollbar selection event handler.
     */
    private void onScrollSelection()
    {
        topIndex = -1;
        bottomIndex = -1;
        refreshHoverState();
        redraw(getClientArea().x, getClientArea().y, getClientArea().width, getClientArea().height,
               false);
    }

    /**
     * Returns the intersection of the given column and given item.
     *
     * @param column column
     * @param item   item
     * @return x,y of top left corner of the cell
     */
    Point getOrigin(GridColumn column, int item)
    {
        int x = 0;

        if (rowHeaderVisible) {
            x += rowHeaderWidth;
        }

        x -= getHScrollSelectionInPixels();

        for (GridColumn colIter : columns) {
            if (colIter == column) {
                break;
            }
            x += colIter.getWidth();
        }

        int y = 0;
        if (item >= 0) {
            if (columnHeadersVisible) {
                y += headerHeight;
            }

            int currIndex = getTopIndex();

            if (item == -1) {
                SWT.error(SWT.ERROR_INVALID_ARGUMENT);
            }

            while (currIndex != item) {
                if (currIndex < item) {
                    y += getItemHeight() + 1;
                    currIndex++;
                } else if (currIndex > item) {
                    currIndex--;
                    y -= getItemHeight() + 1;
                }
            }
        }

        return new Point(x, y);
    }

    /**
     * Sets the hovering variables (hoverItem,hoveringColumn) as well as
     * hoverDetail by talking to the cell renderers. Triggers a redraw if
     * necessary.
     *
     * @param x mouse x
     * @param y mouse y
     * @return true if a new section of the table is now being hovered
     */
    private boolean handleCellHover(int x, int y)
    {

        String detail = "";

        Point point = new Point(x, y);
        final GridColumn col = getColumn(point);
        final int row = getRow(point);

        GridColumn hoverColHeader = null;

        boolean hoverChange = false;

        if (hoveringItem != row || !hoveringDetail.equals(detail) || hoveringColumn != col
            || hoverColHeader != hoveringColumnHeader) {
            hoveringItem = row;
            hoveringDetail = detail;
            hoveringColumn = col;
            hoveringColumnHeader = hoverColHeader;

            // TODO: guess why did they put redraw on mouse move??? It took fucking much memory and processor
            //Rectangle clientArea = getClientArea();
            //redraw(clientArea.x, clientArea.y, clientArea.width, clientArea.height, false);

            hoverChange = true;
        }

        //do normal cell specific tooltip stuff
        if (hoverChange) {
            String newTip = null;
            if ((hoveringItem >= 0) && (hoveringColumn != null)) {
                // get cell specific tooltip
                newTip = getCellToolTip(hoveringColumn.getIndex(), hoveringItem);
            } else if ((hoveringColumn != null) && (hoveringColumnHeader != null)) {
                // get column header specific tooltip
                newTip = hoveringColumn.getHeaderTooltip();
            }

            if (newTip == null) { // no cell or column header specific tooltip then use base Grid tooltip
                newTip = getToolTipText();
            }

            //Avoid unnecessarily resetting tooltip - this will cause the tooltip to jump around
            if (newTip != null && !newTip.equals(displayedToolTipText)) {
                updateToolTipText(newTip);
            } else if (newTip == null && displayedToolTipText != null) {
                updateToolTipText(null);
            }
            displayedToolTipText = newTip;
        }

        return hoverChange;
    }

    /**
     * Sets the tooltip for the whole Grid to the given text.  This method is made available
     * for subclasses to override, when a subclass wants to display a different than the standard
     * SWT/OS tooltip.  Generally, those subclasses would override this event and use this tooltip
     * text in their own tooltip or just override this method to prevent the SWT/OS tooltip from
     * displaying.
     *
     * @param text
     */
    protected void updateToolTipText(@Nullable String text)
    {
        super.setToolTipText(text);
    }

    /**
     * Marks the scroll values obsolete so they will be recalculated.
     */
    protected void setScrollValuesObsolete()
    {
        this.scrollValuesObsolete = true;
        redraw();
    }

    /**
     * Inserts a new column into the table.
     *
     * @param column new column
     * @param index  index to insert new column
     * @return current number of columns
     */
    int newColumn(GridColumn column, int index)
    {

        if (index == -1) {
            columns.add(column);
        } else {
            columns.add(index, column);

            for (int i = 0; i < columns.size(); i++) {
                columns.get(i).setColumnIndex(i);
            }
        }

        recalculateSizes();

        scrollValuesObsolete = true;
        redraw();

        return columns.size() - 1;
    }

    public void recalculateSizes() {
        computeHeaderHeight();
        computeItemHeight();
    }

    /**
     * Removes the given column from the table.
     *
     * @param column column to remove
     */
    void removeColumn(GridColumn column)
    {
        boolean selectionModified = false;

        int index = column.getIndex();

        {
            List<GridPos> removeSelectedCells = new ArrayList<GridPos>();

            for (GridPos cell : selectedCells) {
                if (cell.col == index) {
                    removeSelectedCells.add(cell);
                }
            }

            if (removeSelectedCells.size() > 0) {
                selectedCells.removeAll(removeSelectedCells);
                selectionModified = true;
            }

            for (GridPos cell : selectedCells) {
                if (cell.col >= index) {
                    cell.col--;
                    selectionModified = true;
                }
            }
        }

        columns.remove(column);

        // Check focus column
        if (column == focusColumn) {
            focusColumn = null;
        }
        if (column == shiftSelectionAnchorColumn) {
            shiftSelectionAnchorColumn = null;
        }

        scrollValuesObsolete = true;
        redraw();

        int i = 0;
        for (GridColumn col : columns) {
            col.setColumnIndex(i);
            i++;
        }

        if (selectionModified && !disposing) {
            updateSelectionCache();
        }

    }

    /**
     * Returns the current item in focus.
     *
     * @return item in focus or {@code null}.
     */
    public int getFocusItem()
    {
        checkWidget();
        return focusItem;
    }

    /**
     * Returns the current cell in focus.  If cell selection is disabled, this method returns null.
     *
     * @return cell in focus or {@code null}. x represents the column and y the row the cell is in
     */
    public GridPos getFocusCell()
    {
        checkWidget();

        int x = -1;

        if (focusColumn != null)
            x = focusColumn.getIndex();
        focusCell.col = x;
        focusCell.row = focusItem;
        return focusCell;
    }

    /**
     * Sets the focused item to the given item.
     *
     * @param item item to focus.
     */
    public void setFocusItem(int item)
    {
        checkWidget();
        focusItem = item;
    }

    /**
     * Sets the focused item to the given column. Column focus is only applicable when cell
     * selection is enabled.
     *
     * @param col column to focus.
     */
    public void setFocusColumn(int col)
    {
        checkWidget();
        GridColumn column = getColumn(col);
        if (column == null || column.isDisposed() || column.getParent() != this) {
            SWT.error(SWT.ERROR_INVALID_ARGUMENT);
            return;
        }

        focusColumn = column;
    }


    /**
     * Returns true if the table is set to horizontally scroll column-by-column
     * rather than pixel-by-pixel.
     *
     * @return true if the table is scrolled horizontally by column
     */
    public boolean getColumnScrolling()
    {
        checkWidget();
        return columnScrolling;
    }

    /**
     * Sets the table scrolling method to either scroll column-by-column (true)
     * or pixel-by-pixel (false).
     *
     * @param columnScrolling true to horizontally scroll by column, false to
     *                        scroll by pixel
     */
    public void setColumnScrolling(boolean columnScrolling)
    {
        checkWidget();
        if (rowHeaderVisible && !columnScrolling) {
            return;
        }

        this.columnScrolling = columnScrolling;
        scrollValuesObsolete = true;
        redraw();
    }

    /**
     * Deselects the given cell in the receiver.  If the given cell is already
     * deselected it remains deselected.  Invalid cells are ignored.
     *
     * @param cell cell to deselect.
     */
    public void deselectCell(GridPos cell)
    {
        checkWidget();

        if (cell == null)
            SWT.error(SWT.ERROR_NULL_ARGUMENT);

        selectedCells.remove(cell);
        updateSelectionCache();
        redraw();
    }

    /**
     * Deselects the given cells.  Invalid cells are ignored.
     *
     * @param cells the cells to deselect.
     */
    public void deselectCells(Collection<GridPos> cells)
    {
        checkWidget();

        if (cells == null) {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
            return;
        }

        for (GridPos cell : cells) {
            if (cell == null)
                SWT.error(SWT.ERROR_NULL_ARGUMENT);
        }

        for (GridPos cell : cells) {
            selectedCells.remove(cell);
        }

        updateSelectionCache();

        redraw();
    }

    /**
     * Deselects all selected cells in the receiver.
     */
    public void deselectAllCells()
    {
        checkWidget();
        selectedCells.clear();
        updateSelectionCache();
        redraw();
    }

    /**
     * Selects the given cell.  Invalid cells are ignored.
     *
     * @param cell point whose x values is a column index and y value is an item index
     */
    public void selectCell(@NotNull GridPos cell)
    {
        checkWidget();

        if (cell == null)
            SWT.error(SWT.ERROR_NULL_ARGUMENT);

        addToCellSelection(cell);
        updateSelectionCache();
        redraw();
    }

    /**
     * Selects the given cells.  Invalid cells are ignored.
     *
     * @param cells an arry of points whose x value is a column index and y value is an item index
     */
    public void selectCells(Collection<GridPos> cells)
    {
        checkWidget();

        if (cells == null) {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
            return;
        }

        for (GridPos cell : cells) {
            if (cell == null)
                SWT.error(SWT.ERROR_NULL_ARGUMENT);
        }

        for (GridPos cell : cells) {
            addToCellSelection(cell);
        }

        updateSelectionCache();
        redraw();
    }

    /**
     * Selects all cells in the receiver.
     */
    public void selectAllCells()
    {
        checkWidget();
        selectAllCellsInternal(0);
    }

    /**
     * Selects all cells in the receiver.
     *
     * @return An Event object
     */
    @Nullable
    private Event selectAllCellsInternal(int stateMask)
    {
        if (columns.size() == 0)
            return null;

        if (getItemCount() == 0)
            return null;

        GridColumn oldFocusColumn = focusColumn;
        int oldFocusItem = focusItem;

        focusColumn = columns.get(0);
        focusItem = 0;

        List<GridPos> cells = new ArrayList<GridPos>();
        getAllCells(cells);
        Event selectionEvent = updateCellSelection(cells, stateMask, false, true, EventSource.KEYBOARD);

        focusColumn = oldFocusColumn;
        focusItem = oldFocusItem;

        updateSelectionCache();

        redraw();

        return selectionEvent;
    }

    /**
     * Selects all cells in the given column in the receiver.
     *
     * @param col
     */
    public void selectColumn(int col)
    {
        checkWidget();
        List<GridPos> cells = new ArrayList<GridPos>();
        getCells(getColumn(col), cells);
        selectCells(cells);
    }

    /**
     * Selects the selection to the given cell.  The existing selection is cleared before
     * selecting the given cell.
     *
     * @param cell point whose x values is a column index and y value is an item index
     */
    public void setCellSelection(@NotNull GridPos cell)
    {
        checkWidget();

        if (!isValidCell(cell))
            SWT.error(SWT.ERROR_INVALID_ARGUMENT);

        selectedCells.clear();
        addToCellSelection(cell);
        updateSelectionCache();
        redraw();
    }

    /**
     * Selects the selection to the given set of cell.  The existing selection is cleared before
     * selecting the given cells.
     *
     * @param cells point array whose x values is a column index and y value is an item index
     */
    public void setCellSelection(Collection<GridPos> cells)
    {
        checkWidget();

        if (cells == null) {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
            return;
        }

        for (GridPos cell : cells) {
            if (!isValidCell(cell))
                SWT.error(SWT.ERROR_INVALID_ARGUMENT);
        }

        selectedCells.clear();
        for (GridPos cell : cells) {
            addToCellSelection(cell);
        }

        updateSelectionCache();
        redraw();
    }

    /**
     * Returns an array of cells that are currently selected in the
     * receiver. The order of the items is unspecified. An empty array indicates
     * that no items are selected.
     * <p>
     * Note: This is not the actual structure used by the receiver to maintain
     * its selection, so modifying the array will not affect the receiver.
     * </p>
     *
     * @return an array representing the cell selection
     */
    @NotNull
    public Collection<GridPos> getSelection()
    {
        if (isDisposed()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableCollection(selectedCells);
    }

    @NotNull
    public List<GridColumn> getSelectedColumns()
    {
        return selectedColumns;
    }

    /**
     * Returns selected rows indexes
     * @return indexes of selected rows
     */
    public Collection<Integer> getRowSelection()
    {
        return Collections.unmodifiableCollection(selectedRows.keySet());
    }

    private void getCells(GridColumn col, List<GridPos> cells)
    {
        int colIndex = col.getIndex();

        for (int i = 0; i < getItemCount(); i++) {
            cells.add(new GridPos(colIndex, i));
        }
    }

    private void getCells(int row, List<GridPos> cells)
    {
        for (int i = 0; i < columns.size(); i++) {
            cells.add(new GridPos(i, row));
        }
    }

    private void getAllCells(List<GridPos> cells)
    {
        for (int i = 0; i < getItemCount(); i++) {
            for (int k = 0; k < columns.size(); k++) {
                cells.add(new GridPos(k, i));
            }
        }
    }

    private List<GridPos> getCells(int row)
    {
        List<GridPos> cells = new ArrayList<GridPos>();
        getCells(row, cells);
        return cells;
    }


    private void getCells(int startRow, int endRow, List<GridPos> cells)
    {
        boolean descending = (startRow < endRow);

        int iterItem = endRow;

        do {
            getCells(iterItem, cells);

            if (iterItem == startRow) break;

            if (descending) {
                iterItem--;
            } else {
                iterItem++;
            }
        } while (true);
    }

    private static int blend(int v1, int v2, int ratio)
    {
        return (ratio * v1 + (100 - ratio) * v2) / 100;
    }

    public static RGB blend(RGB c1, RGB c2, int ratio)
    {
        int r = blend(c1.red, c2.red, ratio);
        int g = blend(c1.green, c2.green, ratio);
        int b = blend(c1.blue, c2.blue, ratio);
        return new RGB(r, g, b);
    }

    /**
     * Returns a point whose x and y values are the to and from column indexes of the new selection
     * range inclusive of all spanned columns.
     */
    private Point getSelectionRange(int fromItem, GridColumn fromColumn, int toItem, GridColumn toColumn)
    {
        if (columns.indexOf(fromColumn) > columns.indexOf(toColumn)) {
            GridColumn temp = fromColumn;
            fromColumn = toColumn;
            toColumn = temp;
        }

        if (fromItem > toItem) {
            int temp = fromItem;
            fromItem = toItem;
            toItem = temp;
        }

        boolean firstTime = true;
        int iterItem = fromItem;

        int fromIndex = fromColumn.getIndex();
        int toIndex = toColumn.getIndex();

        do {
            if (!firstTime) {
                iterItem++;
            } else {
                firstTime = false;
            }

            Point cols = getRowSelectionRange(fromColumn, toColumn);

            //check and see if column spanning means that the range increased
            if (cols.x != fromIndex || cols.y != toIndex) {
                GridColumn newFrom = getColumn(cols.x);
                GridColumn newTo = getColumn(cols.y);

                //Unfortunately we have to start all over again from the top with the new range
                return getSelectionRange(fromItem, newFrom, toItem, newTo);
            }
        } while (iterItem != toItem);

        return new Point(fromColumn.getIndex(), toColumn.getIndex());
    }

    /**
     * Returns a point whose x and y value are the to and from column indexes of the new selection
     * range inclusive of all spanned columns.
     *
     * @param fromColumn
     * @param toColumn
     * @return
     */
    private Point getRowSelectionRange(GridColumn fromColumn, GridColumn toColumn)
    {
        int newFrom = fromColumn.getIndex();
        int newTo = toColumn.getIndex();
        return new Point(newFrom, newTo);
    }

    /**
     * Returns true if the given cell's x and y values are valid column and
     * item indexes respectively.
     *
     * @param cell
     * @return
     */
    private boolean isValidCell(GridPos cell)
    {
        if (cell.col < 0 || cell.col >= columns.size())
            return false;

        if (cell.row < 0 || cell.row >= getItemCount()) {
            return false;
        }
        // Valid
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFont(Font font)
    {
        super.setFont(font);
        sizingGC.setFont(font);
    }

    /**
     * Returns the row header width or 0 if row headers are not visible.
     *
     * @return the width of the row headers
     */
    public int getItemHeaderWidth()
    {
        checkWidget();
        if (!rowHeaderVisible)
            return 0;
        return rowHeaderWidth;
    }

    /**
     * Sets the row header width to the specified value. This automatically disables the auto width feature of the grid.
     *
     * @param width the width of the row header
     * @see #getItemHeaderWidth()
     */
    public void setItemHeaderWidth(int width)
    {
        checkWidget();
        rowHeaderWidth = width;
        redraw();
    }

    /**
     * @return the disposing
     */
    boolean isDisposing()
    {
        return disposing;
    }

    /**
     * Returns the receiver's tool tip text, or null if it has
     * not been set.
     *
     * @return the receiver's tool tip text
     */
    @Override
    public String getToolTipText()
    {
        checkWidget();
        return toolTipText;
    }


    /**
     * Sets the receiver's tool tip text to the argument, which
     * may be null indicating that no tool tip text should be shown.
     *
     * @param string the new tool tip text (or null)
     */
    @Override
    public void setToolTipText(String string)
    {
        checkWidget();
        toolTipText = string;
    }

    /**
     * Determines if the mouse is hovering on the selection drag area and changes the
     * pointer and sets field appropriately.
     * <p/>
     * Note:  The 'selection drag area' is that part of the selection,
     * on which a drag event can be initiated.  This is either the border
     * of the selection (i.e. a cell border between a selected and a non-selected
     * cell) or the complete selection (i.e. anywhere on a selected cell).
     *
     * @param x
     * @param y
     * @return
     */
    private boolean handleHoverOnSelectionDragArea(int x, int y)
    {
        boolean over = false;
//    	Point inSelection = null;

        if ((!rowHeaderVisible || x > rowHeaderWidth - SELECTION_DRAG_BORDER_THRESHOLD)
            && (!columnHeadersVisible || y > headerHeight - SELECTION_DRAG_BORDER_THRESHOLD)) {
            // not on a header

            // drag area is the entire selection

            {
                Point p = new Point(x, y);
                GridPos cell = getCell(p);
                over = cell != null && isCellSelected(cell);
            }
        }

        if (over != hoveringOnSelectionDragArea) {
            hoveringOnSelectionDragArea = over;
        }
        return over;
    }

    /**
     * Recalculate the height of the header
     */
    public void recalculateHeader()
    {
        int previous = getHeaderHeight();
        computeHeaderHeight();

        if (previous != getHeaderHeight()) {
            scrollValuesObsolete = true;
            redraw();
        }
    }

    int getStartColumnIndex()
    {
        checkWidget();

        if (startColumnIndex != -1) {
            return startColumnIndex;
        }

        if (!hScroll.getVisible()) {
            startColumnIndex = 0;
        }

        startColumnIndex = hScroll.getSelection();

        return startColumnIndex;
    }

    int getEndColumnIndex()
    {
        checkWidget();

        if (endColumnIndex != -1) {
            return endColumnIndex;
        }

        if (columns.isEmpty()) {
            endColumnIndex = 0;
        } else if (getVisibleGridWidth() < 1) {
            endColumnIndex = getStartColumnIndex();
        } else {
            int x = 0;
            x -= getHScrollSelectionInPixels();

            if (rowHeaderVisible) {
                //row header is actually painted later
                x += rowHeaderWidth;
            }

            int startIndex = getStartColumnIndex();

            for (int i = startIndex; i < columns.size(); i++) {
                endColumnIndex = i;
                GridColumn column = columns.get(i);
                x += column.getWidth();

                if (x > getClientArea().width) {

                    break;
                }
            }

        }

        endColumnIndex = Math.max(0, endColumnIndex);

        return endColumnIndex;
    }

    /**
     * Returns the width of the row headers.
     *
     * @return width of the column header row
     */
    public int getRowHeaderWidth()
    {
        checkWidget();
        return rowHeaderWidth;
    }

    public String getCellText(int column, int row)
    {
        String text = getContentProvider().getCellText(column, row);
        // Truncate too long texts (they are really bad for performance)
        if (text.length() > MAX_TOOLTIP_LENGTH) {
            text = text.substring(0, MAX_TOOLTIP_LENGTH) + " ...";
        }

        return text;
    }

    @Nullable
    public String getCellToolTip(int column, int row)
    {
        String toolTip = getCellText(column, row);
        if (toolTip == null) {
            return null;
        }
        // Show tooltip only if it's larger than column width
        Point ttSize = sizingGC.textExtent(toolTip);
        GridColumn itemColumn = getColumn(column);
        if (ttSize.x > itemColumn.getWidth() || ttSize.y > getItemHeight()) {
            int gridHeight = getBounds().height;
            if (ttSize.y > gridHeight) {
                // Too big tool tip - larger than entire grid
                // Lets chop it
                StringBuilder newToolTip = new StringBuilder();
                StringTokenizer st = new StringTokenizer(toolTip, "'\n");
                int maxLineNumbers = gridHeight / getItemHeight(), lineNumber = 0;
                while (st.hasMoreTokens()) {
                    newToolTip.append(st.nextToken()).append('\n');
                    lineNumber++;
                    if (lineNumber >= maxLineNumbers) {
                        break;
                    }
                }
                toolTip = newToolTip.toString();
            }
            return toolTip;
        } else {
            return null;
        }
    }

    @Nullable
    public Image getCellImage(int column, int row)
    {
        return getContentProvider().getCellImage(column,  row);
    }

    public Color getCellBackground(int column, int row)
    {
        Color color = getContentProvider().getCellBackground(column, row);
        return color != null ? color : getBackground();
    }

    public Color getCellForeground(int column, int row)
    {
        Color color = getContentProvider().getCellForeground(column, row);
        return color != null ? color : getForeground();
    }

    public Rectangle getCellBounds(int columnIndex, int rowIndex) {
        // HACK: The -1000,-1000 xy coordinates below are a hack to deal with
        // GridEditor issues. In
        // normal SWT Table, when an editor is created on Table and its
        // positioned in the header area
        // the header overlays the editor. Because Grid (header and everything)
        // is drawn on one
        // composite, when an editor is positioned in the header area the editor
        // overlays the header.
        // So to fix this, when the editor is anywhere its not supposed to be
        // seen (the editor
        // coordinates are determined by this getBounds) we position it out in
        // timbuktu.

        if (!isShown(rowIndex))
            return new Rectangle(-1000, -1000, 0, 0);

        GridColumn column = getColumn(columnIndex);
        Point origin = getOrigin(column, rowIndex);

        if (origin.x < 0 && isRowHeaderVisible())
            return new Rectangle(-1000, -1000, 0, 0);

        return new Rectangle(origin.x, origin.y, column.getWidth(), getItemHeight());
    }

    private static class CellComparator implements Comparator<GridPos> {
        @Override
        public int compare(GridPos pos1, GridPos pos2)
        {
            int res = pos1.row - pos2.row;
            return res != 0 ? res : pos1.col - pos2.col;
        }
    }

    private void drawEmptyColumnHeader(GC gc, Rectangle bounds)
    {
        gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

        gc.fillRectangle(
            bounds.x, 
            bounds.y, 
            bounds.width + 1,
            bounds.height + 1);
    }

    private void drawEmptyRowHeader(GC gc, Rectangle bounds)
    {
        gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

        gc.fillRectangle(bounds.x, bounds.y, bounds.width, bounds.height + 1);

        gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW));

        gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));

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

    public void drawEmptyCell(GC gc, Rectangle bounds, boolean selected) {

        boolean drawBackground = true;

        if (selected) {
            gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION));
            gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));
        } else {
            if (isEnabled()) {
                drawBackground = false;
            } else {
                gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
            }
            gc.setForeground(getForeground());
        }

        if (drawBackground) {
            gc.fillRectangle(bounds.x, bounds.y, bounds.width + 1,
                bounds.height);
        }

        if (getLinesVisible()) {
            gc.setForeground(getLineColor());
            gc.drawLine(
                bounds.x,
                bounds.y + bounds.height,
                bounds.x + bounds.width,
                bounds.y + bounds.height);
            gc.drawLine(bounds.x + bounds.width - 1,
                bounds.y,
                bounds.x + bounds.width - 1,
                bounds.y + bounds.height);
        }
    }

    private void drawTopLeftCell(GC gc, Rectangle bounds) {
        gc.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

        gc.fillRectangle(
            bounds.x,
            bounds.y,
            bounds.width - 1,
            bounds.height + 1);

        gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));

        gc.drawLine(
            bounds.x + bounds.width - 1,
            bounds.y,
            bounds.x + bounds.width - 1,
            bounds.y + bounds.height);

        gc.drawLine(
            bounds.x,
            bounds.y + bounds.height - 1,
            bounds.x + bounds.width,
            bounds.y + bounds.height - 1);

        //cfgButton.redraw();

    }

}


