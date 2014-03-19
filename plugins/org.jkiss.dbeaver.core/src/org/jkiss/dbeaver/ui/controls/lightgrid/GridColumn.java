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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.controls.lightgrid.renderers.*;

/**
 * Instances of this class represent a column in a grid widget.
 * For grid internal use.
 *
 * @author serge@jkiss.org
 */
public class GridColumn {

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
	private final LightGrid parent;
    private final Object element;

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

	private int minimumWidth = 0;

	private String headerTooltip = null;

    private String text;
    private Image image;
    private int style = SWT.NONE;

    /**
	 * Constructs a new instance of this class given its parent (which must be a
	 * <code>Grid</code>) and a style value describing its behavior and
	 * appearance. The item is added to the end of the items maintained by its
	 * parent.
	 */
	public GridColumn(LightGrid parent, Object element) {
		this(parent, -1, element);
	}

	/**
	 * Constructs a new instance of this class given its parent (which must be a
	 * <code>Grid</code>), a style value describing its behavior and appearance,
	 * and the index at which to place it in the items maintained by its parent.
	 *
	 * @param parent
	 *            an Grid control which will be the parent of the new instance
	 *            (cannot be null)
	 * @param index
	 *            the index to store the receiver in its parent
	 */
	public GridColumn(LightGrid parent, int index, Object element) {
        this.parent = parent;
        this.cellRenderer = new GridCellRenderer(parent);
        this.sortRenderer = new DefaultSortRenderer(this);
        this.element = element;
        parent.newColumn(this, index);

        initCellRenderer();
	}

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Image getImage() {
        return image;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public int getStyle() {
        return style;
    }

    public Object getElement() {
        return element;
    }

    public int getIndex()
    {
        return getParent().indexOf(this);
    }

    public AbstractRenderer getSortRenderer()
    {
        return sortRenderer;
    }

    public void setSortRenderer(AbstractRenderer sortRenderer)
    {
        this.sortRenderer = sortRenderer;
    }

    public void dispose() {
		if (!parent.isDisposing()) {
			parent.removeColumn(this);
		}
	}

	/**
	 * Initialize cell renderer.
	 */
	private void initCellRenderer() {
		if ((getStyle() & SWT.RIGHT) == SWT.RIGHT) {
			cellRenderer.setAlignment(SWT.RIGHT);
		}

		if ((getStyle() & SWT.CENTER) == SWT.CENTER) {
			cellRenderer.setAlignment(SWT.CENTER);
		}

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

	/**
	 * Sets the sort indicator style for the column. This method does not actual
	 * sort the data in the table. Valid values include: SWT.UP, SWT.DOWN,
	 * SWT.NONE.
	 *
	 * @param style
	 *            SWT.UP, SWT.DOWN, SWT.NONE
	 */
	public void setSort(int style) {
		sortStyle = style;
		parent.redraw();
	}

	/**
	 * Returns the sort indicator value.
	 *
	 * @return SWT.UP, SWT.DOWN, SWT.NONE
	 */
	public int getSort() {
		return sortStyle;
	}

    public boolean isSortable()
    {
        return sortStyle != SWT.NONE;
    }

	/**
	 * Causes the receiver to be resized to its preferred size.
	 *
	 */
	public void pack() {
		int newWidth = computeHeaderWidth();
        int columnIndex = getIndex();
        int topIndex = parent.getTopIndex();
        int bottomIndex = parent.getBottomIndex();
        if (topIndex >= 0 && bottomIndex >= topIndex) {
            int itemCount = parent.getItemCount();
            GridCell cell = new GridCell(element, element);
            for (int i = topIndex; i <= bottomIndex && i < itemCount; i++) {
                cell.row = parent.getRowElement(i);
                newWidth = Math.max(newWidth, computeCellWidth(cell));
            }
        }

		setWidth(newWidth);
		parent.redraw();
	}

    private int computeCellWidth(GridCell cell) {
        int x = 0;

        x += leftMargin;

        Image image = parent.getContentProvider().getCellImage(cell);
        if (image != null) {
            x += image.getBounds().width + insideMargin;
        }

        x += parent.sizingGC.textExtent(parent.getCellText(cell)).x + rightMargin;
        return x;
    }

	/**
	 * Returns the column alignment.
	 *
	 * @return SWT.LEFT, SWT.RIGHT, SWT.CENTER
	 */
	public int getAlignment() {
		return cellRenderer.getAlignment();
	}

	/**
	 * Sets the column alignment.
	 *
	 * @param alignment
	 *            SWT.LEFT, SWT.RIGHT, SWT.CENTER
	 */
	public void setAlignment(int alignment) {
		cellRenderer.setAlignment(alignment);
	}

	/**
	 * Returns true if the column is resizeable.
	 *
	 * @return true if the column is resizeable.
	 */
	public boolean getResizeable() {
		return resizeable;
	}

	/**
	 * Sets the column resizeable.
	 *
	 * @param resizeable
	 *            true to make the column resizeable
	 */
	public void setResizeable(boolean resizeable) {
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
		return cellSelectionEnabled;
	}

	/**
	 * Sets whether cells in the receiver can be selected.
	 *
	 * @param cellSelectionEnabled
	 *            the cellSelectionEnabled to set
	 */
	public void setCellSelectionEnabled(boolean cellSelectionEnabled) {
		this.cellSelectionEnabled = cellSelectionEnabled;
	}

	/**
	 * Returns the parent grid.
	 *
	 * @return the parent grid.
	 */
	public LightGrid getParent() {
		return parent;
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
	@Nullable
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
	@Nullable
    public String getHeaderTooltip() {
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
	public void setHeaderTooltip(@Nullable String tooltip) {
		this.headerTooltip = tooltip;
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