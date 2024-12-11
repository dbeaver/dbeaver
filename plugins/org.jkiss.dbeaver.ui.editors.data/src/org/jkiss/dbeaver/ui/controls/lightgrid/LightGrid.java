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

import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIElementFontStyle;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomToolTipHandler;
import org.jkiss.dbeaver.ui.dnd.LocalObjectTransfer;
import org.jkiss.dbeaver.ui.editors.data.internal.DataEditorsMessages;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IntKeyMap;

import java.util.List;
import java.util.*;

/**
 * LightGrid
 * initially based on Nebula grid. Refactored and mostly redone.
 *
 * @author serge@dbeaver.com
 * @author chris.gross@us.ibm.com
 */
public abstract class LightGrid extends Canvas {

    private static final Log log = Log.getLog(LightGrid.class);

    private static final int MAX_CELL_TEXT_LENGTH = 1000;
    private static final int MAX_TOOLTIP_LENGTH = 250;

    protected static final int Event_ChangeSort = 1000;
    protected static final int Event_NavigateLink = 1001;
    protected static final int Event_FilterColumn = 1002;

    /**
     * Horizontal scrolling increment, in pixels.
     */
    private static final int HORZ_SCROLL_INCREMENT = 12;

    /**
     * The area to the left and right of the column boundary/resizer that is
     * still considered the resizer area. This prevents a user from having to be
     * *exactly* over the resizer.
     */
    private static final int COLUMN_RESIZER_THRESHOLD = 4;
    private static final int DEFAULT_ROW_HEADER_WIDTH = 30;
    private static final int MIN_ROW_HEADER_WIDTH = 40;
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

    public enum EventSource {
        MOUSE,
        KEYBOARD,
    }

    static class CellExpandState {
        final int size;
        boolean expanded;

        public CellExpandState(int size) {
            this.size = size;
        }
    }

    static class RowExpandState {
        private final Map<IGridColumn, CellExpandState> columns = new HashMap<>();

        public int getMaxLength() {
            int size = 0;

            for (CellExpandState state : columns.values()) {
                if (state.expanded) {
                    size = Math.max(size, state.size);
                }
            }

            return size;
        }

        public boolean isColumnExpanded(@NotNull IGridColumn column) {
            final CellExpandState state = columns.get(column);
            return state != null && state.expanded;
        }

        public boolean isAnyColumnExpanded() {
            for (CellExpandState state : columns.values()) {
                if (state.expanded) {
                    return true;
                }
            }
            return false;
        }

        public boolean isAllColumnsExpanded() {
            // FIXME: Doesn't work if the containing row has collection attributes whose values are NULL
            for (CellExpandState state : columns.values()) {
                if (!state.expanded) {
                    return false;
                }
            }
            return true;
        }
    }

    static class RowLocation {
        int[] location;

        public RowLocation(IGridRow row) {
            if (row.getParent() == null) {
                this.location = new int[] { row.getRelativeIndex() };
            } else {
                this.location = new int[row.getLevel() + 1];
                int index = this.location.length - 1;
                for (IGridRow r = row; r != null; r = r.getParent()) {
                    this.location[index--] = r.getRelativeIndex();
                }
            }
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof RowLocation &&
                Arrays.equals(this.location, ((RowLocation)obj).location);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(this.location);
        }

        @Override
        public String toString() {
            return Arrays.toString(this.location);
        }
    }

    private record RedrawCell(int row, GridColumn column) {
    }

    // Last calculated client area
    private volatile static Rectangle lastClientArea;

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

    private boolean headerColumnDragStarted, rowHeaderDragStarted;

    private GridColumn shiftSelectionAnchorColumn;

    private GridColumn focusColumn;
    private final GridPos focusCell = new GridPos(-1, -1);

    /**
     * List of table columns in creation/index order.
     */
    private final List<GridColumn> topColumns = new ArrayList<>();
    private final List<GridColumn> columns = new ArrayList<>();
    private int maxColumnDepth = 0;
    protected IGridRow[] gridRows = new IGridRow[0];
    private final Map<RowLocation, RowExpandState> expandedRows = new HashMap<>();

    private int maxColumnDefWidth = 1000;

    private final GridColumnRenderer columnHeaderRenderer;
    private final GridRowRenderer rowHeaderRenderer;
    private final GridCellRenderer cellRenderer;

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
    private boolean hoveringOnColumnIcon = false;
    private boolean hoveringOnColumnSorter = false;
    private boolean hoveringOnColumnFilter = false;
    private boolean hoveringOnLink = false;
    private boolean hoveringOnRowHeader = false;
    private boolean hoveringOnRowExpander = false;
    private boolean isColumnContextMenuShouldBeShown = false;

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
    private Integer hoveringRow;
    private Integer draggingRow;

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

    // Indicates that last time refreshData was called grid control was hidden (had zero size)
    // In that case columns will be repacked even if keepState is true
    private boolean controlWasHidden;

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

    FontMetrics fontMetrics;
    Font normalFont;
    Font boldFont;
    Font italicFont;
    Font commentFont;

    @NotNull
    private Color lineColor;
    private Color lineSelectedColor;
    private Color backgroundColor;
    private Color foregroundColor;
    @NotNull
    private Cursor sortCursor;

    private final CustomToolTipHandler toolTipHandler;

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
        // ? do we need it? It may improve performance a bit (as drawBackground is relatively slow.
        // https://www.eclipse.org/forums/index.php/t/146489/
        // | SWT.NO_BACKGROUND;
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

        GC sizingGC = new GC(this);
        fontMetrics = sizingGC.getFontMetrics();
        sizingGC.dispose();

        normalFont = getFont();
        boldFont = UIUtils.makeBoldFont(normalFont);
        italicFont = UIUtils.modifyFont(normalFont, SWT.ITALIC);
        commentFont = UIUtils.modifyFontSize(italicFont, -1);

        columnHeaderRenderer = new GridColumnRenderer(this);
        rowHeaderRenderer = new GridRowRenderer(this);
        cellRenderer = new GridCellRenderer(this);

        final Display display = getDisplay();
        lineColor = JFaceColors.getErrorBackground(display);
        lineSelectedColor = JFaceColors.getErrorBorder(display);//SWT.COLOR_WIDGET_DARK_SHADOW;

        sortCursor = display.getSystemCursor(SWT.CURSOR_HAND);

        if ((style & SWT.MULTI) != 0) {
            selectionType = SWT.MULTI;
        }

        if (getVerticalBar() != null) {
            getVerticalBar().setVisible(false);
            vScroll = new ScrollBarAdapter(getVerticalBar(), true);
        } else {
            vScroll = new NullScrollBar();
        }

        if (getHorizontalBar() != null) {
            getHorizontalBar().setVisible(false);
            hScroll = new ScrollBarAdapter(getHorizontalBar(), false);
        } else {
            hScroll = new NullScrollBar();
        }

        scrollValuesObsolete = true;

        toolTipHandler = new CustomToolTipHandler(this);

        initListeners();

        recalculateSizes(true);

