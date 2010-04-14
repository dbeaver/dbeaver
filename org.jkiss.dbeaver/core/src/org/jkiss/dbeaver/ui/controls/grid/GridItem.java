/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.grid;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.TypedListener;
import org.jkiss.dbeaver.ui.controls.grid.renderers.GridCellRenderer;

public class GridItem extends Item {
	private ArrayList<Color> backgrounds;
	private ArrayList<Integer> columnSpans;
	private ArrayList<Integer> rowSpans;
    private ArrayList<Font> fonts;
    private ArrayList<Color> foregrounds;
    private ArrayList<Boolean> grayeds;
    private ArrayList<Image> images;
    private ArrayList<String> texts;
    private ArrayList<String> tooltips;

    private ArrayList<GridItem> children;

	private Color defaultBackground;
	private Font defaultFont;
	private Color defaultForeground;

	private int height = 1;
	private boolean expanded = false;

	/**
	 * Level of item in a tree.
	 */
	private int level = 0;

	/**
	 * Parent grid instance.
	 */
	private Grid parent;

	/**
	 * Parent item (if a child item).
	 */
	private GridItem parentItem;

	/**
	 * Is visible?
	 */
	private boolean visible = true;

	/**
	 * Row header text.
	 */
	private String headerText = null;

	/**
	 * Row header image
	 */
	private Image headerImage = null;

	/**
	 * Background color of the header
	 */
	private Color headerBackground = null;

	/**
	 * Foreground color of the header
	 */
	public Color headerForeground = null;

	/**
	 * (SWT.VIRTUAL only) Flag that specifies whether the client has already
	 * been sent a SWT.SetData event.
	 */
	private boolean hasSetData = false;
    private GridItem[] EMPTY_CHILDREN = new GridItem[0];

    /**
	 * Creates a new instance of this class and places the item at the end of
	 * the grid.
	 *
	 * @param parent
	 *            parent grid
	 * @param style
	 *            item style
	 */
	public GridItem(Grid parent, int style) {
		this(parent, style, -1);
	}

	/**
	 * Creates a new instance of this class and places the item in the grid at
	 * the given index.
	 *
	 * @param parent
	 *            parent grid
	 * @param style
	 *            item style
	 * @param index
	 *            index where to insert item
	 */
	public GridItem(Grid parent, int style, int index) {
		super(parent, style, index);

		this.parent = parent;

		parent.newItem(this, index, true);
		parent.newRootItem(this, index);
	}

	/**
	 * Creates a new instance of this class as a child node of the given
	 * GridItem and places the item at the end of the parents items.
	 *
	 * @param parent
	 *            parent item
	 * @param style
	 *            item style
	 */
	public GridItem(GridItem parent, int style) {
		this(parent, style, -1);
	}

