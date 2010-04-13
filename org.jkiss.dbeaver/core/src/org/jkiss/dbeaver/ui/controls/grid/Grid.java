/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.grid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.jkiss.dbeaver.ui.controls.grid.internal.DefaultBottomLeftRenderer;
import org.jkiss.dbeaver.ui.controls.grid.internal.DefaultDropPointRenderer;
import org.jkiss.dbeaver.ui.controls.grid.internal.DefaultEmptyCellRenderer;
import org.jkiss.dbeaver.ui.controls.grid.internal.DefaultEmptyColumnFooterRenderer;
import org.jkiss.dbeaver.ui.controls.grid.internal.DefaultEmptyColumnHeaderRenderer;
import org.jkiss.dbeaver.ui.controls.grid.internal.DefaultEmptyRowHeaderRenderer;
import org.jkiss.dbeaver.ui.controls.grid.internal.DefaultFocusRenderer;
import org.jkiss.dbeaver.ui.controls.grid.internal.DefaultInsertMarkRenderer;
import org.jkiss.dbeaver.ui.controls.grid.internal.DefaultRowHeaderRenderer;
import org.jkiss.dbeaver.ui.controls.grid.internal.DefaultTopLeftRenderer;
import org.jkiss.dbeaver.ui.controls.grid.internal.GridToolTip;
import org.jkiss.dbeaver.ui.controls.grid.internal.IScrollBarProxy;
import org.jkiss.dbeaver.ui.controls.grid.internal.NullScrollBarProxy;
import org.jkiss.dbeaver.ui.controls.grid.internal.ScrollBarProxyAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.accessibility.ACC;
import org.eclipse.swt.accessibility.Accessible;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleControlAdapter;
import org.eclipse.swt.accessibility.AccessibleControlEvent;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.events.TreeEvent;
import org.eclipse.swt.events.TreeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TypedListener;

/**
 * <p>
 * NOTE:  THIS WIDGET AND ITS API ARE STILL UNDER DEVELOPMENT.  THIS IS A PRE-RELEASE ALPHA
 * VERSION.  USERS SHOULD EXPECT API CHANGES IN FUTURE VERSIONS.
 * </p>
 * Instances of this class implement a selectable user interface object that
 * displays a list of images and strings and issue notification when selected.
 * <p>
 * The item children that may be added to instances of this class must be of
 * type {@code GridItem}.
 * </p>
 * <dl>
 * <dt><b>Styles:</b></dt>
 * <dd>SWT.SINGLE, SWT.MULTI, SWT.NO_FOCUS, SWT.CHECK, SWT.VIRTUAL</dd>
 * <dt><b>Events:</b></dt>
 * <dd>Selection, DefaultSelection</dd>
 * </dl>
 *
 * @author chris.gross@us.ibm.com
 */
public class Grid extends Canvas
{
    //TODO: figure out better way to allow renderers to trigger events
    //TODO: scroll as necessary when performing drag select (current strategy ok)
    //TODO: need to refactor the way the range select remembers older selection
    //TODO: remember why i decided i needed to refactor the way the range select remembers older selection
    //TODO: need to alter how column drag selection works to allow selection of spanned cells
    //TODO: JAVADOC!
    //TODO: column freezing

    //TODO: Performance - need to cache top index

	/**
	 * Object holding the visible range
	 */
	public static class GridVisibleRange {
    	private GridItem[] items = new GridItem[0];
    	private GridColumn[] columns = new GridColumn[0];

    	/**
    	 * @return the current items shown
    	 */
		public GridItem[] getItems() {
			return items;
		}

		/**
		 * @return the current columns shown
		 */
		public GridColumn[] getColumns() {
			return columns;
		}
    }

    /**
     * Accessibility default action for column headers and column group headers.
     */
    private static final String ACC_COLUMN_DEFAULT_ACTION = "Click";

    /**
     * Accessibility default action for items.
     */
    private static final String ACC_ITEM_DEFAULT_ACTION = "Double Click";

    /**
     * Accessibility expand action for tree items.
     */
    private static final String ACC_ITEM_ACTION_EXPAND = "Expand";

    /**
     * Accessibility collapse action for tree items.
     */
    private static final String ACC_ITEM_ACTION_COLLAPSE = "Collapse";

    /**
     * Accessibility name for the column group header toggle button.
     */
    private static final String ACC_TOGGLE_BUTTON_NAME = "Toggle Button";

    /**
     * Alpha blending value used when drawing the dragged column header.
     */
    private static final int COLUMN_DRAG_ALPHA = 128;

    /**
     * Number of pixels below the header to draw the drop point.
     */
    private static final int DROP_POINT_LOWER_OFFSET = 3;

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
     * @see #COLUMN_RESIZER_THRESHOLD
     */
    private static final int ROW_RESIZER_THRESHOLD = 3;


    /**
     * The minimum width of a column header.
     */
    private static final int MIN_COLUMN_HEADER_WIDTH = 20;
    /**
     * The minimum height of a row header.
     */
    private static final int MIN_ROW_HEADER_HEIGHT = 10;

    /**
     * The number used when sizing the row header (i.e. size it for '1000')
     * initially.
     */
//    private static final int INITIAL_ROW_HEADER_SIZING_VALUE = 1000;

    /**
     * The factor to multiply the current row header sizing value by when
     * determining the next sizing value. Used for performance reasons.
     */
//    private static final int ROW_HEADER_SIZING_MULTIPLIER = 10;

    /**
     * Tracks whether the scroll values are correct. If not they will be
     * recomputed in onPaint. This allows us to get a free ride on top of the
     * OS's paint event merging to assure that we don't perform this expensive
     * operation when unnecessary.
     */
    private boolean scrollValuesObsolete = false;

    /**
     * All items in the table, not just root items.
     */
    private List<GridItem> items = new ArrayList<GridItem>();

    /**
     * All root items.
     */
    private List<GridItem> rootItems = new ArrayList<GridItem>();

    /**
     * List of selected items.
     */
    private List<GridItem> selectedItems = new ArrayList<GridItem>();

    /**
     * Reference to the item in focus.
     */
    private GridItem focusItem;

    private boolean cellSelectionEnabled = false;

    private List<Point> selectedCells = new ArrayList<Point>();
    private List<Point> selectedCellsBeforeRangeSelect = new ArrayList<Point>();

    private boolean cellDragSelectionOccuring = false;
    private boolean cellRowDragSelectionOccuring = false;
    private boolean cellColumnDragSelectionOccuring = false;
    private boolean cellDragCTRL = false;
    private boolean followupCellSelectionEventOwed = false;

    private boolean cellSelectedOnLastMouseDown;
    private boolean cellRowSelectedOnLastMouseDown;
    private boolean cellColumnSelectedOnLastMouseDown;

    private GridColumn shiftSelectionAnchorColumn;

    private GridColumn focusColumn;

    private List<GridColumn> selectedColumns = new ArrayList<GridColumn>();

    /**
     * This is the column that the user last navigated to, but may not be the focusColumn because
     * that column may be spanned in the current row.  This is only used in situations where the user
     * has used the keyboard to navigate up or down in the table and the focusColumn has switched to
     * a new column because the intended column (was maintained in this var) was spanned.  The table
     * will attempt to set focus back to the intended column during subsequent up/down navigations.
     */
    private GridColumn intendedFocusColumn;


    /**
     * List of table columns in creation/index order.
     */
    private List<GridColumn> columns = new ArrayList<GridColumn>();

    /**
     * List of the table columns in the order they are displayed.
     */
    private List<GridColumn> displayOrderedColumns = new ArrayList<GridColumn>();

    private GridColumnGroup[] columnGroups = new GridColumnGroup[0];

    /**
     * Renderer to paint the top left area when both column and row headers are
     * shown.
     */
    private IRenderer topLeftRenderer = new DefaultTopLeftRenderer();

    /**
     * Renderer to paint the bottom left area when row headers and column footers are shown
     */
    private IRenderer bottomLeftRenderer = new DefaultBottomLeftRenderer();

    /**
     * Renderer used to paint row headers.
     */
    private IRenderer rowHeaderRenderer = new DefaultRowHeaderRenderer();

    /**
     * Renderer used to paint empty column headers, used when the columns don't
     * fill the horz space.
     */
    private IRenderer emptyColumnHeaderRenderer = new DefaultEmptyColumnHeaderRenderer();

    /**
     * Renderer used to paint empty column footers, used when the columns don't
     * fill the horz space.
     */
    private IRenderer emptyColumnFooterRenderer = new DefaultEmptyColumnFooterRenderer();

    /**
     * Renderer used to paint empty cells to fill horz and vert space.
     */
    private GridCellRenderer emptyCellRenderer = new DefaultEmptyCellRenderer();

    /**
     * Renderer used to paint empty row headers when the rows don't fill the
     * vertical space.
     */
    private IRenderer emptyRowHeaderRenderer = new DefaultEmptyRowHeaderRenderer();

    /**
     * Renderers the UI affordance identifying where the dragged column will be
     * dropped.
     */
    private IRenderer dropPointRenderer = new DefaultDropPointRenderer();

    /**
     * Renderer used to paint on top of an already painted row to denote focus.
     */
    private IRenderer focusRenderer = new DefaultFocusRenderer();

    /**
     * Are row headers visible?
     */
    private boolean rowHeaderVisible = false;

    /**
     * Are column headers visible?
     */
    private boolean columnHeadersVisible = false;

    /**
     * Are column footers visible?
     */
    private boolean columnFootersVisible = false;

    /**
     * Type of selection behavior. Valid values are SWT.SINGLE and SWT.MULTI.
     */
    private int selectionType = SWT.SINGLE;

    /**
     * True if selection highlighting is enabled.
     */
    private boolean selectionEnabled = true;

    /**
     * Default height of items.  This value is used
     * for <code>GridItem</code>s with a height
     * of -1.
     */
    private int itemHeight = 1;

    private boolean userModifiedItemHeight = false;

    /**
     * Width of each row header.
     */
    private int rowHeaderWidth = 0;

    /**
     * The row header width is variable. The row header width gets larger as
     * more rows are added to the table to ensure that the row header has enough
     * room to display the longest string of numbers that display in the row
     * header. This determination of how wide to make the row header is rather
     * slow and therefore is only done at every 1000 items (or so). This
     * variable remembers how many items were last computed and therefore when
     * the number of items is greater than this value, we need to recalculate
     * the row header width. See newItem().
     */
//    private int lastRowHeaderWidthCalculationAt = 0;

    /**
     * Height of each column header.
     */
    private int headerHeight = 0;

    /**
     * Height of each column footer
     */
    private int footerHeight = 0;

    /**
     * True if mouse is hover on a column boundary and can resize the column.
     */
    boolean hoveringOnColumnResizer = false;

    /**
     * Reference to the column being resized.
     */
    private GridColumn columnBeingResized;

    /**
     * Are this <code>Grid</code>'s rows resizeable?
     */
    private boolean rowsResizeable = false;

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

    private boolean hoveringOnRowResizer = false;
    private GridItem rowBeingResized;
	private boolean resizingRow = false;
	private int resizingStartY;
	private int resizingRowStartHeight;

    /**
     * Reference to the column whose header is currently in a pushed state.
     */
    private GridColumn columnBeingPushed;

    /**
     * Is the user currently pushing a column header?
     */
    private boolean pushingColumn = false;

    /**
     * Is the user currently pushing a column header and hovering over that same
     * header?
     */
    private boolean pushingAndHovering = false;

    /**
     * X position of the mouse when the user first pushes a column header.
     */
    private int startHeaderPushX = 0;

    /**
     * X position of the mouse when the user has initiated a drag. This is
     * different than startHeaderPushX because the mouse is allowed some
     * 'wiggle-room' until the header is put into drag mode.
     */
    private int startHeaderDragX = 0;

    /**
     * The current X position of the mouse during a header drag.
     */
    private int currentHeaderDragX = 0;

    /**
     * Are we currently dragging a column header?
     */
    private boolean draggingColumn = false;

    private GridColumn dragDropBeforeColumn = null;

    private GridColumn dragDropAfterColumn = null;

    /**
     * True if the current dragDropPoint is a valid drop point for the dragged
     * column. This is false if the column groups are involved and a column is
     * being dropped into or out of its column group.
     */
    private boolean dragDropPointValid = true;

    /**
     * Reference to the currently item that the mouse is currently hovering
     * over.
     */
    private GridItem hoveringItem;

    /**
     * Reference to the column that the mouse is currently hovering over.
     * Includes the header and all cells (all rows) in this column.
     */
    private GridColumn hoveringColumn;

    private GridColumn hoveringColumnHeader;

    private GridColumnGroup hoverColumnGroupHeader;

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
     * True if the mouse is hovering of a cell's text.
     */
    private boolean hoveringOverText = false;

    /**
     * Are the grid lines visible?
     */
    private boolean linesVisible = true;

    /**
     * Are tree lines visible?
     */
	private boolean treeLinesVisible = true;

    /**
     * Grid line color.
     */
    private Color lineColor;

    /**
     * Vertical scrollbar proxy.
     * <p>
     * Note:
     * <ul>
     * <li>{@link Grid#getTopIndex()} is the only method allowed to call vScroll.getSelection()
     * (except #updateScrollbars() of course)</li>
     * <li>{@link Grid#setTopIndex(int)} is the only method allowed to call vScroll.setSelection(int)</li>
     * </ul>
     */
    private IScrollBarProxy vScroll;

    /**
     * Horizontal scrollbar proxy.
     */
    private IScrollBarProxy hScroll;

    /**
     * The number of GridItems whose visible = true. Maintained for
     * performance reasons (rather than iterating over all items).
     */
    private int currentVisibleItems = 0;

    /**
     * Item selected when a multiple selection using shift+click first occurs.
     * This item anchors all further shift+click selections.
     */
    private GridItem shiftSelectionAnchorItem;

    private boolean columnScrolling = false;

    private int groupHeaderHeight;

    private Color cellHeaderSelectionBackground;

    /**
     * Dispose listener.  This listener is removed during the dispose event to allow re-firing of
     * the event.
     */
    private Listener disposeListener;

    /**
     * The inplace tooltip.
     */
    private GridToolTip inplaceToolTip;

    private GC sizingGC;

    private Color backgroundColor;

    /**
     * True if the widget is being disposed.  When true, events are not fired.
     */
    private boolean disposing = false;

    /**
     * True if there is at least one tree node.  This is used by accessibility and various
     * places for optimization.
     */
    private boolean isTree = false;

    /**
     * True if there is at least one <code>GridItem</code> with an individual height.
     * This value is only set to true in {@link GridItem#setHeight(int,boolean)}
     * and it is never reset to false.
     */
    boolean hasDifferingHeights = false;

    /**
     * True if three is at least one cell spanning columns.  This is used in various places for
     * optimizatoin.
     */
    private boolean hasSpanning = false;

    /**
     * Index of first visible item.  The value must never be read directly.  It is cached and
     * updated when appropriate.  #getTopIndex should be called for every client (even internal
     * callers).  A value of -1 indicates that the value is old and will be recomputed.
     *
     * @see #bottomIndex
     */
    int topIndex = -1;
    /**
     * Index of last visible item.  The value must never be read directly.  It is cached and
     * updated when appropriate.  #getBottomIndex() should be called for every client (even internal
     * callers).  A value of -1 indicates that the value is old and will be recomputed.
     * <p>
     * Note that the item with this index is often only partly visible; maybe only
     * a single line of pixels is visible. In extreme cases, bottomIndex may be the
     * same as topIndex.
     *
     * @see #topIndex
     */
    int bottomIndex = -1;

    /**
     * Index of the first visible column. A value of -1 indicates that the value is old and will be recomputed.
     */
    int startColumnIndex = -1;

    /**
     * Index of the the last visible column. A value of -1 indicates that the value is old and will be recomputed.
     */
    int endColumnIndex = -1;

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
     * Flag that is set to true as soon as one image is set on any one item.
     * This is used to mimic Table behavior that resizes the rows on the first image added.
     * See imageSetOnItem.
     */
    private boolean firstImageSet = false;

    /**
     * Mouse capture flag.  Used for inplace tooltips.  This flag must be used to ensure that
     * we don't setCapture(false) in situations where we didn't do setCapture(true).  The OS (SWT?)
     * will automatically capture the mouse for us during a drag operation.
     */
	private boolean inplaceTooltipCapture;

	/**
	 * This is the tooltip text currently used.  This could be the tooltip text for the currently
	 * hovered cell, or the general grid tooltip.  See handleCellHover.
	 */
	private String displayedToolTipText;

	/**
	 * The height of the area at the top and bottom of the
	 * visible grid area in which scrolling is initiated
	 * while dragging over this Grid.
	 */
	private static final int DRAG_SCROLL_AREA_HEIGHT = 12;

	/**
	 * Threshold for the selection border used for drag n drop
	 * in mode (!{@link #dragOnFullSelection}}.
	 */
	private static final int SELECTION_DRAG_BORDER_THRESHOLD = 2;

    private boolean hoveringOnSelectionDragArea = false;

	private GridItem insertMarkItem = null;
	private GridColumn insertMarkColumn = null;
	private boolean insertMarkBefore = false;
    private IRenderer insertMarkRenderer = new DefaultInsertMarkRenderer();
    private boolean sizeOnEveryItemImageChange;
    private boolean autoHeight = false;
    private boolean autoWidth = true;
    private boolean wordWrapRowHeader = false;
    /**
     * A range of rows in a <code>Grid</code>.
     * <p>
     * A row in this sense exists only for visible items
     * (i.e. items with {@link GridItem#isVisible()} == true).
     * Therefore, the items at 'startIndex' and 'endIndex'
     * are always visible.
     *
     * @see Grid#getRowRange(int, int, boolean, boolean)
     */
    private static class RowRange {
        /** index of first item in range */
        public int startIndex;
        /** index of last item in range */
        public int endIndex;
        /** number of rows (i.e. <em>visible</em> items) in this range */
        public int rows;
        /** height in pixels of this range (including horizontal separator between rows) */
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
     * <p>
     *
     * @param parent a composite control which will be the parent of the new
     * instance (cannot be null)
     * @param style the style of control to construct
     * @throws IllegalArgumentException
     * <ul>
     * <li>ERROR_NULL_ARGUMENT - if the parent is null</li>
     * </ul>
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the parent</li>
     * </ul>
     * @see SWT#SINGLE
     * @see SWT#MULTI
     */
    public Grid(Composite parent, int style)
    {
        super(parent, checkStyle(style));

        // initialize drag & drop support
        setData("DEFAULT_DRAG_SOURCE_EFFECT", new GridDragSourceEffect(this));
        setData("DEFAULT_DROP_TARGET_EFFECT", new GridDropTargetEffect(this));

        sizingGC = new GC(this);

        topLeftRenderer.setDisplay(getDisplay());
        bottomLeftRenderer.setDisplay(getDisplay());
        rowHeaderRenderer.setDisplay(getDisplay());
        emptyColumnHeaderRenderer.setDisplay(getDisplay());
        emptyColumnFooterRenderer.setDisplay(getDisplay());
        emptyCellRenderer.setDisplay(getDisplay());
        dropPointRenderer.setDisplay(getDisplay());
        focusRenderer.setDisplay(getDisplay());
        emptyRowHeaderRenderer.setDisplay(getDisplay());
        insertMarkRenderer.setDisplay(getDisplay());

        setForeground(getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND));
        setLineColor(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

        if ((style & SWT.MULTI) != 0)
        {
            selectionType = SWT.MULTI;
        }

        if (getVerticalBar() != null)
        {
            getVerticalBar().setVisible(false);
            vScroll = new ScrollBarProxyAdapter(getVerticalBar());
        }
        else
        {
            vScroll = new NullScrollBarProxy();
        }

        if (getHorizontalBar() != null)
        {
            getHorizontalBar().setVisible(false);
            hScroll = new ScrollBarProxyAdapter(getHorizontalBar());
        }
        else
        {
            hScroll = new NullScrollBarProxy();
        }

        scrollValuesObsolete = true;

        initListeners();
        initAccessible();


        itemHeight = sizingGC.getFontMetrics().getHeight() + 2;


        RGB sel = getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION).getRGB();
        RGB white = getDisplay().getSystemColor(SWT.COLOR_WHITE).getRGB();

        RGB cellSel = blend(sel,white,50);

        cellHeaderSelectionBackground = new Color(getDisplay(),cellSel);

        setDragDetect(false);
    }



