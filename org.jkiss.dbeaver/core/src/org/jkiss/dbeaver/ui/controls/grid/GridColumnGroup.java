/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.grid;

import org.jkiss.dbeaver.ui.controls.grid.renderers.DefaultColumnGroupHeaderRenderer;
import org.jkiss.dbeaver.ui.controls.grid.renderers.GridHeaderRenderer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.TreeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.TypedListener;

import java.util.Vector;

/**
 * <p>
 * NOTE:  THIS WIDGET AND ITS API ARE STILL UNDER DEVELOPMENT.  THIS IS A PRE-RELEASE ALPHA
 * VERSION.  USERS SHOULD EXPECT API CHANGES IN FUTURE VERSIONS.
 * </p>
 * Instances of this class represent a column group in a grid widget.  A column group header is
 * displayed above grouped columns.  The column group can optionally be configured to expand and
 * collapse.  A column group in the expanded state shows {@code GridColumn}s whose detail property
 * is true.  A column group in the collapsed state shows {@code GridColumn}s whose summary property
 * is true.
 * <p>
 * <dl>
 * <dt><b>Styles:</b></dt>
 * <dd>SWT.TOGGLE</dd>
 * <dt><b>Events:</b></dt>
 * <dd>Expand, Collapse</dd>
 * </dl>
 *
 * @author chris.gross@us.ibm.com
 */
public class GridColumnGroup extends Item
{

    private Grid parent;

    private GridColumn[] columns = new GridColumn[] {};

    private boolean expanded = true;

    private Font headerFont;

    /**
     * Header renderer.
     */
    private GridHeaderRenderer headerRenderer = new DefaultColumnGroupHeaderRenderer();

    /**
     * Constructs a new instance of this class given its parent (which must be a Table) and a style
     * value describing its behavior and appearance.
     *
     * @param parent the parent table
     * @param style the style of the group
     * @throws IllegalArgumentException
     * <ul>
     * <li>ERROR_NULL_ARGUMENT - if the parent is null</li>
     * </ul>
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the parent</li>
     * <li>ERROR_INVALID_SUBCLASS - if this class is not an allowed subclass</li>
     * </ul>
     */
    public GridColumnGroup(Grid parent, int style)
    {
        super(parent, style);
        this.parent = parent;

        headerRenderer.setDisplay(getDisplay());
        parent.newColumnGroup(this);
    }

    /**
     * Adds the listener to the collection of listeners who will
     * be notified when an item in the receiver is expanded or collapsed
     * by sending it one of the messages defined in the <code>TreeListener</code>
     * interface.
     *
     * @param listener the listener which should be notified
     *
     * @exception IllegalArgumentException <ul>
     *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
     * </ul>
     * @exception SWTException <ul>
     *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
     * </ul>
     *
     * @see TreeListener
     * @see #removeTreeListener
     */
    public void addTreeListener(TreeListener listener) {
        checkWidget ();
        if (listener == null) SWT.error (SWT.ERROR_NULL_ARGUMENT);
        TypedListener typedListener = new TypedListener (listener);
        addListener (SWT.Expand, typedListener);
        addListener (SWT.Collapse, typedListener);
    }

    /**
     * Removes the listener from the collection of listeners who will
     * be notified when items in the receiver are expanded or collapsed.
     *
     * @param listener the listener which should no longer be notified
     *
     * @exception IllegalArgumentException <ul>
     *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
     * </ul>
     * @exception SWTException <ul>
     *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
     * </ul>
     *
     * @see TreeListener
     * @see #addTreeListener
     */
    public void removeTreeListener(TreeListener listener) {
        checkWidget ();
        if (listener == null) SWT.error (SWT.ERROR_NULL_ARGUMENT);
        removeListener (SWT.Expand, listener);
        removeListener (SWT.Collapse, listener);
    }

    /**
     * Returns the parent grid.
     *
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public Grid getParent()
    {
        checkWidget();
        return parent;
    }

    int getNewColumnIndex()
    {
        if (columns.length == 0)
        {
            return -1;
        }

        GridColumn lastCol = columns[columns.length - 1];
        return parent.indexOf(lastCol) + 1;
    }

    void newColumn(GridColumn column, int index)
    {
        GridColumn[] newAllColumns = new GridColumn[columns.length + 1];
        System.arraycopy(columns, 0, newAllColumns, 0, columns.length);
        newAllColumns[newAllColumns.length - 1] = column;
        columns = newAllColumns;
    }

    void removeColumn(GridColumn col)
    {

    	if (columns.length == 0)
    		return; // widget is disposing

        GridColumn[] newAllColumns = new GridColumn[columns.length - 1];
        int x = 0;
        for (int i = 0; i < columns.length; i++)
        {
            if (columns[i] != col)
            {
                newAllColumns[x] = columns[i];
                x ++;
            }
        }
        columns = newAllColumns;
    }

    /**
     * Returns the columns within this group.
     * <p>
     * Note: This is not the actual structure used by the receiver to maintain
     * its list of items, so modifying the array will not affect the receiver.
     * </p>
     * @return the columns
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
        GridColumn[] newArray = new GridColumn[columns.length];
        System.arraycopy (columns, 0, newArray, 0, columns.length);
        return newArray;
    }

    /**
     * {@inheritDoc}
     */
    public void dispose()
    {
        super.dispose();

        if (parent.isDisposing())
            return;

        GridColumn[] oldColumns = columns;
        columns = new GridColumn[0];

        for (int i = 0; i < oldColumns.length; i++) {
        	oldColumns[i].dispose();
        }

        parent.removeColumnGroup(this);
    }