        addDragAndDropSupport();
        setDragDetect(false);
    }

    @NotNull
    public abstract IGridContentProvider getContentProvider();

    @NotNull
    public abstract IGridLabelProvider getLabelProvider();

    @Nullable
    public abstract IGridController getGridController();

    public GridCellRenderer getCellRenderer() {
        return cellRenderer;
    }

    public void setMaxColumnDefWidth(int maxColumnDefWidth) {
        this.maxColumnDefWidth = maxColumnDefWidth;
    }

    private void collectRowsFromElements(
        List<IGridRow> result,
        Object[] elements)
    {
        int index = 0;
        for (int i = 0; i < elements.length; i++) {
            Object element = elements[i];
            if (element == null) {
                continue;
            }
            IGridRow row = new GridRow(element, i, index);
            result.add(row);
            index++;

            index = collectNestedRows(result, row, index, 1);
        }
    }

    private int collectNestedRows(List<IGridRow> result, IGridRow parentRow, int position, int level) {
        if (expandedRows.isEmpty()) {
            return position;
        }
        final RowExpandState rowState = expandedRows.get(new RowLocation(parentRow));
        if (rowState == null || !rowState.isAnyColumnExpanded()) {
            return position;
        }

        int childrenCount = rowState.getMaxLength();
        Object[] children = getContentProvider().getChildren(parentRow);
        if (children != null && children.length != childrenCount) {
            log.warn("Child elements doesn't match calculated count: " + children.length + "<>" + childrenCount + ". Skip the tail.");
            childrenCount = children.length;
        }

        for (int i = 0; i < childrenCount; i++) {
            // In multi-column grid children elements are null (e.g. array elements)
            // In this case use parent row element
            Object element = children == null ? parentRow.getElement() : children[i];

            IGridRow nestedRow = new GridRowNested(parentRow, position++, i, level, element);
            result.add(nestedRow);

            position = collectNestedRows(result, nestedRow, position, level + 1);
        }
        return position;
    }

    /**
     * Refresh grid data
     */
    public void refreshData(boolean refreshColumns, boolean keepState, boolean fitValue)
    {
        GridPos savedFocus = keepState ? getFocusPos() : null;
        int savedHSB = keepState ? hScroll.getSelection() : -1;
        int savedVSB = keepState ? vScroll.getSelection() : -1;

        Map<Object, Integer> oldWidths = null;
        if (keepState && !controlWasHidden) {
            // Save widths
            oldWidths = new HashMap<>();
            if (!columns.isEmpty()) {
                for (GridColumn column : columns) {
                    oldWidths.put(column.getElement(), column.getWidth());
                }
            }
        }
        if (controlWasHidden) {
            refreshColumns = true;
        }
        controlWasHidden = getClientArea().height == 0;

        if (refreshColumns) {
            this.removeAll();
        } else {
            this.deselectAll();
            topIndex = -1;
            bottomIndex = -1;
        }
        IGridContentProvider contentProvider = getContentProvider();

        Object[] columnElements = null;
        if (refreshColumns) {
            this.maxColumnDepth = 0;

            // Add columns
            columnElements = contentProvider.getElements(true);
            for (Object columnElement : columnElements) {
                GridColumn column = new GridColumn(this, columnElement);
                createChildColumns(column);
            }
        }
        refreshRowsData();
        this.displayedToolTipText = null;

        if (refreshColumns) {
            GC sizingGC = new GC(this);
            // Invalidate columns structure
            boolean hasChildColumns = false, hasPinnedColumns = false;
            for (Iterator<GridColumn> iter = columns.iterator(); iter.hasNext(); ) {
                GridColumn column = iter.next();
                if (column.getParent() == null) {
                    topColumns.add(column);
                    column.setPinIndex(contentProvider.getColumnPinIndex(column));
                    if (column.isPinned()) {
                        hasPinnedColumns = true;
                    }
                } else {
                    hasChildColumns = true;
                }
                if (column.getChildren() != null) {
                    iter.remove();
                }
            }

            if (hasPinnedColumns) {
                // Order respecting pinned state
                Comparator<GridColumn> pinnedComparator = (o1, o2) -> o1.isPinned() == o2.isPinned() ?
                    (o1.getPinIndex() - o2.getPinIndex()) : (o1.isPinned() ? -1 : 1);
                columns.sort(pinnedComparator);
                topColumns.sort(pinnedComparator);
            }

            if (hasChildColumns || hasPinnedColumns) {
                // Rebuild columns model
                columnElements = new Object[columns.size()];
                for (int i = 0; i < columns.size(); i++) {
                    columnElements[i] = columns.get(i).getElement();
                }
            }

            scrollValuesObsolete = true;

            if (getColumnCount() == 1 && (fitValue || getGridController().isMaximizeSingleColumn())) {
                // Here we going to maximize single column to entire grid's width
                // Sometimes (when new grid created and filled with data very fast our client area size is zero
                // So let's add a workaround for it and use column's width in this case
                recalculateSizes(true);
                GridColumn column = getColumn(0);
                int columnWidth = column.computeHeaderWidth(sizingGC);
                int gridWidth = getCurrentOrLastClientArea().width - getRowHeaderWidth() - getHScrollSelectionInPixels() - getVerticalBar().getSize().x;
                if (gridWidth > columnWidth) {
                    columnWidth = gridWidth;
                }
                column.setWidth(columnWidth);
            } else {
                int totalWidth = 0;
                for (GridColumn curColumn : topColumns) {
                    curColumn.pack(sizingGC, false);
                    totalWidth += curColumn.getWidth();
                }
                if (!fitValue) {
                    // If grid width more than screen - lets narrow too long columns
                    int clientWidth = getCurrentOrLastClientArea().width;
                    if (totalWidth > clientWidth && clientWidth != 0) {
                        int normalWidth = 0;
                        List<GridColumn> fatColumns = new ArrayList<>();
                        for (GridColumn curColumn : columns) {
                            int curColumnWidthPercent = (int)((curColumn.getWidth() / (double)  clientWidth) * 100);
                            if (CommonUtils.isEmpty(curColumn.getChildren()) && curColumnWidthPercent > maxColumnDefWidth) {
                                fatColumns.add(curColumn);
                            } else {
                                normalWidth += curColumn.getWidth();
                            }
                        }
                        if (!fatColumns.isEmpty()) {
                            // Narrow fat columns on decWidth
                            int freeSpace = (clientWidth - normalWidth - getBorderWidth() - rowHeaderWidth - vScroll.getWidth())
                                / fatColumns.size();
                            int freeSpacePercent = (int) (((double) freeSpace / clientWidth) * 100);
                            int newFatWidth = (freeSpacePercent > maxColumnDefWidth ? freeSpace : (int) ((double) maxColumnDefWidth / 100 * clientWidth));
                            for (GridColumn curColumn : fatColumns) {
                                curColumn.setWidth(newFatWidth);
                            }
                        }
                    }
                }
            }

            if (oldWidths != null) {
                // Restore widths
                if (oldWidths.size() == columns.size()) {
                    for (GridColumn column : columns) {
                        Integer newWidth = oldWidths.get(column.getElement());
                        if (newWidth != null) {
                            column.setWidth(newWidth);
                        }
                    }
                }
            }
            sizingGC.dispose();
        }
        // Recalculate indexes, sizes and update scrollbars
        topIndex = -1;
        bottomIndex = -1;
        recalculateSizes(true);
        updateScrollbars();

        // Restore state
        if (savedFocus != null) {
            int itemCount = getItemCount();
            if (itemCount == 0) {
                savedFocus.row = -1;
            } else {
                savedFocus.row = Math.min(savedFocus.row, itemCount - 1);
            }
            savedFocus.col = Math.min(savedFocus.col, getColumnCount() - 1);
            setFocusItem(savedFocus.row);
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

        List<IGridRow> rows = new ArrayList<>(initialElements.length);
        collectRowsFromElements(rows, initialElements);
        this.gridRows = rows.toArray(new IGridRow[0]);
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
        Object[] children = getContentProvider().getChildren(parent);
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
        if (pos.col >= columns.size() || pos.row >= gridRows.length) {
            return null;
        }
        return new GridCell(columns.get(pos.col), gridRows[pos.row]);
    }

    @NotNull
    public GridPos cellToPos(GridCell cell)
    {
        int colIndex = columns.indexOf(cell.col);
        int rowIndex = ArrayUtils.indexOf(gridRows, cell.row);
        return new GridPos(colIndex, rowIndex);
    }

    public IGridColumn getColumnByElement(Object element) {
        for (IGridColumn col : columns) {
            if (col.getElement() == element) {
                return col;
            }
        }
        return null;
    }

    public Object getColumnElement(int col) {
        return columns.get(col).getElement();
    }

    public int getColumnIndex(int x, int y) {
        Point dragPoint = getDisplay().map(null, LightGrid.this, new Point(x, y));
        GridColumn column = getColumn(dragPoint);
        return column == null ? -1 : column.getIndex();
    }

    public Rectangle getColumnBounds(int col) {
        return getColumn(col).getBounds();
    }

    public IGridRow getRowByElement(int fromIndex, Object element) {
        for (int i = fromIndex; i < gridRows.length; i++) {
            if (gridRows[i].getElement() == element) {
                return gridRows[i];
            }
        }
        return null;
    }

    @Nullable
    public IGridRow getRow(int row) {
        if (row < 0 || row >= gridRows.length) {
            log.debug("Row index out of range (" + row + ")" );
            return null;
        }
        return gridRows[row];
    }

    @Nullable
    public Object getRowElement(int row) {
        final IGridRow object = getRow(row);
        return object != null ? object.getElement() : null;
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
    public GridColumn getColumn(int index)
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

        int x2 = 0;

        if (rowHeaderVisible) {
            if (point.x <= rowHeaderWidth) {
                return null;
            }

            x2 += rowHeaderWidth;
        }

        int pinnedColumnsWidth = getPinnedColumnsWidth();
        if (pinnedColumnsWidth > 0 && point.x <= pinnedColumnsWidth + rowHeaderWidth) {
            return getColumnWithOffset(point, x2, true);
        }

        x2 += pinnedColumnsWidth;
        x2 -= getHScrollSelectionInPixels();

        return getColumnWithOffset(point, x2, false);
    }

    @Nullable
    private GridColumn getColumnWithOffset(Point point, int x2, boolean pinned) {
        GridColumn overThis = null;
        for (GridColumn column : columns) {
            if (column.isPinned() != pinned) {
                continue;
            }
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
        return gridRows.length;
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
            int visibleGridHeight = getVisibleGridHeight();
            RowRange range = getRowRange(getTopIndex(), visibleGridHeight, false, false);

            bottomIndex = range.endIndex;
            bottomIndexShownCompletely = range.height <= visibleGridHeight;
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
        Rectangle clientArea = getClientArea();
        return getVisibleGridHeight(clientArea);
    }

    private int getVisibleGridHeight(Rectangle clientArea) {
        return clientArea.height - (columnHeadersVisible ? headerHeight : 0);
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
        if (column == null) {
            return -1;
        }
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

    public boolean isHoveringOnHeader() {
        return hoveringOnHeader;
    }

    public boolean isHoveringOnRowHeader() {
        return hoveringOnRowHeader;
    }

    public boolean isHoveringOnLink() {
        return hoveringOnLink;
    }

    public boolean isColumnContextMenuShouldBeShown() {
        return isColumnContextMenuShouldBeShown;
    }
    
    public void setColumnContextMenuShouldBeShown(boolean value) {
        isColumnContextMenuShouldBeShown = value;
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
        expandedRows.clear();
        gridRows = new IGridRow[0];
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
        //setColumnScrolling(true);

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
        int pinnedColumnsWidth = getPinnedColumnsWidth();
        if (pinnedColumnsWidth > 0) {
            if (!col.isPinned()) {
                firstVisibleX += pinnedColumnsWidth;
            }
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
    public void showItem(int item) {
        showItem(item, -1);
    }

    public void showItem(int item, int topOffset) {
        checkWidget();

        updateScrollbars();

        // if no items are visible on screen then abort
        if (getVisibleGridHeight() < 1) {
            return;
        }

        if (topOffset >= 0) {
            setTopIndex(Math.max(0, item - topOffset));
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

        if (newTopIndex != topIndex) {
            setTopIndex(newTopIndex);
        }
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
     * @param decreaseSize
     */
    private void computeHeaderSizes(GC gc, boolean decreaseSize)
    {
        bottomIndex = -1;
        int oldRowHeaderWidth = rowHeaderWidth;
        // Item height
        itemHeight = fontMetrics.getHeight() + 3;

        // Column header height
        int colHeaderHeight = 0;
        for (GridColumn column : topColumns) {
            colHeaderHeight = Math.max(column.getHeaderHeight(true, true), colHeaderHeight);
        }
        headerHeight = colHeaderHeight;
        if (headerHeight <= 0) {
            headerHeight = GridColumn.topMargin + fontMetrics.getHeight() + GridColumn.bottomMargin;
        }

        // Row header width
        int newRowHeaderWidth = DEFAULT_ROW_HEADER_WIDTH;
        for (IGridRow row : gridRows) {
            int width = rowHeaderRenderer.computeHeaderWidth(gc, row, row.getLevel());
            newRowHeaderWidth = Math.max(newRowHeaderWidth, width);
        }
        if (newRowHeaderWidth < MIN_ROW_HEADER_WIDTH) {
            newRowHeaderWidth = MIN_ROW_HEADER_WIDTH;
        }
        if (newRowHeaderWidth > MAX_ROW_HEADER_WIDTH) {
            newRowHeaderWidth = MAX_ROW_HEADER_WIDTH;
        }
        if (newRowHeaderWidth > oldRowHeaderWidth || decreaseSize) {
            rowHeaderWidth = newRowHeaderWidth;
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
        boolean overIcon = false;

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

                            if (column.isOverIcon(x, y)) {
                                overIcon = true;
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
        } else if (overIcon != hoveringOnColumnIcon) {
            setCursor(overIcon ? sortCursor : null);
            hoveringOnColumnIcon = overIcon;
        }

        if(overFilter) {
            setCursor(sortCursor);
        }

        if(overFilter != hoveringOnColumnFilter) {
        	if(!overSorter) {
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

    private void handleHoverOnRowHeader(int x, int y) {
        hoveringRow = null;
        draggingRow = null;

        boolean overExpander = false;
        if ((!rowHeaderVisible || y > headerHeight) && x <= rowHeaderWidth) {
            hoveringOnRowHeader = true;
            int row = getRow(new Point(x, y));
            if (row != -1) {
                hoveringRow = row;
                overExpander = GridRowRenderer.isOverExpander(x, getRow(row).getLevel());
            }
        } else {
            hoveringOnRowHeader = false;
        }
        if (overExpander && !hoveringOnRowExpander) {
            setCursor(sortCursor);
        } else if (!overExpander && hoveringOnRowExpander) {
            setCursor(null);
        }
        hoveringOnRowExpander = overExpander;
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

    public int getMaxVisibleRows(){
        int y = 0;

        if (columnHeadersVisible) {
            y += headerHeight;
        }

        final Rectangle clientArea = getClientArea();
        int availableHeight = clientArea.height - y;
        int itemHeight = getItemHeight();
        return availableHeight / itemHeight + 1;
    }

    private int getPinnedColumnsWidth() {
        int x = 0;
        for (int k = 0; k < columns.size(); k++) {
            if (columns.get(k).isPinned()) {
                x += columns.get(k).getWidth();
            } else {
                break;
            }
        }
        return x;
    }

    /**
     * Paints.
     *
     * @param e paint event
     */
    private void onPaint(@NotNull PaintEvent e) {
        final GC gc = e.gc;
        gc.setBackground(getBackground());
        int redrawRow = -1;
        GridColumn redrawColumn = null;
        if (e.data instanceof RedrawCell redrawCell) {
            redrawRow = redrawCell.row;
            redrawColumn = redrawCell.column;
        }
        boolean isSingleCellPaint = redrawRow >= 0 || redrawColumn != null;

        //this.drawBackground(gc, 0, 0, getSize().x, getSize().y);

        if (scrollValuesObsolete) {
            updateScrollbars();
            scrollValuesObsolete = false;
        }

        int y = 0;

        if (columnHeadersVisible) {
            if (!isSingleCellPaint) {
                paintHeader(gc);
            }
            y += headerHeight;
        }

        final Rectangle clientArea = getClientArea();
        int availableHeight = clientArea.height - y;
        int itemHeight = getItemHeight();
        int visibleRows = availableHeight / itemHeight + 1;
        if (getItemCount() > 0 && availableHeight > 0) {
            RowRange range = getRowRange(getTopIndex(), availableHeight, false, false);
            if (range.height >= availableHeight)
                visibleRows = range.rows;
            else
                visibleRows = range.rows + (availableHeight - range.height) / itemHeight + 1;
        }

        int row = getTopIndex();
        final int hScrollSelectionInPixels = getHScrollSelectionInPixels();
        final GridPos testPos = new GridPos(-1, -1);
        final Rectangle cellBounds = new Rectangle(0, 0, 0, 0);
        int pinnedColumnsWidth = getPinnedColumnsWidth();

        for (int i = 0; i < visibleRows; i++) {
            if (isSingleCellPaint && redrawRow != row) {
                row++;
                y += itemHeight + 1;
                continue;
            }
            int x = 0;

            x -= hScrollSelectionInPixels;

            // get the item to draw
            if (row >= 0 && row < getItemCount()) {

                boolean cellInRowSelected = selectedRows.containsKey(row);

                if (rowHeaderVisible) {
                    // row header is actually painted later
                    x += rowHeaderWidth - 1;
                }
                x += pinnedColumnsWidth;

                // draw regular cells for each column
                for (int k = 0, columnsSize = columns.size(); k < columnsSize; k++) {
                    GridColumn column = columns.get(k);
                    if (column.isPinned()) {
                        continue;
                    }

                    int width = column.getWidth();

                    if (redrawColumn == null || redrawColumn == column) {
                        if (x + width >= 0 && x < clientArea.width) {

                            cellBounds.x = x;
                            cellBounds.y = y;
                            cellBounds.width = width;
                            cellBounds.height = itemHeight;

                            testPos.col = k;
                            testPos.row = row;
                            cellRenderer.paint(
                                gc,
                                cellBounds,
                                selectedCells.contains(testPos),
                                focusItem == row && focusColumn == column,
                                hoveringItem == row && hoveringColumn == column,
                                column,
                                gridRows[row]
                            );
                        }
                    }

                    x += width;
                }

                if (x < clientArea.width && !isSingleCellPaint) {
                    drawEmptyCell(gc, x, y, clientArea.width - x + 1, itemHeight);
                }

                x = 0;

                if (rowHeaderVisible) {

                    if (y >= headerHeight && !isSingleCellPaint) {
                        cellBounds.x = 0;
                        cellBounds.y = y;
                        cellBounds.width = rowHeaderWidth;
                        cellBounds.height = itemHeight + 1;

                        gc.setClipping(cellBounds);
                        try {
                            IGridRow gridRow = gridRows[row];
                            rowHeaderRenderer.paint(
                                gc,
                                cellBounds,
                                cellInRowSelected,
                                gridRow.getLevel(),
                                getRowState(gridRow),
                                gridRow);
                        } finally {
                            gc.setClipping((Rectangle)null);
                        }
                    }
                    x += rowHeaderWidth;
                }
                {
                    // paint pinned columns
                    for (int k = 0; k < columns.size(); k++) {
                        GridColumn pc = columns.get(k);
                        if (!pc.isPinned()) {
                            break;
                        }
                        int width = pc.getWidth();
                        if (pinnedColumnsWidth > 0 || redrawColumn == null || redrawColumn == pc) {
                            cellBounds.x = x;
                            cellBounds.y = y;
                            cellBounds.width = width;
                            cellBounds.height = itemHeight;

                            testPos.col = k;
                            testPos.row = row;
                            cellBounds.height++;
                            gc.setClipping(cellBounds);
                            cellBounds.height--;
                            try {
                                cellRenderer.paint(
                                    gc,
                                    cellBounds,
                                    selectedCells.contains(testPos),
                                    focusItem == row && focusColumn == pc,
                                    hoveringItem == row && hoveringColumn == pc,
                                    pc,
                                    gridRows[row]
                                );
                            } finally {
                                gc.setClipping((Rectangle) null);
                            }
                        }
                        x += width;
                    }
                }
            } else if (!isSingleCellPaint) {

                if (rowHeaderVisible) {
                    //row header is actually painted later
                    x += rowHeaderWidth;
                }
                x += pinnedColumnsWidth;

                for (GridColumn column : columns) {
                    if (column.isPinned()) continue;
                    drawEmptyCell(gc, x, y, column.getWidth(), itemHeight);
                    x += column.getWidth();
                }
                if (x < clientArea.width) {
                    drawEmptyCell(gc, x, y, clientArea.width - x + 1, itemHeight);
                }

                x = 0;

                if (rowHeaderVisible) {
                    drawEmptyRowHeader(gc, x, y, rowHeaderWidth, itemHeight + 1);
                    x += rowHeaderWidth;
                }
                for (GridColumn column : columns) {
                    if (!column.isPinned()) break;
                    drawEmptyCell(gc, x, y, column.getWidth(), itemHeight);
                    x += column.getWidth();
                }
            }

            row++;
            y += itemHeight + 1;
        }

        // Draw lines in the end. Do not paint lines to grid cell to optimize performance
        if (this.isLinesVisible() && !isSingleCellPaint) {
            gc.setForeground(this.getLineColor());

            int startY = 0;
            int startX = 0;
            int width = clientArea.width;
            int height = clientArea.height;
            if (rowHeaderVisible) {
                // row header is actually painted later
                startX += rowHeaderWidth - 1;
                width -= rowHeaderWidth;
            }
            if (columnHeadersVisible) {
                startY += headerHeight - 1;
                height -= headerHeight;
            }

            // Draw horizontal lines
            y = startY;
            for (int i = 0; i < visibleRows; i++) {
                y += itemHeight + 1;
                gc.drawLine(startX + 1, y, startX + width, y);
            }

            // Vertical lines
            int leftSpan = getPinnedColumnsWidth();
            if (rowHeaderVisible) {
                leftSpan += rowHeaderWidth;
            }
            int x = startX;
            x -= hScrollSelectionInPixels;

            for (GridColumn column : columns) {
                x += column.getWidth();
                if (x < leftSpan) {
                    continue;
                }
                gc.drawLine(x, startY + 1, x, startY + height);
            }
        }

        if (pinnedColumnsWidth > 0) {
            // draw pin divider
            gc.setForeground(this.getLineSelectedColor());
            gc.drawLine(rowHeaderWidth + pinnedColumnsWidth - 1, 0, rowHeaderWidth + pinnedColumnsWidth - 1, y);
            gc.drawLine(rowHeaderWidth + pinnedColumnsWidth, 0, rowHeaderWidth + pinnedColumnsWidth, y);
        }

        if (!columns.isEmpty() && gridRows.length > 0) {
            int lastRow = row >= gridRows.length ? gridRows.length - 1 : row;
            getContentProvider().validateDataPresence(columns.get(columns.size() - 1), gridRows[lastRow]);
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
        x += getPinnedColumnsWidth();

        final Rectangle clientArea = getClientArea();
        for (int i = 0, columnsSize = topColumns.size(); i < columnsSize; i++) {
            GridColumn column = topColumns.get(i);
            if (column.isPinned()) {
                continue;
            }
            if (x > clientArea.width)
                break;

            y = paintColumnHeader(gc, x, column);

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
        for (GridColumn column : topColumns) {
            if (column.isPinned()) {
                paintColumnHeader(gc, x, column);
                x += column.getWidth();
            } else {
                break;
            }
        }
    }

    private int paintColumnHeader(@NotNull GC gc, int x, GridColumn column) {
        int y;
        int columnHeight = column.getHeaderHeight(false, false);
        y = 0;
        if (x + column.getWidth() >= 0) {
            paintColumnsHeader(gc, column, x, y, columnHeight, 0);
        }
        return y;
    }

    private void paintColumnsHeader(GC gc, @NotNull GridColumn column, int x, int y, int columnHeight, int level) {
        List<GridColumn> children = column.getChildren();
        int paintHeight = columnHeight;
        if (CommonUtils.isEmpty(children)) {
            paintHeight = columnHeight + (headerHeight - y - columnHeight);
        }
        Rectangle bounds = new Rectangle(x, y, column.getWidth(), paintHeight);
        boolean hover = hoveringOnHeader && hoveringColumn == column;
        columnHeaderRenderer.paint(
            gc,
            bounds,
            selectedColumns.contains(column) || focusColumn == column,
            hover,
            column);
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
            int max = getItemCount() + 1;
            int thumb = (getVisibleGridHeight(clientArea) + 1) / (getItemHeight() + 1);

            // if possible, remember selection,
            // if selection is too large, just make it the max you can
            int selection = Math.min(vScroll.getSelection(), max);

            vScroll.setValues(selection, 0, max, thumb, 1, thumb);
        }

        // if the scrollbar is visible set its values
        if (hScroll.getVisible()) {

            if (!columnScrolling) {
                // horizontal scrolling works pixel by pixel

                int hiddenArea = preferredSize.x - clientArea.width + 1 + (vScroll.getVisible() ? vScroll.getWidth() : 0);

                // if possible, remember selection,
                // if selection is too large, just make it the max you can
                int selection = Math.min(hScroll.getSelection(), hiddenArea - 1);

                hScroll.setValues(
                    selection,
                    0,
                    hiddenArea + clientArea.width - 1,
                    clientArea.width,
                    HORZ_SCROLL_INCREMENT,
                    clientArea.width);
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

                // if possible, remember selection,
                // if selection is too large, just make it the max you can
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
        if (RuntimeUtils.isMacOS() && (stateMask & SWT.CTRL) == SWT.CTRL) {
            /*
             * On macOS, Ctrl + Click is a system shortcut that opens a context menu.
             * More than that, the context menu will be opened even if some additional buttons are pressed.
             *
             * There is no need to do anything with the selection in this case, just return.
             *
             * [dbeaver/dbeaver/issues/10725]
             */
            return null;
        }

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
            boolean alt = (stateMask & SWT.MOD3) == SWT.MOD3;
            boolean reverse = reverseDuplicateSelections;
            if (!selectedCells.containsAll(newCells))
                reverse = false;

            if (dragging) {
                selectedCells.clear();
                selectedCells.addAll(selectedCellsBeforeRangeSelect);
            }

            if (reverse) {
                if (alt && newCells.size() == 1) {
                    // Alt pressed - deselect all cells selected in other rows (#6613)
                    int row = newCells.get(0).row;
                    newCells = new ArrayList<>();
                    for (GridColumn col : selectedColumns) {
                        newCells.add(new GridPos(col.getIndex(), row));
                    }
                }
                selectedCells.removeAll(newCells);
            } else {
                if (alt && newCells.size() == 1) {
                    // Alt pressed - select all cells selected in other rows (#5988)
                    int row = newCells.get(0).row;
                    newCells = new ArrayList<>();
                    for (GridColumn col : selectedColumns) {
                        newCells.add(new GridPos(col.getIndex(), row));
                    }
                }
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

//        if (newCell.row < 0 || newCell.row >= getItemCount()) {
//            return false;
//        }

        return selectedCells.add(newCell);
    }

    private void updateSelectionCache()
    {
        //Update the list of which columns have all their cells selected
        selectedColumns.clear();
        selectedRows.clear();

        IntKeyMap<Boolean> columnIndices = new IntKeyMap<>();
        for (GridPos cell : selectedCells) {
            if (cell.col >= 0) {
                columnIndices.put(cell.col, Boolean.TRUE);
            }
            if (cell.row >= 0) {
                selectedRows.put(cell.row, Boolean.TRUE);
            }
        }
        for (Integer columnIndex : columnIndices.keySet()) {
            selectedColumns.add(columns.get(columnIndex));
        }
        selectedColumns.sort(Comparator.comparingInt(GridColumn::getIndex));
    }

    /**
     * Initialize all listeners.
     */
    private void initListeners()
    {
        disposeListener = this::onDispose;
        addListener(SWT.Dispose, disposeListener);

        addPaintListener(this::onPaint);
        addListener(SWT.Resize, e -> onResize());

        if (getVerticalBar() != null) {
            getVerticalBar().addListener(SWT.Selection, e -> onScrollSelection());
        }

        if (getHorizontalBar() != null) {
            getHorizontalBar().addListener(SWT.Selection, e -> onScrollSelection());
        }

        addListener(SWT.KeyDown, this::onKeyDown);

        addTraverseListener(e -> e.doit = true);

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

        addMouseMoveListener(this::onMouseMove);

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
        addListener(SWT.MouseWheel, this::onMouseVerticalWheel);
        addListener(SWT.MouseHorizontalWheel, this::onMouseHorizontalWheel);
    }

    private void onFocusIn() {
        setDefaultFocusRow();
    }

    public void setDefaultFocusRow() {
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

        UIUtils.dispose(boldFont);
        UIUtils.dispose(italicFont);
        UIUtils.dispose(commentFont);
    }

    /**
     * Mouse wheel event handler.
     *
     * @param e event
     */
    private void onMouseVerticalWheel(Event e)
    {
        if (vScroll.getVisible()) {
            vScroll.handleMouseWheel(e);
            if (getVerticalBar() == null)
                e.doit = false;
        } else {
            if (hScroll.getVisible()) {
                hScroll.handleMouseWheel(e);
                if (getHorizontalBar() == null)
                    e.doit = false;
            }
        }
    }

    private void onMouseHorizontalWheel(Event e) {
        if (hScroll.getVisible() & columnScrolling) {
            scrollHorizontally(e.count);
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
            GridPos cell = getCell(new Point(e.x, e.y));
            if (cell == null || !cell.equalsTo(getFocusPos())) {
                // We don't want to call this event if the selected cell equals active one
                // this is related to bug with wayland handling of force focus, which led to editors
                // loosing focus on double click see #16705
                forceFocus();
            }
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
                if (e.button == 1/* && (hoveringColumn.isOverIcon(e.x, e.y) || selectedColumns.contains(hoveringColumn))*/) {
                    if (dragDetect(e)) {
                        // Drag and drop started
                        headerColumnDragStarted = true;
                        return;
                    }
                }
            } else if (hoveringOnRowHeader && hoveringRow != null) {
                if (e.button == 1 && selectedRows.containsKey(hoveringRow) && dragDetect(e)) {
                    rowHeaderDragStarted = true;
                    return;
                }
            }
        }
        headerColumnDragStarted = false;
        rowHeaderDragStarted = false;

        GridColumn col = null;
        if (row >= 0) {
            col = getColumn(point);
            boolean isSelectedCell = false;
            if (col != null && !getContentProvider().isVoidCell(col, gridRows[row])) {
                isSelectedCell = selectedCells.contains(new GridPos(col.getIndex(), row));
            }

            if (col == null && rowHeaderVisible && e.x <= rowHeaderWidth) {
                // Click on header
                boolean shift = ((e.stateMask & SWT.MOD2) != 0);
                boolean ctrl = false;
                if (!shift) {
                    ctrl = ((e.stateMask & SWT.MOD1) != 0);
                }

                if (e.button == 1 && !shift && !ctrl) {
                    IGridRow gridRow = gridRows[row];
                    if (getRowState(gridRow) != IGridContentProvider.ElementState.NONE) {
                        if (GridRowRenderer.isOverExpander(e.x, gridRow.getLevel()))
                        {
                            toggleRowExpand(getRow(row), null);
                            return;
                        }
                    }
                }
                List<GridPos> cells = new ArrayList<>();

                if (e.button == 1) {
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
                } else {
                    focusItem = row;
                }
                showItem(row);
                redraw();
                //return;
            } else if (e.button == 1 || (e.button == 3 && col != null && !isSelectedCell)) {
                if (col != null) {
                    if (e.stateMask != SWT.MOD2) {
                        focusColumn = col;
                        focusItem = row;
                    }

                    //boolean isClickOnLink = focusColumn != null && focusItem != -1 && cellRenderer.isOverLink(focusColumn, focusItem, e.x, e.y);
                    /*if (!isClickOnLink) */{
                        selectionEvent = updateCellSelection(new GridPos(col.getIndex(), row), e.stateMask, false, true, EventSource.MOUSE);
                        // Trigger selection event always!
                        // It makes sense if grid content was changed but selection remains the same
                        // If user clicks on the same selected cell value - selection event will trigger value redraw in panels
                        selectionEvent = new Event();
                        cellSelectedOnLastMouseDown = (getCellSelectionCount() > 0);
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
        } else if (columnHeadersVisible && e.y <= headerHeight) {
            //column cell selection
            col = getColumn(point);

            if (col == null) {
                return;
            }

            if (e.button == 1) {
                List<GridPos> cells = new ArrayList<>();
                getCells(col, cells);
                selectionEvent = updateCellSelection(cells, e.stateMask, false, true, EventSource.MOUSE);
            }

            cellColumnSelectedOnLastMouseDown = (getCellSelectionCount() > 0);

            focusColumn = col;

            showColumn(col);
            redraw();
        } else {
            // Change focus column anyway
            GridColumn column = getColumn(point);
            if (column == null) {
                // Clicked on top-left cell or outside of grid
                return;
            }
            if (getItemCount() > 0) {
                focusColumn = column;
            }
            cellColumnSelectedOnLastMouseDown = true;
        }

        if (selectionEvent != null) {
            selectionEvent.stateMask = e.stateMask;
            selectionEvent.button = e.button;
            selectionEvent.data = new GridCell(
                col, row < 0 ? null : gridRows[row]);
            selectionEvent.x = e.x;
            selectionEvent.y = e.y;
            notifyListeners(SWT.Selection, selectionEvent);
        }
    }

    public boolean isCellExpanded(@NotNull IGridCell cell) {
        return isCellExpanded(cell.getRow(), cell.getColumn());
    }

    public boolean isCellExpanded(@NotNull IGridRow row, @NotNull IGridColumn column) {
        final RowLocation gridPos = new RowLocation(row);
        final RowExpandState rowState = expandedRows.get(gridPos);
        return rowState != null && rowState.isColumnExpanded(column);
    }

    private boolean isRowExpanded(IGridRow gridRow) {
        final RowLocation gridPos = new RowLocation(gridRow);
        final RowExpandState rowState = expandedRows.get(gridPos);
        return rowState != null && rowState.isAllColumnsExpanded();
    }

    @NotNull
    private IGridContentProvider.ElementState getRowState(@NotNull IGridRow row) {
        if (isRowExpanded(row)) {
            return IGridContentProvider.ElementState.EXPANDED;
        }

        if (getContentProvider().isElementExpandable(row)) {
            return IGridContentProvider.ElementState.COLLAPSED;
        }

        return IGridContentProvider.ElementState.NONE;
    }

    public void toggleRowExpand(@NotNull IGridRow gridRow, @Nullable IGridColumn gridColumn) {
        final IGridContentProvider provider = getContentProvider();

        RowLocation gridPos = new RowLocation(gridRow);
        RowExpandState rowState = expandedRows.get(gridPos);

        if (rowState == null) {
            rowState = new RowExpandState();

            for (GridColumn column : columns) {
                if (provider.hasChildren(gridRow) || provider.hasChildren(column)) {
                    final int size = provider.getCollectionSize(column, gridRow);
                    rowState.columns.put(column, new CellExpandState(size));
                }
            }

            expandedRows.put(gridPos, rowState);
        }

        if (gridColumn == null) {
            final boolean wasExpanded = rowState.isAllColumnsExpanded();
            for (CellExpandState state : rowState.columns.values()) {
                state.expanded = !wasExpanded;
            }
        } else {
            final CellExpandState state = rowState.columns.computeIfAbsent(
                gridColumn,
                col -> new CellExpandState(provider.getCollectionSize(col, gridRow))
            );
            // TODO: We also need to collapse this column in all nested rows
            state.expanded = !state.expanded;
        }

        refreshRowsData();
        recalculateSizes(false);
        updateScrollbars();
        redraw();

/*
        GridNode node = rowNodes.get(gridRows[row]);
        if (node == null || node.state == IGridContentProvider.ElementState.NONE) {
            log.error("Row [" + row + "] state can't be toggled");
            return;
        }
        if (node.state == IGridContentProvider.ElementState.EXPANDED) {
            // Collapse node. Remove all elements with different parent
            int deleteTo;
            for (deleteTo = row + 1; deleteTo < gridRows.length; deleteTo++) {
                if (!node.isParentOf(parentNodes[deleteTo])) {
                    break;
                }
            }
            gridRows = ArrayUtils.deleteArea(IGridRow.class, gridRows, row + 1, deleteTo - 1);
            parentNodes = ArrayUtils.deleteArea(GridNode.class, parentNodes, row + 1, deleteTo - 1);
            node.state = IGridContentProvider.ElementState.COLLAPSED;
        } else {
            // Expand node
            List<Object> result = new ArrayList<>();
            List<GridNode> parents = new ArrayList<>();
            collectRows(result, parents, node, node.rows, node.level);
            gridRows = ArrayUtils.insertArea(IGridRow.class, gridRows, row + 1, result.toArray());
            parentNodes = ArrayUtils.insertArea(GridNode.class, parentNodes, row + 1, parents.toArray());
            node.state = IGridContentProvider.ElementState.EXPANDED;
        }

        if (focusItem > row) {
            focusItem = row;
        }
        selectedCells.removeIf(pos -> pos.row > row);
        updateSelectionCache();
        computeHeaderSizes();
        this.scrollValuesObsolete = true;
        redraw();
*/
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
                GC gc = new GC(this);
                columnBeingResized.pack(gc, true);
                gc.dispose();
                resizingColumn = false;
                scrollValuesObsolete = true;
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
                        newEvent.data = new GridCell(col, gridRows[row]);

                        notifyListeners(SWT.DefaultSelection, newEvent);
                    }
                } else {
                    IGridRow gridRow = gridRows[row];
                    if (getRowState(gridRow) != IGridContentProvider.ElementState.NONE) {
                        if (!GridRowRenderer.isOverExpander(e.x, gridRow.getLevel()))
                        {
                            toggleRowExpand(gridRow, null);
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
            if (!hoveringOnHeader && !hoveringOnRowHeader &&
                e.button == 1 && cellRenderer.isOverLink(focusColumn, focusItem, e.x, e.y)
            ) {
                // Navigate link
                Event event = new Event();
                event.x = e.x;
                event.y = e.y;
                event.stateMask = e.stateMask;
                event.data = new GridCell(focusColumn, gridRows[focusItem]);
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
                    se.data = new GridCell(column, gridRows[rowIndex]);
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
    private void onMouseMove(MouseEvent e) {
        List<RedrawCell> redrawCells = new ArrayList<>();
        //if populated will be fired at end of method.
        Event selectionEvent = null;

        if ((e.stateMask & SWT.BUTTON1) == 0) {
            int oldHoverRow = hoveringItem;
            GridColumn oldHoverColumn = hoveringColumn;
            if (handleHovering(e.x, e.y)) {
                Point point = new Point(e.x, e.y);
                GridColumn redrawColumn = getColumn(point);
                int redrawRow = getRow(point);
                if (redrawColumn != null && redrawRow >= 0) {
                    redrawCells.add(new RedrawCell(redrawRow, redrawColumn));
                }
                if (oldHoverColumn != null && oldHoverRow >= 0) {
                    redrawCells.add(new RedrawCell(oldHoverRow, oldHoverColumn));
                }
            }

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
                selectionEvent.data = new GridCell(column, gridRows[rowIndex]);
            }
            selectionEvent.x = e.x;
            selectionEvent.y = e.y;
            notifyListeners(SWT.Selection, selectionEvent);
        } else if (!redrawCells.isEmpty()) {
            // Repaint one cell
            Event se = new Event();
            se.widget = this;
            se.gc = new GC(this);
            try {
                setRedraw(false);
                for (RedrawCell redrawCell : redrawCells) {
                    PaintEvent pe = new PaintEvent(se);
                    pe.data = redrawCell;
                    onPaint(pe);
                }
            } finally {
                setRedraw(true);
                se.gc.dispose();
            }
        }
    }

    /**
     * Handles the assignment of the correct values to the hover* field
     * variables that let the painting code now what to paint as hovered.
     *
     * @param x mouse x coordinate
     * @param y mouse y coordinate
     * @return
     */
    private boolean handleHovering(int x, int y)
    {
        boolean hoverChanged = handleCellHover(x, y);

        if (columnHeadersVisible) {
            handleHoverOnColumnHeader(x, y);
        }
        if (!hoveringOnHeader && rowHeaderVisible) {
            handleHoverOnRowHeader(x, y);
        }

        return hoverChanged;
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
        //redraw();
    }

    public void scrollHorizontally(int count) {
        Rectangle clientArea = getClientArea();
        GridColumn leftColumn = null, rightColumn = null;
        for (GridColumn column : columns) {
            if (column.isPinned()) {
                clientArea.x += column.getWidth();
                continue;
            }
            Rectangle bounds = column.getBounds();
            if (leftColumn == null) {
                if (bounds.x + bounds.width >= clientArea.x) {
                    leftColumn = column;
                }
            } else if (bounds.x + bounds.width >= clientArea.width) {
                rightColumn = column;
                break;
            }
        }
        GridColumn scrollTo = null;
        if (count > 0) {
            if (leftColumn != null) {
                scrollTo = leftColumn;
            }
        } else {
            if (rightColumn != null) {
                scrollTo = rightColumn;
            }
        }
        if (scrollTo != null) {
            showColumn(scrollTo);
        }
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

        if (e.character == '\r' && focusItem >= 0 && focusItem < gridRows.length) {
            Event newEvent = new Event();
            newEvent.data = new GridCell(focusColumn, gridRows[focusItem]);

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
                    IGridRow gridRow = gridRows[focusItem];
                    if (getContentProvider().hasChildren(gridRow)) {
                        boolean isPlus = (e.keyCode == '+' || e.keyCode == '=' || e.keyCode == SWT.KEYPAD_ADD);
                        boolean isExpanded = isCellExpanded(new GridCell(focusColumn, gridRow));
                        if (isExpanded == isPlus) {
                            toggleRowExpand(gridRows[focusItem], focusColumn);
                        }
                    }
                }
                break;
            case ' ':
                toggleCellValue(focusColumn, gridRows[focusItem]);
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

            if (!shiftPressed) {
                focusColumn = newColumnFocus;
            }
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
            if (newSelection >= 0 && newSelection < gridRows.length) {
                newPos = new GridCell(newColumnFocus, gridRows[newSelection]);
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
            // No need to redraw - it is done in showItem
            //redraw();
        }
    }

    protected void toggleCellValue(IGridColumn column, IGridRow row) {

    }

    /**
     * Resize event handler.
     */
    private void onResize() {
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
    Point getOrigin(IGridColumn column, int item)
    {
        int x = 0;

        if (rowHeaderVisible) {
            x += rowHeaderWidth;
        }

        if (!column.isPinned()) {
            x -= getHScrollSelectionInPixels();
        }

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
        Integer detail =
            (hoveringOnColumnSorter ? 1000000 : 0) +
            (hoveringOnColumnFilter ? 1000000 : 0) +
            y;

        boolean hoverChange = false;

        if (hoveringItem != row || !CommonUtils.equalObjects(hoveringDetail, detail) || hoveringColumn != col || y <= headerHeight) {
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
                if (overLink) {
                    newTip = getContentProvider().getCellLinkText(col, gridRows[row]);
                }
                if (CommonUtils.isEmpty(newTip)) {
                    // get cell specific tooltip
                    newTip = getCellToolTip(hoveringColumn, hoveringItem);
                }
            } else if (columnHeadersVisible && hoveringColumn != null && y <= headerHeight) {
                // get column header specific tooltip
                /*if (hoveringOnColumnSorter) {
                    newTip = NLS.bind(DataEditorsMessages.grid_tooltip_sort_by_column, getLabelProvider().getText(hoveringColumn));
                } else */
                if (hoveringOnColumnFilter) {
                    newTip = DataEditorsMessages.pref_page_database_resultsets_label_show_attr_filters;
                } else {
                    newTip = hoveringColumn.getHeaderTooltip();
                }
            } else if (rowHeaderVisible && hoveringItem >= 0 && x <= rowHeaderWidth) {
                newTip = getLabelProvider().getToolTipText(getRow(hoveringItem));
            } else if (rowHeaderVisible && x <= rowHeaderWidth && columnHeadersVisible && y <= headerHeight) {
                // Top-left cell
                StringBuilder status = new StringBuilder();
                for (IGridStatusColumn gsc : getContentProvider().getStatusColumns()) {
                    String statusText = gsc.getStatusText();
                    if (!CommonUtils.isEmpty(statusText)) {
                        if (!status.isEmpty()) {
                            status.append("\n");
                        }
                        status.append(gsc.getDisplayName())
                            .append(": ").append(statusText);
                    }
                }
                newTip = status.toString();
            }

            //Avoid unnecessarily resetting tooltip - this will cause the tooltip to jump around
            if (newTip != null && !newTip.equals(displayedToolTipText)) {
                toolTipHandler.updateToolTipText(newTip);
            } else if (newTip == null && displayedToolTipText != null) {
                toolTipHandler.updateToolTipText(null);
            }
            displayedToolTipText = newTip;
        }

        return hoverChange;
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

    public void recalculateSizes(boolean decreaseSize) {
        int oldHeaderHeight = headerHeight;
        GC gc = new GC(this);
        computeHeaderSizes(gc, decreaseSize);
        gc.dispose();
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
        if (focusItem < 0 || focusItem >= gridRows.length) {
            return null;
        }
        return gridRows[focusItem].getElement();
    }

    @Nullable
    public IGridRow getFocusRow() {
        if (focusItem < 0 || focusItem >= gridRows.length) {
            return null;
        }
        return gridRows[focusItem];
    }

    @Nullable
    public IGridColumn getFocusColumn() {
        return focusColumn;
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
    public void setColumnScrolling(boolean columnScrolling)
    {
        checkWidget();
//        if (rowHeaderVisible && !columnScrolling) {
//            return;
//        }

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

        List<GridPos> cells = getAllCells();
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
        	GridCell cell = posToCell(pos);
        	if (cell != null)
        		cells.add(cell);
        }
        return cells;
    }

    public int getCellSelectionSize() {
        return selectedCells.size();
    }

    @NotNull
    public List<IGridColumn> getColumnSelection() {
        if (selectedColumns.isEmpty()) {
            return Collections.emptyList();
        }
        List<IGridColumn> selection = new ArrayList<>();
        for (GridColumn col : selectedColumns) {
            selection.add(col);
        }
        return selection;
    }

    public int getColumnSelectionSize() {
        return selectedColumns.size();
    }

    public boolean isRowSelected(int row) {
        return selectedRows.containsKey(row);
    }

    /**
     * Returns selected rows indexes
     * @return indexes of selected rows
     */
    public Collection<Integer> getRowSelection()
    {
        return Collections.unmodifiableCollection(selectedRows.keySet());
    }

    public int getRowSelectionSize() {
        return selectedRows.size();
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

    private List<GridPos> getAllCells()
    {
        int itemCount = getItemCount();
        int columnCount = columns.size();
        List<GridPos> cells = new ArrayList<>(itemCount * columnCount);

        for (int i = 0; i < itemCount; i++) {
            for (int k = 0; k < columnCount; k++) {
                cells.add(new GridPos(k, i));
            }
        }
        return cells;
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

        GC sizingGC = new GC(this);
        sizingGC.setFont(font);
        fontMetrics = sizingGC.getFontMetrics();
        sizingGC.dispose();

        normalFont = font;
        UIUtils.dispose(boldFont);
        UIUtils.dispose(italicFont);
        UIUtils.dispose(commentFont);

        boldFont = UIUtils.makeBoldFont(normalFont);
        italicFont = UIUtils.modifyFont(normalFont, SWT.ITALIC);
        commentFont = UIUtils.modifyFontSize(italicFont, -1);

        redraw();
    }

    @NotNull
    public Font getFont(@NotNull UIElementFontStyle style) {
        return switch (style) {
            case ITALIC -> italicFont;
            case BOLD -> boldFont;
            case NORMAL -> normalFont;
        };
    }

    @NotNull
    String getCellText(Object cellValue) {
        String text = String.valueOf(cellValue);
        // Truncate too long texts (they are really bad for performance)
        if (text.length() > MAX_CELL_TEXT_LENGTH) {
            text = text.substring(0, MAX_CELL_TEXT_LENGTH) + "...";
        }

        return text;
    }

    @Nullable
    private String getCellToolTip(GridColumn col, int row) {
        if (col == null || row < 0 || row >= gridRows.length) {
            return null;
        }
        IGridRow gridRow = gridRows[row];
        String toolTip = getContentProvider().getCellToolTip(col, gridRow);
        if (toolTip.length() > MAX_TOOLTIP_LENGTH) {
            toolTip = toolTip.substring(0, MAX_TOOLTIP_LENGTH) + "...";
        }
        //fixme delete code bellow?
        // Show tooltip only if it's larger than column width
        GC sizingGC = new GC(this);
        Point ttSize = sizingGC.textExtent(toolTip);
        sizingGC.dispose();

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
        gc.setBackground(getLabelProvider().getHeaderBackground(null, false));

        gc.fillRectangle(
            x, 
            y, 
            width + 1,
            height + 1);
    }

    private void drawEmptyRowHeader(GC gc, int x, int y, int width, int height)
    {
        gc.setBackground(getLabelProvider().getHeaderBackground(null, false));

        gc.fillRectangle(x, y, width, height + 1);

        gc.setForeground(getLabelProvider().getHeaderBorder(null));

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
    }

    private void drawTopLeftCell(GC gc, int x, int y, int width, int height) {
        int sortOrder = getContentProvider().getSortOrder(null);
        gc.setBackground(getLabelProvider().getHeaderBackground(null, false));

        gc.fillRectangle(
            x,
            y,
            width - 1,
            height + 1);

        gc.setForeground(getLabelProvider().getHeaderBorder(null));

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

        paintTopLeftCellCustom(gc, y);

        {
            int xPos = x + width - GridColumnRenderer.ARROW_MARGIN;
            if (sortOrder != SWT.NONE) {
                xPos -= GridColumnRenderer.SORT_WIDTH + GridColumnRenderer.IMAGE_SPACING;
            }
            IGridStatusColumn[] statusColumns = getContentProvider().getStatusColumns();
            ArrayUtils.reverse(statusColumns);
            for (IGridStatusColumn gsc : statusColumns) {
                DBPImage statusIcon = gsc.getStatusIcon();
                if (statusIcon != null) {
                    Image statusImage = DBeaverIcons.getImage(statusIcon);
                    Rectangle iconBounds = statusImage.getBounds();
                    xPos -= iconBounds.width;
                    gc.drawImage(statusImage, xPos, y + (height - iconBounds.height) / 2);
                    xPos -= 1;
                }
            }
        }

        if (sortOrder != SWT.NONE) {
            Rectangle sortBounds = new Rectangle(
                x + width - GridColumnRenderer.ARROW_MARGIN - GridColumnRenderer.SORT_WIDTH,
                y + (height - GridColumnRenderer.SORT_HEIGHT) / 2,
                GridColumnRenderer.SORT_WIDTH,
                height);
            GridColumnRenderer.paintSort(gc, sortBounds, sortOrder, true);
        }
    }

    protected void paintTopLeftCellCustom(GC gc, int y) {
    }

    /////////////////////////////////////////////////////////////////////////////////
    // DnD
    /////////////////////////////////////////////////////////////////////////////////

    private void addDragAndDropSupport()
    {
        final int operations = DND.DROP_MOVE | DND.DROP_COPY;// | DND.DROP_MOVE | DND.DROP_LINK | DND.DROP_DEFAULT;

        final DragSource source = new DragSource(this, operations);
        source.setTransfer(GridColumnTransfer.INSTANCE, TextTransfer.getInstance());
        source.addDragListener (new DragSourceListener() {

            private Image dragImage;
            private long lastDragEndTime;

            @Override
            public void dragStart(DragSourceEvent event) {
                if (lastDragEndTime > 0 && System.currentTimeMillis() - lastDragEndTime < 100) {
                    event.doit = false;
                } else {
                    Rectangle columnBounds;
                    if (headerColumnDragStarted && hoveringColumn != null) {
                        draggingColumn = hoveringColumn;
                        columnBounds = hoveringColumn.getBounds();
                    } else if (rowHeaderDragStarted && hoveringRow != null) {
                        draggingRow = hoveringRow;
                        int rowFromTop = hoveringRow - getTopIndex();
                        columnBounds = new Rectangle(0, rowFromTop * getItemHeight(), getRowHeaderWidth(), getItemHeight());
                    } else {
                        event.doit = false;
                        return;
                    }
                    if (dragImage != null) {
                        dragImage.dispose();
                        dragImage = null;
                    }
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
                if (draggingColumn != null) {
                    if (GridColumnTransfer.INSTANCE.isSupportedType(event.dataType)) {
                        List<Object> elements = new ArrayList<>();
                        if (isDragSingleColumn()) {
                            elements.add(draggingColumn.getElement());
                        } else {
                            for (GridColumn col : selectedColumns) {
                                elements.add(col.getElement());
                            }
                        }
                        event.data = elements;
                    } else if (TextTransfer.getInstance().isSupportedType(event.dataType)) {
                        // Copy all selected columns
                        if (selectedColumns.size() > 1 && !isDragSingleColumn()) {
                            StringBuilder text = new StringBuilder();
                            for (GridColumn column : selectedColumns) {
                                if (text.length() > 0) text.append(", ");
                                text.append(getLabelProvider().getText(column));
                            }
                            event.data = text.toString();
                        } else {
                            event.data = getLabelProvider().getText(draggingColumn);
                        }
                    }
                } else if (draggingRow != null) {
                    if (GridColumnTransfer.INSTANCE.isSupportedType(event.dataType)) {
                        List<Object> elements = new ArrayList<>();
                        if (isDragSingleRow()) {
                            elements.add(getRowElement(draggingRow));
                        } else {
                            for (Integer row : selectedRows.keySet()) {
                                elements.add(getRowElement(row));
                            }
                        }
                        event.data = elements;
                    } else if (TextTransfer.getInstance().isSupportedType(event.dataType)) {
                        List<GridColumn> columns = selectedColumns;
                        if (columns.isEmpty()) {
                            columns = LightGrid.this.columns;
                        }
                        Set<Integer> rows = selectedRows.keySet();
                        if (rows.isEmpty()) {
                            rows = Collections.singleton(draggingRow);
                        }

                        StringBuilder text = new StringBuilder();
                        for (Integer row : rows) {
                            if (text.length() > 0) text.append("\n");
                            for (int i = 0; i < columns.size(); i++) {
                                GridColumn column = columns.get(i);
                                Object cellText = getContentProvider().
                                    getCellValue(column, getRow(row), true);

                                if (i > 0) text.append(", ");
                                text.append(cellText);
                            }
                        }
                        event.data = text.toString();
                    }
                }
            }
            @Override
            public void dragFinished(DragSourceEvent event) {
                draggingColumn = null;
                draggingRow = null;
                if (dragImage != null) {
                    UIUtils.dispose(dragImage);
                    dragImage = null;
                }
                lastDragEndTime = System.currentTimeMillis();
            }
        });

        DropTarget dropTarget = new DropTarget(this, operations);
        dropTarget.setTransfer(GridColumnTransfer.INSTANCE, TextTransfer.getInstance());
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
                event.feedback = DND.FEEDBACK_SELECT | DND.FEEDBACK_SCROLL;
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
                return overColumn != null && draggingColumn != overColumn && draggingColumn.isPinned() == overColumn.isPinned();
            }

            private GridColumn getOverColumn(DropTargetEvent event) {
                Point dragPoint = getDisplay().map(null, LightGrid.this, new Point(event.x, event.y));
                return getColumn(dragPoint);
            }

            private void moveColumns(DropTargetEvent event)
            {
                GridColumn overColumn = getOverColumn(event);
                if (overColumn == null || draggingColumn == null || draggingColumn == overColumn) {
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

    private boolean isDragSingleColumn() {
        return draggingColumn != null && !selectedColumns.contains(draggingColumn);
    }

    private boolean isDragSingleRow() {
        return draggingRow != null && !selectedRows.containsKey(draggingRow);
    }

    public final static class GridColumnTransfer extends LocalObjectTransfer<List<Object>> {

        public static final GridColumnTransfer INSTANCE = new GridColumnTransfer();
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


