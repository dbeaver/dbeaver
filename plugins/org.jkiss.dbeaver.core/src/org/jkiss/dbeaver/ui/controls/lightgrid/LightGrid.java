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
package org.jkiss.dbeaver.ui.controls.lightgrid;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.progress.UIJob;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dnd.LocalObjectTransfer;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
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

    private static final Log log = Log.getLog(LightGrid.class);

    private static final int MAX_TOOLTIP_LENGTH = 1000;

    protected static final int Event_ChangeSort = 1000;
    protected static final int Event_NavigateLink = 1001;
    protected static final int Event_FilterColumn = 1002;

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
    private static final int DEFAULT_ROW_HEADER_WIDTH = 30;
    private static final int MAX_ROW_HEADER_WIDTH = 400;


    /**
     * The minimum width of a column header.
     */
    private static final int MIN_COLUMN_HEADER_WIDTH = 32;

    /**
     * Threshold for the selection border used for drag n drop
     * in mode.
     */
    private static final int SELECTION_DRAG_BORDER_THRESHOLD = 2;
    private static final boolean MAXIMIZE_SINGLE_COLUMN = false;

    public enum EventSource {
        MOUSE,
        KEYBOARD,
    }

    static class GridNode {
        GridNode parent;
        Object[] rows;
        IGridContentProvider.ElementState state;
        int level;

        private GridNode(GridNode parent, Object[] rows, IGridContentProvider.ElementState state, int level) {
            this.parent = parent;
            this.rows = rows;
            this.state = state;
            this.level = level;
        }

        boolean isParentOf(GridNode node) {
            for (GridNode p = node; p != null; p = p.parent) {
                if (p == this) {
                    return true;
                }
            }
            return false;
        }
    }

    // Tooltips

    private class ToolTipHandler extends UIJob {
        private String toolTip;
        ToolTipHandler() {
            super("ToolTip handler");
            setSystem(true);
        }

        @Override
        public IStatus runInUIThread(IProgressMonitor monitor) {
            if (!monitor.isCanceled() && !LightGrid.this.isDisposed()) {
                LightGrid.this.setToolTipText(toolTip);
            }
            toolTipHandler = null;
            return Status.OK_STATUS;
        }
    }

    // Last calculated client area
    private volatile static Rectangle lastClientArea;

    private volatile String prevToolTip;
    private volatile ToolTipHandler toolTipHandler;

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

    private final Set<GridPos> selectedCells = new TreeSet<>(new GridPos.PosComparator());
    private final List<GridPos> selectedCellsBeforeRangeSelect = new ArrayList<>();
    private final List<GridColumn> selectedColumns = new ArrayList<>();
    private final IntKeyMap<Boolean> selectedRows = new IntKeyMap<>();

    private boolean cellDragSelectionOccurring = false;
    private boolean cellRowDragSelectionOccurring = false;
    private boolean cellColumnDragSelectionOccurring = false;
    private boolean cellDragCTRL = false;
    private boolean followupCellSelectionEventOwed = false;

    private boolean cellSelectedOnLastMouseDown;
    private boolean cellRowSelectedOnLastMouseDown;
    private boolean cellColumnSelectedOnLastMouseDown;

    private boolean headerColumnDragStarted;

    private GridColumn shiftSelectionAnchorColumn;

    private GridColumn focusColumn;
    private final GridPos focusCell = new GridPos(-1, -1);

    /**
     * List of table columns in creation/index order.
     */
    private final List<GridColumn> topColumns = new ArrayList<>();
    private final List<GridColumn> columns = new ArrayList<>();
    private int maxColumnDepth = 0;
    protected Object[] columnElements = new Object[0];
    protected Object[] rowElements = new Object[0];
    private GridNode[] parentNodes = new GridNode[0];
    private final Map<Object, GridNode> rowNodes = new IdentityHashMap<>();

    private int maxColumnDefWidth = 1000;

    private GridColumnRenderer columnHeaderRenderer;
    private GridRowRenderer rowHeaderRenderer;
    private GridCellRenderer cellRenderer;

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

    private boolean hoveringOnHeader = false;
    private boolean hoveringOnColumnSorter = false;
    private boolean hoveringOnColumnFilter = false;
    private boolean hoveringOnLink = false;

    private GridColumn columnBeingSorted;
    private GridColumn columnBeingFiltered;
    private boolean hoveringOnColumnResizer = false;
    private GridColumn columnBeingResized;
    private boolean resizingColumn = false;
    private int resizingStartX = 0;
    private int resizingColumnStartWidth = 0;
    private int hoveringItem;
    private GridColumn hoveringColumn;
    private GridColumn draggingColumn;

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
    private Object hoveringDetail = null;

    /**
     * Are the grid lines visible?
     */
    private boolean linesVisible = true;

    @NotNull
    private final IGridScrollBar vScroll;
    @NotNull
    private final IGridScrollBar hScroll;

    /**
     * Item selected when a multiple selection using shift+click first occurs.
     * This item anchors all further shift+click selections.
     */
    private int shiftSelectionAnchorItem;

    private boolean columnScrolling = false;

    /**
     * Dispose listener.  This listener is removed during the dispose event to allow re-firing of
     * the event.
     */
    private Listener disposeListener;

    GC sizingGC;
    FontMetrics fontMetrics;
    Font normalFont;

    @NotNull
    private Color lineColor;
    private Color lineSelectedColor;
    private Color backgroundColor;
    private Color foregroundColor;
    @NotNull
    private Cursor sortCursor;

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
     * True if the last visible item is completely visible.  The value must never be read directly.  It is cached and
     * updated when appropriate.  #isShown() should be called for every client (even internal
     * callers).
     *
     * @see #bottomIndex
     */
    private boolean bottomIndexShownCompletely = false;

    /**
     * This is the tooltip text currently used.  This could be the tooltip text for the currently
     * hovered cell, or the general grid tooltip.  See handleCellHover.
     */
    private String displayedToolTipText;

    private boolean hoveringOnHeaderDragArea = false;

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
        fontMetrics = sizingGC.getFontMetrics();
        normalFont = getFont();
        columnHeaderRenderer = new GridColumnRenderer(this);
        rowHeaderRenderer = new GridRowRenderer(this);
        cellRenderer = new GridCellRenderer(this);

        final Display display = getDisplay();
        lineColor = JFaceColors.getErrorBackground(display);
        lineSelectedColor = JFaceColors.getErrorBorder(display);//SWT.COLOR_WIDGET_DARK_SHADOW;
        //setForeground(JFaceColors.getBannerForeground(display));
        //setBackground(JFaceColors.getBannerBackground(display));