    /**
     * Gets the header renderer.
     *
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public GridHeaderRenderer getHeaderRenderer()
    {
        checkWidget();
        return headerRenderer;
    }

    /**
     * Sets the header renderer.
     *
     * @param headerRenderer The headerRenderer to set.
     * @throws IllegalArgumentException
     * <ul>
     * <li>ERROR_NULL_ARGUMENT - if the renderer is null</li>
     * </ul>
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void setHeaderRenderer(GridHeaderRenderer headerRenderer)
    {
        if (headerRenderer == null)
            SWT.error(SWT.ERROR_NULL_ARGUMENT);
        this.headerRenderer = headerRenderer;
        headerRenderer.setDisplay(getDisplay());
    }

    /**
     * Returns true if the receiver is expanded, false otherwise.
     *
     * @return the expanded attribute
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public boolean getExpanded()
    {
        checkWidget();
        return expanded;
    }

    /**
     * Sets the expanded state of the receiver.
     *
     * @param expanded the expanded to set
     * @throws org.eclipse.swt.SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
     */
    public void setExpanded(boolean expanded)
    {
        checkWidget();

        this.expanded = expanded;

        if (!expanded && getParent().getCellSelectionEnabled())
        {
            Vector collapsedCols = new Vector();
            for (int j = 0; j < columns.length; j++)
            {
                if (!columns[j].isSummary())
                {
                    collapsedCols.add(new Integer(getParent().indexOf(columns[j])));
                }
            }
            Point[] selection = getParent().getCellSelection();
            for (int i = 0; i < selection.length; i++)
            {
                if (collapsedCols.contains(new Integer(selection[i].x)))
                {
                    getParent().deselectCell(selection[i]);
                }
            }

            if (getParent().getFocusColumn() != null && getParent().getFocusColumn().getColumnGroup() == this)
            {
                getParent().updateColumnFocus();
            }

            parent.updateColumnSelection();
        }

        if (parent.getCellSelectionEnabled())


        parent.refreshHoverState();
        parent.setScrollValuesObsolete();
        parent.redraw();
    }

    /**
     * Returns the first visible column in this column group.
     *
     * @return first visible column
     */
    GridColumn getFirstVisibleColumn()
    {
        GridColumn[] cols = parent.getColumnsInOrder();
        for (int i = 0; i < cols.length; i++)
        {
            if (cols[i].getColumnGroup() == this && cols[i].isVisible())
            {
                return cols[i];
            }
        }
        return null;
    }

    /**
     * Returns the last visible column in this column group.
     *
     * @return last visible column
     */
    GridColumn getLastVisibleColumn()
    {
        GridColumn[] cols = parent.getColumnsInOrder();
        GridColumn lastVisible = null;
        for (int i = 0; i < cols.length; i++)
        {
            if (cols[i].getColumnGroup() == this && cols[i].isVisible())
            {
                lastVisible = cols[i];
            }
        }
        return lastVisible;
    }

    Rectangle getBounds()
    {
        Rectangle bounds = new Rectangle(0, 0, 0, 0);

        bounds.height = parent.getGroupHeaderHeight();

        boolean foundFirstColumnInGroup = false;

        GridColumn[] cols = parent.getColumnsInOrder();
        for (int i = 0; i < cols.length; i++)
        {
            if (cols[i].getColumnGroup() == this)
            {
            	if  (cols[i].isVisible())
            	{
	                if (!foundFirstColumnInGroup)
	                {
	                    bounds.x = parent.getOrigin(cols[i], null).x;
	                    foundFirstColumnInGroup = true;
	                }
	                bounds.width += cols[i].getWidth();
            	}
            }
            else
            {
                if (foundFirstColumnInGroup)
                {
                    break;
                }
            }
        }

        return bounds;
    }
    /**
     * Sets whether or not text is word-wrapped in the header for this column group. If Grid.setAutoHeight(true) is set, the row height
     * is adjusted to accommodate word-wrapped text.
     * @param wordWrap Set to true to wrap the text, false otherwise
     * @see #getHeaderWordWrap()
     */
    public void setHeaderWordWrap(boolean wordWrap)
    {
        checkWidget();
        headerRenderer.setWordWrap(wordWrap);
        parent.redraw();
    }
    /**
     * Returns whether or not text is word-wrapped in the header for this column group.
     * @return true if the header wraps its text.
     * @see GridColumn#setHeaderWordWrap(boolean)
     */
    public boolean getHeaderWordWrap()
    {
      checkWidget();
      return headerRenderer.isWordWrap();
    }

    /**
     * Returns the font that the receiver will use to paint textual information
     * for the header.
     *
     * @return the receiver's font
     * @throws SWTException
     * <ul>
     * <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
     * <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that
     * created the receiver</li>
     * </ul>
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
     * @param font
     */
    public void setHeaderFont(Font font) {
    	checkWidget();
    	this.headerFont = font;
    }
}
