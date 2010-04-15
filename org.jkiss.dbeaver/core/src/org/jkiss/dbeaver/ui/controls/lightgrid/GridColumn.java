/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package  org.jkiss.dbeaver.ui.controls.lightgrid;

import  org.jkiss.dbeaver.ui.controls.lightgrid.renderers.DefaultCellRenderer;
import  org.jkiss.dbeaver.ui.controls.lightgrid.renderers.DefaultColumnFooterRenderer;
import  org.jkiss.dbeaver.ui.controls.lightgrid.renderers.DefaultColumnHeaderRenderer;
import  org.jkiss.dbeaver.ui.controls.lightgrid.renderers.GridCellRenderer;
import  org.jkiss.dbeaver.ui.controls.lightgrid.renderers.GridFooterRenderer;
import  org.jkiss.dbeaver.ui.controls.lightgrid.renderers.GridHeaderRenderer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.TypedListener;

/**
 * <p>
 * NOTE: THIS WIDGET AND ITS API ARE STILL UNDER DEVELOPMENT. THIS IS A
 * PRE-RELEASE ALPHA VERSION. USERS SHOULD EXPECT API CHANGES IN FUTURE
 * VERSIONS.
 * </p>
 * Instances of this class represent a column in a grid widget.
 * <p>
 * <dl>
 * <dt><b>Styles:</b></dt>
 * <dd>SWT.LEFT, SWT.RIGHT, SWT.CENTER, SWT.CHECK</dd>
 * <dt><b>Events:</b></dt>
 * <dd>Move, Resize, Selection, Show, Hide</dd>
 * </dl>
 *
 * @author chris.gross@us.ibm.com
 */
public class GridColumn extends Item {

	private GridHeaderEditor controlEditor;

	/**
	 * Default width of the column.
	 */
	private static final int DEFAULT_WIDTH = 10;

	/**
	 * Parent table.
	 */
	private LightGrid parent;

	/**
	 * Header renderer.
	 */
	private GridHeaderRenderer headerRenderer = new DefaultColumnHeaderRenderer();

	private GridFooterRenderer footerRenderer = new DefaultColumnFooterRenderer();

	/**
	 * Cell renderer.
	 */
	private GridCellRenderer cellRenderer = new DefaultCellRenderer();

	/**
	 * Width of column.
	 */
	private int width = DEFAULT_WIDTH;

	/**
	 * Sort style of column. Only used to draw indicator, does not actually sort
	 * data.
	 */
	private int sortStyle = SWT.NONE;

	/**
	 * Determines if this column shows toggles.
	 */
	private boolean tree = false;

	/**
	 * Is this column resizable?
	 */
	private boolean resizeable = true;

	/**
	 * Is this column moveable?
	 */
	private boolean moveable = false;

	/**
	 * Is a summary column in a column group. Not applicable if this column is
	 * not in a group.
	 */
	private boolean summary = true;

	/**
	 * Is a detail column in a column group. Not applicable if this column is
	 * not in a group.
	 */
	private boolean detail = true;

	private boolean visible = true;

	private boolean cellSelectionEnabled = true;

	private GridColumnGroup group;

	private boolean checkable = true;

	private Image footerImage;

	private String footerText = "";

	private Font headerFont;

	private Font footerFont;

	private int minimumWidth = 0;

	private String headerTooltip = null;

	/**
	 * Constructs a new instance of this class given its parent (which must be a
	 * <code>Grid</code>) and a style value describing its behavior and
	 * appearance. The item is added to the end of the items maintained by its
	 * parent.
	 *
	 * @param parent
	 *            an Grid control which will be the parent of the new instance
	 *            (cannot be null)
	 * @param style
	 *            the style of control to construct
	 * @throws IllegalArgumentException
	 *             <ul>
	 *             <li>ERROR_NULL_ARGUMENT - if the parent is null</li>
	 *             </ul>
	 * @throws org.eclipse.swt.SWTException
	 *             <ul>
	 *             <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *             thread that created the parent</li>
	 *             <li>
	 *             ERROR_INVALID_SUBCLASS - if this class is not an allowed
	 *             subclass</li>
	 *             </ul>
	 */
	public GridColumn(LightGrid parent, int style) {
		this(parent, style, -1);
	}