/*
        ColorRegistry colorRegistry = UIUtils.getColorRegistry();
        setLineColor(colorRegistry.get(JFacePreferences.QUALIFIER_COLOR));
        setForeground(colorRegistry.get(JFacePreferences.CONTENT_ASSIST_FOREGROUND_COLOR));
        setBackground(colorRegistry.get(JFacePreferences.CONTENT_ASSIST_BACKGROUND_COLOR));
*/
        sortCursor = display.getSystemCursor(SWT.CURSOR_HAND);

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

        addDragAndDropSupport();
        setDragDetect(true);
    }

    @NotNull
    public abstract IGridContentProvider getContentProvider();

    @NotNull
    public abstract IGridLabelProvider getLabelProvider();

    @Nullable
    public abstract IGridController getGridController();

    public boolean hasNodes() {
        return !rowNodes.isEmpty();
    }

    public void setMaxColumnDefWidth(int maxColumnDefWidth) {
        this.maxColumnDefWidth = maxColumnDefWidth;
    }

    private void collectRows(List<Object> result, List<GridNode> parents, @Nullable GridNode parent, Object[] rows, int level)
    {
        for (int i = 0; i < rows.length; i++) {
            Object row = rows[i];
            if (row == null) {
                continue;
            }
            result.add(row);
            parents.add(parent);
            Object[] children = getContentProvider().getChildren(row);
            if (children != null) {
                IGridContentProvider.ElementState state;
                GridNode node = rowNodes.get(row);
                if (node == null) {
                    state = getContentProvider().getDefaultState(row);
                    node = new GridNode(parent, children, state, level + 1);
                } else {
                    state = node.state;
                }
                rowNodes.put(row, node);
                if (state == IGridContentProvider.ElementState.EXPANDED) {
                    collectRows(result, parents, node, children, level + 1);
                }
            }
        }
    }

    /**
     * Refresh grid data
     */
    public void refreshData(boolean refreshColumns, boolean keepState)
    {
        GridPos savedFocus = keepState ? getFocusPos() : null;
        int savedHSB = keepState ? hScroll.getSelection() : -1;
        int savedVSB = keepState ? vScroll.getSelection() : -1;

        if (refreshColumns) {
            this.removeAll();
        } else {
            this.deselectAll();
            topIndex = -1;
            bottomIndex = -1;
        }
        IGridContentProvider contentProvider = getContentProvider();
        refreshRowsData();
        this.displayedToolTipText = null;

        if (refreshColumns) {
            this.maxColumnDepth = 0;

            // Add columns
            this.columnElements = contentProvider.getElements(true);
            for (Object columnElement : columnElements) {
                GridColumn column = new GridColumn(this, columnElement);
                createChildColumns(column);
            }
            // Invalidate columns structure
            boolean hasChildColumns = false;
            for (Iterator<GridColumn> iter = columns.iterator(); iter.hasNext(); ) {
                GridColumn column = iter.next();
                if (column.getParent() == null) {
                    topColumns.add(column);
                } else {
                    hasChildColumns = true;
                }
                if (column.getChildren() != null) {
                    iter.remove();
                }
            }
            if (hasChildColumns) {
                // Rebuild columns model
                columnElements = new Object[columns.size()];
                for (int i = 0; i < columns.size(); i++) {
                    columnElements[i] = columns.get(i).getElement();
                }
            }

            scrollValuesObsolete = true;

            if (getColumnCount() == 1 && MAXIMIZE_SINGLE_COLUMN) {
                // Here we going to maximize single column to entire grid's width
                // Sometimes (when new grid created and filled with data very fast our client area size is zero
                // So let's add a workaround for it and use column's width in this case
                GridColumn column = getColumn(0);
                int columnWidth = column.computeHeaderWidth();
                int gridWidth = getCurrentOrLastClientArea().width - getRowHeaderWidth() - getHScrollSelectionInPixels() - getVerticalBar().getSize().x;
                if (gridWidth > columnWidth) {
                    columnWidth = gridWidth;
                }
                column.setWidth(columnWidth);
            } else {
                int totalWidth = 0;
                for (GridColumn curColumn : topColumns) {
                    curColumn.pack(false);
                    totalWidth += curColumn.getWidth();
                }
                // If grid width more than screen - lets narrow too long columns
                int clientWidth = getCurrentOrLastClientArea().width;
                if (totalWidth > clientWidth) {
                    int normalWidth = 0;
                    List<GridColumn> fatColumns = new ArrayList<>();
                    for (GridColumn curColumn : columns) {
                        if (curColumn.getWidth() > maxColumnDefWidth) {
                            fatColumns.add(curColumn);
                        } else {
                            normalWidth += curColumn.getWidth();
                        }
                    }
                    if (!fatColumns.isEmpty()) {
                        // Narrow fat columns on decWidth
                        int freeSpace = (clientWidth - normalWidth - getBorderWidth() - rowHeaderWidth - vScroll.getWidth())
                            / fatColumns.size();
                        int newFatWidth = (freeSpace > maxColumnDefWidth ? freeSpace : maxColumnDefWidth);
                        for (GridColumn curColumn : fatColumns) {
                            curColumn.setWidth(newFatWidth);
                        }
                    }
                }
            }
        }
        // Recalculate indexes, sizes and update scrollbars
        topIndex = -1;
        bottomIndex = -1;
        recalculateSizes();
        updateScrollbars();

        // Restore state
        if (savedFocus != null) {
            savedFocus.row = Math.min(savedFocus.row, getItemCount() - 1);
            savedFocus.col = Math.min(savedFocus.col, getColumnCount() - 1);
            if (savedFocus.row >= 0) setFocusItem(savedFocus.row);
            if (savedFocus.col >= 0) setFocusColumn(savedFocus.col);
            if (savedFocus.isValid()) selectCell(savedFocus);
        }
        if (savedHSB >= 0) {
            hScroll.setSelection(Math.min(hScroll.getMaximum(), savedHSB));
        }
        if (savedVSB >= 0) {
            vScroll.setSelection(Math.min(vScroll.getMaximum(), savedVSB));
        }
//        // Add focus cell to selection
//        GridPos focusPos = getFocusPos();
//        if (focusPos.isValid()) {
//            selectCell(focusPos);
//        }
    }

    public void refreshRowsData() {
        // Prepare rows
        Object[] initialElements = getContentProvider().getElements(false);
        this.rowNodes.clear();
        List<Object> realRows = new ArrayList<>(initialElements.length);
        List<GridNode> parents = new ArrayList<>(initialElements.length);
        collectRows(realRows, parents, null, initialElements, 0);
        this.rowElements = realRows.toArray();
        this.parentNodes = parents.toArray(new GridNode[parents.size()]);
    }

    /**
     * Returns current or last client area.
     * If Grid controls are stacked then only the top is visible and has real client area.
     * So we cache it - all stack has the same client area
     */
    private Rectangle getCurrentOrLastClientArea() {
        Rectangle clientArea = getClientArea();
        if (clientArea.width == 0) {
            if (lastClientArea == null) {
                return clientArea;
            }
            return lastClientArea;
        }
        lastClientArea = clientArea;
        return clientArea;
    }

    private void createChildColumns(GridColumn parent) {
        Object[] children = getContentProvider().getChildren(parent.getElement());
        if (children != null) {
            for (Object child : children) {
                GridColumn column = new GridColumn(parent, child);
                createChildColumns(column);
            }
        }
        this.maxColumnDepth = Math.max(this.maxColumnDepth, parent.getLevel());
    }

    @Nullable
    public GridCell posToCell(GridPos pos)
    {
        if (pos.col < 0 || pos.row < 0) {
            return null;
        }
        return new GridCell(columnElements[pos.col], rowElements[pos.row]);
    }

    @NotNull
    public GridPos cellToPos(GridCell cell)
    {
        int colIndex = ArrayUtils.indexOf(columnElements, cell.col);
        int rowIndex = ArrayUtils.indexOf(rowElements, cell.row);
        return new GridPos(colIndex, rowIndex);
    }

    public Object getColumnElement(int col) {
        return columnElements[col];
    }

    public Rectangle getColumnBounds(int col) {
        return getColumn(col).getBounds();
    }

    public Object getRowElement(int row) {
        return rowElements[row];
    }

    @Override
    public Color getBackground()
    {
        if (backgroundColor == null) {
            backgroundColor = super.getBackground();
        }
        return backgroundColor;
    }

    @Override
    public void setBackground(Color color)
    {
        super.setBackground(backgroundColor = color);
    }

    ///////////////////////////////////
    // Just caching because native impl creates new objects and called too often

    @Override
    public Color getForeground() {
        if (foregroundColor == null) {
            foregroundColor = super.getForeground();
        }
        return foregroundColor;
    }

    @Override
    public void setForeground(Color color) {
        super.setForeground(foregroundColor = color);
        getContentProvider().resetColors();
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
     * Deselects all selected items in the receiver.  If cell selection is enabled,
     * all cells are deselected.
     */
    public void deselectAll()
    {
        checkWidget();

        selectedCells.clear();
        updateSelectionCache();
        redraw();
    }

    @NotNull
    private GridColumn getColumn(int index)
    {
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
    private GridColumn getColumn(Point point)
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
                for (GridColumn parent = column.getParent(); parent != null; parent = parent.getParent()) {
                    Point parentLoc = getOrigin(parent, -1);
                    if (point.y >= parentLoc.y && point.y <= parentLoc.y + parent.getHeaderHeight(false, false)) {
                        column = parent;
                        break;
                    }
                }
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
        return columns.size();
    }

    Collection<GridColumn> getColumns()
    {
        return columns;
    }

    public IGridScrollBar getHorizontalScrollBarProxy()
    {
        return hScroll;
    }

    public IGridScrollBar getVerticalScrollBarProxy()
    {
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
        return headerHeight;
    }

    private int getRowHeaderWidth()
    {
        return rowHeaderWidth;
    }

    public int getRow(Point point)
    {
        checkWidget();

        if (point == null) {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
            return -1;
        }

        final Rectangle clientArea = getClientArea();
        if (point.x < 0 || point.x > clientArea.width) return -1;

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
        while (row < itemCount && y2 <= clientArea.height) {
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
    private void setItemHeight(int height)
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
    @NotNull
    public Color getLineColor() {
        return lineColor;
    }

    public void setLineColor(@NotNull Color lineColor) {
        this.lineColor = lineColor;
    }

    public Color getLineSelectedColor() {
        return lineSelectedColor;
    }

    public void setLineSelectedColor(Color lineSelectedColor) {
        this.lineSelectedColor = lineSelectedColor;
    }

    /**
     * Returns true if the lines are visible.
     *
     * @return Returns the linesVisible.
     */
    public boolean isLinesVisible()
    {
        return linesVisible;
    }

    /**
     * Returns the next visible item in the table.
     *
     * @return next visible item or null
     */
    private int getNextVisibleItem(int index)
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
    private int getPreviousVisibleItem(int index)
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
    private GridColumn getPreviousVisibleColumn(GridColumn column)
    {
        int index = indexOf(column);
        if (index <= 0)
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
    private GridColumn getNextVisibleColumn(GridColumn column)
    {
        int index = indexOf(column);

        if (index < 0 || index >= columns.size() - 1)
            return null;

        return columns.get(index + 1);
    }

    /**
     * Returns the number of selected cells contained in the receiver.
     *
     * @return the number of selected cells
     */
    private int getCellSelectionCount()
    {
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
     */
    @Nullable
    private RowRange getRowRange(int startIndex, int endIndex)
    {

        // parameter preparation
        int itemCount = getItemCount();
        if (startIndex == -1) {
            // search first visible item
            startIndex = 0;
            if (startIndex == itemCount) return null;
        }
        if (endIndex == -1) {
            // search last visible item
            endIndex = itemCount - 1;
            if (endIndex <= 0) return null;
        }

        // fail fast
        if (startIndex < 0 || endIndex < 0 || startIndex >= itemCount || endIndex >= itemCount
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
    private int getGridHeight()
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
    private int getVisibleGridHeight()
    {
        return getClientArea().height - (columnHeadersVisible ? headerHeight : 0);
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
        column = column.getFirstLeaf();
        int index = columns.indexOf(column);
        if (index < 0) {
            log.warn("Bad column [" + column.getElement() + "]");
        }
        return index;
    }

    /**
     * Returns {@code true} if the receiver's row header is visible, and
     * {@code false} otherwise.
     * <p/>
     *
     * @return the receiver's row header's visibility state
     */
    private boolean isRowHeaderVisible()
    {
        return rowHeaderVisible;
    }

    /**
     * Returns true if the given cell is selected.
     *
     * @param cell cell
     * @return true if the cell is selected.
     */
    private boolean isCellSelected(GridPos cell)
    {
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
        vScroll.setSelection(0);
        hScroll.setSelection(0);

        focusItem = -1;
        focusColumn = null;
        topIndex = -1;
        bottomIndex = -1;
        shiftSelectionAnchorColumn = null;

        topColumns.clear();
        columns.clear();
        columnElements = new Object[0];
        rowElements = new Object[0];
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
    private void setTopIndex(int index)
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
        showColumn(col);
    }

    public void showColumn(Object element)
    {
        for (GridColumn column : columns) {
            if (column.getElement() == element) {
                showColumn(column);
                break;
            }
        }
    }

    private void showColumn(@NotNull GridColumn col)
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
        final Rectangle clientArea = getClientArea();
        if (x >= firstVisibleX
            && (x + col.getWidth()) <= (firstVisibleX + (clientArea.width - firstVisibleX))) {
            return;
        }

        if (!getColumnScrolling()) {
            if (x < firstVisibleX) {
                hScroll.setSelection(getHScrollSelectionInPixels() - (firstVisibleX - x));
            } else {
                if (col.getWidth() > clientArea.width - firstVisibleX) {
                    hScroll.setSelection(getHScrollSelectionInPixels() + (x - firstVisibleX));
                } else {
                    x -= clientArea.width - firstVisibleX - col.getWidth();
                    hScroll.setSelection(getHScrollSelectionInPixels() + (x - firstVisibleX));
                }
            }
        } else {
            if (x < firstVisibleX || col.getWidth() > clientArea.width - firstVisibleX) {
                int sel = indexOf(col);
                hScroll.setSelection(sel);
            } else {
                int availableWidth = clientArea.width - firstVisibleX - col.getWidth();

                GridColumn prevCol = getPreviousVisibleColumn(col);
                GridColumn currentScrollTo = col;

                while (true) {
                    if (prevCol == null || prevCol.getWidth() > availableWidth) {
                        int sel = indexOf(currentScrollTo);
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
     * @param row row number
     * @return true if 'item' is shown
     */
    private boolean isShown(int row)
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
    private void computeHeaderSizes()
    {
        // Item height
        itemHeight = fontMetrics.getHeight() + 3;

        // Column header height
        int colHeaderHeight = 0;
        for (GridColumn column : topColumns) {
            colHeaderHeight = Math.max(column.getHeaderHeight(true, true), colHeaderHeight);
        }
        headerHeight = colHeaderHeight;

        // Row header width
        rowHeaderWidth = DEFAULT_ROW_HEADER_WIDTH;
        for (int i = 0; i < rowElements.length; i++) {
            Object row = rowElements[i];
            GridNode parentNode = parentNodes[i];
            GridNode nr = rowNodes.get(row);
            int width = rowHeaderRenderer.computeHeaderWidth(
                row, nr != null ? nr.level : parentNode == null ? 0 : parentNode.level + 1);
            rowHeaderWidth = Math.max(rowHeaderWidth, width);
        }
        if (rowHeaderWidth > MAX_ROW_HEADER_WIDTH) {
            rowHeaderWidth = MAX_ROW_HEADER_WIDTH;
        }
    }

    /**
     * Returns the x position of the given column. Takes into account scroll
     * position.
     *
     * @param column given column
     * @return x position
     */
    private int getColumnHeaderXPosition(@NotNull GridColumn column)
    {
        int x = 0;

        x -= getHScrollSelectionInPixels();

        if (rowHeaderVisible) {
            x += rowHeaderWidth;
        }
        column = column.getFirstLeaf();
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

        Rectangle clientArea = getClientArea();
        if (columnScrolling) {
            int maxWidth = clientArea.width;
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

        redraw(clientArea.x, clientArea.y, clientArea.width, clientArea.height, false);
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
        boolean overSorter = false, overResizer = false, overFilter = false;
        hoveringOnHeader = false;
        if (y <= headerHeight) {
            int x2 = 0;

            if (rowHeaderVisible) {
                x2 += rowHeaderWidth;
            }

            x2 -= getHScrollSelectionInPixels();

            if (x < x2) {
                int ltSort = getContentProvider().getSortOrder(null);
                if (ltSort != SWT.NONE 
                		&& x > x2 - GridColumnRenderer.SORT_WIDTH - GridColumnRenderer.ARROW_MARGIN 
                		&& x < x2 - GridColumnRenderer.ARROW_MARGIN 
                		&& y > GridColumnRenderer.TOP_MARGIN)
                {
                    columnBeingSorted = null;
                    overSorter = true;
                }

                
            } else {
                if (x > getRowHeaderWidth()) {
                    for (GridColumn column : columns) {
                        if (x >= x2 && x <= x2 + column.getWidth()) {
                            hoveringOnHeader = true;
                            if (column.isOverSortArrow(x - x2, y)) {
                                overSorter = true;
                                columnBeingSorted = column;
                                break;
                            }
                            
                            if(column.isOverFilterButton(x - x2, y)) {
                            	columnBeingFiltered = column;
                            	overFilter = true;
                            	break;
                            }
                            
                            x2 += column.getWidth();
                            if (x2 >= (x - COLUMN_RESIZER_THRESHOLD) && x2 <= (x + COLUMN_RESIZER_THRESHOLD)) {
                                overResizer = true;

                                columnBeingResized = column;
                                break;
                            }
                        } else {
                            x2 += column.getWidth();
                        }
                    }
                }
            }
            // Redraw header
//            GC gc = new GC(this);
//            try {
//                paintHeader(gc);
//            } catch (Exception e) {
//                gc.dispose();
//            }

        } else if (x <= rowHeaderWidth) {
            // Hover in row header
        }
        if (overSorter != hoveringOnColumnSorter) {
            if (overSorter) {
                setCursor(sortCursor);
            } else {
                columnBeingSorted = null;
                setCursor(null);
            }
            hoveringOnColumnSorter = overSorter;
        }
        
        if(overFilter != hoveringOnColumnFilter) {
        	if(overFilter) 
        		setCursor(sortCursor);        	
        	else if(!overSorter) {
        		columnBeingFiltered = null;
        		setCursor(null);
        	}
        		
        	hoveringOnColumnFilter = overFilter;
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

        if (hoveringOnHeader && !overSorter && !overResizer && !overFilter) {
            hoveringOnHeaderDragArea = true;
        } else {
            hoveringOnHeaderDragArea = false;
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
            return new GridPos(indexOf(column), item);
        } else {
            return null;
        }
    }

    /**
     * Paints.
     *
     * @param e paint event
     */
    private void onPaint(@NotNull PaintEvent e)
    {
        final GC gc = e.gc;
        gc.setBackground(getBackground());

        //this.drawBackground(gc, 0, 0, getSize().x, getSize().y);

        if (scrollValuesObsolete) {
            updateScrollbars();
            scrollValuesObsolete = false;
        }

        int y = 0;

        if (columnHeadersVisible) {
            paintHeader(gc);
            y += headerHeight;
        }

        final Rectangle clientArea = getClientArea();
        int availableHeight = clientArea.height - y;
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
        final Rectangle cellBounds = new Rectangle(0, 0, 0, 0);

        for (int i = 0; i < visibleRows; i++) {

            int x = 0;

            x -= hScrollSelectionInPixels;

            // get the item to draw
            if (row >= 0 && row < getItemCount()) {

                boolean cellInRowSelected = selectedRows.containsKey(row);

                if (rowHeaderVisible) {

                    // row header is actually painted later
                    x += rowHeaderWidth;
                }

                // draw regular cells for each column
                for (int k = 0, columnsSize = columns.size(); k < columnsSize; k++) {
                    GridColumn column = columns.get(k);

                    int width = column.getWidth();

                    if (x + width >= 0 && x < clientArea.width) {

                        cellBounds.x = x;
                        cellBounds.y = y;
                        cellBounds.width = width;
                        cellBounds.height = getItemHeight();

                        testPos.col = k;
                        testPos.row = row;
                        cellRenderer.paint(
                            gc,
                            cellBounds,
                            selectedCells.contains(testPos),
                            focusItem == row && focusColumn == column,
                            column.getElement(),
                            rowElements[row]);

                        //gc.setClipping((Rectangle) null);
                    }

                    x += column.getWidth();
                }

                if (x < clientArea.width) {
                    drawEmptyCell(gc, x, y, clientArea.width - x + 1, getItemHeight());
                }

                x = 0;

                GridNode rowNode = this.rowNodes.get(rowElements[row]);
                GridNode parentNode = this.parentNodes[row];
                if (rowHeaderVisible) {

                    if (y >= headerHeight) {
                        cellBounds.x = 0;
                        cellBounds.y = y;
                        cellBounds.width = rowHeaderWidth;
                        cellBounds.height = getItemHeight() + 1;

                        gc.setClipping(cellBounds);
                        try {
                            rowHeaderRenderer.paint(
                                gc,
                                cellBounds,
                                cellInRowSelected,
                                parentNode == null ? 0 : parentNode.level,
                                rowNode == null ? IGridContentProvider.ElementState.NONE : rowNode.state,
                                rowElements[row]);
                        } finally {
                            gc.setClipping((Rectangle)null);
                        }
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
                    drawEmptyCell(gc, x, y, column.getWidth(), getItemHeight());
                    x += column.getWidth();
                }
                if (x < clientArea.width) {
                    drawEmptyCell(gc, x, y, clientArea.width - x + 1, getItemHeight());
                }

                x = 0;

                if (rowHeaderVisible) {
                    drawEmptyRowHeader(gc, x, y, rowHeaderWidth, getItemHeight() + 1);
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
    private void paintHeader(@NotNull GC gc)
    {
        int x = 0;
        int y;

        x -= getHScrollSelectionInPixels();

        if (rowHeaderVisible) {
            // skip left corner
            x += rowHeaderWidth;
        }

        final Rectangle clientArea = getClientArea();
        for (int i = 0, columnsSize = topColumns.size(); i < columnsSize; i++) {
            GridColumn column = topColumns.get(i);
            if (x > clientArea.width)
                break;

            int columnHeight = column.getHeaderHeight(false, false);
            y = 0;
            if (x + column.getWidth() >= 0) {
                paintColumnsHeader(gc, column, x, y, columnHeight, 0);
            }

            x += column.getWidth();
        }

        if (x < clientArea.width) {
            drawEmptyColumnHeader(gc, x, 0, clientArea.width - x, headerHeight);
        }

        x = 0;

        if (rowHeaderVisible) {
            // paint left corner
            drawTopLeftCell(gc, 0, 0, rowHeaderWidth, headerHeight);
            x += rowHeaderWidth;
        }
    }

    private void paintColumnsHeader(GC gc, @NotNull GridColumn column, int x, int y, int columnHeight, int level) {
        List<GridColumn> children = column.getChildren();
        int paintHeight = columnHeight;
        if (CommonUtils.isEmpty(children)) {
            paintHeight = columnHeight * (maxColumnDepth - level + 1);
        }
        Rectangle bounds = new Rectangle(x, y, column.getWidth(), paintHeight);
        boolean hover = hoveringOnHeader && hoveringColumn == column;
        columnHeaderRenderer.paint(gc, bounds, selectedColumns.contains(column), hover, column.getElement());
        if (!CommonUtils.isEmpty(children)) {
            // Draw child columns
            level++;
            int childX = x;
            for (GridColumn child : children) {
                paintColumnsHeader(gc, child, childX, y + columnHeight, columnHeight, level);
                childX += child.getWidth();
            }
        }
    }

    /**
     * Manages the state of the scrollbars when new items are added or the
     * bounds are changed.
     */
    public void updateScrollbars()
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
    private Event updateCellSelection(
            @NotNull GridPos newCell,
            int stateMask,
            boolean dragging,
            boolean reverseDuplicateSelections,
            EventSource eventSource)
    {
        return updateCellSelection(Collections.singletonList(newCell), stateMask, dragging, reverseDuplicateSelections, eventSource);
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
        @NotNull List<GridPos> newCells,
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

        List<GridPos> oldSelection = null;
        if (!shift && !ctrl) {
            if (newCells.size() == 1 &&
                newCells.size() == selectedCells.size() &&
                newCells.get(0).equals(selectedCells.iterator().next()))
            {
                return null;
            }

            selectedCells.clear();
            for (GridPos newCell : newCells) {
                addToCellSelection(newCell);
            }

        } else if (shift) {

            GridPos newCell = newCells.get(0); //shift selection should only occur with one cell, ignoring others
            oldSelection = new ArrayList<>(selectedCells);

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
                        int index = indexOf(currentColumn) + 1;

                        if (index < columns.size()) {
                            currentColumn = columns.get(index);
                        } else {
                            currentColumn = null;
                        }

                        if (currentColumn != null)
                            if (indexOf(currentColumn) > indexOf(endColumn))
                                currentColumn = null;
                    }

                    firstLoop2 = false;

                    if (currentColumn != null) {
                        GridPos cell = new GridPos(indexOf(currentColumn), currentItem);
                        addToCellSelection(cell);
                    }
                } while (currentColumn != endColumn && currentColumn != null);
            } while (currentItem != endItem);

            if (selectedCells.equals(newCells)) {
                return null;
            }

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
        if (oldSelection != null && oldSelection.size() == selectedCells.size() && selectedCells.containsAll(oldSelection)) {
            return null;
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

    private boolean addToCellSelection(GridPos newCell)
    {
        if (newCell.col < 0 || newCell.col >= columns.size())
            return false;

        if (newCell.row < 0 || newCell.row >= getItemCount())
            return false;

        return selectedCells.add(newCell);
    }

    private void updateSelectionCache()
    {
        //Update the list of which columns have all their cells selected
        selectedColumns.clear();
        selectedRows.clear();

        IntKeyMap<Boolean> columnIndices = new IntKeyMap<>();
        for (GridPos cell : selectedCells) {
            columnIndices.put(cell.col, Boolean.TRUE);
            selectedRows.put(cell.row, Boolean.TRUE);
        }
        for (Integer columnIndex : columnIndices.keySet()) {
            selectedColumns.add(columns.get(columnIndex));
        }
        Collections.sort(selectedColumns, new Comparator<GridColumn>() {
            @Override
            public int compare(GridColumn o1, GridColumn o2) {
                return o1.getIndex() - o2.getIndex();
            }
        });
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
        removeAll();
        //We only want to dispose of our items and such *after* anybody else who may have been
        //listening to the dispose has had a chance to do whatever.
        removeListener(SWT.Dispose, disposeListener);
        notifyListeners(SWT.Dispose, event);
        event.type = SWT.None;
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
        
        if(hoveringOnColumnFilter) {
        	handleHoverOnColumnHeader(e.x, e.y);
        	 if (hoveringOnColumnFilter) {
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

        Point point = new Point(e.x, e.y);
        int row = getRow(point);

        if (isListening(SWT.DragDetect)) {

            if (hoveringOnHeaderDragArea && hoveringColumn != null) {
                if (e.button == 1 && hoveringColumn.isOverIcon(e.x, e.y)) {
                    if (dragDetect(e)) {
                        // Drag and drop started
                        headerColumnDragStarted = true;
                        return;
                    }
                }
            }
        }
        headerColumnDragStarted = false;

        GridColumn col = null;
        if (row >= 0) {
            col = getColumn(point);
            boolean isSelectedCell = false;
            if (col != null) {
                isSelectedCell = selectedCells.contains(new GridPos(col.getIndex(), row));
            }

            if (col == null && rowHeaderVisible && e.x <= rowHeaderWidth) {
                boolean shift = ((e.stateMask & SWT.MOD2) != 0);
                boolean ctrl = false;
                if (!shift) {
                    ctrl = ((e.stateMask & SWT.MOD1) != 0);
                }

                if (e.button == 1 && !shift && !ctrl) {
                    GridNode node = rowNodes.get(rowElements[row]);
                    GridNode parentNode = parentNodes[row];
                    if (node != null && node.state != IGridContentProvider.ElementState.NONE) {
                        if (GridRowRenderer.isOverExpander(e.x, parentNode == null ? 0 : parentNode.level))
                        {
                            toggleRowState(row);
                            return;
                        }
                    }
                }
                List<GridPos> cells = new ArrayList<>();

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
                //return;
            } else if (e.button == 1 || (e.button == 3 && col != null && !isSelectedCell)) {
                if (col != null) {
                    selectionEvent = updateCellSelection(new GridPos(col.getIndex(), row), e.stateMask, false, true, EventSource.MOUSE);
                    // Trigger selection event always!
                    // It makes sense if grid content was changed but selection remains the same
                    // If user clicks on the same selected cell value - selection event will trigger value redraw in panels
                    selectionEvent = new Event();
                    cellSelectedOnLastMouseDown = (getCellSelectionCount() > 0);

                    if (e.stateMask != SWT.MOD2) {
                        focusColumn = col;
                        focusItem = row;
                    }
                    //showColumn(col);
                    showItem(row);
                    redraw();
                }
            } else {
                return;
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
            col = getColumn(point);

            if (col == null) return;

            if (getItemCount() == 0)
                return;

            List<GridPos> cells = new ArrayList<>();
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
            GridColumn column = getColumn(point);
            if (column == null) {
                // Clicked on top-left cell or outside of grid
                return;
            }
            focusColumn = column;
        }

        if (selectionEvent != null) {
            selectionEvent.stateMask = e.stateMask;
            selectionEvent.button = e.button;
            selectionEvent.data = new GridCell(col == null ? null : col.getElement(), row < 0 ? null : rowElements[row]);
            selectionEvent.x = e.x;
            selectionEvent.y = e.y;
            notifyListeners(SWT.Selection, selectionEvent);
        }


    }

    private void toggleRowState(int row) {
        GridNode node = rowNodes.get(rowElements[row]);
        if (node == null || node.state == IGridContentProvider.ElementState.NONE) {
            log.error("Row [" + row + "] state can't be toggled");
            return;
        }
        if (node.state == IGridContentProvider.ElementState.EXPANDED) {
            // Collapse node. Remove all elements with different parent
            int deleteTo;
            for (deleteTo = row + 1; deleteTo < rowElements.length; deleteTo++) {
                if (!node.isParentOf(parentNodes[deleteTo])) {
                    break;
                }
            }
            rowElements = ArrayUtils.deleteArea(Object.class, rowElements, row + 1, deleteTo - 1);
            parentNodes = ArrayUtils.deleteArea(GridNode.class, parentNodes, row + 1, deleteTo - 1);
            node.state = IGridContentProvider.ElementState.COLLAPSED;
        } else {
            // Expand node
            List<Object> result = new ArrayList<>();
            List<GridNode> parents = new ArrayList<>();
            collectRows(result, parents, node, node.rows, node.level);
            rowElements = ArrayUtils.insertArea(Object.class, rowElements, row + 1, result.toArray());
            parentNodes = ArrayUtils.insertArea(GridNode.class, parentNodes, row + 1, parents.toArray());
            node.state = IGridContentProvider.ElementState.EXPANDED;
        }

        if (focusItem > row) {
            focusItem = row;
        }
        for (Iterator<GridPos> iter = selectedCells.iterator(); iter.hasNext(); ) {
            GridPos pos = iter.next();
            if (pos.row > row) {
                iter.remove();
            }
        }
        updateSelectionCache();
        computeHeaderSizes();
        this.scrollValuesObsolete = true;
        redraw();
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
                columnBeingResized.pack(true);
                resizingColumn = false;
                handleHoverOnColumnHeader(e.x, e.y);
                redraw();
                return;
            }

            Point point = new Point(e.x, e.y);
            int row = getRow(point);
            GridColumn col = getColumn(point);
            if (row >= 0) {
                if (col != null) {
                    if (isListening(SWT.DefaultSelection)) {
                        Event newEvent = new Event();
                        newEvent.data = new GridCell(col.getElement(), rowElements[row]);

                        notifyListeners(SWT.DefaultSelection, newEvent);
                    }
                } else {
                    GridNode node = rowNodes.get(rowElements[row]);
                    GridNode parentNode = parentNodes[row];
                    if (node != null && node.state != IGridContentProvider.ElementState.NONE) {
                        if (!GridRowRenderer.isOverExpander(e.x, parentNode == null ? 0 : parentNode.level))
                        {
                            toggleRowState(row);
                        }
                    }
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
        if (focusColumn != null && focusItem >= 0) {
            if (e.button == 1 && cellRenderer.isOverLink(focusColumn, focusItem, e.x, e.y)) {
                // Navigate link
                Event event = new Event();
                event.x = e.x;
                event.y = e.y;
                event.stateMask = e.stateMask;
                event.data = new GridCell(focusColumn.getElement(), rowElements[focusItem]);
                notifyListeners(Event_NavigateLink, event);
                return;
            }
        }

        cellSelectedOnLastMouseDown = false;

        if (hoveringOnColumnSorter) {
            handleHoverOnColumnHeader(e.x, e.y);
            if (hoveringOnColumnSorter) {
                if (e.button == 1) {
                    Event event = new Event();
                    event.x = e.x;
                    event.y = e.y;
                    event.data = columnBeingSorted == null ? null : columnBeingSorted.getElement();
                    event.stateMask = e.stateMask;
                    notifyListeners(Event_ChangeSort, event);
                    return;
                }
            }
        }

        if (hoveringOnColumnFilter) {
            handleHoverOnColumnHeader(e.x, e.y);
            if (hoveringOnColumnFilter) {
                if (e.button == 1) {
                    Event event = new Event();
                    event.x = e.x;
                    event.y = e.y;
                    event.data = columnBeingFiltered == null ? null : columnBeingFiltered.getElement();
                    event.stateMask = e.stateMask;
                    notifyListeners(Event_FilterColumn, event);
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
                int rowIndex = getRow(point);
                if (column != null && rowIndex >= 0) {
                    se.data = new GridCell(column.getElement(), rowElements[rowIndex]);
                }
                se.stateMask = e.stateMask;
                se.x = e.x;
                se.y = e.y;
                se.detail = SWT.DROP_DOWN;

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
                    GridPos newCell = new GridPos(intentColumn.getIndex(), intentItem);
                    selectionEvent = updateCellSelection(newCell, ctrlFlag | SWT.MOD2, true, false, EventSource.MOUSE);
                }
                if (cellRowDragSelectionOccurring && handleCellHover(e.x, e.y)) {
                    int intentItem = hoveringItem;

                    if (hoveringItem < 0) {
                        if (e.y > headerHeight) {
                            //then we must be hovering way to the bottom
                            intentItem = getTopIndex() + 1;
                        } else {
                            if (getTopIndex() > 0) {
                                intentItem = getTopIndex() - 1;
                            } else {
                                intentItem = 0;
                            }
                        }
                    }

                    List<GridPos> cells = new ArrayList<>();

                    getCells(intentItem, focusItem, cells);

                    showItem(intentItem);
                    selectionEvent = updateCellSelection(cells, ctrlFlag, true, false, EventSource.MOUSE);
                }
                final GridColumn prevHoveringColumn = hoveringColumn;
                if (cellColumnDragSelectionOccurring && handleCellHover(e.x, e.y)) {
                    boolean dragging;
                    List<GridPos> newSelected = new ArrayList<>();

                    GridColumn iterCol = hoveringColumn;
                    if (iterCol != null) {
                        boolean decreasing = (indexOf(iterCol) > indexOf(focusColumn));
                        dragging = true;

                        while (iterCol != null) {
                            getCells(iterCol, newSelected);

                            if (iterCol == focusColumn) {
                                break;
                            }

                            if (decreasing) {
                                iterCol = getPreviousVisibleColumn(iterCol);
                            } else {
                                iterCol = getNextVisibleColumn(iterCol);
                            }

                        }
                    } else {
                        dragging = false;
                        if (e.x <= rowHeaderWidth) {
                            GridColumn prev = prevHoveringColumn == null ? null : getPreviousVisibleColumn(prevHoveringColumn);
                            if (prev != null) {
                                showColumn(prev);
                                getCells(prev, newSelected);
                                ctrlFlag = SWT.MOD1;
                            }
                        } else {

                        }

                    }

                    selectionEvent = updateCellSelection(newSelected, ctrlFlag, dragging, false, EventSource.MOUSE);
                }

            }
        }

        if (selectionEvent != null) {
            selectionEvent.stateMask = e.stateMask;
            selectionEvent.button = e.button;
            Point point = new Point(e.x, e.y);
            GridColumn column = getColumn(point);
            int rowIndex = getRow(point);
            if (column != null && rowIndex >= 0) {
                selectionEvent.data = new GridCell(column.getElement(), rowElements[rowIndex]);
            }
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
        handleCellHover(x, y);

        if (columnHeadersVisible) {
            handleHoverOnColumnHeader(x, y);
        }
    }

    /**
     * Refreshes the hover* variables according to the mouse location and
     * current state of the table. This is useful is some method call, caused
     * the state of the table to change and therefore the hover effects may have
     * become out of date.
     */
    private void refreshHoverState()
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
        hoveringDetail = null;
        hoveringColumn = null;
        redraw();
    }

    /**
     * Key down event handler.
     *
     * @param e event
     */
    private void onKeyDown(Event e)
    {
        if (focusColumn == null) {
            if (columns.size() == 0)
                return;

            focusColumn = getColumn(0);
        }

        if (e.character == '\r' && focusItem >= 0 && focusItem < rowElements.length) {
            Event newEvent = new Event();
            newEvent.data = new GridCell(focusColumn.getElement(), rowElements[focusItem]);

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
                    if (impliedFocusItem >= 0) {
                        newSelection = impliedFocusItem;

                        int index = indexOf(impliedFocusColumn) + 1;

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
                    if (impliedFocusItem >= 0) {
                        newSelection = impliedFocusItem;

                        int index = indexOf(impliedFocusColumn);

                        if (index > 0) {
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

                newColumnFocus = impliedFocusColumn;

                break;
            case SWT.ARROW_DOWN:
                if (impliedFocusItem >= 0) {
                    newSelection = getNextVisibleItem(impliedFocusItem);
                } else {
                    if (getItemCount() > 0) {
                        newSelection = 0;
                    }
                }

                newColumnFocus = impliedFocusColumn;
                break;
            case SWT.HOME:
                if (ctrlPressed || columns.size() == 1) {
                    newSelection = 0;
                } else {
                    newSelection = impliedFocusItem;
                }
                newColumnFocus = columns.get(0);

                break;
            case SWT.END:
                {
                    if ((ctrlPressed || columns.size() == 1) && getItemCount() > 0) {
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

                newColumnFocus = impliedFocusColumn;
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

                newColumnFocus = impliedFocusColumn;
                //newColumnFocus = focusColumn;
                break;
            case '+':
            case '-':
            case '=':
            case SWT.KEYPAD_ADD:
            case SWT.KEYPAD_SUBTRACT:
                if (focusItem >= 0) {
                    GridNode node = rowNodes.get(rowElements[focusItem]);
                    if (node != null) {
                        boolean isPlus = (e.keyCode == '+' || e.keyCode == '=' || e.keyCode == SWT.KEYPAD_ADD);
                        if ((node.state == IGridContentProvider.ElementState.EXPANDED && !isPlus) ||
                            (node.state == IGridContentProvider.ElementState.COLLAPSED && isPlus))
                        {
                            toggleRowState(focusItem);
                        }
                    }
                }
                break;
            default:
                break;
        }

        if (newSelection < 0) {
            return;
        }

        if (newColumnFocus != null) {
            //if (e.stateMask != SWT.MOD1) {
            Event selEvent = updateCellSelection(
                new GridPos(newColumnFocus.getIndex(), newSelection),
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

            GridCell newPos;
            if (newSelection >= 0 && newSelection < rowElements.length) {
                newPos = new GridCell(newColumnFocus.getElement(), rowElements[newSelection]);
            } else {
                newPos = null;
            }
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
        final Rectangle clientArea = getClientArea();
        redraw(clientArea.x, clientArea.y, clientArea.width, clientArea.height, false);
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

        for (int i = 0; i < columns.size(); i++) {
            GridColumn colIter = columns.get(i);
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
        } else if (columnHeadersVisible && column.getParent() != null) {
            for (GridColumn parent = column.getParent(); parent != null; parent = parent.getParent()) {
                y += parent.getHeaderHeight(false, false);
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
        Point point = new Point(x, y);
        final GridColumn col = getColumn(point);
        final int row = getRow(point);
        Integer detail = y;

        boolean hoverChange = false;

        if (hoveringItem != row || !CommonUtils.equalObjects(hoveringDetail, detail) || hoveringColumn != col) {
            hoveringItem = row;
            hoveringDetail = detail;
            hoveringColumn = col;

            hoverChange = true;
        }

        // Check for link
        boolean overLink = false;
        if (col != null && row >= 0) {
            if (cellRenderer.isOverLink(col, row, x, y)) {
                overLink = true;
            }
        }

        if (overLink) {
            if (!hoveringOnLink) {
                setCursor(sortCursor);
            }
        } else if (hoveringOnLink) {
            setCursor(null);
        }
        hoveringOnLink = overLink;

        //do normal cell specific tooltip stuff
        if (hoverChange) {

            // Check tooltip
            String newTip = null;
            if ((hoveringItem >= 0) && (hoveringColumn != null)) {
                // get cell specific tooltip
                newTip = getCellToolTip(hoveringColumn, hoveringItem);
            } else if (columnHeadersVisible && hoveringColumn != null && y <= headerHeight) {
                // get column header specific tooltip
                newTip = hoveringColumn.getHeaderTooltip();
            } else if (rowHeaderVisible && hoveringItem >= 0 && x <= rowHeaderWidth) {
                newTip = getLabelProvider().getToolTipText(getRowElement(hoveringItem));
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
     * @param text  tooltip text
     */
    private void updateToolTipText(@Nullable String text)
    {
        ToolTipHandler curHandler = this.toolTipHandler;
        if (!CommonUtils.equalObjects(prevToolTip, text)) {
            // New tooltip
            if (curHandler != null) {
                curHandler.cancel();
            }
            prevToolTip = text;
            this.setToolTipText("");
            this.toolTipHandler = new ToolTipHandler();
            this.toolTipHandler.toolTip = text;
            this.toolTipHandler.schedule(500);
        }
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
     */
    void newColumn(GridColumn column, int index)
    {

        if (index == -1) {
            columns.add(column);
        } else {
            columns.add(index, column);
        }
    }

    public void recalculateSizes() {
        int oldHeaderHeight = headerHeight;
        computeHeaderSizes();
        if (oldHeaderHeight != headerHeight) {
            scrollValuesObsolete = true;
        }
    }

    /**
     * Returns the current cell in focus.  If cell selection is disabled, this method returns null.
     *
     * @return cell in focus or {@code null}. x represents the column and y the row the cell is in
     */
    public GridPos getFocusPos()
    {
        checkWidget();

        int x = -1;

        if (focusColumn != null)
            x = focusColumn.getIndex();
        focusCell.col = x;
        focusCell.row = focusItem;
        return focusCell;
    }

    @Nullable
    public Object getFocusColumnElement() {
        return focusColumn == null ? null : focusColumn.getElement();
    }

    @Nullable
    public Object getFocusRowElement() {
        if (focusItem < 0 || focusItem >= rowElements.length) {
            return null;
        }
        return rowElements[focusItem];
    }

    @Nullable
    public GridCell getFocusCell()
    {
        return posToCell(getFocusPos());
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
        if (column == null || column.getGrid() != this) {
            SWT.error(SWT.ERROR_INVALID_ARGUMENT);
            return;
        }

        focusColumn = column;
    }

    public void resetFocus() {
        focusColumn = null;
        focusItem = -1;
    }

    /**
     * Returns true if the table is set to horizontally scroll column-by-column
     * rather than pixel-by-pixel.
     *
     * @return true if the table is scrolled horizontally by column
     */
    private boolean getColumnScrolling()
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
    private void setColumnScrolling(boolean columnScrolling)
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
     * Selects the given cell.  Invalid cells are ignored.
     *
     * @param cell point whose x values is a column index and y value is an item index
     */
    public void selectCell(@NotNull GridPos cell)
    {
        checkWidget();

        addToCellSelection(cell);
        updateSelectionCache();
        redraw();
    }

    /**
     * Selects the given cells.  Invalid cells are ignored.
     *
     * @param cells an array of points whose x value is a column index and y value is an item index
     */
    public void selectCells(@NotNull Collection<GridPos> cells)
    {
        checkWidget();

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

        List<GridPos> cells = new ArrayList<>();
        getAllCells(cells);
        Event selectionEvent = updateCellSelection(cells, stateMask, false, true, EventSource.KEYBOARD);

        focusColumn = oldFocusColumn;
        focusItem = oldFocusItem;

        updateSelectionCache();

        redraw();

        return selectionEvent;
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

    public List<GridCell> getCellSelection()
    {
        if (isDisposed() || selectedCells.isEmpty()) {
            return Collections.emptyList();
        }
        List<GridCell> cells = new ArrayList<>(selectedCells.size());
        for (GridPos pos : selectedCells) {
            cells.add(posToCell(pos));
        }
        return cells;
    }

    @NotNull
    public List<Object> getColumnSelection()
    {
        if (selectedColumns.isEmpty()) {
            return Collections.emptyList();
        }
        List<Object> selection = new ArrayList<>();
        for (GridColumn col : selectedColumns) {
            selection.add(col.getElement());
        }
        return selection;
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
        if (col.getChildren() != null) {
            // Get cells for all leafs
            for (int i = 0; i < columns.size(); i++) {
                if (columns.get(i).isParent(col)) {
                    for (int k = 0; k < getItemCount(); k++) {
                        cells.add(new GridPos(i, k));
                    }
                }
            }
        } else {
            int colIndex = col.getIndex();

            for (int i = 0; i < getItemCount(); i++) {
                cells.add(new GridPos(colIndex, i));
            }
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
        List<GridPos> cells = new ArrayList<>();
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

    /**
     * Returns a point whose x and y values are the to and from column indexes of the new selection
     * range inclusive of all spanned columns.
     */
    private Point getSelectionRange(int fromItem, GridColumn fromColumn, int toItem, GridColumn toColumn)
    {
        if (indexOf(fromColumn) > indexOf(toColumn)) {
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
     * @param cell cell
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



    @Override
    public void setFont(Font font)
    {
        super.setFont(font);
        sizingGC.setFont(font);
        fontMetrics = sizingGC.getFontMetrics();
        normalFont = font;
    }

    public String getCellText(Object colElement, Object rowElement)
    {
        String text = getContentProvider().getCellText(colElement, rowElement);
        // Truncate too long texts (they are really bad for performance)
        if (text.length() > MAX_TOOLTIP_LENGTH) {
            text = text.substring(0, MAX_TOOLTIP_LENGTH) + " ...";
        }

        return text;
    }

    @Nullable
    private String getCellToolTip(GridColumn col, int row)
    {
        String toolTip = getCellText(columnElements[col.getIndex()], rowElements[row]);
        if (toolTip == null) {
            return null;
        }
        // Show tooltip only if it's larger than column width
        Point ttSize = sizingGC.textExtent(toolTip);
        if (ttSize.x > col.getWidth() || ttSize.y > getItemHeight()) {
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
            return "";
        }
    }

    @Nullable
    public DBPImage getCellImage(Object colElement, Object rowElement)
    {
        return getContentProvider().getCellImage(colElement, rowElement);
    }

    public Color getCellBackground(Object colElement, Object rowElement, boolean selected)
    {
        Color color = getContentProvider().getCellBackground(colElement, rowElement, selected);
        return color != null ? color : getBackground();
    }

    public Color getCellForeground(Object colElement, Object rowElement, boolean selected)
    {
        Color color = getContentProvider().getCellForeground(colElement, rowElement, selected);
        return color != null ? color : getForeground();
    }

    public Rectangle getCellBounds(int columnIndex, int rowIndex) {
        if (!isShown(rowIndex))
            return new Rectangle(-1000, -1000, 0, 0);

        GridColumn column = getColumn(columnIndex);
        Point origin = getOrigin(column, rowIndex);

        if (origin.x < 0 && isRowHeaderVisible())
            return new Rectangle(-1000, -1000, 0, 0);

        return new Rectangle(origin.x, origin.y, column.getWidth(), getItemHeight());
    }

    void setDefaultBackground(GC gc) {
        Color background = getLabelProvider().getBackground(null);
        if (background != null) {
            gc.setBackground(background);
        }
    }

    private void drawEmptyColumnHeader(GC gc, int x, int y, int width, int height)
    {
        gc.setBackground(getContentProvider().getCellHeaderBackground());

        gc.fillRectangle(
            x, 
            y, 
            width + 1,
            height + 1);
    }

    private void drawEmptyRowHeader(GC gc, int x, int y, int width, int height)
    {
        gc.setBackground(getContentProvider().getCellHeaderBackground());

        gc.fillRectangle(x, y, width, height + 1);

        gc.setForeground(getContentProvider().getCellHeaderForeground());

        gc.drawLine(
            x + width - 1,
            y,
            x + width - 1,
            y + height - 1);
        gc.drawLine(
            x,
            y + height - 1,
            x + width - 1,
            y + height - 1);
    }

    private void drawEmptyCell(GC gc, int x, int y, int width, int height) {
        IGridLabelProvider labelProvider = getLabelProvider();
        Color foreground = labelProvider.getForeground(null);
        setDefaultBackground(gc);
        gc.setForeground(foreground);

        gc.fillRectangle(x, y, width + 1, height);

        if (isLinesVisible()) {
            gc.setForeground(getLineColor());
            gc.drawLine(
                x,
                y + height,
                x + width,
                y + height);
            gc.drawLine(x + width - 1,
                y,
                x + width - 1,
                y + height);
        }
    }

    private void drawTopLeftCell(GC gc, int x, int y, int width, int height) {
        int sortOrder = getContentProvider().getSortOrder(null);
        gc.setBackground(getContentProvider().getCellHeaderBackground());

        gc.fillRectangle(
            x,
            y,
            width - 1,
            height + 1);

        gc.setForeground(getContentProvider().getCellHeaderForeground());

        gc.drawLine(
            x + width - 1,
            y,
            x + width - 1,
            y + height);

        gc.drawLine(
            x,
            y + height - 1,
            x + width,
            y + height - 1);

        if (sortOrder != SWT.NONE) {
            int arrowWidth = GridColumnRenderer.SORT_WIDTH;
            Rectangle sortBounds = new Rectangle(
                x + width - GridColumnRenderer.ARROW_MARGIN - arrowWidth,
                y + GridColumnRenderer.TOP_MARGIN,
                arrowWidth,
                height);
            GridColumnRenderer.paintSort(gc, sortBounds, sortOrder);
        }
    }

    /////////////////////////////////////////////////////////////////////////////////
    // DnD
    /////////////////////////////////////////////////////////////////////////////////

    private void addDragAndDropSupport()
    {
        final int operations = DND.DROP_MOVE;//DND.DROP_COPY | DND.DROP_MOVE | DND.DROP_LINK | DND.DROP_DEFAULT;

        final DragSource source = new DragSource(this, operations);
        source.setTransfer(new Transfer[] { GridColumnTransfer.INSTANCE });
        source.addDragListener (new DragSourceListener() {

            private Image dragImage;
            private long lastDragEndTime;

            @Override
            public void dragStart(DragSourceEvent event) {
                if (hoveringColumn == null || !headerColumnDragStarted ||
                    (lastDragEndTime > 0 && System.currentTimeMillis() - lastDragEndTime < 100))
                {
                    event.doit = false;
                } else {
                    draggingColumn = hoveringColumn;
                    Rectangle columnBounds = hoveringColumn.getBounds();
                    GC gc = new GC(LightGrid.this);
                    dragImage = new Image(Display.getCurrent(), columnBounds.width, columnBounds.height);
                    gc.copyArea(
                            dragImage,
                            columnBounds.x,
                            columnBounds.y);
                    event.image = dragImage;
                    gc.dispose();
                }
            }

            @Override
            public void dragSetData (DragSourceEvent event) {
            }
            @Override
            public void dragFinished(DragSourceEvent event) {
                draggingColumn = null;
                if (dragImage != null) {
                    UIUtils.dispose(dragImage);
                    dragImage = null;
                }
                lastDragEndTime = System.currentTimeMillis();
            }
        });

        DropTarget dropTarget = new DropTarget(this, operations);
        dropTarget.setTransfer(new Transfer[] {GridColumnTransfer.INSTANCE});
        dropTarget.addDropListener(new DropTargetListener() {
            @Override
            public void dragEnter(DropTargetEvent event)
            {
                handleDragEvent(event);
            }

            @Override
            public void dragLeave(DropTargetEvent event)
            {
                handleDragEvent(event);
            }

            @Override
            public void dragOperationChanged(DropTargetEvent event)
            {
                handleDragEvent(event);
            }

            @Override
            public void dragOver(DropTargetEvent event)
            {
                handleDragEvent(event);
            }

            @Override
            public void drop(DropTargetEvent event)
            {
                handleDragEvent(event);
                if (event.detail == DND.DROP_MOVE) {
                    moveColumns(event);
                }
            }

            @Override
            public void dropAccept(DropTargetEvent event)
            {
                handleDragEvent(event);
            }

            private void handleDragEvent(DropTargetEvent event)
            {
                if (!isDropSupported(event)) {
                    event.detail = DND.DROP_NONE;
                } else {
                    event.detail = DND.DROP_MOVE;
                }
                event.feedback = DND.FEEDBACK_SELECT;
            }

            private boolean isDropSupported(DropTargetEvent event)
            {
                if (!hoveringOnHeaderDragArea) {
                    return false;
                }
                if (draggingColumn == null || draggingColumn.getGrid() != LightGrid.this) {
                    return false;
                }
                GridColumn overColumn = getOverColumn(event);
                return draggingColumn != overColumn;
            }

            private GridColumn getOverColumn(DropTargetEvent event) {
                Point dragPoint = getDisplay().map(null, LightGrid.this, new Point(event.x, event.y));
                return getColumn(dragPoint);
            }

            private void moveColumns(DropTargetEvent event)
            {
                GridColumn overColumn = getOverColumn(event);
                if (draggingColumn == null || draggingColumn == overColumn) {
                    return;
                }
                IGridController gridController = getGridController();
                if (gridController != null) {
                    IGridController.DropLocation location;// = IGridController.DropLocation.SWAP;

                    Point dropPoint = getDisplay().map(null, LightGrid.this, new Point(event.x, event.y));
                    Rectangle columnBounds = overColumn.getBounds();
                    if (dropPoint.x > columnBounds.x + columnBounds.width / 2) {
                        location = IGridController.DropLocation.DROP_AFTER;
                    } else {
                        location = IGridController.DropLocation.DROP_BEFORE;
                    }
                    gridController.moveColumn(draggingColumn.getElement(), overColumn.getElement(), location);
                }
                draggingColumn = null;
            }
        });
    }


    public final static class GridColumnTransfer extends LocalObjectTransfer<GridColumn> {

        private static final GridColumnTransfer INSTANCE = new GridColumnTransfer();
        private static final String TYPE_NAME = "LighGrid.GridColumn Transfer" + System.currentTimeMillis() + ":" + INSTANCE.hashCode();//$NON-NLS-1$
        private static final int TYPEID = registerType(TYPE_NAME);

        private GridColumnTransfer() {
        }

        @Override
        protected int[] getTypeIds() {
            return new int[] { TYPEID };
        }

        @Override
        protected String[] getTypeNames() {
            return new String[] { TYPE_NAME };
        }

    }


}


