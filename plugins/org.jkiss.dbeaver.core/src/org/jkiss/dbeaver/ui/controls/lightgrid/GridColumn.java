/*
 * Copyright (C) 2010-2012 Serge Rieder
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
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Listener;
import org.jkiss.dbeaver.ui.controls.lightgrid.renderers.*;

/**
 * Instances of this class represent a column in a grid widget.
 * For grid internal use.
 */
public class GridColumn extends Item {

	private GridHeaderEditor controlEditor;

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
	private LightGrid parent;

	/**
	 * Header renderer.
	 */
	private GridColumnRenderer headerRenderer;

	private GridColumnRenderer footerRenderer;

    private AbstractRenderer sortRenderer;
	/**
	 * Cell renderer.
	 */
	private GridCellRenderer cellRenderer;

	/**
	 * Width of column.
	 */
	private int width = DEFAULT_WIDTH;

	/**
	 * Sort style of column. Only used to draw indicator, does not actually sort
	 * data.
	 */
	private int sortStyle = SWT.UP;

	/**
	 * Is this column resizable?
	 */
	private boolean resizeable = true;

	private boolean cellSelectionEnabled = true;

	private Image footerImage;

	private String footerText = "";

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
	 */
	public GridColumn(LightGrid parent, int style, int index) {
		super(parent, style, index);

		init(parent, index);
	}

    public int getIndex()
    {
        return getParent().indexOf(this);
    }

	private void init(LightGrid grid, int index) {
		this.parent = grid;
        sortRenderer = new SortArrowRenderer(grid);
        headerRenderer = new DefaultColumnHeaderRenderer(grid);
        footerRenderer = new DefaultColumnFooterRenderer(grid);
        cellRenderer = new DefaultCellRenderer(grid);
		grid.newColumn(this, index);

		initCellRenderer();

/*
        Button btn = new Button(grid, SWT.PUSH | SWT.NO_FOCUS);
        btn.setText("xxx");

        setHeaderControl(btn);
*/
	}

    public AbstractRenderer getSortRenderer()
    {
        return sortRenderer;
    }

    public void setSortRenderer(AbstractRenderer sortRenderer)
    {
        this.sortRenderer = sortRenderer;
    }

    /**
	 * {@inheritDoc}
	 */
	@Override
    public void dispose() {
		if (!parent.isDisposing()) {
			parent.removeColumn(this);
		}
		super.dispose();
	}