	/**
	 * Constructs a new instance of this class given its parent (which must be a
	 * <code>Grid</code>), a style value describing its behavior and appearance,
	 * and the index at which to place it in the items maintained by its parent.
	 *
	 * @param parent
	 *            an Grid control which will be the parent of the new instance
	 *            (cannot be null)
	 * @param style
	 *            the style of control to construct
	 * @param index
	 *            the index to store the receiver in its parent
	 * @throws IllegalArgumentException
	 *             <ul>
	 *             <li>ERROR_NULL_ARGUMENT - if the parent is null</li>
	 *             </ul>
	 * @throws org.eclipse.swt.SWTException
	 *             <ul>
	 *             <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *             thread that created the parent</li>
	 *             <li>
	 *             ERROR_INVALID_SUBCLASS - if this class is not an allowed
	 *             subclass</li>
	 *             </ul>
	 */
	public GridColumn(LightGrid parent, int style, int index) {
		super(parent, style, index);

		init(parent, style, index);
	}

	/**
	 * Constructs a new instance of this class given its parent column group
	 * (which must be a <code>GridColumnGroup</code>), a style value describing
	 * its behavior and appearance, and the index at which to place it in the
	 * items maintained by its parent.
	 *
	 * @param parent
	 *            an Grid control which will be the parent of the new instance
	 *            (cannot be null)
	 * @param style
	 *            the style of control to construct
	 * @throws IllegalArgumentException
	 *             <ul>
	 *             <li>ERROR_NULL_ARGUMENT - if the parent is null</li>
	 *             </ul>
	 * @throws org.eclipse.swt.SWTException
	 *             <ul>
	 *             <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *             thread that created the parent</li>
	 *             <li>
	 *             ERROR_INVALID_SUBCLASS - if this class is not an allowed
	 *             subclass</li>
	 *             </ul>
	 */
	public GridColumn(GridColumnGroup parent, int style) {
		super(parent.getParent(), style, parent.getNewColumnIndex());

		init(parent.getParent(), style, parent.getNewColumnIndex());

		group = parent;

		group.newColumn(this);
	}

	private void init(LightGrid table, int style, int index) {
		this.parent = table;

		table.newColumn(this, index);

		initHeaderRenderer();
		initFooterRenderer();
		initCellRenderer();
	}

	/**
	 * {@inheritDoc}
	 */
	public void dispose() {
		if (!parent.isDisposing()) {
			parent.removeColumn(this);
			if (group != null)
				group.removeColumn(this);
		}
		super.dispose();
	}

	/**
	 * Initialize header renderer.
	 */
	private void initHeaderRenderer() {
		headerRenderer.setDisplay(getDisplay());
	}

	private void initFooterRenderer() {
		footerRenderer.setDisplay(getDisplay());
	}

	/**
	 * Initialize cell renderer.
	 */
	private void initCellRenderer() {
		cellRenderer.setDisplay(getDisplay());

		cellRenderer.setColumn(parent.indexOf(this));

		if ((getStyle() & SWT.RIGHT) == SWT.RIGHT) {
			cellRenderer.setAlignment(SWT.RIGHT);
		}

		if ((getStyle() & SWT.CENTER) == SWT.CENTER) {
			cellRenderer.setAlignment(SWT.CENTER);
		}

	}

	/**
	 * Returns the header renderer.
	 *
	 * @return header renderer
	 */
	public GridHeaderRenderer getHeaderRenderer() {
		return headerRenderer;
	}

