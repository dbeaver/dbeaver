/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package  org.jkiss.dbeaver.ui.controls.lightgrid;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import  org.jkiss.dbeaver.ui.controls.lightgrid.renderers.GridCellRenderer;

public class GridItem /*extends Item */{
	private ArrayList<Color> backgrounds;
    private ArrayList<Font> fonts;
    private ArrayList<Color> foregrounds;
    private ArrayList<Image> images;
    private ArrayList<String> texts;
    private ArrayList<String> tooltips;

	private Color defaultBackground;
	private Font defaultFont;
	private Color defaultForeground;

	private int height = 1;

	/**
	 * Parent grid instance.
	 */
	private LightGrid parent;

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
    private Object data;

    /**
	 * Creates a new instance of this class and places the item at the end of
	 * the grid.
	 *
	 * @param parent
	 *            parent grid
	 * @param style
	 *            item style
	 */
	public GridItem(LightGrid parent) {
		this(parent, -1);
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
	public GridItem(LightGrid parent, int index) {

		this.parent = parent;

		parent.newItem(this, index);
		parent.newRootItem(this, index);
	}

	/**
	 * {@inheritDoc}
	 */
	public void dispose() {
		if (!parent.isDisposing()) {
			parent.removeItem(this);
            parent.removeRootItem(this);
		}
	}

	/**
	 * Returns the receiver's background color.
	 *
	 * @return the background color
	 */
	public Color getBackground() {
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
		return new Point(parent.getColumn(columnIndex).getWidth(), getHeight());
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
		handleVirtual();

		return getCellValue(foregrounds, index, getForeground());
	}

	/**
	 * Returns the height of this <code>GridItem</code>.
	 *
	 * @return height of this <code>GridItem</code>
	 */
	public int getHeight() {
		return height;
	}

	/**
	 * {@inheritDoc}
	 */
	public Image getImage() {
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
		handleVirtual();

		return getCellValue(images, index);
	}

	/**
	 * Returns the receiver's parent, which must be a <code>Grid</code>.
	 *
	 * @return the receiver's parent
	 */
	public LightGrid getParent() {
		return parent;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getText() {
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
		handleVirtual();

		return getCellValue(texts, index, "");
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
	 * Sets the font that the receiver will use to paint textual information for
	 * this item to the font specified by the argument, or to the default font
	 * for that kind of control if the argument is null.
	 *
	 * @param f
	 *            the new font (or null)
	 */
	public void setFont(Font f) {
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
	 * Sets the height of this <code>GridItem</code>.
	 *
	 * @param newHeight
	 *            new height in pixels
	 */
	public void setHeight(int newHeight) {
		if (newHeight < 1)
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		height = newHeight;
		parent.hasDifferingHeights = true;
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

		parent.setScrollValuesObsolete();
		parent.redraw();
	}

	/**
	 * Sets this <code>GridItem</code> to its preferred height.
	 */
	public void pack() {
		int maxPrefHeight = 2;
		GridColumn[] columns = parent.getColumns();
		GC gc = new GC(parent);
		for (int cnt = 0; cnt < columns.length; cnt++) {
			GridCellRenderer renderer = columns[cnt].getCellRenderer();

			renderer.setAlignment(columns[cnt].getAlignment());
			renderer.setColumn(cnt);
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
	 * Returns the receiver's row header text. If the text is <code>null</code>
	 * the row header will display the row number.
	 *
	 * @return the text stored for the row header or code <code>null</code> if
	 *         the default has to be displayed
	 */
	public String getHeaderText() {
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
		return headerImage;
	}

	/**
	 * Returns the receiver's row header background color
	 *
	 * @return the color or <code>null</code> if none
	 */
	public Color getHeaderBackground() {
		return headerBackground;
	}

	/**
	 * Returns the receiver's row header foreground color
	 *
	 * @return the color or <code>null</code> if none
	 */
	public Color getHeaderForeground() {
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
		removeValue(index, images);
		removeValue(index, texts);
		removeValue(index, tooltips);
	}

	void columnAdded(int index) {
		insertValue(index, backgrounds);
		insertValue(index, fonts);
		insertValue(index, foregrounds);
		insertValue(index, images);
		insertValue(index, texts);
		insertValue(index, tooltips);
		hasSetData = false;
	}

	private void handleVirtual() {
		if ((getParent().getStyle() & SWT.VIRTUAL) != 0 && !hasSetData) {
			hasSetData = true;
			Event event = new Event();
			event.data = this;
            event.index = getParent().indexOf(this);
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
	void clear() {
		backgrounds = null;
		fonts = null;
		foregrounds = null;
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

    public Object getData()
    {
        return data;
    }

    public void setData(Object data)
    {
        this.data = data;
    }
}