    /**
     * {@inheritDoc}
     */
    public Color getBackground()
    {
        checkWidget();
        if (backgroundColor == null)
            return getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND);
        return backgroundColor;
    }

    /**
     * {@inheritDoc}
     */
    public void setBackground(Color color)
    {
        checkWidget();
        backgroundColor = color;
        redraw();
    }

    /**
     * Returns the background color of column and row headers when a cell in
     * the row or header is selected.
     *
     * @return cell header selection background color
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public Color getCellHeaderSelectionBackground()
    {
        checkWidget();
        return cellHeaderSelectionBackground;
    }

    /**
     * Sets the background color of column and row headers displayed when a cell in
     * the row or header is selected.
     *
     * @param cellSelectionBackground color to set.
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void setCellHeaderSelectionBackground(Color cellSelectionBackground)
    {
        checkWidget();
        this.cellHeaderSelectionBackground = cellSelectionBackground;
    }

    /**
     * Adds the listener to the collection of listeners who will be notified
     * when the receiver's selection changes, by sending it one of the messages
     * defined in the {@code SelectionListener} interface.
     * <p>
     * Cell selection events may have <code>Event.detail = SWT.DRAG</code> when the
     * user is drag selecting multiple cells.  A follow up selection event will be generated
     * when the drag is complete.
     *
     * @param listener the listener which should be notified
     * @throws IllegalArgumentException
     * <ul>
     * <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
     * </ul>
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void addSelectionListener(SelectionListener listener)
    {
        checkWidget();
        if (listener == null)
        {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
        }
        addListener(SWT.Selection, new TypedListener(listener));
        addListener(SWT.DefaultSelection, new TypedListener(listener));
    }

    /**
     * Adds the listener to the collection of listeners who will be notified
     * when the receiver's items changes, by sending it one of the messages
     * defined in the {@code TreeListener} interface.
     *
     * @param listener the listener which should be notified
     * @throws IllegalArgumentException
     * <ul>
     * <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
     * </ul>
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     * @see TreeListener
     * @see #removeTreeListener
     * @see org.eclipse.swt.events.TreeEvent
     */
    public void addTreeListener(TreeListener listener)
    {
        checkWidget();
        if (listener == null)
        {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
        }

        addListener(SWT.Expand, new TypedListener(listener));
        addListener(SWT.Collapse, new TypedListener(listener));
    }

    /**
     * {@inheritDoc}
     */
    public Point computeSize(int wHint, int hHint, boolean changed)
    {
        checkWidget();

        Point prefSize = null;
        if (wHint == SWT.DEFAULT || hHint == SWT.DEFAULT)
        {
            prefSize = getTableSize();
            prefSize.x += 2 * getBorderWidth();
            prefSize.y += 2 * getBorderWidth();
        }

        int x = 0;
        int y = 0;

        if (wHint == SWT.DEFAULT)
        {
            x += prefSize.x;
            if (getVerticalBar() != null)
            {
                x += getVerticalBar().getSize().x;
            }
        }
        else
        {
            x = wHint;
        }

        if (hHint == SWT.DEFAULT)
        {
            y += prefSize.y;
            if (getHorizontalBar() != null)
            {
                y += getHorizontalBar().getSize().y;
            }
        }
        else
        {
            y = hHint;
        }

        return new Point(x, y);
    }

    /**
     * Deselects the item at the given zero-relative index in the receiver. If
     * the item at the index was already deselected, it remains deselected.
     * Indices that are out of range are ignored.
     * <p>
     * If cell selection is enabled, all cells in the specified item are deselected.
     *
     * @param index the index of the item to deselect
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void deselect(int index)
    {
        checkWidget();

        if (index < 0 || index > items.size() - 1)
        {
            return;
        }

        GridItem item = items.get(index);

        if (!cellSelectionEnabled)
        {
            if (selectedItems.contains(item))
            {
                selectedItems.remove(item);
            }
        }
        else
        {
            deselectCells(getCells(item));
        }
        redraw();
    }

    /**
     * Deselects the items at the given zero-relative indices in the receiver.
     * If the item at the given zero-relative index in the receiver is selected,
     * it is deselected. If the item at the index was not selected, it remains
     * deselected. The range of the indices is inclusive. Indices that are out
     * of range are ignored.
     * <p>
     * If cell selection is enabled, all cells in the given range are deselected.
     *
     * @param start the start index of the items to deselect
     * @param end the end index of the items to deselect
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void deselect(int start, int end)
    {
        checkWidget();

        for (int i = start; i <= end; i++)
        {
            if (i < 0)
            {
                continue;
            }
            if (i > items.size() - 1)
            {
                break;
            }

            GridItem item = items.get(i);

            if (!cellSelectionEnabled)
            {
                if (selectedItems.contains(item))
                {
                    selectedItems.remove(item);
                }
            }
            else
            {
                deselectCells(getCells(item));
            }
        }
        redraw();
    }

    /**
     * Deselects the items at the given zero-relative indices in the receiver.
     * If the item at the given zero-relative index in the receiver is selected,
     * it is deselected. If the item at the index was not selected, it remains
     * deselected. Indices that are out of range and duplicate indices are
     * ignored.
     * <p>
     * If cell selection is enabled, all cells in the given items are deselected.
     *
     * @param indices the array of indices for the items to deselect
     * @throws IllegalArgumentException
     * <ul>
     * <li>ERROR_NULL_ARGUMENT - if the set of indices is null</li>
     * </ul>
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void deselect(int[] indices)
    {
        checkWidget();
        if (indices == null)
        {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
        }

        for (int j : indices) {
            if (j >= 0 && j < items.size()) {
                GridItem item = items.get(j);

                if (!cellSelectionEnabled) {
                    if (selectedItems.contains(item)) {
                        selectedItems.remove(item);
                    }
                } else {
                    deselectCells(getCells(item));
                }
            }
        }
        redraw();
    }

    /**
     * Deselects all selected items in the receiver.  If cell selection is enabled,
     * all cells are deselected.
     *
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void deselectAll()
    {
        checkWidget();

        if (!cellSelectionEnabled)
        {
            selectedItems.clear();
            redraw();
        }
        else
        {
            deselectAllCells();
        }
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
     * @throws IllegalArgumentException
     * <ul>
     * <li>ERROR_INVALID_RANGE - if the index is not between 0 and the number
     * of elements in the list minus 1 (inclusive)</li>
     * </ul>
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public GridColumn getColumn(int index)
    {
        checkWidget();

        if (index < 0 || index > getColumnCount() - 1)
        {
            SWT.error(SWT.ERROR_INVALID_RANGE);
        }

        return columns.get(index);
    }

    /**
     * Returns the column at the given point in the receiver or null if no such
     * column exists. The point is in the coordinate system of the receiver.
     *
     * @param point the point used to locate the column
     * @return the column at the given point
     * @throws IllegalArgumentException
     * <ul>
     * <li>ERROR_NULL_ARGUMENT - if the point is null</li>
     * </ul>
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public GridColumn getColumn(Point point)
    {
    	return getColumn(null, point);
    }

    /**
     * Returns the column at the given point and a known item in the receiver or null if no such
     * column exists. The point is in the coordinate system of the receiver.
     *
     * @param item a known GridItem
     * @param point the point used to locate the column
     * @return the column at the given point
     * @throws IllegalArgumentException
     * <ul>
     * <li>ERROR_NULL_ARGUMENT - if the point is null</li>
     * </ul>
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    private GridColumn getColumn(GridItem item, Point point)
    {
        checkWidget();
        if (point == null)
        {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
        }

        GridColumn overThis = null;

        int x2 = 0;

        if (rowHeaderVisible)
        {
            if (point.x <= rowHeaderWidth)
            {
                return null;
            }

            x2 += rowHeaderWidth;
        }

        x2 -= getHScrollSelectionInPixels();

        for (GridColumn column : displayOrderedColumns) {
            if (!column.isVisible()) {
                continue;
            }

            if (point.x >= x2 && point.x < x2 + column.getWidth()) {
                overThis = column;
                break;
            }

            x2 += column.getWidth();
        }

        if (overThis == null)
        {
            return null;
        }

        if (hasSpanning)
        {
            // special logic for column spanning
        	if(item == null) {
        		item = getItem(point);
        	}

            if (item != null)
            {
                int displayColIndex = displayOrderedColumns.indexOf(overThis);

                // track back all previous columns and check their spanning
                for (int i = 0; i < displayColIndex; i++)
                {
                    if (!displayOrderedColumns.get(i).isVisible())
                    {
                        continue;
                    }

                    int colIndex = indexOf(displayOrderedColumns.get(i));
                    int span = item.getColumnSpan(colIndex);

                    if (i + span >= displayColIndex)
                    {
                        overThis = displayOrderedColumns.get(i);
                        break;
                    }
                }
            }
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
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public int getColumnCount()
    {
        checkWidget();
        return columns.size();
    }

    /**
     * Returns an array of zero-relative integers that map the creation order of
     * the receiver's items to the order in which they are currently being
     * displayed.
     * <p>
     * Specifically, the indices of the returned array represent the current
     * visual order of the items, and the contents of the array represent the
     * creation order of the items.
     * </p>
     * <p>
     * Note: This is not the actual structure used by the receiver to maintain
     * its list of items, so modifying the array will not affect the receiver.
     * </p>
     *
     * @return the current visual order of the receiver's items
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public int[] getColumnOrder()
    {
        checkWidget();

        int[] order = new int[columns.size()];
        int i = 0;
        for (GridColumn col : displayOrderedColumns) {
            order[i] = columns.indexOf(col);
            i++;
        }
        return order;
    }

    /**
     * Returns the number of column groups contained in the receiver.
     *
     * @return the number of column groups
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public int getColumnGroupCount()
    {
        checkWidget();
        return columnGroups.length;
    }

    /**
     * Returns an array of {@code GridColumnGroup}s which are the column groups in the
     * receiver.
     * <p>
     * Note: This is not the actual structure used by the receiver to maintain
     * its list of items, so modifying the array will not affect the receiver.
     * </p>
     *
     * @return the column groups in the receiver
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public GridColumnGroup[] getColumnGroups()
    {
        checkWidget();
        GridColumnGroup[] newArray = new GridColumnGroup[columnGroups.length];
        System.arraycopy (columnGroups, 0, newArray, 0, columnGroups.length);
        return newArray;
    }

    /**
     * Returns the column group at the given, zero-relative index in the receiver.
     * Throws an exception if the index is out of range.
     *
     * @param index the index of the column group to return
     * @return the column group at the given index
     * @throws IllegalArgumentException
     * <ul>
     * <li>ERROR_INVALID_RANGE - if the index is not between 0 and the number
     * of elements in the list minus 1 (inclusive)</li>
     * </ul>
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public GridColumnGroup getColumnGroup(int index)
    {
        checkWidget();

        if (index < 0 || index >= columnGroups.length)
            SWT.error(SWT.ERROR_INVALID_RANGE);

        return columnGroups[index];
    }

    /**
     * Sets the order that the items in the receiver should be displayed in to
     * the given argument which is described in terms of the zero-relative
     * ordering of when the items were added.
     *
     * @param order the new order to display the items
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS -if not called from the thread that
     * created the receiver</li>
     * </ul>
     * @throws IllegalArgumentException
     * <ul>
     * <li>ERROR_NULL_ARGUMENT - if the item order is null</li>
     * <li>ERROR_INVALID_ARGUMENT - if the order is not the same length as the
     * number of items, or if an item is listed twice, or if the order splits a
     * column group</li>
     * </ul>
     */
    public void setColumnOrder(int[] order)
    {
        checkWidget();

        if (order == null)
        {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
        }

        if (order.length != displayOrderedColumns.size())
        {
            SWT.error(SWT.ERROR_INVALID_ARGUMENT);
        }

        boolean[] seen = new boolean[displayOrderedColumns.size()];

        for (int anOrder : order) {
            if (anOrder < 0 || anOrder >= displayOrderedColumns.size()) {
                SWT.error(SWT.ERROR_INVALID_ARGUMENT);
            }
            if (seen[anOrder]) {
                SWT.error(SWT.ERROR_INVALID_ARGUMENT);
            }
            seen[anOrder] = true;
        }

        if (columnGroups.length != 0)
        {
            GridColumnGroup currentGroup = null;
            int colsInGroup = 0;

            for (int anOrder : order) {
                GridColumn col = getColumn(anOrder);

                if (currentGroup != null) {
                    if (col.getColumnGroup() != currentGroup && colsInGroup > 0) {
                        SWT.error(SWT.ERROR_INVALID_ARGUMENT);
                    } else {
                        colsInGroup--;
                        if (colsInGroup <= 0) {
                            currentGroup = null;
                        }
                    }
                } else if (col.getColumnGroup() != null) {
                    currentGroup = col.getColumnGroup();
                    colsInGroup = currentGroup.getColumns().length - 1;
                }
            }
        }

        GridColumn[] cols = getColumns();

        displayOrderedColumns.clear();

        for (int anOrder : order) {
            displayOrderedColumns.add(cols[anOrder]);
        }
    }

    /**
     * Returns an array of {@code GridColumn}s which are the columns in the
     * receiver. If no {@code GridColumn}s were created by the programmer,
     * the array is empty, despite the fact that visually, one column of items
     * may be visible. This occurs when the programmer uses the table like a
     * list, adding items but never creating a column.
     * <p>
     * Note: This is not the actual structure used by the receiver to maintain
     * its list of items, so modifying the array will not affect the receiver.
     * </p>
     *
     * @return the items in the receiver
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public GridColumn[] getColumns()
    {
        checkWidget();
        return columns.toArray(new GridColumn[columns.size()]);
    }

    /**
     * Returns the empty cell renderer.
     *
     * @return Returns the emptyCellRenderer.
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public GridCellRenderer getEmptyCellRenderer()
    {
        checkWidget();
        return emptyCellRenderer;
    }

    /**
     * Returns the empty column header renderer.
     *
     * @return Returns the emptyColumnHeaderRenderer.
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public IRenderer getEmptyColumnHeaderRenderer()
    {
        checkWidget();
        return emptyColumnHeaderRenderer;
    }

    /**
     * Returns the empty column footer renderer.
     *
     * @return Returns the emptyColumnFooterRenderer.
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public IRenderer getEmptyColumnFooterRenderer() {
    	checkWidget();
    	return emptyColumnFooterRenderer;
    }

    /**
     * Returns the empty row header renderer.
     *
     * @return Returns the emptyRowHeaderRenderer.
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public IRenderer getEmptyRowHeaderRenderer()
    {
        checkWidget();
        return emptyRowHeaderRenderer;
    }

    /**
     * Returns the externally managed horizontal scrollbar.
     *
     * @return the external horizontal scrollbar.
     * @see #setHorizontalScrollBarProxy(IScrollBarProxy)
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    protected IScrollBarProxy getHorizontalScrollBarProxy()
    {
        checkWidget();
        return hScroll;
    }

    /**
     * Returns the externally managed vertical scrollbar.
     *
     * @return the external vertical scrollbar.
     * @see #setlVerticalScrollBarProxy(IScrollBarProxy)
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    protected IScrollBarProxy getVerticalScrollBarProxy()
    {
        checkWidget();
        return vScroll;
    }

    /**
     * Gets the focus renderer.
     *
     * @return Returns the focusRenderer.
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public IRenderer getFocusRenderer()
    {
        checkWidget();
        return focusRenderer;
    }

    /**
     * Returns the height of the column headers. If this table has column
     * groups, the returned value includes the height of group headers.
     *
     * @return height of the column header row
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public int getHeaderHeight()
    {
        checkWidget();
        return headerHeight;
    }

    /**
     * Returns the height of the column footers.
     *
     * @return height of the column footer row
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public int getFooterHeight() {
    	checkWidget();
    	return footerHeight;
    }

    /**
     * Returns the height of the column group headers.
     *
     * @return height of column group headers
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public int getGroupHeaderHeight()
    {
        checkWidget();
        return groupHeaderHeight;
    }

    /**
     * Returns {@code true} if the receiver's header is visible, and
     * {@code false} otherwise.
     *
     * @return the receiver's header's visibility state
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public boolean getHeaderVisible()
    {
        checkWidget();
        return columnHeadersVisible;
    }

    /**
     * Returns {@code true} if the receiver's footer is visible, and {@code false} otherwise
     * @return the receiver's footer's visibility state
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public boolean getFooterVisible() {
    	checkWidget();
    	return columnFootersVisible;
    }

    /**
     * Returns the item at the given, zero-relative index in the receiver.
     * Throws an exception if the index is out of range.
     *
     * @param index the index of the item to return
     * @return the item at the given index
     * @throws IllegalArgumentException
     * <ul>
     * <li>ERROR_INVALID_RANGE - if the index is not between 0 and the number of elements in the
     * list minus 1 (inclusive) </li>     *
     * </ul>
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public GridItem getItem(int index)
    {
        checkWidget();

        if (index < 0 || index >= items.size())
        {
            SWT.error(SWT.ERROR_INVALID_RANGE);
        }

        return items.get(index);
    }

    /**
     * Returns the item at the given point in the receiver or null if no such
     * item exists. The point is in the coordinate system of the receiver.
     *
     * @param point the point used to locate the item
     * @return the item at the given point
     * @throws IllegalArgumentException
     * <ul>
     * <li>ERROR_NULL_ARGUMENT - if the point is null</li>
     * </ul>
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public GridItem getItem(Point point)
    {
        checkWidget();

        if (point == null)
            SWT.error(SWT.ERROR_NULL_ARGUMENT);

        if (point.x < 0 || point.x > getClientArea().width) return null;

        Point p = new Point(point.x, point.y);

        int y2=0;

        if (columnHeadersVisible)
        {
            if (p.y <= headerHeight)
            {
                return null;
            }
            y2 += headerHeight;
        }

        GridItem itemToReturn = null;

        int row=getTopIndex();
        while(row<items.size() && y2<=getClientArea().height)
        {
            GridItem currItem = items.get(row);
            if (currItem.isVisible())
            {
                int currItemHeight = currItem.getHeight();

                if (p.y >= y2 && p.y < y2+currItemHeight+1)
                {
                	itemToReturn = currItem;
                	break;
                }

                y2 += currItemHeight +1;
            }
            row++;
        }

        if (hasSpanning)
        {
            if (itemToReturn != null)
            {
            	int itemIndex = this.getIndexOfItem(itemToReturn);

                GridColumn gridColumn = getColumn(itemToReturn, point);
                int displayColIndex = displayOrderedColumns.indexOf(gridColumn);


                // track back all previous columns and check their spanning
                for (int i = 0; i < itemIndex; i++)
                {
                	GridItem gridItem = this.getItem(i);
                    if (!gridItem.isVisible())
                    {
                        continue;
                    }
                    int span = gridItem.getRowSpan(displayColIndex);

                    if (i + span >= itemIndex)
                    {
                        itemToReturn = gridItem;
                        break;
                    }
                }
            }
        }

        return itemToReturn;
    }

    /**
     * Returns the number of items contained in the receiver.
     *
     * @return the number of items
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public int getItemCount()
    {
        checkWidget();
        return getItems().length;
    }

    /**
     * Returns the default height of the items
     * in this <code>Grid</code>. See {@link #setItemHeight(int)}
     * for details.
     *
     * <p>IMPORTANT: The Grid's items need not all have the
     * height returned by this method, because an
     * item's height may have been changed by calling
     * {@link GridItem#setHeight(int)}.
     *
     * @return default height of items
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     * @see #setItemHeight(int)
     */
    public int getItemHeight()
    {
        checkWidget();
        return itemHeight;
    }

    /**
     * Sets the default height for this <code>Grid</code>'s items.  When
     * this method is called, all existing items are resized
     * to the specified height and items created afterwards will be
     * initially sized to this height.
     * <p>
     * As long as no default height was set by the client through this method,
     * the preferred height of the first item in this <code>Grid</code> is
     * used as a default for all items (and is returned by {@link #getItemHeight()}).
     *
     * @param height  default height in pixels
     * @throws IllegalArgumentException
     * <ul>
     * <li>ERROR_INVALID_ARGUMENT - if the height is < 1</li>
     * </ul>
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     *
     * @see GridItem#getHeight()
     * @see GridItem#setHeight(int)
     */
    public void setItemHeight(int height)
    {
        checkWidget();
        if (height < 1)
            SWT.error(SWT.ERROR_INVALID_ARGUMENT);
        itemHeight = height;
        userModifiedItemHeight = true;
        for(int cnt=0;cnt<items.size();cnt++)
            items.get(cnt).setHeight(height);
        hasDifferingHeights=false;
        setScrollValuesObsolete();
        redraw();
    }

    /**
     * Returns true if the rows are resizable.
     *
     * @return the row resizeable state
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     * @see #setRowsResizeable(boolean)
     */
    public boolean getRowsResizeable() {
    	checkWidget();
    	return rowsResizeable;
    }
    /**
     * Sets the rows resizeable state of this <code>Grid</code>.
     * The default is 'false'.
     * <p>
     * If a row in a <code>Grid</code> is resizeable,
     * then the user can interactively change its height
     * by dragging the border of the row header.
     * <p>
     * Note that for rows to be resizable the row headers must be visible.
     *
     * @param rowsResizeable true if this <code>Grid</code>'s rows should be resizable
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     * @see #setRowHeaderVisible(boolean)
     */
    public void setRowsResizeable(boolean rowsResizeable) {
    	checkWidget();
    	this.rowsResizeable=rowsResizeable;
    }

    /**
     * Returns a (possibly empty) array of {@code GridItem}s which are the
     * items in the receiver.
     * <p>
     * Note: This is not the actual structure used by the receiver to maintain
     * its list of items, so modifying the array will not affect the receiver.
     * </p>
     *
     * @return the items in the receiver
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public GridItem[] getItems()
    {
        checkWidget();
        return items.toArray(new GridItem[items.size()]);
    }

    /**
     *
     * @param item
     * @return t
     */
    public int getIndexOfItem(GridItem item)
    {
    	checkWidget();

    	return items.indexOf(item);
    }

    /**
     * Returns the line color.
     *
     * @return Returns the lineColor.
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
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
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public boolean getLinesVisible()
    {
        checkWidget();
        return linesVisible;
    }

	/**
	 * Returns true if the tree lines are visible.
	 *
	 * @return Returns the treeLinesVisible.
	 * @throws org.eclipse.swt.SWTException
	 * <ul>
	 * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
	 * created the receiver</li>
	 * </ul>
	 */
	public boolean getTreeLinesVisible() {
		checkWidget();
		return treeLinesVisible;
	}

    /**
     * Returns the next visible item in the table.
     *
     * @param item item
     * @return next visible item or null
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public GridItem getNextVisibleItem(GridItem item)
    {
        checkWidget();

        int index = items.indexOf(item);
        if (items.size() == index + 1)
        {
            return null;
        }

        GridItem nextItem = items.get(index + 1);

        while (!nextItem.isVisible())
        {
            index++;
            if (items.size() == index + 1)
            {
                return null;
            }

            nextItem = items.get(index + 1);
        }

        return nextItem;
    }

    /**
     * Returns the previous visible item in the table. Passing null for the item
     * will return the last visible item in the table.
     *
     * @param item item or null
     * @return previous visible item or if item==null last visible item
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public GridItem getPreviousVisibleItem(GridItem item)
    {
        checkWidget();

        int index;
        if (item == null)
        {
            index = items.size();
        }
        else
        {
            index = items.indexOf(item);
            if (index == 0)
            {
                return null;
            }
        }

        GridItem prevItem = items.get(index - 1);

        while (!prevItem.isVisible())
        {
            index--;
            if (index == 0)
            {
                return null;
            }

            prevItem = items.get(index - 1);
        }

        return prevItem;
    }

    /**
     * Returns the previous visible column in the table.
     *
     * @param column column
     * @return previous visible column or null
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public GridColumn getPreviousVisibleColumn(GridColumn column)
    {
        checkWidget();

        int index = displayOrderedColumns.indexOf(column);

        if (index == 0)
            return null;

        index --;

        GridColumn previous = displayOrderedColumns.get(index);

        while (!previous.isVisible())
        {
            if (index == 0)
                return null;

            index --;
            previous = displayOrderedColumns.get(index);
        }

        return previous;
    }

    /**
     * Returns the next visible column in the table.
     *
     * @param column column
     * @return next visible column or null
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public GridColumn getNextVisibleColumn(GridColumn column)
    {
        checkWidget();

        int index = displayOrderedColumns.indexOf(column);

        if (index == displayOrderedColumns.size() - 1)
            return null;

        index ++;

        GridColumn next = displayOrderedColumns.get(index);

        while (!next.isVisible())
        {
            if (index == displayOrderedColumns.size() - 1)
                return null;

            index ++;
            next = displayOrderedColumns.get(index);
        }

        return next;
    }

    /**
     * Returns the number of root items contained in the receiver.
     *
     * @return the number of items
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public int getRootItemCount()
    {
        checkWidget();
        return rootItems.size();
    }

    /**
     * Returns a (possibly empty) array of {@code GridItem}s which are
     * the root items in the receiver.
     * <p>
     * Note: This is not the actual structure used by the receiver to maintain
     * its list of items, so modifying the array will not affect the receiver.
     * </p>
     *
     * @return the root items in the receiver
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public GridItem[] getRootItems()
    {
        checkWidget();

        return rootItems.toArray(new GridItem[rootItems.size()]);
    }

    /**
     * TODO: asl;fj
     * @param index
     * @return asdf
     */
    public GridItem getRootItem(int index)
    {
        checkWidget();

        if (index < 0 || index >= rootItems.size())
        {
            SWT.error(SWT.ERROR_INVALID_RANGE);
        }

        return rootItems.get(index);
    }

    /**
     * Gets the row header renderer.
     *
     * @return Returns the rowHeaderRenderer.
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public IRenderer getRowHeaderRenderer()
    {
        checkWidget();
        return rowHeaderRenderer;
    }

    /**
     * Returns a array of {@code GridItem}s that are currently selected in the
     * receiver. The order of the items is unspecified. An empty array indicates
     * that no items are selected.
     * <p>
     * Note: This is not the actual structure used by the receiver to maintain
     * its selection, so modifying the array will not affect the receiver.
     * <p>
     * If cell selection is enabled, any items which contain at least one selected
     * cell are returned.
     *
     * @return an array representing the selection
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public GridItem[] getSelection()
    {
        checkWidget();

        if (!cellSelectionEnabled)
        {
            return selectedItems.toArray(new GridItem[selectedItems.size()]);
        }
        else
        {
            List<GridItem> items = new ArrayList<GridItem>();
            int itemCount = getItemCount();

            for (Point cell : selectedCells) {
                if (cell.y >= 0 && cell.y < itemCount) {
                    GridItem item = getItem(cell.y);
                    if (!items.contains(item))
                        items.add(item);
                }
            }
            return items.toArray(new GridItem[items.size()]);
        }
    }

    /**
     * Returns the number of selected items contained in the receiver.  If cell selection
     * is enabled, the number of items with at least one selected cell are returned.
     *
     * @return the number of selected items
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public int getSelectionCount()
    {
        checkWidget();

        if (!cellSelectionEnabled)
        {
            return selectedItems.size();
        }
        else
        {
            List<GridItem> items = new ArrayList<GridItem>();
            for (Point cell : selectedCells) {
                GridItem item = getItem(cell.y);
                if (!items.contains(item))
                    items.add(item);
            }
            return items.size();
        }
    }

    /**
     * Returns the number of selected cells contained in the receiver.
     *
     * @return the number of selected cells
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
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
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public int getSelectionIndex()
    {
        checkWidget();

        if (!cellSelectionEnabled)
        {
            if (selectedItems.size() == 0)
            {
                return -1;
            }

            return items.indexOf(selectedItems.get(0));
        }
        else
        {
            if (selectedCells.size() == 0)
                return -1;

            return selectedCells.get(0).y;
        }
    }

    /**
     * Returns the zero-relative indices of the items which are currently
     * selected in the receiver. The order of the indices is unspecified. The
     * array is empty if no items are selected.
     * <p>
     * Note: This is not the actual structure used by the receiver to maintain
     * its selection, so modifying the array will not affect the receiver.
     * <p>
     * If cell selection is enabled, returns the indices of any items which
     * contain at least one selected cell.
     *
     * @return the array of indices of the selected items
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public int[] getSelectionIndices()
    {
        checkWidget();

        if (!cellSelectionEnabled)
        {
            int[] indices = new int[selectedItems.size()];
            int i = 0;
            for (GridItem item : selectedItems) {
                indices[i] = items.indexOf(item);
                i++;
            }
            return indices;
        }
        else
        {
            List<GridItem> selectedRows = new ArrayList<GridItem>();
            for (Point cell : selectedCells) {
                GridItem item = getItem(cell.y);
                if (!selectedRows.contains(item))
                    selectedRows.add(item);
            }
            int[] indices = new int[selectedRows.size()];
            int i = 0;
            for (GridItem item : selectedRows) {
                indices[i] = items.indexOf(item);
                i++;
            }
            return indices;
        }
    }

    /**
     * Returns the zero-relative index of the item which is currently at the top
     * of the receiver. This index can change when items are scrolled or new
     * items are added or removed.
     *
     * @return the index of the top item
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public int getTopIndex()
    {
        checkWidget();

        if (topIndex != -1)
            return topIndex;

        if (!vScroll.getVisible())
        {
        	topIndex = 0;
        }
        else
        {
        	// figure out first visible row and last visible row
        	int firstVisibleIndex = vScroll.getSelection();

        	if (isTree)
        	{
        		Iterator itemsIter = items.iterator();
            	int row = firstVisibleIndex + 1;

        		while (row > 0 && itemsIter.hasNext())
        		{
        			GridItem item = (GridItem)itemsIter.next();

        			if (item.isVisible())
        			{
        				row--;
        				if (row == 0)
        				{
        					firstVisibleIndex = items.indexOf(item);
        				}
        			}
        		}
        	}

            topIndex = firstVisibleIndex;

            /*
             *  MOPR  here lies more potential for increasing performance
             *  for the case (isTree || hasDifferingHeights)
             *  the topIndex could be derived from the previous value
             *  depending on a delta of the vScroll.getSelection()
             *  instead of being calculated completely anew
             */
        }

        return topIndex;
    }
    /**
     * Returns the zero-relative index of the item which is currently at the bottom
     * of the receiver. This index can change when items are scrolled, expanded
     * or collapsed or new items are added or removed.
     * <p>
     * Note that the item with this index is often only partly visible; maybe only
     * a single line of pixels is visible. Use {@link #isShown(GridItem)} to find
     * out.
     * <p>
     * In extreme cases, getBottomIndex() may return the same value as
     * {@link #getTopIndex()}.
     *
     * @return the index of the bottom item
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    int getBottomIndex() {
        checkWidget();

        if (bottomIndex != -1)
            return bottomIndex;

        if (items.size() == 0)
        {
            bottomIndex = 0;
        }
        else if (getVisibleGridHeight()<1)
        {
        	bottomIndex = getTopIndex();
        }
        else
        {
            RowRange range = getRowRange(getTopIndex(),getVisibleGridHeight(),false,false);

            bottomIndex = range.endIndex;
            bottomIndexShownCompletely = range.height <= getVisibleGridHeight();
        }

        return bottomIndex;
    }
    /**
     * Returns a {@link RowRange} ranging from
     * the grid item at startIndex to that at endIndex.
     * <p>
     * This is primarily used to measure the height
     * in pixel of such a range and to count the number
     * of visible grid items within the range.
     *
     * @param startIndex index of the first item in the range or -1 to the first visible item in this grid
     * @param endIndex index of the last item in the range or -1 to use the last visible item in this grid
     * @return
     */
    private RowRange getRowRange(int startIndex, int endIndex) {

        // parameter preparation
        if (startIndex == -1)
        {
            // search frist visible item
            do startIndex++; while (startIndex < items.size() && !items.get(startIndex).isVisible());
            if (startIndex == items.size()) return null;
        }
        if (endIndex == -1)
        {
            // search last visible item
    		endIndex = items.size();
            do endIndex--; while (endIndex >= 0 && !items.get(endIndex).isVisible());
            if (endIndex == -1) return null;
        }

        // fail fast
        if (startIndex<0 || endIndex<0 || startIndex>=items.size() || endIndex>=items.size()
                || endIndex < startIndex
                || !items.get(startIndex).isVisible()
                || !items.get(endIndex).isVisible())
            SWT.error(SWT.ERROR_INVALID_ARGUMENT);
        RowRange range = new RowRange();
        range.startIndex = startIndex;
        range.endIndex = endIndex;

        if(isTree || hasDifferingHeights)
        {
            for (int idx=startIndex ; idx<=endIndex ; idx++ )
            {
                GridItem currItem = items.get(idx);

                if(currItem.isVisible())
                {
                    if (range.rows>0)
                        range.height++;		// height of horizontal row separator
                    range.height += currItem.getHeight();
                    range.rows++;
                }
            }
        }
        else
        {
            range.rows = range.endIndex - range.startIndex + 1;
            range.height = ( getItemHeight() + 1 ) * range.rows - 1;
        }

        return range;
    }
    /**
     * This method can be used to build a range of grid rows
     * that is allowed to span a certain height in pixels.
     * <p>
     * It returns a {@link RowRange} that contains information
     * about the range, especially the index of the last
     * element in the range (or if inverse == true, then the
     * index of the first element).
     * <p>
     * Note:  Even if 'forceEndCompletelyInside' is set to
     * true, the last item will not lie completely within
     * the availableHeight, if (height of item at startIndex < availableHeight).
     *
     * @param startIndex  index of the first (if inverse==false) or
     *                    last (if inverse==true) item in the range
     * @param availableHeight height in pixels
     * @param forceEndCompletelyInside if true, the last item in the range will lie completely
     *        within the availableHeight, otherwise it may lie partly outside this range
     * @param inverse  if true, then the first item in the range will be searched, not the last
     * @return range of grid rows
     * @see RowRange
     */
    private RowRange getRowRange(int startIndex, int availableHeight,
                                 boolean forceEndCompletelyInside, boolean inverse) {
        // parameter preparation
        if (startIndex == -1)
        {
        	if(!inverse)
        	{
                // search frist visible item
                do startIndex++; while (startIndex < items.size() && !items.get(startIndex).isVisible());
                if (startIndex == items.size()) return null;
        	}
        	else
        	{
                // search last visible item
        		startIndex = items.size();
                do startIndex--; while (startIndex >= 0 && !items.get(startIndex).isVisible());
                if (startIndex == -1) return null;
        	}
        }

        // fail fast
        if (startIndex < 0 || startIndex >= items.size()
                || !items.get(startIndex).isVisible())
            SWT.error(SWT.ERROR_INVALID_ARGUMENT);

        RowRange range = new RowRange();

        if (availableHeight <= 0)
        {
        	// special case: empty range
            range.startIndex = startIndex;
            range.endIndex = startIndex;
            range.rows = 0;
            range.height = 0;
            return range;
        }

        if (isTree || hasDifferingHeights)
        {
            int otherIndex = startIndex;		// tentative end index
            int consumedItems = 0;
            int consumedHeight = 0;

            // consume height for startEnd  (note: no separator pixel added here)
            consumedItems++;
            consumedHeight += items.get(otherIndex).getHeight();

            // note: we use "+2" in next line, because we only try to add another row if there
            // is room for the separator line + at least one pixel row for the additional item
            while (consumedHeight+2 <= availableHeight)
            {
                // STEP 1:
                // try to find a visible item we can add

                int nextIndex = otherIndex;
                GridItem nextItem;

                do
                {
                    if (!inverse)
                        nextIndex++;
                    else
                        nextIndex--;

                    if (nextIndex >= 0 && nextIndex < items.size())
                        nextItem = items.get(nextIndex);
                    else
                        nextItem = null;
                }
                while (nextItem != null && !nextItem.isVisible());


                if (nextItem == null)
                {
                    // no visible item found
                    break;
                }

                if (forceEndCompletelyInside)
                {
                    // must lie completely within the allowed height
                    if(!(consumedHeight + 1 + nextItem.getHeight() <= availableHeight))
                        break;
                }

                // we found one !!

                // STEP 2:
                // Consume height for this item

                consumedItems++;
                consumedHeight += 1;	// height of separator line
                consumedHeight += nextItem.getHeight();

                // STEP 3:
                // make this item it the current guess for the other end
                otherIndex = nextIndex;
            }

            range.startIndex = !inverse ? startIndex : otherIndex;
            range.endIndex   = !inverse ? otherIndex : startIndex;
            range.rows       = consumedItems;
            range.height     = consumedHeight;
        }
        else
        {
            int availableRows = ( availableHeight + 1 ) / ( getItemHeight() + 1 );

            if ((( getItemHeight() + 1 ) * range.rows - 1) + 1 < availableHeight)
            {
            	// not all available space used yet
            	// - so add another row if it need not be completely within availableHeight
                if (!forceEndCompletelyInside)
                    availableRows++;
            }

            int otherIndex = startIndex + ((availableRows - 1) * (!inverse ? 1 : -1));
            if (otherIndex<0) otherIndex = 0;
            if (otherIndex>=items.size()) otherIndex = items.size() - 1 ;

            range.startIndex = !inverse ? startIndex : otherIndex;
            range.endIndex   = !inverse ? otherIndex : startIndex;
            range.rows       = range.endIndex - range.startIndex + 1;
            range.height     = ( getItemHeight() + 1 ) * range.rows - 1;
        }

        return range;
    }
    /**
     * Returns the height of the plain grid in pixels.
     * <p>
     * This includes all rows for visible items (i.e. items that return true
     * on {@link GridItem#isVisible()} ; not only those currently visible on
     * screen) and the 1 pixel separator between rows.
     * <p>
     * This does <em>not</em> include the height of the column headers.
     *
     * @return height of plain grid
     */
    int getGridHeight() {
        RowRange range = getRowRange(-1,-1);
        return range != null ? range.height : 0;
        /*
         *  MOPR  currently this method is only used in #getTableSize() ;
         *  if it will be used for more important things in the future
         *  (e.g. the max value for vScroll.setValues() when doing pixel-by-pixel
         *  vertical scrolling) then this value should at least be cached or
         *  even updated incrementally when grid items are added/removed or
         *  expaned/collapsed (similar as #currentVisibleItems).
         *  (this is only necessary in the case (isTree || hasDifferingHeights))
         */
    }

    /**
     * Returns the height of the on-screen area that is available
     * for showing the grid's rows, i.e. the client area of the
     * scrollable minus the height of the column headers (if shown).
     *
     * @return height of visible grid in pixels
     */
    int getVisibleGridHeight() {
        return getClientArea().height - (columnHeadersVisible ? headerHeight : 0) - (columnFootersVisible ? footerHeight : 0);
    }

    /**
     * Returns the height of the screen area that is available for showing the grid columns
     * @return
     */
    int getVisibleGridWidth() {
    	return getClientArea().width - ( rowHeaderVisible ? rowHeaderWidth : 0 );
    }

    /**
     * Gets the top left renderer.
     *
     * @return Returns the topLeftRenderer.
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public IRenderer getTopLeftRenderer()
    {
        checkWidget();
        return topLeftRenderer;
    }

    /**
     * Gets the bottom left renderer.
     *
     * @return Returns the bottomLeftRenderer.
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public IRenderer getBottomLeftRenderer()
    {
        checkWidget();
        return bottomLeftRenderer;
    }

    /**
     * Searches the receiver's list starting at the first column (index 0) until
     * a column is found that is equal to the argument, and returns the index of
     * that column. If no column is found, returns -1.
     *
     * @param column the search column
     * @return the index of the column
     * @throws IllegalArgumentException
     * <ul>
     * <li>ERROR_NULL_ARGUMENT - if the column is null</li>
     * </ul>
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public int indexOf(GridColumn column)
    {
        checkWidget();

        if (column == null)
            SWT.error(SWT.ERROR_NULL_ARGUMENT);

        if (column.getParent() != this) return -1;

        return columns.indexOf(column);
    }

    /**
     * Searches the receiver's list starting at the first item (index 0) until
     * an item is found that is equal to the argument, and returns the index of
     * that item. If no item is found, returns -1.
     *
     * @param item the search item
     * @return the index of the item
     * @throws IllegalArgumentException
     * <ul>
     * <li>ERROR_NULL_ARGUMENT - if the item is null</li>
     * </ul>
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public int indexOf(GridItem item)
    {
        checkWidget();

        if (item == null)
            SWT.error(SWT.ERROR_NULL_ARGUMENT);

        if (item.getParent() != this) return -1;

        return items.indexOf(item);
    }

    /**
     * Returns {@code true} if the receiver's row header is visible, and
     * {@code false} otherwise.
     * <p>
     *
     * @return the receiver's row header's visibility state
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public boolean isRowHeaderVisible()
    {
        checkWidget();
        return rowHeaderVisible;
    }

    /**
     * Returns {@code true} if the item is selected, and {@code false}
     * otherwise. Indices out of range are ignored.  If cell selection is
     * enabled, returns true if the item at the given index contains at
     * least one selected cell.
     *
     * @param index the index of the item
     * @return the visibility state of the item at the index
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public boolean isSelected(int index)
    {
        checkWidget();

        if (index < 0 || index >= items.size()) return false;

        if (!cellSelectionEnabled)
        {
            return isSelected(items.get(index));
        }
        else
        {
            for (Point cell : selectedCells) {
                if (cell.y == index) return true;
            }
            return false;
        }
    }

    /**
     * Returns true if the given item is selected.  If cell selection is enabled,
     * returns true if the given item contains at least one selected cell.
     *
     * @param item item
     * @return true if the item is selected.
     * @throws IllegalArgumentException
     * <ul>
     * <li>ERROR_NULL_ARGUMENT - if the item is null</li>
     * </ul>
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public boolean isSelected(GridItem item)
    {
        checkWidget();
        if (!cellSelectionEnabled)
        {
            return selectedItems.contains(item);
        }
        else
        {
            int index = indexOf(item);
            if (index == -1) return false;
            for (Point cell : selectedCells) {
                if (cell.y == index) return true;
            }
            return false;
        }
    }

    /**
     * Returns true if the given cell is selected.
     *
     * @param cell cell
     * @return true if the cell is selected.
     * @throws IllegalArgumentException
     * <ul>
     * <li>ERROR_NULL_ARGUMENT - if the cell is null</li>
     * </ul>
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public boolean isCellSelected(Point cell)
    {
        checkWidget();

        if (cell == null)
            SWT.error(SWT.ERROR_NULL_ARGUMENT);

        return selectedCells.contains(cell);
    }



    /**
     * Removes the item from the receiver at the given zero-relative index.
     *
     * @param index the index for the item
     * @throws IllegalArgumentException
     * <ul>
     * <li>ERROR_INVALID_RANGE - if the index is not between 0 and the number
     * of elements in the list minus 1 (inclusive)</li>
     * </ul>
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void remove(int index)
    {
        checkWidget();
        if (index < 0 || index > items.size() - 1)
        {
            SWT.error(SWT.ERROR_INVALID_RANGE);
        }
        GridItem item = items.get(index);
        item.dispose();
        redraw();
    }

    /**
     * Removes the items from the receiver which are between the given
     * zero-relative start and end indices (inclusive).
     *
     * @param start the start of the range
     * @param end the end of the range
     * @throws IllegalArgumentException
     * <ul>
     * <li>ERROR_INVALID_RANGE - if either the start or end are not between 0
     * and the number of elements in the list minus 1 (inclusive)</li>
     * </ul>
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void remove(int start, int end)
    {
        checkWidget();

        for (int i = end; i >= start; i--)
        {
            if (i < 0 || i > items.size() - 1)
            {
                SWT.error(SWT.ERROR_INVALID_RANGE);
            }
            GridItem item = items.get(i);
            item.dispose();
        }
        redraw();
    }

    /**
     * Removes the items from the receiver's list at the given zero-relative
     * indices.
     *
     * @param indices the array of indices of the items
     * @throws IllegalArgumentException
     * <ul>
     * <li>ERROR_INVALID_RANGE - if the index is not between 0 and the number
     * of elements in the list minus 1 (inclusive)</li>
     * <li>ERROR_NULL_ARGUMENT - if the indices array is null</li>
     * </ul>
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void remove(int[] indices)
    {
        checkWidget();

        if (indices == null)
        {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
        }

        GridItem[] removeThese = new GridItem[indices.length];
        for (int i = 0; i < indices.length; i++)
        {
            int j = indices[i];
            if (j < items.size() && j >= 0)
            {
                removeThese[i] = items.get(j);
            }
            else
            {
                SWT.error(SWT.ERROR_INVALID_RANGE);
            }

        }
        for (GridItem item : removeThese) {
            item.dispose();
        }
        redraw();
    }

    /**
     * Removes all of the items from the receiver.
     *
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void removeAll()
    {
        checkWidget();

        while (items.size() > 0)
        {
            items.get(0).dispose();
        }
        deselectAll();
        redraw();
    }

    /**
     * Removes the listener from the collection of listeners who will be
     * notified when the receiver's selection changes.
     *
     * @param listener the listener which should no longer be notified
     * @see SelectionListener
     * @see #addSelectionListener(SelectionListener)
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void removeSelectionListener(SelectionListener listener)
    {
        checkWidget();
        removeListener(SWT.Selection, listener);
        removeListener(SWT.DefaultSelection, listener);
    }

    /**
     * Removes the listener from the collection of listeners who will be
     * notified when the receiver's items changes.
     *
     * @param listener the listener which should no longer be notified
     * @see TreeListener
     * @see #addTreeListener(TreeListener)
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void removeTreeListener(TreeListener listener)
    {
        checkWidget();
        removeListener(SWT.Expand, listener);
        removeListener(SWT.Collapse, listener);
    }

    /**
     * Selects the item at the given zero-relative index in the receiver. If the
     * item at the index was already selected, it remains selected. Indices that
     * are out of range are ignored.
     * <p>
     * If cell selection is enabled, selects all cells at the given index.
     *
     * @param index the index of the item to select
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void select(int index)
    {
        checkWidget();

        if (!selectionEnabled) return;

        if (index < 0 || index >= items.size()) return;

        GridItem item = items.get(index);

        if (!cellSelectionEnabled)
        {
            if (selectionType == SWT.MULTI && selectedItems.contains(item)) return;

            if (selectionType == SWT.SINGLE) selectedItems.clear();

            selectedItems.add(item);
        }
        else
        {
            selectCells(getCells(item));
        }

        redraw();
    }

    /**
     * Selects the items in the range specified by the given zero-relative
     * indices in the receiver. The range of indices is inclusive. The current
     * selection is not cleared before the new items are selected.
     * <p>
     * If an item in the given range is not selected, it is selected. If an item
     * in the given range was already selected, it remains selected. Indices
     * that are out of range are ignored and no items will be selected if start
     * is greater than end. If the receiver is single-select and there is more
     * than one item in the given range, then all indices are ignored.
     * <p>
     * If cell selection is enabled, all cells within the given range are selected.
     *
     * @param start the start of the range
     * @param end the end of the range
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     * @see Grid#setSelection(int,int)
     */
    public void select(int start, int end)
    {
        checkWidget();

        if (!selectionEnabled) return;

        if (selectionType == SWT.SINGLE && start != end) return;

        if (!cellSelectionEnabled)
        {
            if (selectionType == SWT.SINGLE) selectedItems.clear();
        }

        for (int i = start; i <= end; i++)
        {
            if (i < 0)
            {
                continue;
            }
            if (i > items.size() - 1)
            {
                break;
            }

            GridItem item = items.get(i);

            if (!cellSelectionEnabled)
            {
                if (!selectedItems.contains(item))
                    selectedItems.add(item);
            }
            else
            {
                selectCells(getCells(item));
            }
        }

        redraw();
    }

    /**
     * Selects the items at the given zero-relative indices in the receiver. The
     * current selection is not cleared before the new items are selected.
     * <p>
     * If the item at a given index is not selected, it is selected. If the item
     * at a given index was already selected, it remains selected. Indices that
     * are out of range and duplicate indices are ignored. If the receiver is
     * single-select and multiple indices are specified, then all indices are
     * ignored.
     * <p>
     * If cell selection is enabled, all cells within the given indices are
     * selected.
     *
     * @param indices the array of indices for the items to select
     * @throws IllegalArgumentException
     * <ul>
     * <li>ERROR_NULL_ARGUMENT - if the array of indices is null</li>
     * </ul>
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     * @see Grid#setSelection(int[])
     */
    public void select(int[] indices)
    {
        checkWidget();

        if (indices == null)
        {
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
        }

        if (!selectionEnabled) return;

        if (selectionType == SWT.SINGLE && indices.length > 1) return;

        if (!cellSelectionEnabled)
            if (selectionType == SWT.SINGLE) selectedItems.clear();

        for (int j : indices) {
            if (j >= 0 && j < items.size()) {
                GridItem item = items.get(j);

                if (!cellSelectionEnabled) {
                    if (!selectedItems.contains(item))
                        selectedItems.add(item);
                } else {
                    selectCells(getCells(item));
                }
            }
        }
        redraw();
    }

    /**
     * Selects all of the items in the receiver.
     * <p>
     * If the receiver is single-select, do nothing.  If cell selection is enabled,
     * all cells are selected.
     *
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void selectAll()
    {
        checkWidget();

        if (!selectionEnabled) return;

        if (selectionType == SWT.SINGLE) return;

        if (cellSelectionEnabled)
        {
            selectAllCells();
            return;
        }

        selectedItems.clear();
        selectedItems.addAll(items);
        redraw();
    }

    /**
     * Sets the empty cell renderer.
     *
     * @param emptyCellRenderer The emptyCellRenderer to set.
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void setEmptyCellRenderer(GridCellRenderer emptyCellRenderer)
    {
        checkWidget();
        emptyCellRenderer.setDisplay(getDisplay());
        this.emptyCellRenderer = emptyCellRenderer;
    }

    /**
     * Sets the empty column header renderer.
     *
     * @param emptyColumnHeaderRenderer The emptyColumnHeaderRenderer to set.
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void setEmptyColumnHeaderRenderer(IRenderer emptyColumnHeaderRenderer)
    {
        checkWidget();
        emptyColumnHeaderRenderer.setDisplay(getDisplay());
        this.emptyColumnHeaderRenderer = emptyColumnHeaderRenderer;
    }

    /**
     * Sets the empty column footer renderer.
     *
     * @param emptyColumnFooterRenderer The emptyColumnFooterRenderer to set.
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void setEmptyColumnFooterRenderer(IRenderer emptyColumnFooterRenderer)
    {
        checkWidget();
        emptyColumnFooterRenderer.setDisplay(getDisplay());
        this.emptyColumnFooterRenderer = emptyColumnFooterRenderer;
    }

    /**
     * Sets the empty row header renderer.
     *
     * @param emptyRowHeaderRenderer The emptyRowHeaderRenderer to set.
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void setEmptyRowHeaderRenderer(IRenderer emptyRowHeaderRenderer)
    {
        checkWidget();
        emptyRowHeaderRenderer.setDisplay(getDisplay());
        this.emptyRowHeaderRenderer = emptyRowHeaderRenderer;
    }

    /**
     * Sets the external horizontal scrollbar. Allows the scrolling to be
     * managed externally from the table. This functionality is only intended
     * when SWT.H_SCROLL is not given.
     * <p>
     * Using this feature, a ScrollBar could be instantiated outside the table,
     * wrapped in IScrollBar and thus be 'connected' to the table.
     *
     * @param scroll The horizontal scrollbar to set.
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    protected void setHorizontalScrollBarProxy(IScrollBarProxy scroll)
    {
        checkWidget();
        if (getHorizontalBar() != null)
        {
            return;
        }
        hScroll = scroll;

        hScroll.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				onScrollSelection();
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
    }

    /**
	 * Sets the external vertical scrollbar. Allows the scrolling to be managed
	 * externally from the table. This functionality is only intended when
	 * SWT.V_SCROLL is not given.
	 * <p>
	 * Using this feature, a ScrollBar could be instantiated outside the table,
	 * wrapped in IScrollBar and thus be 'connected' to the table.
	 *
	 * @param scroll
	 *            The vertical scrollbar to set.
	 * @throws org.eclipse.swt.SWTException
	 *             <ul>
	 *             <li>ERROR_WIDGET_DISPOSED - if the receiver has been
	 *             disposed</li>
	 *             <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *             thread that created the receiver</li>
	 *             </ul>
	 */
    protected void setlVerticalScrollBarProxy(IScrollBarProxy scroll)
    {
        checkWidget();
        if (getVerticalBar() != null)
        {
            return;
        }
        vScroll = scroll;

        vScroll.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				onScrollSelection();
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
    }

    /**
     * Sets the focus renderer.
     *
     * @param focusRenderer The focusRenderer to set.
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void setFocusRenderer(IRenderer focusRenderer)
    {
        checkWidget();
        this.focusRenderer = focusRenderer;
    }

    /**
     * Marks the receiver's header as visible if the argument is {@code true},
     * and marks it invisible otherwise.
     *
     * @param show the new visibility state
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void setHeaderVisible(boolean show)
    {
        checkWidget();
        this.columnHeadersVisible = show;
        redraw();
    }

    /**
     * Marks the receiver's footer as visible if the argument is {@code true},
     * and marks it invisible otherwise.
     *
     * @param show the new visibility state
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void setFooterVisible(boolean show)
    {
        checkWidget();
        this.columnFootersVisible = show;
        redraw();
    }

    /**
     * Sets the line color.
     *
     * @param lineColor The lineColor to set.
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
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
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void setLinesVisible(boolean linesVisible)
    {
        checkWidget();
        this.linesVisible = linesVisible;
        redraw();
    }

	/**
	 * Sets the tree line visibility.
	 *
	 * @param treeLinesVisible
	 * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
	 */
	public void setTreeLinesVisible(boolean treeLinesVisible) {
		checkWidget();
		this.treeLinesVisible = treeLinesVisible;
        redraw();
	}

    /**
     * Sets the row header renderer.
     *
     * @param rowHeaderRenderer The rowHeaderRenderer to set.
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void setRowHeaderRenderer(IRenderer rowHeaderRenderer)
    {
        checkWidget();
        rowHeaderRenderer.setDisplay(getDisplay());
        this.rowHeaderRenderer = rowHeaderRenderer;
    }

    /**
     * Marks the receiver's row header as visible if the argument is
     * {@code true}, and marks it invisible otherwise. When row headers are
     * visible, horizontal scrolling is always done by column rather than by
     * pixel.
     *
     * @param show the new visibility state
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void setRowHeaderVisible(boolean show)
    {
        checkWidget();
        this.rowHeaderVisible = show;
        setColumnScrolling(true);

        if (show && isAutoWidth())
        {
	        rowHeaderWidth = 1;

            for (GridItem iterItem : items) {
                rowHeaderWidth = Math.max(
                    rowHeaderWidth,
                    rowHeaderRenderer.computeSize(sizingGC, SWT.DEFAULT, SWT.DEFAULT, iterItem).x);
            }
        }

        redraw();
    }

    /**
     * Selects the item at the given zero-relative index in the receiver. The
     * current selection is first cleared, then the new item is selected.
     * <p>
     * If cell selection is enabled, all cells within the item at the given index
     * are selected.
     *
     * @param index the index of the item to select
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void setSelection(int index)
    {
        checkWidget();

        if (!selectionEnabled) return;

        if (index >= 0 && index < items.size())
        {
            if (!cellSelectionEnabled)
            {
                selectedItems.clear();
                selectedItems.add(items.get(index));
                redraw();
            }
            else
            {
                selectedCells.clear();
                selectCells(getCells(items.get(index)));
            }
        }
    }

    /**
     * Selects the items in the range specified by the given zero-relative
     * indices in the receiver. The range of indices is inclusive. The current
     * selection is cleared before the new items are selected.
     * <p>
     * Indices that are out of range are ignored and no items will be selected
     * if start is greater than end. If the receiver is single-select and there
     * is more than one item in the given range, then all indices are ignored.
     * <p>
     * If cell selection is enabled, all cells within the given range are selected.
     *
     * @param start the start index of the items to select
     * @param end the end index of the items to select
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     * @see Grid#deselectAll()
     * @see Grid#select(int,int)
     */
    public void setSelection(int start, int end)
    {
        checkWidget();

        if (!selectionEnabled) return;

        if (selectionType == SWT.SINGLE && start != end) return;

        if (!cellSelectionEnabled)
        {
            selectedItems.clear();
        }
        else
        {
            selectedCells.clear();
        }

        for (int i = start; i <= end; i++)
        {
            if (i < 0)
            {
                continue;
            }
            if (i > items.size() - 1)
            {
                break;
            }

            GridItem item = items.get(i);

            if (!cellSelectionEnabled)
            {
                selectedItems.add(item);
            }
            else
            {
                selectCells(getCells(item));
            }
        }
        redraw();
    }

    /**
     * Selects the items at the given zero-relative indices in the receiver. The
     * current selection is cleared before the new items are selected.
     * <p>
     * Indices that are out of range and duplicate indices are ignored. If the
     * receiver is single-select and multiple indices are specified, then all
     * indices are ignored.
     * <p>
     * If cell selection is enabled, all cells within the given indices are selected.
     *
     * @param indices the indices of the items to select
     * @throws IllegalArgumentException
     * <ul>
     * <li>ERROR_NULL_ARGUMENT - if the array of indices is null</li>
     * </ul>
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     * @see Grid#deselectAll()
     * @see Grid#select(int[])
     */
    public void setSelection(int[] indices)
    {
        checkWidget();

        if (!selectionEnabled) return;

        if (selectionType == SWT.SINGLE && indices.length > 1) return;

        if (!cellSelectionEnabled)
        {
            selectedItems.clear();
        }
        else
        {
            selectedCells.clear();
        }

        for (int j : indices) {
            if (j < 0) {
                continue;
            }
            if (j > items.size() - 1) {
                break;
            }

            GridItem item = items.get(j);

            if (!cellSelectionEnabled) {
                selectedItems.add(item);
            } else {
                selectCells(getCells(item));
            }
        }
        redraw();
    }

    /**
     * Sets the receiver's selection to be the given array of items. The current
     * selection is cleared before the new items are selected.
     * <p>
     * Items that are not in the receiver are ignored. If the receiver is
     * single-select and multiple items are specified, then all items are
     * ignored.  If cell selection is enabled, all cells within the given items
     * are selected.
     *
     * @param _items the array of items
     * @throws IllegalArgumentException
     * <ul>
     * <li>ERROR_NULL_ARGUMENT - if the array of items is null</li>
     * <li>ERROR_INVALID_ARGUMENT - if one of the items has been disposed</li>
     * </ul>
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     * @see Grid#deselectAll()
     * @see Grid#select(int[])
     * @see Grid#setSelection(int[])
     */
    public void setSelection(GridItem[] _items)
    {
        checkWidget();

        if (!selectionEnabled) return;

        if (_items == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);

        if (selectionType == SWT.SINGLE && _items.length > 1) return;

        if (!cellSelectionEnabled)
        {
            selectedItems.clear();
        }
        else
        {
            selectedCells.clear();
        }

        for (GridItem item : _items) {
            if (item == null) continue;
            if (item.isDisposed()) SWT.error(SWT.ERROR_INVALID_ARGUMENT);
            if (item.getParent() != this) continue;

            if (!cellSelectionEnabled) {
                selectedItems.add(item);
            } else {
                selectCells(getCells(item));
            }
        }

        redraw();
    }

    /**
     * Sets the zero-relative index of the item which is currently at the top of
     * the receiver. This index can change when items are scrolled or new items
     * are added and removed.
     *
     * @param index the index of the top item
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void setTopIndex(int index)
    {
        checkWidget();
        if (index < 0 || index >= items.size())
        {
            return;
        }

        GridItem item = items.get(index);
        if (!item.isVisible())
        {
            return;
        }

        if (!vScroll.getVisible())
        {
            return;
        }

        int vScrollAmount = 0;

        for (int i = 0; i < index; i++)
        {
            if (items.get(i).isVisible())
            {
                vScrollAmount++;
            }
        }

        vScroll.setSelection(vScrollAmount);
        topIndex = -1;
        bottomIndex = -1;
        redraw();
    }

    /**
     * Sets the top left renderer.
     *
     * @param topLeftRenderer The topLeftRenderer to set.
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void setTopLeftRenderer(IRenderer topLeftRenderer)
    {
        checkWidget();
        topLeftRenderer.setDisplay(getDisplay());
        this.topLeftRenderer = topLeftRenderer;
    }

    /**
     * Sets the bottom left renderer.
     *
     * @param bottomLeftRenderer The topLeftRenderer to set.
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void setBottomLeftRenderer(IRenderer bottomLeftRenderer)
    {
        checkWidget();
        bottomLeftRenderer.setDisplay(getDisplay());
        this.bottomLeftRenderer = bottomLeftRenderer;
    }

    /**
     * Shows the column. If the column is already showing in the receiver, this
     * method simply returns. Otherwise, the columns are scrolled until the
     * column is visible.
     *
     * @param col the column to be shown
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void showColumn(GridColumn col)
    {
        checkWidget();

        if (!col.isVisible())
        {
        	GridColumnGroup group = col.getColumnGroup();
        	group.setExpanded(!group.getExpanded());
            if (group.getExpanded())
            {
                group.notifyListeners(SWT.Expand,new Event());
            }
            else
            {
                group.notifyListeners(SWT.Collapse,new Event());
            }
        }

        if (!hScroll.getVisible())
        {
            return;
        }

        int x = getColumnHeaderXPosition(col);

        int firstVisibleX = 0;
        if (rowHeaderVisible)
        {
            firstVisibleX = rowHeaderWidth;
        }

        // if its visible just return
        if (x >= firstVisibleX
            && (x + col.getWidth()) <= (firstVisibleX + (getClientArea().width - firstVisibleX)))
        {
            return;
        }

        if (!getColumnScrolling())
        {
            if (x < firstVisibleX)
            {
                hScroll.setSelection(getHScrollSelectionInPixels() - (firstVisibleX - x));
            }
            else
            {
                if (col.getWidth() > getClientArea().width - firstVisibleX)
                {
                    hScroll.setSelection(getHScrollSelectionInPixels() + (x - firstVisibleX));
                }
                else
                {
                    x -= getClientArea().width - firstVisibleX - col.getWidth();
                    hScroll.setSelection(getHScrollSelectionInPixels() + (x - firstVisibleX));
                }
            }
        }
        else
        {
            if (x < firstVisibleX || col.getWidth() > getClientArea().width - firstVisibleX)
            {
                int sel = displayOrderedColumns.indexOf(col);
                hScroll.setSelection(sel);
            }
            else
            {
                int availableWidth = getClientArea().width - firstVisibleX - col.getWidth();

                GridColumn prevCol = getPreviousVisibleColumn(col);
                GridColumn currentScrollTo = col;

                while (true)
                {
                    if (prevCol == null || prevCol.getWidth() > availableWidth)
                    {
                        int sel = displayOrderedColumns.indexOf(currentScrollTo);
                        hScroll.setSelection(sel);
                        break;
                    }
                    else
                    {
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
     *
     * <p>Here, "completely" only refers to the item's height, not its
     * width. This means this method returns true also if some cells
     * are horizontally scrolled away.
     *
     * @param item
     * @return true if 'item' is shown
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * <li>ERROR_INVALID_ARGUMENT - if 'item' is not contained in the receiver</li>
     * </ul>
     */
    boolean isShown(GridItem item)
    {
        checkWidget();

        if(!item.isVisible())
            return false;

        int itemIndex = items.indexOf(item);

        if (itemIndex == -1)
            SWT.error(SWT.ERROR_INVALID_ARGUMENT);

        int firstVisibleIndex = getTopIndex();
        int lastVisibleIndex = getBottomIndex();

        return (itemIndex >= firstVisibleIndex && itemIndex < lastVisibleIndex)
    	       ||
    	       (itemIndex == lastVisibleIndex && bottomIndexShownCompletely);
    }
    /**
     * Shows the item. If the item is already showing in the receiver, this
     * method simply returns. Otherwise, the items are scrolled until the item
     * is visible.
     *
     * @param item the item to be shown
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * <li>ERROR_INVALID_ARGUMENT - if 'item' is not contained in the receiver</li>
     * </ul>
     */
    public void showItem(GridItem item)
    {
        checkWidget();

        updateScrollbars();

        // if no items are visible on screen then abort
        if (getVisibleGridHeight()<1)
        {
            return;
        }

        // if its visible just return
        if (isShown(item))
        {
            return;
        }

        if (!item.isVisible())
        {
            GridItem parent = item.getParentItem();
            do
            {
                if (!parent.isExpanded())
                {
                    parent.setExpanded(true);
                    parent.fireEvent(SWT.Expand);
                }
                parent = parent.getParentItem();
            }
            while (parent != null);
        }

        int newTopIndex = items.indexOf(item);

        if (newTopIndex >= getBottomIndex())
        {
            RowRange range = getRowRange(newTopIndex,getVisibleGridHeight(),true,true);	// note: inverse==true
            newTopIndex = range.startIndex;		// note: use startIndex because of inverse==true
        }

        setTopIndex(newTopIndex);
    }

    /**
     * Shows the selection. If the selection is already showing in the receiver,
     * this method simply returns. Otherwise, the items are scrolled until the
     * selection is visible.
     *
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void showSelection()
    {
        checkWidget();

        if (scrollValuesObsolete)
            updateScrollbars();

        GridItem item;

        if (!cellSelectionEnabled)
        {
            if (selectedItems.size() == 0)
            {
                return;
            }

            item = selectedItems.get(0);
            showItem(item);
        }
        else
        {
            if (selectedCells.size() == 0) return;

            Point cell = selectedCells.get(0);
            item = getItem(cell.y);
            showItem(item);
            GridColumn col = getColumn(cell.x);
            showColumn(col);
        }

    }

    /**
     * Enables selection highlighting if the argument is <code>true</code>.
     *
     * @param selectionEnabled the selection enabled state
     *
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void setSelectionEnabled(boolean selectionEnabled)
    {
        checkWidget();

        if (!selectionEnabled)
        {
            selectedItems.clear();
            redraw();
        }

        this.selectionEnabled = selectionEnabled;
    }

    /**
     * Returns <code>true</code> if selection is enabled, false otherwise.
     *
     * @return the selection enabled state
     *
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public boolean getSelectionEnabled()
    {
        checkWidget();
        return selectionEnabled;
    }


    /**
     * Computes and sets the height of the header row. This method will ask for
     * the preferred size of all the column headers and use the max.
     *
     * @param gc GC for font metrics, etc.
     */
    private void computeHeaderHeight(GC gc)
    {

        int colHeaderHeight = 0;
        for (GridColumn column : columns) {
            colHeaderHeight = Math
                .max(column.getHeaderRenderer().computeSize(gc, column.getWidth(), SWT.DEFAULT,
                                                            column).y, colHeaderHeight);
        }

        int groupHeight = 0;
        for (GridColumnGroup group : columnGroups)
        {
            groupHeight = Math.max(group.getHeaderRenderer().computeSize(gc, SWT.DEFAULT,
                                                                         SWT.DEFAULT, group).y,
                                   groupHeight);
        }

        headerHeight = colHeaderHeight + groupHeight;
        groupHeaderHeight = groupHeight;
    }

    private void computeFooterHeight(GC gc)
    {

        int colFooterHeight = 0;
        for (GridColumn column : columns) {
            colFooterHeight = Math
                .max(column.getFooterRenderer().computeSize(gc, column.getWidth(), SWT.DEFAULT,
                                                            column).y, colFooterHeight);
        }

        footerHeight = colFooterHeight;
    }

    /**
     * Returns the computed default item height. Currently this method just gets the
     * preferred size of all the cells in the given row and returns that (it is
     * then used as the height of all rows with items having a height of -1).
     *
     * @param item item to use for sizing
     * @param gc GC used to perform font metrics,etc.
     * @return the row height
     */
    private int computeItemHeight(GridItem item, GC gc)
    {
        int height = 1;

        if (columns.size() == 0 || items.size() == 0)
        {
            return height;
        }

        for (GridColumn column : columns) {
            height = Math.max(height, column.getCellRenderer().computeSize(gc, SWT.DEFAULT,
                                                                           SWT.DEFAULT,
                                                                           item).y);
        }

        if( rowHeaderVisible && rowHeaderRenderer != null ) {
        	height = Math.max(height, rowHeaderRenderer.computeSize(gc, SWT.DEFAULT,
                    SWT.DEFAULT, item).y);
        }

        return height <= 0 ? 16 : height;
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
        if (!column.isVisible())
        {
            return -1;
        }

        int x = 0;

        x -= getHScrollSelectionInPixels();

        if (rowHeaderVisible)
        {
            x += rowHeaderWidth;
        }
        for (GridColumn column2 : displayOrderedColumns) {
            if (!column2.isVisible()) {
                continue;
            }

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
        if (columnScrolling)
        {
            int pixels = 0;
            for (int i = 0; i < selection; i++)
            {
                pixels += displayOrderedColumns.get(i).getWidth();
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

        if (columnHeadersVisible)
        {
            y += headerHeight;
        }

        if(columnFootersVisible) {
        	y += footerHeight;
        }

        y += getGridHeight();

        if (rowHeaderVisible)
        {
            x += rowHeaderWidth;
        }

        for (GridColumn column : columns) {
            if (column.isVisible()) {
                x += column.getWidth();
            }
        }

        return new Point(x, y);
    }

    /**
     * Manages the header column dragging and calculates the drop point,
     * triggers a redraw.
     *
     * @param x mouse x
     * @return true if this event has been consumed.
     */
    private boolean handleColumnDragging(int x)
    {

        GridColumn local_dragDropBeforeColumn = null;
        GridColumn local_dragDropAfterColumn = null;

        int x2 = 1;

        if (rowHeaderVisible)
        {
            x2 += rowHeaderWidth + 1;
        }

        x2 -= getHScrollSelectionInPixels();

        GridColumn previousVisibleCol = null;
        boolean nextVisibleColumnIsBeforeCol = false;
        GridColumn firstVisibleCol = null;
        GridColumn lastVisibleCol = null;

        if (x < x2)
        {
            for (GridColumn column : displayOrderedColumns) {
                if (!column.isVisible()) {
                    continue;
                }
                local_dragDropBeforeColumn = column;
                break;
            }
            local_dragDropAfterColumn = null;
        }
        else
        {
            for (GridColumn column : displayOrderedColumns) {
                if (!column.isVisible()) {
                    continue;
                }

                if (firstVisibleCol == null) {
                    firstVisibleCol = column;
                }
                lastVisibleCol = column;

                if (nextVisibleColumnIsBeforeCol) {
                    local_dragDropBeforeColumn = column;
                    nextVisibleColumnIsBeforeCol = false;
                }

                if (x >= x2 && x <= (x2 + column.getWidth())) {
                    if (x <= (x2 + column.getWidth() / 2)) {
                        local_dragDropBeforeColumn = column;
                        local_dragDropAfterColumn = previousVisibleCol;
                    } else {
                        local_dragDropAfterColumn = column;

                        // the next visible column is the before col
                        nextVisibleColumnIsBeforeCol = true;
                    }
                }

                x2 += column.getWidth();
                previousVisibleCol = column;
            }

            if (local_dragDropBeforeColumn == null)
            {
                local_dragDropAfterColumn = lastVisibleCol;
            }
        }

        currentHeaderDragX = x;

        if (local_dragDropBeforeColumn != dragDropBeforeColumn
            || (dragDropBeforeColumn == null && dragDropAfterColumn == null))
        {
            dragDropPointValid = true;

            // Determine if valid drop point
            if (columnGroups.length != 0)
            {

                if (columnBeingPushed.getColumnGroup() == null)
                {
                    if (local_dragDropBeforeColumn != null
                        && local_dragDropAfterColumn != null
                        && local_dragDropBeforeColumn.getColumnGroup() != null
                        && local_dragDropBeforeColumn.getColumnGroup() == local_dragDropAfterColumn
                            .getColumnGroup())
                    {
                        // Dont move a column w/o a group in between two columns
                        // in the same group
                        dragDropPointValid = false;
                    }
                }
                else
                {
                    if (!(local_dragDropBeforeColumn != null && local_dragDropBeforeColumn
                        .getColumnGroup() == columnBeingPushed.getColumnGroup())
                        && !(local_dragDropAfterColumn != null && local_dragDropAfterColumn
                            .getColumnGroup() == columnBeingPushed.getColumnGroup()))
                    {
                        // Dont move a column with a group
                        dragDropPointValid = false;
                    }
                }
            }
            else
            {
                dragDropPointValid = true;
            }
        }

        dragDropBeforeColumn = local_dragDropBeforeColumn;
        dragDropAfterColumn = local_dragDropAfterColumn;

        Rectangle clientArea = getClientArea();
        redraw(clientArea.x,clientArea.y,clientArea.width,clientArea.height,false);

        return true;
    }

    /**
     * Handles the moving of columns after a column is dropped.
     */
    private void handleColumnDrop()
    {
        draggingColumn = false;

        if ((dragDropBeforeColumn != columnBeingPushed && dragDropAfterColumn != columnBeingPushed)
            && (columnGroups.length == 0 || dragDropPointValid))
        {

            int notifyFrom = displayOrderedColumns.indexOf(columnBeingPushed);
            int notifyTo = notifyFrom;

            displayOrderedColumns.remove(columnBeingPushed);

            if (dragDropBeforeColumn == null)
            {

                notifyTo = displayOrderedColumns.size();
                displayOrderedColumns.add(columnBeingPushed);
            }
            else if (dragDropAfterColumn == null)
            {
                displayOrderedColumns.add(0, columnBeingPushed);
                notifyFrom = 0;
            }
            else
            {
                int insertAtIndex;

                if (columnGroups.length != 0)
                {
                    // ensure that we aren't putting this column into a group,
                    // this is possible if
                    // there are invisible columns between the after and before
                    // cols

                    if (dragDropBeforeColumn.getColumnGroup() == columnBeingPushed.getColumnGroup())
                    {
                        insertAtIndex = displayOrderedColumns.indexOf(dragDropBeforeColumn);
                    }
                    else if (dragDropAfterColumn.getColumnGroup() == columnBeingPushed
                        .getColumnGroup())
                    {
                        insertAtIndex = displayOrderedColumns.indexOf(dragDropAfterColumn) + 1;
                    }
                    else
                    {
                        if (dragDropBeforeColumn.getColumnGroup() == null)
                        {
                            insertAtIndex = displayOrderedColumns.indexOf(dragDropBeforeColumn);
                        }
                        else
                        {
                            GridColumnGroup beforeGroup = dragDropBeforeColumn.getColumnGroup();
                            insertAtIndex = displayOrderedColumns.indexOf(dragDropBeforeColumn);
                            while (insertAtIndex > 0
                                   && displayOrderedColumns.get(insertAtIndex -1).getColumnGroup() == beforeGroup)
                            {
                                insertAtIndex--;
                            }

                        }
                    }
                }
                else
                {
                    insertAtIndex = displayOrderedColumns.indexOf(dragDropBeforeColumn);
                }
                displayOrderedColumns.add(insertAtIndex, columnBeingPushed);
                notifyFrom = Math.min(notifyFrom, insertAtIndex);
                notifyTo = Math.max(notifyTo, insertAtIndex);
            }

            for (int i = notifyFrom; i <= notifyTo; i++)
            {
                displayOrderedColumns.get(i).fireMoved();
            }
        }

        redraw();
    }

    /**
     * Determines if the mouse is pushing the header but has since move out of
     * the header bounds and therefore should be drawn unpushed. Also initiates
     * a column header drag when appropriate.
     *
     * @param x mouse x
     * @param y mouse y
     * @return true if this event has been consumed.
     */
    private boolean handleColumnHeaderHoverWhilePushing(int x, int y)
    {
        GridColumn overThis = overColumnHeader(x, y);

        if ((overThis == columnBeingPushed) != pushingAndHovering)
        {
            pushingAndHovering = (overThis == columnBeingPushed);
            redraw();
        }
        if (columnBeingPushed.getMoveable())
        {

            if (pushingAndHovering && Math.abs(startHeaderPushX - x) > 3)
            {

                // stop pushing
                pushingColumn = false;
                columnBeingPushed.getHeaderRenderer().setMouseDown(false);
                columnBeingPushed.getHeaderRenderer().setHover(false);

                // now dragging
                draggingColumn = true;
                columnBeingPushed.getHeaderRenderer().setMouseDown(false);

                startHeaderDragX = x;

                dragDropAfterColumn = null;
                dragDropBeforeColumn = null;
                dragDropPointValid = true;

                handleColumnDragging(x);
            }
        }

        return true;
    }

    /**
     * Determines if a column group header has been clicked and forwards the
     * event to the header renderer.
     *
     * @param x mouse x
     * @param y mouse y
     * @return true if this event has been consumed.
     */
    private boolean handleColumnGroupHeaderClick(int x, int y)
    {

        if (!columnHeadersVisible)
        {
            return false;
        }

        GridColumnGroup overThis = overColumnGroupHeader(x, y);

        if (overThis == null)
        {
            return false;
        }

        int headerX = 0;
        if (rowHeaderVisible)
        {
            headerX += rowHeaderWidth;
        }

        int width = 0;
        boolean firstCol = false;

        for (GridColumn col : displayOrderedColumns) {
            if (col.getColumnGroup() == overThis && col.isVisible()) {
                firstCol = true;
                width += col.getWidth();
            }
            if (!firstCol && col.isVisible()) {
                headerX += col.getWidth();
            }
        }

        overThis.getHeaderRenderer().setBounds(headerX - getHScrollSelectionInPixels(), 0, width,
                                               groupHeaderHeight);
        return overThis.getHeaderRenderer()
            .notify(IInternalWidget.LeftMouseButtonDown, new Point(x, y), overThis);
    }

    /**
     * Determines if a column header has been clicked, updates the renderer
     * state and triggers a redraw if necesary.
     *
     * @param x mouse x
     * @param y mouse y
     * @return true if this event has been consumed.
     */
    private boolean handleColumnHeaderPush(int x, int y)
    {
        if (!columnHeadersVisible)
        {
            return false;
        }

        GridColumn overThis = overColumnHeader(x, y);

        if (overThis == null)
        {
            return false;
        }

        if (cellSelectionEnabled && !overThis.getMoveable())
        {
            return false;
        }

        columnBeingPushed = overThis;

        // draw pushed
        columnBeingPushed.getHeaderRenderer().setMouseDown(true);
        columnBeingPushed.getHeaderRenderer().setHover(true);
        pushingAndHovering = true;
        redraw();

        startHeaderPushX = x;
        pushingColumn = true;

        setCapture(true);

        return true;
    }

    private boolean handleColumnFooterPush(int x, int y)
    {
    	if(!columnFootersVisible) {
    		return false;
    	}

    	GridColumn overThis = overColumnFooter(x, y);

        return overThis != null;

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
        if (newWidth < MIN_COLUMN_HEADER_WIDTH)
        {
            newWidth = MIN_COLUMN_HEADER_WIDTH;
        }

        if (columnScrolling)
        {
        	int maxWidth = getClientArea().width;
        	if (rowHeaderVisible)
        		maxWidth -= rowHeaderWidth;
        	if (newWidth > maxWidth)
        		newWidth = maxWidth;
        }

        if (newWidth == columnBeingResized.getWidth())
        {
        	return;
        }

        columnBeingResized.setWidth(newWidth,false);
        scrollValuesObsolete = true;

        Rectangle clientArea = getClientArea();
        redraw(clientArea.x,clientArea.y,clientArea.width,clientArea.height,false);

        columnBeingResized.fireResized();

        for (int index = displayOrderedColumns.indexOf(columnBeingResized) + 1; index < displayOrderedColumns.size(); index ++)
        {
            GridColumn col = displayOrderedColumns.get(index);
            if (col.isVisible()) col.fireMoved();
        }
    }
    /**
     * Sets the new height of the item of the row being resized and fires the appropriate
     * listeners.
     *
     * @param x mouse x
     */
    private void handleRowResizerDragging(int y)
    {
        int newHeight = resizingRowStartHeight + (y - resizingStartY);
        if (newHeight < MIN_ROW_HEADER_HEIGHT)
        {
            newHeight = MIN_ROW_HEADER_HEIGHT;
        }

        if (newHeight > getClientArea().height)
        {
            newHeight = getClientArea().height;
        }

        if (newHeight == rowBeingResized.getHeight())
        {
            return;
        }

        Event e = new Event();
        e.item = rowBeingResized;
        e.widget = this;
        e.detail = newHeight;

        rowBeingResized.notifyListeners(SWT.Resize, e);

        if (!e.doit)
        	return;

        newHeight = e.detail;

        if (newHeight < MIN_ROW_HEADER_HEIGHT)
        {
            newHeight = MIN_ROW_HEADER_HEIGHT;
        }

        if (newHeight > getClientArea().height)
        {
            newHeight = getClientArea().height;
        }

        rowBeingResized.setHeight(newHeight);
        scrollValuesObsolete = true;

        Rectangle clientArea = getClientArea();
        redraw(clientArea.x,clientArea.y,clientArea.width,clientArea.height,false);
    }

    /**
     * Determines if the mouse is hovering on a column resizer and changes the
     * pointer and sets field appropriately.
     *
     * @param x mouse x
     * @param y mouse y
     * @return true if this event has been consumed.
     */
    private boolean handleHoverOnColumnResizer(int x, int y)
    {
        boolean over = false;
        if (y <= headerHeight)
        {
            int x2 = 0;

            if (rowHeaderVisible)
            {
                x2 += rowHeaderWidth;
            }

            x2 -= getHScrollSelectionInPixels();

            for (GridColumn column : displayOrderedColumns) {
                if (!column.isVisible()) {
                    continue;
                }
                x2 += column.getWidth();

                if (x2 >= (x - COLUMN_RESIZER_THRESHOLD) && x2 <= (x + COLUMN_RESIZER_THRESHOLD)) {
                    if (column.getResizeable()) {
                        if (column.getColumnGroup() != null && y <= groupHeaderHeight) {
                            // if this is not the last column
                            if (column != column.getColumnGroup().getLastVisibleColumn()) {
                                break;
                            }
                        }

                        over = true;
                        columnBeingResized = column;
                    }
                    break;
                }
            }
        }

        if (over != hoveringOnColumnResizer)
        {
            if (over)
            {
                setCursor(getDisplay().getSystemCursor(SWT.CURSOR_SIZEWE));
            }
            else
            {
                columnBeingResized = null;
                setCursor(null);
            }
            hoveringOnColumnResizer = over;
        }
        return over;
    }

    /**
     * Determines if the mouse is hovering on a row resizer and changes the
     * pointer and sets field appropriately.
     *
     * @param x mouse x
     * @param y mouse y
     * @return true if this event has been consumed.
     */
    private boolean handleHoverOnRowResizer(int x, int y)
    {
    	rowBeingResized = null;
        boolean over = false;
        if (x <= rowHeaderWidth)
        {
            int y2 = 0;

            if (columnHeadersVisible)
            {
                y2 += headerHeight;
            }

            int row=getTopIndex();
            while(row<items.size() && y2<=getClientArea().height)
            {
                GridItem currItem = items.get(row);
                if (currItem.isVisible())
                {
                	y2 += currItem.getHeight() +1;

                	if (y2 >= (y - ROW_RESIZER_THRESHOLD) && y2 <= (y + ROW_RESIZER_THRESHOLD))
                	{
//                		if (currItem.isResizeable())
                		{
                			over = true;
                			rowBeingResized = currItem;
                		}
                		// do not brake here, because in case of overlapping
                		// row resizers we need to find the last one
                	}
                	else
                	{
                		if(rowBeingResized != null)
                		{
                			// we have passed all (overlapping) row resizers, so break
                			break;
                		}
                	}
                }
                row++;
            }
        }

        if (over != hoveringOnRowResizer)
        {
            if (over)
            {
                setCursor(getDisplay().getSystemCursor(SWT.CURSOR_SIZENS));
            }
            else
            {
                rowBeingResized = null;
                setCursor(null);
            }
            hoveringOnRowResizer = over;
        }
        return over;
    }

    /**
     * Returns the cell at the given point in the receiver or null if no such
     * cell exists. The point is in the coordinate system of the receiver.
     *
     * @param point the point used to locate the item
     * @return the cell at the given point
     * @throws IllegalArgumentException
     * <ul>
     * <li>ERROR_NULL_ARGUMENT - if the point is null</li>
     * </ul>
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public Point getCell(Point point)
    {
        checkWidget();

        if (point == null)
            SWT.error(SWT.ERROR_NULL_ARGUMENT);

        if (point.x < 0 || point.x > getClientArea().width) return null;

        GridItem item = getItem(point);
        GridColumn column = getColumn(point);

        if (item!=null && column!=null)
        {
        	return new Point(columns.indexOf(column),items.indexOf(item));
        }
        else
        {
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
        int insertMarkPosX1 = -1;		// we will populate these values while drawing the cells
        int insertMarkPosX2 = -1;
        int insertMarkPosY = -1;
        boolean insertMarkPosFound = false;

        GridCellSpanManager cellSpanManager = new GridCellSpanManager();

        e.gc.setBackground(getBackground());
        this.drawBackground(e.gc,0,0,getSize().x,getSize().y);

        if (scrollValuesObsolete)
        {
            updateScrollbars();
            scrollValuesObsolete = false;
        }

        int x;
        int y = 0;

        if (columnHeadersVisible)
        {
            paintHeader(e.gc);
            y += headerHeight;
        }

        int availableHeight = getClientArea().height-y;
        int visibleRows = availableHeight / getItemHeight() + 1;
        if (items.size()>0 && availableHeight>0)
        {
            RowRange range = getRowRange(getTopIndex(),availableHeight,false,false);
            if (range.height >= availableHeight)
                visibleRows = range.rows;
            else
                visibleRows = range.rows + (availableHeight-range.height) / getItemHeight() + 1;
        }

        int firstVisibleIndex = getTopIndex();
        int firstItemToDraw = firstVisibleIndex;

        if(hasSpanning) {
        	// We need to find the first Item to draw. An earlier item can row-span the first visible item.
        	for(int rowIndex = 0; rowIndex < firstVisibleIndex; rowIndex++)
        	{
        		GridItem itemForRow = items.get(rowIndex);
                int colIndex = 0;

                int maxRowSpanForItem = 0;
                for (GridColumn column : displayOrderedColumns) {
                    if (!column.isVisible()) {
                        colIndex++;
                        continue;
                    }

                    int rowSpan = itemForRow.getRowSpan(colIndex);
                    maxRowSpanForItem = rowSpan > maxRowSpanForItem ? rowSpan : maxRowSpanForItem;
                }

                if(rowIndex + maxRowSpanForItem >= firstVisibleIndex) {
                	firstItemToDraw = rowIndex;
                	break;
                } else {
                	rowIndex += maxRowSpanForItem;
                }
        	}

        	for(int rowIndex = firstItemToDraw; rowIndex < firstVisibleIndex; rowIndex++)
        	{
        		GridItem itemForRow = items.get(rowIndex);
        		y = y - itemForRow.getHeight() - 1;
        	}
        }

        int row = firstItemToDraw;

        for (int i = 0; i < visibleRows + (firstVisibleIndex - firstItemToDraw); i++)
        {

            x = 0;

            x -= getHScrollSelectionInPixels();

            // get the item to draw
            GridItem item = null;
            if (row < items.size())
            {
                item = items.get(row);

                while (!item.isVisible() && row < items.size() - 1)
                {
                    row++;
                    item = items.get(row);
                }
            }
            if (item != null && !item.isVisible())
            {
                item = null;
            }

            if (item != null)
            {
                boolean cellInRowSelected = false;


                if (rowHeaderVisible)
                {

                    // row header is actually painted later
                    x += rowHeaderWidth;
                }

                int focusY = y;

                int colIndex = 0;

                // draw regular cells for each column
                for (GridColumn column : displayOrderedColumns) {

                    boolean skipCell = cellSpanManager.skipCell(colIndex, row);
                    int indexOfColumn = indexOf(column);

                    if (!column.isVisible()) {
                        colIndex++;
                        if (skipCell) {
                            cellSpanManager.consumeCell(colIndex, row);
                        }
                        continue;
                    }

                    int width = item.getCellSize(indexOfColumn).x;

                    if (!skipCell) {

                        int nrRowsToSpan = item.getRowSpan(indexOfColumn);
                        int nrColumnsToSpan = item.getColumnSpan(indexOfColumn);

                        if (nrRowsToSpan > 0 || nrColumnsToSpan > 0) {
                            cellSpanManager.addCellSpanInfo(colIndex, row, nrColumnsToSpan, nrRowsToSpan);
                        }

                        if (x + width >= 0 && x < getClientArea().width) {
                            Point sizeOfColumn = item.getCellSize(indexOf(column));

                            column.getCellRenderer().setBounds(x, y, width, sizeOfColumn.y);
                            int cellInHeaderDelta = headerHeight - y;
                            if (cellInHeaderDelta > 0) {
                                e.gc.setClipping(new Rectangle(x - 1, y + cellInHeaderDelta, width + 1,
                                                               sizeOfColumn.y + 2 - cellInHeaderDelta));
                            } else {
                                e.gc.setClipping(new Rectangle(x - 1, y - 1, width + 1, sizeOfColumn.y + 2));
                            }

                            column.getCellRenderer().setRow(i + 1);

                            column.getCellRenderer().setSelected(selectedItems.contains(item));
                            column.getCellRenderer().setFocus(this.isFocusControl());
                            column.getCellRenderer().setRowFocus(focusItem == item);
                            column.getCellRenderer().setCellFocus(
                                cellSelectionEnabled && focusItem == item && focusColumn == column);

                            column.getCellRenderer().setRowHover(hoveringItem == item);
                            column.getCellRenderer().setColumnHover(hoveringColumn == column);

                            if (selectedCells.contains(new Point(indexOf(column), row))) {
                                column.getCellRenderer().setCellSelected(true);
                                cellInRowSelected = true;
                            } else {
                                column.getCellRenderer().setCellSelected(false);
                            }

                            if (hoveringItem == item && hoveringColumn == column) {
                                column.getCellRenderer().setHoverDetail(hoveringDetail);
                            } else {
                                column.getCellRenderer().setHoverDetail("");
                            }

                            column.getCellRenderer().paint(e.gc, item);

                            e.gc.setClipping((Rectangle) null);

                            // collect the insertMark position
                            if (!insertMarkPosFound && insertMarkItem == item && (insertMarkColumn == null || insertMarkColumn == column)) {
                                // y-pos
                                insertMarkPosY = y - 1;
                                if (!insertMarkBefore)
                                    insertMarkPosY += item.getHeight() + 1;
                                // x1-pos
                                insertMarkPosX1 = x;
                                if (column.isTree()) {
                                    insertMarkPosX1 += Math.min(
                                        width,
                                        column.getCellRenderer().getTextBounds(item, false).x);
                                }

                                // x2-pos
                                if (insertMarkColumn == null) {
                                    insertMarkPosX2 = getClientArea().x + getClientArea().width;
                                } else {
                                    insertMarkPosX2 = x + width;
                                }

                                insertMarkPosFound = true;
                            }
                        }
                    } else {
                        cellSpanManager.consumeCell(colIndex, row);
                    }

                    x += column.getWidth();
                    colIndex++;

                }

                if (x < getClientArea().width)
                {
                    // insertMarkPos needs correction
                    if(insertMarkPosFound && insertMarkColumn == null)
                        insertMarkPosX2 = x;

                    emptyCellRenderer.setSelected(selectedItems.contains(item));
                    emptyCellRenderer.setFocus(this.isFocusControl());
                    emptyCellRenderer.setRow(i + 1);
                    emptyCellRenderer.setBounds(x, y, getClientArea().width - x + 1, item.getHeight());
                    emptyCellRenderer.setColumn(getColumnCount());
                    emptyCellRenderer.paint(e.gc, item);
                }

                x = 0;

                if (rowHeaderVisible)
                {

                    if (!cellSelectionEnabled)
                    {
                        rowHeaderRenderer.setSelected(selectedItems.contains(item));
                    }
                    else
                    {
                        rowHeaderRenderer.setSelected(cellInRowSelected);
                    }
                    if(y >= headerHeight)
                    {
	                    rowHeaderRenderer.setBounds(0, y, rowHeaderWidth, item.getHeight() + 1);
	                    rowHeaderRenderer.paint(e.gc, item);
                    }
                    x += rowHeaderWidth;
                }

                // focus
                if (isFocusControl() && !cellSelectionEnabled)
                {
                    if (item == focusItem)
                    {
                        if (focusRenderer != null)
                        {
                            int focusX = 0;
                            if (rowHeaderVisible)
                            {
                                focusX = rowHeaderWidth;
                            }
                            focusRenderer
                                .setBounds(focusX, focusY - 1, getClientArea().width - focusX - 1,
                                		item.getHeight() + 1);
                            focusRenderer.paint(e.gc, item);
                        }
                    }
                }

                y += item.getHeight() + 1;
            }
            else
            {

                if (rowHeaderVisible)
                {
                    //row header is actually painted later
                    x += rowHeaderWidth;
                }

                emptyCellRenderer.setBounds(x, y, getClientArea().width - x, getItemHeight());
                emptyCellRenderer.setFocus(false);
                emptyCellRenderer.setSelected(false);
                emptyCellRenderer.setRow(i + 1);

                for (GridColumn column : displayOrderedColumns) {
                    if (column.isVisible()) {
                        emptyCellRenderer.setBounds(x, y, column.getWidth(), getItemHeight());
                        emptyCellRenderer.setColumn(indexOf(column));
                        emptyCellRenderer.paint(e.gc, this);

                        x += column.getWidth();
                    }
                }

                if (x < getClientArea().width)
                {
                    emptyCellRenderer.setBounds(x, y, getClientArea().width - x + 1, getItemHeight());
                    emptyCellRenderer.setColumn(getColumnCount());
                    emptyCellRenderer.paint(e.gc, this);
                }


                x = 0;

                if (rowHeaderVisible)
                {
                    emptyRowHeaderRenderer.setBounds(x, y, rowHeaderWidth, getItemHeight() + 1);
                    emptyRowHeaderRenderer.paint(e.gc, this);

                    x += rowHeaderWidth;
                }

                y += getItemHeight() + 1;
            }

            row++;
        }

        // draw drop point
        if (draggingColumn)
        {
            if ((dragDropAfterColumn != null || dragDropBeforeColumn != null)
                && (dragDropAfterColumn != columnBeingPushed && dragDropBeforeColumn != columnBeingPushed)
                && dragDropPointValid)
            {
                if (dragDropBeforeColumn != null)
                {
                    x = getColumnHeaderXPosition(dragDropBeforeColumn);
                }
                else
                {
                    x = getColumnHeaderXPosition(dragDropAfterColumn)
                        + dragDropAfterColumn.getWidth();
                }

                Point size = dropPointRenderer.computeSize(e.gc, SWT.DEFAULT, SWT.DEFAULT, null);
                x -= size.x / 2;
                if (x < 0)
                {
                    x = 0;
                }
                dropPointRenderer.setBounds(x - 1, headerHeight + DROP_POINT_LOWER_OFFSET, size.x,
                                            size.y);
                dropPointRenderer.paint(e.gc, null);
            }
        }

        // draw insertion mark
        if (insertMarkPosFound)
        {
            e.gc.setClipping(
                    rowHeaderVisible ? rowHeaderWidth : 0,
                    columnHeadersVisible ? headerHeight : 0,
                    getClientArea().width,
                    getClientArea().height);
            insertMarkRenderer.paint(e.gc, new Rectangle(insertMarkPosX1, insertMarkPosY, insertMarkPosX2 - insertMarkPosX1, 0));
        }

        if (columnFootersVisible)
        {
            paintFooter(e.gc);
        }
    }

    /**
     * Returns a column reference if the x,y coordinates are over a column
     * header (header only).
     *
     * @param x mouse x
     * @param y mouse y
     * @return column reference which mouse is over, or null.
     */
    private GridColumn overColumnHeader(int x, int y)
    {
        GridColumn col = null;

        if (y <= headerHeight && y > 0)
        {
            col = getColumn(new Point(x, y));
            if (col != null && col.getColumnGroup() != null)
            {
                if (y <= groupHeaderHeight)
                {
                    return null;
                }
            }
        }

        return col;
    }

    /**
     * Returns a column reference if the x,y coordinates are over a column
     * header (header only).
     *
     * @param x mouse x
     * @param y mouse y
     * @return column reference which mouse is over, or null.
     */
    private GridColumn overColumnFooter(int x, int y)
    {
        GridColumn col = null;

        if (y >= getClientArea().height - footerHeight )
        {
            col = getColumn(new Point(x, y));
        }

        return col;
    }

    /**
     * Returns a column group reference if the x,y coordinates are over a column
     * group header (header only).
     *
     * @param x mouse x
     * @param y mouse y
     * @return column group reference which mouse is over, or null.
     */
    private GridColumnGroup overColumnGroupHeader(int x, int y)
    {
        GridColumnGroup group = null;

        if (y <= groupHeaderHeight && y > 0)
        {
            GridColumn col = getColumn(new Point(x, y));
            if (col != null)
            {
                group = col.getColumnGroup();
            }
        }

        return group;
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

        if (rowHeaderVisible)
        {
            // paint left corner
            // topLeftRenderer.setBounds(0, y, rowHeaderWidth, headerHeight);
            // topLeftRenderer.paint(gc, null);
            x += rowHeaderWidth;
        }

        GridColumnGroup previousPaintedGroup = null;

        for (GridColumn column : displayOrderedColumns) {
            if (x > getClientArea().width)
                break;

            int height;

            if (!column.isVisible()) {
                continue;
            }

            if (column.getColumnGroup() != null) {

                if (column.getColumnGroup() != previousPaintedGroup) {
                    int width = column.getWidth();

                    GridColumn nextCol = null;
                    if (displayOrderedColumns.indexOf(column) + 1 < displayOrderedColumns.size()) {
                        nextCol = displayOrderedColumns
                            .get(displayOrderedColumns.indexOf(column) + 1);
                    }

                    while (nextCol != null && nextCol.getColumnGroup() == column.getColumnGroup()) {

                        if ((nextCol.getColumnGroup().getExpanded() && !nextCol.isDetail())
                            || (!nextCol.getColumnGroup().getExpanded() && !nextCol.isSummary())) {
                        } else {
                            width += nextCol.getWidth();
                        }

                        if (displayOrderedColumns.indexOf(nextCol) + 1 < displayOrderedColumns
                            .size()) {
                            nextCol = displayOrderedColumns.get(displayOrderedColumns.indexOf(nextCol) + 1);
                        } else {
                            nextCol = null;
                        }
                    }

                    boolean selected = true;

                    for (int i = 0; i < column.getColumnGroup().getColumns().length; i++) {
                        GridColumn col = column.getColumnGroup().getColumns()[i];
                        if (col.isVisible() && (column.getMoveable() || !selectedColumns.contains(col))) {
                            selected = false;
                            break;
                        }
                    }


                    column.getColumnGroup().getHeaderRenderer().setSelected(selected);
                    column.getColumnGroup().getHeaderRenderer()
                        .setHover(hoverColumnGroupHeader == column.getColumnGroup());
                    column.getColumnGroup().getHeaderRenderer().setHoverDetail(hoveringDetail);

                    column.getColumnGroup().getHeaderRenderer().setBounds(x, 0, width,
                                                                          groupHeaderHeight);

                    column.getColumnGroup().getHeaderRenderer().paint(gc, column.getColumnGroup());

                    previousPaintedGroup = column.getColumnGroup();
                }

                height = headerHeight - groupHeaderHeight;
                y = groupHeaderHeight;
            } else {
                height = headerHeight;
                y = 0;
            }

            if (pushingColumn) {
                column.getHeaderRenderer().setHover(
                    columnBeingPushed == column
                        && pushingAndHovering);
            } else {
                column.getHeaderRenderer().setHover(hoveringColumnHeader == column);
            }

            column.getHeaderRenderer().setHoverDetail(hoveringDetail);

            column.getHeaderRenderer().setBounds(x, y, column.getWidth(), height);

            if (cellSelectionEnabled)
                column.getHeaderRenderer().setSelected(selectedColumns.contains(column));

            if (x + column.getWidth() >= 0) {
                column.getHeaderRenderer().paint(gc, column);
            }

            x += column.getWidth();
        }

        if (x < getClientArea().width)
        {
            emptyColumnHeaderRenderer.setBounds(x, 0, getClientArea().width - x, headerHeight);
            emptyColumnHeaderRenderer.paint(gc, null);
        }

        x = 0;

        if (rowHeaderVisible)
        {
            // paint left corner
            topLeftRenderer.setBounds(0, 0, rowHeaderWidth, headerHeight);
            topLeftRenderer.paint(gc, this);
            x += rowHeaderWidth;
        }

        if (draggingColumn)
        {

            gc.setAlpha(COLUMN_DRAG_ALPHA);

            columnBeingPushed.getHeaderRenderer().setSelected(false);

            int height;

            if (columnBeingPushed.getColumnGroup() != null)
            {
                height = headerHeight - groupHeaderHeight;
                y = groupHeaderHeight;
            }
            else
            {
                height = headerHeight;
                y = 0;
            }

            columnBeingPushed.getHeaderRenderer()
                .setBounds(
                           getColumnHeaderXPosition(columnBeingPushed)
                               + (currentHeaderDragX - startHeaderDragX), y,
                           columnBeingPushed.getWidth(), height);
            columnBeingPushed.getHeaderRenderer().paint(gc, columnBeingPushed);
            columnBeingPushed.getHeaderRenderer().setSelected(false);

            gc.setAlpha(-1);
            gc.setAdvanced(false);
        }

    }

    private void paintFooter(GC gc) {
    	int x = 0;
        int y;

        x -= getHScrollSelectionInPixels();

        if (rowHeaderVisible)
        {
            // paint left corner
            // topLeftRenderer.setBounds(0, y, rowHeaderWidth, headerHeight);
            // topLeftRenderer.paint(gc, null);
            x += rowHeaderWidth;
        }

        for (GridColumn column : displayOrderedColumns) {
            if (x > getClientArea().width)
                break;

            int height;

            if (!column.isVisible()) {
                continue;
            }

            height = footerHeight;
            y = getClientArea().height - height;

            column.getFooterRenderer().setBounds(x, y, column.getWidth(), height);
            if (x + column.getWidth() >= 0) {
                column.getFooterRenderer().paint(gc, column);
            }

            x += column.getWidth();
        }

      if (x < getClientArea().width)
      {
    	  emptyColumnFooterRenderer.setBounds(x, getClientArea().height - footerHeight, getClientArea().width - x, footerHeight);
    	  emptyColumnFooterRenderer.paint(gc, null);
      }

      if (rowHeaderVisible)
      {
        // paint left corner
    	bottomLeftRenderer.setBounds(0, getClientArea().height-footerHeight, rowHeaderWidth, footerHeight);
    	bottomLeftRenderer.paint(gc, this);
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
        // that if a scrollbar is turned on/off, the other scrollbar's
        // visibility may be affected (more
        // area may have been added/removed.
        for (int doublePass = 1; doublePass <= 2; doublePass++)
        {

            if (preferredSize.y > clientArea.height)
            {
                vScroll.setVisible(true);
            }
            else
            {
                vScroll.setVisible(false);
                vScroll.setValues(0, 0, 1, 1, 1, 1);
            }
            if (preferredSize.x > clientArea.width)
            {
                hScroll.setVisible(true);
            }
            else
            {
                hScroll.setVisible(false);
                hScroll.setValues(0, 0, 1, 1, 1, 1);
            }

            // get the clientArea again with the now visible/invisible
            // scrollbars
            clientArea = getClientArea();
        }

        // if the scrollbar is visible set its values
        if (vScroll.getVisible())
        {
            int max = currentVisibleItems;
            int thumb = 1;

            if(!hasDifferingHeights)
            {
            	// in this case, the number of visible rows on screen is constant,
            	// so use this as thumb
                thumb = ( getVisibleGridHeight() + 1 ) / ( getItemHeight() + 1 );
            }
            else
            {
                // in this case, the number of visible rows on screen is variable,
            	// so we have to use 1 as thumb and decrease max by the number of
            	// rows on the last page
            	if(getVisibleGridHeight()>=1) {
            		RowRange range = getRowRange(-1,getVisibleGridHeight(),true,true);
            		max -= range.rows - 1;
            	}
            }

            // if possible, remember selection, if selection is too large, just
            // make it the max you can
            int selection = Math.min(vScroll.getSelection(), max);

            vScroll.setValues(selection, 0, max, thumb, 1, thumb);
        }

        // if the scrollbar is visible set its values
        if (hScroll.getVisible())
        {

            if (!columnScrolling)
            {
                // horizontal scrolling works pixel by pixel

                int hiddenArea = preferredSize.x - clientArea.width + 1;

                // if possible, remember selection, if selection is too large,
                // just
                // make it the max you can
                int selection = Math.min(hScroll.getSelection(), hiddenArea - 1);

                hScroll.setValues(selection, 0, hiddenArea + clientArea.width - 1, clientArea.width,
                                 HORZ_SCROLL_INCREMENT, clientArea.width);
            }
            else
            {
                // horizontal scrolling is column by column

                int hiddenArea = preferredSize.x - clientArea.width + 1;

                int max = 0;
                int i = 0;

                while (hiddenArea > 0 && i < getColumnCount())
                {
                    GridColumn col = displayOrderedColumns.get(i);

                    i++;

                    if (col.isVisible())
                    {
                        hiddenArea -= col.getWidth();
                        max++;
                    }
                }

                max++;

                // max should never be greater than the number of visible cols
                int visCols = 0;
                for (GridColumn element : columns) {
                    if (element.isVisible()) {
                        visCols++;
                    }
                }
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
     * Adds/removes items from the selected items list based on the
     * selection/deselection of the given item.
     *
     * @param item item being selected/unselected
     * @param stateMask key state during selection
     *
     * @return selection event that needs to be fired or null
     */
    private Event updateSelection(GridItem item, int stateMask)
    {
        if (!selectionEnabled)
        {
            return null;
        }

        Event selectionEvent = null;

        if (selectionType == SWT.SINGLE)
        {
            if (selectedItems.contains(item)) return null;

            selectedItems.clear();
            selectedItems.add(item);

            Rectangle clientArea = getClientArea();
            redraw(clientArea.x,clientArea.y,clientArea.width,clientArea.height,false);

            selectionEvent = new Event();
            selectionEvent.item = item;
        }
        else if (selectionType == SWT.MULTI)
        {
            boolean shift = false;
            boolean ctrl = false;

            if ((stateMask & SWT.MOD2) == SWT.MOD2)
            {
                shift = true;
            }

            if ((stateMask & SWT.MOD1) == SWT.MOD1)
            {
                ctrl = true;
            }

            if (!shift && !ctrl)
            {
                if (selectedItems.size() == 1 && selectedItems.contains(item)) return null;

                selectedItems.clear();

                selectedItems.add(item);

                Rectangle clientArea = getClientArea();
                redraw(clientArea.x,clientArea.y,clientArea.width,clientArea.height,false);

                shiftSelectionAnchorItem = null;

                selectionEvent = new Event();
                selectionEvent.item = item;
            }
            else if (shift)
            {

                if (shiftSelectionAnchorItem == null)
                {
                    shiftSelectionAnchorItem = focusItem;
                }

//                if (shiftSelectionAnchorItem == item)
//                {
//                    return;
//                }

                boolean maintainAnchorSelection = false;

                if (!ctrl)
                {
                    if (selectedItems.contains(shiftSelectionAnchorItem))
                    {
                        maintainAnchorSelection = true;
                    }
                    selectedItems.clear();
                }

                int anchorIndex = items.indexOf(shiftSelectionAnchorItem);
                int itemIndex = items.indexOf(item);

                int min;
                int max;

                if (anchorIndex < itemIndex)
                {
                    if (maintainAnchorSelection)
                    {
                        min = anchorIndex;
                    }
                    else
                    {
                        min = anchorIndex + 1;
                    }
                    max = itemIndex;
                }
                else
                {
                    if (maintainAnchorSelection)
                    {
                        max = anchorIndex;
                    }
                    else
                    {
                        max = anchorIndex - 1;
                    }
                    min = itemIndex;
                }

                for (int i = min; i <= max; i++)
                {
                    if (!selectedItems.contains(items.get(i)) && items.get(i).isVisible())
                    {
                        selectedItems.add(items.get(i));
                    }
                }
                Rectangle clientArea = getClientArea();
                redraw(clientArea.x,clientArea.y,clientArea.width,clientArea.height,false);

                selectionEvent = new Event();
            }
            else if (ctrl)
            {
                if (selectedItems.contains(item))
                {
                    selectedItems.remove(item);
                }
                else
                {
                    selectedItems.add(item);
                }
                Rectangle clientArea = getClientArea();
                redraw(clientArea.x,clientArea.y,clientArea.width,clientArea.height,false);

                shiftSelectionAnchorItem = null;

                selectionEvent = new Event();
                selectionEvent.item = item;
            }
        }

        Rectangle clientArea = getClientArea();
        redraw(clientArea.x,clientArea.y,clientArea.width,clientArea.height,false);

        return selectionEvent;
    }

    /**
     * Updates cell selection.
     *
     * @param newCell newly clicked, navigated to cell.
     * @param stateMask statemask during preceeding mouse or key event.
     * @param dragging true if the user is dragging.
     * @param reverseDuplicateSelections true if the user is reversing selection rather than adding to.
     *
     * @return selection event that will need to be fired or null.
     */
    private Event updateCellSelection(Point newCell, int stateMask, boolean dragging, boolean reverseDuplicateSelections)
    {
        List<Point> v = new ArrayList<Point>();
        v.add(newCell);
        return updateCellSelection(v, stateMask, dragging, reverseDuplicateSelections);
    }

    /**
     * Updates cell selection.
     *
     * @param newCell newly clicked, navigated to cells.
     * @param stateMask statemask during preceeding mouse or key event.
     * @param dragging true if the user is dragging.
     * @param reverseDuplicateSelections true if the user is reversing selection rather than adding to.
     *
     * @return selection event that will need to be fired or null.
     */
    private Event updateCellSelection(List<Point> newCells, int stateMask, boolean dragging, boolean reverseDuplicateSelections)
    {
        boolean shift = false;
        boolean ctrl = false;

        if ((stateMask & SWT.MOD2) == SWT.MOD2)
        {
            shift = true;
        }
        else
        {
            shiftSelectionAnchorColumn = null;
            shiftSelectionAnchorItem = null;
        }

        if ((stateMask & SWT.MOD1) == SWT.MOD1)
        {
            ctrl = true;
        }

        if (!shift && !ctrl)
        {
            if (newCells.equals(selectedCells)) return null;

            selectedCells.clear();
            for (Point newCell : newCells) {
                addToCellSelection(newCell);
            }

        }
        else if (shift)
        {

            Point newCell = newCells.get(0); //shift selection should only occur with one
            //cell, ignoring others

            if ((focusColumn == null) || (focusItem == null))
            {
                return null;
            }

            shiftSelectionAnchorColumn = getColumn(newCell.x);
            shiftSelectionAnchorItem = getItem(newCell.y);

            if (ctrl)
            {
                selectedCells.clear();
                selectedCells.addAll(selectedCellsBeforeRangeSelect);
            }
            else
            {
                selectedCells.clear();
            }




            GridColumn currentColumn = focusColumn;
            GridItem currentItem = focusItem;

            GridColumn endColumn = getColumn(newCell.x);
            GridItem endItem = getItem(newCell.y);

            Point newRange = getSelectionRange(currentItem,currentColumn,endItem,endColumn);

            currentColumn = getColumn(newRange.x);
            endColumn = getColumn(newRange.y);

            GridColumn startCol = currentColumn;

            if (indexOf(currentItem) > indexOf(endItem))
            {
                GridItem temp = currentItem;
                currentItem = endItem;
                endItem = temp;
            }

            boolean firstLoop = true;

            do
            {
                if (!firstLoop)
                {
                    currentItem = getNextVisibleItem(currentItem);
                }

                firstLoop = false;

                boolean firstLoop2 = true;

                currentColumn = startCol;

                do
                {
                    if (!firstLoop2)
                    {
                        int index = displayOrderedColumns.indexOf(currentColumn) + 1;

                        if (index < displayOrderedColumns.size())
                        {
                            currentColumn = getVisibleColumn_DegradeRight(currentItem, displayOrderedColumns.get(index));
                        }
                        else
                        {
                            currentColumn = null;
                        }

                        if (currentColumn!= null)
                            if (displayOrderedColumns.indexOf(currentColumn) > displayOrderedColumns.indexOf(endColumn))
                                currentColumn = null;
                    }

                    firstLoop2 = false;

                    if (currentColumn != null)
                    {
                        Point cell = new Point(indexOf(currentColumn),indexOf(currentItem));
                        addToCellSelection(cell);
                    }
                } while (currentColumn != endColumn && currentColumn != null);
            } while (currentItem != endItem);
        }
        else if (ctrl)
        {
            boolean reverse = reverseDuplicateSelections;
            if (!selectedCells.containsAll(newCells))
                reverse = false;

            if (dragging)
            {
                selectedCells.clear();
                selectedCells.addAll(selectedCellsBeforeRangeSelect);
            }

            if (reverse)
            {
                selectedCells.removeAll(newCells);
            }
            else
            {
                for (Point newCell : newCells) {
                    addToCellSelection(newCell);
                }
            }
        }

        updateColumnSelection();

        Event e = new Event();
        if (dragging)
        {
            e.detail = SWT.DRAG;
            followupCellSelectionEventOwed = true;
        }

        Rectangle clientArea = getClientArea();
        redraw(clientArea.x,clientArea.y,clientArea.width,clientArea.height,false);

        return e;
    }

    private void addToCellSelection(Point newCell)
    {
        if (newCell.x < 0 || newCell.x >= columns.size())
            return;

        if (newCell.y < 0 || newCell.y >= items.size())
            return;

        if (getColumn(newCell.x).getCellSelectionEnabled())
        {
        	Iterator it = selectedCells.iterator();
        	boolean found = false;
        	while( it.hasNext() ) {
        		Point p = (Point) it.next();
        		if( newCell.equals(p) ) {
        			found = true;
        			break;
        		}
        	}

        	if( ! found ) {
        		selectedCells.add(newCell);
        	}
        }
    }

    void updateColumnSelection()
    {
        //Update the list of which columns have all their cells selected
        selectedColumns.clear();

        for (Point cell : selectedCells) {
            GridColumn col = getColumn(cell.x);

            selectedColumns.add(col);
        }
    }

    /**
     * Initialize all listeners.
     */
    private void initListeners()
    {
        disposeListener = new Listener()
        {
            public void handleEvent(Event e)
            {
                onDispose(e);
            }
        };
        addListener(SWT.Dispose,disposeListener);

        addPaintListener(new PaintListener()
        {
            public void paintControl(PaintEvent e)
            {
                onPaint(e);
            }
        });

        addListener(SWT.Resize, new Listener()
        {
            public void handleEvent(Event e)
            {
                onResize();
            }
        });

        if (getVerticalBar() != null)
        {
            getVerticalBar().addListener(SWT.Selection, new Listener()
            {
                public void handleEvent(Event e)
                {
                    onScrollSelection();
                }
            });
        }

        if (getHorizontalBar() != null)
        {
            getHorizontalBar().addListener(SWT.Selection, new Listener()
            {
                public void handleEvent(Event e)
                {
                    onScrollSelection();
                }
            });
        }

        addListener(SWT.KeyDown, new Listener()
        {
            public void handleEvent(Event e)
            {
                onKeyDown(e);
            }
        });

        addTraverseListener(new TraverseListener()
        {
            public void keyTraversed(TraverseEvent e)
            {
                e.doit = true;
            }
        });

        addMouseListener(new MouseListener()
        {
            public void mouseDoubleClick(MouseEvent e)
            {
                onMouseDoubleClick(e);
            }

            public void mouseDown(MouseEvent e)
            {
                onMouseDown(e);
            }

            public void mouseUp(MouseEvent e)
            {
                onMouseUp(e);
            }
        });

        addMouseMoveListener(new MouseMoveListener()
        {
            public void mouseMove(MouseEvent e)
            {
                onMouseMove(e);
            }
        });

        addMouseTrackListener(new MouseTrackListener()
        {
            public void mouseEnter(MouseEvent e)
            {
            }

            public void mouseExit(MouseEvent e)
            {
                onMouseExit(e);
            }

            public void mouseHover(MouseEvent e)
            {
            }
        });

        addFocusListener(new FocusListener()
        {
            public void focusGained(FocusEvent e)
            {
            	onFocusIn();
                redraw();
            }

            public void focusLost(FocusEvent e)
            {
                redraw();
            }
        });

        // Special code to reflect mouse wheel events if using an external
        // scroller
        addListener(SWT.MouseWheel, new Listener()
        {
            public void handleEvent(Event e)
            {
                onMouseWheel(e);
            }
        });
    }

    private void onFocusIn()
    {
    	if (!items.isEmpty() && focusItem == null)
    	{
    		focusItem = items.get(0);
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

        cellHeaderSelectionBackground.dispose();

        for (GridItem item : items) {
            item.dispose();
        }

        for (GridColumnGroup columnGroup : columnGroups) {
            columnGroup.dispose();
        }

        for (GridColumn col : columns) {
            col.dispose();
        }

        sizingGC.dispose();
    }

    /**
     * Mouse wheel event handler.
     *
     * @param e event
     */
    private void onMouseWheel(Event e)
    {
        if (vScroll.getVisible())
        {
            vScroll.handleMouseWheel(e);
            if (getVerticalBar() == null)
            	e.doit = false;
        }
        else if (hScroll.getVisible())
        {
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
        // children can get focus instead. This only affects the table
        // when an editor is open
        // and therefore the table has a child. The solution is to
        // forceFocus()
        if ((getStyle() & SWT.NO_FOCUS) != SWT.NO_FOCUS)
        {
            forceFocus();
        }

        hideToolTip();

        //if populated will be fired at end of method.
        Event selectionEvent = null;

        cellSelectedOnLastMouseDown = false;
        cellRowSelectedOnLastMouseDown = false;
        cellColumnSelectedOnLastMouseDown = false;

        if (hoveringOnColumnResizer)
        {
            if (e.button == 1)
            {
                resizingColumn = true;
                resizingStartX = e.x;
                resizingColumnStartWidth = columnBeingResized.getWidth();
            }
            return;
        }
        if (rowsResizeable && hoveringOnRowResizer)
        {
        	if (e.button == 1)
        	{
        		resizingRow = true;
        		resizingStartY = e.y;
        		resizingRowStartHeight = rowBeingResized.getHeight();
        	}
        	return;
        }


        if (e.button == 1 && handleColumnHeaderPush(e.x, e.y))
        {
            return;
        }

        if (e.button == 1 && handleColumnGroupHeaderClick(e.x, e.y))
        {
            return;
        }

        if(e.button == 1 && handleColumnFooterPush(e.x,e.y)) {
        	return;
        }

        GridItem item = getItem(new Point(e.x, e.y));

        if (e.button == 1 && item != null && handleCellClick(item,e.x, e.y))
        {
            return;
        }

        if (isListening(SWT.DragDetect))
        {
	        if ((cellSelectionEnabled && hoveringOnSelectionDragArea) ||
	        	(!cellSelectionEnabled && item != null && selectedItems.contains(item)))
	        {
	        	if(dragDetect(e))
	        	{
	        		return;
	        	}
	        }
        }

        if (item != null)
        {
            if (cellSelectionEnabled)
            {
                GridColumn col = getColumn(new Point(e.x, e.y));
                boolean isSelectedCell = false;
                if (col != null)
                    isSelectedCell = selectedCells.contains(new Point(indexOf(col),indexOf(item)));

                if (e.button == 1 || (e.button == 3 && col != null && !isSelectedCell))
                {
                    if (col != null)
                    {
                        selectionEvent = updateCellSelection(new Point(indexOf(col),indexOf(item)), e.stateMask, false, true);
                        cellSelectedOnLastMouseDown = (getCellSelectionCount() > 0);

                        if (e.stateMask != SWT.MOD2)
                        {
                            focusColumn = col;
                            focusItem = item;
                        }
                        //showColumn(col);
                        showItem(item);
                        redraw();
                    }
                    else if (rowHeaderVisible)
                    {
                        if (e.x <= rowHeaderWidth)
                        {

                            boolean shift = ((e.stateMask & SWT.MOD2) != 0);
                            boolean ctrl = false;
                            if (!shift)
                            {
                                ctrl = ((e.stateMask & SWT.MOD1) != 0);
                            }

                            List<Point> cells = new ArrayList<Point>();

                            if (shift)
                            {
                                getCells(item,focusItem,cells);
                            }
                            else
                            {
                                getCells(item,cells);
                            }

                            int newStateMask = SWT.NONE;
                            if (ctrl) newStateMask = SWT.MOD1;

                            selectionEvent = updateCellSelection(cells, newStateMask, shift, ctrl);
                            cellRowSelectedOnLastMouseDown = (getCellSelectionCount() > 0);

                            if (!shift)
                            {
                                //set focus back to the first visible column
                                focusColumn = getColumn(new Point(rowHeaderWidth + 1,e.y));

                                focusItem = item;
                            }
                            showItem(item);
                            redraw();
                        }
                    }
                    intendedFocusColumn = focusColumn;
                }
            }
            else
            {
            	if (e.button == 2 || e.button > 3)
            	{
            		return;
            	}

                if (e.button == 3 && selectionType == SWT.MULTI)
                {
                    if ((e.stateMask & SWT.MOD2) == SWT.MOD2)
                    {
                        return;
                    }

                    if ((e.stateMask & SWT.MOD1) == SWT.MOD1)
                    {
                    	return;
                    }

                    if (selectedItems.contains(item))
                    {
                        return;
                    }
                }
                selectionEvent = updateSelection(item, e.stateMask);


                focusItem = item;
                showItem(item);
                redraw();
            }
        }
        else if (cellSelectionEnabled && e.button == 1 && rowHeaderVisible && e.x <= rowHeaderWidth && e.y < headerHeight)
        {
        	// Nothing to select
        	if(items.size() == 0) {
        		return;
        	}

            //click on the top left corner means select everything
            selectionEvent = selectAllCellsInternal();

            focusColumn = getColumn(new Point(rowHeaderWidth + 1,1));
            focusItem = getItem(getTopIndex());
        }
        else if (cellSelectionEnabled && e.button == 1 && columnHeadersVisible && e.y <= headerHeight)
        {
            //column cell selection
            GridColumn col = getColumn(new Point(e.x,e.y));

            if (col == null) return;

            if (getItemCount() == 0)
                return;

            List<Point> cells = new ArrayList<Point>();

            GridColumnGroup group = col.getColumnGroup();
            if (group != null && e.y < groupHeaderHeight) {
                getCells(group, cells);
            } else {
                getCells(col, cells);
            }

            selectionEvent = updateCellSelection(cells, e.stateMask, false, true);
            cellColumnSelectedOnLastMouseDown = (getCellSelectionCount() > 0);

            GridItem newFocusItem = getItem(0);

            while (newFocusItem != null && getSpanningColumn(newFocusItem, col) != null)
            {
                newFocusItem = getNextVisibleItem(newFocusItem);
            }

            if (newFocusItem != null)
            {
                focusColumn = col;
                focusItem = newFocusItem;
            }

            showColumn(col);
            redraw();
        }

        if (selectionEvent != null)
        {
            selectionEvent.stateMask = e.stateMask;
            selectionEvent.button = e.button;
            selectionEvent.item = item;
            selectionEvent.x = e.x;
            selectionEvent.y = e.y;
            notifyListeners(SWT.Selection, selectionEvent);

            if (!cellSelectionEnabled)
            {
                if (isListening(SWT.DragDetect))
                {
                	dragDetect(e);
                }
            }
        }


    }

    /**
     * Mouse double click event handler.
     *
     * @param e event
     */
    private void onMouseDoubleClick(MouseEvent e)
    {
        if (e.button == 1)
        {

            if (hoveringOnColumnResizer)
            {
                columnBeingResized.pack();
                columnBeingResized.fireResized();
                for (int index = displayOrderedColumns.indexOf(columnBeingResized) + 1; index < displayOrderedColumns.size(); index ++)
                {
                    GridColumn col = displayOrderedColumns.get(index);
                    if (col.isVisible()) col.fireMoved();
                }
                resizingColumn = false;
                handleHoverOnColumnResizer(e.x, e.y);
                return;
            }
            else if (rowsResizeable && hoveringOnRowResizer) {
                List sel = Arrays.asList(getSelection());
                if(sel.contains(rowBeingResized))
                {
                	// the user double-clicked a row resizer of a selected row
                	// so update all selected rows
                    for(int cnt=0;cnt<sel.size();cnt++)
                        ((GridItem)sel.get(cnt)).pack();
                    redraw();
                }
                else
                {
                	// otherwise only update the row the user double-clicked
                	rowBeingResized.pack();
                }

                resizingRow = false;
                handleHoverOnRowResizer(e.x, e.y);
                return;
            }

            GridItem item = getItem(new Point(e.x, e.y));
            if (item != null)
            {
            	if (isListening(SWT.DefaultSelection))
            	{
                Event newEvent = new Event();
                newEvent.item = item;

                notifyListeners(SWT.DefaultSelection, newEvent);
            	}
            	else if (item.getItemCount() > 0)
            	{
          			item.setExpanded(!item.isExpanded());

                    if (item.isExpanded())
                    {
                        item.fireEvent(SWT.Expand);
                    }
                    else
                    {
                        item.fireEvent(SWT.Collapse);
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
    	cellSelectedOnLastMouseDown = false;

        if (resizingColumn)
        {
            resizingColumn = false;
            handleHoverOnColumnResizer(e.x, e.y); // resets cursor if
            // necessary
            return;
        }
        if (resizingRow)
        {
        	resizingRow = false;
        	handleHoverOnRowResizer(e.x, e.y); // resets cursor if
            // necessary
            return;
        }

        if (pushingColumn)
        {
            pushingColumn = false;
            columnBeingPushed.getHeaderRenderer().setMouseDown(false);
            columnBeingPushed.getHeaderRenderer().setHover(false);
            redraw();
            if (pushingAndHovering)
            {
                columnBeingPushed.fireListeners();
            }
            setCapture(false);
            return;
        }

        if (draggingColumn)
        {
            handleColumnDrop();
            return;
        }

        if (cellDragSelectionOccuring || cellRowDragSelectionOccuring || cellColumnDragSelectionOccuring)
        {
            cellDragSelectionOccuring = false;
            cellRowDragSelectionOccuring = false;
            cellColumnDragSelectionOccuring = false;
            setCursor(null);

            if (followupCellSelectionEventOwed)
            {
            	Event se = new Event();
            	se.button = e.button;
            	se.item = getItem(new Point(e.x, e.y));
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
    	//check to see if the mouse is outside the grid
    	//this should only happen when the mouse is captured for inplace
    	//tooltips - see bug 203364
    	if (inplaceTooltipCapture && (e.x < 0 || e.y < 0 || e.x >= getBounds().width || e.y >= getBounds().height))
    	{
    		setCapture(false);
    		inplaceTooltipCapture = false;
    		return;  //a mouseexit event should occur immediately
    	}


        //if populated will be fired at end of method.
        Event selectionEvent = null;


        if ((e.stateMask & SWT.BUTTON1) == 0)
        {
            handleHovering(e.x, e.y);
        }
        else
        {
            if (draggingColumn)
            {
                handleColumnDragging(e.x);
                return;
            }

            if (resizingColumn)
            {
                handleColumnResizerDragging(e.x);
                return;
            }
            if (resizingRow)
            {
                handleRowResizerDragging(e.y);
                return;
            }
            if (pushingColumn)
            {
                handleColumnHeaderHoverWhilePushing(e.x, e.y);
                return;
            }
            if (cellSelectionEnabled)
            {
                if (!cellDragSelectionOccuring && cellSelectedOnLastMouseDown)
                {
                    cellDragSelectionOccuring = true;
                    //XXX: make this user definable
                    setCursor(getDisplay().getSystemCursor(SWT.CURSOR_CROSS));
                    cellDragCTRL = ((e.stateMask & SWT.MOD1) != 0);
                    if (cellDragCTRL)
                    {
                        selectedCellsBeforeRangeSelect.clear();
                        selectedCellsBeforeRangeSelect.addAll(selectedCells);
                    }
                }
                if (!cellRowDragSelectionOccuring && cellRowSelectedOnLastMouseDown)
                {
                    cellRowDragSelectionOccuring = true;
                    setCursor(getDisplay().getSystemCursor(SWT.CURSOR_CROSS));
                    cellDragCTRL = ((e.stateMask & SWT.MOD1) != 0);
                    if (cellDragCTRL)
                    {
                        selectedCellsBeforeRangeSelect.clear();
                        selectedCellsBeforeRangeSelect.addAll(selectedCells);
                    }
                }

                if (!cellColumnDragSelectionOccuring && cellColumnSelectedOnLastMouseDown)
                {
                    cellColumnDragSelectionOccuring = true;
                    setCursor(getDisplay().getSystemCursor(SWT.CURSOR_CROSS));
                    cellDragCTRL = ((e.stateMask & SWT.MOD1) != 0);
                    if (cellDragCTRL)
                    {
                        selectedCellsBeforeRangeSelect.clear();
                        selectedCellsBeforeRangeSelect.addAll(selectedCells);
                    }
                }

                int ctrlFlag = (cellDragCTRL ? SWT.MOD1 : SWT.NONE);

                if (cellDragSelectionOccuring && handleCellHover(e.x, e.y))
                {
                    GridColumn intentColumn = hoveringColumn;
                    GridItem intentItem = hoveringItem;

                    if (hoveringItem == null)
                    {
                        if (e.y > headerHeight)
                        {
                            //then we must be hovering way to the bottom
                            intentItem = getPreviousVisibleItem(null);
                        }
                        else
                        {
                            intentItem = items.get(0);
                        }
                    }


                    if (hoveringColumn == null)
                    {
                        if (e.x > rowHeaderWidth)
                        {
                            //then we must be hovering way to the right
                            intentColumn = getVisibleColumn_DegradeLeft(intentItem, displayOrderedColumns.get(displayOrderedColumns.size() - 1));
                        }
                        else
                        {
                            GridColumn firstCol = displayOrderedColumns.get(0);
                            if (!firstCol.isVisible())
                            {
                                firstCol = getNextVisibleColumn(firstCol);
                            }
                            intentColumn = firstCol;
                        }
                    }

                    showColumn(intentColumn);
                    showItem(intentItem);
                    selectionEvent = updateCellSelection(new Point(indexOf(intentColumn),indexOf(intentItem)),ctrlFlag | SWT.MOD2, true, false);
                }
                if (cellRowDragSelectionOccuring && handleCellHover(e.x, e.y))
                {
                    GridItem intentItem = hoveringItem;

                    if (hoveringItem == null)
                    {
                        if (e.y > headerHeight)
                        {
                            //then we must be hovering way to the bottom
                            intentItem = getPreviousVisibleItem(null);
                        }
                        else
                        {
                            if (getTopIndex() > 0)
                            {
                                intentItem = getPreviousVisibleItem(items.get(getTopIndex()));
                            }
                            else
                            {
                                intentItem = items.get(0);
                            }
                        }
                    }

                    List<Point> cells = new ArrayList<Point>();

                    getCells(intentItem,focusItem,cells);

                    showItem(intentItem);
                    selectionEvent = updateCellSelection(cells,ctrlFlag, true, false);
                }
                if (cellColumnDragSelectionOccuring && handleCellHover(e.x, e.y))
                {
                    GridColumn intentCol = hoveringColumn;

                    if (intentCol == null)
                    {
                        if (e.y < rowHeaderWidth)
                        {
                            //TODO: get the first col to the left
                        }
                        else
                        {
                            //TODO: get the first col to the right
                        }
                    }

                    if (intentCol == null) return;  //temporary

                    GridColumn iterCol = intentCol;

                    List<Point> newSelected = new ArrayList<Point>();

                    boolean decreasing = (displayOrderedColumns.indexOf(iterCol) > displayOrderedColumns.indexOf(focusColumn));

                    do
                    {
                        getCells(iterCol, newSelected);

                        if (iterCol == focusColumn)
                        {
                            break;
                        }

                        if (decreasing)
                        {
                            iterCol = getPreviousVisibleColumn(iterCol);
                        }
                        else
                        {
                            iterCol = getNextVisibleColumn(iterCol);
                        }

                    } while (true);

                    selectionEvent = updateCellSelection(newSelected, ctrlFlag, true, false);
                }

            }
        }

        if (selectionEvent != null)
        {
            selectionEvent.stateMask = e.stateMask;
            selectionEvent.button = e.button;
            selectionEvent.item = getItem(new Point(e.x, e.y));
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

        // Is this Grid a DragSource ??
        if (cellSelectionEnabled && getData("DragSource") != null) {
            if (handleHoverOnSelectionDragArea(x, y))
            {
                return;
            }
        }

        if (columnHeadersVisible)
        {
            if (handleHoverOnColumnResizer(x, y))
            {
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
                return;
            }
        }
        if (rowsResizeable && rowHeaderVisible)
        {
        	if (handleHoverOnRowResizer(x, y))
        	{
        		return;
        	}
        }

       // handleCellHover(x, y);
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
            hoveringItem = null;
            hoveringDetail = "";
            hoveringColumn = null;
            hoveringOverText = false;
            hideToolTip();
            redraw();
    }

    /**
     * Key down event handler.
     *
     * @param e event
     */
    private void onKeyDown(Event e)
    {
        if (focusColumn == null)
        {
        	if (columns.size() == 0)
        		return;

            focusColumn = getColumn(0);
            intendedFocusColumn = focusColumn;
        }

        if (e.character == '\r' && focusItem != null)
        {
            Event newEvent = new Event();
            newEvent.item = focusItem;

            notifyListeners(SWT.DefaultSelection, newEvent);
            return;
        }

        int attemptExpandCollapse = 0;
        if ((e.character == '-' || (!cellSelectionEnabled && e.keyCode == SWT.ARROW_LEFT)) && focusItem != null && focusItem.isExpanded())
        {
            attemptExpandCollapse = SWT.Collapse;
        }
        else if ((e.character == '+' || (!cellSelectionEnabled && e.keyCode == SWT.ARROW_RIGHT)) && focusItem != null && !focusItem.isExpanded())
        {
            attemptExpandCollapse = SWT.Expand;
        }

        if (attemptExpandCollapse != 0 && focusItem != null && focusItem.hasChildren())
        {
            int performExpandCollapse = 0;

            if (cellSelectionEnabled && focusColumn != null && focusColumn.isTree())
            {
                performExpandCollapse = attemptExpandCollapse;
            }
            else if (!cellSelectionEnabled)
            {
                performExpandCollapse = attemptExpandCollapse;
            }

            if (performExpandCollapse == SWT.Expand)
            {
                focusItem.setExpanded(true);
                focusItem.fireEvent(SWT.Expand);
                return;
            }
            if (performExpandCollapse == SWT.Collapse)
            {
                focusItem.setExpanded(false);
                focusItem.fireEvent(SWT.Collapse);
                return;
            }
        }

        if (e.character == ' ')
        {
            handleSpaceBarDown(e);
        }


        GridItem newSelection = null;
        GridColumn newColumnFocus = null;

        //These two variables are used because the key navigation when the shift key is down is
        //based, not off the focus item/column, but rather off the implied focus (i.e. where the
        //keyboard has extended focus to).
        GridItem impliedFocusItem = focusItem;
        GridColumn impliedFocusColumn = focusColumn;

        if (cellSelectionEnabled && e.stateMask == SWT.MOD2)
        {
            if (shiftSelectionAnchorColumn != null)
            {
                impliedFocusItem = shiftSelectionAnchorItem;
                impliedFocusColumn = shiftSelectionAnchorColumn;
            }
        }

        switch (e.keyCode)
        {
            case SWT.ARROW_RIGHT :
                if (cellSelectionEnabled)
                {
                    if (impliedFocusItem != null && impliedFocusColumn != null)
                    {
                        newSelection = impliedFocusItem;

                        int index = displayOrderedColumns.indexOf(impliedFocusColumn);

                        int jumpAhead = impliedFocusItem.getColumnSpan(indexOf(impliedFocusColumn));

                        jumpAhead ++;

                        while (jumpAhead > 0)
                        {
                            index ++;
                            if (index < displayOrderedColumns.size())
                            {
                                if (displayOrderedColumns.get(index).isVisible())
                                    jumpAhead --;
                            }
                            else
                            {
                                break;
                            }
                        }

                        if (index < displayOrderedColumns.size())
                        {
                            newColumnFocus = displayOrderedColumns.get(index);
                        }
                        else
                        {
                            newColumnFocus = impliedFocusColumn;
                        }
                    }
                    intendedFocusColumn = newColumnFocus;
                }
                else
                {
                    if (impliedFocusItem != null && impliedFocusItem.hasChildren())
                    {
                        newSelection = impliedFocusItem.getItem(0);
                    }
                }
                break;
            case SWT.ARROW_LEFT :
                if (cellSelectionEnabled)
                {
                    if (impliedFocusItem != null && impliedFocusColumn != null)
                    {
                        newSelection = impliedFocusItem;

                        int index = displayOrderedColumns.indexOf(impliedFocusColumn);

                        if (index != 0)
                        {
                            newColumnFocus = displayOrderedColumns.get(index -1);

                            newColumnFocus = getVisibleColumn_DegradeLeft(impliedFocusItem, newColumnFocus);
                        }
                        else
                        {
                            newColumnFocus = impliedFocusColumn;
                        }
                    }
                    intendedFocusColumn = newColumnFocus;
                }
                else
                {
                    if (impliedFocusItem != null && impliedFocusItem.getParentItem() != null)
                    {
                        newSelection = impliedFocusItem.getParentItem();
                    }
                }
                break;
            case SWT.ARROW_UP :
                if (impliedFocusItem != null)
                {
                    newSelection = getPreviousVisibleItem(impliedFocusItem);
                }

                if (impliedFocusColumn != null)
                {
                    if (newSelection != null)
                    {
                        newColumnFocus = getVisibleColumn_DegradeLeft(newSelection, intendedFocusColumn);
                    }
                    else
                    {
                        newColumnFocus = impliedFocusColumn;
                    }
                }

                break;
            case SWT.ARROW_DOWN :
                if (impliedFocusItem != null)
                {
                    newSelection = getNextVisibleItem(impliedFocusItem);
                }
                else
                {
                    if (items.size() > 0)
                    {
                        newSelection = items.get(0);
                    }
                }

                if (impliedFocusColumn != null)
                {
                    if (newSelection != null)
                    {
                        newColumnFocus = getVisibleColumn_DegradeLeft(newSelection, intendedFocusColumn);
                    }
                    else
                    {
                        newColumnFocus = impliedFocusColumn;
                    }
                }
                break;
            case SWT.HOME :

                if (!cellSelectionEnabled)
                {
                    if (items.size() > 0)
                    {
                        newSelection = items.get(0);
                    }
                }
                else
                {
                    newSelection = impliedFocusItem;
                    newColumnFocus = getVisibleColumn_DegradeRight(newSelection, displayOrderedColumns.get(0));
                }

                break;
            case SWT.END :
                if (!cellSelectionEnabled)
                {
                    if (items.size() > 0)
                    {
                        newSelection = getPreviousVisibleItem(null);
                    }
                }
                else
                {
                    newSelection = impliedFocusItem;
                    newColumnFocus = getVisibleColumn_DegradeLeft(newSelection, displayOrderedColumns.get(displayOrderedColumns.size() - 1));
                }

                break;
            case SWT.PAGE_UP :
                int topIndex = getTopIndex();

                newSelection = items.get(topIndex);

                if (focusItem == newSelection)
                {
                    RowRange range = getRowRange(getTopIndex(),getVisibleGridHeight(),false,true);
                    newSelection = items.get(range.startIndex);
                }

                newColumnFocus = focusColumn;
                break;
            case SWT.PAGE_DOWN :
                int bottomIndex = getBottomIndex();

                newSelection = items.get(bottomIndex);

                if(!isShown(newSelection))
                {
                	// the item at bottom index is not shown completely
                	GridItem tmpItem = getPreviousVisibleItem(newSelection);
                	if(tmpItem!=null)
                		newSelection = tmpItem;
                }

                if (focusItem == newSelection)
                {
                    RowRange range = getRowRange(getBottomIndex(),getVisibleGridHeight(),true,false);
                    newSelection = items.get(range.endIndex);
                }

                newColumnFocus = focusColumn;
                break;
            default :
                break;
        }

        if (newSelection == null)
        {
            return;
        }

        if (cellSelectionEnabled)
        {
            if (e.stateMask != SWT.MOD2)
                focusColumn = newColumnFocus;
            showColumn(newColumnFocus);

            if (e.stateMask != SWT.MOD2)
                focusItem = newSelection;
            showItem(newSelection);

            if (e.stateMask != SWT.MOD1)
            {
                Event selEvent = updateCellSelection(new Point(indexOf(newColumnFocus),indexOf(newSelection)),e.stateMask, false, false);
                if (selEvent != null)
                {
                	selEvent.stateMask = e.stateMask;
                	selEvent.character = e.character;
                	selEvent.keyCode = e.keyCode;
                	notifyListeners(SWT.Selection, selEvent);
                }
            }

            redraw();
        }
        else
        {
            Event selectionEvent = null;
            if (selectionType == SWT.SINGLE || e.stateMask != SWT.MOD1)
            {
                selectionEvent = updateSelection(newSelection, e.stateMask);
                if (selectionEvent != null)
                {
                	selectionEvent.stateMask = e.stateMask;
                	selectionEvent.character = e.character;
                	selectionEvent.keyCode = e.keyCode;
                }
            }

            focusItem = newSelection;
            showItem(newSelection);
            redraw();

            if (selectionEvent != null)
                notifyListeners(SWT.Selection, selectionEvent);
        }
    }

    private void handleSpaceBarDown(Event event)
    {
    	if (focusItem == null)
    		return;

        if (selectionEnabled && !cellSelectionEnabled && !selectedItems.contains(focusItem))
        {
            selectedItems.add(focusItem);
            redraw();
            Event e = new Event();
            e.item = focusItem;
            e.stateMask = event.stateMask;
        	e.character = event.character;
        	e.keyCode = event.keyCode;
            notifyListeners(SWT.Selection, e);
        }

        if (!cellSelectionEnabled)
        {
            boolean checkFirstCol = false;
            boolean first = true;

            for (GridColumn col : columns) {

                if (first) {
                    if (!col.isCheck()) break;

                    first = false;
                    checkFirstCol = true;
                } else {
                    if (col.isCheck()) {
                        checkFirstCol = false;
                        break;
                    }
                }
            }

            if (checkFirstCol)
            {
                focusItem.setChecked(!focusItem.getChecked());
                redraw();
                focusItem.fireCheckEvent(0);
            }
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
     * @param item item
     * @return x,y of top left corner of the cell
     */
    Point getOrigin(GridColumn column, GridItem item)
    {
        int x = 0;

        if (rowHeaderVisible)
        {
            x += rowHeaderWidth;
        }

        x -= getHScrollSelectionInPixels();

        for (GridColumn colIter : displayOrderedColumns) {
            if (colIter == column) {
                break;
            }

            if (colIter.isVisible()) {
                x += colIter.getWidth();
            }
        }

        int y = 0;
        if (item != null)
        {
            if (columnHeadersVisible)
            {
                y += headerHeight;
            }

            int currIndex=getTopIndex();
            int itemIndex=items.indexOf(item);

            if (itemIndex == -1)
            {
            	SWT.error(SWT.ERROR_INVALID_ARGUMENT);
            }

            while(currIndex!=itemIndex)
            {
            	if(currIndex<itemIndex)
            	{
            		GridItem currItem = items.get(currIndex);
            		if(currItem.isVisible())
            		{
            			y += currItem.getHeight() + 1;
            		}
            		currIndex++;
            	}
            	else if(currIndex>itemIndex)
            	{
            		currIndex--;
            		GridItem currItem = items.get(currIndex);
            		if(currItem.isVisible())
            		{
            			y -= currItem.getHeight() + 1;
            		}
            	}
            }
        }
        else
        {
            if (column.getColumnGroup() != null)
            {
                y += groupHeaderHeight;
            }
        }

        return new Point(x, y);
    }

    /**
     * Determines (which cell/if a cell) has been clicked (mouse down really)
     * and notifies the appropriate renderer. Returns true when a cell has
     * responded to this event in some way and prevents the event from
     * triggering an action further down the chain (like a selection).
     *
     * @param item item clicked
     * @param x mouse x
     * @param y mouse y
     * @return true if this event has been consumed.
     */
    private boolean handleCellClick(GridItem item, int x, int y)
    {

        // if(!isTree)
        // return false;

        GridColumn col = getColumn(new Point(x, y));
        if (col == null)
        {
            return false;
        }

        col.getCellRenderer().setBounds(item.getBounds(indexOf(col)));
        return col.getCellRenderer().notify(IInternalWidget.LeftMouseButtonDown, new Point(x, y), item);

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

        boolean overText = false;

        final GridColumn col = getColumn(new Point(x, y));
        final GridItem item = getItem(new Point(x, y));

        GridColumnGroup hoverColGroup = null;
        GridColumn hoverColHeader = null;

        if (col != null)
        {
            if (item != null)
            {
            	if( y < getClientArea().height - footerHeight  ) {
                    col.getCellRenderer().setBounds(item.getBounds(columns.indexOf(col)));

                    if (col.getCellRenderer().notify(IInternalWidget.MouseMove, new Point(x, y), item))
                    {
                        detail = col.getCellRenderer().getHoverDetail();
                    }

                    Rectangle textBounds = col.getCellRenderer().getTextBounds(item,false);

                    if (textBounds != null)
                    {
                        Point p = new Point(x - col.getCellRenderer().getBounds().x, y - col.getCellRenderer().getBounds().y);
                        overText = textBounds.contains(p);
                    }
            	}
            }
            else
            {
                if (y < headerHeight)
                {
                    if (columnGroups.length != 0 && y < groupHeaderHeight
                        && col.getColumnGroup() != null)
                    {
                        hoverColGroup = col.getColumnGroup();
                        hoverColGroup.getHeaderRenderer().setBounds(hoverColGroup.getBounds());
                        if (hoverColGroup.getHeaderRenderer()
                            .notify(IInternalWidget.MouseMove, new Point(x, y), hoverColGroup))
                        {
                            detail = hoverColGroup.getHeaderRenderer().getHoverDetail();
                        }

                        Rectangle textBounds = hoverColGroup.getHeaderRenderer().getTextBounds(hoverColGroup,false);

                        if (textBounds != null)
                        {
                            Point p = new Point(x - hoverColGroup.getHeaderRenderer().getBounds().x, y - hoverColGroup.getHeaderRenderer().getBounds().y);
                            overText = textBounds.contains(p);
                        }
                    }
                    else
                    {
                        // on col header
                        hoverColHeader = col;

                        col.getHeaderRenderer().setBounds(col.getBounds());
                        if (col.getHeaderRenderer().notify(IInternalWidget.MouseMove, new Point(x, y),
                                                           col))
                        {
                            detail = col.getHeaderRenderer().getHoverDetail();
                        }

                        Rectangle textBounds = col.getHeaderRenderer().getTextBounds(col,false);

                        if (textBounds != null)
                        {
                            Point p = new Point(x - col.getHeaderRenderer().getBounds().x, y - col.getHeaderRenderer().getBounds().y);
                            overText = textBounds.contains(p);
                        }
                    }
                }
            }
        }

        boolean hoverChange = false;

        if (hoveringItem != item || !hoveringDetail.equals(detail) || hoveringColumn != col
            || hoverColGroup != hoverColumnGroupHeader || hoverColHeader != hoveringColumnHeader)
        {
            hoveringItem = item;
            hoveringDetail = detail;
            hoveringColumn = col;
            hoveringColumnHeader = hoverColHeader;
            hoverColumnGroupHeader = hoverColGroup;

            Rectangle clientArea = getClientArea();
            redraw(clientArea.x,clientArea.y,clientArea.width,clientArea.height,false);

            hoverChange = true;
        }

        //do inplace toolTip stuff
        if (hoverChange || hoveringOverText != overText)
        {
            hoveringOverText = overText;

            if (overText){

                Rectangle cellBounds = null;
                Rectangle textBounds = null;
                Rectangle preferredTextBounds = null;

                if (hoveringItem != null && hoveringItem.getToolTipText(indexOf(col)) == null && //no inplace tooltips when regular tooltip
                		!col.getWordWrap()) //dont show inplace tooltips for cells with wordwrap
                {
	                cellBounds = col.getCellRenderer().getBounds();
	                if (cellBounds.x + cellBounds.width > getSize().x)
	                {
	                    cellBounds.width = getSize().x - cellBounds.x;
	                }
	                textBounds = col.getCellRenderer().getTextBounds(item,false);
	                preferredTextBounds = col.getCellRenderer().getTextBounds(item,true);
                }
                else if (hoveringColumnHeader != null && hoveringColumnHeader.getHeaderTooltip() == null) //no inplace tooltips when regular tooltip
                {
                    cellBounds = hoveringColumnHeader.getHeaderRenderer().getBounds();
                    if (cellBounds.x + cellBounds.width > getSize().x)
                    {
                        cellBounds.width = getSize().x - cellBounds.x;
                    }
                    textBounds = hoveringColumnHeader.getHeaderRenderer().getTextBounds(col,false);
                    preferredTextBounds = hoveringColumnHeader.getHeaderRenderer().getTextBounds(col,true);
                }
                else if (hoverColumnGroupHeader != null)
                {
                    cellBounds = hoverColumnGroupHeader.getHeaderRenderer().getBounds();
                    if (cellBounds.x + cellBounds.width > getSize().x)
                    {
                        cellBounds.width = getSize().x - cellBounds.x;
                    }
                    textBounds = hoverColumnGroupHeader.getHeaderRenderer().getTextBounds(hoverColumnGroupHeader,false);
                    preferredTextBounds = hoverColumnGroupHeader.getHeaderRenderer().getTextBounds(hoverColumnGroupHeader,true);
                }

                //if we are truncated
                if (textBounds != null && textBounds.width < preferredTextBounds.width)
                {
                    showToolTip(item,col, hoverColumnGroupHeader, new Point(cellBounds.x + textBounds.x,cellBounds.y +
                                                    textBounds.y));
                    //the following 2 lines are done here rather than in showToolTip to allow
                    //that method to be overridden yet still capture the mouse.
                    setCapture(true);
                    inplaceTooltipCapture = true;
                }
            }
            else
            {
                hideToolTip();
            }
        }

        //do normal cell specific tooltip stuff
        if (hoverChange)
        {
        	String newTip = null;
			if ((hoveringItem != null) && (hoveringColumn != null)) {
				// get cell specific tooltip
				newTip = hoveringItem.getToolTipText(indexOf(hoveringColumn));
			} else if ((hoveringColumn != null) && (hoveringColumnHeader != null)) {
				// get column header specific tooltip
				newTip = hoveringColumn.getHeaderTooltip();
			}

			if (newTip == null) { // no cell or column header specific tooltip then use base Grid tooltip
				newTip = getToolTipText();
			}

        	//Avoid unnecessarily resetting tooltip - this will cause the tooltip to jump around
        	if (newTip != null && !newTip.equals(displayedToolTipText))
        	{
        		updateToolTipText(newTip);
        	}
        	else if(newTip == null && displayedToolTipText != null)
        	{
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
    protected void updateToolTipText(String text)
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
     * @param index index to insert new column
     * @return current number of columns
     */
    int newColumn(GridColumn column, int index)
    {

        if (index == -1)
        {
            columns.add(column);
            displayOrderedColumns.add(column);
        }
        else
        {
            columns.add(index, column);
            displayOrderedColumns.add(index, column);

            for (int i = 0; i < columns.size(); i++) {
				columns.get(i).setColumnIndex(i);
			}
        }

        computeHeaderHeight(sizingGC);
        computeFooterHeight(sizingGC);

        updatePrimaryCheckColumn();

        for (GridItem item : items) {
            item.columnAdded(index);
        }

        scrollValuesObsolete = true;
        redraw();

        return columns.size() - 1;
    }

    /**
     * Removes the given column from the table.
     *
     * @param column column to remove
     */
    void removeColumn(GridColumn column)
    {
        boolean selectionModified = false;

        int index = indexOf(column);

        if (cellSelectionEnabled)
        {
            List<Point> removeSelectedCells = new ArrayList<Point>();

            for (Point cell : selectedCells) {
                if (cell.x == index) {
                    removeSelectedCells.add(cell);
                }
            }

            if (removeSelectedCells.size() > 0)
            {
                selectedCells.removeAll(removeSelectedCells);
                selectionModified = true;
            }

            for (Point cell : selectedCells) {
                if (cell.x >= index) {
                    cell.x--;
                    selectionModified = true;
                }
            }
        }

        columns.remove(column);
        displayOrderedColumns.remove(column);

        // Check focus column
        if (column == focusColumn) {
            focusColumn = null;
        }

        updatePrimaryCheckColumn();

        scrollValuesObsolete = true;
        redraw();

        for (GridItem item : items) {
            item.columnRemoved(index);
        }

        int i = 0;
        for (GridColumn col : columns) {
            col.setColumnIndex(i);
            i++;
        }

        if (selectionModified && !disposing)
        {
            updateColumnSelection();
        }

    }

    /**
     * Manages the setting of the checkbox column when the SWT.CHECK style was given to the
     * table.  This method will ensure that the first column of the table always has a checkbox
     * when SWT.CHECK is given to the table.
     */
    private void updatePrimaryCheckColumn()
    {
        if ((getStyle() & SWT.CHECK) == SWT.CHECK)
        {
            boolean firstCol = true;

            for (GridColumn col : columns) {
                col.setTableCheck(firstCol);
                firstCol = false;
            }
        }
    }

    void newRootItem(GridItem item, int index)
    {
        if (index == -1 || index >= rootItems.size())
        {
            rootItems.add(item);
        }
        else
        {
            rootItems.add(index,item);
        }
    }

    void removeRootItem(GridItem item)
    {
        rootItems.remove(item);
    }

    /**
     * Creates the new item at the given index. Only called from GridItem
     * constructor.
     *
     * @param item new item
     * @param index index to insert the item at
     * @return the index where the item was insert
     */
    int newItem(GridItem item, int index, boolean root)
    {
        int row;

        if (!isTree)
        {
            if (item.getParentItem() != null)
            {
                isTree = true;
            }
        }

        //Have to convert indexes, this method needs a flat index, the method is called with indexes
        //that are relative to the level
        if (root && index != -1)
        {
            if (index >= rootItems.size())
            {
                index = -1;
            }
            else
            {
                index = items.indexOf(rootItems.get(index));
            }
        }
        else if (!root)
        {
            if (index >= item.getParentItem().getItems().length || index == -1)
            {
              GridItem rightMostDescendent = item.getParentItem();

                while (rightMostDescendent.getItems().length > 0)
                {
                    rightMostDescendent = rightMostDescendent.getItems()[rightMostDescendent
                        .getItems().length - 1];
                }

                index = indexOf(rightMostDescendent) + 1;
            }
            else
            {
                index = indexOf(item.getParentItem().getItems()[index]);
            }
        }

        if (index == -1)
        {
            items.add(item);
            row = items.size() - 1;
        }
        else
        {
            items.add(index, item);
            row = index;
        }

        if (items.size() == 1 && !userModifiedItemHeight)
            itemHeight = computeItemHeight(item,sizingGC);

        item.initializeHeight(itemHeight);

        if (isRowHeaderVisible() && isAutoWidth())
        {
	        rowHeaderWidth = Math.max(rowHeaderWidth,rowHeaderRenderer
	            .computeSize(sizingGC, SWT.DEFAULT, SWT.DEFAULT, item).x);
        }

        scrollValuesObsolete = true;
        topIndex = -1;
        bottomIndex = -1;

        currentVisibleItems++;

        redraw();

        return row;
    }

    /**
     * Removes the given item from the table. This method is only called from
     * the item's dispose method.
     *
     * @param item item to remove
     */
    void removeItem(GridItem item)
    {
        Point[] cells = getCells(item);
        boolean selectionModified = false;

        items.remove(item);

        if (disposing)
            return;

        if (selectedItems.remove(item))
            selectionModified = true;

        for (Point cell : cells) {
            if (selectedCells.remove(cell))
                selectionModified = true;
        }

        if (focusItem == item)
        {
            focusItem = null;
        }

        scrollValuesObsolete = true;
        topIndex = -1;
        bottomIndex = -1;
        if (item.isVisible())
        {
            currentVisibleItems--;
        }

        if (selectionModified && !disposing)
        {
            updateColumnSelection();
        }

        redraw();
    }

    /**
     * Creates the given column group at the given index. This method is only
     * called from the {@code GridColumnGroup}'s constructor.
     *
     * @param group group to add.
     */
    void newColumnGroup(GridColumnGroup group)
    {
        GridColumnGroup[] newColumnGroups = new GridColumnGroup[columnGroups.length + 1];
        System.arraycopy(columnGroups, 0, newColumnGroups, 0, columnGroups.length);
        newColumnGroups[newColumnGroups.length - 1] = group;
        columnGroups = newColumnGroups;

        // if we just added the first col group, then we need to up the row
        // height
        if (columnGroups.length == 1)
        {
            computeHeaderHeight(sizingGC);
        }

        scrollValuesObsolete = true;
        redraw();
    }

    /**
     * Removes the given column group from the table. This method is only called
     * from the {@code GridColumnGroup}'s dispose method.
     *
     * @param group group to remove.
     */
    void removeColumnGroup(GridColumnGroup group)
    {
        GridColumnGroup[] newColumnGroups = new GridColumnGroup[columnGroups.length - 1];
        int newIndex = 0;
        for (int i = 0; i < columnGroups.length; i++)
        {
            if (columnGroups[i] != group)
            {
                newColumnGroups[newIndex] = columnGroups[i];
                newIndex++;
            }
        }
        columnGroups = newColumnGroups;

        if (columnGroups.length == 0)
        {
            computeHeaderHeight(sizingGC);
        }

        scrollValuesObsolete = true;
        redraw();
    }

    /**
     * Updates the cached number of visible items by the given amount.
     *
     * @param amount amount to update cached total
     */
    void updateVisibleItems(int amount)
    {
        currentVisibleItems += amount;
    }

    /**
     * Returns the current item in focus.
     *
     * @return item in focus or {@code null}.
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public GridItem getFocusItem()
    {
        checkWidget();
        return focusItem;
    }

    /**
     * Returns the current cell in focus.  If cell selection is disabled, this method returns null.
     *
     * @return cell in focus or {@code null}. x represents the column and y the row the cell is in
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public Point getFocusCell()
    {
        checkWidget();
        if (!cellSelectionEnabled) return null;

        int x = -1;
        int y = -1;

        if (focusColumn != null)
            x = indexOf(focusColumn);

        if (focusItem != null)
            y = indexOf(focusItem);

        return new Point(x,y);
    }

    /**
     * Sets the focused item to the given item.
     *
     * @param item item to focus.
     * @throws IllegalArgumentException
     * <ul>
     * <li>ERROR_INVALID_ARGUMENT - if item is disposed</li>
     * </ul>
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void setFocusItem(GridItem item)
    {
        checkWidget();
        //TODO: check and make sure this item is valid for focus
        if (item == null || item.isDisposed() || item.getParent() != this)
        {
            SWT.error(SWT.ERROR_INVALID_ARGUMENT);
        }
        focusItem = item;
    }

    /**
     * Sets the focused item to the given column. Column focus is only applicable when cell
     * selection is enabled.
     *
     * @param column column to focus.
     * @throws IllegalArgumentException
     * <ul>
     * <li>ERROR_INVALID_ARGUMENT - if item is disposed</li>
     * </ul>
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void setFocusColumn(GridColumn column)
    {
        checkWidget();
        //TODO: check and make sure this item is valid for focus
        if (column == null || column.isDisposed() || column.getParent() != this || !column.isVisible())
        {
            SWT.error(SWT.ERROR_INVALID_ARGUMENT);
        }

        focusColumn = column;
        intendedFocusColumn = column;
    }


    /**
     * Returns an array of the columns in their display order.
     *
     * @return columns in display order
     */
    GridColumn[] getColumnsInOrder()
    {
        checkWidget();
        return displayOrderedColumns.toArray(new GridColumn[columns.size()]);
    }

    /**
     * Returns true if the table is set to horizontally scroll column-by-column
     * rather than pixel-by-pixel.
     *
     * @return true if the table is scrolled horizontally by column
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
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
     * scroll by pixel
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void setColumnScrolling(boolean columnScrolling)
    {
    	checkWidget();
        if (rowHeaderVisible && !columnScrolling)
        {
            return;
        }

        this.columnScrolling = columnScrolling;
        scrollValuesObsolete = true;
        redraw();
    }

    /**
     * Returns the first visible column that is not spanned by any other column that is either the
     * given column or any of the columns displaying to the left of the given column.  If the
     * given column and subsequent columns to the right are either not visible or spanned, this
     * method will return null.
     *
     * @param item
     * @param col
     * @return
     */
    private GridColumn getVisibleColumn_DegradeLeft(GridItem item, GridColumn col)
    {
        int index = displayOrderedColumns.indexOf(col);

        GridColumn prevCol = col;

        int i = 0;
        while (!prevCol.isVisible())
        {
            i ++;
            if (index - i < 0)
                return null;

            prevCol = displayOrderedColumns.get(index - i);
        }

        index = displayOrderedColumns.indexOf(prevCol);

        for (int j = 0; j < index; j++)
        {
            GridColumn tempCol = displayOrderedColumns.get(j);

            if (!tempCol.isVisible())
            {
                continue;
            }

            if (item.getColumnSpan(indexOf(tempCol)) >= index - j)
            {
                prevCol = tempCol;
                break;
            }
        }

        return prevCol;
    }

    /**
     * Returns the first visible column that is not spanned by any other column that is either the
     * given column or any of the columns displaying to the right of the given column.  If the
     * given column and subsequent columns to the right are either not visible or spanned, this
     * method will return null.
     *
     * @param item
     * @param col
     * @return
     */
    private GridColumn getVisibleColumn_DegradeRight(GridItem item, GridColumn col)
    {
        int index = displayOrderedColumns.indexOf(col);

        int i = 0;
        GridColumn nextCol = col;
        while (!nextCol.isVisible())
        {
            i ++;
            if (index + i == displayOrderedColumns.size())
                return null;

            nextCol = displayOrderedColumns.get(index + i);
        }


        index = displayOrderedColumns.indexOf(nextCol);
        int startIndex = index;

        while (index > 0)
        {

            index --;
            GridColumn prevCol = displayOrderedColumns.get(index);

            if (item.getColumnSpan(indexOf(prevCol)) >= startIndex - index)
            {
                if (startIndex == displayOrderedColumns.size() - 1)
                {
                    return null;
                }
                else
                {
                    return getVisibleColumn_DegradeRight(item, displayOrderedColumns.get(startIndex + 1));
                }
            }

        }

        return nextCol;
    }

    /**
     * Returns true if the cells are selectable in the reciever.
     *
     * @return cell selection enablement status.
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public boolean getCellSelectionEnabled()
    {
        checkWidget();
        return cellSelectionEnabled;
    }

    /**
     * Sets whether cells are selectable in the receiver.
     *
     * @param cellSelection the cellSelection to set
     *
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void setCellSelectionEnabled(boolean cellSelection)
    {
        checkWidget();
        if (!cellSelection)
        {
            selectedCells.clear();
            redraw();
        }
        else
        {
            selectedItems.clear();
            redraw();
        }

        this.cellSelectionEnabled = cellSelection;
    }

    /**
     * @return <code>true</code> if cell selection is enabled
     */
    public boolean isCellSelectionEnabled() {
    	return cellSelectionEnabled;
    }

    /**
     * Deselects the given cell in the receiver.  If the given cell is already
     * deselected it remains deselected.  Invalid cells are ignored.
     *
     * @param cell cell to deselect.
     * @throws IllegalArgumentException
     * <ul>
     * <li>ERROR_NULL_ARGUMENT - if the cell is null</li>
     * </ul>
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void deselectCell(Point cell)
    {
        checkWidget();

        if (cell == null)
            SWT.error(SWT.ERROR_NULL_ARGUMENT);

        selectedCells.remove(cell);
        updateColumnSelection();
        redraw();
    }

    /**
     * Deselects the given cells.  Invalid cells are ignored.
     *
     * @param cells the cells to deselect.
     *
     * @throws IllegalArgumentException
     * <ul>
     * <li>ERROR_NULL_ARGUMENT - if the set of cells or any cell is null</li>
     * </ul>
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void deselectCells(Point[] cells)
    {
        checkWidget();

        if (cells == null)
            SWT.error(SWT.ERROR_NULL_ARGUMENT);

        for (Point cell : cells) {
            if (cell == null)
                SWT.error(SWT.ERROR_NULL_ARGUMENT);
        }

        for (Point cell : cells) {
            selectedCells.remove(cell);
        }

        updateColumnSelection();

        redraw();
    }

    /**
     * Deselects all selected cells in the receiver.
     *
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void deselectAllCells()
    {
        checkWidget();
        selectedCells.clear();
        updateColumnSelection();
        redraw();
    }

    /**
     * Selects the given cell.  Invalid cells are ignored.
     *
     * @param cell point whose x values is a column index and y value is an item index
     * @throws IllegalArgumentException
     * <ul>
     * <li>ERROR_NULL_ARGUMENT - if the item is null</li>
     * </ul>
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void selectCell(Point cell)
    {
        checkWidget();

        if (!cellSelectionEnabled) return;

        if (cell == null)
            SWT.error(SWT.ERROR_NULL_ARGUMENT);

        addToCellSelection(cell);
        updateColumnSelection();
        redraw();
    }

    /**
     * Selects the given cells.  Invalid cells are ignored.
     *
     * @param cells an arry of points whose x value is a column index and y value is an item index
     * @throws IllegalArgumentException
     * <ul>
     * <li>ERROR_NULL_ARGUMENT - if the set of cells or an individual cell is null</li>
     * </ul>
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void selectCells(Point[] cells)
    {
        checkWidget();

        if (!cellSelectionEnabled) return;

        if (cells == null)
            SWT.error(SWT.ERROR_NULL_ARGUMENT);

        for (Point cell : cells) {
            if (cell == null)
                SWT.error(SWT.ERROR_NULL_ARGUMENT);
        }

        for (Point cell : cells) {
            addToCellSelection(cell);
        }

        updateColumnSelection();
        redraw();
    }

    /**
     * Selects all cells in the receiver.
     *
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void selectAllCells()
    {
        checkWidget();
        selectAllCellsInternal();
    }

    /**
     * Selects all cells in the receiver.
     *
     * @return An Event object
     *
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    private Event selectAllCellsInternal()
    {
        if (!cellSelectionEnabled) return null;

        if (columns.size() == 0)
            return null;

        if(items.size() == 0)
        	return null;

        int index = 0;
        GridColumn column = displayOrderedColumns.get(index);

        while (!column.isVisible())
        {
            index ++;

            if (index >= columns.size())
                return null;

            column = displayOrderedColumns.get(index);
        }

        GridColumn oldFocusColumn = focusColumn;
        GridItem oldFocusItem = focusItem;

        focusColumn = column;
        focusItem = items.get(0);

        GridItem lastItem = getPreviousVisibleItem(null);
        GridColumn lastCol = getVisibleColumn_DegradeLeft(lastItem, displayOrderedColumns.get(displayOrderedColumns.size() -1));

        Event event = updateCellSelection(new Point(indexOf(lastCol),indexOf(lastItem)),SWT.MOD2, true, false);

        focusColumn = oldFocusColumn;
        focusItem = oldFocusItem;

        updateColumnSelection();

        redraw();
        return event;
    }

    /**
     * Selects all cells in the given column in the receiver.
     *
     * @param col
     *
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void selectColumn(int col) {
        checkWidget();
    	List<Point> cells = new ArrayList<Point>();
		getCells(getColumn(col), cells);
		selectCells(cells.toArray(new Point[cells.size()]));
    }

    /**
     * Selects all cells in the given column group in the receiver.
     *
     * @param colGroup the column group
     *
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void selectColumnGroup(int colGroup) {
        selectColumnGroup(getColumnGroup(colGroup));
    }

    /**
     * Selects all cells in the given column group in the receiver.
     *
     * @param colGroup the column group
     *
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void selectColumnGroup(GridColumnGroup colGroup) {
        checkWidget();
    	List<Point> cells = new ArrayList<Point>();
		getCells(colGroup, cells);
		selectCells(cells.toArray(new Point[cells.size()]));
    }

    /**
     * Selects the selection to the given cell.  The existing selection is cleared before
     * selecting the given cell.
     *
     * @param cell point whose x values is a column index and y value is an item index
     * @throws IllegalArgumentException
     * <ul>
     * <li>ERROR_NULL_ARGUMENT - if the item is null</li>
     * <li>ERROR_INVALID_ARGUMENT - if the cell is invalid</li>
     * </ul>
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void setCellSelection(Point cell)
    {
        checkWidget();

        if (!cellSelectionEnabled) return;

        if (cell == null)
            SWT.error(SWT.ERROR_NULL_ARGUMENT);

        if (!isValidCell(cell))
            SWT.error(SWT.ERROR_INVALID_ARGUMENT);

        selectedCells.clear();
        addToCellSelection(cell);
        updateColumnSelection();
        redraw();
    }

    /**
     * Selects the selection to the given set of cell.  The existing selection is cleared before
     * selecting the given cells.
     *
     * @param cells point array whose x values is a column index and y value is an item index
     * @throws IllegalArgumentException
     * <ul>
     * <li>ERROR_NULL_ARGUMENT - if the cell array or an individual cell is null</li>
     * <li>ERROR_INVALID_ARGUMENT - if the a cell is invalid</li>
     * </ul>
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void setCellSelection(Point[] cells)
    {
        checkWidget();

        if (!cellSelectionEnabled) return;

        if (cells == null)
            SWT.error(SWT.ERROR_NULL_ARGUMENT);

        for (Point cell : cells) {
            if (cell == null)
                SWT.error(SWT.ERROR_NULL_ARGUMENT);

            if (!isValidCell(cell))
                SWT.error(SWT.ERROR_INVALID_ARGUMENT);
        }

        selectedCells.clear();
        for (Point cell : cells) {
            addToCellSelection(cell);
        }

        updateColumnSelection();
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
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public Point[] getCellSelection()
    {
        checkWidget();
        return selectedCells.toArray(new Point[selectedCells.size()]);
    }




    GridColumn getFocusColumn()
    {
        return focusColumn;
    }

    void updateColumnFocus()
    {
        if (!focusColumn.isVisible())
        {
            int index = displayOrderedColumns.indexOf(focusColumn);
            if (index > 0)
            {
                GridColumn prev = displayOrderedColumns.get(index - 1);
                prev = getVisibleColumn_DegradeLeft(focusItem,prev);
                if (prev == null)
                {
                    prev = getVisibleColumn_DegradeRight(focusItem,focusColumn);
                }
                focusColumn = prev;
            }
            else
            {
                focusColumn = getVisibleColumn_DegradeRight(focusItem,focusColumn);
            }
        }
    }

    private void getCells(GridColumn col, List<Point> cells)
    {
        int colIndex = indexOf(col);

        int columnAtPosition = 0;
        for (GridColumn nextCol : displayOrderedColumns) {
            if (!nextCol.isVisible()) continue;

            if (nextCol == col) break;

            columnAtPosition++;
        }


        GridItem item = null;
        if (getItemCount() > 0)
            item = getItem(0);

        while (item != null)
        {
            //is cell spanned
            int position = -1;
            boolean spanned = false;
            for (GridColumn nextCol : displayOrderedColumns) {
                if (!nextCol.isVisible()) continue;

                if (nextCol == col) break;

                int span = item.getColumnSpan(indexOf(nextCol));

                if (position + span >= columnAtPosition) {
                    spanned = true;
                    break;
                }
            }

            if (!spanned && item.getColumnSpan(colIndex) == 0)
            {
                cells.add(new Point(colIndex,indexOf(item)));
            }

            item = getNextVisibleItem(item);
        }
    }

    private void getCells(GridColumnGroup colGroup, List<Point> cells)
    {
        GridColumn[] cols = colGroup.getColumns();
        for (GridColumn col : cols) {
            getCells(col, cells);
        }
    }

    private void getCells(GridItem item, List<Point> cells)
    {
        int itemIndex = indexOf(item);

        int span = 0;

        for (GridColumn nextCol : displayOrderedColumns) {
            if (span > 0) {
                span--;
                continue;
            }

            if (!nextCol.isVisible()) continue;

            span = item.getColumnSpan(indexOf(nextCol));

            cells.add(new Point(indexOf(nextCol), itemIndex));
        }
    }

    private Point[] getCells(GridItem item)
    {
        List<Point> cells = new ArrayList<Point>();

        int itemIndex = indexOf(item);

        int span = 0;

        for (GridColumn nextCol : displayOrderedColumns) {
            if (span > 0) {
                span--;
                continue;
            }

            if (!nextCol.isVisible()) continue;

            span = item.getColumnSpan(indexOf(nextCol));

            cells.add(new Point(indexOf(nextCol), itemIndex));
        }
        return cells.toArray(new Point[cells.size()]);
    }


    private void getCells(GridItem fromItem, GridItem toItem, List<Point> cells)
    {
        boolean descending = (indexOf(fromItem) < indexOf(toItem));

        GridItem iterItem = toItem;

        do
        {
            getCells(iterItem,cells);

            if (iterItem == fromItem) break;

            if (descending)
            {
                iterItem = getPreviousVisibleItem(iterItem);
            }
            else
            {
                iterItem = getNextVisibleItem(iterItem);
            }
        } while (true);
    }

    private int blend(int v1, int v2, int ratio) {
        return (ratio*v1 + (100-ratio)*v2)/100;
    }

    private RGB blend(RGB c1, RGB c2, int ratio) {
        int r = blend(c1.red, c2.red, ratio);
        int g = blend(c1.green, c2.green, ratio);
        int b = blend(c1.blue, c2.blue, ratio);
        return new RGB(r, g, b);
    }

    /**
     * Returns a point whose x and y values are the to and from column indexes of the new selection
     * range inclusive of all spanned columns.
     *
     * @param fromItem
     * @param fromColumn
     * @param toItem
     * @param toColumn
     * @return
     */
    private Point getSelectionRange(GridItem fromItem, GridColumn fromColumn, GridItem toItem, GridColumn toColumn)
    {
        if (displayOrderedColumns.indexOf(fromColumn) > displayOrderedColumns.indexOf(toColumn))
        {
            GridColumn temp = fromColumn;
            fromColumn = toColumn;
            toColumn = temp;
        }

        if (indexOf(fromItem) > indexOf(toItem))
        {
            GridItem temp = fromItem;
            fromItem = toItem;
            toItem = temp;
        }

        boolean firstTime = true;
        GridItem iterItem = fromItem;

        int fromIndex = indexOf(fromColumn);
        int toIndex = indexOf(toColumn);

        do
        {
            if (!firstTime)
            {
                iterItem = getNextVisibleItem(iterItem);
            }
            else
            {
                firstTime = false;
            }

            Point cols = getRowSelectionRange(iterItem, fromColumn, toColumn);

            //check and see if column spanning means that the range increased
            if (cols.x != fromIndex || cols.y != toIndex)
            {
                GridColumn newFrom = getColumn(cols.x);
                GridColumn newTo = getColumn(cols.y);

                //Unfortunately we have to start all over again from the top with the new range
                return getSelectionRange(fromItem, newFrom, toItem, newTo);
            }
        } while (iterItem != toItem);

        return new Point(indexOf(fromColumn),indexOf(toColumn));
    }

    /**
     * Returns a point whose x and y value are the to and from column indexes of the new selection
     * range inclusive of all spanned columns.
     *
     * @param item
     * @param fromColumn
     * @param toColumn
     * @return
     */
    private Point getRowSelectionRange(GridItem item, GridColumn fromColumn, GridColumn toColumn)
    {
        int newFrom = indexOf(fromColumn);
        int newTo = indexOf(toColumn);

        int span = 0;
        int spanningColIndex = -1;
        boolean spanningBeyondToCol = false;

        for (GridColumn col : displayOrderedColumns) {
            if (!col.isVisible()) {
                if (span > 0) span--;
                continue;
            }

            if (span > 0) {
                if (col == fromColumn) {
                    newFrom = spanningColIndex;
                } else if (col == toColumn && span > 1) {
                    spanningBeyondToCol = true;
                }

                span--;

                if (spanningBeyondToCol && span == 0) {
                    newTo = indexOf(col);
                    break;
                }
            } else {
                int index = indexOf(col);
                span = item.getColumnSpan(index);
                if (span > 0) spanningColIndex = index;

                if (col == toColumn && span > 0)
                    spanningBeyondToCol = true;
            }


            if (col == toColumn && !spanningBeyondToCol)
                break;

        }

        return new Point(newFrom,newTo);
    }

    /**
     * Returns the column which is spanning the given column for the given item or null if it is not
     * being spanned.
     *
     * @param item
     * @param column
     * @return
     */
    private GridColumn getSpanningColumn(GridItem item, GridColumn column)
    {
        int span = 0;
        GridColumn spanningCol = null;

        for (GridColumn col : displayOrderedColumns) {
            if (col == column) {
                return spanningCol;
            }

            if (span > 0) {
                span--;
                if (span == 0) spanningCol = null;
            } else {
                int index = indexOf(col);
                span = item.getColumnSpan(index);

                if (span > 0) spanningCol = col;
            }
        }
        return spanningCol;
    }

    /**
     * Returns true if the given cell's x and y values are valid column and
     * item indexes respectively.
     *
     * @param cell
     * @return
     */
    private boolean isValidCell(Point cell)
    {
        if (cell.x < 0 || cell.x >= columns.size())
            return false;

        if (cell.y < 0 || cell.y >= items.size())
            return false;

        return true;
    }

    /**
     * Shows the inplace tooltip for the given item and column.  The location is the x and y origin
     * of the text in the cell.
     * <p>
     * This method may be overriden to provide their own custom tooltips.
     *
     * @param item the item currently hovered over or null.
     * @param column the column currently hovered over or null.
     * @param group the group currently hovered over or null.
     * @param location the x,y origin of the text in the hovered object.
     */
    protected void showToolTip(GridItem item, GridColumn column, GridColumnGroup group, Point location)
    {
        if (inplaceToolTip == null)
        {
            inplaceToolTip = new GridToolTip(this);
        }

        if (group != null)
        {
            inplaceToolTip.setFont(getFont());
            inplaceToolTip.setText(group.getText());
        }
        else if (item != null)
        {
            inplaceToolTip.setFont(item.getFont(item.getParent().indexOf(column)));
            inplaceToolTip.setText(item.getText(item.getParent().indexOf(column)));
        }
        else if (column != null)
        {
            inplaceToolTip.setFont(getFont());
            inplaceToolTip.setText(column.getText());
        }



        Point p = getDisplay().map(this, null, location);

        inplaceToolTip.setLocation(p);

        inplaceToolTip.setVisible(true);
    }

    /**
     * Hides the inplace tooltip.
     * <p>
     * This method must be overriden when showToolTip is overriden.  Subclasses must
     * call super when overriding this method.
     */
    protected void hideToolTip()
    {
        if (inplaceToolTip != null)
        {
            inplaceToolTip.setVisible(false);
        }
        if (inplaceTooltipCapture)
    	{
        	setCapture(false);
        	inplaceTooltipCapture = false;
    	}
    }

    void recalculateRowHeaderHeight(GridItem item,int oldHeight, int newHeight) {
    	checkWidget();

    	if( newHeight > itemHeight ) {
    		itemHeight = newHeight;

    		userModifiedItemHeight = false;
        	hasDifferingHeights=false;

        	itemHeight = computeItemHeight(items.get(0), sizingGC);

        	for(int cnt=0;cnt<items.size();cnt++)
                items.get(cnt).setHeight(itemHeight);

        	setScrollValuesObsolete();
            redraw();
    	}

    }


    void recalculateRowHeaderWidth(GridItem item,int oldWidth, int newWidth)
    {
        if (!isAutoWidth())
          return;

        if (newWidth > rowHeaderWidth)
        {
            rowHeaderWidth = newWidth;
        }
        else if (newWidth < rowHeaderWidth && oldWidth == rowHeaderWidth)
        {
            //if the changed width is smaller, and the previous width of that rows header was equal
            //to the current row header width then its possible that we may need to make the new
            //row header width smaller, but to do that we need to ask all the rows all over again
            for (GridItem iterItem : items) {
                newWidth = Math.max(newWidth,
                                    rowHeaderRenderer.computeSize(sizingGC, SWT.DEFAULT, SWT.DEFAULT, iterItem).x);
            }

            rowHeaderWidth = newWidth;
        }
        redraw();
    }

    /**
     * {@inheritDoc}
     */
    public void setFont(Font font)
    {
        super.setFont(font);
        sizingGC.setFont(font);
    }

    /**
     * Returns the row header width or 0 if row headers are not visible.
     *
     * @return the width of the row headers
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
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
     * @param width the width of the row header
     * @see #getItemHeaderWidth()
     * @see #setAutoWidth(boolean)
     */
    public void setItemHeaderWidth(int width)
    {
      checkWidget();
      rowHeaderWidth = width;
      setAutoWidth(false);
      redraw();
    }

    /**
     * Sets the number of items contained in the receiver.
     *
     * @param count the number of items
     *
     * @exception org.eclipse.swt.SWTException
     * <ul>
     *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
     * </ul>
     */
    public void setItemCount(int count)
    {
        checkWidget();
        setRedraw(false);
        if (count < 0)
            count = 0;


        if (count < items.size())
        {
          //TODO delete and clear items if necessary
        }

        while (count > items.size())
        {
            new GridItem(this,SWT.NONE);
        }
        setRedraw(true);
    }

    /**
     * Initialize accessibility.
     */
    private void initAccessible()
    {
        final Accessible accessible = getAccessible();
        accessible.addAccessibleListener(new AccessibleAdapter()
        {
            public void getDescription(AccessibleEvent e)
            {
                int childID = e.childID;
                if (childID >= 0 && childID < items.size())
                {
                    String descrption = "";
                    for (int i = 0; i < columns.size(); i++)
                    {
                        if (i != 0)
                        {
                            descrption += columns.get(i).getText() + " : ";
                            descrption += items.get(childID).getText(i) + " ";
                        }
                    }
                    e.result = descrption;
                }
            }

            public void getName(AccessibleEvent e)
            {
                int childID = e.childID;
                if (childID >= 0 && childID < items.size())
                {
                    // Name of the items
                    e.result = items.get(childID).getText();
                }
                else if (childID >= items.size() && childID < items.size() + columns.size())
                {
                    // Name of the column headers
                    e.result = columns.get(childID - items.size()).getText();
                }
                else if (childID >= items.size() + columns.size()
                         && childID < items.size() + columns.size() + columnGroups.length)
                {
                    // Name of the column group headers
                    e.result = columnGroups[childID - items.size() - columns.size()].getText();
                }
                else if (childID >= items.size() + columns.size() + columnGroups.length
                         && childID < items.size() + columns.size() + columnGroups.length
                                      + columnGroups.length)
                {
                    // Name of the toggle button for column group headers
                    e.result = ACC_TOGGLE_BUTTON_NAME;
                }
            }
        });

        accessible.addAccessibleControlListener(new AccessibleControlAdapter()
        {
            public void getChildAtPoint(AccessibleControlEvent e)
            {
                Point location = toControl(e.x, e.y);
                e.childID = ACC.CHILDID_SELF;

                // Grid Items
                GridItem item = getItem(location);
                if (item != null)
                {
                    for (int i = 0; i < getItems().length; i++)
                    {
                        if (item.equals(getItem(i)))
                        {
                            e.childID = i;
                            return;
                        }
                    }
                }
                else
                {
                    // Column Headers
                    GridColumn column = overColumnHeader(location.x, location.y);
                    if (column != null)
                    {
                        for (int i = 0; i < getColumns().length; i++)
                        {
                            if (column.equals(getColumn(i)))
                            {
                                e.childID = getItems().length + i;
                                return;
                            }
                        }
                    }
                    else
                    {
                        // Column Group headers
                        GridColumnGroup columnGroup = overColumnGroupHeader(location.x, location.y);
                        if (columnGroup != null)
                        {
                            for (int i = 0; i < getColumnGroups().length; i++)
                            {
                                if (columnGroup.equals(getColumnGroup(i)))
                                {
                                    Rectangle toggle = columnGroup
                                        .getHeaderRenderer().getToggleBounds();
                                    if (toggle.contains(location.x, location.y))
                                    {
                                        // Toggle button for column group
                                        // header
                                        e.childID = getItems().length + getColumns().length
                                                    + getColumnGroups().length + i;
                                    }
                                    else
                                    {
                                        // Column Group header
                                        e.childID = getItems().length + getColumns().length + i;
                                    }
                                    return;
                                }
                            }
                        }
                    }
                }
            }

            public void getChildCount(AccessibleControlEvent e)
            {
                if (e.childID == ACC.CHILDID_SELF)
                {
                    int length = items.size();

                    if (isTree)
                    {
                        // Child count for parent. Here if the item parent
                        // is not an other item,
                        // it is consider as children of Grid
                        for (GridItem item : items) {
                            if (item.getParentItem() != null) {
                                length--;
                            }
                        }
                    }
                    e.detail = length;
                }
            }

            public void getChildren(AccessibleControlEvent e)
            {
                if (e.childID == ACC.CHILDID_SELF)
                {
                    int length = items.size();
                    if (isTree)
                    {
                        for (GridItem item : items) {
                            if (item.getParentItem() != null) {
                                length--;
                            }
                        }

                        Object[] children = new Object[length];
                        int j = 0;

                        for (int i = 0; i < items.size(); i++)
                        {
                            if (items.get(i).getParentItem() == null)
                            {
                                children[j] = new Integer(i);
                                j++;
                            }
                        }
                        e.children = children;
                    }
                    else
                    {
                        Object[] children = new Object[length];
                        for (int i = 0; i < items.size(); i++)
                        {
                            children[i] = new Integer(i);
                        }
                        e.children = children;
                    }
                }
            }

            public void getDefaultAction(AccessibleControlEvent e)
            {
                int childID = e.childID;
                if (childID >= 0 && childID < items.size())
                {
                    if (getItem(childID).hasChildren())
                    {
                        // Action of tree items
                        if (getItem(childID).isExpanded())
                        {
                            e.result = ACC_ITEM_ACTION_COLLAPSE;
                        }
                        else
                        {
                            e.result = ACC_ITEM_ACTION_EXPAND;
                        }
                    }
                    else
                    {
                        // action of default items
                        e.result = ACC_ITEM_DEFAULT_ACTION;
                    }
                }
                else if (childID >= items.size()
                         && childID < items.size() + columns.size() + columnGroups.length)
                {
                    // action of column and column group header
                    e.result = ACC_COLUMN_DEFAULT_ACTION;
                }
                else if (childID >= items.size() + columns.size() + columnGroups.length
                         && childID < items.size() + columns.size() + columnGroups.length
                                      + columnGroups.length)
                {
                    // action of toggle button of column group header
                    e.result = SWT.getMessage("SWT_Press");
                }
            }

            public void getLocation(AccessibleControlEvent e)
            {
                // location of parent
                Rectangle location = getBounds();
                location.x = 0;
                location.y = 0;
                int childID = e.childID;

                if (childID >= 0 && childID < items.size())
                {
                    // location of items
                    GridItem item = getItem(childID);
                    if (item != null)
                    {
                        Point p = getOrigin(columns.get(0), item);
                        location.y = p.y;
                        location.height = item.getHeight();
                    }
                }
                else if (childID >= items.size() && childID < items.size() + columns.size())
                {
                    // location of columns headers
                    GridColumn column = getColumn(childID - items.size());
                    if (column != null)
                    {
                        location.x = getColumnHeaderXPosition(column);
                        if (column.getColumnGroup() == null)
                        {
                            location.y = 0;
                        }
                        else
                        {
                            location.y = groupHeaderHeight;
                        }
                        location.height = headerHeight;
                        location.width = column.getWidth();
                    }
                }
                else if (childID >= items.size() + columns.size()
                         && childID < items.size() + columns.size() + columnGroups.length)
                {
                    // location of column group header
                    GridColumnGroup columnGroup = getColumnGroup(childID - items.size()
                                                                 - columns.size());
                    if (columnGroup != null)
                    {
                        location.y = 0;
                        location.height = groupHeaderHeight;
                        location.x = getColumnHeaderXPosition(columnGroup.getFirstVisibleColumn());
                        int width = 0;
                        for (int i = 0; i < columnGroup.getColumns().length; i++)
                        {
                            if (columnGroup.getColumns()[i].isVisible())
                            {
                                width += columnGroup.getColumns()[i].getWidth();
                            }
                        }
                        location.width = width;
                    }
                }
                else if (childID >= items.size() + columns.size() + columnGroups.length
                         && childID < items.size() + columns.size() + columnGroups.length
                                      + columnGroups.length)
                {
                    // location of toggle button of column group header
                    GridColumnGroup columnGroup = getColumnGroup(childID - items.size()
                                                                 - columns.size()
                                                                 - columnGroups.length);
                    location = columnGroup.getHeaderRenderer().getToggleBounds();
                }

                if (location != null)
                {
                    Point pt = toDisplay(location.x, location.y);
                    e.x = pt.x;
                    e.y = pt.y;
                    e.width = location.width;
                    e.height = location.height;
                }
            }

            public void getRole(AccessibleControlEvent e)
            {
                int childID = e.childID;
                if (childID >= 0 && childID < items.size())
                {
                    // role of items
                    if (isTree)
                    {
                        e.detail = ACC.ROLE_TREEITEM;
                    }
                    else
                    {
                        e.detail = ACC.ROLE_LISTITEM;
                    }
                }
                else if (childID >= items.size()
                         && childID < items.size() + columns.size() + columnGroups.length)
                {
                    // role of columns headers and column group headers
                    e.detail = ACC.ROLE_TABLECOLUMNHEADER;
                }
                else if (childID >= items.size() + columns.size() + columnGroups.length
                         && childID < items.size() + columns.size() + columnGroups.length
                                      + columnGroups.length)
                {
                    // role of toggle button of column group headers
                    e.detail = ACC.ROLE_PUSHBUTTON;
                }
                else if (childID == ACC.CHILDID_SELF)
                {
                    // role of parent
                    if (isTree)
                    {
                        e.detail = ACC.ROLE_TREE;
                    }
                    else
                    {
                        e.detail = ACC.ROLE_TABLE;
                    }
                }
            }

            public void getSelection(AccessibleControlEvent e)
            {
                e.childID = ACC.CHILDID_NONE;
                if (selectedItems.size() == 1)
                {
                    // Single selection
                    e.childID = indexOf(selectedItems.get(0));
                }
                else if (selectedItems.size() > 1)
                {
                    // multiple selection
                    e.childID = ACC.CHILDID_MULTIPLE;
                    int length = selectedItems.size();
                    Object[] children = new Object[length];

                    for (int i = 0; i < length; i++)
                    {
                        GridItem item = selectedItems.get(i);
                        children[i] = new Integer(indexOf(item));
                    }
                    e.children = children;
                }
            }

            public void getState(AccessibleControlEvent e)
            {
                int childID = e.childID;
                if (childID >= 0 && childID < items.size())
                {
                    // state of items
                    e.detail = ACC.STATE_SELECTABLE;
                    if (getDisplay().getActiveShell() == getParent().getShell())
                    {
                        e.detail |= ACC.STATE_FOCUSABLE;
                    }

                    if (selectedItems.contains(getItem(childID)))
                    {
                        e.detail |= ACC.STATE_SELECTED;
                        if (getDisplay().getActiveShell() == getParent().getShell())
                        {
                            e.detail |= ACC.STATE_FOCUSED;
                        }
                    }

                    if (getItem(childID).getChecked())
                    {
                        e.detail |= ACC.STATE_CHECKED;
                    }

                    // only for tree type items
                    if (getItem(childID).hasChildren())
                    {
                        if (getItem(childID).isExpanded())
                        {
                            e.detail |= ACC.STATE_EXPANDED;
                        }
                        else
                        {
                            e.detail |= ACC.STATE_COLLAPSED;
                        }
                    }

                    if (!getItem(childID).isVisible())
                    {
                        e.detail |= ACC.STATE_INVISIBLE;
                    }
                }
                else if (childID >= items.size()
                         && childID < items.size() + columns.size() + columnGroups.length)
                {
                    // state of column headers and column group headers
                    e.detail = ACC.STATE_READONLY;
                }
                else if (childID >= items.size() + columns.size() + columnGroups.length
                         && childID < items.size() + columns.size() + columnGroups.length
                                      + columnGroups.length)
                {
                    // state of toggle button of column group headers
                    if (getColumnGroup(
                                       childID - items.size() - columns.size()
                                           - columnGroups.length).getExpanded())
                    {
                        e.detail = ACC.STATE_EXPANDED;
                    }
                    else
                    {
                        e.detail = ACC.STATE_COLLAPSED;
                    }
                }
            }

            public void getValue(AccessibleControlEvent e)
            {
                int childID = e.childID;
                if (childID >= 0 && childID < items.size())
                {
                    // value for tree items
                    if (isTree)
                    {
                        e.result = "" + getItem(childID).getLevel();
                    }
                }
            }
        });

        addListener(SWT.Selection, new Listener()
        {
            public void handleEvent(Event event)
            {
                if (selectedItems.size() > 0)
                {
                    accessible.setFocus(items.indexOf(selectedItems.get(selectedItems.size() - 1)));
                }
            }
        });

        addTreeListener(new TreeListener()
        {
            public void treeCollapsed(TreeEvent e)
            {
                if (getFocusItem() != null)
                {
                    accessible.setFocus(items.indexOf(getFocusItem()));
                }
            }

            public void treeExpanded(TreeEvent e)
            {
                if (getFocusItem() != null)
                {
                    accessible.setFocus(items.indexOf(getFocusItem()));
                }
            }
        });
    }

    /**
     * @return the disposing
     */
    boolean isDisposing()
    {
        return disposing;
    }

    /**
     * @param hasSpanning the hasSpanning to set
     */
    void setHasSpanning(boolean hasSpanning)
    {
        this.hasSpanning = hasSpanning;
    }

    /**
     * Returns the receiver's tool tip text, or null if it has
     * not been set.
     *
     * @return the receiver's tool tip text
     *
     * @exception SWTException <ul>
     *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
     * </ul>
     */
	public String getToolTipText() {
		checkWidget();
		return toolTipText;
	}


	/**
	 * Sets the receiver's tool tip text to the argument, which
	 * may be null indicating that no tool tip text should be shown.
	 *
	 * @param string the new tool tip text (or null)
	 *
	 * @exception SWTException <ul>
	 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
	 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
	 * </ul>
	 */
	public void setToolTipText(String string) {
		checkWidget();
		toolTipText = string;
	}

	/**
	 * Updates the row height when the first image is set on an item.
	 * @param column the column the image is change
	 * @param item item which images has just been set on.
	 */
	void imageSetOnItem(int column, GridItem item)
	{
		if( sizeOnEveryItemImageChange ) {
			if( item == null || item.getImage(column) == null )
				return;

			int height = item.getImage(column).getBounds().height;
			//FIXME Needs better algorithm
			if( height + 20 > getItemHeight() ) {
				height = computeItemHeight(item,sizingGC);
				setItemHeight(height);
			}
		} else {
			if (firstImageSet || userModifiedItemHeight) return;

			int height = computeItemHeight(item,sizingGC);
			setItemHeight(height);

			firstImageSet = true;
		}
	}

    /**
     * Determines if the mouse is hovering on the selection drag area and changes the
     * pointer and sets field appropriately.
     * <p>
     * Note:  The 'selection drag area' is that part of the selection,
     * on which a drag event can be initiated.  This is either the border
     * of the selection (i.e. a cell border between a slected and a non-selected
     * cell) or the complete selection (i.e. anywhere on a selected cell).
     * What area serves as drag area is determined by {@link #setDragOnFullSelection(boolean)}.
     *
     * @param x
     * @param y
     * @return
     * @see #setDragOnFullSelection(boolean)
     */
    private boolean handleHoverOnSelectionDragArea(int x, int y)
    {
        boolean over = false;
//    	Point inSelection = null;

        if ( (!rowHeaderVisible || x > rowHeaderWidth-SELECTION_DRAG_BORDER_THRESHOLD)
        		&& (!columnHeadersVisible || y > headerHeight-SELECTION_DRAG_BORDER_THRESHOLD) )
        {
            // not on a header

//            if(!dragOnFullSelection)
//            {
//                // drag area is the border of the selection
//
//                if(cellSelectionEnabled)
//                {
//                    Point neP = new Point( x-SELECTION_DRAG_BORDER_THRESHOLD, y-SELECTION_DRAG_BORDER_THRESHOLD );
//                    Point ne = getCell(neP);
//                    Point nwP = new Point( x+SELECTION_DRAG_BORDER_THRESHOLD, y-SELECTION_DRAG_BORDER_THRESHOLD );
//                    Point nw = getCell(nwP);
//                    Point swP = new Point( x+SELECTION_DRAG_BORDER_THRESHOLD, y+SELECTION_DRAG_BORDER_THRESHOLD );
//                    Point sw = getCell(swP);
//                    Point seP = new Point( x-SELECTION_DRAG_BORDER_THRESHOLD, y+SELECTION_DRAG_BORDER_THRESHOLD );
//                    Point se = getCell(seP);
//
//                    boolean neSel = ne != null && isCellSelected(ne);
//                    boolean nwSel = nw != null && isCellSelected(nw);
//                    boolean swSel = sw != null && isCellSelected(sw);
//                    boolean seSel = se != null && isCellSelected(se);
//
//                    over = (neSel || nwSel || swSel || seSel) && (!neSel || !nwSel || !swSel || !seSel);
////                    inSelection = neSel ? neP : nwSel ? nwP : swSel ? swP : seSel ? seP : null;
//                }
//                else
//                {
//                    Point nP = new Point( x, y-SELECTION_DRAG_BORDER_THRESHOLD );
//                    GridItem n = getItem(nP);
//                    Point sP = new Point( x, y+SELECTION_DRAG_BORDER_THRESHOLD );
//                    GridItem s = getItem(sP);
//
//                    boolean nSel = n != null && isSelected(n);
//                    boolean sSel = s != null && isSelected(s);
//
//                    over = nSel != sSel;
////                    inSelection = nSel ? nP : sSel ? sP : null;
//                }
//            }
//            else
//            {
                // drag area is the entire selection

                if(cellSelectionEnabled)
                {
                	Point p = new Point(x,y);
                    Point cell = getCell(p);
                    over = cell !=null && isCellSelected(cell);
//                    inSelection = over ? p : null;
                }
                else
                {
                	Point p = new Point(x,y);
                    GridItem item = getItem(p);
                    over = item != null && isSelected(item);
//                    inSelection = over ? p : null;
                }
            }
//        }

        if (over != hoveringOnSelectionDragArea)
        {
//            if (over)
//            {
//                // use drag cursor only in border mode
//                if (!dragOnFullSelection)
//                    setCursor(getDisplay().getSystemCursor(SWT.CURSOR_SIZEALL));
////                potentialDragStart = inSelection;
//            }
//            else
//            {
//                setCursor(null);
////                potentialDragStart = null;
//            }
            hoveringOnSelectionDragArea = over;
        }
        return over;
    }

    /**
     * Display a mark indicating the point at which an item will be inserted.
     * This is used as a visual hint to show where a dragged item will be
     * inserted when dropped on the grid.  This method should not be called
     * directly, instead {@link DND#FEEDBACK_INSERT_BEFORE} or
     * {@link DND#FEEDBACK_INSERT_AFTER} should be set in
     * {@link DropTargetEvent#feedback} from within a {@link DropTargetListener}.
     *
     * @param item  the insert item.  Null will clear the insertion mark.
     * @param column  the column of the cell.  Null will make the insertion mark span all columns.
     * @param before  true places the insert mark above 'item'. false places
     *                the insert mark below 'item'.
     *
     * @exception IllegalArgumentException <ul>
     *    <li>ERROR_INVALID_ARGUMENT - if the item or column has been disposed</li>
     * </ul>
     * @exception SWTException <ul>
     *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
     * </ul>
     */
    void setInsertMark (GridItem item, GridColumn column, boolean before) {
    	checkWidget ();
    	if (item != null) {
    		if (item.isDisposed())
    			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
    	}
    	if (column != null) {
    		if (column.isDisposed())
    			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
    	}
    	insertMarkItem = item;
    	insertMarkColumn = column;
    	insertMarkBefore = before;
    	redraw();
    }

    /**
     * A helper method for {@link GridDropTargetEffect#dragOver(DropTargetEvent)}.
     *
     * @param point
     * @return true if point is near the top or bottom border of the visible grid area
     */
    boolean isInDragScrollArea(Point point) {
    	int rhw = rowHeaderVisible ? rowHeaderWidth : 0;
    	int chh = columnHeadersVisible ? headerHeight : 0;
    	Rectangle top = new Rectangle(rhw, chh, getClientArea().width - rhw, DRAG_SCROLL_AREA_HEIGHT);
    	Rectangle bottom = new Rectangle(rhw, getClientArea().height - DRAG_SCROLL_AREA_HEIGHT, getClientArea().width - rhw, DRAG_SCROLL_AREA_HEIGHT);
    	return top.contains(point) || bottom.contains(point);
    }

    /**
     * Clears the item at the given zero-relative index in the receiver.
     * The text, icon and other attributes of the item are set to the default
     * value.  If the table was created with the <code>SWT.VIRTUAL</code> style,
     * these attributes are requested again as needed.
     *
     * @param index the index of the item to clear
     * @param allChildren <code>true</code> if all child items of the indexed item should be
     * cleared recursively, and <code>false</code> otherwise
     *
     * @exception IllegalArgumentException <ul>
     *    <li>ERROR_INVALID_RANGE - if the index is not between 0 and the number of elements in the list minus 1 (inclusive)</li>
     * </ul>
     * @exception SWTException <ul>
     *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
     * </ul>
     *
     * @see SWT#VIRTUAL
     * @see SWT#SetData
     */
    public void clear(int index, boolean allChildren) {
    	checkWidget();
        if (index < 0 || index >= items.size()) {
            SWT.error(SWT.ERROR_INVALID_RANGE);
        }

    	GridItem item = getItem(index);
		item.clear(allChildren);
    	redraw();
    }

    /**
     * Clears the items in the receiver which are between the given
     * zero-relative start and end indices (inclusive).  The text, icon
     * and other attributes of the items are set to their default values.
     * If the table was created with the <code>SWT.VIRTUAL</code> style,
     * these attributes are requested again as needed.
     *
     * @param start the start index of the item to clear
     * @param end the end index of the item to clear
     * @param allChildren <code>true</code> if all child items of the range of items should be
     * cleared recursively, and <code>false</code> otherwise
     *
     * @exception IllegalArgumentException <ul>
     *    <li>ERROR_INVALID_RANGE - if either the start or end are not between 0 and the number of elements in the list minus 1 (inclusive)</li>
     * </ul>
     * @exception SWTException <ul>
     *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
     * </ul>
     *
     * @see SWT#VIRTUAL
     * @see SWT#SetData
     */
    public void clear(int start, int end, boolean allChildren) {
    	checkWidget();
    	if (start > end) return;

    	int count = items.size();
    	if (!(0 <= start && start <= end && end < count)) {
    		SWT.error(SWT.ERROR_INVALID_RANGE);
    	}
		for (int i=start; i<=end; i++) {
			GridItem item = items.get(i);
			item.clear(allChildren);
		}
		redraw();
    }

    /**
     * Clears the items at the given zero-relative indices in the receiver.
     * The text, icon and other attributes of the items are set to their default
     * values.  If the table was created with the <code>SWT.VIRTUAL</code> style,
     * these attributes are requested again as needed.
     *
     * @param indices the array of indices of the items
     * @param allChildren <code>true</code> if all child items of the indexed items should be
     * cleared recursively, and <code>false</code> otherwise
     *
     * @exception IllegalArgumentException <ul>
     *    <li>ERROR_INVALID_RANGE - if the index is not between 0 and the number of elements in the list minus 1 (inclusive)</li>
     *    <li>ERROR_NULL_ARGUMENT - if the indices array is null</li>
     * </ul>
     * @exception SWTException <ul>
     *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
     * </ul>
     *
     * @see SWT#VIRTUAL
     * @see SWT#SetData
     */
    public void clear(int [] indices, boolean allChildren) {
    	checkWidget();
    	if (indices == null) {
    		SWT.error(SWT.ERROR_NULL_ARGUMENT);
    	}
    	if (indices.length == 0) return;

    	int count = items.size();
    	for (int i=0; i<indices.length; i++) {
    		if (!(0 <= indices[i] && indices[i] < count)) {
    			SWT.error(SWT.ERROR_INVALID_RANGE);
    		}
    	}
    	for (int i=0; i<indices.length; i++) {
			GridItem item = items.get(indices[i]);
			item.clear(allChildren);
    	}
		redraw();
    }

    /**
     * Clears all the items in the receiver. The text, icon and other
     * attributes of the items are set to their default values. If the
     * table was created with the <code>SWT.VIRTUAL</code> style, these
     * attributes are requested again as needed.
     *
     * @param allChildren <code>true</code> if all child items of each item should be
     * cleared recursively, and <code>false</code> otherwise
     *
     * @exception SWTException <ul>
     *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
     * </ul>
     *
     * @see SWT#VIRTUAL
     * @see SWT#SetData
     */
    public void clearAll(boolean allChildren) {
    	checkWidget();
    	if (items.size() > 0)
    		clear(0, items.size()-1, allChildren);
    }

    /**
     * Recalculate the height of the header
     */
    public void recalculateHeader() {
    	int previous = getHeaderHeight();
        computeHeaderHeight(sizingGC);

        if( previous != getHeaderHeight() ) {
        	scrollValuesObsolete = true;
        	redraw();
        }
    }

    /**
     * Query the grid for all currently visible rows and columns
     * <p>
     * <b>This support is provisional and may change</b>
     * </p>
     * @return all currently visible rows and columns
     */
    public GridVisibleRange getVisibleRange() {
    	//FIXME I think we should remember the topIndex in the onPaint-method
    	int topIndex = getTopIndex();
    	int bottomIndex = getBottomIndex();
    	int startColumnIndex = getStartColumnIndex();
    	int endColumnIndex = getEndColumnIndex();

    	GridVisibleRange range = new GridVisibleRange();
    	range.items = new GridItem[0];
    	range.columns = new GridColumn[0];

    	if( topIndex <= bottomIndex ) {
    		if( items.size() > 0 ) {
        		range.items = new GridItem[bottomIndex - topIndex + 1];
        		for( int i = topIndex; i <= bottomIndex; i++ ) {
        			range.items[i - topIndex] = items.get(i);
        		}
    		}
    	}

    	if( startColumnIndex <= endColumnIndex ) {
    		if( displayOrderedColumns.size() > 0 ) {
    			List<GridColumn> cols = new ArrayList<GridColumn>();
    			for( int i = startColumnIndex; i <= endColumnIndex; i++ ) {
    				GridColumn col = displayOrderedColumns.get(i);
    				if( col.isVisible() ) {
    					cols.add(col);
    				}
    			}

    			range.columns = new GridColumn[cols.size()];
    			cols.toArray(range.columns);
    		}
    	}

    	return range;
    }

    int getStartColumnIndex() {
    	checkWidget();

    	if( startColumnIndex != -1 ) {
    		return startColumnIndex;
    	}

    	if( !hScroll.getVisible() ) {
    		startColumnIndex = 0;
    	}

    	startColumnIndex = hScroll.getSelection();

    	return startColumnIndex;
    }

    int getEndColumnIndex() {
    	checkWidget();

    	if( endColumnIndex != -1 ) {
    		return endColumnIndex;
    	}

    	if( displayOrderedColumns.size() == 0 ) {
    		endColumnIndex = 0;
    	} else if( getVisibleGridWidth() < 1 ) {
    		endColumnIndex = getStartColumnIndex();
    	} else {
    		int x = 0;
            x -= getHScrollSelectionInPixels();

            if (rowHeaderVisible)
            {
                //row header is actually painted later
                x += rowHeaderWidth;
            }

            int startIndex = getStartColumnIndex();
            GridColumn[] columns = new GridColumn[displayOrderedColumns.size()];
            displayOrderedColumns.toArray(columns);

            for (int i = startIndex; i < columns.length; i++)
            {
            	endColumnIndex = i;
                GridColumn column = columns[i];

                if (column.isVisible())
                {
                    x += column.getWidth();
                }

                if (x > getClientArea().width) {

                	break;
                }
            }

    	}

    	endColumnIndex = Math.max(0, endColumnIndex);

    	return endColumnIndex;
    }

    void setSizeOnEveryItemImageChange(boolean sizeOnEveryItemImageChange) {
    	this.sizeOnEveryItemImageChange = sizeOnEveryItemImageChange;
    }

	/**
	 * Returns the width of the row headers.
	 *
	 * @return width of the column header row
	 * @throws org.eclipse.swt.SWTException
	 *             <ul>
	 *             <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
	 *             </li>
	 *             <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *             thread that created the receiver</li>
	 *             </ul>
	 */
	public int getRowHeaderWidth() {
		checkWidget();
		return rowHeaderWidth;
	}

    /**
     * Sets the value of the auto-height feature. When enabled, this feature resizes the height of rows to
     * reflect the content of cells with word-wrapping enabled. Cell word-wrapping is enabled via the GridColumn.setWordWrap(boolean) method.
     * If column headers have word-wrapping enabled, this feature will also resize the height of the column headers as necessary.
     * @param enabled Set to true to enable this feature, false (default) otherwise.
     */
    public void setAutoHeight(boolean enabled)
    {
      if (autoHeight == enabled)
        return;

        checkWidget();
        autoHeight = enabled;
        setRowsResizeable(false); // turn of resizing of row height since it conflicts with this property
        redraw();
    }

    /**
     * Returns the value of the auto-height feature, which resizes row heights and column header heights based on word-wrapped content.
     * @return Returns whether or not the auto-height feature is enabled.
     * @see #setAutoHeight(boolean)
     */
    public boolean isAutoHeight()
    {
      return autoHeight;
    }

    /**
     * Sets the value of the auto-width feature. When enabled, this feature resizes the width of the row headers to
     * reflect the content of row headers.
     * @param enabled Set to true to enable this feature, false (default) otherwise.
     * @see #isAutoWidth()
     */
    public void setAutoWidth(boolean enabled)
    {
      if (autoWidth == enabled)
        return;

        checkWidget();
        autoWidth = enabled;
        redraw();
    }

    /**
     * Returns the value of the auto-height feature, which resizes row header width based on content.
     * @return Returns whether or not the auto-width feature is enabled.
     * @see #setAutoWidth(boolean)
     */
    public boolean isAutoWidth()
    {
      return autoWidth;
    }

    /**
     * Sets the value of the word-wrap feature for row headers. When enabled, this feature will word-wrap the contents of row headers.
     * @param enabled Set to true to enable this feature, false (default) otherwise.
     * @see #isWordWrapHeader()
     */
    public void setWordWrapHeader(boolean enabled)
    {
      if (wordWrapRowHeader == enabled)
        return;

        checkWidget();
        wordWrapRowHeader = enabled;
        redraw();
    }

    /**
     * Returns the value of the row header word-wrap feature, which word-wraps the content of row headers.
     * @return Returns whether or not the row header word-wrap feature is enabled.
     * @see #setWordWrapHeader(boolean)
     */
    public boolean isWordWrapHeader()
    {
      return wordWrapRowHeader;
    }
}