	/**
	 * Initialize cell renderer.
	 */
	private void initCellRenderer() {
		cellRenderer.setColumn(getIndex());

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
	public GridColumnRenderer getHeaderRenderer() {
		return headerRenderer;
	}

	GridColumnRenderer getFooterRenderer() {
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

    public int computeHeaderHeight()
    {
        int y = topMargin + parent.sizingGC.getFontMetrics().getHeight() + bottomMargin;


        if (getImage() != null) {
            y = Math.max(y, topMargin + getImage().getBounds().height + bottomMargin);
        }

        if( getHeaderControl() != null ) {
            y += getHeaderControl().computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
        }

		return y;
    }

    public boolean isOverSortArrow(int x)
    {
        if (!isSortable()) {
            return false;
        }
        int arrowEnd = getBounds().width - rightMargin;
        int arrowBegin = arrowEnd - sortRenderer.getBounds().width;
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
            x += rightMargin + sortRenderer.getBounds().width;
        }

        return x;
    }

    public int computeFooterHeight()
    {
        int y = topMargin;

        y += parent.sizingGC.getFontMetrics().getHeight();

        y += bottomMargin;

        if (getFooterImage() != null) {
            y = Math.max(y, topMargin + getFooterImage().getBounds().height + bottomMargin);
        }

        return y;
    }

	/**
	 * Sets the sort indicator style for the column. This method does not actual
	 * sort the data in the table. Valid values include: SWT.UP, SWT.DOWN,
	 * SWT.NONE.
	 *
	 * @param style
	 *            SWT.UP, SWT.DOWN, SWT.NONE
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
	 */
	public int getSort() {
		checkWidget();
		return sortStyle;
	}

    public boolean isSortable()
    {
        return sortStyle != SWT.NONE;
    }

	/**
	 * Adds the listener to the collection of listeners who will be notified
	 * when the receiver's is pushed, by sending it one of the messages defined
	 * in the <code>SelectionListener</code> interface.
	 *
	 * @param listener
	 *            the listener which should be notified
	 */
	public void addSelectionListener(final SelectionListener listener) {
		checkWidget();
		this.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event)
            {
                listener.widgetSelected(new SelectionEvent(event));
            }
        });
	}

	/**
	 * Removes the listener from the collection of listeners who will be
	 * notified when the receiver's selection changes.
	 *
	 * @param listener
	 *            the listener which should no longer be notified
	 * @see SelectionListener
	 * @see #addSelectionListener(SelectionListener)
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
	 * Causes the receiver to be resized to its preferred size.
	 *
	 */
	public void pack() {
		checkWidget();

		int newWidth = computeHeaderWidth();
        if (parent.getContentLabelProvider() != null) {
            int columnIndex = getIndex();
            int topIndex = parent.getTopIndex();
            int bottomIndex = parent.getBottomIndex();
            if (topIndex >= 0 && bottomIndex > topIndex) {
                for (int i = topIndex; i <= bottomIndex; i++) {
                    newWidth = Math.max(newWidth, computeCellWidth(columnIndex, i));
                }
            }
        }
		setWidth(newWidth);
		parent.redraw();
	}

    private int computeCellWidth(int column, int row) {
        int x = 0;

        x += leftMargin;

        Image image = parent.getCellImage(column, row);
        if (image != null) {
            x += image.getBounds().width + insideMargin;
        }

        x += parent.sizingGC.textExtent(parent.getCellText(column, row)).x + rightMargin;
        return x;
    }

    /**
	 * Sets the cell renderer.
	 *
	 * @param cellRenderer
	 *            The cellRenderer to set.
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
	 */
	public void setHeaderRenderer(GridColumnRenderer headerRenderer) {
		checkWidget();
		this.headerRenderer = headerRenderer;
	}

	/**
	 * Sets the header renderer.
	 *
	 * @param footerRenderer
	 *            The footerRenderer to set.
	 */
	public void setFooterRenderer(GridColumnRenderer footerRenderer) {
		checkWidget();
		this.footerRenderer = footerRenderer;
	}

	/**
	 * Adds a listener to the list of listeners notified when the column is
	 * moved or resized.
	 *
	 * @param listener
	 *            listener
	 */
	public void addControlListener(final ControlListener listener) {
		checkWidget();
		addListener(SWT.Resize, new Listener() {
            @Override
            public void handleEvent(Event event)
            {
                listener.controlResized(new ControlEvent(event));
            }
        });
		addListener(SWT.Move, new Listener() {
            @Override
            public void handleEvent(Event event)
            {
                listener.controlMoved(new ControlEvent(event));
            }
        });
	}

	/**
	 * Removes the given control listener.
	 *
	 * @param listener
	 *            listener.
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
	 * Returns the column alignment.
	 *
	 * @return SWT.LEFT, SWT.RIGHT, SWT.CENTER
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
	 */
	public void setAlignment(int alignment) {
		checkWidget();
		cellRenderer.setAlignment(alignment);
	}

	/**
	 * Returns true if the column is resizeable.
	 *
	 * @return true if the column is resizeable.
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
	 */
	public void setResizeable(boolean resizeable) {
		checkWidget();
		this.resizeable = resizeable;
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
		checkWidget();
		return cellSelectionEnabled;
	}

	/**
	 * Sets whether cells in the receiver can be selected.
	 *
	 * @param cellSelectionEnabled
	 *            the cellSelectionEnabled to set
	 */
	public void setCellSelectionEnabled(boolean cellSelectionEnabled) {
		checkWidget();
		this.cellSelectionEnabled = cellSelectionEnabled;
	}

	/**
	 * Returns the parent grid.
	 *
	 * @return the parent grid.
	 */
	public LightGrid getParent() {
		checkWidget();
		return parent;
	}

	void setColumnIndex(int newIndex) {
		cellRenderer.setColumn(newIndex);
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

				@Override
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