	/**
	 * Creates a new instance of this class as a child node of the given Grid
	 * and places the item at the given index in the parent items list.
	 *
	 * @param parent
	 *            parent item
	 * @param style
	 *            item style
	 * @param index
	 *            index to place item
	 */
	public GridItem(GridItem parent, int style, int index) {
		super(parent, style, index);

		parentItem = parent;
		this.parent = parentItem.getParent();

		this.parent.newItem(this, index, false);

		level = parentItem.getLevel() + 1;

		parentItem.newItem(this, index);

		if (parent.isVisible() && parent.isExpanded()) {
			setVisible(true);
		} else {
			setVisible(false);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void dispose() {
		if (!parent.isDisposing()) {
			parent.removeItem(this);

			if (parentItem != null) {
				parentItem.remove(this);
			} else {
				parent.removeRootItem(this);
			}

            if (children != null) {
                for (int i = children.size() - 1; i >= 0; i--) {
                    children.get(i).dispose();
                }
            }
		}
		super.dispose();
	}

	/**
	 * Adds the listener to the collection of listeners who will be notified
	 * when the row is resized, by sending it one of the messages defined in the
	 * <code>ControlListener</code> interface.
	 * <p>
	 * Clients who wish to override the standard row resize logic should use the
	 * untyped listener mechanisms. The untyped <code>Event</code> object passed
	 * to an untyped listener will have its <code>detail</code> field populated
	 * with the new row height. Clients may alter this value to, for example,
	 * enforce minimum or maximum row sizes. Clients may also set the
	 * <code>doit</code> field to false to prevent the entire resize operation.
	 *
	 * @param listener
	 *            the listener which should be notified
	 */
	public void addControlListener(ControlListener listener) {
		checkWidget();
		if (listener == null)
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		TypedListener typedListener = new TypedListener(listener);
		addListener(SWT.Resize, typedListener);
	}

	/**
	 * Removes the listener from the collection of listeners who will be
	 * notified when the row is resized.
	 *
	 * @param listener
	 *            the listener which should no longer be notified
	 */
	public void removeControlListener(ControlListener listener) {
		checkWidget();
		if (listener == null)
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		removeListener(SWT.Resize, listener);
	}

	/**
	 * Fires the given event type on the parent Grid instance. This method
	 * should only be called from within a cell renderer. Any other use is not
	 * intended.
	 *
	 * @param eventId
	 *            SWT event constant
	 */
	public void fireEvent(int eventId) {
		checkWidget();

		Event e = new Event();
		e.display = getDisplay();
		e.widget = this;
		e.item = this;
		e.type = eventId;

		getParent().notifyListeners(eventId, e);
	}

	/**
	 * Returns the receiver's background color.
	 *
	 * @return the background color
	 */
	public Color getBackground() {
		checkWidget();

		if (defaultBackground == null) {
			return parent.getBackground();
		}
		return defaultBackground;
	}

	/**
	 * Returns the background color at the given column index in the receiver.
	 *
	 * @param index
	 *            the column index
	 * @return the background color
	 */
	public Color getBackground(int index) {
		checkWidget();

		handleVirtual();

		return getCellValue(backgrounds, index, getBackground());
	}

    /**
	 * Returns a rectangle describing the receiver's size and location relative
	 * to its parent at a column in the table.
	 *
	 * @param columnIndex
	 *            the index that specifies the column
	 * @return the receiver's bounding column rectangle
	 */
	public Rectangle getBounds(int columnIndex) {
		checkWidget();

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
		if (!isVisible())
			return new Rectangle(-1000, -1000, 0, 0);

		if (!parent.isShown(this))
			return new Rectangle(-1000, -1000, 0, 0);

		Point origin = parent.getOrigin(parent.getColumn(columnIndex), this);

		if (origin.x < 0 && parent.isRowHeaderVisible())
			return new Rectangle(-1000, -1000, 0, 0);

		Point cellSize = this.getCellSize(columnIndex);

		return new Rectangle(origin.x, origin.y, cellSize.x, cellSize.y);
	}

	/**
	 *
	 * @param columnIndex
	 * @return width and height
	 */
	protected Point getCellSize(int columnIndex) {
		int width = 0;

		int span = getColumnSpan(columnIndex);
		for (int i = 0; i <= span; i++) {
			if (parent.getColumnCount() <= columnIndex + i) {
				break;
			}
			width += parent.getColumn(columnIndex + i).getWidth();
		}

		int indexOfCurrentItem = parent.getIndexOfItem(this);

		GridItem item = parent.getItem(indexOfCurrentItem);
		int height = item.getHeight();
		span = getRowSpan(columnIndex);

		for (int i = 1; i <= span; i++) {
			/* We will probably need another escape condition here */
			if (parent.getItems().length <= indexOfCurrentItem + i) {
				break;
			}

			item = parent.getItem(indexOfCurrentItem + i);
			if (item.isVisible()) {
				height += item.getHeight() + 1;
			}
		}

		return new Point(width, height);
	}

	/**
	 * Returns the column span for the given column index in the receiver.
	 *
	 * @param index
	 *            the column index
	 * @return the number of columns spanned (0 equals no columns spanned)
	 */
	public int getColumnSpan(int index) {
		checkWidget();
		Integer i = getCellValue(columnSpans, index);
		if (i == null) {
			return 0;
		}
		return i;
	}

	/**
	 * Returns the row span for the given column index in the receiver.
	 *
	 * @param index
	 *            the row index
	 * @return the number of row spanned (0 equals no row spanned)
	 */
	public int getRowSpan(int index) {
		checkWidget();
        Integer i = getCellValue(rowSpans, index);
        if (i != null) {
            return i;
        }

		return 0;
	}

	/**
	 * Returns the font that the receiver will use to paint textual information
	 * for this item.
	 *
	 * @return the receiver's font
	 */
	public Font getFont() {
		if (defaultFont == null) {
			return parent.getFont();
		}
		return defaultFont;
	}

	/**
	 * Returns the font that the receiver will use to paint textual information
	 * for the specified cell in this item.
	 *
	 * @param index
	 *            the column index
	 * @return the receiver's font
	 */
	public Font getFont(int index) {
		checkWidget();

		handleVirtual();

		return getCellValue(fonts, index, getFont());
	}

	/**
	 * Returns the foreground color that the receiver will use to draw.
	 *
	 * @return the receiver's foreground color
	 */
	public Color getForeground() {
		if (defaultForeground == null) {
			return parent.getForeground();
		}
		return defaultForeground;
	}

	/**
	 * Returns the foreground color at the given column index in the receiver.
	 *
	 * @param index
	 *            the column index
	 * @return the foreground color
	 */
	public Color getForeground(int index) {
		checkWidget();

		handleVirtual();

		return getCellValue(foregrounds, index, getForeground());
	}

	/**
	 * Returns <code>true</code> if the first column in the receiver is grayed,
	 * and false otherwise. When the GridColumn does not have the
	 * <code>CHECK</code> style, return false.
	 *
	 * @return the grayed state of the checkbox
	 */
	public boolean getGrayed() {
		return getGrayed(0);
	}

	/**
	 * Returns <code>true</code> if the column at the given index in the
	 * receiver is grayed, and false otherwise. When the GridColumn does not
	 * have the <code>CHECK</code> style, return false.
	 *
	 * @param index
	 *            the column index
	 * @return the grayed state of the checkbox
	 */
	public boolean getGrayed(int index) {
		checkWidget();

		handleVirtual();

		Boolean b = getCellValue(grayeds, index);
        return b != null && b;
    }

	/**
	 * Returns the height of this <code>GridItem</code>.
	 *
	 * @return height of this <code>GridItem</code>
	 */
	public int getHeight() {
		checkWidget();
		return height;
	}

	/**
	 * {@inheritDoc}
	 */
	public Image getImage() {
		checkWidget();
		return getImage(0);
	}

	/**
	 * Returns the image stored at the given column index in the receiver, or
	 * null if the image has not been set or if the column does not exist.
	 *
	 * @param index
	 *            the column index
	 * @return the image stored at the given column index in the receiver
	 */
	public Image getImage(int index) {
		checkWidget();

		handleVirtual();

		return getCellValue(images, index);
	}

	/**
	 * Returns the item at the given, zero-relative index in the receiver.
	 * Throws an exception if the index is out of range.
	 *
	 * @param index
	 *            the index of the item to return
	 * @return the item at the given index
	 */
	public GridItem getItem(int index) {
		checkWidget();
		return getCellValue(children, index);
	}

	/**
	 * Returns the number of items contained in the receiver that are direct
	 * item children of the receiver.
	 *
	 * @return the number of items
	 */
	public int getItemCount() {
		checkWidget();
		return children == null ? 0 : children.size();
	}

	/**
	 * Searches the receiver's list starting at the first item (index 0) until
	 * an item is found that is equal to the argument, and returns the index of
	 * that item. If no item is found, returns -1.
	 *
	 * @param item
	 *            the search item
	 * @return the index of the item
	 */
	public int indexOf(GridItem item) {
		checkWidget();
		if (item == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
        } else if (item.isDisposed()) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
        }

		return children == null ? -1 : children.indexOf(item);
	}

	/**
	 * Returns a (possibly empty) array of <code>GridItem</code>s which are the
	 * direct item children of the receiver.
	 * <p>
	 * Note: This is not the actual structure used by the receiver to maintain
	 * its list of items, so modifying the array will not affect the receiver.
	 * </p>
	 *
	 * @return the receiver's items
	 */
	public GridItem[] getItems() {
        if (children == null) {
            return EMPTY_CHILDREN;
        }
		return children.toArray(new GridItem[children.size()]);
	}

	/**
	 * Returns the level of this item in the tree.
	 *
	 * @return the level of the item in the tree
	 */
	public int getLevel() {
		checkWidget();
		return level;
	}

	/**
	 * Returns the receiver's parent, which must be a <code>Grid</code>.
	 *
	 * @return the receiver's parent
	 */
	public Grid getParent() {
		checkWidget();
		return parent;
	}

	/**
	 * Returns the receiver's parent item, which must be a <code>GridItem</code>
	 * or null when the receiver is a root.
	 *
	 * @return the receiver's parent item
	 */
	public GridItem getParentItem() {
		checkWidget();
		return parentItem;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getText() {
		checkWidget();
		return getText(0);
	}

	/**
	 * Returns the text stored at the given column index in the receiver, or
	 * empty string if the text has not been set.
	 *
	 * @param index
	 *            the column index
	 * @return the text stored at the given column index in the receiver
	 */
	public String getText(int index) {
		checkWidget();

		handleVirtual();

		return getCellValue(texts, index, "");
	}

	/**
	 * Returns true if this item has children.
	 *
	 * @return true if this item has children
	 */
	public boolean hasChildren() {
		checkWidget();
		return children != null;
	}

	/**
	 * Returns <code>true</code> if the receiver is expanded, and false
	 * otherwise.
	 * <p>
	 *
	 * @return the expanded state
	 */
	public boolean isExpanded() {
		checkWidget();
		return expanded;
	}

	/**
	 * Sets the receiver's background color to the color specified by the
	 * argument, or to the default system color for the item if the argument is
	 * null.
	 *
	 * @param background
	 *            the new color (or null)
	 */
	public void setBackground(Color background) {
		checkWidget();

		if (background != null && background.isDisposed()) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}

		defaultBackground = background;
		parent.redraw();
	}

	/**
	 * Sets the background color at the given column index in the receiver to
	 * the color specified by the argument, or to the default system color for
	 * the item if the argument is null.
	 *
	 * @param index
	 *            the column index
	 * @param background
	 *            the new color (or null)
	 */
	public void setBackground(int index, Color background) {
		checkWidget();
		if (background != null && background.isDisposed()) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
        if (backgrounds == null) {
            backgrounds = makeList();
        }
		backgrounds.set(index, background);
		parent.redraw();
	}

	/**
	 * Sets the column spanning for the column at the given index to span the
	 * given number of subsequent columns.
	 *
	 * @param index
	 *            column index that should span
	 * @param span
	 *            number of subsequent columns to span
	 */
	public void setColumnSpan(int index, int span) {
		checkWidget();
        if (columnSpans == null) {
            columnSpans = makeList();
        }
		columnSpans.set(index, span);
		parent.setHasSpanning(true);
		parent.redraw();
	}

	/**
	 * Sets the row spanning for the row at the given index to span the given
	 * number of subsequent rows.
	 *
	 * @param index
	 *            row index that should span
	 * @param span
	 *            number of subsequent rows to span
	 */
	public void setRowSpan(int index, int span) {
		checkWidget();
        if (rowSpans == null) {
            rowSpans = makeList();
        }
		rowSpans.set(index, span);
		parent.setHasSpanning(true);
		parent.redraw();
	}

	/**
	 * Sets the expanded state of the receiver.
	 * <p>
	 *
	 * @param expanded
	 *            the new expanded state
	 */
	public void setExpanded(boolean expanded) {
		checkWidget();
		this.expanded = expanded;

		// We must unselect any items that are becoming invisible
		// and thus if we change the selection we have to fire a selection event
		boolean unselected = false;

        if (children != null) {
            for (GridItem item : children) {
                item.setVisible(expanded && visible);
                if (!expanded) {
                    if (!getParent().getCellSelectionEnabled()) {
                        if (getParent().isSelected(item)) {
                            unselected = true;
                            getParent().deselect(getParent().indexOf(item));
                        }
                        if (deselectChildren(item)) {
                            unselected = true;
                        }
                    } else {
                        if (deselectCells(item)) {
                            unselected = true;
                        }
                    }
                }
            }
        }

		this.getParent().topIndex = -1;
		this.getParent().bottomIndex = -1;
		this.getParent().setScrollValuesObsolete();

		if (unselected) {
			Event e = new Event();
			e.item = this;
			getParent().notifyListeners(SWT.Selection, e);
		}
		if (getParent().getFocusItem() != null
				&& !getParent().getFocusItem().isVisible()) {
			getParent().setFocusItem(this);
		}

		if (getParent().getCellSelectionEnabled()) {
			getParent().updateColumnSelection();
		}
	}

	private boolean deselectCells(GridItem item) {
		boolean flag = false;

		int index = getParent().indexOf(item);

		GridColumn[] columns = getParent().getColumns();

        for (GridColumn column : columns) {
            Point cell = new Point(getParent().indexOf(column), index);
            if (getParent().isCellSelected(cell)) {
                flag = true;
                getParent().deselectCell(cell);
            }
        }

		GridItem[] kids = item.getItems();
        for (GridItem kid : kids) {
            if (deselectCells(kid)) {
                flag = true;
            }
        }

		return flag;
	}

	/**
	 * Deselects the given item's children recursively.
	 *
	 * @param item
	 *            item to deselect children.
	 * @return true if an item was deselected
	 */
	private boolean deselectChildren(GridItem item) {
		boolean flag = false;
		GridItem[] kids = item.getItems();
        for (GridItem kid : kids) {
            if (getParent().isSelected(kid)) {
                flag = true;
            }
            getParent().deselect(getParent().indexOf(kid));
            if (deselectChildren(kid)) {
                flag = true;
            }
        }
		return flag;
	}

	/**
	 * Sets the font that the receiver will use to paint textual information for
	 * this item to the font specified by the argument, or to the default font
	 * for that kind of control if the argument is null.
	 *
	 * @param f
	 *            the new font (or null)
	 */
	public void setFont(Font f) {
		checkWidget();
		if (f != null && f.isDisposed()) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		defaultFont = f;
		parent.redraw();
	}

	/**
	 * Sets the font that the receiver will use to paint textual information for
	 * the specified cell in this item to the font specified by the argument, or
	 * to the default font for that kind of control if the argument is null.
	 *
	 * @param index
	 *            the column index
	 * @param font
	 *            the new font (or null)
	 */
	public void setFont(int index, Font font) {
		checkWidget();
		if (font != null && font.isDisposed()) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
        if (fonts == null) {
            fonts = makeList();
        }
		fonts.set(index, font);
		parent.redraw();
	}

	/**
	 * Sets the receiver's foreground color to the color specified by the
	 * argument, or to the default system color for the item if the argument is
	 * null.
	 *
	 * @param foreground
	 *            the new color (or null)
	 */
	public void setForeground(Color foreground) {
		checkWidget();
		if (foreground != null && foreground.isDisposed()) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		defaultForeground = foreground;
		parent.redraw();
	}

	/**
	 * Sets the foreground color at the given column index in the receiver to
	 * the color specified by the argument, or to the default system color for
	 * the item if the argument is null.
	 *
	 * @param index
	 *            the column index
	 * @param foreground
	 *            the new color (or null)
	 */
	public void setForeground(int index, Color foreground) {
		checkWidget();
		if (foreground != null && foreground.isDisposed()) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
        if (foregrounds == null) {
            foregrounds = makeList();
        }
		foregrounds.set(index, foreground);
		parent.redraw();
	}

	/**
	 * Sets the grayed state of the checkbox for the first column. This state
	 * change only applies if the GridColumn was created with the SWT.CHECK
	 * style.
	 *
	 * @param grayed
	 *            the new grayed state of the checkbox;
	 */
	public void setGrayed(boolean grayed) {
		checkWidget();
		setGrayed(0, grayed);
		parent.redraw();
	}

	/**
	 * Sets the grayed state of the checkbox for the given column index. This
	 * state change only applies if the GridColumn was created with the
	 * SWT.CHECK style.
	 *
	 * @param index
	 *            the column index
	 * @param grayed
	 *            the new grayed state of the checkbox;
	 */
	public void setGrayed(int index, boolean grayed) {
		checkWidget();
        if (grayeds == null) {
            grayeds = makeList();
        }
		grayeds.set(index, grayed);
		parent.redraw();
	}

	/**
	 * Sets the height of this <code>GridItem</code>.
	 *
	 * @param newHeight
	 *            new height in pixels
	 */
	public void setHeight(int newHeight) {
		checkWidget();
		if (newHeight < 1)
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		height = newHeight;
		parent.hasDifferingHeights = true;
		if (isVisible()) {
			int myIndex = parent.indexOf(this);
			if (parent.getTopIndex() <= myIndex
					&& myIndex <= parent.getBottomIndex()) // note: cannot use
															// Grid#isShown()
															// here, because
															// that returns
															// false for
															// partially shown
															// items
				parent.bottomIndex = -1;
		}
		parent.setScrollValuesObsolete();
		parent.redraw();
	}

	/**
	 * Sets this <code>GridItem</code> to its preferred height.
	 */
	public void pack() {
		checkWidget();

		int maxPrefHeight = 2;
		GridColumn[] columns = parent.getColumns();
		GC gc = new GC(parent);
		for (int cnt = 0; cnt < columns.length; cnt++) {
			if (!columns[cnt].isVisible())
				continue; // invisible columns do not affect item/row height

			GridCellRenderer renderer = columns[cnt].getCellRenderer();

			renderer.setAlignment(columns[cnt].getAlignment());
			renderer.setColumn(cnt);
			renderer.setTree(columns[cnt].isTree());
			renderer.setWordWrap(columns[cnt].getWordWrap());

			Point size = renderer.computeSize(gc, columns[cnt].getWidth(),
					SWT.DEFAULT, this);
			if (size != null)
				maxPrefHeight = Math.max(maxPrefHeight, size.y);
		}
		gc.dispose();

		setHeight(maxPrefHeight);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setImage(Image image) {
		setImage(0, image);
		parent.redraw();
	}

	/**
	 * Sets the receiver's image at a column.
	 *
	 * @param index
	 *            the column index
	 * @param image
	 *            the new image
	 */
	public void setImage(int index, Image image) {
		checkWidget();
		if (image != null && image.isDisposed()) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
        if (images == null) {
            images = makeList();
        }
		images.set(index, image);

		parent.imageSetOnItem(index, this);

		parent.redraw();
	}

	/**
	 * Sets the receiver's text at a column.
	 *
	 * @param index
	 *            the column index
	 * @param text
	 *            the new text
	 */
	public void setText(int index, String text) {
		checkWidget();
		if (text == null) {
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		}
        if (texts == null) {
            texts = makeList();
        }
		texts.set(index, text);
		parent.redraw();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setText(String string) {
		setText(0, string);
		parent.redraw();
	}

	/**
	 * Adds items to the given list to ensure the list is large enough to hold a
	 * value for each column.
	 *
	 * @param al
	 *            list
	 */
	private <T> void ensureSize(ArrayList<T> al) {
		int count = Math.max(1, parent.getColumnCount());
		al.ensureCapacity(count);
		while (al.size() <= count) {
			al.add(null);
		}
	}

	/**
	 * Removes the given child item from the list of children.
	 *
	 * @param child
	 *            child to remove
	 */
	private void remove(GridItem child) {
        if (children != null) {
		    children.remove(child);
            if (children.isEmpty()) {
                children = null;
            }
        }
	}

	/**
	 * Returns true if the item is visible because its parent items are all
	 * expanded. This method does not determine if the item is in the currently
	 * visible range.
	 *
	 * @return Returns the visible.
	 */
	boolean isVisible() {
		return visible;
	}

	/**
	 * Creates a new child item in this item at the given index.
	 *
	 * @param item
	 *            new child item
	 * @param index
	 *            index
	 */
	void newItem(GridItem item, int index) {
        if (children == null) {
            children = new ArrayList<GridItem>();
        }
		if (index == -1) {
			children.add(item);
		} else {
			children.add(index, item);
		}
	}

	/**
	 * Sets the visible state of this item. The visible state is determined by
	 * the expansion state of all of its parent items. If all parent items are
	 * expanded it is visible.
	 *
	 * @param visible
	 *            The visible to set.
	 */
	void setVisible(boolean visible) {
		if (this.visible == visible) {
			return;
		}

		this.visible = visible;

		if (visible) {
			parent.updateVisibleItems(1);
		} else {
			parent.updateVisibleItems(-1);
		}

		if (children != null) {
			boolean childrenVisible = visible;
			if (visible) {
				childrenVisible = expanded;
			}
            for (GridItem item : children) {
                item.setVisible(childrenVisible);
            }
		}
	}

	/**
	 * Returns the receiver's row header text. If the text is <code>null</code>
	 * the row header will display the row number.
	 *
	 * @return the text stored for the row header or code <code>null</code> if
	 *         the default has to be displayed
	 */
	public String getHeaderText() {
		checkWidget();

		// handleVirtual();

		return headerText;
	}

	/**
	 * Returns the receiver's row header image.
	 *
	 * @return the image stored for the header or <code>null</code> if none has
	 *         to be displayed
	 */
	public Image getHeaderImage() {
		checkWidget();
		return headerImage;
	}

	/**
	 * Returns the receiver's row header background color
	 *
	 * @return the color or <code>null</code> if none
	 */
	public Color getHeaderBackground() {
		checkWidget();
		return headerBackground;
	}

	/**
	 * Returns the receiver's row header foreground color
	 *
	 * @return the color or <code>null</code> if none
	 */
	public Color getHeaderForeground() {
		checkWidget();
		return headerForeground;
	}

	/**
	 * Sets the receiver's row header text. If the text is <code>null</code> the
	 * row header will display the row number.
	 *
	 * @param text
	 *            the new text
	 */
	public void setHeaderText(String text) {
		checkWidget();
		// if (text == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		if (!text.equals(headerText)) {
			GC gc = new GC(parent);

			int oldWidth = parent.getRowHeaderRenderer().computeSize(gc,
					SWT.DEFAULT, SWT.DEFAULT, this).x;

			this.headerText = text;

			int newWidth = parent.getRowHeaderRenderer().computeSize(gc,
					SWT.DEFAULT, SWT.DEFAULT, this).x;

			gc.dispose();

			parent.recalculateRowHeaderWidth(oldWidth, newWidth);
		}
		parent.redraw();
	}

	/**
	 * Sets the receiver's row header image. If the image is <code>null</code>
	 * none is shown in the header
	 *
	 * @param image
	 *            the new image
	 */
	public void setHeaderImage(Image image) {
		checkWidget();
		// if (text == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
		if (image != headerImage) {
			GC gc = new GC(parent);

			int oldWidth = parent.getRowHeaderRenderer().computeSize(gc, SWT.DEFAULT, SWT.DEFAULT, this).x;
			//int oldHeight = parent.getRowHeaderRenderer().computeSize(gc, SWT.DEFAULT, SWT.DEFAULT, this).y;

			this.headerImage = image;

			int newWidth = parent.getRowHeaderRenderer().computeSize(gc, SWT.DEFAULT, SWT.DEFAULT, this).x;
			int newHeight = parent.getRowHeaderRenderer().computeSize(gc, SWT.DEFAULT, SWT.DEFAULT, this).y;

			gc.dispose();

			parent.recalculateRowHeaderWidth(oldWidth, newWidth);
			parent.recalculateRowHeaderHeight(newHeight);
		}
		parent.redraw();
	}

	/**
	 * Set the new header background
	 *
	 * @param headerBackground
	 *            the color or <code>null</code>
	 */
	public void setHeaderBackground(Color headerBackground) {
		checkWidget();
		this.headerBackground = headerBackground;
		parent.redraw();
	}

	/**
	 * Set the new header foreground
	 *
	 * @param headerForeground
	 *            the color or <code>null</code>
	 */
	public void setHeaderForeground(Color headerForeground) {
		checkWidget();
		this.headerForeground = headerForeground;
		parent.redraw();
	}

	/**
	 * Returns the tooltip for the given cell.
	 *
	 * @param index
	 *            the column index
	 * @return the tooltip
	 */
	public String getToolTipText(int index) {
		checkWidget();

		handleVirtual();

		return getCellValue(tooltips, index);
	}

	/**
	 * Sets the tooltip for the given column index.
	 *
	 * @param index
	 *            the column index
	 * @param tooltip
	 *            the tooltip text
	 */
	public void setToolTipText(int index, String tooltip) {
		checkWidget();
        if (tooltips == null) {
            tooltips = makeList();
        }
		tooltips.set(index, tooltip);
	}

	/**
	 * Notifies the item that a column has been removed.
	 *
	 * @param index
	 *            index of column removed.
	 */
	void columnRemoved(int index) {
		removeValue(index, backgrounds);
		removeValue(index, fonts);
		removeValue(index, foregrounds);
		removeValue(index, grayeds);
		removeValue(index, images);
		removeValue(index, texts);
		removeValue(index, columnSpans);
		removeValue(index, rowSpans);
		removeValue(index, tooltips);
	}

	void columnAdded(int index) {
		insertValue(index, backgrounds);
		insertValue(index, fonts);
		insertValue(index, foregrounds);
		insertValue(index, grayeds);
		insertValue(index, images);
		insertValue(index, texts);
		insertValue(index, columnSpans);
		insertValue(index, rowSpans);
		insertValue(index, tooltips);
		hasSetData = false;
	}

	private void handleVirtual() {
		if ((getParent().getStyle() & SWT.VIRTUAL) != 0 && !hasSetData) {
			hasSetData = true;
			Event event = new Event();
			event.item = this;
			if (parentItem == null) {
				event.index = getParent().indexOf(this);
			} else {
				event.index = parentItem.indexOf(this);
			}
			getParent().notifyListeners(SWT.SetData, event);
		}
	}

	/**
	 * Sets the initial item height for this item.
	 *
	 * @param height
	 *            initial height.
	 */
	void initializeHeight(int height) {
		this.height = height;
	}

	/**
	 * Clears all properties of this item and resets values to their defaults.
	 *
	 * @param allChildren
	 *            <code>true</code> if all child items should be cleared
	 *            recursively, and <code>false</code> otherwise
	 */
	void clear(boolean allChildren) {
		backgrounds = null;
		columnSpans = null;
		rowSpans = null;
		fonts = null;
		foregrounds = null;
		grayeds = null;
		images = null;
		texts = null;
		tooltips = null;

		defaultForeground = null;
		defaultBackground = null;
		defaultFont = null;

		hasSetData = false;
		headerText = null;
		headerImage = null;
		headerBackground = null;
		headerForeground = null;

		// Recursively clear children if requested.
		if (allChildren && children != null) {
			for (int i = children.size() - 1; i >= 0; i--) {
				children.get(i).clear(true);
			}
            children = null;
		}
	}

    private <T> ArrayList<T> makeList()
    {
        ArrayList<T> newList = new ArrayList<T>();
        ensureSize(newList);
        return newList;
    }

    private <T> T getCellValue(ArrayList<T> list, int index)
    {
        return getCellValue(list, index, null);
    }

    private <T> T getCellValue(ArrayList<T> list, int index, T defValue)
    {
        return list == null || list.size() <= index ? defValue : list.get(index);
    }

    private <T> void insertValue(int index, List<T> list) {
        if (list == null) {
            // do nothing
        } else if (index == -1) {
            list.add(null);
        } else {
            list.add(index, null);
        }
    }

    private <T> void removeValue(int index, List<T> list) {
        if (list != null && list.size() > index) {
            list.remove(index);
        }
    }

}