	GridFooterRenderer getFooterRenderer() {
		return footerRenderer;
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
	 * @throws org.eclipse.swt.SWTException
	 *             <ul>
	 *             <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
	 *             </li>
	 *             <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *             thread that created the receiver</li>
	 *             </ul>
	 */
	public int getWidth() {
		checkWidget();
		return width;
	}

	/**
	 * Sets the width of the column.
	 *
	 * @param width
	 *            new width
	 * @throws org.eclipse.swt.SWTException
	 *             <ul>
	 *             <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
	 *             </li>
	 *             <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *             thread that created the receiver</li>
	 *             </ul>
	 */
	public void setWidth(int width) {
		checkWidget();
		setWidth(width, true);
	}

	void setWidth(int width, boolean redraw) {
		this.width = Math.max(minimumWidth, width);
		if (redraw) {
			parent.setScrollValuesObsolete();
			parent.redraw();
		}
	}

	/**
	 * Sets the sort indicator style for the column. This method does not actual
	 * sort the data in the table. Valid values include: SWT.UP, SWT.DOWN,
	 * SWT.NONE.
	 *
	 * @param style
	 *            SWT.UP, SWT.DOWN, SWT.NONE
	 * @throws org.eclipse.swt.SWTException
	 *             <ul>
	 *             <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
	 *             </li>
	 *             <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *             thread that created the receiver</li>
	 *             </ul>
	 */
	public void setSort(int style) {
		checkWidget();
		sortStyle = style;
		parent.redraw();
	}

	/**
	 * Returns the sort indicator value.
	 *
	 * @return SWT.UP, SWT.DOWN, SWT.NONE
	 * @throws org.eclipse.swt.SWTException
	 *             <ul>
	 *             <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
	 *             </li>
	 *             <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *             thread that created the receiver</li>
	 *             </ul>
	 */
	public int getSort() {
		checkWidget();
		return sortStyle;
	}

	/**
	 * Adds the listener to the collection of listeners who will be notified
	 * when the receiver's is pushed, by sending it one of the messages defined
	 * in the <code>SelectionListener</code> interface.
	 *
	 * @param listener
	 *            the listener which should be notified
	 * @throws IllegalArgumentException
	 *             <ul>
	 *             <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
	 *             </ul>
	 * @throws org.eclipse.swt.SWTException
	 *             <ul>
	 *             <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
	 *             </li>
	 *             <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *             thread that created the receiver</li>
	 *             </ul>
	 */
	public void addSelectionListener(SelectionListener listener) {
		checkWidget();
		if (listener == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		this.addListener(SWT.Selection, new TypedListener(listener));
	}

	/**
	 * Removes the listener from the collection of listeners who will be
	 * notified when the receiver's selection changes.
	 *
	 * @param listener
	 *            the listener which should no longer be notified
	 * @see SelectionListener
	 * @see #addSelectionListener(SelectionListener)
	 * @throws org.eclipse.swt.SWTException
	 *             <ul>
	 *             <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
	 *             </li>
	 *             <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *             thread that created the receiver</li>
	 *             </ul>
	 */
	public void removeSelectionListener(SelectionListener listener) {
		checkWidget();
		this.removeListener(SWT.Selection, listener);
	}

	/**
	 * Fires selection listeners.
	 */
	void fireListeners() {
		Event e = new Event();
		e.display = this.getDisplay();
		e.item = this;
		e.widget = parent;

		this.notifyListeners(SWT.Selection, e);
	}

	/**
	 * Returns true if the column is visible, false otherwise. If the column is
	 * in a group and the group is not expanded and this is a detail column,
	 * returns false (and vice versa).
	 *
	 * @return true if visible, false otherwise
	 *
	 * @throws org.eclipse.swt.SWTException
	 *             <ul>
	 *             <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
	 *             </li>
	 *             <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *             thread that created the receiver</li>
	 *             </ul>
	 */
	public boolean isVisible() {
		checkWidget();
		if (group != null) {
			if ((group.getExpanded() && !isDetail())
					|| (!group.getExpanded() && !isSummary())) {
				return false;
			}
		}
		return visible;
	}

	/**
	 * Returns the visibility state as set with {@code setVisible}.
	 *
	 * @return the visible
	 * @throws org.eclipse.swt.SWTException
	 *             <ul>
	 *             <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
	 *             </li>
	 *             <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *             thread that created the receiver</li>
	 *             </ul>
	 */
	public boolean getVisible() {
		checkWidget();
		return visible;
	}

	/**
	 * Sets the column's visibility.
	 *
	 * @param visible
	 *            the visible to set
	 * @throws org.eclipse.swt.SWTException
	 *             <ul>
	 *             <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
	 *             </li>
	 *             <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *             thread that created the receiver</li>
	 *             </ul>
	 */
	public void setVisible(boolean visible) {
		checkWidget();

		boolean before = isVisible();

		this.visible = visible;

		if (isVisible() != before) {
			if (visible) {
				notifyListeners(SWT.Show, new Event());
			} else {
				notifyListeners(SWT.Hide, new Event());
			}

			GridColumn[] colsOrdered = parent.getColumnsInOrder();
			boolean fire = false;
            for (GridColumn column : colsOrdered) {
                if (column == this) {
                    fire = true;
                } else {
                    if (column.isVisible())
                        column.fireMoved();
                }
            }

			parent.redraw();
		}
	}

	/**
	 * Causes the receiver to be resized to its preferred size.
	 *
	 * @throws org.eclipse.swt.SWTException
	 *             <ul>
	 *             <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
	 *             </li>
	 *             <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *             thread that created the receiver</li>
	 *             </ul>
	 */
	public void pack() {
		checkWidget();

		GC gc = new GC(parent);
		int newWidth = getHeaderRenderer().computeSize(gc, SWT.DEFAULT,
				SWT.DEFAULT, this).x;
		GridItem[] items = parent.getItems();
        for (GridItem item : items) {
            newWidth = Math.max(newWidth, getCellRenderer().computeSize(
                gc,
                SWT.DEFAULT, SWT.DEFAULT, item).x);
        }
		gc.dispose();
		setWidth(newWidth);
		parent.redraw();
	}

	/**
	 * Returns true if this column includes a tree toggle.
	 *
	 * @return true if the column includes the tree toggle.
	 * @throws org.eclipse.swt.SWTException
	 *             <ul>
	 *             <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
	 *             </li>
	 *             <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *             thread that created the receiver</li>
	 *             </ul>
	 */
	public boolean isTree() {
		checkWidget();
		return tree;
	}

	/**
	 * Sets the cell renderer.
	 *
	 * @param cellRenderer
	 *            The cellRenderer to set.
	 * @throws org.eclipse.swt.SWTException
	 *             <ul>
	 *             <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
	 *             </li>
	 *             <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *             thread that created the receiver</li>
	 *             </ul>
	 */
	public void setCellRenderer(GridCellRenderer cellRenderer) {
		checkWidget();

		this.cellRenderer = cellRenderer;
		initCellRenderer();
	}

	/**
	 * Sets the header renderer.
	 *
	 * @param headerRenderer
	 *            The headerRenderer to set.
	 * @throws org.eclipse.swt.SWTException
	 *             <ul>
	 *             <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
	 *             </li>
	 *             <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *             thread that created the receiver</li>
	 *             </ul>
	 */
	public void setHeaderRenderer(GridHeaderRenderer headerRenderer) {
		checkWidget();
		this.headerRenderer = headerRenderer;
		initHeaderRenderer();
	}

	/**
	 * Sets the header renderer.
	 *
	 * @param footerRenderer
	 *            The footerRenderer to set.
	 * @throws org.eclipse.swt.SWTException
	 *             <ul>
	 *             <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
	 *             </li>
	 *             <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *             thread that created the receiver</li>
	 *             </ul>
	 */
	public void setFooterRenderer(GridFooterRenderer footerRenderer) {
		checkWidget();
		this.footerRenderer = footerRenderer;
		initFooterRenderer();
	}

	/**
	 * Adds a listener to the list of listeners notified when the column is
	 * moved or resized.
	 *
	 * @param listener
	 *            listener
	 * @throws IllegalArgumentException
	 *             <ul>
	 *             <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
	 *             </ul>
	 * @throws org.eclipse.swt.SWTException
	 *             <ul>
	 *             <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
	 *             </li>
	 *             <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *             thread that created the receiver</li>
	 *             </ul>
	 */
	public void addControlListener(ControlListener listener) {
		checkWidget();
		if (listener == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		TypedListener typedListener = new TypedListener(listener);
		addListener(SWT.Resize, typedListener);
		addListener(SWT.Move, typedListener);
	}

	/**
	 * Removes the given control listener.
	 *
	 * @param listener
	 *            listener.
	 * @throws IllegalArgumentException
	 *             <ul>
	 *             <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
	 *             </ul>
	 * @throws org.eclipse.swt.SWTException
	 *             <ul>
	 *             <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
	 *             </li>
	 *             <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *             thread that created the receiver</li>
	 *             </ul>
	 */
	public void removeControlListener(ControlListener listener) {
		checkWidget();
		if (listener == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
		removeListener(SWT.Resize, listener);
		removeListener(SWT.Move, listener);
	}

	/**
	 * Fires moved event.
	 */
	void fireMoved() {
		Event e = new Event();
		e.display = this.getDisplay();
		e.item = this;
		e.widget = parent;

		this.notifyListeners(SWT.Move, e);
	}

	/**
	 * Fires resized event.
	 */
	void fireResized() {
		Event e = new Event();
		e.display = this.getDisplay();
		e.item = this;
		e.widget = parent;

		this.notifyListeners(SWT.Resize, e);
	}

	/**
	 * Adds or removes the columns tree toggle.
	 *
	 * @param tree
	 *            true to add toggle.
	 * @throws org.eclipse.swt.SWTException
	 *             <ul>
	 *             <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
	 *             </li>
	 *             <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *             thread that created the receiver</li>
	 *             </ul>
	 */
	public void setTree(boolean tree) {
		checkWidget();

		this.tree = tree;
		parent.redraw();
	}

	/**
	 * Returns the column alignment.
	 *
	 * @return SWT.LEFT, SWT.RIGHT, SWT.CENTER
	 * @throws org.eclipse.swt.SWTException
	 *             <ul>
	 *             <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
	 *             </li>
	 *             <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *             thread that created the receiver</li>
	 *             </ul>
	 */
	public int getAlignment() {
		checkWidget();
		return cellRenderer.getAlignment();
	}

	/**
	 * Sets the column alignment.
	 *
	 * @param alignment
	 *            SWT.LEFT, SWT.RIGHT, SWT.CENTER
	 * @throws org.eclipse.swt.SWTException
	 *             <ul>
	 *             <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
	 *             </li>
	 *             <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *             thread that created the receiver</li>
	 *             </ul>
	 */
	public void setAlignment(int alignment) {
		checkWidget();
		cellRenderer.setAlignment(alignment);
	}

	/**
	 * Returns true if this column is moveable.
	 *
	 * @return true if moveable.
	 * @throws org.eclipse.swt.SWTException
	 *             <ul>
	 *             <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
	 *             </li>
	 *             <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *             thread that created the receiver</li>
	 *             </ul>
	 */
	public boolean getMoveable() {
		checkWidget();
		return moveable;
	}

	/**
	 * Sets the column moveable or fixed.
	 *
	 * @param moveable
	 *            true to enable column moving
	 * @throws org.eclipse.swt.SWTException
	 *             <ul>
	 *             <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
	 *             </li>
	 *             <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *             thread that created the receiver</li>
	 *             </ul>
	 */
	public void setMoveable(boolean moveable) {
		checkWidget();
		this.moveable = moveable;
		parent.redraw();
	}

	/**
	 * Returns true if the column is resizeable.
	 *
	 * @return true if the column is resizeable.
	 * @throws org.eclipse.swt.SWTException
	 *             <ul>
	 *             <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
	 *             </li>
	 *             <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *             thread that created the receiver</li>
	 *             </ul>
	 */
	public boolean getResizeable() {
		checkWidget();
		return resizeable;
	}

	/**
	 * Sets the column resizeable.
	 *
	 * @param resizeable
	 *            true to make the column resizeable
	 * @throws org.eclipse.swt.SWTException
	 *             <ul>
	 *             <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
	 *             </li>
	 *             <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *             thread that created the receiver</li>
	 *             </ul>
	 */
	public void setResizeable(boolean resizeable) {
		checkWidget();
		this.resizeable = resizeable;
	}

	/**
	 * Returns the column group if this column was created inside a group, or
	 * {@code null} otherwise.
	 *
	 * @return the column group.
	 * @throws org.eclipse.swt.SWTException
	 *             <ul>
	 *             <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
	 *             </li>
	 *             <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *             thread that created the receiver</li>
	 *             </ul>
	 */
	public GridColumnGroup getColumnGroup() {
		checkWidget();
		return group;
	}

	/**
	 * Returns true if this column is set as a detail column in a column group.
	 * Detail columns are shown when the group is expanded.
	 *
	 * @return true if the column is a detail column.
	 * @throws org.eclipse.swt.SWTException
	 *             <ul>
	 *             <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
	 *             </li>
	 *             <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *             thread that created the receiver</li>
	 *             </ul>
	 */
	public boolean isDetail() {
		checkWidget();
		return detail;
	}

	/**
	 * Sets the column as a detail column in a column group. Detail columns are
	 * shown when a column group is expanded. If this column was not created in
	 * a column group, this method has no effect.
	 *
	 * @param detail
	 *            true to show this column when the group is expanded.
	 * @throws org.eclipse.swt.SWTException
	 *             <ul>
	 *             <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
	 *             </li>
	 *             <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *             thread that created the receiver</li>
	 *             </ul>
	 */
	public void setDetail(boolean detail) {
		checkWidget();
		this.detail = detail;
	}

	/**
	 * Returns true if this column is set as a summary column in a column group.
	 * Summary columns are shown when the group is collapsed.
	 *
	 * @return true if the column is a summary column.
	 * @throws org.eclipse.swt.SWTException
	 *             <ul>
	 *             <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
	 *             </li>
	 *             <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *             thread that created the receiver</li>
	 *             </ul>
	 */
	public boolean isSummary() {
		checkWidget();
		return summary;
	}

	/**
	 * Sets the column as a summary column in a column group. Summary columns
	 * are shown when a column group is collapsed. If this column was not
	 * created in a column group, this method has no effect.
	 *
	 * @param summary
	 *            true to show this column when the group is collapsed.
	 * @throws org.eclipse.swt.SWTException
	 *             <ul>
	 *             <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
	 *             </li>
	 *             <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *             thread that created the receiver</li>
	 *             </ul>
	 */
	public void setSummary(boolean summary) {
		checkWidget();
		this.summary = summary;
	}

	/**
	 * Returns the bounds of this column's header.
	 *
	 * @return bounds of the column header
	 */
	Rectangle getBounds() {
		Rectangle bounds = new Rectangle(0, 0, 0, 0);

		if (!isVisible()) {
			return bounds;
		}

		Point loc = parent.getOrigin(this, null);
		bounds.x = loc.x;
		bounds.y = loc.y;
		bounds.width = getWidth();
		bounds.height = parent.getHeaderHeight();
		if (getColumnGroup() != null) {
			bounds.height -= parent.getGroupHeaderHeight();
		}

		return bounds;
	}

	/**
	 * Returns true if cells in the receiver can be selected.
	 *
	 * @return the cellSelectionEnabled
	 * @throws org.eclipse.swt.SWTException
	 *             <ul>
	 *             <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
	 *             </li>
	 *             <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *             thread that created the receiver</li>
	 *             </ul>
	 */
	public boolean getCellSelectionEnabled() {
		checkWidget();
		return cellSelectionEnabled;
	}

	/**
	 * Sets whether cells in the receiver can be selected.
	 *
	 * @param cellSelectionEnabled
	 *            the cellSelectionEnabled to set
	 * @throws org.eclipse.swt.SWTException
	 *             <ul>
	 *             <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
	 *             </li>
	 *             <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *             thread that created the receiver</li>
	 *             </ul>
	 */
	public void setCellSelectionEnabled(boolean cellSelectionEnabled) {
		checkWidget();
		this.cellSelectionEnabled = cellSelectionEnabled;
	}

	/**
	 * Returns the parent grid.
	 *
	 * @return the parent grid.
	 * @throws org.eclipse.swt.SWTException
	 *             <ul>
	 *             <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
	 *             </li>
	 *             <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *             thread that created the receiver</li>
	 *             </ul>
	 */
	public LightGrid getParent() {
		checkWidget();
		return parent;
	}

	/**
	 * Returns the checkable state. If false the checkboxes in the column cannot
	 * be checked.
	 *
	 * @return true if the column is checkable (only applicable when style is
	 *         SWT.CHECK).
	 * @throws org.eclipse.swt.SWTException
	 *             <ul>
	 *             <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
	 *             </li>
	 *             <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *             thread that created the receiver</li>
	 *             </ul>
	 */
	public boolean getCheckable() {
		checkWidget();
		return checkable;
	}

	/**
	 * Sets the checkable state. If false the checkboxes in the column cannot be
	 * checked.
	 *
	 * @param checkable
	 *            the new checkable state.
	 * @throws org.eclipse.swt.SWTException
	 *             <ul>
	 *             <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
	 *             </li>
	 *             <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *             thread that created the receiver</li>
	 *             </ul>
	 */
	public void setCheckable(boolean checkable) {
		checkWidget();
		this.checkable = checkable;
	}

	void setColumnIndex(int newIndex) {
		cellRenderer.setColumn(newIndex);
	}

	/**
	 * Returns the true if the cells in receiver wrap their text.
	 *
	 * @return true if the cells wrap their text.
	 * @throws org.eclipse.swt.SWTException
	 *             <ul>
	 *             <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
	 *             </li>
	 *             <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *             thread that created the receiver</li>
	 *             </ul>
	 */
	public boolean getWordWrap() {
		checkWidget();
		return cellRenderer.isWordWrap();
	}

	/**
	 * If the argument is true, wraps the text in the receiver's cells. This
	 * feature will not cause the row height to expand to accommodate the
	 * wrapped text. Please use <code>Grid#setItemHeight</code> to change the
	 * height of each row.
	 *
	 * @param wordWrap
	 *            true to make cells wrap their text.
	 * @throws org.eclipse.swt.SWTException
	 *             <ul>
	 *             <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
	 *             </li>
	 *             <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *             thread that created the receiver</li>
	 *             </ul>
	 */
	public void setWordWrap(boolean wordWrap) {
		checkWidget();
		cellRenderer.setWordWrap(wordWrap);
		parent.redraw();
	}

	/**
	 * Sets whether or not text is word-wrapped in the header for this column.
	 * If Grid.setAutoHeight(true) is set, the row height is adjusted to
	 * accommodate word-wrapped text.
	 *
	 * @param wordWrap
	 *            Set to true to wrap the text, false otherwise
	 * @see #getHeaderWordWrap()
	 */
	public void setHeaderWordWrap(boolean wordWrap) {
		checkWidget();
		headerRenderer.setWordWrap(wordWrap);
		parent.redraw();
	}

	/**
	 * Returns whether or not text is word-wrapped in the header for this
	 * column.
	 *
	 * @return true if the header wraps its text.
	 * @see GridColumn#setHeaderWordWrap(boolean)
	 */
	public boolean getHeaderWordWrap() {
		checkWidget();
		return headerRenderer.isWordWrap();
	}

	/**
	 * Set a new editor at the top of the control. If there's an editor already
	 * set it is disposed.
	 *
	 * @param control
	 *            the control to be displayed in the header
	 */
	public void setHeaderControl(Control control) {
		if (this.controlEditor == null) {
			this.controlEditor = new GridHeaderEditor(this);
			this.controlEditor.initColumn();
		}
		this.controlEditor.setEditor(control);
		getParent().recalculateHeader();

		if (control != null) {
			// We need to realign if multiple editors are set it is possible
			// that
			// a later one needs more space
			control.getDisplay().asyncExec(new Runnable() {

				public void run() {
					if (GridColumn.this.controlEditor != null
							&& GridColumn.this.controlEditor.getEditor() != null) {
						GridColumn.this.controlEditor.layout();
					}
				}

			});
		}
	}

	/**
	 * @return the current header control
	 */
	public Control getHeaderControl() {
		if (this.controlEditor != null) {
			return this.controlEditor.getEditor();
		}
		return null;
	}


	/**
	 * Returns the tooltip of the column header.
	 *
	 * @return the tooltip text
	 */
	public String getHeaderTooltip() {
		checkWidget();
		return headerTooltip;
	}

	/**
	 * Sets the tooltip text of the column header.
	 *
	 * @param tooltip the tooltip text
	 */
	public void setHeaderTooltip(String tooltip) {
		checkWidget();
		this.headerTooltip = tooltip;
	}

	/**
	 * Sets the receiver's footer image to the argument, which may be null
	 * indicating that no image should be displayed.
	 *
	 * @param image
	 *            the image to display on the receiver (may be null)
	 */
	public void setFooterImage(Image image) {
		checkWidget();
		if (image != null && image.isDisposed())
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		this.footerImage = image;
	}

	/**
	 * Sets the receiver's footer text.
	 *
	 * @param string
	 *            the new text
	 */
	public void setFooterText(String string) {
		checkWidget();
		if (string == null)
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		this.footerText = string;
	}

	/**
	 * Returns the receiver's footer image if it has one, or null if it does
	 * not.
	 *
	 * @return the receiver's image
	 */
	public Image getFooterImage() {
		checkWidget();
		return footerImage;
	}

	/**
	 * Returns the receiver's footer text, which will be an empty string if it
	 * has never been set.
	 *
	 * @return the receiver's text
	 */
	public String getFooterText() {
		checkWidget();
		return footerText;
	}

	/**
	 * Returns the font that the receiver will use to paint textual information
	 * for the header.
	 *
	 * @return the receiver's font
	 * @throws SWTException
	 *             <ul>
	 *             <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
	 *             </li>
	 *             <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *             thread that created the receiver</li>
	 *             </ul>
	 */
	public Font getHeaderFont() {
		checkWidget();

		if (headerFont == null) {
			return parent.getFont();
		}
		return headerFont;
	}

	/**
	 * Sets the Font to be used when displaying the Header text.
	 *
	 * @param font
	 */
	public void setHeaderFont(Font font) {
		checkWidget();
		headerFont = font;
	}

	/**
	 * Returns the font that the receiver will use to paint textual information
	 * for the footer.
	 *
	 * @return the receiver's font
	 * @throws SWTException
	 *             <ul>
	 *             <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed
	 *             </li>
	 *             <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *             thread that created the receiver</li>
	 *             </ul>
	 */
	public Font getFooterFont() {
		checkWidget();

		if (footerFont == null) {
			return parent.getFont();
		}
		return footerFont;
	}

	/**
	 * Sets the Font to be used when displaying the Footer text.
	 *
	 * @param font
	 */
	public void setFooterFont(Font font) {
		checkWidget();
		footerFont = font;
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